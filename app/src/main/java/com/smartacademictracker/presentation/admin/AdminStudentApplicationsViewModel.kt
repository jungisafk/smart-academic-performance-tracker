package com.smartacademictracker.presentation.admin

import android.util.Log
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
    private val notificationSenderService: com.smartacademictracker.data.notification.NotificationSenderService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminStudentApplicationsUiState())
    val uiState: StateFlow<AdminStudentApplicationsUiState> = _uiState.asStateFlow()

    private val _applications = MutableStateFlow<List<SubjectApplication>>(emptyList())
    val applications: StateFlow<List<SubjectApplication>> = _applications.asStateFlow()

    fun loadApplications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = subjectApplicationRepository.getAllApplications()
                result.onSuccess { applicationsList ->
                    _applications.value = applicationsList
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    Log.d("AdminStudentApps", "Loaded ${applicationsList.size} student applications")
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load applications"
                    )
                    Log.e("AdminStudentApps", "Error loading applications: ${exception.message}")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load applications"
                )
                Log.e("AdminStudentApps", "Exception loading applications: ${e.message}")
            }
        }
    }

    fun approveApplication(applicationId: String) {
        viewModelScope.launch {
            Log.d("AdminStudentApps", "approveApplication called for ID: $applicationId")
            _uiState.value = _uiState.value.copy(
                processingApplications = _uiState.value.processingApplications + applicationId,
                error = null
            )
            
            try {
                Log.d("AdminStudentApps", "Getting application details for ID: $applicationId")
                // Get the application details
                val applicationResult = subjectApplicationRepository.getApplicationById(applicationId)
                applicationResult.onSuccess { application ->
                    Log.d("AdminStudentApps", "Application found: ${application.studentName} for subject: ${application.subjectName}")
                    Log.d("AdminStudentApps", "Updating application status to APPROVED")
                    // Update application status to approved
                    val statusResult = subjectApplicationRepository.updateApplicationStatus(
                        applicationId, 
                        ApplicationStatus.APPROVED
                    )
                    statusResult.onSuccess {
                        Log.d("AdminStudentApps", "Application status updated successfully, creating enrollment")
                        // Create student enrollment
                        createStudentEnrollment(application)
                    }.onFailure { exception ->
                        Log.e("AdminStudentApps", "Error updating application status: ${exception.message}")
                        _uiState.value = _uiState.value.copy(
                            processingApplications = _uiState.value.processingApplications - applicationId,
                            error = "Failed to update application status: ${exception.message}"
                        )
                    }
                }.onFailure { exception ->
                    Log.e("AdminStudentApps", "Error getting application: ${exception.message}")
                    _uiState.value = _uiState.value.copy(
                        processingApplications = _uiState.value.processingApplications - applicationId,
                        error = "Failed to get application details: ${exception.message}"
                    )
                }
            } catch (e: Exception) {
                Log.e("AdminStudentApps", "Exception approving application: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    processingApplications = _uiState.value.processingApplications - applicationId,
                    error = "Failed to approve application: ${e.message}"
                )
            }
        }
    }

    private suspend fun createStudentEnrollment(application: SubjectApplication) {
        try {
            Log.d("AdminStudentApps", "Starting enrollment creation for application: ${application.id}")
            Log.d("AdminStudentApps", "Application details: studentId=${application.studentId}, subjectId=${application.subjectId}, sectionName=${application.sectionName}")
            
            // Get student information
            val studentResult = userRepository.getUserById(application.studentId)
            studentResult.onSuccess { student ->
                Log.d("AdminStudentApps", "Student found: ${student?.firstName} ${student?.lastName}")
                
                // Get subject information
                val subjectResult = subjectRepository.getSubjectById(application.subjectId)
                subjectResult.onSuccess { subject ->
                    Log.d("AdminStudentApps", "Subject found: ${subject?.name}")
                    
                    // Create enrollment
                    val enrollment = StudentEnrollment(
                        studentId = application.studentId,
                        studentName = application.studentName,
                        subjectId = application.subjectId,
                        subjectName = application.subjectName,
                        sectionName = application.sectionName,
                        courseId = application.courseId,
                        courseName = application.courseName,
                        yearLevelId = application.yearLevelId,
                        yearLevelName = application.yearLevelName,
                        academicPeriodId = application.academicPeriodId,
                        enrollmentDate = System.currentTimeMillis(),
                        status = EnrollmentStatus.ACTIVE
                    )

                    Log.d("AdminStudentApps", "Calling studentEnrollmentRepository.enrollStudent")
                    val enrollmentResult = studentEnrollmentRepository.enrollStudent(enrollment)
                    enrollmentResult.onSuccess { enrollmentId ->
                        Log.d("AdminStudentApps", "Successfully created enrollment for student ${application.studentName} with ID: $enrollmentId")
                        
                        // Notify student that their application was approved
                        notificationSenderService.sendApplicationStatusNotification(
                            userId = application.studentId,
                            applicationType = "Subject Application",
                            status = "approved",
                            subjectName = application.subjectName
                        )
                        
                        _uiState.value = _uiState.value.copy(
                            processingApplications = _uiState.value.processingApplications - application.id
                        )
                        // Reload applications to show updated status
                        loadApplications()
                    }.onFailure { exception ->
                        Log.e("AdminStudentApps", "Error creating enrollment: ${exception.message}")
                        _uiState.value = _uiState.value.copy(
                            processingApplications = _uiState.value.processingApplications - application.id,
                            error = "Application approved but failed to create enrollment: ${exception.message}"
                        )
                    }
                }.onFailure { exception ->
                    Log.e("AdminStudentApps", "Error getting subject: ${exception.message}")
                    _uiState.value = _uiState.value.copy(
                        processingApplications = _uiState.value.processingApplications - application.id,
                        error = "Failed to get subject information: ${exception.message}"
                    )
                }
            }.onFailure { exception ->
                Log.e("AdminStudentApps", "Error getting student: ${exception.message}")
                _uiState.value = _uiState.value.copy(
                    processingApplications = _uiState.value.processingApplications - application.id,
                    error = "Failed to get student information: ${exception.message}"
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                processingApplications = _uiState.value.processingApplications - application.id,
                error = "Failed to create enrollment: ${e.message}"
            )
            Log.e("AdminStudentApps", "Exception creating enrollment: ${e.message}")
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
                        Log.d("AdminStudentApps", "Successfully rejected application $applicationId")
                        
                        // Notify student that their application was rejected
                        notificationSenderService.sendApplicationStatusNotification(
                            userId = application.studentId,
                            applicationType = "Subject Application",
                            status = "rejected",
                            subjectName = application.subjectName,
                            reason = application.remarks
                        )
                        
                        // Reload applications to show updated status
                        loadApplications()
                    }.onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            processingApplications = _uiState.value.processingApplications - applicationId,
                            error = "Failed to reject application: ${exception.message}"
                        )
                        Log.e("AdminStudentApps", "Error rejecting application: ${exception.message}")
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        processingApplications = _uiState.value.processingApplications - applicationId,
                        error = "Failed to get application details: ${exception.message}"
                    )
                    Log.e("AdminStudentApps", "Error getting application: ${exception.message}")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    processingApplications = _uiState.value.processingApplications - applicationId,
                    error = "Failed to reject application: ${e.message}"
                )
                Log.e("AdminStudentApps", "Exception rejecting application: ${e.message}")
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
