package com.smartacademictracker.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.smartacademictracker.data.model.StudentEnrollment
import com.smartacademictracker.data.model.EnrollmentStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudentEnrollmentRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val enrollmentsCollection = firestore.collection("student_enrollments")

    /**
     * Get all enrollments for a specific student
     */
    suspend fun getEnrollmentsByStudent(studentId: String): Result<List<StudentEnrollment>> {
        return try {
            val snapshot = enrollmentsCollection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("status", EnrollmentStatus.ACTIVE.name)
                .get()
                .await()
            val enrollments = snapshot.toObjects(StudentEnrollment::class.java)
            Result.success(enrollments)
        } catch (e: Exception) {
            Log.e("StudentEnrollmentRepository", "Failed to get enrollments for student $studentId: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get real-time flow of enrollments for a specific student
     */
    fun getEnrollmentsByStudentFlow(studentId: String): Flow<List<StudentEnrollment>> = callbackFlow {
        val listenerRegistration = enrollmentsCollection
            .whereEqualTo("studentId", studentId)
            .whereEqualTo("status", EnrollmentStatus.ACTIVE.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("StudentEnrollmentRepository", "Real-time listener error: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val enrollments = snapshot.toObjects(StudentEnrollment::class.java)
                    trySend(enrollments)
                }
            }
        
        awaitClose {
            listenerRegistration.remove()
        }
    }

    /**
     * Get all students enrolled in a specific section
     */
    suspend fun getStudentsBySection(subjectId: String, sectionName: String): Result<List<StudentEnrollment>> {
        return try {
            val snapshot = enrollmentsCollection
                .whereEqualTo("subjectId", subjectId)
                .whereEqualTo("sectionName", sectionName)
                .whereEqualTo("status", EnrollmentStatus.ACTIVE.name)
                .get()
                .await()
            
            val enrollments = snapshot.toObjects(StudentEnrollment::class.java)
            Result.success(enrollments)
        } catch (e: Exception) {
            Log.e("StudentEnrollmentRepository", "Failed to get students for section $sectionName in subject $subjectId: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get all students enrolled in a subject (across all sections)
     */
    suspend fun getStudentsBySubject(subjectId: String): Result<List<StudentEnrollment>> {
        return try {
            val snapshot = enrollmentsCollection
                .whereEqualTo("subjectId", subjectId)
                .whereEqualTo("status", EnrollmentStatus.ACTIVE.name)
                .get()
                .await()
            val enrollments = snapshot.toObjects(StudentEnrollment::class.java)
            Result.success(enrollments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all ACTIVE enrollments for a teacher across all their sections
     */
    suspend fun getActiveEnrollmentsByTeacher(teacherId: String): Result<List<StudentEnrollment>> {
        return try {
            val snapshot = enrollmentsCollection
                .whereEqualTo("teacherId", teacherId)
                .whereEqualTo("status", EnrollmentStatus.ACTIVE.name)
                .get()
                .await()
            val enrollments = snapshot.toObjects(StudentEnrollment::class.java)
            Result.success(enrollments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Enroll a student in a section
     */
    suspend fun enrollStudent(enrollment: StudentEnrollment): Result<String> {
        return try {
            val docRef = enrollmentsCollection.add(enrollment).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("StudentEnrollmentRepository", "Enrollment creation failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Update enrollment status (for dropping, kicking, etc.)
     */
    suspend fun updateEnrollmentStatus(
        enrollmentId: String, 
        status: EnrollmentStatus, 
        updatedBy: String,
        updatedByName: String,
        notes: String = ""
    ): Result<Unit> {
        return try {
            val updates = mapOf(
                "status" to status.name,
                "updatedAt" to System.currentTimeMillis(),
                "updatedBy" to updatedBy,
                "updatedByName" to updatedByName,
                "notes" to notes
            )
            enrollmentsCollection.document(enrollmentId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if student is already enrolled in a section
     */
    suspend fun isStudentEnrolled(studentId: String, subjectId: String, sectionName: String): Result<Boolean> {
        return try {
            val snapshot = enrollmentsCollection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("subjectId", subjectId)
                .whereEqualTo("sectionName", sectionName)
                .whereEqualTo("status", EnrollmentStatus.ACTIVE.name)
                .get()
                .await()
            Result.success(!snapshot.isEmpty)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all enrollments for a student in a specific subject (regardless of status)
     * This is useful for checking if a student was previously enrolled but removed
     */
    suspend fun getStudentEnrollmentsBySubject(studentId: String, subjectId: String): Result<List<StudentEnrollment>> {
        return try {
            val snapshot = enrollmentsCollection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("subjectId", subjectId)
                .get()
                .await()
            val enrollments = snapshot.toObjects(StudentEnrollment::class.java)
            Result.success(enrollments)
        } catch (e: Exception) {
            Log.e("StudentEnrollmentRepository", "Failed to get enrollments for student $studentId in subject $subjectId: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get enrollment by ID
     */
    suspend fun getEnrollmentById(enrollmentId: String): Result<StudentEnrollment?> {
        return try {
            val document = enrollmentsCollection.document(enrollmentId).get().await()
            if (document.exists()) {
                val enrollment = document.toObject(StudentEnrollment::class.java)
                Result.success(enrollment)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Remove student from section (drop enrollment)
     */
    suspend fun dropStudent(enrollmentId: String, droppedBy: String, droppedByName: String, reason: String = ""): Result<Unit> {
        return updateEnrollmentStatus(
            enrollmentId = enrollmentId,
            status = EnrollmentStatus.DROPPED,
            updatedBy = droppedBy,
            updatedByName = droppedByName,
            notes = reason
        )
    }

    /**
     * Kick student from section
     */
    suspend fun kickStudent(enrollmentId: String, kickedBy: String, kickedByName: String, reason: String = ""): Result<Unit> {
        return updateEnrollmentStatus(
            enrollmentId = enrollmentId,
            status = EnrollmentStatus.KICKED,
            updatedBy = kickedBy,
            updatedByName = kickedByName,
            notes = reason
        )
    }

    /**
     * Get all active enrollments
     */
    suspend fun getAllActiveEnrollments(): Result<List<StudentEnrollment>> {
        return try {
            val snapshot = enrollmentsCollection
                .whereEqualTo("status", EnrollmentStatus.ACTIVE.name)
                .get()
                .await()
            val enrollments = snapshot.toObjects(StudentEnrollment::class.java)
            Result.success(enrollments)
        } catch (e: Exception) {
            Log.e("StudentEnrollmentRepository", "Failed to get all enrollments: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get real-time flow of students enrolled in a specific section
     */
    fun getStudentsBySectionFlow(subjectId: String, sectionName: String): Flow<List<StudentEnrollment>> = callbackFlow {
        val listener = enrollmentsCollection
            .whereEqualTo("subjectId", subjectId)
            .whereEqualTo("sectionName", sectionName)
            .whereEqualTo("status", EnrollmentStatus.ACTIVE.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("StudentEnrollmentRepository", "Real-time listener error: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val enrollments = snapshot.toObjects(StudentEnrollment::class.java)
                    trySend(enrollments)
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    /**
     * Get real-time flow of students enrolled in a subject (across all sections)
     */
    fun getStudentsBySubjectFlow(subjectId: String): Flow<List<StudentEnrollment>> = callbackFlow {
        val listener = enrollmentsCollection
            .whereEqualTo("subjectId", subjectId)
            .whereEqualTo("status", EnrollmentStatus.ACTIVE.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("StudentEnrollmentRepository", "Real-time listener error: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val enrollments = snapshot.toObjects(StudentEnrollment::class.java)
                    trySend(enrollments)
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    /**
     * Get real-time flow of active enrollments for a teacher
     */
    fun getActiveEnrollmentsByTeacherFlow(teacherId: String): Flow<List<StudentEnrollment>> = callbackFlow {
        val listener = enrollmentsCollection
            .whereEqualTo("teacherId", teacherId)
            .whereEqualTo("status", EnrollmentStatus.ACTIVE.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("StudentEnrollmentRepository", "Real-time listener error: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val enrollments = snapshot.toObjects(StudentEnrollment::class.java)
                    trySend(enrollments)
                }
            }

        awaitClose {
            listener.remove()
        }
    }
}
