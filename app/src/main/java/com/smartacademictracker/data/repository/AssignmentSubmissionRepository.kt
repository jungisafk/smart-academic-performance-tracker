package com.smartacademictracker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.smartacademictracker.data.model.AssignmentSubmission
import com.smartacademictracker.data.model.SubmissionStatus
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssignmentSubmissionRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val submissionsCollection = firestore.collection("assignment_submissions")

    suspend fun createSubmission(submission: AssignmentSubmission): Result<AssignmentSubmission> {
        return try {
            val docRef = submissionsCollection.add(submission).await()
            val createdSubmission = submission.copy(id = docRef.id)
            submissionsCollection.document(docRef.id).set(createdSubmission).await()
            Result.success(createdSubmission)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSubmissionsBySubject(subjectId: String): Result<List<AssignmentSubmission>> {
        return try {
            val snapshot = submissionsCollection
                .whereEqualTo("subjectId", subjectId)
                .get()
                .await()
            val submissions = snapshot.toObjects(AssignmentSubmission::class.java)
            Result.success(submissions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSubmissionsByStudent(studentId: String): Result<List<AssignmentSubmission>> {
        return try {
            val snapshot = submissionsCollection
                .whereEqualTo("studentId", studentId)
                .get()
                .await()
            val submissions = snapshot.toObjects(AssignmentSubmission::class.java)
            Result.success(submissions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSubmissionsByStatus(status: SubmissionStatus): Result<List<AssignmentSubmission>> {
        return try {
            val snapshot = submissionsCollection
                .whereEqualTo("status", status.name)
                .get()
                .await()
            val submissions = snapshot.toObjects(AssignmentSubmission::class.java)
            Result.success(submissions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSubmissionStatus(
        submissionId: String,
        status: SubmissionStatus,
        feedback: String? = null,
        grade: Double? = null
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "status" to status.name
            )
            feedback?.let { updates["feedback"] = it }
            grade?.let { updates["grade"] = it }
            if (status == SubmissionStatus.GRADED) {
                updates["gradedDate"] = System.currentTimeMillis()
            }
            
            submissionsCollection.document(submissionId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSubmissionById(submissionId: String): Result<AssignmentSubmission> {
        return try {
            val document = submissionsCollection.document(submissionId).get().await()
            val submission = document.toObject(AssignmentSubmission::class.java)
                ?: throw Exception("Submission not found")
            Result.success(submission)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLateSubmissions(): Result<List<AssignmentSubmission>> {
        return try {
            val currentTime = System.currentTimeMillis()
            val snapshot = submissionsCollection
                .whereEqualTo("lateSubmission", true)
                .get()
                .await()
            val submissions = snapshot.toObjects(AssignmentSubmission::class.java)
            Result.success(submissions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSubmissionStatistics(subjectId: String): Result<SubmissionStatistics> {
        return try {
            val submissions = getSubmissionsBySubject(subjectId).getOrThrow()
            
            val totalSubmissions = submissions.size
            val submittedCount = submissions.count { it.status == SubmissionStatus.SUBMITTED }
            val lateCount = submissions.count { it.status == SubmissionStatus.LATE }
            val gradedCount = submissions.count { it.status == SubmissionStatus.GRADED }
            val pendingCount = submissions.count { it.status == SubmissionStatus.PENDING }
            
            val submissionRate = if (totalSubmissions > 0) {
                (submittedCount.toDouble() / totalSubmissions) * 100
            } else 0.0
            
            val statistics = SubmissionStatistics(
                totalAssignments = totalSubmissions,
                submittedCount = submittedCount,
                lateCount = lateCount,
                gradedCount = gradedCount,
                pendingCount = pendingCount,
                submissionRate = submissionRate
            )
            
            Result.success(statistics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class SubmissionStatistics(
    val totalAssignments: Int,
    val submittedCount: Int,
    val lateCount: Int,
    val gradedCount: Int,
    val pendingCount: Int,
    val submissionRate: Double
)
