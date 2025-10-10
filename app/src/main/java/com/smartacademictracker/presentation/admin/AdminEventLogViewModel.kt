package com.smartacademictracker.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.AuditTrail
import com.smartacademictracker.data.model.Notification
import com.smartacademictracker.data.model.AuditTrailFilter
import com.smartacademictracker.data.repository.AuditTrailRepository
import com.smartacademictracker.data.repository.NotificationRepository
import com.smartacademictracker.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminEventLogViewModel @Inject constructor(
    private val auditTrailRepository: AuditTrailRepository,
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminEventLogUiState())
    val uiState: StateFlow<AdminEventLogUiState> = _uiState.asStateFlow()

    private val _auditTrails = MutableStateFlow<List<AuditTrail>>(emptyList())
    val auditTrails: StateFlow<List<AuditTrail>> = _auditTrails.asStateFlow()

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    fun loadEventLogs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Load audit trails
                val auditResult = auditTrailRepository.getAuditTrailEntries(AuditTrailFilter())
                auditResult.onSuccess { trails ->
                    _auditTrails.value = trails
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to load audit trails: ${exception.message}"
                    )
                }

                // Load recent notifications
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { currentUser ->
                    if (currentUser != null) {
                        val notificationResult = notificationRepository.getNotificationsByUserId(currentUser.id)
                        notificationResult.onSuccess { notifs ->
                            _notifications.value = notifs
                        }
                    }
                }

                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load event logs"
                )
            }
        }
    }

    fun loadEnrollmentEvents() {
        viewModelScope.launch {
            try {
                // Filter audit trails for enrollment-related events
                val enrollmentEvents = _auditTrails.value.filter { trail ->
                    trail.reason.contains("left", ignoreCase = true) ||
                    trail.reason.contains("removed", ignoreCase = true) ||
                    trail.reason.contains("kicked", ignoreCase = true) ||
                    trail.action.name in listOf("DELETED", "REJECTED")
                }
                
                _uiState.value = _uiState.value.copy(
                    enrollmentEvents = enrollmentEvents
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load enrollment events: ${e.message}"
                )
            }
        }
    }

    fun loadGradeCompletionNotifications() {
        viewModelScope.launch {
            try {
                // Filter notifications for grade completion
                val gradeCompletionNotifications = _notifications.value.filter { notification ->
                    notification.type.name in listOf("ALL_GRADES_SUBMITTED", "GRADE_COMPLETION_NOTIFICATION")
                }
                
                _uiState.value = _uiState.value.copy(
                    gradeCompletionNotifications = gradeCompletionNotifications
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load grade completion notifications: ${e.message}"
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

data class AdminEventLogUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val enrollmentEvents: List<AuditTrail> = emptyList(),
    val gradeCompletionNotifications: List<Notification> = emptyList()
)
