package com.smartacademictracker.data.model

import com.google.firebase.firestore.DocumentId

data class StudentApplication(
    @DocumentId
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val studentEmail: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val subjectCode: String = "",
    val applicationReason: String = "",
    val status: StudentApplicationStatus = StudentApplicationStatus.PENDING,
    val appliedAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long? = null,
    val reviewedBy: String? = null, // Teacher ID who reviewed
    val teacherComments: String? = null,
    val yearLevelId: String = "",
    val courseId: String = "",
    // Computed fields for display (populated by repository)
    val yearLevelName: String = "",
    val courseName: String = "",
    val courseCode: String = ""
)

enum class StudentApplicationStatus {
    PENDING,
    APPROVED,
    REJECTED
}
