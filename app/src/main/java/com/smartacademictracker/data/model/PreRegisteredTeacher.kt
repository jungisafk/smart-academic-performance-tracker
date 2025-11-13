package com.smartacademictracker.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Pre-registered teacher data model
 * Used for institutional teacher pre-registration before account activation
 * Admins populate this collection, teachers activate their accounts using their teacher ID
 */
data class PreRegisteredTeacher(
    @DocumentId
    val id: String = "",
    
    // Unique teacher identification
    val teacherId: String = "",           // e.g., "T-2024-001" or "EMP-12345"
    
    // Personal information
    val firstName: String = "",
    val lastName: String = "",
    val middleName: String? = null,
    val suffix: String? = null,           // Jr., Sr., III, etc.
    
    // Academic/Department information
    val departmentCourseId: String = "",  // Reference to Course document ID (their department)
    val departmentCourseName: String = "", // Denormalized for display
    val departmentCourseCode: String = "", // Denormalized for display
    val employmentType: EmploymentType = EmploymentType.FULL_TIME,
    val position: String? = null,         // e.g., "Professor", "Associate Professor", "Instructor"
    val specialization: String? = null,   // Field of expertise
    
    // Optional contact information
    val email: String? = null,            // Optional institutional email
    val phoneNumber: String? = null,
    val dateOfBirth: String? = null,      // Format: "YYYY-MM-DD"
    val address: String? = null,
    
    // Employment details
    val dateHired: String? = null,        // Format: "YYYY-MM-DD"
    val employeeNumber: String? = null,   // Alternative employee identifier
    
    // Registration status
    @PropertyName("isRegistered")
    val isRegistered: Boolean = false,    // Marks if account is activated
    val registeredAt: Long? = null,       // Timestamp when account was activated
    val firebaseUserId: String? = null,   // Firebase Auth UID after activation
    
    // Audit trail
    val createdBy: String = "",           // Admin user ID who added this record
    val createdByName: String = "",       // Admin name for display
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String? = null,
    val notes: String = "",               // Admin notes about the teacher
    
    // Status flags
    val active: Boolean = true,           // Can be deactivated without deletion
    val emailSent: Boolean = false,       // Track if welcome email was sent
    val emailSentAt: Long? = null
)

/**
 * Employment type for teachers
 */
enum class EmploymentType(val displayName: String) {
    FULL_TIME("Full-time"),
    PART_TIME("Part-time"),
    CONTRACT("Contract"),
    TEMPORARY("Temporary"),
    ADJUNCT("Adjunct"),
    VISITING("Visiting")
}

