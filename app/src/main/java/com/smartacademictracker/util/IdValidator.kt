package com.smartacademictracker.util

/**
 * Utility class for validating Student and Teacher IDs
 * Ensures IDs match the institutional format standards
 */
object IdValidator {
    
    /**
     * Student ID format: YYYY-NNNN (e.g., "2024-1234")
     * - YYYY: 4-digit year
     * - NNNN: 4-digit sequential number
     */
    private val STUDENT_ID_PATTERN = Regex("^\\d{4}-\\d{4}$")
    
    /**
     * Teacher ID format: T-YYYY-NNN (e.g., "T-2024-001")
     * - T: Teacher prefix
     * - YYYY: 4-digit year
     * - NNN: 3-digit sequential number
     */
    private val TEACHER_ID_PATTERN = Regex("^T-\\d{4}-\\d{3}$")
    
    /**
     * Alternative Teacher ID format: EMP-NNNNN (e.g., "EMP-12345")
     * - EMP: Employee prefix
     * - NNNNN: 5-digit employee number
     */
    private val EMPLOYEE_ID_PATTERN = Regex("^EMP-\\d{5}$")
    
    /**
     * Admin ID format: A-YYYY-NNN (e.g., "A-2024-001")
     * - A: Admin prefix
     * - YYYY: 4-digit year
     * - NNN: 3-digit sequential number
     */
    private val ADMIN_ID_PATTERN = Regex("^A-\\d{4}-\\d{3}$")
    
    /**
     * Validate Student ID format
     * @param studentId The student ID to validate
     * @return ValidationResult with success status and error message if invalid
     */
    fun validateStudentId(studentId: String): ValidationResult {
        if (studentId.isBlank()) {
            return ValidationResult(false, "Student ID cannot be empty")
        }
        
        val trimmed = studentId.trim()
        
        if (!STUDENT_ID_PATTERN.matches(trimmed)) {
            return ValidationResult(
                false, 
                "Invalid Student ID format. Expected format: YYYY-NNNN (e.g., 2024-1234)"
            )
        }
        
        // Extract and validate year
        val year = extractYearFromStudentId(trimmed)
        if (year != null) {
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            if (year.toInt() < 2000 || year.toInt() > currentYear + 1) {
                return ValidationResult(
                    false,
                    "Invalid year in Student ID. Year must be between 2000 and ${currentYear + 1}"
                )
            }
        }
        
        return ValidationResult(true, null)
    }
    
    /**
     * Validate Teacher ID format
     * Accepts both T-YYYY-NNN and EMP-NNNNN formats
     * @param teacherId The teacher ID to validate
     * @return ValidationResult with success status and error message if invalid
     */
    fun validateTeacherId(teacherId: String): ValidationResult {
        if (teacherId.isBlank()) {
            return ValidationResult(false, "Teacher ID cannot be empty")
        }
        
        val trimmed = teacherId.trim().uppercase()
        
        val isValidTeacherFormat = TEACHER_ID_PATTERN.matches(trimmed)
        val isValidEmployeeFormat = EMPLOYEE_ID_PATTERN.matches(trimmed)
        
        if (!isValidTeacherFormat && !isValidEmployeeFormat) {
            return ValidationResult(
                false,
                "Invalid Teacher ID format. Expected format: T-YYYY-NNN (e.g., T-2024-001) or EMP-NNNNN (e.g., EMP-12345)"
            )
        }
        
        // Validate year if using T-YYYY-NNN format
        if (isValidTeacherFormat) {
            val year = extractYearFromTeacherId(trimmed)
            if (year != null) {
                val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                if (year.toInt() < 2000 || year.toInt() > currentYear + 1) {
                    return ValidationResult(
                        false,
                        "Invalid year in Teacher ID. Year must be between 2000 and ${currentYear + 1}"
                    )
                }
            }
        }
        
        return ValidationResult(true, null)
    }
    
