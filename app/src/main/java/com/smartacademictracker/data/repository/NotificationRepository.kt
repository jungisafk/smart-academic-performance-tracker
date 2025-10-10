package com.smartacademictracker.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.smartacademictracker.data.model.Notification
import com.smartacademictracker.data.model.NotificationPreferences
import com.smartacademictracker.data.model.NotificationStats
import com.smartacademictracker.data.model.NotificationType
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
            val docRef = notificationsCollection.add(notification).await()
            val createdNotification = notification.copy(id = docRef.id)
            notificationsCollection.document(docRef.id).set(createdNotification).await()
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
            // Sort in memory as temporary workaround while index builds
            val sortedNotifications = notifications.sortedByDescending { it.createdAt }
            Result.success(sortedNotifications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUnreadNotificationsByUserId(userId: String): Result<List<Notification>> {
        return try {
            val snapshot = notificationsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .await()
            val notifications = snapshot.toObjects(Notification::class.java)
            // Sort in memory as temporary workaround while index builds
            val sortedNotifications = notifications.sortedByDescending { it.createdAt }
            Result.success(sortedNotifications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markNotificationAsRead(notificationId: String): Result<Unit> {
        return try {
            notificationsCollection.document(notificationId).update(
                "isRead", true,
                "readAt", Timestamp.now()
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAllNotificationsAsRead(userId: String): Result<Unit> {
        return try {
            val snapshot = notificationsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .await()

            val batch = firestore.batch()
            snapshot.documents.forEach { doc ->
                batch.update(doc.reference, "isRead", true, "readAt", Timestamp.now())
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
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
