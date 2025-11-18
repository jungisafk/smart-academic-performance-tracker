package com.smartacademictracker.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class User(
    @DocumentId
    val id: String = "",
    val email: String = "",
    
    // ID-based authentication fields
    val studentId: String? = null,      // For students: e.g., "2024-1234"
    val teacherId: String? = null,      // For teachers: e.g., "T-2024-001"
    val employeeId: String? = null,     // Alternative employee identifier for teachers
    
    // Personal information
    val firstName: String = "",
    val lastName: String = "",
    val middleName: String? = null,
    val suffix: String? = null,         // Jr., Sr., III, etc.
    val role: String = "STUDENT",       // Store as string for faster serialization
    val profileImageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val active: Boolean = true,         // Use 'active' directly to match Firestore
    
    // Student-specific fields
    val yearLevelId: String? = null,    // For students: Reference to YearLevel document ID
    val courseId: String? = null,       // For students: Reference to Course document ID
    val section: String? = null,        // Student section (e.g., "A", "B", "1A")
    val enrollmentYear: String? = null, // Year enrolled (e.g., "2024-2025")
    val lastAcademicPeriodId: String? = null, // Last academic period the student was in (for year level progression)
    
    // Teacher-specific fields
    val departmentCourseId: String? = null, // For teachers: Reference to Course document ID (department they teach in)
    val employmentType: String? = null, // Full-time, Part-time, Contract, etc.
    val position: String? = null,       // Professor, Associate Professor, Instructor, etc.
    val specialization: String? = null, // Field of expertise
    val dateHired: String? = null,      // Format: "YYYY-MM-DD"
    
    // Computed fields for display (populated by repository)
    val yearLevelName: String? = null,
    val courseName: String? = null,
    val courseCode: String? = null,
    val departmentCourseName: String? = null, // For teachers: Department name
    val departmentCourseCode: String? = null, // For teachers: Department code
    
    // Security and audit
    val lastLoginAt: Long? = null,
    val passwordChangedAt: Long? = null,
    val mustChangePassword: Boolean = false,
    val accountSource: String = "MANUAL", // "MANUAL" or "PRE_REGISTERED"
    val actualEmail: String? = null // Actual email from CSV/pre-registration (for password reset)
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
