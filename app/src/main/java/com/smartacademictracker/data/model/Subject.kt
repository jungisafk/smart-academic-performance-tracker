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
    val academicPeriodId: String = "", // Reference to active academic period
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val yearLevelId: String = "", // Reference to YearLevel document ID
    val courseId: String = "", // Reference to Course document ID
    val maxStudents: Int = 30, // Maximum number of students that can enroll
    val numberOfSections: Int = 1, // Number of sections for this subject
    val sections: List<String> = emptyList(), // Generated section names (e.g., ["IT101A", "IT101B"])
    val subjectType: SubjectType = SubjectType.MAJOR, // MAJOR: only teachers of that course can see/apply, MINOR: any teacher can apply
    // Computed fields for display (populated by repository)
    val yearLevelName: String = "",
    val courseName: String = "",
    val courseCode: String = ""
)

/**
 * Subject type determines visibility and application rules for teachers
 * - MAJOR: Only teachers of the same course/department can see and apply
 * - MINOR: Any teacher can see and apply (e.g., general education subjects)
 */
enum class SubjectType(val displayName: String) {
    MAJOR("Major Subject"),
    MINOR("Minor Subject")
}
