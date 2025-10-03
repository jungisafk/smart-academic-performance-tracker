package com.smartacademictracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Offline subject entity for local storage
 * Stores subject data locally for offline access
 */
@Entity(
    tableName = "offline_subjects",
    indices = [
        Index(value = ["code"], unique = true),
        Index(value = ["teacherId"]),
        Index(value = ["syncStatus"])
    ]
)
data class OfflineSubject(
    @PrimaryKey
    val id: String,
    val name: String,
    val code: String,
    val description: String = "",
    val credits: Int = 3,
    val teacherId: String,
    val teacherName: String,
    val yearLevelId: String? = null,
    val courseId: String? = null,
    val active: Boolean = true,
    val semester: String = "",
    val academicYear: String = "",
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val lastModified: Long = System.currentTimeMillis()
)
