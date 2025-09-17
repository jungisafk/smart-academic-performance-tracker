package com.smartacademictracker.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class Enrollment(
    @DocumentId
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val subjectCode: String = "",
    val enrolledAt: Long = System.currentTimeMillis(),
    val semester: String = "",
    val academicYear: String = "",
    val active: Boolean = true
)
