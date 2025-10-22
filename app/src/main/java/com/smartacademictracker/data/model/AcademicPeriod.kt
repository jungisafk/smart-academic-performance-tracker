package com.smartacademictracker.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class AcademicPeriod(
    @DocumentId
    val id: String = "",
    val name: String = "", // e.g., "Academic Year 2024-2025"
    val academicYear: String = "", // e.g., "2024-2025"
    val semester: Semester = Semester.FIRST_SEMESTER,
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = System.currentTimeMillis(),
    @PropertyName("active")
    val isActive: Boolean = false, // Only one period can be active at a time
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "", // User ID who created this period
    val createdByName: String = "" // Name of the user who created this period
)

data class AcademicPeriodOverview(
    val totalPeriods: Int = 0,
    val activePeriod: AcademicPeriod? = null,
    val currentSemester: String = "",
    val currentAcademicYear: String = ""
)
