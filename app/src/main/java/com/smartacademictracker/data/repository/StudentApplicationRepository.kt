package com.smartacademictracker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.smartacademictracker.data.model.StudentApplication
import com.smartacademictracker.data.model.StudentApplicationStatus
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
            println("DEBUG: StudentApplicationRepository - Creating application for student: ${application.studentId}, subject: ${application.subjectId}")
            val docRef = applicationsCollection.add(application).await()
            val createdApplication = application.copy(id = docRef.id)
            println("DEBUG: StudentApplicationRepository - Created document with ID: ${docRef.id}")
            applicationsCollection.document(docRef.id).set(createdApplication).await()
            println("DEBUG: StudentApplicationRepository - Application saved successfully with ID: ${createdApplication.id}")
            Result.success(createdApplication)
        } catch (e: Exception) {
            println("DEBUG: StudentApplicationRepository - Error creating application: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getApplicationsByStudent(studentId: String): Result<List<StudentApplication>> {
        return try {
            println("DEBUG: StudentApplicationRepository - Querying applications for student: $studentId")
            val snapshot = applicationsCollection
                .whereEqualTo("studentId", studentId)
                .get()
                .await()
            val applications = snapshot.toObjects(StudentApplication::class.java)
                .sortedByDescending { it.appliedAt } // Sort in memory instead
            println("DEBUG: StudentApplicationRepository - Found ${applications.size} applications for student $studentId")
            applications.forEach { app ->
                println("DEBUG: Application - ID: ${app.id}, Subject: ${app.subjectName}, Status: ${app.status}")
            }
            Result.success(applications)
        } catch (e: Exception) {
            println("DEBUG: StudentApplicationRepository - Error querying applications: ${e.message}")
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
            println("DEBUG: StudentApplicationRepository - Querying applications for teacher: $teacherId, subjects: $subjectIds")
            
            if (subjectIds.isEmpty()) {
                println("DEBUG: StudentApplicationRepository - No subject IDs provided")
                return Result.success(emptyList())
            }
            
            val applications = mutableListOf<StudentApplication>()
            
            // Firestore doesn't support 'in' queries with more than 10 items, so we need to batch them
            val batches = subjectIds.chunked(10)
            
            for (batch in batches) {
                println("DEBUG: StudentApplicationRepository - Querying batch: $batch")
                val snapshot = applicationsCollection
                    .whereIn("subjectId", batch)
                    .get()
                    .await()
                val batchApplications = snapshot.toObjects(StudentApplication::class.java)
                println("DEBUG: StudentApplicationRepository - Found ${batchApplications.size} applications in batch")
                applications.addAll(batchApplications)
            }
            
            // Sort in memory instead of using orderBy
            val sortedApplications = applications.sortedByDescending { it.appliedAt }
            println("DEBUG: StudentApplicationRepository - Total applications found: ${sortedApplications.size}")
            sortedApplications.forEach { app ->
                println("DEBUG: Application - ID: ${app.id}, Subject: ${app.subjectName}, Student: ${app.studentName}, Status: ${app.status}")
            }
            
            Result.success(sortedApplications)
        } catch (e: Exception) {
            println("DEBUG: StudentApplicationRepository - Error querying applications for teacher subjects: ${e.message}")
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
            Result.success(!snapshot.isEmpty)
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
}
