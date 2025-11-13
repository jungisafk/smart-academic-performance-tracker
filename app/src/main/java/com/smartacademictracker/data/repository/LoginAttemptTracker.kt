package com.smartacademictracker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.smartacademictracker.data.model.LoginAttempt
import com.smartacademictracker.data.model.LoginAttemptResult
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for tracking login attempts and implementing rate limiting
 * Prevents brute force attacks by locking accounts after multiple failed attempts
 */
@Singleton
class LoginAttemptTracker @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val collection = firestore.collection("login_attempts")
    
    companion object {
        private const val MAX_ATTEMPTS = 5           // Maximum failed attempts before lockout
        private const val LOCKOUT_DURATION = 30 * 60 * 1000L  // 30 minutes in milliseconds
        private const val ATTEMPT_RESET_DURATION = 15 * 60 * 1000L  // Reset attempts after 15 minutes
    }
    
    /**
     * Check if user is allowed to attempt login
     * @param userId The student ID or teacher ID
     * @return LoginAttemptResult indicating if login is allowed or account is locked
     */
    suspend fun checkLoginAllowed(userId: String): LoginAttemptResult {
        return try {
            val doc = collection.document(userId).get().await()
            
            if (!doc.exists()) {
                return LoginAttemptResult.Allowed
            }
            
            val attempt = doc.toObject(LoginAttempt::class.java) ?: return LoginAttemptResult.Allowed
            
            // Check if account is locked
            if (attempt.lockedUntil != null && attempt.lockedUntil > System.currentTimeMillis()) {
                return LoginAttemptResult.Locked(attempt.lockedUntil)
            }
            
            // Check if attempts should be reset (after ATTEMPT_RESET_DURATION)
            val timeSinceLastAttempt = System.currentTimeMillis() - attempt.lastAttemptAt
            if (timeSinceLastAttempt > ATTEMPT_RESET_DURATION) {
                // Reset attempts
                clearAttempts(userId)
                return LoginAttemptResult.Allowed
            }
            
            LoginAttemptResult.Allowed
        } catch (e: Exception) {
            // If we can't check, allow the attempt (fail-open for availability)
            LoginAttemptResult.Allowed
        }
    }
    
    /**
     * Record a failed login attempt
     * @param userId The student ID or teacher ID
     * @param ipAddress Optional IP address of the attempt
     * @param deviceInfo Optional device information
     * @return LoginAttemptResult indicating if account is now locked
     */
    suspend fun recordFailedAttempt(
        userId: String,
        ipAddress: String? = null,
        deviceInfo: String? = null
    ): LoginAttemptResult {
        return try {
            val docRef = collection.document(userId)
            val snapshot = docRef.get().await()
            
            val current = if (snapshot.exists()) {
                snapshot.toObject(LoginAttempt::class.java) ?: LoginAttempt(id = userId, userId = userId)
            } else {
                LoginAttempt(id = userId, userId = userId)
            }
            
            // Check if account is already locked
            if (current.lockedUntil != null && current.lockedUntil > System.currentTimeMillis()) {
                return LoginAttemptResult.Locked(current.lockedUntil)
            }
            
            // Check if attempts should be reset
            val timeSinceLastAttempt = System.currentTimeMillis() - current.lastAttemptAt
            val newAttempts = if (timeSinceLastAttempt > ATTEMPT_RESET_DURATION) {
                1 // Reset and start fresh
            } else {
                current.attempts + 1
            }
            
            // Determine if account should be locked
            val newLockout = if (newAttempts >= MAX_ATTEMPTS) {
                System.currentTimeMillis() + LOCKOUT_DURATION
            } else {
                null
            }
            
            // Save updated attempt record
            val updated = LoginAttempt(
                id = userId,
                userId = userId,
                attempts = newAttempts,
                lastAttemptAt = System.currentTimeMillis(),
                lockedUntil = newLockout,
                ipAddress = ipAddress,
                deviceInfo = deviceInfo,
                createdAt = current.createdAt
            )
            
            docRef.set(updated).await()
            
            // Return result
            if (newLockout != null) {
                LoginAttemptResult.Locked(newLockout)
            } else {
                LoginAttemptResult.Failed(MAX_ATTEMPTS - newAttempts)
            }
        } catch (e: Exception) {
            // If we can't record, return a generic failed result
            LoginAttemptResult.Failed(MAX_ATTEMPTS - 1)
        }
    }
    
    /**
     * Clear login attempts after successful login
     * @param userId The student ID or teacher ID
     */
    suspend fun clearAttempts(userId: String) {
        try {
            collection.document(userId).delete().await()
        } catch (e: Exception) {
            // Ignore errors when clearing
        }
    }
    
    /**
     * Manually unlock an account (admin function)
     * @param userId The student ID or teacher ID
     */
    suspend fun unlockAccount(userId: String): Result<Unit> {
        return try {
            collection.document(userId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get login attempt information for a user (admin function)
     * @param userId The student ID or teacher ID
     */
    suspend fun getAttemptInfo(userId: String): Result<LoginAttempt?> {
        return try {
            val doc = collection.document(userId).get().await()
            val attempt = if (doc.exists()) {
                doc.toObject(LoginAttempt::class.java)
            } else {
                null
            }
            Result.success(attempt)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all locked accounts (admin function)
     */
    suspend fun getLockedAccounts(): Result<List<LoginAttempt>> {
        return try {
            val currentTime = System.currentTimeMillis()
            val snapshot = collection
                .whereGreaterThan("lockedUntil", currentTime)
                .get()
                .await()
            
            val attempts = snapshot.toObjects(LoginAttempt::class.java)
            Result.success(attempts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

