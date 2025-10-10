package com.smartacademictracker.data.utils

import com.smartacademictracker.data.model.Grade
import com.smartacademictracker.data.model.GradePeriod
import com.smartacademictracker.data.model.GradeStatus
import com.smartacademictracker.data.model.StudentGradeAggregate

/**
 * Centralized grade calculation engine for academic performance tracking
 * Implements the standard academic formula: Prelim (30%) + Midterm (30%) + Final (40%)
 */
object GradeCalculationEngine {
    
    /**
     * Calculate final average using the standard academic formula
     * @param prelimGrade Preliminary grade (0-100)
     * @param midtermGrade Midterm grade (0-100)
     * @param finalGrade Final grade (0-100)
     * @return Final average or null if any grade is missing
     */
    fun calculateFinalAverage(
        prelimGrade: Double?,
        midtermGrade: Double?,
        finalGrade: Double?
    ): Double? {
        return if (prelimGrade != null && midtermGrade != null && finalGrade != null) {
            (prelimGrade * GradePeriod.PRELIM.weight) + 
            (midtermGrade * GradePeriod.MIDTERM.weight) + 
            (finalGrade * GradePeriod.FINAL.weight)
        } else null
    }
    
    /**
     * Determine grade status based on final average
     * @param finalAverage Calculated final average
     * @return GradeStatus enum value
     */
    fun determineGradeStatus(finalAverage: Double?): GradeStatus {
        return when {
            finalAverage == null -> GradeStatus.INCOMPLETE
            finalAverage >= GradeStatus.PASSING.threshold -> GradeStatus.PASSING
            finalAverage >= GradeStatus.AT_RISK.threshold -> GradeStatus.AT_RISK
            else -> GradeStatus.FAILING
        }
    }
    
    /**
     * Calculate numeric grade based on final average (1.0-5.0 scale)
     * @param finalAverage Calculated final average
     * @return Numeric grade string (1.0-5.0 scale)
     */
    fun calculateLetterGrade(finalAverage: Double?): String {
        val average = finalAverage ?: return "INC"
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
     * Create a complete student grade aggregate from individual grades
     * @param grades List of individual grade records for a student in a subject
     * @param studentId Student identifier
     * @param studentName Student display name
     * @param subjectId Subject identifier
     * @param subjectName Subject display name
     * @param teacherId Teacher identifier
     * @param semester Current semester
     * @param academicYear Current academic year
     * @return Complete StudentGradeAggregate with calculated values
     */
    fun createStudentGradeAggregate(
        grades: List<Grade>,
        studentId: String,
        studentName: String,
        subjectId: String,
        subjectName: String,
        teacherId: String,
        semester: String,
        academicYear: String
    ): StudentGradeAggregate {
        val prelimGrade = grades.find { it.gradePeriod == GradePeriod.PRELIM }?.score
        val midtermGrade = grades.find { it.gradePeriod == GradePeriod.MIDTERM }?.score
        val finalGrade = grades.find { it.gradePeriod == GradePeriod.FINAL }?.score
        
        val finalAverage = calculateFinalAverage(prelimGrade, midtermGrade, finalGrade)
        val status = determineGradeStatus(finalAverage)
        val letterGrade = calculateLetterGrade(finalAverage)
        
        return StudentGradeAggregate(
            id = "${studentId}_${subjectId}_${semester}_${academicYear}",
            studentId = studentId,
            studentName = studentName,
            subjectId = subjectId,
            subjectName = subjectName,
            teacherId = teacherId,
            prelimGrade = prelimGrade,
            midtermGrade = midtermGrade,
            finalGrade = finalGrade,
            finalAverage = finalAverage,
            status = status,
            letterGrade = letterGrade,
            semester = semester,
            academicYear = academicYear,
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    /**
     * Update student grade aggregate when a new grade is added
     * @param aggregate Existing aggregate
     * @param newGrade New grade to incorporate
     * @return Updated aggregate with recalculated values
     */
    fun updateStudentGradeAggregate(
        aggregate: StudentGradeAggregate,
        newGrade: Grade
    ): StudentGradeAggregate {
        val updatedAggregate = when (newGrade.gradePeriod) {
            GradePeriod.PRELIM -> aggregate.copy(prelimGrade = newGrade.score)
            GradePeriod.MIDTERM -> aggregate.copy(midtermGrade = newGrade.score)
            GradePeriod.FINAL -> aggregate.copy(finalGrade = newGrade.score)
        }
        
        val finalAverage = calculateFinalAverage(
            updatedAggregate.prelimGrade,
            updatedAggregate.midtermGrade,
            updatedAggregate.finalGrade
        )
        val status = determineGradeStatus(finalAverage)
        val letterGrade = calculateLetterGrade(finalAverage)
        
        return updatedAggregate.copy(
            finalAverage = finalAverage,
            status = status,
            letterGrade = letterGrade,
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    /**
     * Calculate class average for a specific grade period
     * @param grades List of grades for the same period
     * @return Class average or null if no grades
     */
    fun calculateClassAverage(grades: List<Grade>): Double? {
        if (grades.isEmpty()) return null
        return grades.map { it.score }.average()
    }
    
    /**
     * Calculate overall class final averages
     * @param aggregates List of student grade aggregates
     * @return Class final average or null if no complete grades
     */
    fun calculateClassFinalAverage(aggregates: List<StudentGradeAggregate>): Double? {
        val completeAverages = aggregates.mapNotNull { it.finalAverage }
        return if (completeAverages.isEmpty()) null else completeAverages.average()
    }
    
    /**
     * Get grade distribution statistics
     * @param aggregates List of student grade aggregates
     * @return Map of grade status to count
     */
    fun getGradeDistribution(aggregates: List<StudentGradeAggregate>): Map<GradeStatus, Int> {
        return aggregates.groupBy { it.status }
            .mapValues { it.value.size }
            .withDefault { 0 }
    }
    
    /**
     * Validate grade input
     * @param score Grade score to validate
     * @return True if valid (0-100), false otherwise
     */
    fun isValidGrade(score: Double): Boolean {
        return score in 0.0..100.0
    }
    
    /**
     * Format grade for display
     * @param score Grade score
     * @param decimalPlaces Number of decimal places to show
     * @return Formatted grade string
     */
    fun formatGrade(score: Double?, decimalPlaces: Int = 2): String {
        return score?.let { "%.${decimalPlaces}f".format(it) } ?: "N/A"
    }
    
    /**
     * Get grade color indicator for UI
     * @param status Grade status
     * @return Color resource identifier (as string for now)
     */
    fun getGradeStatusColor(status: GradeStatus): String {
        return when (status) {
            GradeStatus.PASSING -> "green"
            GradeStatus.AT_RISK -> "yellow"
            GradeStatus.FAILING -> "red"
            GradeStatus.INCOMPLETE -> "gray"
        }
    }
}
