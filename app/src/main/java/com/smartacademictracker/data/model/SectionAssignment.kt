package com.smartacademictracker.data.model

import com.google.firebase.firestore.DocumentId

data class SectionAssignment(
    @DocumentId
    val id: String = "",
    val subjectId: String = "", // Reference to Subject document ID
    val sectionName: String = "", // e.g., "IT101A", "IT101B"
    val teacherId: String = "", // Reference to User document ID (teacher)
    val teacherName: String = "", // Teacher's name for display
    val teacherEmail: String = "", // Teacher's email
    val assignedAt: Long = System.currentTimeMillis(), // When the assignment was made
    val assignedBy: String = "", // Admin user ID who made the assignment
    val assignedByName: String = "", // Admin's name who made the assignment
    val status: AssignmentStatus = AssignmentStatus.ACTIVE, // Assignment status
    val academicPeriodId: String = "", // Reference to active academic period
    val notes: String = "" // Optional notes about the assignment
)

enum class AssignmentStatus(val displayName: String) {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    TERMINATED("Terminated")
}
