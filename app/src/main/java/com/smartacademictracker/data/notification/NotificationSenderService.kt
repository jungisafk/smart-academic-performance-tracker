package com.smartacademictracker.data.notification

import com.google.firebase.Timestamp
import com.smartacademictracker.data.model.Notification
import com.smartacademictracker.data.model.NotificationType
import com.smartacademictracker.data.model.NotificationPriority
import com.smartacademictracker.data.repository.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationSenderService @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val localNotificationService: LocalNotificationService,
    private val templateService: NotificationTemplateService
) {

    fun sendGradeUpdateNotification(
        userId: String,
        subjectName: String,
        grade: String,
        period: String,
        score: Double,
        maxScore: Double
    ) {
        val variables = mapOf(
            "subjectName" to subjectName,
            "gradePeriod" to period,
            "score" to score.toString(),
            "maxScore" to maxScore.toString(),
            "percentage" to String.format("%.1f", (score / maxScore) * 100)
        )

        sendNotification(
            userId = userId,
            type = NotificationType.GRADE_UPDATE,
            variables = variables,
            priority = NotificationPriority.HIGH
        )
    }

    fun sendApplicationStatusNotification(
        userId: String,
        applicationType: String,
        status: String,
        subjectName: String? = null,
        reason: String? = null
    ) {
        val variables = mutableMapOf(
            "applicationType" to applicationType,
            "subjectName" to (subjectName ?: ""),
            "reason" to (reason ?: "")
        )

        val type = when {
            status == "approved" && applicationType.contains("Teacher", ignoreCase = true) -> NotificationType.TEACHER_APPLICATION_APPROVED
            status == "rejected" && applicationType.contains("Teacher", ignoreCase = true) -> NotificationType.TEACHER_APPLICATION_REJECTED
            status == "approved" && applicationType.contains("Subject", ignoreCase = true) -> NotificationType.SUBJECT_APPLICATION_APPROVED
            status == "rejected" && applicationType.contains("Subject", ignoreCase = true) -> NotificationType.SUBJECT_APPLICATION_REJECTED
            status == "approved" -> NotificationType.APPLICATION_APPROVED
            else -> NotificationType.APPLICATION_REJECTED
        }

        sendNotification(
            userId = userId,
            type = type,
            variables = variables,
            priority = NotificationPriority.HIGH
        )
    }

    fun sendDeadlineReminderNotification(
        userId: String,
        deadlineType: String,
        deadlineDate: String,
        subjectName: String? = null
    ) {
        val variables = mapOf(
            "deadlineType" to deadlineType,
            "subjectName" to (subjectName ?: ""),
            "deadlineDate" to deadlineDate
        )

        sendNotification(
            userId = userId,
            type = NotificationType.DEADLINE_REMINDER,
            variables = variables,
            priority = NotificationPriority.NORMAL
        )
    }

    fun sendSystemAnnouncementNotification(
        userId: String,
        title: String,
        message: String
    ) {
        val variables = mapOf(
            "title" to title,
            "message" to message
        )

        sendNotification(
            userId = userId,
            type = NotificationType.SYSTEM_ANNOUNCEMENT,
            variables = variables,
            priority = NotificationPriority.NORMAL
        )
    }

    fun sendPerformanceAlertNotification(
        userId: String,
        subjectName: String,
        performance: String
    ) {
        val variables = mapOf(
            "subjectName" to subjectName,
            "performance" to performance
        )

        sendNotification(
            userId = userId,
            type = NotificationType.PERFORMANCE_ALERT,
            variables = variables,
            priority = NotificationPriority.HIGH
        )
    }

    fun sendGradeSubmissionDeadlineNotification(
        userId: String,
        subjectName: String,
        deadlineDate: String
    ) {
        val variables = mapOf(
            "subjectName" to subjectName,
            "deadlineDate" to deadlineDate
        )

        sendNotification(
            userId = userId,
            type = NotificationType.GRADE_SUBMISSION_DEADLINE,
            variables = variables,
            priority = NotificationPriority.NORMAL
        )
    }

    fun sendAcademicPeriodActivatedNotification(
        userId: String,
        periodName: String
    ) {
        val variables = mapOf(
            "periodName" to periodName
        )

        sendNotification(
            userId = userId,
            type = NotificationType.ACADEMIC_PERIOD_ACTIVATED,
            variables = variables,
            priority = NotificationPriority.NORMAL
        )
    }

    fun sendNotification(
        userId: String,
        type: NotificationType,
        variables: Map<String, String>,
        priority: NotificationPriority = NotificationPriority.NORMAL
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Create notification from template
                val notification = templateService.createNotificationFromTemplate(
                    type = type,
                    userId = userId,
                    variables = variables,
                    priority = priority
                )

                // Save to database
                val result = notificationRepository.createNotification(notification)
                result.fold(
                    onSuccess = { savedNotification ->
                        // Show local notification
                        showLocalNotification(savedNotification)
                    },
                    onFailure = { error ->
                        // Handle error silently or log it
                        println("Failed to save notification: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                // Handle error silently or log it
                println("Failed to send notification: ${e.message}")
            }
        }
    }

    private fun showLocalNotification(notification: Notification) {
        localNotificationService.showNotification(
            title = notification.title,
            message = notification.message,
            type = notification.type,
            priority = notification.priority,
            actionUrl = notification.actionUrl,
            data = notification.data
        )
    }

    fun sendBulkNotification(
        userIds: List<String>,
        type: NotificationType,
        variables: Map<String, String>,
        priority: NotificationPriority = NotificationPriority.NORMAL
    ) {
        userIds.forEach { userId ->
            sendNotification(userId, type, variables, priority)
        }
    }

    fun sendNotificationToAllUsers(
        type: NotificationType,
        variables: Map<String, String>,
        priority: NotificationPriority = NotificationPriority.NORMAL
    ) {
        // This would typically get all user IDs from the database
        // For now, we'll implement a placeholder
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get all user IDs from the database
                // This is a placeholder - in a real implementation,
                // you would query the users collection
                val allUserIds = listOf<String>() // Placeholder
                
                allUserIds.forEach { userId ->
                    sendNotification(userId, type, variables, priority)
                }
            } catch (e: Exception) {
                println("Failed to send bulk notification: ${e.message}")
            }
        }
    }
}