    /**
     * Validate Admin ID format
     * Format: A-YYYY-NNN (e.g., "A-2024-001")
     * @param adminId The admin ID to validate
     * @return ValidationResult with success status and error message if invalid
     */
    fun validateAdminId(adminId: String): ValidationResult {
        if (adminId.isBlank()) {
            return ValidationResult(false, "Admin ID cannot be empty")
        }
        
        val trimmed = adminId.trim().uppercase()
        
        if (!ADMIN_ID_PATTERN.matches(trimmed)) {
            return ValidationResult(
                false,
                "Invalid Admin ID format. Expected format: A-YYYY-NNN (e.g., A-2024-001)"
            )
        }
        
        // Validate year
        val year = extractYearFromAdminId(trimmed)
        if (year != null) {
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            if (year.toInt() < 2000 || year.toInt() > currentYear + 1) {
                return ValidationResult(
                    false,
                    "Invalid year in Admin ID. Year must be between 2000 and ${currentYear + 1}"
                )
            }
        }
        
        return ValidationResult(true, null)
    }
    
    /**
     * Extract year from Student ID
     * @param studentId Student ID in format YYYY-NNNN
     * @return Year as string or null if extraction fails
     */
    fun extractYearFromStudentId(studentId: String): String? {
        return try {
            studentId.trim().split("-").firstOrNull()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Extract year from Teacher ID (T-YYYY-NNN format)
     * @param teacherId Teacher ID in format T-YYYY-NNN
     * @return Year as string or null if extraction fails
     */
    fun extractYearFromTeacherId(teacherId: String): String? {
        return try {
            val parts = teacherId.trim().split("-")
            if (parts.size >= 2 && parts[0].uppercase() == "T") {
                parts[1]
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Extract year from Admin ID (A-YYYY-NNN format)
     * @param adminId Admin ID in format A-YYYY-NNN
     * @return Year as string or null if extraction fails
     */
    fun extractYearFromAdminId(adminId: String): String? {
        return try {
            val parts = adminId.trim().split("-")
            if (parts.size >= 2 && parts[0].uppercase() == "A") {
                parts[1]
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Format Student ID by ensuring proper format
     * @param studentId Raw student ID input
     * @return Formatted student ID
     */
    fun formatStudentId(studentId: String): String {
        return studentId.trim().uppercase()
    }
    
    /**
     * Format Teacher ID by ensuring proper format
     * @param teacherId Raw teacher ID input
     * @return Formatted teacher ID
     */
    fun formatTeacherId(teacherId: String): String {
        return teacherId.trim().uppercase()
    }
    
    /**
     * Format Admin ID by ensuring proper format
     * @param adminId Raw admin ID input
     * @return Formatted admin ID
     */
    fun formatAdminId(adminId: String): String {
        return adminId.trim().uppercase()
    }
    
    /**
     * Check if an ID looks like a student ID
     * @param id The ID to check
     * @return True if it matches student ID pattern
     */
    fun isStudentIdFormat(id: String): Boolean {
        return STUDENT_ID_PATTERN.matches(id.trim())
    }
    
    /**
     * Check if an ID looks like a teacher ID
     * @param id The ID to check
     * @return True if it matches teacher ID pattern
     */
    fun isTeacherIdFormat(id: String): Boolean {
        val trimmed = id.trim().uppercase()
        return TEACHER_ID_PATTERN.matches(trimmed) || EMPLOYEE_ID_PATTERN.matches(trimmed)
    }
    
    /**
     * Check if an ID looks like an admin ID
     * @param id The ID to check
     * @return True if it matches admin ID pattern
     */
    fun isAdminIdFormat(id: String): Boolean {
        val trimmed = id.trim().uppercase()
        return ADMIN_ID_PATTERN.matches(trimmed)
    }
    
    /**
     * Generate next student ID for a given year
     * @param year The enrollment year
     * @param lastNumber The last sequential number used
     * @return Next student ID
     */
    fun generateNextStudentId(year: Int, lastNumber: Int): String {
        val nextNumber = (lastNumber + 1).toString().padStart(4, '0')
        return "$year-$nextNumber"
    }
    
    /**
     * Generate next teacher ID for a given year
     * @param year The hiring year
     * @param lastNumber The last sequential number used
     * @return Next teacher ID
     */
    fun generateNextTeacherId(year: Int, lastNumber: Int): String {
        val nextNumber = (lastNumber + 1).toString().padStart(3, '0')
        return "T-$year-$nextNumber"
    }
    
    /**
     * Generate next admin ID for a given year
     * @param year The year
     * @param lastNumber The last sequential number used
     * @return Next admin ID
     */
    fun generateNextAdminId(year: Int, lastNumber: Int): String {
        val nextNumber = (lastNumber + 1).toString().padStart(3, '0')
        return "A-$year-$nextNumber"
    }
}

/**
 * Result of ID validation
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String?
)

