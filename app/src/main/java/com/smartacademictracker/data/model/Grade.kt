package com.smartacademictracker.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.IgnoreExtraProperties

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
    val academicPeriodId: String = "", // Reference to active academic period
    // Grade locking fields
    @PropertyName("locked")
    val isLocked: Boolean = false, // Whether the grade is locked from editing
    @PropertyName("editRequested")
    val editRequested: Boolean = false, // Whether teacher requested permission to edit
    @PropertyName("lockedAt")
    val lockedAt: Long? = null, // Timestamp when grade was locked
    @PropertyName("lockedBy")
    val lockedBy: String? = null, // User ID who locked the grade (teacherId when saved)
    @PropertyName("unlockedBy")
    val unlockedBy: String? = null, // Admin ID who unlocked the grade
    @PropertyName("unlockedAt")
    val unlockedAt: Long? = null // Timestamp when grade was unlocked
) {
    fun calculatePercentage(): Double {
        return if (maxScore > 0) (score / maxScore) * 100 else 0.0
    }
    
    fun calculateLetterGrade(): String {
        val percent = calculatePercentage()
        return when {
            percent >= 100 -> "1.0"
            percent >= 99 -> "1.1"
            percent >= 98 -> "1.2"
            percent >= 97 -> "1.3"
            percent >= 96 -> "1.4"
            percent >= 95 -> "1.5"
            percent >= 94 -> "1.6"
            percent >= 93 -> "1.7"
            percent >= 92 -> "1.8"
            percent >= 91 -> "1.9"
            percent >= 90 -> "2.0"
            percent >= 89 -> "2.1"
            percent >= 88 -> "2.2"
            percent >= 87 -> "2.3"
            percent >= 86 -> "2.4"
            percent >= 85 -> "2.5"
            percent >= 84 -> "2.6"
            percent >= 83 -> "2.7"
            percent >= 82 -> "2.8"
            percent >= 81 -> "2.9"
            percent >= 80 -> "3.0"
            percent >= 79 -> "3.1"
            percent >= 78 -> "3.2"
            percent >= 77 -> "3.3"
            percent >= 76 -> "3.4"
            percent >= 75 -> "3.5"
            else -> "5.0"
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
@IgnoreExtraProperties
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
     * Round final average: if decimal is >= 0.5, round up to next whole number
     * @param average Calculated average
     * @return Rounded average
     */
    private fun roundFinalAverage(average: Double): Double {
        val wholePart = average.toInt()
        val decimalPart = average - wholePart
        
        // If decimal is >= 0.5, round up to next whole number
        // Use tolerance of 0.001 to handle floating point precision issues
        return if (decimalPart >= 0.5 - 0.001) {
            (wholePart + 1).toDouble()
        } else {
            wholePart.toDouble()
        }
    }
    
    /**
     * Calculate final average using standard academic formula:
     * Prelim (30%) + Midterm (30%) + Final (40%)
     * 
     * IMPORTANT: Individual period grades (prelimGrade, midtermGrade, finalGrade) are used
     * with full decimal precision and are NOT rounded. Only the final average result is rounded.
     * 
     * Rounding rule: Final average with decimal >= 0.5 rounds up (e.g., 78.7 -> 79, 74.5 -> 75)
     */
    fun calculateFinalAverage(): Double? {
        return if (prelimGrade != null && midtermGrade != null && finalGrade != null) {
            val calculated = (prelimGrade * GradePeriod.PRELIM.weight) + 
                            (midtermGrade * GradePeriod.MIDTERM.weight) + 
                            (finalGrade * GradePeriod.FINAL.weight)
            roundFinalAverage(calculated)
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
     * Calculate numeric grade based on final average (1.0-5.0 scale)
     */
    fun calculateLetterGrade(): String {
        val average = calculateFinalAverage() ?: return "INC"
        return when {
            average >= 100 -> "1.0"
            average >= 99 -> "1.1"
            average >= 98 -> "1.2"
            average >= 97 -> "1.3"
            average >= 96 -> "1.4"
            average >= 95 -> "1.5"
            average >= 94 -> "1.6"
            average >= 93 -> "1.7"
            average >= 92 -> "1.8"
            average >= 91 -> "1.9"
            average >= 90 -> "2.0"
            average >= 89 -> "2.1"
            average >= 88 -> "2.2"
            average >= 87 -> "2.3"
            average >= 86 -> "2.4"
            average >= 85 -> "2.5"
            average >= 84 -> "2.6"
            average >= 83 -> "2.7"
            average >= 82 -> "2.8"
            average >= 81 -> "2.9"
            average >= 80 -> "3.0"
            average >= 79 -> "3.1"
            average >= 78 -> "3.2"
            average >= 77 -> "3.3"
            average >= 76 -> "3.4"
            average >= 75 -> "3.5"
            else -> "5.0"
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
