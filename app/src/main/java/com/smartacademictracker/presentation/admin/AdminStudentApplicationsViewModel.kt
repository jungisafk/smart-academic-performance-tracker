package com.smartacademictracker.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.SubjectApplication
import com.smartacademictracker.data.model.ApplicationStatus
import com.smartacademictracker.data.model.StudentEnrollment
import com.smartacademictracker.data.model.EnrollmentStatus
import com.smartacademictracker.data.repository.SubjectApplicationRepository
import com.smartacademictracker.data.repository.StudentEnrollmentRepository
import com.smartacademictracker.data.repository.UserRepository
import com.smartacademictracker.data.repository.SubjectRepository
import com.smartacademictracker.data.repository.SectionAssignmentRepository
import com.smartacademictracker.data.manager.AdminDataCache
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminStudentApplicationsViewModel @Inject constructor(
    private val subjectApplicationRepository: SubjectApplicationRepository,
    private val studentEnrollmentRepository: StudentEnrollmentRepository,
    private val userRepository: UserRepository,
    private val subjectRepository: SubjectRepository,
    private val sectionAssignmentRepository: SectionAssignmentRepository,
    private val notificationSenderService: com.smartacademictracker.data.notification.NotificationSenderService,
    private val adminDataCache: AdminDataCache
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminStudentApplicationsUiState())
    val uiState: StateFlow<AdminStudentApplicationsUiState> = _uiState.asStateFlow()

    private val _applications = MutableStateFlow<List<SubjectApplication>>(emptyList())
    val applications: StateFlow<List<SubjectApplication>> = _applications.asStateFlow()

    init {
        // Load cached data immediately if available
        val cachedApplications = adminDataCache.cachedStudentApplications.value
        if (cachedApplications.isNotEmpty() && adminDataCache.isCacheValid()) {
            _applications.value = cachedApplications
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun loadApplications(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            // Load cached data first if available and not forcing refresh
            if (!forceRefresh && adminDataCache.cachedStudentApplications.value.isNotEmpty() && adminDataCache.isCacheValid()) {
                _applications.value = adminDataCache.cachedStudentApplications.value
                _uiState.value = _uiState.value.copy(isLoading = false)
            } else {
                // Only show loading if we don't have cached data
                if (adminDataCache.cachedStudentApplications.value.isEmpty()) {
                    _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                }
            }
            
            try {
                val result = subjectApplicationRepository.getAllApplications()
                result.onSuccess { applicationsList ->
                    _applications.value = applicationsList
                    adminDataCache.updateStudentApplications(applicationsList)
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load applications"
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

    fun approveApplication(applicationId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                processingApplications = _uiState.value.processingApplications + applicationId,
                error = null
            )
            
            try {
                val applicationResult = subjectApplicationRepository.getApplicationById(applicationId)
                applicationResult.onSuccess { application ->
                    val statusResult = subjectApplicationRepository.updateApplicationStatus(
                        applicationId, 
                        ApplicationStatus.APPROVED
                    )
                    statusResult.onSuccess {
                        createStudentEnrollment(application)
                    }.onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            processingApplications = _uiState.value.processingApplications - applicationId,
                            error = "Failed to update application status: ${exception.message}"
                        )
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        processingApplications = _uiState.value.processingApplications - applicationId,
                        error = "Failed to get application details: ${exception.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    processingApplications = _uiState.value.processingApplications - applicationId,
                    error = "Failed to approve application: ${e.message}"
                )
            }
        }
    }

    private suspend fun createStudentEnrollment(application: SubjectApplication) {
        try {
            // Get section assignment for this subject and section to get correct section name and teacher info
            val sectionAssignmentsResult = sectionAssignmentRepository.getSectionAssignmentsBySubject(application.subjectId)
            sectionAssignmentsResult.onSuccess { assignments ->
                // Find the section assignment that matches the application's section name
                // Handle both short names (like "A") and full names (like "PROG101A")
                val matchingAssignment = assignments.find { assignment ->
                    assignment.status == com.smartacademictracker.data.model.AssignmentStatus.ACTIVE &&
                    (assignment.sectionName == application.sectionName ||
                     assignment.sectionName.endsWith(application.sectionName) ||
                     application.sectionName.endsWith(assignment.sectionName.takeLast(1)))
                }
                
                // Use the section name from the assignment if found, otherwise use the application's section name
                val enrollmentSectionName = matchingAssignment?.sectionName ?: application.sectionName
                val teacherId = matchingAssignment?.teacherId ?: ""
                val teacherName = matchingAssignment?.teacherName ?: ""
                val teacherEmail = matchingAssignment?.teacherEmail ?: ""
                
                // Warn if no matching assignment found (but still proceed with enrollment)
                if (matchingAssignment == null) {
                    android.util.Log.w("AdminStudentAppsVM", "No matching section assignment found for subject ${application.subjectId} section ${application.sectionName}. Enrollment will be created without teacher info.")
                }
                
                val studentResult = userRepository.getUserById(application.studentId)
                studentResult.onSuccess { student ->
                    val subjectResult = subjectRepository.getSubjectById(application.subjectId)
                    subjectResult.onSuccess { subject ->
                        // Check if student is already enrolled to prevent duplicates
                        val isEnrolledResult = studentEnrollmentRepository.isStudentEnrolled(
                            application.studentId,
                            application.subjectId,
                            enrollmentSectionName
                        )
                        
                        val isAlreadyEnrolled = isEnrolledResult.getOrNull() ?: false
                        if (isAlreadyEnrolled) {
                            android.util.Log.w("AdminStudentAppsVM", "Student ${application.studentId} is already enrolled in ${application.subjectId} section $enrollmentSectionName")
                            // Still show success message since application was approved
                            notificationSenderService.sendApplicationStatusNotification(
                                userId = application.studentId,
                                applicationType = "Subject Application",
                                status = "approved",
                                subjectName = application.subjectName
                            )
                            _uiState.value = _uiState.value.copy(
                                processingApplications = _uiState.value.processingApplications - application.id
                            )
                            loadApplications(forceRefresh = true)
                            return@onSuccess
                        }
                        
                        val enrollment = StudentEnrollment(
                            studentId = application.studentId,
                            studentName = application.studentName,
                            studentEmail = student?.email ?: "",
                            subjectId = application.subjectId,
                            subjectName = application.subjectName,
                            subjectCode = subject?.code ?: "",
                            sectionName = enrollmentSectionName, // Use section name from assignment (e.g., "PROG101A")
                            teacherId = teacherId,
                            teacherName = teacherName,
                            teacherEmail = teacherEmail,
                            courseId = application.courseId,
                            courseName = application.courseName,
                            yearLevelId = application.yearLevelId,
                            yearLevelName = application.yearLevelName,
                            semester = subject?.semester ?: com.smartacademictracker.data.model.Semester.FIRST_SEMESTER,
                            academicYear = subject?.academicYear ?: "",
                            academicPeriodId = matchingAssignment?.academicPeriodId ?: application.academicPeriodId,
                            enrollmentDate = System.currentTimeMillis(),
                            status = EnrollmentStatus.ACTIVE,
                            enrolledBy = "admin", // TODO: Get actual admin user ID
                            enrolledByName = "Admin",
                            notes = "Enrolled after application approval by admin",
                            createdAt = System.currentTimeMillis()
                        )

                        val enrollmentResult = studentEnrollmentRepository.enrollStudent(enrollment)
                        enrollmentResult.onSuccess { enrollmentId ->
                            android.util.Log.i("AdminStudentAppsVM", "Successfully created enrollment $enrollmentId for student ${application.studentId} in subject ${application.subjectId}")
                            
                            // Send application approved notification
                            notificationSenderService.sendApplicationStatusNotification(
                                userId = application.studentId,
                                applicationType = "Subject Application",
                                status = "approved",
                                subjectName = application.subjectName
                            )
                            
                            // Send enrollment notification
                            notificationSenderService.sendStudentEnrolledNotification(
                                studentId = application.studentId,
                                subjectName = application.subjectName,
                                sectionName = enrollmentSectionName,
                                teacherName = teacherName.ifEmpty { "TBA" }
                            )
                            
                            _uiState.value = _uiState.value.copy(
                                processingApplications = _uiState.value.processingApplications - application.id
                            )
                            loadApplications(forceRefresh = true)
                        }.onFailure { exception ->
                            val errorMsg = "Application approved but failed to create enrollment: ${exception.message}"
                            android.util.Log.e("AdminStudentAppsVM", errorMsg, exception)
                            _uiState.value = _uiState.value.copy(
                                processingApplications = _uiState.value.processingApplications - application.id,
                                error = errorMsg
                            )
                        }
                    }.onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            processingApplications = _uiState.value.processingApplications - application.id,
                            error = "Failed to get subject information: ${exception.message}"
                        )
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        processingApplications = _uiState.value.processingApplications - application.id,
                        error = "Failed to get student information: ${exception.message}"
                    )
                }
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    processingApplications = _uiState.value.processingApplications - application.id,
                    error = "Failed to get section assignments: ${exception.message}"
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                processingApplications = _uiState.value.processingApplications - application.id,
                error = "Failed to create enrollment: ${e.message}"
            )
        }
    }

    fun rejectApplication(applicationId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                processingApplications = _uiState.value.processingApplications + applicationId,
                error = null
            )
            
            try {
                // Get application details first to notify student
                val applicationResult = subjectApplicationRepository.getApplicationById(applicationId)
                applicationResult.onSuccess { application ->
                    val result = subjectApplicationRepository.updateApplicationStatus(
                        applicationId, 
                        ApplicationStatus.REJECTED
                    )
                    result.onSuccess {
                        notificationSenderService.sendApplicationStatusNotification(
                            userId = application.studentId,
                            applicationType = "Subject Application",
                            status = "rejected",
                            subjectName = application.subjectName,
                            reason = application.remarks
                        )
                        
                        loadApplications(forceRefresh = true)
                    }.onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            processingApplications = _uiState.value.processingApplications - applicationId,
                            error = "Failed to reject application: ${exception.message}"
                        )
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        processingApplications = _uiState.value.processingApplications - applicationId,
                        error = "Failed to get application details: ${exception.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    processingApplications = _uiState.value.processingApplications - applicationId,
                    error = "Failed to reject application: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class AdminStudentApplicationsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val processingApplications: Set<String> = emptySet()
)
