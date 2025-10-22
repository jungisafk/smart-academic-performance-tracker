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
    val gradePeriod: GradePeriod = GradePeriod.PRELIM,
    val score: Double = 0.0,
    val maxScore: Double = 100.0,
    val percentage: Double = 0.0,
    val letterGrade: String = "",
    val description: String = "",
    val dateRecorded: Long = System.currentTimeMillis(),
    val semester: String = "",
    val academicYear: String = "",
    val academicPeriodId: String = "" // Reference to active academic period
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

/**
 * Grade periods for academic assessment
 * Each period has specific weight in final average calculation
 */
enum class GradePeriod(val displayName: String, val weight: Double) {
    PRELIM("Preliminary", 0.30),
    MIDTERM("Midterm", 0.30),
    FINAL("Final", 0.40)
}

/**
 * Student grade status based on calculated final average
 */
enum class GradeStatus(val displayName: String, val threshold: Double) {
    INCOMPLETE("Incomplete", 0.0),
    FAILING("Failing", 0.0),
    AT_RISK("At Risk", 60.0),
    PASSING("Passing", 75.0)
}

/**
 * Aggregate model for student's complete grade record in a subject
 */
data class StudentGradeAggregate(
    @DocumentId
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val teacherId: String = "",
    val prelimGrade: Double? = null,
    val midtermGrade: Double? = null,
    val finalGrade: Double? = null,
    val finalAverage: Double? = null,
    val status: GradeStatus = GradeStatus.INCOMPLETE,
    val letterGrade: String = "",
    val semester: String = "",
    val academicYear: String = "",
    val academicPeriodId: String = "", // Reference to active academic period
    val lastUpdated: Long = System.currentTimeMillis()
) {
    /**
     * Calculate final average using standard academic formula:
     * Prelim (30%) + Midterm (30%) + Final (40%)
     */
    fun calculateFinalAverage(): Double? {
        return if (prelimGrade != null && midtermGrade != null && finalGrade != null) {
            (prelimGrade * GradePeriod.PRELIM.weight) + 
            (midtermGrade * GradePeriod.MIDTERM.weight) + 
            (finalGrade * GradePeriod.FINAL.weight)
        } else null
    }
    
    /**
     * Determine grade status based on final average
     */
    fun determineGradeStatus(): GradeStatus {
        val average = calculateFinalAverage()
        return when {
            average == null -> GradeStatus.INCOMPLETE
            average >= GradeStatus.PASSING.threshold -> GradeStatus.PASSING
            average >= GradeStatus.AT_RISK.threshold -> GradeStatus.AT_RISK
            else -> GradeStatus.FAILING
        }
    }
    
    /**
     * Calculate letter grade based on final average
     */
    fun calculateLetterGrade(): String {
        val average = calculateFinalAverage() ?: return "INC"
        return when {
            average >= 97 -> "A+"
            average >= 93 -> "A"
            average >= 90 -> "A-"
            average >= 87 -> "B+"
            average >= 83 -> "B"
            average >= 80 -> "B-"
            average >= 77 -> "C+"
            average >= 73 -> "C"
            average >= 70 -> "C-"
            average >= 67 -> "D+"
            average >= 65 -> "D"
            else -> "F"
        }
    }
    
    /**
     * Get completion percentage (how many grades are recorded)
     */
    fun getCompletionPercentage(): Double {
        val totalPeriods = 3.0
        val completedPeriods = listOfNotNull(prelimGrade, midtermGrade, finalGrade).size
        return (completedPeriods / totalPeriods) * 100
    }
}
