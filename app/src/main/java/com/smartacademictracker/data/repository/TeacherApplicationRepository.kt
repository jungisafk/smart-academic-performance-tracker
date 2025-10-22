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
                .get()
                .await()
            val applications = snapshot.toObjects(TeacherApplication::class.java)
            // Sort by appliedAt in descending order locally
            val sortedApplications = applications.sortedByDescending { it.appliedAt }
            Result.success(sortedApplications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getApplicationsByStatus(status: ApplicationStatus): Result<List<TeacherApplication>> {
        return try {
            val snapshot = applicationsCollection
                .whereEqualTo("status", status.name)
                .get()
                .await()
            val applications = snapshot.toObjects(TeacherApplication::class.java)
            // Sort by appliedAt in descending order locally
            val sortedApplications = applications.sortedByDescending { it.appliedAt }
            Result.success(sortedApplications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getApplicationsByTeacher(teacherId: String): Result<List<TeacherApplication>> {
        return try {
            val snapshot = applicationsCollection
                .whereEqualTo("teacherId", teacherId)
                .get()
                .await()
            val applications = snapshot.toObjects(TeacherApplication::class.java)
            // Sort by appliedAt in descending order locally
            val sortedApplications = applications.sortedByDescending { it.appliedAt }
            Result.success(sortedApplications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getApplicationsBySubject(subjectId: String): Result<List<TeacherApplication>> {
        return try {
            val snapshot = applicationsCollection
                .whereEqualTo("subjectId", subjectId)
                .get()
                .await()
            val applications = snapshot.toObjects(TeacherApplication::class.java)
            // Sort by appliedAt in descending order locally
            val sortedApplications = applications.sortedByDescending { it.appliedAt }
            Result.success(sortedApplications)
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

    suspend fun cancelApplication(applicationId: String): Result<Unit> {
        return try {
            applicationsCollection.document(applicationId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun hasTeacherAppliedForSubjectAnyStatus(teacherId: String, subjectId: String): Result<Boolean> {
        return try {
            val snapshot = applicationsCollection
                .whereEqualTo("teacherId", teacherId)
                .whereEqualTo("subjectId", subjectId)
                .get()
                .await()
            Result.success(!snapshot.isEmpty)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDuplicateApplications(): Result<List<TeacherApplication>> {
        return try {
            val snapshot = applicationsCollection.get().await()
            val applications = snapshot.toObjects(TeacherApplication::class.java)
            
            // Group by teacherId and subjectId
            val groupedApplications = applications.groupBy { "${it.teacherId}_${it.subjectId}" }
            
            // Find groups with more than one application
            val duplicates = groupedApplications.values.filter { it.size > 1 }.flatten()
            
            Result.success(duplicates)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeDuplicateApplications(keepMostRecent: Boolean = true): Result<Int> {
        return try {
            val duplicatesResult = getDuplicateApplications()
            if (duplicatesResult.isFailure) {
                return Result.failure(duplicatesResult.exceptionOrNull() ?: Exception("Failed to get duplicates"))
            }
            
            val duplicates = duplicatesResult.getOrThrow()
            if (duplicates.isEmpty()) {
                return Result.success(0)
            }
            
            // Group duplicates by teacher-subject pair
            val groupedDuplicates = duplicates.groupBy { "${it.teacherId}_${it.subjectId}" }
            var deletedCount = 0
            
            groupedDuplicates.values.forEach { duplicateGroup ->
                if (duplicateGroup.size > 1) {
                    // Sort by appliedAt (most recent first if keepMostRecent is true)
                    val sortedGroup = if (keepMostRecent) {
                        duplicateGroup.sortedByDescending { it.appliedAt }
                    } else {
                        duplicateGroup.sortedBy { it.appliedAt }
                    }
                    
                    // Keep the first one, delete the rest
                    val toDelete = sortedGroup.drop(1)
                    
                    toDelete.forEach { application ->
                        try {
                            applicationsCollection.document(application.id).delete().await()
                            deletedCount++
                        } catch (e: Exception) {
                            println("Error deleting application ${application.id}: ${e.message}")
                        }
                    }
                }
            }
            
            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
