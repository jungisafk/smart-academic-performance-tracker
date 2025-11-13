package com.smartacademictracker.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Pre-registered student data model
 * Used for institutional student pre-registration before account activation
 * Admins populate this collection, students activate their accounts using their student ID
 */
data class PreRegisteredStudent(
    @DocumentId
    val id: String = "",
    
    // Unique student identification
        val studentId: String = "",           // e.g., "2024-1234"
    
    // Personal information
    val firstName: String = "",
    val lastName: String = "",
    val middleName: String? = null,
    val suffix: String? = null,           // Jr., Sr., III, etc.
    
    // Academic information
    val courseId: String = "",            // Reference to Course document ID
    val courseName: String = "",          // Denormalized for display
    val courseCode: String = "",          // Denormalized for display
    val yearLevelId: String = "",         // Reference to YearLevel document ID
    val yearLevelName: String = "",       // Denormalized for display
    val section: String? = null,          // Student section (e.g., "A", "B", "1A")
    val enrollmentYear: String = "",      // Academic year enrolled (e.g., "2024-2025")
    
    // Optional contact information
    val email: String? = null,            // Optional institutional email
    val phoneNumber: String? = null,
    val dateOfBirth: String? = null,      // Format: "YYYY-MM-DD"
    val address: String? = null,
    
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
    val notes: String = "",               // Admin notes about the student
    
    // Status flags
    val active: Boolean = true,           // Can be deactivated without deletion
    val emailSent: Boolean = false,       // Track if welcome email was sent
    val emailSentAt: Long? = null
)

