package com.smartacademictracker.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.smartacademictracker.MainActivity
import com.smartacademictracker.R
import com.smartacademictracker.data.model.Notification
import com.smartacademictracker.data.model.NotificationType
import com.smartacademictracker.data.model.NotificationPriority
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalNotificationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val CHANNEL_ID = "local_notifications"
        private const val CHANNEL_NAME = "Local Notifications"
        private const val CHANNEL_DESCRIPTION = "Local app notifications"
        private const val NOTIFICATION_ID_BASE = 3000
    }
    
    private val notificationManager = NotificationManagerCompat.from(context)
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
            }
            
            val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            systemNotificationManager.createNotificationChannel(channel)
        }
    }
    
    fun showNotification(
        title: String,
        message: String,
        type: NotificationType = NotificationType.GENERAL,
        priority: NotificationPriority = NotificationPriority.NORMAL,
        actionUrl: String? = null,
        data: Map<String, String> = emptyMap()
    ) {
        if (!notificationManager.areNotificationsEnabled()) {
            return
        }
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            actionUrl?.let { putExtra("action_url", it) }
            data.forEach { (key, value) -> putExtra(key, value) }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            type.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(getNotificationIcon(type))
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(getNotificationPriority(priority))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(getNotificationCategory(type))
            .build()
        
        val notificationId = NOTIFICATION_ID_BASE + type.hashCode()
        try {
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Handle permission denied gracefully
        }
    }
    
    fun showGradeUpdateNotification(
        subjectName: String,
        grade: String,
        period: String
    ) {
        showNotification(
            title = "Grade Update",
            message = "New $period grade for $subjectName: $grade",
            type = NotificationType.GRADE_UPDATE,
            priority = NotificationPriority.HIGH
        )
    }
    
    fun showApplicationStatusNotification(
        applicationType: String,
        status: String,
        subjectName: String? = null
    ) {
        val message = if (subjectName != null) {
            "Your $applicationType application for $subjectName has been $status"
        } else {
            "Your $applicationType application has been $status"
        }
        
        showNotification(
            title = "Application $status",
            message = message,
            type = if (status == "approved") NotificationType.APPLICATION_APPROVED else NotificationType.APPLICATION_REJECTED,
            priority = NotificationPriority.HIGH
        )
    }
    
    fun showDeadlineReminderNotification(
        deadlineType: String,
        deadlineDate: String,
        subjectName: String? = null
    ) {
        val message = if (subjectName != null) {
            "$deadlineType deadline for $subjectName is on $deadlineDate"
        } else {
            "$deadlineType deadline is on $deadlineDate"
        }
        
        showNotification(
            title = "Deadline Reminder",
            message = message,
            type = NotificationType.DEADLINE_REMINDER,
            priority = NotificationPriority.NORMAL
        )
    }
    
    fun showSystemAnnouncementNotification(
        title: String,
        message: String
    ) {
        showNotification(
            title = title,
            message = message,
            type = NotificationType.SYSTEM_ANNOUNCEMENT,
            priority = NotificationPriority.NORMAL
        )
    }
    
    fun showPerformanceAlertNotification(
        subjectName: String,
        performance: String
    ) {
        showNotification(
            title = "Performance Alert",
            message = "Your performance in $subjectName is $performance. Consider seeking help.",
            type = NotificationType.PERFORMANCE_ALERT,
            priority = NotificationPriority.HIGH
        )
    }
    
    private fun getNotificationIcon(type: NotificationType): Int {
        return when (type) {
            NotificationType.GRADE_UPDATE -> R.drawable.ic_grade
            NotificationType.APPLICATION_APPROVED -> R.drawable.ic_check
            NotificationType.APPLICATION_REJECTED -> R.drawable.ic_cancel
            NotificationType.DEADLINE_REMINDER -> R.drawable.ic_schedule
            NotificationType.SYSTEM_ANNOUNCEMENT -> R.drawable.ic_announcement
            NotificationType.PERFORMANCE_ALERT -> R.drawable.ic_warning
            else -> R.drawable.ic_notification
        }
    }
    
    private fun getNotificationPriority(priority: NotificationPriority): Int {
        return when (priority) {
            NotificationPriority.LOW -> NotificationCompat.PRIORITY_LOW
            NotificationPriority.NORMAL -> NotificationCompat.PRIORITY_DEFAULT
            NotificationPriority.HIGH -> NotificationCompat.PRIORITY_HIGH
            NotificationPriority.URGENT -> NotificationCompat.PRIORITY_MAX
        }
    }
    
    private fun getNotificationCategory(type: NotificationType): String {
        return when (type) {
            NotificationType.GRADE_UPDATE -> NotificationCompat.CATEGORY_STATUS
            NotificationType.APPLICATION_APPROVED,
            NotificationType.APPLICATION_REJECTED -> NotificationCompat.CATEGORY_SOCIAL
            NotificationType.DEADLINE_REMINDER -> NotificationCompat.CATEGORY_REMINDER
            NotificationType.SYSTEM_ANNOUNCEMENT -> NotificationCompat.CATEGORY_SOCIAL
            NotificationType.PERFORMANCE_ALERT -> NotificationCompat.CATEGORY_STATUS
            else -> NotificationCompat.CATEGORY_MESSAGE
        }
    }
    
    fun cancelNotification(type: NotificationType) {
        val notificationId = NOTIFICATION_ID_BASE + type.hashCode()
        notificationManager.cancel(notificationId)
    }
    
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
}
