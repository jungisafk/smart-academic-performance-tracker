package com.smartacademictracker.presentation.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.Subject
import com.smartacademictracker.data.model.StudentApplication
import com.smartacademictracker.data.model.StudentApplicationStatus
import com.smartacademictracker.data.repository.StudentApplicationRepository
import com.smartacademictracker.data.repository.SubjectRepository
import com.smartacademictracker.data.repository.UserRepository
import com.smartacademictracker.data.repository.YearLevelRepository
import com.smartacademictracker.data.manager.StudentDataCache
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

@HiltViewModel
class StudentSubjectApplicationViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val subjectRepository: SubjectRepository,
    private val yearLevelRepository: YearLevelRepository,
    private val studentApplicationRepository: StudentApplicationRepository,
    private val sectionAssignmentRepository: com.smartacademictracker.data.repository.SectionAssignmentRepository,
    private val studentEnrollmentRepository: com.smartacademictracker.data.repository.StudentEnrollmentRepository,
    private val notificationSenderService: com.smartacademictracker.data.notification.NotificationSenderService,
    private val studentDataCache: StudentDataCache
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentSubjectApplicationUiState())
    val uiState: StateFlow<StudentSubjectApplicationUiState> = _uiState.asStateFlow()

    private val _availableSubjects = MutableStateFlow<List<Subject>>(emptyList())
    val availableSubjects: StateFlow<List<Subject>> = _availableSubjects.asStateFlow()

    private val _myApplications = MutableStateFlow<List<StudentApplication>>(emptyList())
    val myApplications: StateFlow<List<StudentApplication>> = _myApplications.asStateFlow()

    private val _selectedYearLevel = MutableStateFlow("")
    val selectedYearLevel: StateFlow<String> = _selectedYearLevel.asStateFlow()

    private val _selectedCourse = MutableStateFlow("")
    val selectedCourse: StateFlow<String> = _selectedCourse.asStateFlow()

    // Map to track which subjects the student can apply for (considering enrollment status)
    private val _canApplyForSubject = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val canApplyForSubject: StateFlow<Map<String, Boolean>> = _canApplyForSubject.asStateFlow()

    init {
        // Load cached applications immediately if available (these are already student-specific)
        val cachedApplications = studentDataCache.cachedStudentApplications.value
        if (cachedApplications.isNotEmpty() && studentDataCache.isCacheValid()) {
            _myApplications.value = cachedApplications
        }
        // Note: Don't load cached subjects here - they need to be filtered first
        // Filtering will happen in loadAvailableSubjects()
    }

    fun loadAvailableSubjects(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            // Check cache first
            val cachedSubjects = studentDataCache.cachedSubjects.value
            
            // Only show loading if no cached data or cache is invalid
            if (forceRefresh || !studentDataCache.isCacheValid() || cachedSubjects.isEmpty()) {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }
            
            try {
                // Get current user to filter by year level and course
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        // Load all year levels first (needed for filtering)
                        val allYearLevelsResult = yearLevelRepository.getAllYearLevels()
                        allYearLevelsResult.onSuccess { allYearLevels ->
                            // Create a map of yearLevelId -> level number
                            val yearLevelIdToLevelMap = allYearLevels.associateBy({ it.id }, { it.level })
                            
                            // Get student's year level number
                            val studentYearLevelNumber = user.yearLevelId?.let { yearLevelIdToLevelMap[it] }
                            
                            // Helper function to filter subjects
                            fun filterSubjectsList(subjects: List<Subject>): List<Subject> {
                                return subjects.filter { subject ->
                                    val subjectLevel = yearLevelIdToLevelMap[subject.yearLevelId] ?: 0
                                    val isActive = subject.active
                                    val matchesYearLevelById = user.yearLevelId == null || subject.yearLevelId == user.yearLevelId
                                    val matchesYearLevelByNumber = studentYearLevelNumber != null && subjectLevel == studentYearLevelNumber
                                    val isMinor = subject.subjectType == com.smartacademictracker.data.model.SubjectType.MINOR
                                    val matchesCourse = user.courseId == null || subject.courseId == user.courseId
                                    
                                    // For major subjects: match by year level ID and course
                                    // For minor subjects: match by year level NUMBER (allows cross-course matching)
                                    if (isMinor) {
                                        isActive && matchesYearLevelByNumber
                                    } else {
                                        isActive && matchesYearLevelById && matchesCourse
                                    }
                                }
                            }
                            
                            // Use cached data immediately if available and valid, but FILTER it first
                            if (!forceRefresh && studentDataCache.isCacheValid() && cachedSubjects.isNotEmpty()) {
                                val filteredCachedSubjects = filterSubjectsList(cachedSubjects)
                                
                                // Load section assignments to filter out subjects without teachers
                                val studentCourseId = user.courseId
                                val allAssignments = mutableListOf<com.smartacademictracker.data.model.SectionAssignment>()
                                
                                // Load assignments for each filtered subject in parallel
                                coroutineScope {
                                    val assignmentDeferreds = filteredCachedSubjects.map { subject ->
                                        async {
                                            val isMinor = subject.subjectType == com.smartacademictracker.data.model.SubjectType.MINOR
                                            val result = if (isMinor) {
                                                sectionAssignmentRepository.getSectionAssignmentsBySubject(subject.id)
                                            } else {
                                                if (studentCourseId != null) {
                                                    sectionAssignmentRepository.getSectionAssignmentsBySubjectAndCourse(subject.id, studentCourseId)
                                                } else {
                                                    return@async emptyList()
                                                }
                                            }
                                            result.getOrNull() ?: emptyList()
                                        }
                                    }
                                    
                                    // Wait for all assignments to load
                                    val assignmentResults = assignmentDeferreds.map { it.await() }
                                    allAssignments.addAll(assignmentResults.flatten())
                                }
                                
                                // Filter subjects to only include those with at least one active teacher assignment
                                val subjectsWithTeachers = filteredCachedSubjects.filter { subject ->
                                    allAssignments.any { assignment ->
                                        assignment.subjectId == subject.id &&
                                        assignment.teacherId.isNotEmpty() &&
                                        assignment.status == com.smartacademictracker.data.model.AssignmentStatus.ACTIVE
                                    }
                                }
                                
                                _availableSubjects.value = subjectsWithTeachers
                                _selectedYearLevel.value = user.yearLevelName ?: ""
                                _selectedCourse.value = user.courseCode ?: ""
                                _uiState.value = _uiState.value.copy(isLoading = false)
                                
                                // Update canApplyForSubject map after loading subjects
                                updateCanApplyForSubjects(user.id)
                            }
                            
                            // Load all active subjects in background
                            val subjectsResult = subjectRepository.getAllSubjects()
                            subjectsResult.onSuccess { subjectsList ->
                                // Update cache with all subjects (unfiltered)
                                studentDataCache.updateSubjects(subjectsList)
                                
                                // Filter subjects based on user's year level and course
                                val filteredSubjects = filterSubjectsList(subjectsList)
                                
                                // Load section assignments to filter out subjects without teachers
                                val studentCourseId = user.courseId
                                val allAssignments = mutableListOf<com.smartacademictracker.data.model.SectionAssignment>()
                                
                                // Load assignments for each filtered subject in parallel
                                coroutineScope {
                                    val assignmentDeferreds = filteredSubjects.map { subject ->
                                        async {
                                            val isMinor = subject.subjectType == com.smartacademictracker.data.model.SubjectType.MINOR
                                            val result = if (isMinor) {
                                                sectionAssignmentRepository.getSectionAssignmentsBySubject(subject.id)
                                            } else {
                                                if (studentCourseId != null) {
                                                    sectionAssignmentRepository.getSectionAssignmentsBySubjectAndCourse(subject.id, studentCourseId)
                                                } else {
                                                    return@async emptyList()
                                                }
                                            }
                                            result.getOrNull() ?: emptyList()
                                        }
                                    }
                                    
                                    // Wait for all assignments to load
                                    val assignmentResults = assignmentDeferreds.map { it.await() }
                                    allAssignments.addAll(assignmentResults.flatten())
                                }
                                
                                // Filter subjects to only include those with at least one active teacher assignment
                                val subjectsWithTeachers = filteredSubjects.filter { subject ->
                                    allAssignments.any { assignment ->
                                        assignment.subjectId == subject.id &&
                                        assignment.teacherId.isNotEmpty() &&
                                        assignment.status == com.smartacademictracker.data.model.AssignmentStatus.ACTIVE
                                    }
                                }
                                
                                _availableSubjects.value = subjectsWithTeachers
                                _selectedYearLevel.value = user.yearLevelName ?: ""
                                _selectedCourse.value = user.courseCode ?: ""
                                _uiState.value = _uiState.value.copy(isLoading = false)
                                
                                // Update canApplyForSubject map after loading subjects
                                updateCanApplyForSubjects(user.id)
                            }.onFailure { exception ->
                                if (!studentDataCache.isCacheValid()) {
                                    _uiState.value = _uiState.value.copy(
                                        isLoading = false,
                                        error = exception.message ?: "Failed to load subjects"
                                    )
                                }
                            }
                        }.onFailure { exception ->
                            if (!studentDataCache.isCacheValid()) {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = exception.message ?: "Failed to load year levels"
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
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load user data"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load subjects"
                )
            }
        }
    }

    fun loadMyApplications(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            // Check cache first
            val cachedApplications = studentDataCache.cachedStudentApplications.value
            
            // Use cached data immediately if available and valid
            if (!forceRefresh && studentDataCache.isCacheValid() && cachedApplications.isNotEmpty()) {
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        _myApplications.value = cachedApplications.filter { it.studentId == user.id }
                        // Update canApplyForSubject map
                        updateCanApplyForSubjects(user.id)
                    }
                }
            }
            
            try {
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        val applicationsResult = studentApplicationRepository.getApplicationsByStudent(user.id)
                        applicationsResult.onSuccess { applicationsList ->
                            _myApplications.value = applicationsList
                            studentDataCache.updateStudentApplications(applicationsList)
                            // Update canApplyForSubject map after loading applications
                            updateCanApplyForSubjects(user.id)
                        }.onFailure { exception ->
                            if (!studentDataCache.isCacheValid()) {
                                _uiState.value = _uiState.value.copy(error = "Failed to load applications: ${exception.message}")
                            }
                        }
                    }
                }.onFailure { exception ->
                    if (!studentDataCache.isCacheValid()) {
                        _uiState.value = _uiState.value.copy(error = "Failed to get user: ${exception.message}")
                    }
                }
            } catch (e: Exception) {
                if (!studentDataCache.isCacheValid()) {
                    _uiState.value = _uiState.value.copy(error = "Exception loading applications: ${e.message}")
                }
            }
        }
    }

    /**
     * Update the canApplyForSubject map by checking enrollment status for each subject
     * Checks all available subjects, not just those with applications
     */
    private suspend fun updateCanApplyForSubjects(studentId: String) {
        val canApplyMap = mutableMapOf<String, Boolean>()
        
        // Get all subject IDs from available subjects
        val allSubjectIds = _availableSubjects.value.map { it.id }.distinct()
        
        // Check each subject
        for (subjectId in allSubjectIds) {
            val applications = _myApplications.value.filter { it.subjectId == subjectId }
            
            // Check if there's a PENDING application - can't apply
            val hasPending = applications.any { it.status == StudentApplicationStatus.PENDING }
            if (hasPending) {
                canApplyMap[subjectId] = false
                continue
            }
            
            // Check if there's an APPROVED application - need to check enrollment
            val hasApproved = applications.any { it.status == StudentApplicationStatus.APPROVED }
            if (hasApproved) {
                // Check enrollment status
                val enrollmentsResult = studentEnrollmentRepository.getStudentEnrollmentsBySubject(studentId, subjectId)
                val hasActiveEnrollment = if (enrollmentsResult.isSuccess) {
                    val enrollments = enrollmentsResult.getOrNull().orEmpty()
                    enrollments.any { 
                        it.status == com.smartacademictracker.data.model.EnrollmentStatus.ACTIVE 
                    }
                } else {
                    false
                }
                // Can apply if there's APPROVED application but NO active enrollment (was KICKED/DROPPED)
                canApplyMap[subjectId] = !hasActiveEnrollment
            } else {
                // No PENDING or APPROVED application - can apply
                canApplyMap[subjectId] = true
            }
        }
        
        _canApplyForSubject.value = canApplyMap
    }

    fun applyForSubject(subjectId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                applyingSubjects = _uiState.value.applyingSubjects + subjectId,
                error = null
            )
            
            try {
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        // IMPORTANT: Check enrollment status first
                        // If student was KICKED or DROPPED, allow reapplication even if there's an APPROVED application
                        // Check ALL enrollments for this student in this subject (including KICKED/DROPPED)
                        val enrollmentsResult = studentEnrollmentRepository.getStudentEnrollmentsBySubject(user.id, subjectId)
                        val hasActiveEnrollment = if (enrollmentsResult.isSuccess) {
                            val enrollments = enrollmentsResult.getOrNull().orEmpty()
                            enrollments.any { 
                                it.status == com.smartacademictracker.data.model.EnrollmentStatus.ACTIVE 
                            }
                        } else {
                            false
                        }
                        
                        if (hasActiveEnrollment) {
                            _uiState.value = _uiState.value.copy(
                                applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                                error = "You are already enrolled in this subject"
                            )
                            return@onSuccess
                        }
                        
                        // Check for PENDING applications - always block these
                        val pendingApplication = _myApplications.value.find { 
                            it.subjectId == subjectId && 
                            it.status == StudentApplicationStatus.PENDING 
                        }
                        if (pendingApplication != null) {
                            _uiState.value = _uiState.value.copy(
                                applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                                error = "You already have a pending application for this subject"
                            )
                            return@onSuccess
                        }
                        
                        // Check for APPROVED applications - only block if student has ACTIVE enrollment
                        // If student was KICKED or DROPPED, allow reapplication
                        val approvedApplication = _myApplications.value.find { 
                            it.subjectId == subjectId && 
                            it.status == StudentApplicationStatus.APPROVED 
                        }
                        if (approvedApplication != null && hasActiveEnrollment) {
                            _uiState.value = _uiState.value.copy(
                                applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                                error = "You are already enrolled in this subject"
                            )
                            return@onSuccess
                        }
                        // If there's an APPROVED application but NO active enrollment,
                        // the student was likely KICKED or DROPPED - allow reapplication
                        
                        // Proceed with application creation
                        // Get subject details
                        val subjectResult = subjectRepository.getSubjectById(subjectId)
                                subjectResult.onSuccess { subject ->
                                    // Validate that the subject has an assigned teacher
                                    if (subject.teacherId == null || subject.teacherId.isEmpty()) {
                                        _uiState.value = _uiState.value.copy(
                                            applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                                            error = "This subject does not have an assigned teacher yet. Please wait for a teacher to be assigned before applying."
                                        )
                                        return@onSuccess
                                    }
                                    
                                    // Create application
                                    val application = StudentApplication(
                                        studentId = user.id,
                                        studentName = "${user.firstName} ${user.lastName}",
                                        studentEmail = user.email,
                                        subjectId = subject.id,
                                        subjectName = subject.name,
                                        subjectCode = subject.code,
                                        applicationReason = "I would like to enroll in this subject to further my studies in ${subject.courseName}.",
                                        status = StudentApplicationStatus.PENDING,
                                        yearLevelId = user.yearLevelId ?: "",
                                        courseId = user.courseId ?: "",
                                        yearLevelName = user.yearLevelName ?: "",
                                        courseName = user.courseName ?: "",
                                        courseCode = user.courseCode ?: ""
                                    )

                                    val createResult = studentApplicationRepository.createApplication(application)
                                    createResult.onSuccess { createdApplication ->
                                        // Notify teacher about the new student application
                                        notifyTeacherOfStudentApplication(createdApplication, subject)
                                        
                                        _uiState.value = _uiState.value.copy(
                                            applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                                            isApplicationSuccess = true
                                        )
                                        
                                        // Add a small delay to ensure the application is saved before reloading
                                        kotlinx.coroutines.delay(500)
                                        
                                        // Reload applications to update UI
                                        loadMyApplications(forceRefresh = true)
                                        
                                        // Update canApplyForSubject map after creating application
                                        updateCanApplyForSubjects(user.id)
                                    }.onFailure { exception ->
                                        _uiState.value = _uiState.value.copy(
                                            applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                                            error = exception.message ?: "Failed to create application"
                                        )
                                    }
                                }.onFailure { exception ->
                                    _uiState.value = _uiState.value.copy(
                                        applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                                        error = exception.message ?: "Failed to get subject details"
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

    fun setSelectedYearLevel(yearLevel: String) {
        _selectedYearLevel.value = yearLevel
        // Filter subjects based on new year level
        filterSubjects()
    }

    fun setSelectedCourse(course: String) {
        _selectedCourse.value = course
        // Filter subjects based on new course
        filterSubjects()
    }

    private fun filterSubjects() {
        viewModelScope.launch {
            try {
                // Check cache first
                val cachedSubjects = studentDataCache.cachedSubjects.value
                val subjectsList = if (cachedSubjects.isNotEmpty() && studentDataCache.isCacheValid()) {
                    cachedSubjects
                } else {
                    val subjectsResult = subjectRepository.getAllSubjects()
                    subjectsResult.getOrElse { emptyList() }
                }
                
                if (subjectsList.isNotEmpty()) {
                    // Filter subjects based on selected year level and course
                    // - Major subjects: must match both year level AND course
                    // - Minor subjects: must match year level (regardless of course)
                    val filteredSubjects = subjectsList.filter { subject ->
                        subject.active &&
                        (selectedYearLevel.value.isEmpty() || subject.yearLevelName == selectedYearLevel.value) &&
                        (subject.subjectType == com.smartacademictracker.data.model.SubjectType.MINOR || 
                         selectedCourse.value.isEmpty() || 
                         subject.courseCode == selectedCourse.value)
                    }
                    _availableSubjects.value = filteredSubjects
                }
            } catch (e: Exception) {
                // Silently fail - use cached data if available
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }
    
    fun clearApplicationSuccess() {
        _uiState.value = _uiState.value.copy(isApplicationSuccess = false)
    }
    
    fun refreshData() {
        loadAvailableSubjects(forceRefresh = true)
        loadMyApplications(forceRefresh = true)
    }
    
    fun cancelApplication(applicationId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = studentApplicationRepository.deleteApplication(applicationId)
                result.onSuccess {
                    // Reload applications to update UI
                    loadMyApplications(forceRefresh = true)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Application cancelled successfully"
                    )
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
    
    /**
     * Notify teacher when a student applies for their subject
     */
    private suspend fun notifyTeacherOfStudentApplication(application: StudentApplication, subject: Subject) {
        try {
            val teacherId = subject.teacherId
            if (teacherId != null && teacherId.isNotEmpty()) {
                notificationSenderService.sendNotification(
                    userId = teacherId,
                    type = com.smartacademictracker.data.model.NotificationType.STUDENT_APPLICATION_SUBMITTED,
                    variables = mapOf(
                        "studentName" to application.studentName,
                        "subjectName" to application.subjectName,
                        "applicationId" to application.id
                    ),
                    priority = com.smartacademictracker.data.model.NotificationPriority.NORMAL
                )
            }
        } catch (e: Exception) {
            // Silently fail notification
        }
    }
}

data class StudentSubjectApplicationUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val applyingSubjects: Set<String> = emptySet(),
    val isApplicationSuccess: Boolean = false,
    val successMessage: String? = null
)
