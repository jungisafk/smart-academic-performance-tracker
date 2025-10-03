package com.smartacademictracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Offline user entity for local storage
 * Stores user data locally for offline access
 */
@Entity(
    tableName = "offline_users",
    indices = [
        Index(value = ["email"], unique = true),
        Index(value = ["role"]),
        Index(value = ["syncStatus"])
    ]
)
data class OfflineUser(
    @PrimaryKey
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String,
    val profileImageUrl: String? = null,
    val createdAt: Long,
    val active: Boolean = true,
    val yearLevelId: String? = null,
    val courseId: String? = null,
    val yearLevelName: String? = null,
    val courseName: String? = null,
    val courseCode: String? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val lastModified: Long = System.currentTimeMillis()
)
