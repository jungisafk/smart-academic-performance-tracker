package com.smartacademictracker.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.smartacademictracker.R
import com.smartacademictracker.data.model.Grade
import com.smartacademictracker.data.model.GradePeriod
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GradeNotificationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val CHANNEL_ID = "grade_notifications"
        private const val CHANNEL_NAME = "Grade Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for grade updates"
        private const val NOTIFICATION_ID_BASE = 1000
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
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun showGradeUpdateNotification(grade: Grade) {
        if (!notificationManager.areNotificationsEnabled()) {
            return
        }
        
        val intent = Intent(context, com.smartacademictracker.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Grade Update")
            .setContentText("New ${grade.gradePeriod.displayName} grade for ${grade.subjectName}: ${String.format("%.1f", grade.percentage)}%")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("You received a ${grade.gradePeriod.displayName} grade of ${grade.score}/${grade.maxScore} (${String.format("%.1f", grade.percentage)}%) in ${grade.subjectName}"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        val notificationId = NOTIFICATION_ID_BASE + grade.hashCode()
        try {
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Handle permission denied gracefully
        }
    }
    
    fun showMultipleGradesNotification(grades: List<Grade>) {
        if (grades.isEmpty() || !notificationManager.areNotificationsEnabled()) return
        
        val intent = Intent(context, com.smartacademictracker.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val subjects = grades.map { it.subjectName }.distinct()
        val contentText = if (subjects.size == 1) {
            "New grades for ${subjects.first()}"
        } else {
            "New grades for ${subjects.size} subjects"
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Multiple Grade Updates")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(grades.joinToString("\n") { 
                    "${it.subjectName}: ${it.gradePeriod.displayName} - ${String.format("%.1f", it.percentage)}%" 
                }))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        try {
            notificationManager.notify(NOTIFICATION_ID_BASE + 1, notification)
        } catch (e: SecurityException) {
            // Handle permission denied gracefully
        }
    }
    
    fun showGradeReminderNotification(subjectName: String, period: GradePeriod) {
        if (!notificationManager.areNotificationsEnabled()) return
        
        val intent = Intent(context, com.smartacademictracker.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Grade Reminder")
            .setContentText("Check your ${period.displayName} grade for ${subjectName}")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        try {
            notificationManager.notify(NOTIFICATION_ID_BASE + 2, notification)
        } catch (e: SecurityException) {
            // Handle permission denied gracefully
        }
    }
    
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
}
