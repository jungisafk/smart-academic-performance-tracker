package com.smartacademictracker.presentation.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.smartacademictracker.data.model.Notification
import com.smartacademictracker.data.model.NotificationType
import com.smartacademictracker.data.model.NotificationPriority
import com.smartacademictracker.data.repository.NotificationRepository
import com.smartacademictracker.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { currentUser ->
                    if (currentUser != null) {
                        // Load all notifications for the user
                        val notificationsResult = notificationRepository.getNotificationsByUserId(currentUser.id)
                        notificationsResult.onSuccess { notificationList ->
                            _notifications.value = notificationList
                            
                            // Count unread notifications
                            val unread = notificationList.count { !it.isRead }
                            _unreadCount.value = unread
                            
                            _uiState.value = _uiState.value.copy(isLoading = false)
                        }.onFailure { exception ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Failed to load notifications: ${exception.message}"
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
                        error = "Failed to get user: ${exception.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load notifications"
                )
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                val result = notificationRepository.markNotificationAsRead(notificationId)
                result.onSuccess {
                    // Update local state
                    _notifications.value = _notifications.value.map { notification ->
                        if (notification.id == notificationId) {
                            notification.copy(isRead = true)
                        } else {
                            notification
                        }
                    }
                    
                    // Update unread count
                    val unread = _notifications.value.count { !it.isRead }
                    _unreadCount.value = unread
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to mark notification as read: ${exception.message}"
                    )
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
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { currentUser ->
                    if (currentUser != null) {
                        val result = notificationRepository.markAllNotificationsAsRead(currentUser.id)
                        result.onSuccess {
                            // Update local state
                            _notifications.value = _notifications.value.map { notification ->
                                notification.copy(isRead = true)
                            }
                            _unreadCount.value = 0
                        }.onFailure { exception ->
                            _uiState.value = _uiState.value.copy(
                                error = "Failed to mark all notifications as read: ${exception.message}"
                            )
                        }
                    }
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
                val result = notificationRepository.deleteNotification(notificationId)
                result.onSuccess {
                    // Remove from local state
                    _notifications.value = _notifications.value.filter { it.id != notificationId }
                    
                    // Update unread count
                    val unread = _notifications.value.count { !it.isRead }
                    _unreadCount.value = unread
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to delete notification: ${exception.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to delete notification"
                )
            }
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            try {
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { currentUser ->
                    if (currentUser != null) {
                        val result = notificationRepository.deleteAllNotifications(currentUser.id)
                        result.onSuccess {
                            // Clear local state
                            _notifications.value = emptyList()
                            _unreadCount.value = 0
                        }.onFailure { exception ->
                            _uiState.value = _uiState.value.copy(
                                error = "Failed to clear all notifications: ${exception.message}"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to clear all notifications"
                )
            }
        }
    }

    fun createTestNotifications() {
        viewModelScope.launch {
            try {
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { currentUser ->
                    if (currentUser != null) {
                        val testNotifications = listOf(
                            Notification(
                                id = "",
                                userId = currentUser.id,
                                title = "Grade Released",
                                message = "Your grade for Mathematics has been released. Check your grades section.",
                                type = NotificationType.GRADE_UPDATE,
                                priority = NotificationPriority.HIGH,
                                isRead = false,
                                createdAt = Timestamp.now(),
                                readAt = null
                            ),
                            Notification(
                                id = "",
                                userId = currentUser.id,
                                title = "Student Left Class",
                                message = "John Doe has left your Mathematics class.",
                                type = NotificationType.STUDENT_LEFT_CLASS,
                                priority = NotificationPriority.NORMAL,
                                isRead = false,
                                createdAt = Timestamp.now(),
                                readAt = null
                            ),
                            Notification(
                                id = "",
                                userId = currentUser.id,
                                title = "All Grades Submitted",
                                message = "All grades for Mathematics class have been submitted successfully.",
                                type = NotificationType.ALL_GRADES_SUBMITTED,
                                priority = NotificationPriority.HIGH,
                                isRead = true,
                                createdAt = Timestamp.now(),
                                readAt = Timestamp.now()
                            ),
                            Notification(
                                id = "",
                                userId = currentUser.id,
                                title = "Application Approved",
                                message = "Your application for Computer Science has been approved.",
                                type = NotificationType.APPLICATION_APPROVED,
                                priority = NotificationPriority.HIGH,
                                isRead = false,
                                createdAt = Timestamp.now(),
                                readAt = null
                            ),
                            Notification(
                                id = "",
                                userId = currentUser.id,
                                title = "Deadline Reminder",
                                message = "Assignment submission deadline is approaching for Physics.",
                                type = NotificationType.DEADLINE_REMINDER,
                                priority = NotificationPriority.URGENT,
                                isRead = false,
                                createdAt = Timestamp.now(),
                                readAt = null
                            ),
                            Notification(
                                id = "",
                                userId = currentUser.id,
                                title = "System Announcement",
                                message = "The system will undergo maintenance this weekend.",
                                type = NotificationType.SYSTEM_ANNOUNCEMENT,
                                priority = NotificationPriority.NORMAL,
                                isRead = false,
                                createdAt = Timestamp.now(),
                                readAt = null
                            )
                        )
                        
                        // Create each notification
                        testNotifications.forEach { notification ->
                            notificationRepository.createNotification(notification)
                        }
                        
                        // Reload notifications to show the new ones
                        loadNotifications()
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to create test notifications"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class NotificationUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
