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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
    private val localNotificationService: com.smartacademictracker.data.notification.LocalNotificationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()
    
    private var notificationsFlowJob: kotlinx.coroutines.Job? = null
    private var previousNotificationIds = mutableSetOf<String>()
    private var isFirstLoad = true

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { currentUser ->
                    if (currentUser != null) {
                        // Cancel any existing flow job
                        notificationsFlowJob?.cancel()
                        
                        // Reset first load flag when reloading
                        // This ensures that if loadNotifications() is called again, we re-initialize previousNotificationIds
                        isFirstLoad = true
                        
                        // Set up real-time listener for notifications
                        notificationsFlowJob = viewModelScope.launch {
                            notificationRepository.getNotificationsByUserIdFlow(currentUser.id)
                                .catch { exception: Throwable ->
                                    // Fallback to one-time query on error
                                    val result = notificationRepository.getNotificationsByUserId(currentUser.id)
                                    result.onSuccess { notificationList: List<Notification> ->
                                        _notifications.value = notificationList
                                        val unread = notificationList.count { notification: Notification -> !notification.isRead }
                                        _unreadCount.value = unread
                                        _uiState.value = _uiState.value.copy(isLoading = false)
                                    }
                                }
                                .collect { notificationList: List<Notification> ->
                                    android.util.Log.d("NotificationViewModel", "Flow collected ${notificationList.size} notifications")
                                    
                                    // Log read status of all notifications
                                    notificationList.forEach { notification ->
                                        android.util.Log.d("NotificationViewModel", "Notification ${notification.id}: isRead=${notification.isRead}, title=${notification.title}")
                                    }
                                    
                                    val currentNotificationIds = notificationList.map { notification: Notification -> notification.id }.toSet()
                                    
                                    // On first load, initialize previousNotificationIds with all current notification IDs
                                    // This prevents existing notifications from being treated as "new"
                                    if (isFirstLoad) {
                                        android.util.Log.d("NotificationViewModel", "First load - initializing previousNotificationIds with ${currentNotificationIds.size} notification IDs")
                                        previousNotificationIds = currentNotificationIds.toMutableSet()
                                        isFirstLoad = false
                                        // Don't trigger any notifications on first load - these are existing notifications
                                    } else {
                                        // After first load, check for truly new notifications
                                        val newNotifications = notificationList.filter { notification: Notification ->
                                            notification.id !in previousNotificationIds && !notification.isRead 
                                        }
                                        
                                        android.util.Log.d("NotificationViewModel", "New notifications count: ${newNotifications.size}, previousNotificationIds size: ${previousNotificationIds.size}")
                                        
                                        // Only trigger system notifications for truly new unread notifications
                                        newNotifications.forEach { notification: Notification ->
                                            android.util.Log.d("NotificationViewModel", "Triggering system notification for NEW notification: ${notification.id}, isRead=${notification.isRead}")
                                            localNotificationService.showNotification(
                                                title = notification.title,
                                                message = notification.message,
                                                type = notification.type,
                                                priority = notification.priority,
                                                actionUrl = notification.actionUrl,
                                                data = notification.data
                                            )
                                        }
                                        
                                        // Update previous notification IDs - only add new ones, don't remove old ones
                                        // This ensures that when you come back to the screen, already-seen notifications
                                        // won't trigger system notifications again
                                        previousNotificationIds.addAll(currentNotificationIds)
                                        android.util.Log.d("NotificationViewModel", "Updated previousNotificationIds size: ${previousNotificationIds.size}")
                                    }
                                    
                                    // Update state with fresh data from Firestore (includes read status)
                                    val unreadCount = notificationList.count { notification: Notification -> !notification.isRead }
                                    android.util.Log.d("NotificationViewModel", "Updating state - total: ${notificationList.size}, unread: $unreadCount")
                                    _notifications.value = notificationList
                                    _unreadCount.value = unreadCount
                                    _uiState.value = _uiState.value.copy(isLoading = false)
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
                android.util.Log.d("NotificationViewModel", "markAsRead called for notificationId: $notificationId")
                val currentNotifications = _notifications.value
                val notificationToMark = currentNotifications.find { it.id == notificationId }
                android.util.Log.d("NotificationViewModel", "Current notification state - isRead: ${notificationToMark?.isRead}, exists: ${notificationToMark != null}")
                
                val result = notificationRepository.markNotificationAsRead(notificationId)
                result.onSuccess {
                    android.util.Log.d("NotificationViewModel", "markNotificationAsRead succeeded in repository")
                    // Update local state
                    _notifications.value = _notifications.value.map { notification ->
                        if (notification.id == notificationId) {
                            android.util.Log.d("NotificationViewModel", "Updating local state - marking notification ${notification.id} as read")
                            notification.copy(isRead = true)
                        } else {
                            notification
                        }
                    }
                    
                    // Update unread count
                    val unread = _notifications.value.count { !it.isRead }
                    android.util.Log.d("NotificationViewModel", "Updated unread count: $unread")
                    _unreadCount.value = unread
                }.onFailure { exception ->
                    android.util.Log.e("NotificationViewModel", "markNotificationAsRead failed: ${exception.message}", exception)
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to mark notification as read: ${exception.message}"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("NotificationViewModel", "Exception in markAsRead: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to mark notification as read"
                )
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                android.util.Log.d("NotificationViewModel", "markAllAsRead called")
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { currentUser ->
                    if (currentUser != null) {
                        val currentNotifications = _notifications.value
                        val unreadCountBefore = currentNotifications.count { !it.isRead }
                        android.util.Log.d("NotificationViewModel", "Marking all as read - userId: ${currentUser.id}, unreadCount: $unreadCountBefore")
                        
                        val result = notificationRepository.markAllNotificationsAsRead(currentUser.id)
                        result.onSuccess {
                            android.util.Log.d("NotificationViewModel", "markAllNotificationsAsRead succeeded in repository")
                            // Update local state immediately
                            _notifications.value = _notifications.value.map { notification ->
                                android.util.Log.d("NotificationViewModel", "Updating local state - marking notification ${notification.id} as read")
                                notification.copy(isRead = true)
                            }
                            _unreadCount.value = 0
                            
                            // Update previousNotificationIds to include all current notifications
                            // This ensures they won't trigger system notifications when reloaded
                            previousNotificationIds = _notifications.value.map { it.id }.toMutableSet()
                            android.util.Log.d("NotificationViewModel", "Updated previousNotificationIds size: ${previousNotificationIds.size}")
                            
                            // The real-time listener will automatically update with the correct read status from Firestore
                        }.onFailure { exception ->
                            android.util.Log.e("NotificationViewModel", "markAllNotificationsAsRead failed: ${exception.message}", exception)
                            _uiState.value = _uiState.value.copy(
                                error = "Failed to mark all notifications as read: ${exception.message}"
                            )
                        }
                    } else {
                        android.util.Log.e("NotificationViewModel", "Current user is null")
                    }
                }.onFailure { exception ->
                    android.util.Log.e("NotificationViewModel", "Failed to get current user: ${exception.message}", exception)
                }
            } catch (e: Exception) {
                android.util.Log.e("NotificationViewModel", "Exception in markAllAsRead: ${e.message}", e)
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
