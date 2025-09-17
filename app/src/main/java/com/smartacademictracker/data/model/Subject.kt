package com.smartacademictracker.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class Subject(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val code: String = "",
    val description: String = "",
    val teacherId: String? = null,
    val teacherName: String? = null,
    val credits: Int = 3,
    val semester: String = "",
    val academicYear: String = "",
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
