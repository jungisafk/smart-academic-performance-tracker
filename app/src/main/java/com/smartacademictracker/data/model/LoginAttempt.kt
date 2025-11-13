package com.smartacademictracker.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Login attempt tracking model
 * Used for rate limiting and account lockout security
 */
data class LoginAttempt(
    @DocumentId
    val id: String = "",                  // Document ID (typically the userId)
    val userId: String = "",              // Student ID or Teacher ID
    val attempts: Int = 0,                // Number of failed attempts
    val lastAttemptAt: Long = System.currentTimeMillis(),
    val lockedUntil: Long? = null,        // Timestamp when account will be unlocked
    val ipAddress: String? = null,        // Optional: track IP for security
    val deviceInfo: String? = null,       // Optional: track device info
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Result of login attempt validation
 */
sealed class LoginAttemptResult {
    /**
     * Login attempt is allowed
     */
    object Allowed : LoginAttemptResult()
    
    /**
     * Login attempt failed, account not yet locked
     * @param remainingAttempts Number of attempts remaining before lockout
     */
    data class Failed(val remainingAttempts: Int) : LoginAttemptResult()
    
    /**
     * Account is locked due to too many failed attempts
     * @param lockedUntil Timestamp when account will be unlocked
     */
    data class Locked(val lockedUntil: Long) : LoginAttemptResult() {
        /**
         * Get remaining lock time in minutes
         */
        fun getRemainingMinutes(): Long {
            val remaining = lockedUntil - System.currentTimeMillis()
            return maxOf(0, remaining / 1000 / 60)
        }
        
        /**
         * Get formatted lock time message
         */
        fun getLockMessage(): String {
            val minutes = getRemainingMinutes()
            return when {
                minutes == 0L -> "Account is locked. Please try again in a few moments."
                minutes == 1L -> "Account is locked. Please try again in 1 minute."
                minutes < 60 -> "Account is locked. Please try again in $minutes minutes."
                else -> {
                    val hours = minutes / 60
                    "Account is locked. Please try again in $hours hour${if (hours > 1) "s" else ""}."
                }
            }
        }
    }
}

