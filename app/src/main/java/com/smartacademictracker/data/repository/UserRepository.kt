package com.smartacademictracker.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.smartacademictracker.data.model.User
import com.smartacademictracker.data.model.UserRole
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
            accountSource = data["accountSource"] as? String ?: "MANUAL"
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
            // Check if login is allowed (rate limiting)
            when (val attemptResult = loginAttemptTracker.checkLoginAllowed(userId)) {
                is com.smartacademictracker.data.model.LoginAttemptResult.Locked -> {
                    return Result.failure(Exception(attemptResult.getLockMessage()))
                }
                else -> { /* Continue */ }
            }
            
            // Convert ID to email format for Firebase
            val email = convertIdToEmail(userId, userType)
            
            // Authenticate with Firebase
            val authResult = try {
                auth.signInWithEmailAndPassword(email, password).await()
            } catch (e: Exception) {
                // Record failed attempt
                when (val result = loginAttemptTracker.recordFailedAttempt(userId)) {
                    is com.smartacademictracker.data.model.LoginAttemptResult.Failed -> {
                        return Result.failure(
                            Exception("Invalid credentials. ${result.remainingAttempts} attempt(s) remaining.")
                        )
                    }
                    is com.smartacademictracker.data.model.LoginAttemptResult.Locked -> {
                        return Result.failure(Exception(result.getLockMessage()))
                    }
                    else -> {
                        return Result.failure(Exception("Invalid credentials"))
                    }
                }
            }
            
            val firebaseUserId = authResult.user?.uid 
                ?: throw Exception("Failed to sign in")
            
            // Fetch user data
            val user = getUserById(firebaseUserId).getOrThrow()
            
            // Clear login attempts on successful login
            loginAttemptTracker.clearAttempts(userId)
            
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
            
            // Create Firebase Auth account
            val email = convertIdToEmail(userId, userType)
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
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
        return User(
            id = firebaseUserId,
            email = convertIdToEmail(preRegStudent.studentId, UserRole.STUDENT),
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
            accountSource = "PRE_REGISTERED"
        )
    }
    
    /**
     * Create User from PreRegisteredTeacher
     */
    private fun createUserFromPreRegTeacher(
        firebaseUserId: String,
        preRegTeacher: com.smartacademictracker.data.model.PreRegisteredTeacher
    ): User {
        return User(
            id = firebaseUserId,
            email = convertIdToEmail(preRegTeacher.teacherId, UserRole.TEACHER),
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
            accountSource = "PRE_REGISTERED"
        )
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
}
