package com.smartacademictracker.data.validation

import com.smartacademictracker.data.model.Grade
import com.smartacademictracker.data.model.GradePeriod
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GradeValidationService @Inject constructor() {
    
    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String> = emptyList(),
        val warnings: List<String> = emptyList()
    )
    
    fun validateGrade(grade: Grade): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // Score validation
        if (grade.score < 0) {
            errors.add("Score cannot be negative")
        }
        if (grade.score > grade.maxScore) {
            errors.add("Score cannot exceed maximum score")
        }
        if (grade.score > 100) {
            errors.add("Score cannot exceed 100")
        }
        
        // Max score validation
        if (grade.maxScore <= 0) {
            errors.add("Maximum score must be greater than 0")
        }
        if (grade.maxScore > 100) {
            errors.add("Maximum score cannot exceed 100")
        }
        
        // Percentage validation
        val calculatedPercentage = (grade.score / grade.maxScore) * 100
        if (Math.abs(calculatedPercentage - grade.percentage) > 0.1) {
            errors.add("Percentage calculation mismatch")
        }
        
        // Grade period validation
        if (grade.gradePeriod == null) {
            errors.add("Grade period is required")
        }
        
        // Required field validation
        if (grade.studentId.isBlank()) {
            errors.add("Student ID is required")
        }
        if (grade.subjectId.isBlank()) {
            errors.add("Subject ID is required")
        }
        if (grade.teacherId.isBlank()) {
            errors.add("Teacher ID is required")
        }
        if (grade.studentName.isBlank()) {
            errors.add("Student name is required")
        }
        if (grade.subjectName.isBlank()) {
            errors.add("Subject name is required")
        }
        
        // Business logic validation
        if (grade.score < 50 && grade.percentage < 50) {
            warnings.add("Grade is below passing threshold")
        }
        
        if (grade.percentage >= 90) {
            warnings.add("Excellent performance!")
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    fun validateGradeInput(
        score: Double,
        maxScore: Double,
        gradePeriod: GradePeriod?
    ): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // Score validation
        if (score < 0) {
            errors.add("Score cannot be negative")
        }
        if (score > maxScore) {
            errors.add("Score cannot exceed maximum score")
        }
        if (score > 100) {
            errors.add("Score cannot exceed 100")
        }
        
        // Max score validation
        if (maxScore <= 0) {
            errors.add("Maximum score must be greater than 0")
        }
        if (maxScore > 100) {
            errors.add("Maximum score cannot exceed 100")
        }
        
        // Grade period validation
        if (gradePeriod == null) {
            errors.add("Grade period is required")
        }
        
        // Business logic validation
        val percentage = (score / maxScore) * 100
        if (percentage < 50) {
            warnings.add("Grade is below passing threshold")
        }
        
        if (percentage >= 90) {
            warnings.add("Excellent performance!")
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    fun validateBatchGrades(grades: List<Grade>): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        if (grades.isEmpty()) {
            errors.add("No grades provided for validation")
            return ValidationResult(false, errors)
        }
        
        // Validate each grade
        grades.forEachIndexed { index, grade ->
            val result = validateGrade(grade)
            if (!result.isValid) {
                errors.add("Grade ${index + 1}: ${result.errors.joinToString(", ")}")
            }
            warnings.addAll(result.warnings.map { "Grade ${index + 1}: $it" })
        }
        
        // Check for duplicate grades
        val duplicateGrades = grades.groupBy { "${it.studentId}_${it.subjectId}_${it.gradePeriod}" }
            .filter { it.value.size > 1 }
        
        if (duplicateGrades.isNotEmpty()) {
            errors.add("Duplicate grades found for the same student, subject, and period")
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    fun validateGradeUpdate(oldGrade: Grade, newGrade: Grade): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // Validate the new grade
        val newGradeValidation = validateGrade(newGrade)
        errors.addAll(newGradeValidation.errors)
        warnings.addAll(newGradeValidation.warnings)
        
        // Check for significant changes
        val scoreDifference = Math.abs(newGrade.score - oldGrade.score)
        if (scoreDifference > 20) {
            warnings.add("Significant score change detected (${scoreDifference} points)")
        }
        
        // Check for period changes
        if (oldGrade.gradePeriod != newGrade.gradePeriod) {
            errors.add("Grade period cannot be changed")
        }
        
        // Check for student/subject changes
        if (oldGrade.studentId != newGrade.studentId) {
            errors.add("Student cannot be changed")
        }
        if (oldGrade.subjectId != newGrade.subjectId) {
            errors.add("Subject cannot be changed")
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    fun validateGradeSubmission(grades: List<Grade>): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        if (grades.isEmpty()) {
            errors.add("No grades to submit")
            return ValidationResult(false, errors)
        }
        
        // Check if all required periods are present
        val requiredPeriods = setOf(GradePeriod.PRELIM, GradePeriod.MIDTERM, GradePeriod.FINAL)
        val presentPeriods = grades.map { it.gradePeriod }.toSet()
        val missingPeriods = requiredPeriods - presentPeriods
        
        if (missingPeriods.isNotEmpty()) {
            warnings.add("Missing grade periods: ${missingPeriods.joinToString(", ")}")
        }
        
        // Validate each grade
        val batchValidation = validateBatchGrades(grades)
        errors.addAll(batchValidation.errors)
        warnings.addAll(batchValidation.warnings)
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
}
