package com.smartacademictracker.data.model

import com.google.firebase.firestore.DocumentId

data class GradeComment(
    @DocumentId
    val id: String = "",
    val gradeId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val comment: String = "",
    val commentType: CommentType = CommentType.FEEDBACK,
    val isPrivate: Boolean = false,
    val academicPeriodId: String = "", // Reference to active academic period
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class CommentType(val displayName: String) {
    FEEDBACK("Feedback"),
    SUGGESTION("Suggestion"),
    ENCOURAGEMENT("Encouragement"),
    CONCERN("Concern"),
    ACHIEVEMENT("Achievement")
}
