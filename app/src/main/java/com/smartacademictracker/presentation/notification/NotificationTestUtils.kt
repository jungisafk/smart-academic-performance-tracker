package com.smartacademictracker.presentation.notification

import com.google.firebase.Timestamp
import com.smartacademictracker.data.model.Notification
import com.smartacademictracker.data.model.NotificationType
import com.smartacademictracker.data.model.NotificationPriority
import com.smartacademictracker.data.repository.NotificationRepository
import com.smartacademictracker.data.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationTestUtils @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository
) {
    
    fun createTestNotifications(coroutineScope: CoroutineScope, userId: String) {
        coroutineScope.launch {
            val testNotifications = listOf(
                Notification(
                    id = "",
                    userId = userId,
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
                    userId = userId,
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
                    userId = userId,
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
                    userId = userId,
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
                    userId = userId,
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
                    userId = userId,
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
        }
    }
}
