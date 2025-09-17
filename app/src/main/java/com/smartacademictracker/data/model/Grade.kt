package com.smartacademictracker.data.model

import com.google.firebase.firestore.DocumentId

data class Grade(
    @DocumentId
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val teacherId: String = "",
    val gradeType: GradeType = GradeType.QUIZ,
    val score: Double = 0.0,
    val maxScore: Double = 100.0,
    val percentage: Double = 0.0,
    val letterGrade: String = "",
    val description: String = "",
    val dateRecorded: Long = System.currentTimeMillis(),
    val semester: String = "",
    val academicYear: String = ""
) {
    fun calculatePercentage(): Double {
        return if (maxScore > 0) (score / maxScore) * 100 else 0.0
    }
    
    fun calculateLetterGrade(): String {
        val percent = calculatePercentage()
        return when {
            percent >= 97 -> "A+"
            percent >= 93 -> "A"
            percent >= 90 -> "A-"
            percent >= 87 -> "B+"
            percent >= 83 -> "B"
            percent >= 80 -> "B-"
            percent >= 77 -> "C+"
            percent >= 73 -> "C"
            percent >= 70 -> "C-"
            percent >= 67 -> "D+"
            percent >= 65 -> "D"
            else -> "F"
        }
    }
}

enum class GradeType {
    QUIZ,
    EXAM,
    ACTIVITY,
    PROJECT,
    HOMEWORK,
    PARTICIPATION
}
