package com.smartacademictracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import com.smartacademictracker.data.model.GradePeriod

/**
 * Offline grade entity for local storage
 * Stores grades locally when device is offline
 */
@Entity(
    tableName = "offline_grades",
    indices = [
        Index(value = ["studentId", "subjectId", "gradePeriod"], unique = true),
        Index(value = ["syncStatus"]),
        Index(value = ["lastModified"])
    ]
)
data class OfflineGrade(
    @PrimaryKey
    val id: String,
    val studentId: String,
    val studentName: String,
    val subjectId: String,
    val subjectName: String,
    val teacherId: String,
    val gradePeriod: GradePeriod,
    val score: Double,
    val maxScore: Double = 100.0,
    val percentage: Double,
    val letterGrade: String,
    val description: String = "",
    val dateRecorded: Long,
    val semester: String,
    val academicYear: String,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val lastModified: Long = System.currentTimeMillis(),
    // val conflictResolution: ConflictResolution? = null // Temporarily disabled
)

/**
 * Sync status for offline grades
 */
enum class SyncStatus {
    PENDING,        // Waiting to be synced
    SYNCING,        // Currently being synced
    SYNCED,         // Successfully synced
    FAILED,         // Sync failed
    CONFLICT        // Has conflicts that need resolution
}

