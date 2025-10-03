package com.smartacademictracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Offline enrollment entity for local storage
 * Stores enrollment data locally for offline access
 */
@Entity(
    tableName = "offline_enrollments",
    indices = [
        Index(value = ["studentId", "subjectId"], unique = true),
        Index(value = ["syncStatus"])
    ]
)
data class OfflineEnrollment(
    @PrimaryKey
    val id: String,
    val studentId: String,
    val studentName: String,
    val subjectId: String,
    val subjectName: String,
    val subjectCode: String,
    val teacherId: String,
    val teacherName: String,
    val semester: String,
    val academicYear: String,
    val enrollmentDate: Long,
    val active: Boolean = true,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val lastModified: Long = System.currentTimeMillis()
)
