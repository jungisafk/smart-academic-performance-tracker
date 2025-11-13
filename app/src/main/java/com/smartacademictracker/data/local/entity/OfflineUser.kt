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
    
    // ID-based authentication fields
    val studentId: String? = null,
    val teacherId: String? = null,
    val employeeId: String? = null,
    
    val firstName: String,
    val lastName: String,
    val middleName: String? = null,
    val suffix: String? = null,
    val role: String,
    val profileImageUrl: String? = null,
    val createdAt: Long,
    val active: Boolean = true,
    
    // Student-specific fields
    val yearLevelId: String? = null,
    val courseId: String? = null,
    val section: String? = null,
    val enrollmentYear: String? = null,
    val yearLevelName: String? = null,
    val courseName: String? = null,
    val courseCode: String? = null,
    
    // Teacher-specific fields
    val departmentCourseId: String? = null,
    val employmentType: String? = null,
    val position: String? = null,
    val specialization: String? = null,
    
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val lastModified: Long = System.currentTimeMillis()
)
