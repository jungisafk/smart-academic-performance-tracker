package com.smartacademictracker.presentation.student

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.StudentEnrollment
import com.smartacademictracker.data.model.EnrollmentStatus
import com.smartacademictracker.data.repository.StudentEnrollmentRepository
import com.smartacademictracker.data.notification.NotificationSenderService
import com.smartacademictracker.data.notification.EnrollmentNotificationService
import com.smartacademictracker.data.model.NotificationType
import com.smartacademictracker.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentEnrollmentViewModel @Inject constructor(
    private val enrollmentRepository: StudentEnrollmentRepository,
    private val userRepository: UserRepository,
    private val notificationSenderService: NotificationSenderService,
    private val enrollmentNotificationService: EnrollmentNotificationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentEnrollmentUiState())
    val uiState: StateFlow<StudentEnrollmentUiState> = _uiState.asStateFlow()

    private val _enrollments = MutableStateFlow<List<StudentEnrollment>>(emptyList())
    val enrollments: StateFlow<List<StudentEnrollment>> = _enrollments.asStateFlow()

    fun loadEnrollments() {
        viewModelScope.launch {
            Log.d("StudentEnrollment", "Loading student enrollments...")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { currentUser ->
                    if (currentUser == null) {
                        Log.e("StudentEnrollment", "Current user is null")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "User not found"
                        )
                        return@onSuccess
                    }
                    
                    Log.d("StudentEnrollment", "Loading enrollments for student: ${currentUser.id} (${currentUser.firstName} ${currentUser.lastName})")
                    val result = enrollmentRepository.getEnrollmentsByStudent(currentUser.id)
                    result.onSuccess { enrollmentList ->
                        Log.d("StudentEnrollment", "Found ${enrollmentList.size} enrollments for student")
                        enrollmentList.forEach { enrollment ->
                            Log.d("StudentEnrollment", "Enrollment: ${enrollment.subjectName} - ${enrollment.sectionName} (Status: ${enrollment.status})")
                        }
                        _enrollments.value = enrollmentList
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = null
                        )
                    }.onFailure { exception ->
                        Log.e("StudentEnrollment", "Failed to load enrollments: ${exception.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load enrollments"
                        )
                    }
                }.onFailure { exception ->
                    Log.e("StudentEnrollment", "Failed to get current user: ${exception.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to get user information"
                    )
                }
            } catch (e: Exception) {
                Log.e("StudentEnrollment", "Exception in loadEnrollments: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load enrollments"
                )
            }
        }
    }

    fun leaveSection(enrollmentId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { currentUser ->
                    if (currentUser == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "User not found"
                        )
                        return@onSuccess
                    }
                    
                    val result = enrollmentRepository.updateEnrollmentStatus(
                        enrollmentId = enrollmentId,
                        status = EnrollmentStatus.DROPPED,
                        updatedBy = currentUser.id,
                        updatedByName = "${currentUser.firstName} ${currentUser.lastName}",
                        notes = "Student voluntarily left the section"
                    )
                    
                    result.onSuccess {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            successMessage = "Successfully left the section"
                        )
                        
                        // Get enrollment details for comprehensive notifications
                        enrollmentRepository.getEnrollmentById(enrollmentId).onSuccess { enrollment ->
                            if (enrollment != null) {
                                // Send comprehensive notifications using the new service
                                enrollmentNotificationService.notifyStudentLeftClass(
                                    studentId = currentUser.id,
                                    studentName = "${currentUser.firstName} ${currentUser.lastName}",
                                    subjectId = enrollment.subjectId,
                                    subjectName = enrollment.subjectName,
                                    teacherId = enrollment.teacherId,
                                    teacherName = enrollment.teacherName,
                                    reason = "Student voluntarily left the class"
                                )
                            }
                        }
                        
                        // Reload enrollments
                        loadEnrollments()
                    }.onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to leave section"
                        )
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to get user information"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to leave section"
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
}

data class StudentEnrollmentUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
