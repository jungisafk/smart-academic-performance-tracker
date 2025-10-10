package com.smartacademictracker.data.model

import com.google.firebase.firestore.DocumentId

data class StudentEnrollment(
    @DocumentId
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val studentEmail: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val subjectCode: String = "",
    val sectionName: String = "", // e.g., "IT101A", "IT101B"
    val teacherId: String = "",
    val teacherName: String = "",
    val teacherEmail: String = "",
    val courseId: String = "",
    val courseName: String = "",
    val yearLevelId: String = "",
    val yearLevelName: String = "",
    val semester: Semester = Semester.FIRST_SEMESTER,
    val academicYear: String = "",
    val academicPeriodId: String = "", // Reference to active academic period
    val enrollmentDate: Long = System.currentTimeMillis(),
    val status: EnrollmentStatus = EnrollmentStatus.ACTIVE,
    val enrolledBy: String = "", // Who enrolled the student (admin/teacher)
    val enrolledByName: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

enum class EnrollmentStatus(val displayName: String) {
    ACTIVE("Active"),
    DROPPED("Dropped"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    KICKED("Kicked")
}
