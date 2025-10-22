package com.smartacademictracker.data.model

import com.google.firebase.firestore.DocumentId

data class SubjectApplication(
    @DocumentId
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val sectionName: String = "", // Specific section the student is applying for
    val courseId: String = "",
    val courseName: String = "",
    val yearLevelId: String = "",
    val yearLevelName: String = "",
    val semester: Semester = Semester.FIRST_SEMESTER,
    val academicYear: String = "",
    val academicPeriodId: String = "", // Reference to active academic period
    val status: ApplicationStatus = ApplicationStatus.PENDING,
    val appliedDate: Long = System.currentTimeMillis(),
    val processedDate: Long? = null,
    val processedBy: String? = null,
    val remarks: String? = null,
    val reviewedDate: Long? = null,
    val reviewerName: String? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
