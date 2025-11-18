package com.smartacademictracker.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.smartacademictracker.data.model.StudentApplication
import com.smartacademictracker.data.model.StudentApplicationStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudentApplicationRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val applicationsCollection = firestore.collection("student_applications")

    suspend fun createApplication(application: StudentApplication): Result<StudentApplication> {
        return try {
            val docRef = applicationsCollection.add(application).await()
            val createdApplication = application.copy(id = docRef.id)
            applicationsCollection.document(docRef.id).set(createdApplication).await()
            Result.success(createdApplication)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getApplicationsByStudent(studentId: String): Result<List<StudentApplication>> {
        return try {
            val snapshot = applicationsCollection
                .whereEqualTo("studentId", studentId)
                .get()
                .await()
            val applications = snapshot.toObjects(StudentApplication::class.java)
                .sortedByDescending { it.appliedAt } // Sort in memory instead
            Result.success(applications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getApplicationsBySubject(subjectId: String): Result<List<StudentApplication>> {
        return try {
            val snapshot = applicationsCollection
                .whereEqualTo("subjectId", subjectId)
                .orderBy("appliedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            val applications = snapshot.toObjects(StudentApplication::class.java)
            Result.success(applications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getApplicationsByTeacher(teacherId: String): Result<List<StudentApplication>> {
        return try {
            val snapshot = applicationsCollection
                .whereEqualTo("reviewedBy", teacherId)
                .orderBy("appliedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            val applications = snapshot.toObjects(StudentApplication::class.java)
            Result.success(applications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getApplicationsForTeacherSubjects(teacherId: String, subjectIds: List<String>): Result<List<StudentApplication>> {
        return try {
            if (subjectIds.isEmpty()) {
                return Result.success(emptyList())
            }
            
            val applications = mutableListOf<StudentApplication>()
            
            // Firestore doesn't support 'in' queries with more than 10 items, so we need to batch them
            val batches = subjectIds.chunked(10)
            
            for (batch in batches) {
                val snapshot = applicationsCollection
                    .whereIn("subjectId", batch)
                    .get()
                    .await()
                val batchApplications = snapshot.toObjects(StudentApplication::class.java)
                applications.addAll(batchApplications)
            }
            
            // Sort in memory instead of using orderBy
            val sortedApplications = applications.sortedByDescending { it.appliedAt }
            
            Result.success(sortedApplications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPendingApplicationsByTeacher(teacherId: String): Result<List<StudentApplication>> {
        return try {
            val snapshot = applicationsCollection
                .whereEqualTo("reviewedBy", teacherId)
                .whereEqualTo("status", StudentApplicationStatus.PENDING.name)
                .orderBy("appliedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            val applications = snapshot.toObjects(StudentApplication::class.java)
            Result.success(applications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateApplicationStatus(
        applicationId: String,
        status: StudentApplicationStatus,
        teacherId: String,
        comments: String? = null
    ): Result<Unit> {
        return try {
            val updateData = mutableMapOf<String, Any>(
                "status" to status.name,
                "reviewedAt" to System.currentTimeMillis(),
                "reviewedBy" to teacherId
            )
            
            if (comments != null) {
                updateData["teacherComments"] = comments
            }
            
            applicationsCollection.document(applicationId).update(updateData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun hasStudentAppliedForSubject(studentId: String, subjectId: String): Result<Boolean> {
        return try {
            val snapshot = applicationsCollection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("subjectId", subjectId)
                .get()
                .await()
            // Only return true if there's a PENDING application
            // APPROVED applications should only block if student has ACTIVE enrollment
            // (This check is done in the ViewModel by checking enrollment status)
            // WITHDRAWN and REJECTED applications should allow reapplication
            val applications = snapshot.toObjects(StudentApplication::class.java)
            val hasPendingApplication = applications.any { app ->
                app.status == StudentApplicationStatus.PENDING
            }
            Result.success(hasPendingApplication)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getApplicationById(applicationId: String): Result<StudentApplication?> {
        return try {
            val document = applicationsCollection.document(applicationId).get().await()
            if (document.exists()) {
                val application = document.toObject(StudentApplication::class.java)
                Result.success(application)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllApplications(): Result<List<StudentApplication>> {
        return try {
            val snapshot = applicationsCollection.get().await()
            val applications = snapshot.toObjects(StudentApplication::class.java)
                .sortedByDescending { it.appliedAt }
            Result.success(applications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteApplication(applicationId: String): Result<Unit> {
        return try {
            applicationsCollection.document(applicationId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get real-time flow of applications for teacher's subjects
     */
    fun getApplicationsForTeacherSubjectsFlow(teacherId: String, subjectIds: List<String>): Flow<List<StudentApplication>> = callbackFlow {
        if (subjectIds.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listeners = mutableListOf<ListenerRegistration>()
        val allApplications = mutableListOf<StudentApplication>()

        try {
            // Firestore doesn't support 'in' queries with more than 10 items, so we need to batch them
            val batches = subjectIds.chunked(10)

            batches.forEach { batch ->
                val listener = applicationsCollection
                    .whereIn("subjectId", batch)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("StudentApplicationRepo", "Real-time listener error: ${error.message}")
                            close(error)
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            val batchApplications = snapshot.toObjects(StudentApplication::class.java)
                            // Update applications for this batch
                            allApplications.removeAll { it.subjectId in batch }
                            allApplications.addAll(batchApplications)
                            // Sort and send updated list
                            val sorted = allApplications.sortedByDescending { it.appliedAt }
                            trySend(sorted)
                        }
                    }
                listeners.add(listener)
            }
        } catch (e: Exception) {
            Log.e("StudentApplicationRepo", "Error setting up real-time listeners: ${e.message}")
            close(e)
        }

        awaitClose {
            listeners.forEach { it.remove() }
        }
    }

    /**
     * Get real-time flow of applications by student
     */
    fun getApplicationsByStudentFlow(studentId: String): Flow<List<StudentApplication>> = callbackFlow {
        val listener = applicationsCollection
            .whereEqualTo("studentId", studentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("StudentApplicationRepo", "Real-time listener error: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val applications = snapshot.toObjects(StudentApplication::class.java)
                        .sortedByDescending { it.appliedAt }
                    trySend(applications)
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    /**
     * Get real-time flow of applications by subject
     */
    fun getApplicationsBySubjectFlow(subjectId: String): Flow<List<StudentApplication>> = callbackFlow {
        val listener = applicationsCollection
            .whereEqualTo("subjectId", subjectId)
            .orderBy("appliedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("StudentApplicationRepo", "Real-time listener error: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val applications = snapshot.toObjects(StudentApplication::class.java)
                    trySend(applications)
                }
            }

        awaitClose {
            listener.remove()
        }
    }
}
