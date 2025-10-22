package com.smartacademictracker.data.model

import com.google.firebase.firestore.DocumentId

data class AssignmentSubmission(
    @DocumentId
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val teacherId: String = "",
    val assignmentTitle: String = "",
    val assignmentDescription: String = "",
    val dueDate: Long = System.currentTimeMillis(),
    val submittedDate: Long? = null,
    val status: SubmissionStatus = SubmissionStatus.PENDING,
    val submissionType: SubmissionType = SubmissionType.ONLINE,
    val fileUrl: String? = null,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val textSubmission: String? = null,
    val grade: Double? = null,
    val feedback: String? = null,
    val gradedDate: Long? = null,
    val lateSubmission: Boolean = false,
    val academicPeriodId: String = "", // Reference to active academic period
    val createdAt: Long = System.currentTimeMillis()
)

enum class SubmissionStatus(val displayName: String) {
    PENDING("Pending"),
    SUBMITTED("Submitted"),
    LATE("Late"),
    GRADED("Graded"),
    RETURNED("Returned")
}

enum class SubmissionType(val displayName: String) {
    ONLINE("Online"),
    FILE_UPLOAD("File Upload"),
    TEXT("Text Submission"),
    OFFLINE("Offline")
}
