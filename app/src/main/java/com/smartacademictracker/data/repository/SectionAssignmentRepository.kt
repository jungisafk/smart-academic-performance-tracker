package com.smartacademictracker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.smartacademictracker.data.model.SectionAssignment
import com.smartacademictracker.data.model.AssignmentStatus
import com.smartacademictracker.data.service.AcademicPeriodFilterService
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SectionAssignmentRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val academicPeriodFilterService: AcademicPeriodFilterService
) {
    private val sectionAssignmentsCollection = firestore.collection("section_assignments")

    suspend fun createSectionAssignment(assignment: SectionAssignment): Result<SectionAssignment> {
        return try {
            // Check if there's an active academic period
            if (!academicPeriodFilterService.hasActiveAcademicPeriod()) {
                return Result.failure(Exception("No active academic period found. Please create an academic period first."))
            }
            
            // Get the active academic period ID
            val activePeriodId = academicPeriodFilterService.getActiveAcademicPeriodId()
            
            // Create assignment with active academic period
            val assignmentWithPeriod = assignment.copy(academicPeriodId = activePeriodId)
            val docRef = sectionAssignmentsCollection.add(assignmentWithPeriod).await()
            val createdAssignment = assignmentWithPeriod.copy(id = docRef.id)
            sectionAssignmentsCollection.document(docRef.id).set(createdAssignment).await()
            
            println("DEBUG: SectionAssignmentRepository - Created section assignment: ${createdAssignment.sectionName} -> ${createdAssignment.teacherName}")
            Result.success(createdAssignment)
        } catch (e: Exception) {
            println("DEBUG: SectionAssignmentRepository - Error creating section assignment: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateSectionAssignment(assignment: SectionAssignment): Result<Unit> {
        return try {
            sectionAssignmentsCollection.document(assignment.id).set(assignment).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSectionAssignment(assignmentId: String): Result<Unit> {
        return try {
            sectionAssignmentsCollection.document(assignmentId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSectionAssignmentsBySubject(subjectId: String): Result<List<SectionAssignment>> {
        return try {
            val snapshot = sectionAssignmentsCollection
                .whereEqualTo("subjectId", subjectId)
                .whereEqualTo("status", AssignmentStatus.ACTIVE.name)
                .get()
                .await()
            val assignments = snapshot.toObjects(SectionAssignment::class.java)
            Result.success(assignments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSectionAssignmentsBySubjectAndCourse(subjectId: String, courseId: String): Result<List<SectionAssignment>> {
        return try {
            val snapshot = sectionAssignmentsCollection
                .whereEqualTo("subjectId", subjectId)
                .whereEqualTo("courseId", courseId)
                .whereEqualTo("status", AssignmentStatus.ACTIVE.name)
                .get()
                .await()
            val assignments = snapshot.toObjects(SectionAssignment::class.java)
            Result.success(assignments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSectionAssignmentsByTeacher(teacherId: String): Result<List<SectionAssignment>> {
        return try {
            val snapshot = sectionAssignmentsCollection
                .whereEqualTo("teacherId", teacherId)
                .whereEqualTo("status", AssignmentStatus.ACTIVE.name)
                .get()
                .await()
            val assignments = snapshot.toObjects(SectionAssignment::class.java)
            Result.success(assignments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllSectionAssignments(): Result<List<SectionAssignment>> {
        return try {
            val snapshot = sectionAssignmentsCollection
                .whereEqualTo("status", AssignmentStatus.ACTIVE.name)
                .get()
                .await()
            val assignments = snapshot.toObjects(SectionAssignment::class.java)
            Result.success(assignments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSectionAssignmentBySection(subjectId: String, sectionName: String): Result<SectionAssignment?> {
        return try {
            val snapshot = sectionAssignmentsCollection
                .whereEqualTo("subjectId", subjectId)
                .whereEqualTo("sectionName", sectionName)
                .whereEqualTo("status", AssignmentStatus.ACTIVE.name)
                .limit(1)
                .get()
                .await()
            
            val assignment = if (snapshot.isEmpty) {
                null
            } else {
                snapshot.documents.first().toObject(SectionAssignment::class.java)
            }
            Result.success(assignment)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isSectionAvailable(subjectId: String, sectionName: String): Result<Boolean> {
        return try {
            val assignmentResult = getSectionAssignmentBySection(subjectId, sectionName)
            assignmentResult.map { assignment ->
                // Section is available if no assignment exists
                assignment == null
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
