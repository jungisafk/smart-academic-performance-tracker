package com.smartacademictracker.data.model

import com.google.firebase.firestore.DocumentId

data class AcademicPeriod(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val semester: String = "", // e.g., "Fall", "Spring", "Summer"
    val academicYear: String = "", // e.g., "2024-2025"
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = System.currentTimeMillis(),
    val isCurrent: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "" // Admin user ID who created this period
) {
    /**
     * Get display name for the academic period
     */
    fun getDisplayName(): String {
        return "$semester $academicYear"
    }
    
    /**
     * Check if the period is currently active (between start and end dates)
     */
    fun isActive(): Boolean {
        val currentTime = System.currentTimeMillis()
        return currentTime >= startDate && currentTime <= endDate
    }
    
    /**
     * Get the duration in days
     */
    fun getDurationInDays(): Long {
        return (endDate - startDate) / (24 * 60 * 60 * 1000)
    }
    
    /**
     * Get progress percentage (0.0 to 1.0) based on current date
     */
    fun getProgressPercentage(): Double {
        val currentTime = System.currentTimeMillis()
        val totalDuration = endDate - startDate
        val elapsed = currentTime - startDate
        
        return when {
            elapsed <= 0 -> 0.0
            elapsed >= totalDuration -> 1.0
            else -> elapsed.toDouble() / totalDuration.toDouble()
        }
    }
}
