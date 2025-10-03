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
     * Calculate letter grade based on final average
     * @param finalAverage Calculated final average
     * @return Letter grade string
     */
    fun calculateLetterGrade(finalAverage: Double?): String {
        val average = finalAverage ?: return "INC"
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
