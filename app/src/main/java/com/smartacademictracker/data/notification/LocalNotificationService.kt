package com.smartacademictracker.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
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
        private val DEFAULT_VIBRATION_PATTERN = longArrayOf(0, 250, 250, 250)
        private val HIGH_PRIORITY_VIBRATION_PATTERN = longArrayOf(0, 500, 200, 500)
    }
    
    private val notificationManager = NotificationManagerCompat.from(context)
    private val defaultSoundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    
    init {
        Log.d("LocalNotificationService", "Initializing LocalNotificationService")
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        Log.d("LocalNotificationService", "createNotificationChannel called, SDK_INT: ${Build.VERSION.SDK_INT}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                // Create channel with HIGH importance to ensure sound and visibility
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESCRIPTION
                    enableVibration(true)
                    vibrationPattern = HIGH_PRIORITY_VIBRATION_PATTERN
                    setSound(defaultSoundUri, null)
                    enableLights(true)
                    setShowBadge(true)
                }
                
                val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                systemNotificationManager.createNotificationChannel(channel)
                Log.d("LocalNotificationService", "Notification channel created successfully: $CHANNEL_ID")
                
                // Verify channel was created
                val createdChannel = systemNotificationManager.getNotificationChannel(CHANNEL_ID)
                if (createdChannel != null) {
                    Log.d("LocalNotificationService", "Channel verified - Importance: ${createdChannel.importance}, Sound: ${createdChannel.sound}, Vibration: ${createdChannel.shouldVibrate()}")
                } else {
                    Log.e("LocalNotificationService", "ERROR: Channel was not created properly!")
                }
            } catch (e: Exception) {
                Log.e("LocalNotificationService", "ERROR creating notification channel: ${e.message}", e)
            }
        } else {
            Log.d("LocalNotificationService", "Android version < O, skipping channel creation")
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
        Log.d("LocalNotificationService", "showNotification called - Title: $title, Type: $type, Priority: $priority")
        
        val areNotificationsEnabled = notificationManager.areNotificationsEnabled()
        Log.d("LocalNotificationService", "Notifications enabled: $areNotificationsEnabled")
        
        if (!areNotificationsEnabled) {
            Log.w("LocalNotificationService", "Notifications are DISABLED - notification will not be shown")
            return
        }
        
        try {
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
            Log.d("LocalNotificationService", "PendingIntent created successfully")
            
            val iconResId = getNotificationIcon(type)
            Log.d("LocalNotificationService", "Notification icon: $iconResId")
            
            // Verify icon resource exists
            try {
                val iconDrawable = context.getDrawable(iconResId)
                if (iconDrawable == null) {
                    Log.e("LocalNotificationService", "ERROR: Icon resource $iconResId does not exist! Using default icon.")
                }
            } catch (e: Exception) {
                Log.e("LocalNotificationService", "ERROR: Failed to load icon resource $iconResId: ${e.message}")
            }
            
            val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(iconResId)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(getNotificationPriority(priority))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setCategory(getNotificationCategory(type))
                .setDefaults(NotificationCompat.DEFAULT_ALL) // Enable sound, vibration, and lights
                .setSound(defaultSoundUri) // Explicitly set sound
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on lock screen
            
            // Add vibration based on priority
            when (priority) {
                NotificationPriority.HIGH, NotificationPriority.URGENT -> {
                    notificationBuilder.setVibrate(HIGH_PRIORITY_VIBRATION_PATTERN)
                    Log.d("LocalNotificationService", "High priority vibration pattern set")
                }
                NotificationPriority.NORMAL -> {
                    notificationBuilder.setVibrate(DEFAULT_VIBRATION_PATTERN)
                    Log.d("LocalNotificationService", "Normal priority vibration pattern set")
                }
                NotificationPriority.LOW -> {
                    Log.d("LocalNotificationService", "Low priority - no vibration")
                }
            }
            
            val notification = notificationBuilder.build()
            Log.d("LocalNotificationService", "Notification built successfully")
            
            val notificationId = NOTIFICATION_ID_BASE + type.hashCode()
            Log.d("LocalNotificationService", "Posting notification with ID: $notificationId, Channel: $CHANNEL_ID")
            
            // Verify channel exists before posting
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channel = systemNotificationManager.getNotificationChannel(CHANNEL_ID)
                if (channel == null) {
                    Log.e("LocalNotificationService", "ERROR: Notification channel does not exist! Recreating...")
                    createNotificationChannel()
                } else {
                    Log.d("LocalNotificationService", "Channel verified - Importance: ${channel.importance}")
                }
            }
            
            notificationManager.notify(notificationId, notification)
            Log.d("LocalNotificationService", "Notification posted successfully with ID: $notificationId")
        } catch (e: SecurityException) {
            Log.e("LocalNotificationService", "SecurityException posting notification: ${e.message}", e)
        } catch (e: Exception) {
            Log.e("LocalNotificationService", "Exception posting notification: ${e.message}", e)
            e.printStackTrace()
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
