package com.smartacademictracker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.smartacademictracker.data.model.AuditAction
import com.smartacademictracker.data.model.AuditTrail
import com.smartacademictracker.data.model.AuditTrailFilter
import com.smartacademictracker.data.model.AuditTrailSummary
import com.smartacademictracker.data.model.Grade
import com.smartacademictracker.data.model.GradePeriod
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuditTrailRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val auditTrailCollection = firestore.collection("audit_trail")
    
    /**
     * Create an audit trail entry for a grade action
     */
    suspend fun createAuditEntry(
        grade: Grade,
        action: AuditAction,
        oldValue: Double? = null,
        newValue: Double? = null,
        oldLetterGrade: String? = null,
        newLetterGrade: String? = null,
        reason: String = "",
        teacherName: String = ""
    ): Result<AuditTrail> {
        return try {
            val auditEntry = AuditTrail(
                gradeId = grade.id,
                studentId = grade.studentId,
                studentName = grade.studentName,
                subjectId = grade.subjectId,
                subjectName = grade.subjectName,
                teacherId = grade.teacherId,
                teacherName = teacherName,
                action = action,
                oldValue = oldValue,
                newValue = newValue,
                oldLetterGrade = oldLetterGrade,
                newLetterGrade = newLetterGrade,
                gradePeriod = grade.gradePeriod,
                semester = grade.semester,
                academicYear = grade.academicYear,
                reason = reason
            )
            
            val docRef = auditTrailCollection.add(auditEntry).await()
            val createdEntry = auditEntry.copy(id = docRef.id)
            auditTrailCollection.document(docRef.id).set(createdEntry).await()
            Result.success(createdEntry)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get audit trail entries with filtering
     */
    suspend fun getAuditTrailEntries(filter: AuditTrailFilter): Result<List<AuditTrail>> {
        return try {
            var query: Query = auditTrailCollection
            
            // Apply filters
            filter.studentId?.let { query = query.whereEqualTo("studentId", it) }
            filter.subjectId?.let { query = query.whereEqualTo("subjectId", it) }
            filter.teacherId?.let { query = query.whereEqualTo("teacherId", it) }
            filter.action?.let { query = query.whereEqualTo("action", it.name) }
            filter.gradePeriod?.let { query = query.whereEqualTo("gradePeriod", it.name) }
            filter.semester?.let { query = query.whereEqualTo("semester", it) }
            filter.academicYear?.let { query = query.whereEqualTo("academicYear", it) }
            
            // Date range filter
            if (filter.startDate != null && filter.endDate != null) {
                query = query.whereGreaterThanOrEqualTo("timestamp", filter.startDate)
                    .whereLessThanOrEqualTo("timestamp", filter.endDate)
            }
            
            // Order by timestamp descending
            query = query.orderBy("timestamp", Query.Direction.DESCENDING)
            
            val snapshot = query.get().await()
            val entries = snapshot.toObjects(AuditTrail::class.java)
            Result.success(entries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get audit trail for a specific grade
     */
    suspend fun getAuditTrailForGrade(gradeId: String): Result<List<AuditTrail>> {
        return try {
            val snapshot = auditTrailCollection
                .whereEqualTo("gradeId", gradeId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            val entries = snapshot.toObjects(AuditTrail::class.java)
            Result.success(entries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get audit trail for a specific student
     */
    suspend fun getAuditTrailForStudent(studentId: String): Result<List<AuditTrail>> {
        return try {
            val snapshot = auditTrailCollection
                .whereEqualTo("studentId", studentId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            val entries = snapshot.toObjects(AuditTrail::class.java)
            Result.success(entries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get audit trail for a specific teacher
     */
    suspend fun getAuditTrailForTeacher(teacherId: String): Result<List<AuditTrail>> {
        return try {
            val snapshot = auditTrailCollection
                .whereEqualTo("teacherId", teacherId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            val entries = snapshot.toObjects(AuditTrail::class.java)
            Result.success(entries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get audit trail summary for reporting
     */
    suspend fun getAuditTrailSummary(
        startDate: Long? = null,
        endDate: Long? = null,
        teacherId: String? = null,
        subjectId: String? = null
    ): Result<AuditTrailSummary> {
        return try {
            var query: Query = auditTrailCollection
            
            // Apply date filters
            if (startDate != null && endDate != null) {
                query = query.whereGreaterThanOrEqualTo("timestamp", startDate)
                    .whereLessThanOrEqualTo("timestamp", endDate)
            }
            
            // Apply teacher filter
            teacherId?.let { query = query.whereEqualTo("teacherId", it) }
            
            // Apply subject filter
            subjectId?.let { query = query.whereEqualTo("subjectId", it) }
            
            val snapshot = query.get().await()
            val entries = snapshot.toObjects(AuditTrail::class.java)
            
            val actionsByType = entries.groupBy { it.action }
                .mapValues { it.value.size }
            
            val actionsByTeacher = entries.groupBy { it.teacherId }
                .mapValues { it.value.size }
            
            val actionsBySubject = entries.groupBy { it.subjectId }
                .mapValues { it.value.size }
            
            val recentActivity = entries.take(10)
            
            val dateRange = if (entries.isNotEmpty()) {
                val timestamps = entries.map { it.timestamp }
                Pair(timestamps.minOrNull() ?: 0L, timestamps.maxOrNull() ?: 0L)
            } else null
            
            val summary = AuditTrailSummary(
                totalActions = entries.size,
                actionsByType = actionsByType,
                actionsByTeacher = actionsByTeacher,
                actionsBySubject = actionsBySubject,
                recentActivity = recentActivity,
                dateRange = dateRange
            )
            
            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get grade change frequency for a student
     */
    suspend fun getGradeChangeFrequency(studentId: String): Result<Map<String, Int>> {
        return try {
            val snapshot = auditTrailCollection
                .whereEqualTo("studentId", studentId)
                .get()
                .await()
            val entries = snapshot.toObjects(AuditTrail::class.java)
            
            val frequency = entries.groupBy { it.subjectId }
                .mapValues { it.value.size }
            
            Result.success(frequency)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get teacher activity summary
     */
    suspend fun getTeacherActivitySummary(teacherId: String): Result<Map<AuditAction, Int>> {
        return try {
            val snapshot = auditTrailCollection
                .whereEqualTo("teacherId", teacherId)
                .get()
                .await()
            val entries = snapshot.toObjects(AuditTrail::class.java)
            
            val activity = entries.groupBy { it.action }
                .mapValues { it.value.size }
            
            Result.success(activity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete old audit trail entries (for data cleanup)
     */
    suspend fun deleteOldAuditEntries(olderThanDays: Int): Result<Int> {
        return try {
            val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
            val snapshot = auditTrailCollection
                .whereLessThan("timestamp", cutoffTime)
                .get()
                .await()
            
            val batch = firestore.batch()
            var deletedCount = 0
            
            for (document in snapshot.documents) {
                batch.delete(document.reference)
                deletedCount++
            }
            
            if (deletedCount > 0) {
                batch.commit().await()
            }
            
            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
