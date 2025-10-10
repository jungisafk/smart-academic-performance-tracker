package com.smartacademictracker.presentation.teacher

import android.util.Log
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val notificationSenderService: com.smartacademictracker.data.notification.NotificationSenderService
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

    fun loadSubjects() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Get current user
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        Log.d("TeacherSubjects", "Loading subjects for teacher: ${user.id}")
                        
                        // Load ALL subjects first (like admin side does)
                        val allSubjectsResult = subjectRepository.getAllSubjects()
                        allSubjectsResult.onSuccess { allSubjects ->
                            Log.d("TeacherSubjects", "Loaded ${allSubjects.size} total subjects")
                            allSubjects.forEach { subject ->
                                Log.d("TeacherSubjects", "Subject - ID: ${subject.id}, Name: ${subject.name}, Code: ${subject.code}, TeacherId: ${subject.teacherId}")
                                Log.d("TeacherSubjects", "Subject ${subject.name} - Sections: ${subject.sections}")
                            }
                            
                            // Filter available subjects (subjects with available sections)
                            filterAvailableSubjects(user.id, user.departmentCourseId, allSubjects)

                            // Load teacher's assigned subjects (from section assignments)
                            loadMySubjects(user.id, allSubjects)

                            // Load teacher's applied subjects
                            loadAppliedSubjects(user.id)

                            _uiState.value = _uiState.value.copy(isLoading = false)
                        }.onFailure { exception ->
                            Log.d("TeacherSubjects", "Failed to load all subjects: ${exception.message}")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to load subjects"
                            )
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

    fun applyForSubject(subjectId: String) {
        viewModelScope.launch {
            Log.d("TeacherSubjects", "Applying for subject: $subjectId")
            _uiState.value = _uiState.value.copy(
                applyingSubjects = _uiState.value.applyingSubjects + subjectId,
                error = null
            )
            
            try {
                // Get current user
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        Log.d("TeacherSubjects", "User found: ${user.email}")
                        
                        // Check if teacher has an active application (PENDING or APPROVED) for this subject
                        // Allow reapplication if previous application was REJECTED
                        val hasActiveResult = teacherApplicationRepository.hasTeacherActiveApplication(user.id, subjectId)
                        hasActiveResult.onSuccess { hasActive ->
                            if (hasActive) {
                                Log.d("TeacherSubjects", "Teacher has an active application for this subject")
                                _uiState.value = _uiState.value.copy(
                                    applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                                    error = "You have already applied for this subject. Please check your applications."
                                )
                                return@onSuccess
                            }
                            
                            // Get subject details
                            val subjectResult = subjectRepository.getSubjectById(subjectId)
                            subjectResult.onSuccess { subject ->
                                Log.d("TeacherSubjects", "Subject found: ${subject.name}, Type: ${subject.subjectType}, CourseId: ${subject.courseId}")
                                
                                // Validate that teacher can apply for this subject based on department and type
                                val canApply = when (subject.subjectType) {
                                    com.smartacademictracker.data.model.SubjectType.MAJOR -> {
                                        // MAJOR subjects: only teachers of the same course/department can apply
                                        user.departmentCourseId != null && subject.courseId == user.departmentCourseId
                                    }
                                    com.smartacademictracker.data.model.SubjectType.MINOR -> {
                                        // MINOR subjects: any teacher can apply
                                        true
                                    }
                                }
                                
                                if (!canApply) {
                                    Log.d("TeacherSubjects", "Teacher cannot apply for this subject - department mismatch")
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

                                Log.d("TeacherSubjects", "Creating application for: ${application.subjectName}")
                                // Submit application
                                val applicationResult = teacherApplicationRepository.createApplication(application)
                                applicationResult.onSuccess {
                                    Log.d("TeacherSubjects", "Application created successfully!")
                                    
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
                                    Log.d("TeacherSubjects", "Application creation failed: ${exception.message}")
                                    _uiState.value = _uiState.value.copy(
                                        applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                                        error = exception.message ?: "Failed to submit application"
                                    )
                                }
                            }.onFailure { exception ->
                                Log.d("TeacherSubjects", "Subject not found: ${exception.message}")
                                _uiState.value = _uiState.value.copy(
                                    applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                                    error = exception.message ?: "Subject not found"
                                )
                            }
                        }.onFailure { exception ->
                            Log.d("TeacherSubjects", "Failed to check application status: ${exception.message}")
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
            Log.d("TeacherSubjects", "Cancelling application: $applicationId")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                teacherApplicationRepository.cancelApplication(applicationId).onSuccess {
                    Log.d("TeacherSubjects", "Application cancelled successfully")
                    // Reload all data to reflect the change
                    loadSubjects()
                }.onFailure { exception ->
                    Log.d("TeacherSubjects", "Failed to cancel application: ${exception.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to cancel application"
                    )
                }
            } catch (e: Exception) {
                Log.d("TeacherSubjects", "Exception cancelling application: ${e.message}")
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
                                val filteredEnrollments = enrollments.filter { 
                                    it.sectionName in sectionNames && it.status.name == "ACTIVE"
                                }
                                Log.d("TeacherSubjects", "Found ${filteredEnrollments.size} students in sections $sectionNames for subject $subjectId")
                                return filteredEnrollments.size
                            }.onFailure { exception ->
                                Log.d("TeacherSubjects", "Error getting student enrollments: ${exception.message}")
                                return 0
                            }
                        } else {
                            Log.d("TeacherSubjects", "No section assignments found for teacher ${user.id} in subject $subjectId")
                            return 0
                        }
                    }.onFailure { exception ->
                        Log.d("TeacherSubjects", "Error getting section assignments: ${exception.message}")
                        return 0
                    }
                }
            }.onFailure { exception ->
                Log.d("TeacherSubjects", "Error getting current user: ${exception.message}")
                return 0
            }
            0
        } catch (e: Exception) {
            Log.d("TeacherSubjects", "Error getting student count for subject $subjectId: ${e.message}")
            0
        }
    }

    suspend fun getSectionAvailability(subjectId: String): Map<String, Boolean> {
        return try {
            Log.d("TeacherSubjects", "Getting section availability for subject $subjectId")
            val assignmentsResult = sectionAssignmentRepository.getSectionAssignmentsBySubject(subjectId)
            assignmentsResult.onSuccess { assignments ->
                Log.d("TeacherSubjects", "Found ${assignments.size} assignments for subject $subjectId")
                assignments.forEach { assignment ->
                    Log.d("TeacherSubjects", "Assignment - Section: ${assignment.sectionName}, Teacher: ${assignment.teacherId}")
                }
                
                // Get the subject to know all its sections
                val subjectResult = subjectRepository.getSubjectById(subjectId)
                subjectResult.onSuccess { subject ->
                    val assignedSections = assignments.map { it.sectionName }.toSet()
                    val allSections = subject.sections.toSet()
                    
                    Log.d("TeacherSubjects", "Subject $subjectId - All sections: $allSections")
                    Log.d("TeacherSubjects", "Subject $subjectId - Assigned sections: $assignedSections")
                    
                    // Return map of section name to availability (true = available, false = assigned)
                    val availability = allSections.associateWith { sectionName ->
                        !assignedSections.contains(sectionName)
                    }
                    Log.d("TeacherSubjects", "Subject $subjectId - Section availability: $availability")
                    return availability
                }.onFailure { exception ->
                    Log.d("TeacherSubjects", "Failed to get subject $subjectId: ${exception.message}")
                    return emptyMap()
                }
            }.onFailure { exception ->
                Log.d("TeacherSubjects", "Failed to get assignments for subject $subjectId: ${exception.message}")
                return emptyMap()
            }
            emptyMap()
        } catch (e: Exception) {
            Log.d("TeacherSubjects", "Error getting section availability for subject $subjectId: ${e.message}")
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
            Log.d("TeacherSubjects", "Error getting assigned sections for subject $subjectId: ${e.message}")
            emptyList()
        }
    }

    private suspend fun loadMySubjects(teacherId: String, allSubjects: List<Subject>) {
        try {
            // Get section assignments for this teacher
            val assignmentsResult = sectionAssignmentRepository.getSectionAssignmentsByTeacher(teacherId)
            assignmentsResult.onSuccess { assignments ->
                Log.d("TeacherSubjects", "Found ${assignments.size} section assignments for teacher")
                
                // Get unique subject IDs from assignments
                val assignedSubjectIds = assignments.map { it.subjectId }.toSet()
                Log.d("TeacherSubjects", "Assigned subject IDs: $assignedSubjectIds")
                
                // Filter subjects that teacher is assigned to
                val mySubjects = allSubjects.filter { it.id in assignedSubjectIds }
                _mySubjects.value = mySubjects
                Log.d("TeacherSubjects", "Found ${mySubjects.size} subjects assigned to teacher")
                
                mySubjects.forEach { subject ->
                    Log.d("TeacherSubjects", "My Subject - ID: ${subject.id}, Name: ${subject.name}, Code: ${subject.code}")
                }
            }.onFailure { exception ->
                Log.d("TeacherSubjects", "Failed to load section assignments: ${exception.message}")
                _mySubjects.value = emptyList()
            }
        } catch (e: Exception) {
            Log.d("TeacherSubjects", "Error loading my subjects: ${e.message}")
            _mySubjects.value = emptyList()
        }
    }

    private suspend fun filterAvailableSubjects(teacherId: String, teacherDepartmentCourseId: String?, subjects: List<Subject>) {
        try {
            Log.d("TeacherSubjects", "Filtering ${subjects.size} subjects for teacher $teacherId (Department: $teacherDepartmentCourseId)")
            subjects.forEach { subject ->
                Log.d("TeacherSubjects", "Available Subject - ID: ${subject.id}, Name: ${subject.name}, Code: ${subject.code}, CourseId: ${subject.courseId}, Type: ${subject.subjectType}, TeacherId: ${subject.teacherId}")
            }
            
            // Get teacher's applications
            val applicationsResult = teacherApplicationRepository.getApplicationsByTeacher(teacherId)
            applicationsResult.onSuccess { applications ->
                Log.d("TeacherSubjects", "Found ${applications.size} applications for teacher")
                applications.forEach { app ->
                    Log.d("TeacherSubjects", "Application - SubjectId: ${app.subjectId}, Status: ${app.status}")
                }
                
                // Get subject IDs that teacher has active applications for (PENDING or APPROVED)
                // Exclude REJECTED applications to allow reapplication
                val appliedSubjectIds = applications
                    .filter { it.status == ApplicationStatus.PENDING || it.status == ApplicationStatus.APPROVED }
                    .map { it.subjectId }
                    .toSet()
                
                Log.d("TeacherSubjects", "Applied subject IDs: $appliedSubjectIds")
                
                // Filter subjects based on department and subject type rules
                val availableSubjects = mutableListOf<Subject>()
                for (subject in subjects) {
                    val notApplied = subject.id !in appliedSubjectIds
                    Log.d("TeacherSubjects", "Checking subject ${subject.name} (${subject.id}) - Not Applied: $notApplied")
                    
                    if (notApplied) {
                        // Check department and subject type visibility rules
                        val isVisible = when (subject.subjectType) {
                            com.smartacademictracker.data.model.SubjectType.MAJOR -> {
                                // MAJOR subjects: only visible to teachers of the same course/department
                                teacherDepartmentCourseId != null && subject.courseId == teacherDepartmentCourseId
                            }
                            com.smartacademictracker.data.model.SubjectType.MINOR -> {
                                // MINOR subjects: visible to all teachers
                                true
                            }
                        }
                        
                        Log.d("TeacherSubjects", "Subject ${subject.name} - Type: ${subject.subjectType}, IsVisible: $isVisible")
                        
                        if (isVisible) {
                            // Check if this subject has any available sections
                            val sectionAvailability = getSectionAvailability(subject.id)
                            val hasAvailableSections = sectionAvailability.values.any { it }
                            
                            Log.d("TeacherSubjects", "Subject ${subject.name} - Section Availability: $sectionAvailability")
                            Log.d("TeacherSubjects", "Subject ${subject.name} - Has Available Sections: $hasAvailableSections")
                            
                            if (hasAvailableSections) {
                                availableSubjects.add(subject)
                                Log.d("TeacherSubjects", "Added ${subject.name} to available subjects")
                            } else {
                                Log.d("TeacherSubjects", "${subject.name} has no available sections, skipping")
                            }
                        } else {
                            Log.d("TeacherSubjects", "${subject.name} is not visible to this teacher (MAJOR subject from different department), skipping")
                        }
                    } else {
                        Log.d("TeacherSubjects", "${subject.name} already applied, skipping")
                    }
                }
                _availableSubjects.value = availableSubjects
                
                Log.d("TeacherSubjects", "Final available subjects: ${availableSubjects.size}")
                availableSubjects.forEach { subject ->
                    Log.d("TeacherSubjects", "Final Available - ID: ${subject.id}, Name: ${subject.name}, Code: ${subject.code}, Type: ${subject.subjectType}")
                }
            }.onFailure { exception ->
                Log.d("TeacherSubjects", "Failed to load applications: ${exception.message}")
                // If we can't load applications, filter by department/type rules only
                val availableSubjects = subjects.filter { subject ->
                    when (subject.subjectType) {
                        com.smartacademictracker.data.model.SubjectType.MAJOR -> {
                            teacherDepartmentCourseId != null && subject.courseId == teacherDepartmentCourseId
                        }
                        com.smartacademictracker.data.model.SubjectType.MINOR -> true
                    } && subject.teacherId == null
                }
                _availableSubjects.value = availableSubjects
                Log.d("TeacherSubjects", "Fallback: showing ${availableSubjects.size} unassigned subjects")
            }
        } catch (e: Exception) {
            Log.d("TeacherSubjects", "Exception filtering subjects: ${e.message}")
            // If there's an exception, filter by department/type rules only
            val availableSubjects = subjects.filter { subject ->
                when (subject.subjectType) {
                    com.smartacademictracker.data.model.SubjectType.MAJOR -> {
                        teacherDepartmentCourseId != null && subject.courseId == teacherDepartmentCourseId
                    }
                    com.smartacademictracker.data.model.SubjectType.MINOR -> true
                } && subject.teacherId == null
            }
            _availableSubjects.value = availableSubjects
            Log.d("TeacherSubjects", "Exception fallback: showing ${availableSubjects.size} unassigned subjects")
        }
    }

    private suspend fun loadAppliedSubjects(teacherId: String) {
        try {
            // Get teacher's applications
            val applicationsResult = teacherApplicationRepository.getApplicationsByTeacher(teacherId)
            applicationsResult.onSuccess { applications ->
                Log.d("TeacherSubjects", "Found ${applications.size} applications for teacher")
                
                // Store all applications
                _applications.value = applications
                
                // Store approved applications separately
                val approvedApps = applications.filter { it.status == ApplicationStatus.APPROVED }
                _approvedApplications.value = approvedApps
                Log.d("TeacherSubjects", "Found ${approvedApps.size} approved applications")
                
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
                
                Log.d("TeacherSubjects", "Applied subjects loaded: ${appliedSubjectsList.size}")
            }.onFailure { exception ->
                Log.d("TeacherSubjects", "Failed to load applied subjects: ${exception.message}")
                _appliedSubjects.value = emptyList()
            }
        } catch (e: Exception) {
            Log.d("TeacherSubjects", "Exception loading applied subjects: ${e.message}")
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
            Log.e("TeacherSubjects", "Failed to notify admins of teacher application: ${e.message}")
        }
    }
}

data class TeacherSubjectsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val applyingSubjects: Set<String> = emptySet()
)
