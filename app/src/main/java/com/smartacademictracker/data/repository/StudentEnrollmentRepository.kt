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
            Log.d("StudentEnrollmentRepository", "Fetching enrollments for student: $studentId")
            val snapshot = enrollmentsCollection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("status", EnrollmentStatus.ACTIVE.name)
                .get()
                .await()
            val enrollments = snapshot.toObjects(StudentEnrollment::class.java)
            Log.d("StudentEnrollmentRepository", "Found ${enrollments.size} active enrollments for student $studentId")
            enrollments.forEach { enrollment ->
                Log.d("StudentEnrollmentRepository", "Enrollment: ${enrollment.subjectName} - ${enrollment.sectionName} (Status: ${enrollment.status})")
            }
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
        Log.d("StudentEnrollmentRepository", "Setting up real-time listener for student: $studentId")
        
        val listenerRegistration = enrollmentsCollection
            .whereEqualTo("studentId", studentId)
            .whereEqualTo("status", EnrollmentStatus.ACTIVE.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("StudentEnrollmentRepository", "Error in enrollment listener: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val enrollments = snapshot.toObjects(StudentEnrollment::class.java)
                    Log.d("StudentEnrollmentRepository", "Real-time update: Found ${enrollments.size} active enrollments for student $studentId")
                    enrollments.forEach { enrollment ->
                        Log.d("StudentEnrollmentRepository", "Enrollment: ${enrollment.subjectName} (${enrollment.subjectCode}) - Section: ${enrollment.sectionName}, Status: ${enrollment.status}")
                    }
                    trySend(enrollments)
                }
            }
        
        awaitClose {
            Log.d("StudentEnrollmentRepository", "Removing real-time listener for student: $studentId")
            listenerRegistration.remove()
        }
    }

    /**
     * Get all students enrolled in a specific section
     */
    suspend fun getStudentsBySection(subjectId: String, sectionName: String): Result<List<StudentEnrollment>> {
        return try {
            Log.d("StudentEnrollmentRepository", "Fetching students for subject: $subjectId, section: $sectionName")
            Log.d("StudentEnrollmentRepository", "Query: subjectId=$subjectId, sectionName=$sectionName, status=${EnrollmentStatus.ACTIVE.name}")
            
            val snapshot = enrollmentsCollection
                .whereEqualTo("subjectId", subjectId)
                .whereEqualTo("sectionName", sectionName)
                .whereEqualTo("status", EnrollmentStatus.ACTIVE.name)
                .get()
                .await()
                
            Log.d("StudentEnrollmentRepository", "Query completed. Document count: ${snapshot.size()}")
            
            val enrollments = snapshot.toObjects(StudentEnrollment::class.java)
            Log.d("StudentEnrollmentRepository", "Found ${enrollments.size} active students in section $sectionName for subject $subjectId")
            
            if (enrollments.isEmpty()) {
                Log.w("StudentEnrollmentRepository", "No students found. Checking if there are any enrollments for this subject...")
                // Let's also check if there are any enrollments for this subject at all
                val allEnrollmentsSnapshot = enrollmentsCollection
                    .whereEqualTo("subjectId", subjectId)
                    .get()
                    .await()
                Log.d("StudentEnrollmentRepository", "Total enrollments for subject $subjectId: ${allEnrollmentsSnapshot.size()}")
                allEnrollmentsSnapshot.documents.forEach { doc ->
                    val data = doc.data
                    Log.d("StudentEnrollmentRepository", "Enrollment doc: sectionName=${data?.get("sectionName")}, status=${data?.get("status")}")
                }
            }
            
            enrollments.forEach { enrollment ->
                Log.d("StudentEnrollmentRepository", "Student: ${enrollment.studentName} (${enrollment.studentId}) - Status: ${enrollment.status}")
            }
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
            Log.d("StudentEnrollmentRepository", "Creating enrollment for student: ${enrollment.studentName} in subject: ${enrollment.subjectName} - ${enrollment.sectionName}")
            Log.d("StudentEnrollmentRepository", "Enrollment details: studentId=${enrollment.studentId}, subjectId=${enrollment.subjectId}, sectionName=${enrollment.sectionName}, status=${enrollment.status}")
            
            val docRef = enrollmentsCollection.add(enrollment).await()
            Log.d("StudentEnrollmentRepository", "Enrollment created successfully with ID: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("StudentEnrollmentRepository", "Failed to create enrollment: ${e.message}", e)
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
}
