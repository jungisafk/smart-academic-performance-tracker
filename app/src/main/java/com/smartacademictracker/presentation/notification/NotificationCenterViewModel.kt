package com.smartacademictracker.presentation.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.Notification
import com.smartacademictracker.data.repository.NotificationRepository
import com.smartacademictracker.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationCenterViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationCenterUiState())
    val uiState: StateFlow<NotificationCenterUiState> = _uiState.asStateFlow()

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val userIdResult = getCurrentUserId()
                userIdResult.onSuccess { userId ->
                    val result = notificationRepository.getNotificationsByUserId(userId)
                    result.fold(
                        onSuccess = { notificationList ->
                            _notifications.value = notificationList
                            _uiState.value = _uiState.value.copy(isLoading = false)
                        },
                        onFailure = { error ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = error.message ?: "Failed to load notifications"
                            )
                        }
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to get user information"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    fun refreshNotifications() {
        loadNotifications()
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                notificationRepository.markNotificationAsRead(notificationId)
                // Update local state
                _notifications.value = _notifications.value.map { notification ->
                    if (notification.id == notificationId) {
                        notification.copy(isRead = true)
                    } else {
                        notification
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to mark notification as read"
                )
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                val userIdResult = getCurrentUserId()
                userIdResult.onSuccess { userId ->
                    notificationRepository.markAllNotificationsAsRead(userId)
                    // Update local state
                    _notifications.value = _notifications.value.map { notification ->
                        notification.copy(isRead = true)
                    }
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "Failed to get user information"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to mark all notifications as read"
                )
            }
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            try {
                notificationRepository.deleteNotification(notificationId)
                // Update local state
                _notifications.value = _notifications.value.filter { 
                    it.id != notificationId 
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to delete notification"
                )
            }
        }
    }

    fun deleteAllNotifications() {
        viewModelScope.launch {
            try {
                val userIdResult = getCurrentUserId()
                userIdResult.onSuccess { userId ->
                    notificationRepository.deleteAllNotifications(userId)
                    _notifications.value = emptyList()
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "Failed to get user information"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to delete all notifications"
                )
            }
        }
    }

    private suspend fun getCurrentUserId(): Result<String> {
        return try {
            val userResult = userRepository.getCurrentUser()
            userResult.fold(
                onSuccess = { user ->
                    if (user != null) {
                        Result.success(user.id)
                    } else {
                        Result.failure(IllegalStateException("User not authenticated"))
                    }
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Failed to get current user: ${e.message}"))
        }
    }
}

data class NotificationCenterUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
