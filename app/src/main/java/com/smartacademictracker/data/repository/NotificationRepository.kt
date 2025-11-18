package com.smartacademictracker.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.smartacademictracker.data.model.Notification
import com.smartacademictracker.data.model.NotificationPreferences
import com.smartacademictracker.data.model.NotificationStats
import com.smartacademictracker.data.model.NotificationType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val notificationsCollection = firestore.collection("notifications")
    private val preferencesCollection = firestore.collection("notification_preferences")
    private val statsCollection = firestore.collection("notification_stats")
    private val fcmTokensCollection = firestore.collection("fcm_tokens")

    suspend fun createNotification(notification: Notification): Result<Notification> {
        return try {
            // Convert enum to string for Firestore rules compatibility
            val notificationMap = hashMapOf<String, Any?>(
                "userId" to notification.userId,
                "title" to notification.title,
                "message" to notification.message,
                "type" to notification.type.name, // Store enum as string
                "priority" to notification.priority.name, // Store enum as string
                "read" to notification.isRead,
                "delivered" to notification.isDelivered,
                "createdAt" to notification.createdAt,
                "readAt" to notification.readAt,
                "data" to notification.data,
                "actionUrl" to notification.actionUrl,
                "academicPeriodId" to notification.academicPeriodId
            )
            
            // Create document directly with set() instead of add() then set()
            // This ensures all fields including type are set correctly for rule evaluation
            val docRef = notificationsCollection.document()
            notificationMap["id"] = docRef.id
            docRef.set(notificationMap).await()
            val createdNotification = notification.copy(id = docRef.id)
            Result.success(createdNotification)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNotificationsByUserId(userId: String): Result<List<Notification>> {
        return try {
            val snapshot = notificationsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val notifications = snapshot.toObjects(Notification::class.java)
            // Fix any notifications that still have {message} placeholder
            val fixedNotifications = notifications.map { notification ->
                if (notification.message == "{message}" || notification.message.contains("{message}")) {
                    android.util.Log.w("NotificationRepository", "Found notification with {message} placeholder: ${notification.id}, type: ${notification.type}")
                    val defaultMessage = when (notification.type) {
                        com.smartacademictracker.data.model.NotificationType.GRADE_UPDATE -> 
                            "Your grade for ${notification.title.replace("Grade Update - ", "")} has been updated"
                        else -> notification.message.replace("{message}", "You have a new notification")
                    }
                    notification.copy(message = defaultMessage)
                } else {
                    notification
                }
            }
            // Sort in memory as temporary workaround while index builds
            val sortedNotifications = fixedNotifications.sortedByDescending { it.createdAt }
            Result.success(sortedNotifications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUnreadNotificationsByUserId(userId: String): Result<List<Notification>> {
        return try {
            android.util.Log.d("NotificationRepository", "getUnreadNotificationsByUserId called for userId: $userId")
            val snapshot = notificationsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("read", false)  // Firestore uses "read" field, not "isRead"
                .get()
                .await()
            val notifications = snapshot.toObjects(Notification::class.java)
            android.util.Log.d("NotificationRepository", "Found ${notifications.size} unread notifications")
            // Sort in memory as temporary workaround while index builds
            val sortedNotifications = notifications.sortedByDescending { it.createdAt }
            Result.success(sortedNotifications)
        } catch (e: Exception) {
            android.util.Log.e("NotificationRepository", "getUnreadNotificationsByUserId failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun markNotificationAsRead(notificationId: String): Result<Unit> {
        return try {
            android.util.Log.d("NotificationRepository", "markNotificationAsRead called for notificationId: $notificationId")
            val updateResult = notificationsCollection.document(notificationId).update(
                "read", true,  // Firestore uses "read" field, not "isRead"
                "readAt", Timestamp.now()
            ).await()
            android.util.Log.d("NotificationRepository", "markNotificationAsRead succeeded for notificationId: $notificationId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("NotificationRepository", "markNotificationAsRead failed for notificationId: $notificationId, error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun markAllNotificationsAsRead(userId: String): Result<Unit> {
        return try {
            android.util.Log.d("NotificationRepository", "markAllNotificationsAsRead called for userId: $userId")
            val snapshot = notificationsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("read", false)  // Firestore uses "read" field, not "isRead"
                .get()
                .await()

            android.util.Log.d("NotificationRepository", "Found ${snapshot.documents.size} unread notifications to mark as read")
            
            if (snapshot.documents.isEmpty()) {
                android.util.Log.d("NotificationRepository", "No unread notifications found")
                return Result.success(Unit)
            }

            val batch = firestore.batch()
            snapshot.documents.forEach { doc ->
                android.util.Log.d("NotificationRepository", "Adding notification ${doc.id} to batch update")
                batch.update(doc.reference, "read", true, "readAt", Timestamp.now())
            }
            val commitResult = batch.commit().await()
            android.util.Log.d("NotificationRepository", "markAllNotificationsAsRead batch commit succeeded")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("NotificationRepository", "markAllNotificationsAsRead failed for userId: $userId, error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return try {
            notificationsCollection.document(notificationId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAllNotifications(userId: String): Result<Unit> {
        return try {
            val snapshot = notificationsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val batch = firestore.batch()
            snapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNotificationPreferences(userId: String): Result<NotificationPreferences> {
        return try {
            val document = preferencesCollection.document(userId).get().await()
            if (document.exists()) {
                val preferences = document.toObject(NotificationPreferences::class.java)
                Result.success(preferences ?: NotificationPreferences(userId = userId))
            } else {
                // Create default preferences
                val defaultPreferences = NotificationPreferences(userId = userId)
                preferencesCollection.document(userId).set(defaultPreferences).await()
                Result.success(defaultPreferences)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateNotificationPreferences(preferences: NotificationPreferences): Result<Unit> {
        return try {
            preferencesCollection.document(preferences.userId).set(preferences).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNotificationStats(userId: String): Result<NotificationStats> {
        return try {
            val document = statsCollection.document(userId).get().await()
            if (document.exists()) {
                val stats = document.toObject(NotificationStats::class.java)
                Result.success(stats ?: NotificationStats(userId = userId))
            } else {
                val defaultStats = NotificationStats(userId = userId)
                statsCollection.document(userId).set(defaultStats).await()
                Result.success(defaultStats)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateNotificationStats(stats: NotificationStats): Result<Unit> {
        return try {
            statsCollection.document(stats.userId).set(stats).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateFCMToken(token: String): Result<Unit> {
        return try {
            // This would typically be called with a userId, but for now we'll use a generic approach
            fcmTokensCollection.document("current_token").set(
                mapOf(
                    "token" to token,
                    "updatedAt" to Timestamp.now()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNotificationsByType(userId: String, type: NotificationType): Result<List<Notification>> {
        return try {
            val snapshot = notificationsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", type.name)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            val notifications = snapshot.toObjects(Notification::class.java)
            Result.success(notifications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getNotificationsByUserIdFlow(userId: String): Flow<List<Notification>> = callbackFlow {
        android.util.Log.d("NotificationRepository", "getNotificationsByUserIdFlow called for userId: $userId")
        val listener = notificationsCollection
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("NotificationRepository", "Flow listener error: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    android.util.Log.d("NotificationRepository", "Flow listener received ${snapshot.documents.size} documents")
                    val notifications = snapshot.toObjects(Notification::class.java)
                    
                    // Log read status from Firestore for debugging
                    notifications.forEach { notification ->
                        // Check raw Firestore data to see what's actually stored
                        val rawDoc = snapshot.documents.find { it.id == notification.id }
                        val rawReadValue = rawDoc?.get("read")
                        android.util.Log.d("NotificationRepository", "Notification from Firestore - id: ${notification.id}, isRead property: ${notification.isRead}, raw 'read' field: $rawReadValue, title: ${notification.title}")
                    }
                    
                    // Fix any notifications that still have {message} placeholder
                    val fixedNotifications = notifications.map { notification ->
                        if (notification.message == "{message}" || notification.message.contains("{message}")) {
                            // Try to reconstruct the message from the template
                            android.util.Log.w("NotificationRepository", "Found notification with {message} placeholder: ${notification.id}, type: ${notification.type}")
                            // For now, use a default message based on type
                            val defaultMessage = when (notification.type) {
                                com.smartacademictracker.data.model.NotificationType.GRADE_UPDATE -> 
                                    "Your grade for ${notification.title.replace("Grade Update - ", "")} has been updated"
                                else -> notification.message.replace("{message}", "You have a new notification")
                            }
                            notification.copy(message = defaultMessage)
                        } else {
                            notification
                        }
                    }
                    android.util.Log.d("NotificationRepository", "Sending ${fixedNotifications.size} notifications through Flow")
                    trySend(fixedNotifications)
                }
            }
        awaitClose { 
            android.util.Log.d("NotificationRepository", "Flow listener closed")
            listener.remove() 
        }
    }

    suspend fun getRecentNotifications(userId: String, limit: Int = 10): Result<List<Notification>> {
        return try {
            val snapshot = notificationsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val notifications = snapshot.toObjects(Notification::class.java)
            // Sort in memory and limit as temporary workaround while index builds
            val sortedNotifications = notifications
                .sortedByDescending { it.createdAt }
                .take(limit)
            Result.success(sortedNotifications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
