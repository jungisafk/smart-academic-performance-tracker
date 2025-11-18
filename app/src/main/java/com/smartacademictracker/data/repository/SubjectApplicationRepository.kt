package com.smartacademictracker.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.smartacademictracker.data.model.SubjectApplication
import com.smartacademictracker.data.model.ApplicationStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubjectApplicationRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val applicationsCollection = firestore.collection("subject_applications")

    suspend fun createApplication(application: SubjectApplication): Result<SubjectApplication> {
        return try {
            val docRef = applicationsCollection.add(application).await()
            val createdApplication = application.copy(id = docRef.id)
            applicationsCollection.document(docRef.id).set(createdApplication).await()
            Result.success(createdApplication)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getApplicationById(applicationId: String): Result<SubjectApplication> {
        return try {
            val document = applicationsCollection.document(applicationId).get().await()
            val application = document.toObject(SubjectApplication::class.java)
                ?: throw Exception("Application not found")
            Result.success(application)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getApplicationsByStudentId(studentId: String): Result<List<SubjectApplication>> {
        return try {
            val snapshot = applicationsCollection
                .whereEqualTo("studentId", studentId)
                .get()
                .await()
            val applications = snapshot.toObjects(SubjectApplication::class.java)
            Result.success(applications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateApplicationStatus(
        applicationId: String,
        status: ApplicationStatus,
        processedBy: String? = null,
        remarks: String? = null
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "status" to status.name,
                "processedDate" to System.currentTimeMillis()
            )
            processedBy?.let { updates["processedBy"] = it }
            remarks?.let { updates["remarks"] = it }
            
            applicationsCollection.document(applicationId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun withdrawApplication(applicationId: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "status" to ApplicationStatus.WITHDRAWN.name,
                "processedDate" to System.currentTimeMillis()
            )
            applicationsCollection.document(applicationId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllApplications(): Result<List<SubjectApplication>> {
        return try {
            val snapshot = applicationsCollection.get().await()
            val applications = snapshot.toObjects(SubjectApplication::class.java)
            Result.success(applications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getApplicationsByStatus(status: ApplicationStatus): Result<List<SubjectApplication>> {
        return try {
            val snapshot = applicationsCollection
                .whereEqualTo("status", status.name)
                .get()
                .await()
            val applications = snapshot.toObjects(SubjectApplication::class.java)
            Result.success(applications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getApplicationsForTeacherSubjects(subjectIds: List<String>): Result<List<SubjectApplication>> {
        return try {
            if (subjectIds.isEmpty()) return Result.success(emptyList())

            val applications = mutableListOf<SubjectApplication>()
            val batches = subjectIds.chunked(10)
            for (batch in batches) {
                val snapshot = applicationsCollection
                    .whereIn("subjectId", batch)
                    .get()
                    .await()
                applications.addAll(snapshot.toObjects(SubjectApplication::class.java))
            }
            val sorted = applications.sortedByDescending { it.appliedDate }
            Result.success(sorted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get real-time flow of applications for teacher's subjects
     */
    fun getApplicationsForTeacherSubjectsFlow(subjectIds: List<String>): Flow<List<SubjectApplication>> = callbackFlow {
        if (subjectIds.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listeners = mutableListOf<ListenerRegistration>()
        val allApplications = mutableListOf<SubjectApplication>()

        try {
            val batches = subjectIds.chunked(10)

            batches.forEach { batch ->
                val listener = applicationsCollection
                    .whereIn("subjectId", batch)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("SubjectApplicationRepo", "Real-time listener error: ${error.message}")
                            close(error)
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            val batchApplications = snapshot.toObjects(SubjectApplication::class.java)
                            // Update applications for this batch
                            allApplications.removeAll { it.subjectId in batch }
                            allApplications.addAll(batchApplications)
                            // Sort and send updated list
                            val sorted = allApplications.sortedByDescending { it.appliedDate }
                            trySend(sorted)
                        }
                    }
                listeners.add(listener)
            }
        } catch (e: Exception) {
            Log.e("SubjectApplicationRepo", "Error setting up real-time listeners: ${e.message}")
            close(e)
        }

        awaitClose {
            listeners.forEach { it.remove() }
        }
    }

    /**
     * Get real-time flow of applications by student
     */
    fun getApplicationsByStudentIdFlow(studentId: String): Flow<List<SubjectApplication>> = callbackFlow {
        val listener = applicationsCollection
            .whereEqualTo("studentId", studentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("SubjectApplicationRepo", "Real-time listener error: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val applications = snapshot.toObjects(SubjectApplication::class.java)
                    trySend(applications)
                }
            }

        awaitClose {
            listener.remove()
        }
    }
}
