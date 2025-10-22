package com.smartacademictracker.data.model

data class GradeExport(
    val subjectId: String = "",
    val subjectName: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val academicYear: String = "",
    val semester: String = "",
    val exportDate: Long = System.currentTimeMillis(),
    val academicPeriodId: String = "", // Reference to active academic period
    val students: List<StudentGradeExport> = emptyList()
)

data class StudentGradeExport(
    val studentId: String = "",
    val studentName: String = "",
    val studentNumber: String = "",
    val prelimGrade: Double? = null,
    val midtermGrade: Double? = null,
    val finalGrade: Double? = null,
    val finalAverage: Double? = null,
    val letterGrade: String = "",
    val status: String = "",
    val attendance: Double? = null,
    val participation: Double? = null
)

enum class ExportFormat(val displayName: String, val fileExtension: String) {
    EXCEL("Excel", "xlsx"),
    PDF("PDF", "pdf"),
    CSV("CSV", "csv")
}

enum class ExportType(val displayName: String) {
    SUBJECT_GRADES("Subject Grades"),
    CLASS_SUMMARY("Class Summary"),
    INDIVIDUAL_REPORTS("Individual Reports"),
    COMPARATIVE_ANALYSIS("Comparative Analysis")
}
