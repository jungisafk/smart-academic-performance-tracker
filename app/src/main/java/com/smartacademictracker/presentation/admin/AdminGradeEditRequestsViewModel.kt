package com.smartacademictracker.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.Grade
import com.smartacademictracker.data.repository.GradeRepository
import com.smartacademictracker.data.repository.UserRepository
import com.smartacademictracker.data.notification.NotificationSenderService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminGradeEditRequestsViewModel @Inject constructor(
    private val gradeRepository: GradeRepository,
    private val userRepository: UserRepository,
    private val notificationSenderService: NotificationSenderService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminGradeEditRequestsUiState())
    val uiState: StateFlow<AdminGradeEditRequestsUiState> = _uiState.asStateFlow()

    private val _gradeRequests = MutableStateFlow<List<Grade>>(emptyList())
    val gradeRequests: StateFlow<List<Grade>> = _gradeRequests.asStateFlow()

    private val _gradeHistory = MutableStateFlow<List<Grade>>(emptyList())
    val gradeHistory: StateFlow<List<Grade>> = _gradeHistory.asStateFlow()

    init {
        // Set up real-time listener for grade edit requests
        // Only show loading if we don't have any cached data
        viewModelScope.launch {
            gradeRepository.getGradesWithEditRequestsFlow()
                .onStart {
                    // Only show loading if we don't have any data yet
                    if (_gradeRequests.value.isEmpty()) {
                        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                    }
                }
                .catch { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load grade edit requests"
                    )
                }
                .collect { grades ->
                    _gradeRequests.value = grades
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        requestCount = grades.size,
                        error = null
                    )
                }
        }
        
        // Load history of unlocked grades
        loadGradeEditHistory()
    }
    
    fun loadGradeEditHistory() {
        viewModelScope.launch {
            val result = gradeRepository.getUnlockedGradesHistory(limit = 100)
            result.onSuccess { grades ->
                _gradeHistory.value = grades
            }.onFailure { exception ->
                // Don't show error for history, just log it
                println("Failed to load grade edit history: ${exception.message}")
            }
        }
    }

    fun loadGradeEditRequests() {
        // Real-time updates are handled automatically via Flow
        // This method is kept for backward compatibility but does nothing
        // as the Flow in init already handles all updates
    }

    fun approveEditRequest(gradeId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val currentUser = userRepository.getCurrentUser().getOrNull()
            val adminId = currentUser?.id ?: ""
            
            if (adminId.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Admin user not found"
                )
                return@launch
            }
            
            // Get grade details before unlocking for notification
            val gradeResult = gradeRepository.getGradeById(gradeId)
            val grade = gradeResult.getOrNull()
            
            val result = gradeRepository.unlockGradeForEdit(gradeId, adminId)
            
            result.onSuccess {
                // Notify teacher about approval
                if (grade != null) {
                    val adminName = currentUser?.let { "${it.firstName} ${it.lastName}" } ?: "Admin"
                    notificationSenderService.sendGradeEditRequestApprovedNotification(
                        teacherId = grade.teacherId,
                        studentName = grade.studentName,
                        subjectName = grade.subjectName,
                        gradePeriod = grade.gradePeriod.displayName
                    )
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Grade edit request approved"
                )
                // Real-time Flow will automatically update the list
                // Reload history to show the newly unlocked grade
                loadGradeEditHistory()
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = exception.message ?: "Failed to approve edit request"
                )
            }
        }
    }
    
    fun rejectEditRequest(gradeId: String, reason: String? = null) {
        // If reason is empty string, convert to null
        val rejectionReason = if (reason.isNullOrBlank()) null else reason
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val currentUser = userRepository.getCurrentUser().getOrNull()
            val adminId = currentUser?.id ?: ""
            
            if (adminId.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Admin user not found"
                )
                return@launch
            }
            
            // Get grade details before rejecting for notification
            val gradeResult = gradeRepository.getGradeById(gradeId)
            val grade = gradeResult.getOrNull()
            
            val result = gradeRepository.rejectGradeEditRequest(gradeId, adminId)
            
            result.onSuccess {
                // Notify teacher about rejection
                if (grade != null) {
                    notificationSenderService.sendGradeEditRequestRejectedNotification(
                        teacherId = grade.teacherId,
                        studentName = grade.studentName,
                        subjectName = grade.subjectName,
                        gradePeriod = grade.gradePeriod.displayName,
                        reason = rejectionReason
                    )
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Grade edit request rejected"
                )
                // Real-time Flow will automatically update the list
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = exception.message ?: "Failed to reject edit request"
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

data class AdminGradeEditRequestsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val requestCount: Int = 0
)

