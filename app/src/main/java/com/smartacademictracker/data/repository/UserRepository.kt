package com.smartacademictracker.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.smartacademictracker.data.model.User
import com.smartacademictracker.data.model.UserRole
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val preRegisteredRepository: PreRegisteredRepository,
    private val loginAttemptTracker: LoginAttemptTracker
) {
    private val usersCollection = firestore.collection("users")
    
    companion object {
        private const val SCHOOL_DOMAIN = "sjp2cd.edu.ph"  // Configure this for your institution
    }

    suspend fun createUser(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        role: UserRole,
        courseId: String? = null,
        yearLevelId: String? = null,
        departmentCourseId: String? = null
    ): Result<User> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: throw Exception("Failed to create user")
            
            val user = User(
                id = userId,
                email = email,
                firstName = firstName,
                lastName = lastName,
                role = role.value,
                courseId = courseId,
                yearLevelId = yearLevelId,
                departmentCourseId = departmentCourseId
            )
            
            usersCollection.document(userId).set(user).await()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: throw Exception("Failed to sign in")
            
            val user = getUserById(userId).getOrThrow()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserById(userId: String): Result<User> {
        return try {
            val document = usersCollection.document(userId).get().await()
            if (!document.exists()) {
                throw Exception("User not found")
            }
            
            // Convert Firestore document to User, handling Timestamp to Long conversion
            val data = document.data ?: throw Exception("User data is null")
            val user = convertDocumentToUser(document.id, data)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Convert Firestore document data to User object, handling Timestamp to Long conversion
     */
    private fun convertDocumentToUser(documentId: String, data: Map<String, Any?>): User {
        // Helper function to convert Timestamp to Long
        fun convertTimestamp(value: Any?): Long? {
            return when (value) {
                is Timestamp -> {
                    // Convert Firebase Timestamp to milliseconds
                    // Timestamp has seconds (Long) and nanoseconds (Int)
                    value.seconds * 1000 + value.nanoseconds / 1_000_000
                }
                is Long -> value
                is Number -> value.toLong()
                null -> null
                else -> null
            }
        }
        
        return User(
            id = documentId,
            email = data["email"] as? String ?: "",
            studentId = data["studentId"] as? String,
            teacherId = data["teacherId"] as? String,
            employeeId = data["employeeId"] as? String,
            firstName = data["firstName"] as? String ?: "",
            lastName = data["lastName"] as? String ?: "",
            middleName = data["middleName"] as? String,
            suffix = data["suffix"] as? String,
            role = data["role"] as? String ?: "STUDENT",
            profileImageUrl = data["profileImageUrl"] as? String,
            createdAt = convertTimestamp(data["createdAt"]) ?: System.currentTimeMillis(),
            active = data["active"] as? Boolean ?: true,
            yearLevelId = data["yearLevelId"] as? String,
            courseId = data["courseId"] as? String,
            section = data["section"] as? String,
            enrollmentYear = data["enrollmentYear"] as? String,
            lastAcademicPeriodId = data["lastAcademicPeriodId"] as? String,
            departmentCourseId = data["departmentCourseId"] as? String,
            employmentType = data["employmentType"] as? String,
            position = data["position"] as? String,
            specialization = data["specialization"] as? String,
            dateHired = data["dateHired"] as? String,
            yearLevelName = data["yearLevelName"] as? String,
            courseName = data["courseName"] as? String,
            courseCode = data["courseCode"] as? String,
            departmentCourseName = data["departmentCourseName"] as? String,
            departmentCourseCode = data["departmentCourseCode"] as? String,
            lastLoginAt = convertTimestamp(data["lastLoginAt"]),
            passwordChangedAt = convertTimestamp(data["passwordChangedAt"]),
            mustChangePassword = data["mustChangePassword"] as? Boolean ?: false,
            accountSource = data["accountSource"] as? String ?: "MANUAL",
            actualEmail = data["actualEmail"] as? String
        )
    }

    suspend fun getCurrentUser(): Result<User?> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                getUserById(currentUser.uid)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get real-time flow of current user
     * This listens to changes in Firebase Auth state and the current user's document
     * IMPORTANT: When a new user signs in, this Flow will restart and listen to the new user's document
     */
    fun getCurrentUserFlow(): Flow<User?> = callbackFlow {
        var firestoreListener: com.google.firebase.firestore.ListenerRegistration? = null
        
        // Listen to Firebase Auth state changes
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            // Remove old listener when auth state changes
            firestoreListener?.remove()
            firestoreListener = null
            
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                trySend(null)
                return@AuthStateListener
            }
            
            // Set up new listener for the current user's document
            firestoreListener = usersCollection.document(currentUser.uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("UserRepository", "Error in Firestore listener: ${error.message}", error)
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null && snapshot.exists()) {
                        try {
                            val data = snapshot.data ?: return@addSnapshotListener
                            val user = convertDocumentToUser(snapshot.id, data)
                            trySend(user)
                        } catch (e: Exception) {
                            // Skip corrupted user data
                            android.util.Log.e("UserRepository", "Error converting user document: ${e.message}", e)
                        }
                    } else {
                        trySend(null)
                    }
                }
        }
        
        // Register auth state listener
        auth.addAuthStateListener(authStateListener)
        
        // Trigger initial state check
        authStateListener.onAuthStateChanged(auth)
        
        awaitClose {
            // Clean up listeners
            auth.removeAuthStateListener(authStateListener)
            firestoreListener?.remove()
        }
    }

    suspend fun updateUser(user: User): Result<Unit> {
        return try {
            usersCollection.document(user.id).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllUsers(): Result<List<User>> {
        return try {
            val snapshot = usersCollection.get().await()
            val users = snapshot.documents.mapNotNull { doc ->
                try {
                    convertDocumentToUser(doc.id, doc.data ?: emptyMap())
                } catch (e: Exception) {
                    null
                }
            }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUsersByRole(role: UserRole): Result<List<User>> {
        return try {
            val snapshot = usersCollection
                .whereEqualTo("role", role.value)
                .get()
                .await()
            val users = snapshot.documents.mapNotNull { doc ->
                try {
                    convertDocumentToUser(doc.id, doc.data ?: emptyMap())
                } catch (e: Exception) {
                    null
                }
            }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }

    fun isUserSignedIn(): Boolean {
        return auth.currentUser != null
    }

    suspend fun updateUserStatus(userId: String, active: Boolean): Result<Unit> {
        return try {
            usersCollection.document(userId).update("active", active).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserRole(userId: String, newRole: String): Result<Unit> {
        return try {
            usersCollection.document(userId).update("role", newRole).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTeacherDepartment(userId: String, departmentCourseId: String?): Result<Unit> {
        return try {
            val updates = hashMapOf<String, Any?>(
                "departmentCourseId" to departmentCourseId
            )
            usersCollection.document(userId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update student's year level (used for year level progression)
     */
    suspend fun updateStudentYearLevel(
        studentId: String,
        newYearLevelId: String,
        newYearLevelName: String,
        academicPeriodId: String
    ): Result<Unit> {
        return try {
            android.util.Log.d("UserRepository", "updateStudentYearLevel - studentId: $studentId, newYearLevelId: $newYearLevelId, newYearLevelName: $newYearLevelName, academicPeriodId: $academicPeriodId")
            val updates = hashMapOf<String, Any>(
                "yearLevelId" to newYearLevelId,
                "yearLevelName" to newYearLevelName, // IMPORTANT: Update yearLevelName so UI displays correctly
                "lastAcademicPeriodId" to academicPeriodId
            )
            usersCollection.document(studentId).update(updates).await()
            android.util.Log.d("UserRepository", "updateStudentYearLevel - Successfully updated student $studentId to year level $newYearLevelName")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "updateStudentYearLevel - Failed to update student $studentId: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update student's last academic period (without changing year level)
     */
    suspend fun updateStudentAcademicPeriod(
        studentId: String,
        academicPeriodId: String
    ): Result<Unit> {
        return try {
            val updates = hashMapOf<String, Any>(
                "lastAcademicPeriodId" to academicPeriodId
            )
            usersCollection.document(studentId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update user profile information (firstName, lastName, middleName)
     */
    suspend fun updateUserProfile(
        userId: String,
        firstName: String,
        lastName: String,
        middleName: String?
    ): Result<Unit> {
        return try {
            val updates = hashMapOf<String, Any?>(
                "firstName" to firstName,
                "lastName" to lastName,
                "middleName" to middleName
            )
            usersCollection.document(userId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Change user password using Firebase Auth
     * Requires re-authentication with current password
     */
    suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("No user signed in"))
            
            // Re-authenticate with current password
            val email = currentUser.email
                ?: return Result.failure(Exception("User email not found"))
            
            // Validate that current password is not empty
            if (currentPassword.isBlank()) {
                return Result.failure(Exception("Current password cannot be empty"))
            }
            
            // Validate that new password is not empty
            if (newPassword.isBlank()) {
                return Result.failure(Exception("New password cannot be empty"))
            }
            
            // Check if new password is different from current password
            if (currentPassword == newPassword) {
                return Result.failure(Exception("New password must be different from current password"))
            }
            
            try {
                val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, currentPassword)
                currentUser.reauthenticate(credential).await()
            } catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
                // Handle specific Firebase Auth credential errors
                return Result.failure(Exception("Current password is incorrect. Please check your password and try again."))
            } catch (e: com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                return Result.failure(Exception("User account is invalid or has been disabled. Please contact support."))
            } catch (e: com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                return Result.failure(Exception("For security reasons, please sign out and sign in again before changing your password."))
            } catch (e: Exception) {
                // Check for specific error messages
                val errorMessage = e.message ?: "Authentication failed"
                when {
                    errorMessage.contains("auth/wrong-password", ignoreCase = true) ||
                    errorMessage.contains("auth/invalid-credential", ignoreCase = true) ||
                    errorMessage.contains("incorrect", ignoreCase = true) ||
                    errorMessage.contains("malformed", ignoreCase = true) -> {
                        return Result.failure(Exception("Current password is incorrect. Please check your password and try again."))
                    }
                    errorMessage.contains("auth/requires-recent-login", ignoreCase = true) -> {
                        return Result.failure(Exception("For security reasons, please sign out and sign in again before changing your password."))
                    }
                    else -> {
                        return Result.failure(Exception("Authentication failed: $errorMessage"))
                    }
                }
            }
            
            // Update password
            try {
                currentUser.updatePassword(newPassword).await()
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Failed to update password"
                return Result.failure(Exception("Failed to update password: $errorMessage"))
            }
            
            // Update passwordChangedAt timestamp in Firestore
            try {
                usersCollection.document(currentUser.uid).update(
                    "passwordChangedAt", Timestamp.now()
                ).await()
            } catch (e: Exception) {
                // Log but don't fail - password was changed successfully in Auth
                android.util.Log.w("UserRepository", "Failed to update passwordChangedAt timestamp: ${e.message}")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== ID-BASED AUTHENTICATION METHODS ====================
    
    /**
     * Sign in using Student ID or Teacher ID
     * The ID is converted to an email format for Firebase Auth
     * @param userId Student ID (e.g., "2024-1234") or Teacher ID (e.g., "T-2024-001")
     * @param password User's password
     * @param userType The role type (STUDENT or TEACHER)
     */
    suspend fun signInWithId(
        userId: String,
        password: String,
        userType: UserRole
    ): Result<User> {
        return try {
            // Normalize userId (trim whitespace) to ensure consistent lockout checks
            val normalizedUserId = userId.trim()
            
            // Check if login is allowed (rate limiting) - check using the exact userId being attempted
            when (val attemptResult = loginAttemptTracker.checkLoginAllowed(normalizedUserId)) {
                is com.smartacademictracker.data.model.LoginAttemptResult.Locked -> {
                    android.util.Log.w("UserRepository", "Login blocked - account locked for userId: $normalizedUserId")
                    return Result.failure(Exception(attemptResult.getLockMessage()))
                }
                else -> { 
                    android.util.Log.d("UserRepository", "Login allowed for userId: $normalizedUserId")
                    /* Continue */ 
                }
            }
            
            // Convert ID to email format for Firebase (use normalized userId)
            val convertedEmail = convertIdToEmail(normalizedUserId, userType)
            android.util.Log.d("UserRepository", "signInWithId - userId: $normalizedUserId, converted email: $convertedEmail, userType: $userType")
            
            // Try to get actual email from User document first (for activated accounts)
            // Then try pre-registration (for accounts not yet activated)
            var actualEmail: String? = null
            
            // First, try to find user by ID in Firestore to get actualEmail
            try {
                val userDoc = when (userType) {
                    UserRole.STUDENT -> {
                        usersCollection.whereEqualTo("studentId", normalizedUserId).limit(1).get().await()
                            .documents.firstOrNull()
                    }
                    UserRole.TEACHER -> {
                        usersCollection.whereEqualTo("teacherId", normalizedUserId).limit(1).get().await()
                            .documents.firstOrNull()
                    }
                    else -> null
                }
                
                if (userDoc != null) {
                    val userData = userDoc.data
                    actualEmail = userData?.get("actualEmail") as? String
                    if (actualEmail.isNullOrBlank()) {
                        actualEmail = userData?.get("email") as? String
                    }
                    android.util.Log.d("UserRepository", "Found user document - actualEmail: ${userData?.get("actualEmail")}, email: ${userData?.get("email")}")
                }
            } catch (e: Exception) {
                android.util.Log.w("UserRepository", "Could not check User document for actualEmail: ${e.message}")
            }
            
            // If not found in User document, try pre-registration
            if (actualEmail.isNullOrBlank()) {
                actualEmail = when (userType) {
                    UserRole.STUDENT -> {
                        val preReg = preRegisteredRepository.getPreRegisteredStudent(normalizedUserId).getOrNull()
                        preReg?.email?.takeIf { it.isNotBlank() }
                    }
                    UserRole.TEACHER -> {
                        val preReg = preRegisteredRepository.getPreRegisteredTeacher(normalizedUserId).getOrNull()
                        preReg?.email?.takeIf { it.isNotBlank() }
                    }
                    else -> null
                }
            }
            
            // Try actual email first (if available), then converted email (for backward compatibility)
            val emailsToTry = listOfNotNull(actualEmail, convertedEmail).distinct()
            android.util.Log.d("UserRepository", "Trying emails in order: $emailsToTry")
            
            var lastException: Exception? = null
            
            // Authenticate with Firebase - try converted email first, then actual email
            var authResult: com.google.firebase.auth.AuthResult? = null
            try {
                for (email in emailsToTry) {
                    try {
                        android.util.Log.d("UserRepository", "Attempting Firebase Auth sign in with email: $email")
                        val result = auth.signInWithEmailAndPassword(email, password).await()
                        android.util.Log.d("UserRepository", "Firebase Auth sign in successful - UID: ${result.user?.uid}, email: $email")
                        authResult = result
                        break // Success, exit loop
                    } catch (e: Exception) {
                        android.util.Log.w("UserRepository", "Failed to sign in with email $email: ${e.message}")
                        lastException = e
                        // Continue to next email
                    }
                }
                
                if (authResult == null) {
                    // All emails failed
                    throw lastException ?: Exception("Failed to sign in with any email")
                }
            } catch (e: Exception) {
                android.util.Log.e("UserRepository", "Firebase Auth sign in failed for all emails - last error: ${e.message}", e)
                // Record failed attempt using normalized userId
                when (val result = loginAttemptTracker.recordFailedAttempt(normalizedUserId)) {
                    is com.smartacademictracker.data.model.LoginAttemptResult.Failed -> {
                        android.util.Log.w("UserRepository", "Failed attempt recorded for userId: $normalizedUserId, remaining: ${result.remainingAttempts}")
                        return Result.failure(
                            Exception("Invalid credentials. ${result.remainingAttempts} attempt(s) remaining.")
                        )
                    }
                    is com.smartacademictracker.data.model.LoginAttemptResult.Locked -> {
                        android.util.Log.w("UserRepository", "Account locked for userId: $normalizedUserId")
                        return Result.failure(Exception(result.getLockMessage()))
                    }
                    else -> {
                        return Result.failure(Exception("Invalid credentials"))
                    }
                }
            }
            
            val firebaseUserId = authResult?.user?.uid 
                ?: throw Exception("Failed to sign in")
            
            // Fetch user data
            val user = getUserById(firebaseUserId).getOrThrow()
            
            // Clear login attempts on successful login using normalized userId
            loginAttemptTracker.clearAttempts(normalizedUserId)
            android.util.Log.d("UserRepository", "Successful login for userId: $normalizedUserId, cleared attempts")
            
            // Update last login
            updateLastLogin(firebaseUserId)
            
            // If user successfully signed in, ensure pre-registered record is marked as activated
            // This handles cases where activation happened but the record wasn't updated
            when (userType) {
                UserRole.STUDENT -> {
                    try {
                        val preRegisteredResult = preRegisteredRepository.getPreRegisteredStudent(userId)
                        preRegisteredResult.onSuccess { preRegistered ->
                            // If not already marked as registered, mark it now
                            if (!preRegistered.isRegistered || preRegistered.firebaseUserId != firebaseUserId) {
                                android.util.Log.d("UserRepository", "Marking student $userId as registered after successful sign-in")
                                preRegisteredRepository.markStudentAsRegistered(userId, firebaseUserId)
                            }
                        }
                        // Silently fail if pre-registered record doesn't exist (user might have been created differently)
                    } catch (e: Exception) {
                        android.util.Log.w("UserRepository", "Could not update pre-registered status for student $userId: ${e.message}")
                    }
                }
                UserRole.TEACHER -> {
                    try {
                        val preRegisteredResult = preRegisteredRepository.getPreRegisteredTeacher(userId)
                        preRegisteredResult.onSuccess { preRegistered ->
                            // If not already marked as registered, mark it now
                            if (!preRegistered.isRegistered || preRegistered.firebaseUserId != firebaseUserId) {
                                android.util.Log.d("UserRepository", "Marking teacher $userId as registered after successful sign-in")
                                preRegisteredRepository.markTeacherAsRegistered(userId, firebaseUserId)
                            }
                        }
                        // Silently fail if pre-registered record doesn't exist (user might have been created differently)
                    } catch (e: Exception) {
                        android.util.Log.w("UserRepository", "Could not update pre-registered status for teacher $userId: ${e.message}")
                    }
                }
                else -> { /* Admins don't have pre-registered records */ }
            }
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Activate account using pre-registered data
     * First-time registration for students and teachers using their institutional ID
     * @param userId Student ID or Teacher ID from pre-registration
     * @param password New password for the account
     * @param confirmPassword Password confirmation
     * @param userType The role type (STUDENT or TEACHER)
     */
    suspend fun activateAccount(
        userId: String,
        password: String,
        confirmPassword: String,
        userType: UserRole
    ): Result<User> {
        return try {
            // Validate passwords match
            if (password != confirmPassword) {
                throw Exception("Passwords do not match")
            }
            
            // Validate password strength
            val passwordValidation = com.smartacademictracker.util.PasswordValidator.validate(password)
            if (!passwordValidation.isValid) {
                throw Exception(passwordValidation.errors.joinToString("\n"))
            }
            
            // Look up pre-registered data
            val preRegData = when (userType) {
                UserRole.STUDENT -> {
                    preRegisteredRepository.getPreRegisteredStudent(userId).getOrThrow()
                }
                UserRole.TEACHER -> {
                    preRegisteredRepository.getPreRegisteredTeacher(userId).getOrThrow()
                }
                else -> throw Exception("Invalid user type for account activation")
            }
            
            // Check if already registered
            if (when (userType) {
                UserRole.STUDENT -> (preRegData as com.smartacademictracker.data.model.PreRegisteredStudent).isRegistered
                UserRole.TEACHER -> (preRegData as com.smartacademictracker.data.model.PreRegisteredTeacher).isRegistered
                else -> false
            }) {
                throw Exception("Account already activated. Please sign in with your credentials.")
            }
            
            // Get actual email from pre-registration
            val actualEmail = when (userType) {
                UserRole.STUDENT -> (preRegData as com.smartacademictracker.data.model.PreRegisteredStudent).email?.trim()
                UserRole.TEACHER -> (preRegData as com.smartacademictracker.data.model.PreRegisteredTeacher).email?.trim()
                else -> null
            }
            
            // IMPORTANT: Use actual email for Firebase Auth if available (for password reset)
            // Otherwise fall back to converted email for backward compatibility
            val emailForAuth = if (actualEmail.isNullOrBlank()) {
                convertIdToEmail(userId, userType)
            } else {
                actualEmail
            }
            
            android.util.Log.d("UserRepository", "activateAccount - userId: $userId, actualEmail: $actualEmail, emailForAuth: $emailForAuth, userType: $userType")
            
            if (actualEmail.isNullOrBlank()) {
                android.util.Log.w("UserRepository", "No actual email found in pre-registration data, using converted email: $emailForAuth")
            } else {
                android.util.Log.d("UserRepository", "Using actual email from CSV for Firebase Auth: $emailForAuth (password reset will use this email)")
            }
            
            // Create Firebase Auth account with actual email (if available) or converted email (fallback)
            val authResult = auth.createUserWithEmailAndPassword(emailForAuth, password).await()
            val firebaseUserId = authResult.user?.uid 
                ?: throw Exception("Failed to create account")
            
            // Create user document with pre-registered data
            val user = when (userType) {
                UserRole.STUDENT -> createUserFromPreRegStudent(
                    firebaseUserId, 
                    preRegData as com.smartacademictracker.data.model.PreRegisteredStudent
                )
                UserRole.TEACHER -> createUserFromPreRegTeacher(
                    firebaseUserId,
                    preRegData as com.smartacademictracker.data.model.PreRegisteredTeacher
                )
                else -> throw Exception("Invalid user type")
            }
            
            // Save user document
            usersCollection.document(firebaseUserId).set(user).await()
            
            // Mark as registered in pre-registration collection
            when (userType) {
                UserRole.STUDENT -> preRegisteredRepository.markStudentAsRegistered(userId, firebaseUserId)
                UserRole.TEACHER -> preRegisteredRepository.markTeacherAsRegistered(userId, firebaseUserId)
                else -> { /* Do nothing */ }
            }
            
            Result.success(user)
        } catch (e: Exception) {
            // Clean up Firebase Auth if user document creation failed
            try {
                auth.currentUser?.delete()?.await()
            } catch (_: Exception) { }
            
            Result.failure(e)
        }
    }
    
    /**
     * Convert Student/Teacher/Admin ID to email format for Firebase Auth
     * Example: "2024-1234" → "s2024-1234@sjp2cd.edu.ph"
     * Example: "T-2024-001" → "t-2024-001@sjp2cd.edu.ph"
     * Example: "A-2024-001" → "a-2024-001@sjp2cd.edu.ph"
     */
    private fun convertIdToEmail(userId: String, userType: UserRole): String {
        val cleanId = userId.trim().lowercase().replace(" ", "")
        
        return when (userType) {
            UserRole.STUDENT -> {
                // Student IDs don't have a prefix, so add "s"
                "s$cleanId@$SCHOOL_DOMAIN"
            }
            UserRole.TEACHER -> {
                // Teacher IDs already have "T-" prefix, just lowercase it
                "$cleanId@$SCHOOL_DOMAIN"
            }
            UserRole.ADMIN -> {
                // Admin IDs already have "A-" prefix, just lowercase it
                "$cleanId@$SCHOOL_DOMAIN"
            }
            else -> {
                "$cleanId@$SCHOOL_DOMAIN"
            }
        }
    }
    
    /**
     * Create User from PreRegisteredStudent
     */
    private fun createUserFromPreRegStudent(
        firebaseUserId: String,
        preRegStudent: com.smartacademictracker.data.model.PreRegisteredStudent
    ): User {
        // Get actual email from pre-registration
        val actualEmail = preRegStudent.email?.trim()?.takeIf { it.isNotBlank() }
        // Use actual email if available, otherwise use converted email
        val email = actualEmail ?: convertIdToEmail(preRegStudent.studentId, UserRole.STUDENT)
        android.util.Log.d("UserRepository", "createUserFromPreRegStudent - studentId: ${preRegStudent.studentId}, email: $email, actualEmail: $actualEmail")
        
        return User(
            id = firebaseUserId,
            email = email, // Use actual email if available, otherwise converted email
            studentId = preRegStudent.studentId,
            firstName = preRegStudent.firstName,
            lastName = preRegStudent.lastName,
            middleName = preRegStudent.middleName,
            suffix = preRegStudent.suffix,
            role = UserRole.STUDENT.value,
            courseId = preRegStudent.courseId,
            courseName = preRegStudent.courseName,
            courseCode = preRegStudent.courseCode,
            yearLevelId = preRegStudent.yearLevelId,
            yearLevelName = preRegStudent.yearLevelName,
            section = preRegStudent.section,
            enrollmentYear = preRegStudent.enrollmentYear,
            active = true,
            createdAt = System.currentTimeMillis(),
            accountSource = "PRE_REGISTERED",
            actualEmail = actualEmail // Store actual email from CSV for password reset
        )
    }
    
    /**
     * Create User from PreRegisteredTeacher
     */
    private fun createUserFromPreRegTeacher(
        firebaseUserId: String,
        preRegTeacher: com.smartacademictracker.data.model.PreRegisteredTeacher
    ): User {
        // Get actual email from pre-registration
        val actualEmail = preRegTeacher.email?.trim()?.takeIf { it.isNotBlank() }
        // Use actual email if available, otherwise use converted email
        val email = actualEmail ?: convertIdToEmail(preRegTeacher.teacherId, UserRole.TEACHER)
        android.util.Log.d("UserRepository", "createUserFromPreRegTeacher - teacherId: ${preRegTeacher.teacherId}, email: $email, actualEmail: $actualEmail")
        
        return User(
            id = firebaseUserId,
            email = email, // Use actual email if available, otherwise converted email
            teacherId = preRegTeacher.teacherId,
            employeeId = preRegTeacher.employeeNumber,
            firstName = preRegTeacher.firstName,
            lastName = preRegTeacher.lastName,
            middleName = preRegTeacher.middleName,
            suffix = preRegTeacher.suffix,
            role = UserRole.TEACHER.value,
            departmentCourseId = preRegTeacher.departmentCourseId,
            departmentCourseName = preRegTeacher.departmentCourseName,
            departmentCourseCode = preRegTeacher.departmentCourseCode,
            employmentType = preRegTeacher.employmentType.name,
            position = preRegTeacher.position,
            specialization = preRegTeacher.specialization,
            dateHired = preRegTeacher.dateHired,
            active = true,
            createdAt = System.currentTimeMillis(),
            accountSource = "PRE_REGISTERED",
            actualEmail = actualEmail // Store actual email from CSV for password reset
        )
    }
    
    /**
     * Send password reset email
     * Supports both email-based and ID-based authentication
     */
    suspend fun sendPasswordResetEmail(
        identifier: String,
        userType: UserRole
    ): Result<Unit> {
        return try {
            android.util.Log.d("UserRepository", "sendPasswordResetEmail called - identifier: $identifier, userType: $userType")
            
            // IMPORTANT: For password reset, we need to use the SAME email format that's used for login
            // Login uses convertIdToEmail() which converts ID to email format (e.g., "2024-001" -> "s2024-001@sjp2cd.edu.ph")
            // So password reset should also use convertIdToEmail() to ensure consistency
            
            // Determine which email to use for password reset
            val emailToUse: String?
            
            if (android.util.Patterns.EMAIL_ADDRESS.matcher(identifier).matches()) {
                // Identifier is already an email - use it directly
                android.util.Log.d("UserRepository", "Identifier is an email: $identifier")
                emailToUse = identifier
            } else {
                // Identifier is an ID - look up the user's actual email from Firestore
                android.util.Log.d("UserRepository", "Looking up user with ID: $identifier")
                
                val userResult = when (userType) {
                    UserRole.STUDENT -> {
                        usersCollection.whereEqualTo("studentId", identifier).limit(1).get().await()
                            .documents.firstOrNull()?.toObject(User::class.java)
                    }
                    UserRole.TEACHER -> {
                        usersCollection.whereEqualTo("teacherId", identifier).limit(1).get().await()
                            .documents.firstOrNull()?.toObject(User::class.java)
                    }
                    else -> null
                }
                
                if (userResult != null) {
                    // IMPORTANT: Use actualEmail if available (from CSV import), otherwise use email field
                    // actualEmail contains the real email from CSV/pre-registration
                    // email field contains the converted email (s2024-001@sjp2cd.edu.ph) for Firebase Auth
                    val emailFromRecord = userResult.actualEmail?.takeIf { it.isNotBlank() } 
                        ?: userResult.email
                    
                    android.util.Log.d("UserRepository", "Found user in Firestore - actualEmail: ${userResult.actualEmail}, email: ${userResult.email}, using: $emailFromRecord")
                    
                    if (emailFromRecord.isNullOrBlank()) {
                        android.util.Log.e("UserRepository", "User found but email is blank")
                        return Result.failure(Exception("Account found but email is not set. Please contact support."))
                    }
                    
                    emailToUse = emailFromRecord
                } else {
                    // User not found in Firestore - try pre-registration collection
                    android.util.Log.d("UserRepository", "User not found in Firestore, checking pre-registration collection")
                    val preRegEmail = when (userType) {
                        UserRole.STUDENT -> {
                            preRegisteredRepository.getPreRegisteredStudent(identifier).getOrNull()?.email?.trim()
                        }
                        UserRole.TEACHER -> {
                            preRegisteredRepository.getPreRegisteredTeacher(identifier).getOrNull()?.email?.trim()
                        }
                        else -> null
                    }
                    
                    if (preRegEmail != null && preRegEmail.isNotBlank()) {
                        android.util.Log.d("UserRepository", "Found email in pre-registration: $preRegEmail")
                        emailToUse = preRegEmail
                    } else {
                        android.util.Log.w("UserRepository", "User not found in Firestore or pre-registration for ID: $identifier")
                        return Result.failure(Exception("No account found with this ID. Please verify your information and try again. If you haven't activated your account yet, please activate it first."))
                    }
                }
            }
            
            // Validate email is not empty
            if (emailToUse.isNullOrBlank()) {
                android.util.Log.e("UserRepository", "Email is blank for identifier: $identifier")
                return Result.failure(Exception("Email address not found for the provided identifier"))
            }
            
            android.util.Log.d("UserRepository", "Using email for password reset: $emailToUse")
            android.util.Log.d("UserRepository", "Email format valid: ${android.util.Patterns.EMAIL_ADDRESS.matcher(emailToUse).matches()}")
            
            // Send password reset email using the actual email from Firestore
            try {
                android.util.Log.d("UserRepository", "Sending password reset email to: $emailToUse")
                auth.sendPasswordResetEmail(emailToUse).await()
                android.util.Log.d("UserRepository", "Password reset email sent successfully to: $emailToUse")
                android.util.Log.d("UserRepository", "Note: Check your email inbox and spam folder for the reset link")
                return Result.success(Unit)
            } catch (e: com.google.firebase.auth.FirebaseAuthException) {
                val errorCode = e.errorCode
                val errorMessage = e.message ?: "Unknown error"
                android.util.Log.e("UserRepository", "Firebase Auth error sending password reset: $errorCode - $errorMessage", e)
                
                when (errorCode) {
                    "ERROR_USER_NOT_FOUND" -> {
                        android.util.Log.w("UserRepository", "Account not found in Firebase Auth for email: $emailToUse")
                        return Result.failure(Exception("Account found but not activated in Firebase Auth. Please activate your account first before resetting password. If you've already activated your account, please contact support."))
                    }
                    "ERROR_INVALID_EMAIL" -> {
                        return Result.failure(Exception("Invalid email address. Please contact support."))
                    }
                    "ERROR_TOO_MANY_REQUESTS" -> {
                        return Result.failure(Exception("Too many password reset requests. Please wait a few minutes and try again."))
                    }
                    else -> {
                        return Result.failure(Exception("Failed to send password reset email: $errorMessage (Code: $errorCode)"))
                    }
                }
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Failed to send password reset email"
                android.util.Log.e("UserRepository", "Exception sending password reset email: $errorMessage", e)
                
                when {
                    errorMessage.contains("auth/user-not-found", ignoreCase = true) ||
                    errorMessage.contains("ERROR_USER_NOT_FOUND", ignoreCase = true) -> {
                        return Result.failure(Exception("Account found but not activated in Firebase Auth. Please activate your account first before resetting password. If you've already activated your account, please contact support."))
                    }
                    errorMessage.contains("auth/invalid-email", ignoreCase = true) ||
                    errorMessage.contains("ERROR_INVALID_EMAIL", ignoreCase = true) -> {
                        return Result.failure(Exception("Invalid email address. Please contact support."))
                    }
                    errorMessage.contains("auth/too-many-requests", ignoreCase = true) ||
                    errorMessage.contains("ERROR_TOO_MANY_REQUESTS", ignoreCase = true) -> {
                        return Result.failure(Exception("Too many password reset requests. Please wait a few minutes and try again."))
                    }
                    else -> {
                        return Result.failure(Exception("Failed to send password reset email: $errorMessage"))
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Unexpected error in sendPasswordResetEmail: ${e.message}", e)
            Result.failure(Exception("An unexpected error occurred: ${e.message ?: "Unknown error"}"))
        }
    }
    
    /**
     * Update last login timestamp
     */
    private suspend fun updateLastLogin(userId: String) {
        try {
            usersCollection.document(userId)
                .update("lastLoginAt", System.currentTimeMillis())
                .await()
        } catch (_: Exception) {
            // Ignore errors when updating last login
        }
    }
    
    /**
     * Check if a user ID exists (for validation during activation)
     */
    suspend fun checkUserIdExists(userId: String, userType: UserRole): Result<Boolean> {
        return preRegisteredRepository.checkIdExists(userId, userType)
    }
    
    /**
     * Get user by student ID or teacher ID
     */
    suspend fun getUserByInstitutionalId(userId: String, userType: UserRole): Result<User?> {
        return try {
            val field = when (userType) {
                UserRole.STUDENT -> "studentId"
                UserRole.TEACHER -> "teacherId"
                else -> return Result.success(null)
            }
            
            val snapshot = usersCollection
                .whereEqualTo(field, userId)
                .limit(1)
                .get()
                .await()
            
            if (snapshot.isEmpty) {
                Result.success(null)
            } else {
                val doc = snapshot.documents[0]
                val user = convertDocumentToUser(doc.id, doc.data ?: emptyMap())
                Result.success(user)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get real-time flow of all users
     */
    fun getAllUsersFlow(): Flow<List<User>> = callbackFlow {
        val listener = usersCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val users = mutableListOf<User>()
                    for (document in snapshot.documents) {
                        try {
                            val data = document.data ?: continue
                            val user = convertDocumentToUser(document.id, data)
                            users.add(user)
                        } catch (e: Exception) {
                            // Skip corrupted users
                        }
                    }
                    trySend(users)
                }
            }
        
        awaitClose {
            listener.remove()
        }
    }
    
    /**
     * Get real-time flow of users by role
     */
    fun getUsersByRoleFlow(role: UserRole): Flow<List<User>> = callbackFlow {
        val listener = usersCollection
            .whereEqualTo("role", role.value)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val users = mutableListOf<User>()
                    for (document in snapshot.documents) {
                        try {
                            val data = document.data ?: continue
                            val user = convertDocumentToUser(document.id, data)
                            users.add(user)
                        } catch (e: Exception) {
                            // Skip corrupted users
                        }
                    }
                    trySend(users)
                }
            }
        
        awaitClose {
            listener.remove()
        }
    }
}
