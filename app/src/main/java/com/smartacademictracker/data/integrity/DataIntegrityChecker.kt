package com.smartacademictracker.data.integrity

import com.google.firebase.firestore.FirebaseFirestore
import com.smartacademictracker.data.model.Grade
import com.smartacademictracker.data.model.GradePeriod
import com.smartacademictracker.data.model.User
import com.smartacademictracker.data.model.UserRole
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataIntegrityChecker @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    
    data class IntegrityCheckResult(
        val isValid: Boolean,
        val errors: List<IntegrityError> = emptyList(),
        val warnings: List<IntegrityWarning> = emptyList(),
        val suggestions: List<IntegritySuggestion> = emptyList()
    )
    
    data class IntegrityError(
        val type: ErrorType,
        val message: String,
        val resourceId: String,
        val severity: ErrorSeverity
    )
    
    data class IntegrityWarning(
        val type: WarningType,
        val message: String,
        val resourceId: String,
        val suggestion: String
    )
    
    data class IntegritySuggestion(
        val type: SuggestionType,
        val message: String,
        val resourceId: String,
        val action: String
    )
    
    enum class ErrorType {
        MISSING_REFERENCE,
        INVALID_DATA,
        DUPLICATE_ENTRY,
        ORPHANED_RECORD,
        INCONSISTENT_DATA
    }
    
    enum class WarningType {
        POTENTIAL_ISSUE,
        DATA_QUALITY,
        PERFORMANCE_IMPACT
    }
    
    enum class SuggestionType {
        OPTIMIZATION,
        DATA_CLEANUP,
        STRUCTURE_IMPROVEMENT
    }
    
    enum class ErrorSeverity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
    
    suspend fun performFullIntegrityCheck(): IntegrityCheckResult {
        val errors = mutableListOf<IntegrityError>()
        val warnings = mutableListOf<IntegrityWarning>()
        val suggestions = mutableListOf<IntegritySuggestion>()
        
        // Check users integrity
        val usersCheck = checkUsersIntegrity()
        errors.addAll(usersCheck.errors)
        warnings.addAll(usersCheck.warnings)
        suggestions.addAll(usersCheck.suggestions)
        
        // Check grades integrity
        val gradesCheck = checkGradesIntegrity()
        errors.addAll(gradesCheck.errors)
        warnings.addAll(gradesCheck.warnings)
        suggestions.addAll(gradesCheck.suggestions)
        
        // Check enrollments integrity
        val enrollmentsCheck = checkEnrollmentsIntegrity()
        errors.addAll(enrollmentsCheck.errors)
        warnings.addAll(enrollmentsCheck.warnings)
        suggestions.addAll(enrollmentsCheck.suggestions)
        
        // Check subjects integrity
        val subjectsCheck = checkSubjectsIntegrity()
        errors.addAll(subjectsCheck.errors)
        warnings.addAll(subjectsCheck.warnings)
        suggestions.addAll(subjectsCheck.suggestions)
        
        return IntegrityCheckResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
            suggestions = suggestions
        )
    }
    
    private suspend fun checkUsersIntegrity(): IntegrityCheckResult {
        val errors = mutableListOf<IntegrityError>()
        val warnings = mutableListOf<IntegrityWarning>()
        val suggestions = mutableListOf<IntegritySuggestion>()
        
        try {
            val usersSnapshot = firestore.collection("users").get().await()
            val users = usersSnapshot.toObjects(User::class.java)
            
            // Check for duplicate emails
            val emailGroups = users.groupBy { it.email }
            emailGroups.forEach { (email, userList) ->
                if (userList.size > 1) {
                    errors.add(
                        IntegrityError(
                            type = ErrorType.DUPLICATE_ENTRY,
                            message = "Duplicate email found: $email",
                            resourceId = userList.first().id,
                            severity = ErrorSeverity.HIGH
                        )
                    )
                }
            }
            
            // Check for invalid roles
            users.forEach { user ->
                if (!UserRole.values().any { it.name == user.role }) {
                    errors.add(
                        IntegrityError(
                            type = ErrorType.INVALID_DATA,
                            message = "Invalid user role: ${user.role}",
                            resourceId = user.id,
                            severity = ErrorSeverity.MEDIUM
                        )
                    )
                }
            }
            
            // Check for missing required fields
            users.forEach { user ->
                if (user.firstName.isBlank()) {
                    errors.add(
                        IntegrityError(
                            type = ErrorType.INVALID_DATA,
                            message = "Missing first name",
                            resourceId = user.id,
                            severity = ErrorSeverity.MEDIUM
                        )
                    )
                }
                if (user.lastName.isBlank()) {
                    errors.add(
                        IntegrityError(
                            type = ErrorType.INVALID_DATA,
                            message = "Missing last name",
                            resourceId = user.id,
                            severity = ErrorSeverity.MEDIUM
                        )
                    )
                }
            }
            
        } catch (e: Exception) {
            errors.add(
                IntegrityError(
                    type = ErrorType.INVALID_DATA,
                    message = "Failed to check users integrity: ${e.message}",
                    resourceId = "users_collection",
                    severity = ErrorSeverity.CRITICAL
                )
            )
        }
        
        return IntegrityCheckResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
            suggestions = suggestions
        )
    }
    
    private suspend fun checkGradesIntegrity(): IntegrityCheckResult {
        val errors = mutableListOf<IntegrityError>()
        val warnings = mutableListOf<IntegrityWarning>()
        val suggestions = mutableListOf<IntegritySuggestion>()
        
        try {
            val gradesSnapshot = firestore.collection("grades").get().await()
            val grades = gradesSnapshot.toObjects(Grade::class.java)
            
            // Check for invalid grade values
            grades.forEach { grade ->
                if (grade.score < 0 || grade.score > 100) {
                    errors.add(
                        IntegrityError(
                            type = ErrorType.INVALID_DATA,
                            message = "Invalid score: ${grade.score}",
                            resourceId = grade.id,
                            severity = ErrorSeverity.HIGH
                        )
                    )
                }
                
                if (grade.maxScore <= 0 || grade.maxScore > 100) {
                    errors.add(
                        IntegrityError(
                            type = ErrorType.INVALID_DATA,
                            message = "Invalid max score: ${grade.maxScore}",
                            resourceId = grade.id,
                            severity = ErrorSeverity.HIGH
                        )
                    )
                }
                
                // Check percentage calculation
                val expectedPercentage = (grade.score / grade.maxScore) * 100
                if (Math.abs(expectedPercentage - grade.percentage) > 0.1) {
                    errors.add(
                        IntegrityError(
                            type = ErrorType.INCONSISTENT_DATA,
                            message = "Percentage calculation mismatch: expected $expectedPercentage, got ${grade.percentage}",
                            resourceId = grade.id,
                            severity = ErrorSeverity.MEDIUM
                        )
                    )
                }
            }
            
            // Check for duplicate grades
            val duplicateGrades = grades.groupBy { "${it.studentId}_${it.subjectId}_${it.gradePeriod}" }
                .filter { it.value.size > 1 }
            
            duplicateGrades.forEach { (key, gradeList) ->
                errors.add(
                    IntegrityError(
                        type = ErrorType.DUPLICATE_ENTRY,
                        message = "Duplicate grades found for key: $key",
                        resourceId = gradeList.first().id,
                        severity = ErrorSeverity.HIGH
                    )
                )
            }
            
            // Check for orphaned grades (student or subject doesn't exist)
            val studentIds = firestore.collection("users")
                .whereEqualTo("role", "STUDENT")
                .get().await()
                .toObjects(User::class.java)
                .map { it.id }
                .toSet()
            
            val subjectIds = firestore.collection("subjects").get().await()
                .documents.map { it.id }.toSet()
            
            grades.forEach { grade ->
                if (!studentIds.contains(grade.studentId)) {
                    errors.add(
                        IntegrityError(
                            type = ErrorType.ORPHANED_RECORD,
                            message = "Grade references non-existent student: ${grade.studentId}",
                            resourceId = grade.id,
                            severity = ErrorSeverity.HIGH
                        )
                    )
                }
                
                if (!subjectIds.contains(grade.subjectId)) {
                    errors.add(
                        IntegrityError(
                            type = ErrorType.ORPHANED_RECORD,
                            message = "Grade references non-existent subject: ${grade.subjectId}",
                            resourceId = grade.id,
                            severity = ErrorSeverity.HIGH
                        )
                    )
                }
            }
            
        } catch (e: Exception) {
            errors.add(
                IntegrityError(
                    type = ErrorType.INVALID_DATA,
                    message = "Failed to check grades integrity: ${e.message}",
                    resourceId = "grades_collection",
                    severity = ErrorSeverity.CRITICAL
                )
            )
        }
        
        return IntegrityCheckResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
            suggestions = suggestions
        )
    }
    
    private suspend fun checkEnrollmentsIntegrity(): IntegrityCheckResult {
        val errors = mutableListOf<IntegrityError>()
        val warnings = mutableListOf<IntegrityWarning>()
        val suggestions = mutableListOf<IntegritySuggestion>()
        
        try {
            val enrollmentsSnapshot = firestore.collection("enrollments").get().await()
            val enrollments = enrollmentsSnapshot.toObjects(com.smartacademictracker.data.model.Enrollment::class.java)
            
            // Check for duplicate enrollments
            val duplicateEnrollments = enrollments.groupBy { "${it.studentId}_${it.subjectId}" }
                .filter { it.value.size > 1 }
            
            duplicateEnrollments.forEach { (key, enrollmentList) ->
                errors.add(
                    IntegrityError(
                        type = ErrorType.DUPLICATE_ENTRY,
                        message = "Duplicate enrollment found for key: $key",
                        resourceId = enrollmentList.first().id,
                        severity = ErrorSeverity.HIGH
                    )
                )
            }
            
            // Check for orphaned enrollments
            val studentIds = firestore.collection("users")
                .whereEqualTo("role", "STUDENT")
                .get().await()
                .toObjects(User::class.java)
                .map { it.id }
                .toSet()
            
            val subjectIds = firestore.collection("subjects").get().await()
                .documents.map { it.id }.toSet()
            
            enrollments.forEach { enrollment ->
                if (!studentIds.contains(enrollment.studentId)) {
                    errors.add(
                        IntegrityError(
                            type = ErrorType.ORPHANED_RECORD,
                            message = "Enrollment references non-existent student: ${enrollment.studentId}",
                            resourceId = enrollment.id,
                            severity = ErrorSeverity.HIGH
                        )
                    )
                }
                
                if (!subjectIds.contains(enrollment.subjectId)) {
                    errors.add(
                        IntegrityError(
                            type = ErrorType.ORPHANED_RECORD,
                            message = "Enrollment references non-existent subject: ${enrollment.subjectId}",
                            resourceId = enrollment.id,
                            severity = ErrorSeverity.HIGH
                        )
                    )
                }
            }
            
        } catch (e: Exception) {
            errors.add(
                IntegrityError(
                    type = ErrorType.INVALID_DATA,
                    message = "Failed to check enrollments integrity: ${e.message}",
                    resourceId = "enrollments_collection",
                    severity = ErrorSeverity.CRITICAL
                )
            )
        }
        
        return IntegrityCheckResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
            suggestions = suggestions
        )
    }
    
    private suspend fun checkSubjectsIntegrity(): IntegrityCheckResult {
        val errors = mutableListOf<IntegrityError>()
        val warnings = mutableListOf<IntegrityWarning>()
        val suggestions = mutableListOf<IntegritySuggestion>()
        
        try {
            val subjectsSnapshot = firestore.collection("subjects").get().await()
            val subjects = subjectsSnapshot.toObjects(com.smartacademictracker.data.model.Subject::class.java)
            
            // Check for missing required fields
            subjects.forEach { subject ->
                if (subject.name.isBlank()) {
                    errors.add(
                        IntegrityError(
                            type = ErrorType.INVALID_DATA,
                            message = "Missing subject name",
                            resourceId = subject.id,
                            severity = ErrorSeverity.MEDIUM
                        )
                    )
                }
                
                if (subject.code.isBlank()) {
                    errors.add(
                        IntegrityError(
                            type = ErrorType.INVALID_DATA,
                            message = "Missing subject code",
                            resourceId = subject.id,
                            severity = ErrorSeverity.MEDIUM
                        )
                    )
                }
            }
            
            // Check for duplicate subject codes
            val duplicateCodes = subjects.groupBy { it.code }
                .filter { it.value.size > 1 }
            
            duplicateCodes.forEach { (code, subjectList) ->
                errors.add(
                    IntegrityError(
                        type = ErrorType.DUPLICATE_ENTRY,
                        message = "Duplicate subject code: $code",
                        resourceId = subjectList.first().id,
                        severity = ErrorSeverity.HIGH
                    )
                )
            }
            
        } catch (e: Exception) {
            errors.add(
                IntegrityError(
                    type = ErrorType.INVALID_DATA,
                    message = "Failed to check subjects integrity: ${e.message}",
                    resourceId = "subjects_collection",
                    severity = ErrorSeverity.CRITICAL
                )
            )
        }
        
        return IntegrityCheckResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
            suggestions = suggestions
        )
    }
}
