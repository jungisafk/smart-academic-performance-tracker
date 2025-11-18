package com.smartacademictracker.presentation.teacher

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.StudentApplication
import com.smartacademictracker.data.model.StudentApplicationStatus
import com.smartacademictracker.data.repository.StudentApplicationRepository
import com.smartacademictracker.data.repository.SubjectRepository
import com.smartacademictracker.data.repository.UserRepository
import com.smartacademictracker.data.repository.StudentEnrollmentRepository
import com.smartacademictracker.data.repository.SectionAssignmentRepository
import com.smartacademictracker.data.model.StudentEnrollment
import com.smartacademictracker.data.model.EnrollmentStatus
import com.smartacademictracker.data.manager.RealtimeDataManager
import com.smartacademictracker.data.manager.TeacherDataCache
import com.smartacademictracker.data.notification.NotificationSenderService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeacherStudentApplicationsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val studentApplicationRepository: StudentApplicationRepository,
    private val subjectApplicationRepository: com.smartacademictracker.data.repository.SubjectApplicationRepository,
    private val subjectRepository: SubjectRepository,
    private val studentEnrollmentRepository: StudentEnrollmentRepository,
    private val sectionAssignmentRepository: com.smartacademictracker.data.repository.SectionAssignmentRepository,
    private val realtimeDataManager: RealtimeDataManager,
    private val teacherDataCache: TeacherDataCache,
    private val notificationSenderService: NotificationSenderService
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherStudentApplicationsUiState())
    val uiState: StateFlow<TeacherStudentApplicationsUiState> = _uiState.asStateFlow()

    private val _applications = MutableStateFlow<List<StudentApplication>>(emptyList())
    val applications: StateFlow<List<StudentApplication>> = _applications.asStateFlow()

    // Tracks which backing collection each displayed application came from
    private val applicationSourceById = mutableMapOf<String, ApplicationSource>()

    private enum class ApplicationSource { LEGACY_STUDENT, CURRENT_SUBJECT }

    init {
        // Load cached data immediately if available
        val cachedApplications = teacherDataCache.cachedStudentApplications.value
        if (cachedApplications.isNotEmpty() && teacherDataCache.isCacheValid()) {
            _applications.value = cachedApplications
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
        
        // Set up real-time listeners
        setupRealtimeListeners()
    }

    private fun setupRealtimeListeners() {
        viewModelScope.launch {
            try {
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        // Get subjects that this teacher teaches
                        val subjectsResult = subjectRepository.getSubjectsByTeacher(user.id)
                        subjectsResult.onSuccess { subjects ->
                            val subjectIds = subjects.map { it.id }
                            
                            if (subjectIds.isEmpty()) {
                                _applications.value = emptyList()
                                _uiState.value = _uiState.value.copy(isLoading = false)
                                return@onSuccess
                            }
                            
                            // Combine real-time flows from both collections
                            val legacyFlow = studentApplicationRepository.getApplicationsForTeacherSubjectsFlow(user.id, subjectIds)
                            val currentFlow = subjectApplicationRepository.getApplicationsForTeacherSubjectsFlow(subjectIds)
                            
                            combine(legacyFlow, currentFlow) { legacyList, currentList ->
                                // Update source mapping
                                applicationSourceById.clear()
                                legacyList.forEach { applicationSourceById[it.id] = ApplicationSource.LEGACY_STUDENT }
                                
                                // Convert SubjectApplication to StudentApplication
                                val convertedCurrentList = currentList.map { sa ->
                                    val studentApp = com.smartacademictracker.data.model.StudentApplication(
                                        id = sa.id,
                                        studentId = sa.studentId,
                                        studentName = sa.studentName,
                                        subjectId = sa.subjectId,
                                        subjectName = sa.subjectName,
                                        subjectCode = "",
                                        status = when (sa.status) {
                                            com.smartacademictracker.data.model.ApplicationStatus.PENDING -> com.smartacademictracker.data.model.StudentApplicationStatus.PENDING
                                            com.smartacademictracker.data.model.ApplicationStatus.APPROVED -> com.smartacademictracker.data.model.StudentApplicationStatus.APPROVED
                                            com.smartacademictracker.data.model.ApplicationStatus.REJECTED -> com.smartacademictracker.data.model.StudentApplicationStatus.REJECTED
                                            com.smartacademictracker.data.model.ApplicationStatus.WITHDRAWN -> com.smartacademictracker.data.model.StudentApplicationStatus.REJECTED
                                        },
                                        appliedAt = sa.appliedDate,
                                        teacherComments = sa.remarks
                                    )
                                    applicationSourceById[studentApp.id] = ApplicationSource.CURRENT_SUBJECT
                                    studentApp
                                }
                                
                                // Combine and sort
                                (legacyList + convertedCurrentList).sortedByDescending { it.appliedAt }
                            }.collect { combinedApplications ->
                                _applications.value = combinedApplications
                                teacherDataCache.updateStudentApplications(combinedApplications)
                                _uiState.value = _uiState.value.copy(isLoading = false)
                            }
                        }.onFailure { exception ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to load teacher subjects"
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
                    error = e.message ?: "Failed to setup real-time listeners"
                )
            }
        }
    }

    fun loadApplications(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            // Load cached data first if available and not forcing refresh
            if (!forceRefresh && teacherDataCache.cachedStudentApplications.value.isNotEmpty() && teacherDataCache.isCacheValid()) {
                _applications.value = teacherDataCache.cachedStudentApplications.value
                _uiState.value = _uiState.value.copy(isLoading = false)
            } else {
                if (teacherDataCache.cachedStudentApplications.value.isEmpty()) {
                    _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                }
            }
            
            try {
                // Get current user (teacher)
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        // Get subjects that this teacher actually teaches (owns)
                        val subjectsResult = subjectRepository.getSubjectsByTeacher(user.id)
                        subjectsResult.onSuccess { subjects ->
                            val subjectIds = subjects.map { it.id }
                            
                            if (subjectIds.isEmpty()) {
                                _applications.value = emptyList()
                                _uiState.value = _uiState.value.copy(isLoading = false)
                                return@onSuccess
                            }
                            
                            // Now get applications for these subjects from both legacy and current collections
                            val legacyResult = studentApplicationRepository.getApplicationsForTeacherSubjects(user.id, subjectIds)
                            val legacyList = legacyResult.getOrElse { emptyList<com.smartacademictracker.data.model.StudentApplication>() }
                            
                            val currentResult = subjectApplicationRepository.getApplicationsForTeacherSubjects(subjectIds)
                            val currentList = currentResult.getOrElse { emptyList<com.smartacademictracker.data.model.SubjectApplication>() }.map { sa ->
                                com.smartacademictracker.data.model.StudentApplication(
                                    id = sa.id,
                                    studentId = sa.studentId,
                                    studentName = sa.studentName,
                                    subjectId = sa.subjectId,
                                    subjectName = sa.subjectName,
                                    subjectCode = "",
                                    status = when (sa.status) {
                                        com.smartacademictracker.data.model.ApplicationStatus.PENDING -> com.smartacademictracker.data.model.StudentApplicationStatus.PENDING
                                        com.smartacademictracker.data.model.ApplicationStatus.APPROVED -> com.smartacademictracker.data.model.StudentApplicationStatus.APPROVED
                                        com.smartacademictracker.data.model.ApplicationStatus.REJECTED -> com.smartacademictracker.data.model.StudentApplicationStatus.REJECTED
                                        com.smartacademictracker.data.model.ApplicationStatus.WITHDRAWN -> com.smartacademictracker.data.model.StudentApplicationStatus.REJECTED
                                    },
                                    appliedAt = sa.appliedDate,
                                    teacherComments = sa.remarks
                                )
                            }
                            // Update source mapping
                            applicationSourceById.clear()
                            legacyList.forEach { applicationSourceById[it.id] = ApplicationSource.LEGACY_STUDENT }
                            currentList.forEach { applicationSourceById[it.id] = ApplicationSource.CURRENT_SUBJECT }

                            val combined = (legacyList + currentList).sortedByDescending { it.appliedAt }
                            _applications.value = combined
                            teacherDataCache.updateStudentApplications(combined)
                            _uiState.value = _uiState.value.copy(isLoading = false)
                            
                        }.onFailure { exception ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to load teacher subjects"
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
                    error = e.message ?: "Failed to load applications"
                )
            }
        }
    }

    fun updateApplicationStatus(
        applicationId: String,
        status: StudentApplicationStatus,
        comments: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                processingApplications = _uiState.value.processingApplications + applicationId,
                error = null
            )
            
            try {
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        val source = applicationSourceById[applicationId] ?: ApplicationSource.LEGACY_STUDENT
                        val updateResult = when (source) {
                            ApplicationSource.LEGACY_STUDENT ->
                                studentApplicationRepository.updateApplicationStatus(
                                    applicationId = applicationId,
                                    status = status,
                                    teacherId = user.id,
                                    comments = comments
                                )
                            ApplicationSource.CURRENT_SUBJECT ->
                                {
                                    val mapped = when (status) {
                                        StudentApplicationStatus.PENDING -> com.smartacademictracker.data.model.ApplicationStatus.PENDING
                                        StudentApplicationStatus.APPROVED -> com.smartacademictracker.data.model.ApplicationStatus.APPROVED
                                        StudentApplicationStatus.REJECTED -> com.smartacademictracker.data.model.ApplicationStatus.REJECTED
                                    }
                                    subjectApplicationRepository.updateApplicationStatus(
                                        applicationId = applicationId,
                                        status = mapped,
                                        processedBy = user.id,
                                        remarks = comments
                                    ).map { Unit }
                                }
                        }
                        
                        updateResult.onSuccess {
                            // Send notification based on status
                            val source = applicationSourceById[applicationId] ?: ApplicationSource.LEGACY_STUDENT
                            
                            when (source) {
                                ApplicationSource.LEGACY_STUDENT -> {
                                    val applicationResult = studentApplicationRepository.getApplicationById(applicationId)
                                    applicationResult.onSuccess { app ->
                                        if (app != null) {
                                            // Send notification
                                            notificationSenderService.sendApplicationStatusNotification(
                                                userId = app.studentId,
                                                applicationType = "Subject Application",
                                                status = if (status == StudentApplicationStatus.APPROVED) "approved" else "rejected",
                                                subjectName = app.subjectName,
                                                reason = if (status == StudentApplicationStatus.REJECTED) comments else null
                                            )
                                            
                                            // If approved, create enrollment
                                            if (status == StudentApplicationStatus.APPROVED) {
                                                createStudentEnrollment(app)
                                            }
                                        }
                                    }
                                }
                                ApplicationSource.CURRENT_SUBJECT -> {
                                    val applicationResult = subjectApplicationRepository.getApplicationById(applicationId)
                                    applicationResult.onSuccess { sa ->
                                        if (sa != null) {
                                            // Send notification
                                            notificationSenderService.sendApplicationStatusNotification(
                                                userId = sa.studentId,
                                                applicationType = "Subject Application",
                                                status = if (status == StudentApplicationStatus.APPROVED) "approved" else "rejected",
                                                subjectName = sa.subjectName,
                                                reason = if (status == StudentApplicationStatus.REJECTED) comments else null
                                            )
                                            
                                            // If approved, create enrollment
                                            if (status == StudentApplicationStatus.APPROVED) {
                                                createStudentEnrollmentFromSubjectApplication(sa)
                                            }
                                        }
                                    }
                                }
                            }
                            
                            _uiState.value = _uiState.value.copy(
                                processingApplications = _uiState.value.processingApplications - applicationId
                            )
                            // Real-time listener will automatically update UI, no need to reload
                        }.onFailure { exception ->
                            _uiState.value = _uiState.value.copy(
                                processingApplications = _uiState.value.processingApplications - applicationId,
                                error = exception.message ?: "Failed to update application status"
                            )
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            processingApplications = _uiState.value.processingApplications - applicationId,
                            error = "User not found"
                        )
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        processingApplications = _uiState.value.processingApplications - applicationId,
                        error = exception.message ?: "Failed to get user data"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    processingApplications = _uiState.value.processingApplications - applicationId,
                    error = e.message ?: "Failed to update application status"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun refreshApplications() {
        loadApplications()
    }

    fun approveApplication(applicationId: String) {
        updateApplicationStatus(applicationId, StudentApplicationStatus.APPROVED)
    }

    fun rejectApplication(applicationId: String) {
        updateApplicationStatus(applicationId, StudentApplicationStatus.REJECTED)
    }

    private suspend fun createStudentEnrollment(application: StudentApplication) {
        try {
            // Get current teacher's section assignment for this subject
            val currentUserResult = userRepository.getCurrentUser()
            currentUserResult.onSuccess { teacher ->
                if (teacher == null) {
                    Log.e("TeacherStudentAppsVM", "Teacher is null")
                    return@onSuccess
                }
                
                // Get teacher's section assignments for this subject
                val sectionAssignmentsResult = sectionAssignmentRepository.getSectionAssignmentsByTeacher(teacher.id)
                sectionAssignmentsResult.onSuccess { assignments ->
                    // Find the section assignment for this subject
                    val teacherAssignment = assignments.find { it.subjectId == application.subjectId && it.status == com.smartacademictracker.data.model.AssignmentStatus.ACTIVE }
                    
                    if (teacherAssignment == null) {
                        val errorMsg = "No active section assignment found for this subject. Cannot create enrollment."
                        Log.e("TeacherStudentAppsVM", errorMsg)
                        _uiState.value = _uiState.value.copy(
                            error = errorMsg
                        )
                        return@onSuccess
                    }
                    
                    // Get student information
                    val studentResult = userRepository.getUserById(application.studentId)
                    studentResult.onSuccess { student ->
                        if (student == null) {
                            Log.e("TeacherStudentAppsVM", "Student not found for studentId=${application.studentId}")
                            return@onSuccess
                        }
                        
                        // Get subject information
                        val subjectResult = subjectRepository.getSubjectById(application.subjectId)
                        subjectResult.onSuccess { subject ->
                            if (subject == null) {
                                Log.e("TeacherStudentAppsVM", "Subject not found for subjectId=${application.subjectId}")
                                return@onSuccess
                            }
                            
                            // Check if student is already enrolled to prevent duplicates
                            val isEnrolledResult = studentEnrollmentRepository.isStudentEnrolled(
                                application.studentId,
                                application.subjectId,
                                teacherAssignment.sectionName
                            )
                            
                            val isAlreadyEnrolled = isEnrolledResult.getOrNull() ?: false
                            if (isAlreadyEnrolled) {
                                Log.w("TeacherStudentAppsVM", "Student ${application.studentId} is already enrolled in ${application.subjectId} section ${teacherAssignment.sectionName}")
                                return@onSuccess
                            }
                            
                            // Create enrollment with correct section name and teacher info from section assignment
                            val enrollment = StudentEnrollment(
                                studentId = application.studentId,
                                studentName = application.studentName,
                                studentEmail = student.email ?: "",
                                subjectId = application.subjectId,
                                subjectName = application.subjectName,
                                subjectCode = subject.code ?: "",
                                sectionName = teacherAssignment.sectionName, // Use section name from assignment (e.g., "PROG101A")
                                teacherId = teacher.id,
                                teacherName = "${teacher.firstName} ${teacher.lastName}",
                                teacherEmail = teacher.email,
                                courseId = application.courseId,
                                courseName = application.courseName,
                                yearLevelId = application.yearLevelId,
                                yearLevelName = application.yearLevelName,
                                semester = subject.semester ?: com.smartacademictracker.data.model.Semester.FIRST_SEMESTER,
                                academicYear = subject.academicYear ?: "",
                                academicPeriodId = teacherAssignment.academicPeriodId,
                                enrollmentDate = System.currentTimeMillis(),
                                status = EnrollmentStatus.ACTIVE,
                                enrolledBy = teacher.id,
                                enrolledByName = "${teacher.firstName} ${teacher.lastName}",
                                notes = "Enrolled after application approval",
                                createdAt = System.currentTimeMillis()
                            )

                            val enrollmentResult = studentEnrollmentRepository.enrollStudent(enrollment)
                            enrollmentResult.onSuccess { enrollmentId ->
                                Log.i("TeacherStudentAppsVM", "Successfully created enrollment $enrollmentId for student ${application.studentId} in subject ${application.subjectId}")
                                
                                // Send enrollment notification
                                notificationSenderService.sendStudentEnrolledNotification(
                                    studentId = application.studentId,
                                    subjectName = application.subjectName,
                                    sectionName = enrollment.sectionName,
                                    teacherName = "${teacher.firstName} ${teacher.lastName}"
                                )
                                
                                realtimeDataManager.loadAllData()
                            }.onFailure { exception ->
                                val errorMsg = "Failed to create enrollment: ${exception.message}"
                                Log.e("TeacherStudentAppsVM", errorMsg, exception)
                                // Update UI state to show error to user
                                _uiState.value = _uiState.value.copy(
                                    error = errorMsg
                                )
                            }
                        }.onFailure { exception ->
                            Log.e("TeacherStudentAppsVM", "Failed to get subject: ${exception.message}", exception)
                        }
                    }.onFailure { exception ->
                        Log.e("TeacherStudentAppsVM", "Failed to get student: ${exception.message}", exception)
                    }
                }.onFailure { exception ->
                    Log.e("TeacherStudentAppsVM", "Failed to get section assignments: ${exception.message}", exception)
                }
            }.onFailure { exception ->
                Log.e("TeacherStudentAppsVM", "Failed to get current user: ${exception.message}", exception)
            }
        } catch (e: Exception) {
            Log.e("TeacherStudentAppsVM", "Exception in createStudentEnrollment: ${e.message}", e)
        }
    }

    private suspend fun createStudentEnrollmentFromSubjectApplication(application: com.smartacademictracker.data.model.SubjectApplication) {
        try {
            // Get current teacher's section assignment for this subject
            val currentUserResult = userRepository.getCurrentUser()
            currentUserResult.onSuccess { teacher ->
                if (teacher == null) {
                    Log.e("TeacherStudentAppsVM", "Teacher is null")
                    return@onSuccess
                }
                
                val sectionAssignmentsResult = sectionAssignmentRepository.getSectionAssignmentsByTeacher(teacher.id)
                sectionAssignmentsResult.onSuccess { assignments ->
                    // Verify teacher is assigned to this subject
                    val teacherAssignment = assignments.find { 
                        it.subjectId == application.subjectId && 
                        it.status == com.smartacademictracker.data.model.AssignmentStatus.ACTIVE 
                    }
                    if (teacherAssignment == null) {
                        val errorMsg = "No active section assignment found for this subject. Cannot create enrollment."
                        Log.e("TeacherStudentAppsVM", errorMsg)
                        _uiState.value = _uiState.value.copy(
                            error = errorMsg
                        )
                        return@onSuccess
                    }
                    
                    // Use the section name from the teacher's section assignment (e.g., "PROG101A")
                    // This ensures the enrollment matches the teacher's assigned section name exactly
                    // If the application has a section name, verify it matches the assignment
                    val enrollmentSection = if (application.sectionName.isNotEmpty()) {
                        // Check if application section matches teacher's assigned section
                        // If it's a short name (like "A"), check if teacher's section ends with it (e.g., "PROG101A" ends with "A")
                        if (application.sectionName == teacherAssignment.sectionName || 
                            teacherAssignment.sectionName.endsWith(application.sectionName)) {
                            teacherAssignment.sectionName // Use full section name from assignment
                        } else {
                            teacherAssignment.sectionName // Default to teacher's assigned section
                        }
                    } else {
                        // Fallback to teacher's assigned section if application doesn't specify
                        teacherAssignment.sectionName
                    }
                    
                    // Get student information
                    val studentResult = userRepository.getUserById(application.studentId)
                    studentResult.onSuccess { student ->
                        if (student == null) {
                            Log.e("TeacherStudentAppsVM", "Student not found for studentId=${application.studentId}")
                            return@onSuccess
                        }
                        
                        // Get subject information
                        val subjectResult = subjectRepository.getSubjectById(application.subjectId)
                        subjectResult.onSuccess { subject ->
                            if (subject == null) {
                                Log.e("TeacherStudentAppsVM", "Subject not found for subjectId=${application.subjectId}")
                                return@onSuccess
                            }
                            
                            // Check if student is already enrolled to prevent duplicates
                            val isEnrolledResult = studentEnrollmentRepository.isStudentEnrolled(
                                application.studentId,
                                application.subjectId,
                                enrollmentSection
                            )
                            
                            val isAlreadyEnrolled = isEnrolledResult.getOrNull() ?: false
                            if (isAlreadyEnrolled) {
                                Log.w("TeacherStudentAppsVM", "Student ${application.studentId} is already enrolled in ${application.subjectId} section $enrollmentSection")
                                return@onSuccess
                            }
                            
                            // Create enrollment with the section from the application
                            val enrollment = StudentEnrollment(
                                studentId = application.studentId,
                                studentName = application.studentName,
                                studentEmail = student.email ?: "",
                                subjectId = application.subjectId,
                                subjectName = application.subjectName,
                                subjectCode = subject.code ?: "",
                                sectionName = enrollmentSection, // Use full section name from assignment
                                teacherId = teacher.id,
                                teacherName = "${teacher.firstName} ${teacher.lastName}",
                                teacherEmail = teacher.email,
                                courseId = application.courseId,
                                courseName = application.courseName,
                                yearLevelId = application.yearLevelId,
                                yearLevelName = application.yearLevelName,
                                semester = subject.semester ?: com.smartacademictracker.data.model.Semester.FIRST_SEMESTER,
                                academicYear = subject.academicYear ?: "",
                                academicPeriodId = teacherAssignment.academicPeriodId,
                                enrollmentDate = System.currentTimeMillis(),
                                status = EnrollmentStatus.ACTIVE,
                                enrolledBy = teacher.id,
                                enrolledByName = "${teacher.firstName} ${teacher.lastName}",
                                notes = "Enrolled after application approval",
                                createdAt = System.currentTimeMillis()
                            )

                            val enrollmentResult = studentEnrollmentRepository.enrollStudent(enrollment)
                            enrollmentResult.onSuccess { enrollmentId ->
                                Log.i("TeacherStudentAppsVM", "Successfully created enrollment $enrollmentId for student ${application.studentId} in subject ${application.subjectId}")
                                
                                // Send enrollment notification
                                notificationSenderService.sendStudentEnrolledNotification(
                                    studentId = application.studentId,
                                    subjectName = application.subjectName,
                                    sectionName = enrollmentSection,
                                    teacherName = "${teacher.firstName} ${teacher.lastName}"
                                )
                                
                                realtimeDataManager.loadAllData()
                            }.onFailure { exception ->
                                val errorMsg = "Failed to create enrollment: ${exception.message}"
                                Log.e("TeacherStudentAppsVM", errorMsg, exception)
                                // Update UI state to show error to user
                                _uiState.value = _uiState.value.copy(
                                    error = errorMsg
                                )
                            }
                        }.onFailure { exception ->
                            Log.e("TeacherStudentAppsVM", "Failed to get subject: ${exception.message}", exception)
                        }
                    }.onFailure { exception ->
                        Log.e("TeacherStudentAppsVM", "Failed to get student: ${exception.message}", exception)
                    }
                }.onFailure { exception ->
                    Log.e("TeacherStudentAppsVM", "Failed to get section assignments: ${exception.message}", exception)
                }
            }.onFailure { exception ->
                Log.e("TeacherStudentAppsVM", "Failed to get current teacher: ${exception.message}", exception)
            }
        } catch (e: Exception) {
            Log.e("TeacherStudentAppsVM", "Exception in createStudentEnrollmentFromSubjectApplication: ${e.message}", e)
        }
    }
}

data class TeacherStudentApplicationsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val processingApplications: Set<String> = emptySet()
)
