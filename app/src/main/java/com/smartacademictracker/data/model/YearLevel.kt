package com.smartacademictracker.data.model

import com.google.firebase.firestore.DocumentId

data class YearLevel(
    @DocumentId
    val id: String = "",
    val name: String = "", // e.g., "1st Year", "2nd Year", "3rd Year", "4th Year"
    val level: Int = 1, // Numeric level (1, 2, 3, 4)
    val description: String = "",
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
