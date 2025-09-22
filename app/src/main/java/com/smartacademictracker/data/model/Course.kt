package com.smartacademictracker.data.model

import com.google.firebase.firestore.DocumentId

data class Course(
    @DocumentId
    val id: String = "",
    val name: String = "", // e.g., "Information and Communication Technology", "Information Technology"
    val code: String = "", // e.g., "ICT", "IT"
    val description: String = "",
    val duration: Int = 4, // Duration in years
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
