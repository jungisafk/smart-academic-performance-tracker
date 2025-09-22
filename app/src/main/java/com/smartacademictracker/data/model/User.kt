package com.smartacademictracker.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class User(
    @DocumentId
    val id: String = "",
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val role: String = "STUDENT", // Store as string for faster serialization
    val profileImageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val active: Boolean = true, // Use 'active' directly to match Firestore
    val yearLevelId: String? = null, // For students: Reference to YearLevel document ID
    val courseId: String? = null, // For students: Reference to Course document ID
    // Computed fields for display (populated by repository)
    val yearLevelName: String? = null,
    val courseName: String? = null,
    val courseCode: String? = null
)

enum class UserRole {
    STUDENT,
    TEACHER,
    ADMIN;
    
    val value: String
        get() = this.name
    
    companion object {
        fun fromString(value: String): UserRole {
            return when (value.uppercase()) {
                "STUDENT" -> STUDENT
                "TEACHER" -> TEACHER
                "ADMIN" -> ADMIN
                else -> STUDENT
            }
        }
    }
}
