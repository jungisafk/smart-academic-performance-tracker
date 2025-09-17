package com.smartacademictracker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.smartacademictracker.data.model.Enrollment
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnrollmentRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val enrollmentsCollection = firestore.collection("enrollments")

    suspend fun createEnrollment(enrollment: Enrollment): Result<Enrollment> {
        return try {
            val docRef = enrollmentsCollection.add(enrollment).await()
            val createdEnrollment = enrollment.copy(id = docRef.id)
            enrollmentsCollection.document(docRef.id).set(createdEnrollment).await()
            Result.success(createdEnrollment)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getEnrollmentsByStudent(studentId: String): Result<List<Enrollment>> {
        return try {
            val snapshot = enrollmentsCollection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("isActive", true)
                .get()
                .await()
            val enrollments = snapshot.toObjects(Enrollment::class.java)
            Result.success(enrollments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getEnrollmentsBySubject(subjectId: String): Result<List<Enrollment>> {
        return try {
            val snapshot = enrollmentsCollection
                .whereEqualTo("subjectId", subjectId)
                .whereEqualTo("isActive", true)
                .get()
                .await()
            val enrollments = snapshot.toObjects(Enrollment::class.java)
            Result.success(enrollments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isStudentEnrolledInSubject(studentId: String, subjectId: String): Result<Boolean> {
        return try {
            val snapshot = enrollmentsCollection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("subjectId", subjectId)
                .whereEqualTo("isActive", true)
                .get()
                .await()
            Result.success(!snapshot.isEmpty)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun enrollStudent(
        studentId: String,
        studentName: String,
        subjectId: String,
        subjectName: String,
        subjectCode: String,
        semester: String,
        academicYear: String
    ): Result<Enrollment> {
        return try {
            val enrollment = Enrollment(
                studentId = studentId,
                studentName = studentName,
                subjectId = subjectId,
                subjectName = subjectName,
                subjectCode = subjectCode,
                semester = semester,
                academicYear = academicYear
            )
            createEnrollment(enrollment)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unenrollStudent(studentId: String, subjectId: String): Result<Unit> {
        return try {
            val snapshot = enrollmentsCollection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("subjectId", subjectId)
                .whereEqualTo("isActive", true)
                .get()
                .await()
            
            for (document in snapshot.documents) {
                enrollmentsCollection.document(document.id)
                    .update("isActive", false)
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
