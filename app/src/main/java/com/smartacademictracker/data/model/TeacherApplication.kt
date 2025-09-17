package com.smartacademictracker.data.model

import com.google.firebase.firestore.DocumentId

data class TeacherApplication(
    @DocumentId
    val id: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val teacherEmail: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val subjectCode: String = "",
    val applicationReason: String = "",
    val status: ApplicationStatus = ApplicationStatus.PENDING,
    val appliedAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long? = null,
    val reviewedBy: String? = null,
    val adminComments: String? = null
)

enum class ApplicationStatus {
    PENDING,
    APPROVED,
    REJECTED
}
