package com.smartacademictracker.presentation.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.Subject
import com.smartacademictracker.data.model.TeacherApplication
import com.smartacademictracker.data.model.ApplicationStatus
import com.smartacademictracker.data.repository.SubjectRepository
import com.smartacademictracker.data.repository.TeacherApplicationRepository
import com.smartacademictracker.data.repository.UserRepository
import com.smartacademictracker.data.repository.EnrollmentRepository
import com.smartacademictracker.data.repository.SectionAssignmentRepository
import com.smartacademictracker.data.repository.StudentEnrollmentRepository
import com.smartacademictracker.data.manager.TeacherDataCache
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeacherSubjectsViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val teacherApplicationRepository: TeacherApplicationRepository,
    private val userRepository: UserRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val sectionAssignmentRepository: SectionAssignmentRepository,
    private val studentEnrollmentRepository: StudentEnrollmentRepository,
    private val notificationSenderService: com.smartacademictracker.data.notification.NotificationSenderService,
    private val teacherDataCache: TeacherDataCache
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherSubjectsUiState())
    val uiState: StateFlow<TeacherSubjectsUiState> = _uiState.asStateFlow()

    private val _availableSubjects = MutableStateFlow<List<Subject>>(emptyList())
    val availableSubjects: StateFlow<List<Subject>> = _availableSubjects.asStateFlow()

    private val _mySubjects = MutableStateFlow<List<Subject>>(emptyList())
    val mySubjects: StateFlow<List<Subject>> = _mySubjects.asStateFlow()

    private val _appliedSubjects = MutableStateFlow<List<Subject>>(emptyList())
    val appliedSubjects: StateFlow<List<Subject>> = _appliedSubjects.asStateFlow()

    private val _applications = MutableStateFlow<List<TeacherApplication>>(emptyList())
    val applications: StateFlow<List<TeacherApplication>> = _applications.asStateFlow()

    private val _approvedApplications = MutableStateFlow<List<TeacherApplication>>(emptyList())
    val approvedApplications: StateFlow<List<TeacherApplication>> = _approvedApplications.asStateFlow()
    
    private var subjectsFlowJob: Job? = null
    private var currentTeacherId: String? = null

    init {
        // Preload data immediately when ViewModel is created
        viewModelScope.launch {
            val cachedSubjects = teacherDataCache.cachedSubjects.value
            if (cachedSubjects.isNotEmpty() && teacherDataCache.isCacheValid()) {
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        // Process cached data through filtering functions
                        filterAvailableSubjects(user.id, user.departmentCourseId, cachedSubjects)
                        loadMySubjects(user.id, cachedSubjects)
                        loadAppliedSubjects(user.id)
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                }
            } else {
                // If no cache, start loading immediately
                loadSubjects(forceRefresh = false)
            }
        }
    }

    fun loadSubjects(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val cachedSubjects = teacherDataCache.cachedSubjects.value
            val hasValidCache = !forceRefresh && cachedSubjects.isNotEmpty() && teacherDataCache.isCacheValid()
            
            // Show loading only if we don't have cached data
            if (!hasValidCache) {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }
            
            try {
                // Get current user
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        // If we have cached subjects, process them immediately while loading fresh data
                        if (hasValidCache) {
                            // Process cached data through filtering functions immediately
                            filterAvailableSubjects(user.id, user.departmentCourseId, cachedSubjects)
                            loadMySubjects(user.id, cachedSubjects)
                            loadAppliedSubjects(user.id)
                            _uiState.value = _uiState.value.copy(isLoading = false)
                        }
                        
                        // Cancel any existing flow job
                        if (currentTeacherId != user.id) {
                            subjectsFlowJob?.cancel()
                            currentTeacherId = user.id
                        }
                        
                        // Set up real-time listener for teacher's subjects
                        if (subjectsFlowJob == null || !subjectsFlowJob!!.isActive) {
                            android.util.Log.d("TeacherSubjectsViewModel", "Setting up real-time listener for teacher subjects - teacherId: ${user.id}")
                            subjectsFlowJob = viewModelScope.launch {
                                subjectRepository.getSubjectsByTeacherFlow(user.id)
                                    .catch { exception ->
                                        android.util.Log.e("TeacherSubjectsViewModel", "Flow error in getSubjectsByTeacherFlow: ${exception.message}", exception)
                                        // Fallback to one-time query on error
                                        val allSubjectsResult = subjectRepository.getAllSubjects()
                                        allSubjectsResult.onSuccess { allSubjects ->
                                            android.util.Log.d("TeacherSubjectsViewModel", "Fallback: Loaded ${allSubjects.size} subjects")
                                            teacherDataCache.updateSubjects(allSubjects)
                                            filterAvailableSubjects(user.id, user.departmentCourseId, allSubjects)
                                            loadMySubjects(user.id, allSubjects)
                                            loadAppliedSubjects(user.id)
                                            _uiState.value = _uiState.value.copy(isLoading = false)
                                        }
                                    }
                                    .collect { subjectsFromFlow ->
                                        android.util.Log.d("TeacherSubjectsViewModel", "Flow collected ${subjectsFromFlow.size} subjects for teacher ${user.id}")
                                        subjectsFromFlow.forEach { subject ->
                                            android.util.Log.d("TeacherSubjectsViewModel", "Subject from Flow - id: ${subject.id}, name: ${subject.name}, teacherId: ${subject.teacherId}")
                                        }
                                        
                                        // IMPORTANT: getSubjectsByTeacherFlow only returns subjects where teacherId field is set on Subject document
                                        // But teachers are assigned via section_assignments, so we need to load ALL subjects
                                        // and then filter by section_assignments in loadMySubjects
                                        android.util.Log.d("TeacherSubjectsViewModel", "Loading ALL subjects to properly filter by section_assignments")
                                        val allSubjectsResult = subjectRepository.getAllSubjects()
                                        allSubjectsResult.onSuccess { allSubjects ->
                                            android.util.Log.d("TeacherSubjectsViewModel", "Loaded ${allSubjects.size} total subjects")
                                            teacherDataCache.updateSubjects(allSubjects)
                                            
                                            // Process fresh data through filtering functions
                                            android.util.Log.d("TeacherSubjectsViewModel", "Processing subjects through filtering functions")
                                            filterAvailableSubjects(user.id, user.departmentCourseId, allSubjects)
                                            loadMySubjects(user.id, allSubjects)
                                            loadAppliedSubjects(user.id)
                                            
                                            _uiState.value = _uiState.value.copy(isLoading = false)
                                        }.onFailure { exception ->
                                            android.util.Log.e("TeacherSubjectsViewModel", "Failed to load all subjects: ${exception.message}", exception)
                                            // Fallback to using subjects from Flow
                                            teacherDataCache.updateSubjects(subjectsFromFlow)
                                            filterAvailableSubjects(user.id, user.departmentCourseId, subjectsFromFlow)
                                            loadMySubjects(user.id, subjectsFromFlow)
                                            loadAppliedSubjects(user.id)
                                            _uiState.value = _uiState.value.copy(isLoading = false)
                                        }
                                    }
                            }
                        } else {
                            android.util.Log.d("TeacherSubjectsViewModel", "Flow job already active, skipping setup")
                        }
                        
                        // Also load all subjects for available subjects filtering
                        val allSubjectsResult = subjectRepository.getAllSubjects()
                        allSubjectsResult.onSuccess { allSubjects ->
                            teacherDataCache.updateSubjects(allSubjects)
                            filterAvailableSubjects(user.id, user.departmentCourseId, allSubjects)
                            _uiState.value = _uiState.value.copy(isLoading = false)
                        }.onFailure { exception ->
                            // Only show error if we don't have cached data or forcing refresh
                            if (!hasValidCache || forceRefresh) {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = exception.message ?: "Failed to load subjects"
                                )
                            }
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "User not found"
                        )
                    }
                }.onFailure { exception ->
                    // Only show error if we don't have cached data or forcing refresh
                    if (!hasValidCache || forceRefresh) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load user data"
                        )
                    }
                }
            } catch (e: Exception) {
                // Only show error if we don't have cached data or forcing refresh
                if (!hasValidCache || forceRefresh) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load subjects"
                    )
                }
            }
        }
    }

    fun applyForSubject(subjectId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                applyingSubjects = _uiState.value.applyingSubjects + subjectId,
                error = null
            )
            
            try {
                // Get current user
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        // Check if teacher has an active application (PENDING or APPROVED) for this subject
                        // Allow reapplication if previous application was REJECTED
                        val hasActiveResult = teacherApplicationRepository.hasTeacherActiveApplication(user.id, subjectId)
                        hasActiveResult.onSuccess { hasActive ->
                            if (hasActive) {
                                _uiState.value = _uiState.value.copy(
                                    applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                                    error = "You have already applied for this subject. Please check your applications."
                                )
                                return@onSuccess
                            }
                            
                            // Get subject details
                            val subjectResult = subjectRepository.getSubjectById(subjectId)
                            subjectResult.onSuccess { subject ->
                                // Validate that teacher can apply for this subject based on department and type
                                val canApply = when (subject.subjectType) {
                                    com.smartacademictracker.data.model.SubjectType.MAJOR -> {
                                        // MAJOR subjects: only teachers of the same course/department can apply
                                        user.departmentCourseId != null && 
                                        user.departmentCourseId.isNotBlank() && 
                                        subject.courseId == user.departmentCourseId
                                    }
                                    com.smartacademictracker.data.model.SubjectType.MINOR -> {
                                        // MINOR subjects: any teacher can apply (cross-departmental)
                                        true
                                    }
                                }
                                
                                if (!canApply) {
                                    _uiState.value = _uiState.value.copy(
                                        applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                                        error = "You don't have permission to apply for this subject. MAJOR subjects are only available to teachers in the same department."
                                    )
                                    return@onSuccess
                                }
                                
                                // Create application
                                val application = TeacherApplication(
                                    teacherId = user.id,
                                    teacherName = "${user.firstName} ${user.lastName}",
                                    teacherEmail = user.email,
                                    subjectId = subject.id,
                                    subjectName = subject.name,
                                    subjectCode = subject.code,
                                    applicationReason = "I would like to teach this subject based on my expertise and experience.",
                                    status = ApplicationStatus.PENDING
                                )

                                // Submit application
                                val applicationResult = teacherApplicationRepository.createApplication(application)
                                applicationResult.onSuccess {
                                    // Notify all admins about the new teacher application
                                    notifyAdminsOfTeacherApplication(application)
                                    
                                    _uiState.value = _uiState.value.copy(
                                        applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                                        successMessage = "Application submitted successfully!"
                                    )
                                    // Reload subjects to update the list (non-blocking, in background)
                                    viewModelScope.launch {
                                        // Only reload what's necessary - don't block UI
                                        loadAppliedSubjects(user.id)
                                        
                                        // Update available subjects by removing the applied one
                                        val currentAvailable = _availableSubjects.value.toMutableList()
                                        currentAvailable.removeAll { it.id == subjectId }
                                        _availableSubjects.value = currentAvailable
                                    }
                                }.onFailure { exception ->
                                    _uiState.value = _uiState.value.copy(
                                        applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                                        error = exception.message ?: "Failed to submit application"
                                    )
                                }
                            }.onFailure { exception ->
                                _uiState.value = _uiState.value.copy(
                                    applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                                    error = exception.message ?: "Subject not found"
                                )
                            }
                        }.onFailure { exception ->
                            _uiState.value = _uiState.value.copy(
                                applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                                error = exception.message ?: "Failed to check application status"
                            )
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                            error = "User not found"
                        )
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                        error = exception.message ?: "Failed to get user data"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                    error = e.message ?: "Failed to apply for subject"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun refreshSubjects() {
        loadSubjects()
    }

    fun cancelApplication(applicationId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                teacherApplicationRepository.cancelApplication(applicationId).onSuccess {
                    // Reload all data to reflect the change
                    loadSubjects()
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to cancel application"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to cancel application"
                )
            }
        }
    }
    
    suspend fun getStudentCountForSubject(subjectId: String): Int {
        return try {
            // Use the new StudentEnrollmentRepository to get active students
            val currentUserResult = userRepository.getCurrentUser()
            currentUserResult.onSuccess { user ->
                if (user != null) {
                    // Get teacher's assigned sections for this subject
                    val assignmentsResult = sectionAssignmentRepository.getSectionAssignmentsByTeacher(user.id)
                    assignmentsResult.onSuccess { assignments ->
                        val subjectAssignments = assignments.filter { it.subjectId == subjectId }
                        if (subjectAssignments.isNotEmpty()) {
                            // Get students from assigned sections
                            val sectionNames = subjectAssignments.map { it.sectionName }
                            val studentEnrollmentsResult = studentEnrollmentRepository.getStudentsBySubject(subjectId)
                            studentEnrollmentsResult.onSuccess { enrollments ->
                                // Filter by teacher's assigned sections
                                // Note: getStudentsBySubject already filters by ACTIVE status, but we double-check
                                val filteredEnrollments = enrollments.filter { 
                                    it.sectionName in sectionNames && it.status == com.smartacademictracker.data.model.EnrollmentStatus.ACTIVE
                                }
                                return filteredEnrollments.size
                            }.onFailure { exception ->
                                return 0
                            }
                        } else {
                            return 0
                        }
                    }.onFailure { exception ->
                        return 0
                    }
                }
            }.onFailure { exception ->
                return 0
            }
            0
        } catch (e: Exception) {
            0
        }
    }

    suspend fun getSectionAvailability(subjectId: String): Map<String, Boolean> {
        return try {
            val assignmentsResult = sectionAssignmentRepository.getSectionAssignmentsBySubject(subjectId)
            assignmentsResult.onSuccess { assignments ->
                // Get the subject to know all its sections
                val subjectResult = subjectRepository.getSubjectById(subjectId)
                subjectResult.onSuccess { subject ->
                    val assignedSections = assignments.map { it.sectionName }.toSet()
                    val allSections = subject.sections.toSet()
                    
                    // Return map of section name to availability (true = available, false = assigned)
                    val availability = allSections.associateWith { sectionName ->
                        !assignedSections.contains(sectionName)
                    }
                    return availability
                }.onFailure { exception ->
                    return emptyMap()
                }
            }.onFailure { exception ->
                return emptyMap()
            }
            emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    suspend fun getAssignedSectionsForSubject(subjectId: String): List<String> {
        return try {
            val currentUserResult = userRepository.getCurrentUser()
            currentUserResult.onSuccess { user ->
                if (user != null) {
                    val assignmentsResult = sectionAssignmentRepository.getSectionAssignmentsByTeacher(user.id)
                    assignmentsResult.onSuccess { assignments ->
                        // Filter assignments for this specific subject
                        val subjectAssignments = assignments.filter { it.subjectId == subjectId }
                        return subjectAssignments.map { it.sectionName }
                    }.onFailure {
                        return emptyList()
                    }
                }
            }.onFailure {
                return emptyList()
            }
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun loadMySubjects(teacherId: String, allSubjects: List<Subject>) {
        try {
            android.util.Log.d("TeacherSubjectsViewModel", "loadMySubjects called - teacherId: $teacherId, allSubjects count: ${allSubjects.size}")
            android.util.Log.d("TeacherSubjectsViewModel", "allSubjects IDs: ${allSubjects.map { it.id }}")
            
            // Get section assignments for this teacher
            val assignmentsResult = sectionAssignmentRepository.getSectionAssignmentsByTeacher(teacherId)
            assignmentsResult.onSuccess { assignments ->
                android.util.Log.d("TeacherSubjectsViewModel", "getSectionAssignmentsByTeacher returned ${assignments.size} assignments")
                assignments.forEach { assignment ->
                    android.util.Log.d("TeacherSubjectsViewModel", "Assignment - subjectId: ${assignment.subjectId}, sectionName: ${assignment.sectionName}, status: ${assignment.status}, teacherId: ${assignment.teacherId}")
                }
                
                // Filter only ACTIVE assignments
                val activeAssignments = assignments.filter { it.status == com.smartacademictracker.data.model.AssignmentStatus.ACTIVE }
                android.util.Log.d("TeacherSubjectsViewModel", "Active assignments count: ${activeAssignments.size}")
                
                // Get unique subject IDs from assignments
                val assignedSubjectIds = activeAssignments.map { it.subjectId }.toSet()
                android.util.Log.d("TeacherSubjectsViewModel", "Assigned subject IDs: $assignedSubjectIds")
                
                // Filter subjects that teacher is assigned to
                val mySubjects = allSubjects.filter { it.id in assignedSubjectIds }
                android.util.Log.d("TeacherSubjectsViewModel", "Filtered mySubjects count: ${mySubjects.size}")
                mySubjects.forEach { subject ->
                    android.util.Log.d("TeacherSubjectsViewModel", "My Subject - id: ${subject.id}, name: ${subject.name}")
                }
                _mySubjects.value = mySubjects
            }.onFailure { exception ->
                android.util.Log.e("TeacherSubjectsViewModel", "Failed to get section assignments: ${exception.message}", exception)
                _mySubjects.value = emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("TeacherSubjectsViewModel", "Exception in loadMySubjects: ${e.message}", e)
            _mySubjects.value = emptyList()
        }
    }

    private suspend fun filterAvailableSubjects(teacherId: String, teacherDepartmentCourseId: String?, subjects: List<Subject>) {
        try {
            // Load all section assignments once (more efficient than querying per subject)
            val allAssignmentsResult = sectionAssignmentRepository.getAllSectionAssignments()
            val allAssignments = allAssignmentsResult.getOrNull().orEmpty()
            
            // Group assignments by subject ID for quick lookup
            val assignmentsBySubject = allAssignments.groupBy { it.subjectId }
            
            // Get teacher's applications
            val applicationsResult = teacherApplicationRepository.getApplicationsByTeacher(teacherId)
            applicationsResult.onSuccess { applications ->
                // Get subject IDs that teacher has active applications for (PENDING or APPROVED)
                // Exclude REJECTED applications to allow reapplication
                val appliedSubjectIds = applications
                    .filter { it.status == ApplicationStatus.PENDING || it.status == ApplicationStatus.APPROVED }
                    .map { it.subjectId }
                    .toSet()
                
                // Filter subjects based on department and subject type rules
                val availableSubjects = mutableListOf<Subject>()
                for (subject in subjects) {
                    val notApplied = subject.id !in appliedSubjectIds
                    
                    if (notApplied) {
                        // Check department and subject type visibility rules
                        val isVisible = when (subject.subjectType) {
                            com.smartacademictracker.data.model.SubjectType.MAJOR -> {
                                // MAJOR subjects: only visible to teachers of the same course/department
                                teacherDepartmentCourseId != null && 
                                teacherDepartmentCourseId.isNotBlank() && 
                                subject.courseId == teacherDepartmentCourseId
                            }
                            com.smartacademictracker.data.model.SubjectType.MINOR -> {
                                // MINOR subjects: visible to all teachers (cross-departmental)
                                true
                            }
                        }
                        
                        if (isVisible) {
                            // Check if this subject has any available sections using pre-loaded assignments
                            val subjectAssignments = assignmentsBySubject[subject.id].orEmpty()
                            val assignedSections = subjectAssignments.map { it.sectionName }.toSet()
                            val allSections = subject.sections.toSet()
                            val hasAvailableSections = allSections.any { it !in assignedSections }
                            
                            if (hasAvailableSections) {
                                availableSubjects.add(subject)
                            }
                        }
                    }
                }
                _availableSubjects.value = availableSubjects
            }.onFailure { exception ->
                // If we can't load applications, filter by department/type rules only
                val availableSubjects = subjects.filter { subject ->
                    val isVisible = when (subject.subjectType) {
                        com.smartacademictracker.data.model.SubjectType.MAJOR -> {
                            teacherDepartmentCourseId != null && 
                            teacherDepartmentCourseId.isNotBlank() && 
                            subject.courseId == teacherDepartmentCourseId
                        }
                        com.smartacademictracker.data.model.SubjectType.MINOR -> {
                            // MINOR subjects: visible to all teachers (cross-departmental)
                            true
                        }
                    }
                    if (isVisible) {
                        // Check section availability using pre-loaded assignments
                        val subjectAssignments = assignmentsBySubject[subject.id].orEmpty()
                        val assignedSections = subjectAssignments.map { it.sectionName }.toSet()
                        val allSections = subject.sections.toSet()
                        allSections.any { it !in assignedSections }
                    } else {
                        false
                    }
                }
                _availableSubjects.value = availableSubjects
            }
        } catch (e: Exception) {
            // If there's an exception, filter by department/type rules only
            val availableSubjects = subjects.filter { subject ->
                val isVisible = when (subject.subjectType) {
                    com.smartacademictracker.data.model.SubjectType.MAJOR -> {
                        teacherDepartmentCourseId != null && 
                        teacherDepartmentCourseId.isNotBlank() && 
                        subject.courseId == teacherDepartmentCourseId
                    }
                    com.smartacademictracker.data.model.SubjectType.MINOR -> {
                        // MINOR subjects: also restricted to teacher's department
                        teacherDepartmentCourseId != null && 
                        teacherDepartmentCourseId.isNotBlank() && 
                        subject.courseId == teacherDepartmentCourseId
                    }
                }
                isVisible && subject.teacherId == null
            }
            _availableSubjects.value = availableSubjects
        }
    }

    private suspend fun loadAppliedSubjects(teacherId: String) {
        try {
            // Get teacher's applications
            val applicationsResult = teacherApplicationRepository.getApplicationsByTeacher(teacherId)
            applicationsResult.onSuccess { applications ->
                // Store all applications
                _applications.value = applications
                
                // Store approved applications separately
                val approvedApps = applications.filter { it.status == ApplicationStatus.APPROVED }
                _approvedApplications.value = approvedApps
                
                // Get subject IDs that teacher has applied for
                val appliedSubjectIds = applications
                    .filter { it.status == ApplicationStatus.PENDING }
                    .map { it.subjectId }
                    .toSet()
                
                // Load subject details for applied subjects
                val appliedSubjectsList = mutableListOf<Subject>()
                for (subjectId in appliedSubjectIds) {
                    val subjectResult = subjectRepository.getSubjectById(subjectId)
                    subjectResult.onSuccess { subject ->
                        appliedSubjectsList.add(subject)
                    }
                }
                _appliedSubjects.value = appliedSubjectsList
            }.onFailure { exception ->
                _appliedSubjects.value = emptyList()
            }
        } catch (e: Exception) {
            _appliedSubjects.value = emptyList()
        }
    }
    
    /**
     * Notify all admins when a teacher applies for a subject
     */
    private suspend fun notifyAdminsOfTeacherApplication(application: TeacherApplication) {
        try {
            val adminsResult = userRepository.getUsersByRole(com.smartacademictracker.data.model.UserRole.ADMIN)
            adminsResult.onSuccess { admins ->
                admins.forEach { admin ->
                    notificationSenderService.sendNotification(
                        userId = admin.id,
                        type = com.smartacademictracker.data.model.NotificationType.TEACHER_APPLICATION_SUBMITTED,
                        variables = mapOf(
                            "teacherName" to application.teacherName,
                            "subjectName" to application.subjectName,
                            "subjectCode" to application.subjectCode,
                            "applicationId" to application.id
                        ),
                        priority = com.smartacademictracker.data.model.NotificationPriority.NORMAL
                    )
                }
            }
        } catch (e: Exception) {
            // Silent failure
        }
    }
}

data class TeacherSubjectsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val applyingSubjects: Set<String> = emptySet()
)
