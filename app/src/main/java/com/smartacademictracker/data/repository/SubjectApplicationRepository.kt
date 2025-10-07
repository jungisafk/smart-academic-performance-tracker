package com.smartacademictracker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.smartacademictracker.data.model.SubjectApplication
import com.smartacademictracker.data.model.ApplicationStatus
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
}
