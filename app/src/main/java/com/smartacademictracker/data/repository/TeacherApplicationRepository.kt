package com.smartacademictracker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.smartacademictracker.data.model.ApplicationStatus
import com.smartacademictracker.data.model.TeacherApplication
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TeacherApplicationRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val applicationsCollection = firestore.collection("teacher_applications")

    suspend fun createApplication(application: TeacherApplication): Result<TeacherApplication> {
        return try {
            val docRef = applicationsCollection.add(application).await()
            val createdApplication = application.copy(id = docRef.id)
            applicationsCollection.document(docRef.id).set(createdApplication).await()
            Result.success(createdApplication)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateApplication(application: TeacherApplication): Result<Unit> {
        return try {
            applicationsCollection.document(application.id).set(application).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getApplicationById(applicationId: String): Result<TeacherApplication> {
        return try {
            val document = applicationsCollection.document(applicationId).get().await()
            val application = document.toObject(TeacherApplication::class.java)
                ?: throw Exception("Application not found")
            Result.success(application)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllApplications(): Result<List<TeacherApplication>> {
        return try {
            val snapshot = applicationsCollection
                .orderBy("appliedAt", Query.Direction.DESCENDING)
                .get()
                .await()
            val applications = snapshot.toObjects(TeacherApplication::class.java)
            Result.success(applications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getApplicationsByStatus(status: ApplicationStatus): Result<List<TeacherApplication>> {
        return try {
            val snapshot = applicationsCollection
                .whereEqualTo("status", status.name)
                .orderBy("appliedAt", Query.Direction.DESCENDING)
                .get()
                .await()
            val applications = snapshot.toObjects(TeacherApplication::class.java)
            Result.success(applications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getApplicationsByTeacher(teacherId: String): Result<List<TeacherApplication>> {
        return try {
            val snapshot = applicationsCollection
                .whereEqualTo("teacherId", teacherId)
                .orderBy("appliedAt", Query.Direction.DESCENDING)
                .get()
                .await()
            val applications = snapshot.toObjects(TeacherApplication::class.java)
            Result.success(applications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getApplicationsBySubject(subjectId: String): Result<List<TeacherApplication>> {
        return try {
            val snapshot = applicationsCollection
                .whereEqualTo("subjectId", subjectId)
                .orderBy("appliedAt", Query.Direction.DESCENDING)
                .get()
                .await()
            val applications = snapshot.toObjects(TeacherApplication::class.java)
            Result.success(applications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun approveApplication(
        applicationId: String, 
        reviewerId: String, 
        comments: String? = null
    ): Result<Unit> {
        return try {
            val updates = mapOf(
                "status" to ApplicationStatus.APPROVED.name,
                "reviewedAt" to System.currentTimeMillis(),
                "reviewedBy" to reviewerId,
                "adminComments" to comments
            )
            applicationsCollection.document(applicationId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectApplication(
        applicationId: String, 
        reviewerId: String, 
        comments: String? = null
    ): Result<Unit> {
        return try {
            val updates = mapOf(
                "status" to ApplicationStatus.REJECTED.name,
                "reviewedAt" to System.currentTimeMillis(),
                "reviewedBy" to reviewerId,
                "adminComments" to comments
            )
            applicationsCollection.document(applicationId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun hasTeacherAppliedForSubject(teacherId: String, subjectId: String): Result<Boolean> {
        return try {
            val snapshot = applicationsCollection
                .whereEqualTo("teacherId", teacherId)
                .whereEqualTo("subjectId", subjectId)
                .whereEqualTo("status", ApplicationStatus.PENDING.name)
                .get()
                .await()
            Result.success(!snapshot.isEmpty)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateApplicationStatus(applicationId: String, status: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "status" to status,
                "reviewedAt" to System.currentTimeMillis()
            )
            applicationsCollection.document(applicationId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
