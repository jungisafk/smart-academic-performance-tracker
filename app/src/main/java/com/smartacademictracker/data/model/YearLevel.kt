package com.smartacademictracker.data.model

import com.google.firebase.firestore.DocumentId

data class YearLevel(
    @DocumentId
    val id: String = "",
    val courseId: String = "", // Reference to the course this year level belongs to
    val name: String = "", // e.g., "1st Year", "2nd Year", "3rd Year", "4th Year"
    val level: Int = 1, // Numeric level (1, 2, 3, 4)
    val description: String = "",
    val hasSummerClass: Boolean = false, // Whether this year level has summer classes (not for 4th year)
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
