package com.smartacademictracker.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Comprehensive data model for viewing all data within an academic period
 */
data class AcademicPeriodData(
    @DocumentId
    val periodId: String = "",
    val academicPeriod: AcademicPeriod? = null,
    val courses: List<Course> = emptyList(),
    val yearLevels: List<YearLevel> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val sectionAssignments: List<SectionAssignment> = emptyList(),
    val teacherApplications: List<TeacherApplication> = emptyList(),
    val studentApplications: List<SubjectApplication> = emptyList(),
    val grades: List<Grade> = emptyList(),
    val users: List<User> = emptyList(),
    val statistics: AcademicPeriodStatistics = AcademicPeriodStatistics()
)

/**
 * Statistics for an academic period
 */
data class AcademicPeriodStatistics(
    val totalCourses: Int = 0,
    val totalYearLevels: Int = 0,
    val totalSubjects: Int = 0,
    val totalSections: Int = 0,
    val totalTeachers: Int = 0,
    val totalStudents: Int = 0,
    val totalAdmins: Int = 0,
    val totalTeacherApplications: Int = 0,
    val totalStudentApplications: Int = 0,
    val totalGrades: Int = 0,
    val activeSectionAssignments: Int = 0,
    val pendingTeacherApplications: Int = 0,
    val pendingStudentApplications: Int = 0
)

/**
 * Summary data for academic period comparison
 */
data class AcademicPeriodSummary(
    val periodId: String = "",
    val periodName: String = "",
    val academicYear: String = "",
    val semester: String = "",
    val startDate: Long = 0,
    val endDate: Long = 0,
    val isActive: Boolean = false,
    val statistics: AcademicPeriodStatistics = AcademicPeriodStatistics()
)
