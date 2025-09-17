package com.smartacademictracker.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.smartacademictracker.data.model.User
import com.smartacademictracker.data.model.UserRole
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val usersCollection = firestore.collection("users")

    suspend fun createUser(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        role: UserRole
    ): Result<User> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: throw Exception("Failed to create user")
            
            val user = User(
                id = userId,
                email = email,
                firstName = firstName,
                lastName = lastName,
                role = role.value
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
            val user = document.toObject(User::class.java)
                ?: throw Exception("User not found")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
            val users = snapshot.toObjects(User::class.java)
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUsersByRole(role: UserRole): Result<List<User>> {
        return try {
            val snapshot = usersCollection
                .whereEqualTo("role", role.name)
                .get()
                .await()
            val users = snapshot.toObjects(User::class.java)
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
}
