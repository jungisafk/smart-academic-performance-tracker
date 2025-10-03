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
    val semester: Semester = Semester.FIRST_SEMESTER,
    val academicYear: String = "",
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val yearLevelId: String = "", // Reference to YearLevel document ID
    val courseId: String = "", // Reference to Course document ID
    val maxStudents: Int = 30, // Maximum number of students that can enroll
    // Computed fields for display (populated by repository)
    val yearLevelName: String = "",
    val courseName: String = "",
    val courseCode: String = ""
)
