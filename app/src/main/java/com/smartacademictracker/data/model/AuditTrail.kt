package com.smartacademictracker.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Audit trail entry for tracking all grade changes
 * Provides complete history of grade modifications for administrative oversight
 */
data class AuditTrail(
    @DocumentId
    val id: String = "",
    val gradeId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val action: AuditAction = AuditAction.CREATED,
    val oldValue: Double? = null,
    val newValue: Double? = null,
    val oldLetterGrade: String? = null,
    val newLetterGrade: String? = null,
    val gradePeriod: GradePeriod = GradePeriod.PRELIM,
    val timestamp: Long = System.currentTimeMillis(),
    val semester: String = "",
    val academicYear: String = "",
    val reason: String = "",
    val ipAddress: String = "",
    val userAgent: String = ""
)

/**
 * Types of audit actions that can be performed on grades
 */
enum class AuditAction(val displayName: String) {
    CREATED("Grade Created"),
    UPDATED("Grade Updated"),
    DELETED("Grade Deleted"),
    BULK_UPDATED("Bulk Grade Update"),
    CORRECTED("Grade Corrected"),
    APPROVED("Grade Approved"),
    REJECTED("Grade Rejected"),
    SYNCED("Grade Synced"),
    CONFLICT_RESOLVED("Conflict Resolved")
}

/**
 * Audit trail filter criteria for querying
 */
data class AuditTrailFilter(
    val studentId: String? = null,
    val subjectId: String? = null,
    val teacherId: String? = null,
    val action: AuditAction? = null,
    val gradePeriod: GradePeriod? = null,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val semester: String? = null,
    val academicYear: String? = null
)

/**
 * Audit trail summary for reporting
 */
data class AuditTrailSummary(
    val totalActions: Int = 0,
    val actionsByType: Map<AuditAction, Int> = emptyMap(),
    val actionsByTeacher: Map<String, Int> = emptyMap(),
    val actionsBySubject: Map<String, Int> = emptyMap(),
    val recentActivity: List<AuditTrail> = emptyList(),
    val dateRange: Pair<Long, Long>? = null
)
