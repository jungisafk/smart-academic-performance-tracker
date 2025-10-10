package com.smartacademictracker.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.smartacademictracker.MainActivity
import com.smartacademictracker.R
import com.smartacademictracker.data.model.Notification
import com.smartacademictracker.data.model.NotificationType
import com.smartacademictracker.data.repository.NotificationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationRepository: NotificationRepository

    @Inject
    lateinit var localNotificationService: LocalNotificationService

    companion object {
        private const val CHANNEL_ID = "fcm_notifications"
        private const val CHANNEL_NAME = "FCM Notifications"
        private const val CHANNEL_DESCRIPTION = "Push notifications from Firebase"
        private const val NOTIFICATION_ID_BASE = 2000
    }

    override fun onCreate() {
        super.onCreate()
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

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Handle data payload
        remoteMessage.data.let { data ->
            if (data.isNotEmpty()) {
                handleDataMessage(data)
            }
        }

        // Handle notification payload
        remoteMessage.notification?.let { notification ->
            handleNotificationMessage(notification.title, notification.body, remoteMessage.data)
        }
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val title = data["title"] ?: "New Notification"
        val message = data["message"] ?: ""
        val type = data["type"]?.let { NotificationType.valueOf(it) } ?: NotificationType.GENERAL
        val userId = data["userId"] ?: ""
        val actionUrl = data["actionUrl"]

        // Create local notification
        showLocalNotification(title, message, type, actionUrl)

        // Save to local database
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notification = Notification(
                    userId = userId,
                    title = title,
                    message = message,
                    type = type,
                    isDelivered = true,
                    data = data,
                    actionUrl = actionUrl
                )
                notificationRepository.createNotification(notification)
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    private fun handleNotificationMessage(title: String?, body: String?, data: Map<String, String>) {
        val notificationTitle = title ?: "New Notification"
        val notificationBody = body ?: ""
        val type = data["type"]?.let { NotificationType.valueOf(it) } ?: NotificationType.GENERAL
        val actionUrl = data["actionUrl"]

        showLocalNotification(notificationTitle, notificationBody, type, actionUrl)
    }

    private fun showLocalNotification(
        title: String,
        message: String,
        type: NotificationType,
        actionUrl: String?
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            actionUrl?.let { putExtra("action_url", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(getNotificationPriority(type))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationId = NOTIFICATION_ID_BASE + type.hashCode()
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Handle permission denied gracefully
        }
    }

    private fun getNotificationPriority(type: NotificationType): Int {
        return when (type) {
            NotificationType.GRADE_UPDATE,
            NotificationType.APPLICATION_APPROVED,
            NotificationType.APPLICATION_REJECTED -> NotificationCompat.PRIORITY_HIGH
            NotificationType.DEADLINE_REMINDER,
            NotificationType.GRADE_SUBMISSION_DEADLINE -> NotificationCompat.PRIORITY_DEFAULT
            NotificationType.SYSTEM_ANNOUNCEMENT,
            NotificationType.PERFORMANCE_ALERT -> NotificationCompat.PRIORITY_DEFAULT
            else -> NotificationCompat.PRIORITY_LOW
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        
        // Send token to server
        CoroutineScope(Dispatchers.IO).launch {
            try {
                notificationRepository.updateFCMToken(token)
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
}
