package com.smartacademictracker.util

/**
 * Utility class for validating password strength
 * Enforces institutional password policies
 */
object PasswordValidator {
    
    // Password requirements
    private const val MIN_LENGTH = 8
    private const val MAX_LENGTH = 128
    private const val SPECIAL_CHARACTERS = "!@#\$%^&*()_+-=[]{}|;:,.<>?"
    
    /**
     * Validate password against all rules
     * @param password The password to validate
     * @return PasswordValidationResult with validation status and error messages
     */
    fun validate(password: String): PasswordValidationResult {
        val errors = mutableListOf<String>()
        
        // Length check
        if (password.length < MIN_LENGTH) {
            errors.add("Password must be at least $MIN_LENGTH characters long")
        }
        
        if (password.length > MAX_LENGTH) {
            errors.add("Password must not exceed $MAX_LENGTH characters")
        }
        
        // Uppercase letter check
        if (!password.any { it.isUpperCase() }) {
            errors.add("Password must contain at least one uppercase letter (A-Z)")
        }
        
        // Lowercase letter check
        if (!password.any { it.isLowerCase() }) {
            errors.add("Password must contain at least one lowercase letter (a-z)")
        }
        
        // Digit check
        if (!password.any { it.isDigit() }) {
            errors.add("Password must contain at least one number (0-9)")
        }
        
        // Note: Special character, common password, and sequential character checks removed
        
        return PasswordValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            strength = calculatePasswordStrength(password)
        )
    }
    
    /**
     * Quick validation for minimum requirements only
     * Useful for less strict scenarios
     */
    fun validateMinimum(password: String): PasswordValidationResult {
        val errors = mutableListOf<String>()
        
        if (password.length < MIN_LENGTH) {
            errors.add("Password must be at least $MIN_LENGTH characters long")
        }
        
        if (password.length > MAX_LENGTH) {
            errors.add("Password must not exceed $MAX_LENGTH characters")
        }
        
        return PasswordValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            strength = calculatePasswordStrength(password)
        )
    }
    
    /**
     * Check if passwords match
     */
    fun passwordsMatch(password: String, confirmPassword: String): Boolean {
        return password == confirmPassword
    }
    
    /**
     * Calculate password strength score (0-100)
     */
    private fun calculatePasswordStrength(password: String): Int {
        var score = 0
        
        // Length score (max 30 points)
        score += minOf(password.length * 2, 30)
        
        // Character diversity (max 30 points)
        if (password.any { it.isUpperCase() }) score += 10
        if (password.any { it.isLowerCase() }) score += 10
        if (password.any { it.isDigit() }) score += 10
        
        // Additional length bonus (max 20 points)
        if (password.length >= 12) score += 5
        if (password.length >= 16) score += 5
        if (password.length >= 20) score += 5
        
        // Variety bonus (max 20 points)
        val uniqueChars = password.toSet().size
        score += minOf(uniqueChars, 20)
        
        // Note: Sequential and common password penalties removed
        
        return maxOf(0, minOf(score, 100))
    }
    
    /**
     * Get password strength label
     */
    fun getStrengthLabel(score: Int): String {
        return when {
            score < 30 -> "Weak"
            score < 60 -> "Fair"
            score < 80 -> "Good"
            score < 90 -> "Strong"
            else -> "Very Strong"
        }
    }
    
    /**
     * Check for sequential characters
     */
    private fun hasSequentialCharacters(password: String): Boolean {
        val sequences = listOf(
            "012", "123", "234", "345", "456", "567", "678", "789",
            "abc", "bcd", "cde", "def", "efg", "fgh", "ghi", "hij",
            "ijk", "jkl", "klm", "lmn", "mno", "nop", "opq", "pqr",
            "qrs", "rst", "stu", "tuv", "uvw", "vwx", "wxy", "xyz"
        )
        
        val lowerPassword = password.lowercase()
        return sequences.any { seq -> 
            lowerPassword.contains(seq) || lowerPassword.contains(seq.reversed())
        }
    }
    
    /**
     * Check against common password list
     */
    private fun isCommonPassword(password: String): Boolean {
        val commonPasswords = setOf(
            "password", "password123", "12345678", "qwerty", "abc123",
            "monkey", "letmein", "trustno1", "dragon", "baseball",
            "iloveyou", "master", "sunshine", "ashley", "bailey",
            "passw0rd", "shadow", "superman", "qazwsx", "michael",
            "football", "welcome", "jesus", "ninja", "mustang",
            "password1", "123456789", "Password1", "Password123"
        )
        
        return commonPasswords.contains(password.lowercase())
    }
    
    /**
     * Generate password requirements text
     */
    fun getRequirementsText(): String {
        return """
            Password Requirements:
            • At least $MIN_LENGTH characters long
            • Contains uppercase letter (A-Z)
            • Contains lowercase letter (a-z)
            • Contains number (0-9)
        """.trimIndent()
    }
    
    /**
     * Check individual password requirements
     * Returns a list of requirements with their status
     */
    fun checkPasswordRequirements(password: String): List<PasswordRequirement> {
        return listOf(
            PasswordRequirement(
                description = "At least $MIN_LENGTH characters long",
                isMet = password.length >= MIN_LENGTH
            ),
            PasswordRequirement(
                description = "Contains at least one uppercase letter (A-Z)",
                isMet = password.any { it.isUpperCase() }
            ),
            PasswordRequirement(
                description = "Contains at least one lowercase letter (a-z)",
                isMet = password.any { it.isLowerCase() }
            ),
            PasswordRequirement(
                description = "Contains at least one number (0-9)",
                isMet = password.any { it.isDigit() }
            )
            // Note: Special character, common password, and sequential character requirements removed
        )
    }
}

/**
 * Individual password requirement check
 */
data class PasswordRequirement(
    val description: String,
    val isMet: Boolean
)

/**
 * Result of password validation
 */
data class PasswordValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val strength: Int = 0
) {
    val strengthLabel: String
        get() = PasswordValidator.getStrengthLabel(strength)
}

