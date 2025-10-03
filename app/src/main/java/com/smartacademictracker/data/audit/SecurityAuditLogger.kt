package com.smartacademictracker.data.audit

import com.google.firebase.firestore.FirebaseFirestore
import com.smartacademictracker.data.model.AuditAction
import com.smartacademictracker.data.model.AuditTrail
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityAuditLogger @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val auditCollection = firestore.collection("audit_trails")
    
    suspend fun logSecurityEvent(
        eventType: SecurityEventType,
        userId: String,
        userRole: String,
        details: Map<String, Any> = emptyMap(),
        severity: SecuritySeverity = SecuritySeverity.INFO
    ): Result<Unit> {
        return try {
            val auditTrail = AuditTrail(
                id = UUID.randomUUID().toString(),
                gradeId = "", // Not applicable for security events
                studentId = userId,
                studentName = details["userName"] as? String ?: "Unknown",
                subjectId = "", // Not applicable for security events
                subjectName = eventType.displayName,
                teacherId = "", // Not applicable for security events
                teacherName = userRole,
                gradePeriod = com.smartacademictracker.data.model.GradePeriod.PRELIM, // Default value
                action = AuditAction.CREATED, // Default action
                oldValue = null,
                newValue = null,
                oldLetterGrade = null,
                newLetterGrade = null,
                timestamp = System.currentTimeMillis()
            )
            
            val securityEvent = SecurityEvent(
                id = auditTrail.id,
                eventType = eventType,
                userId = userId,
                userRole = userRole,
                timestamp = auditTrail.timestamp,
                severity = severity,
                details = details,
                ipAddress = details["ipAddress"] as? String,
                userAgent = details["userAgent"] as? String,
                sessionId = details["sessionId"] as? String
            )
            
            auditCollection.document(auditTrail.id).set(securityEvent).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun logAuthenticationEvent(
        eventType: AuthenticationEventType,
        userId: String,
        userRole: String,
        details: Map<String, Any> = emptyMap()
    ): Result<Unit> {
        return logSecurityEvent(
            eventType = SecurityEventType.AUTHENTICATION,
            userId = userId,
            userRole = userRole,
            details = details + mapOf(
                "authEventType" to eventType.name,
                "eventDescription" to eventType.description
            ),
            severity = when (eventType) {
                AuthenticationEventType.LOGIN_SUCCESS -> SecuritySeverity.INFO
                AuthenticationEventType.LOGIN_FAILED -> SecuritySeverity.WARNING
                AuthenticationEventType.LOGOUT -> SecuritySeverity.INFO
                AuthenticationEventType.PASSWORD_CHANGE -> SecuritySeverity.INFO
                AuthenticationEventType.ACCOUNT_LOCKED -> SecuritySeverity.CRITICAL
                AuthenticationEventType.SUSPICIOUS_ACTIVITY -> SecuritySeverity.CRITICAL
            }
        )
    }
    
    suspend fun logDataAccessEvent(
        eventType: DataAccessEventType,
        userId: String,
        userRole: String,
        resourceType: String,
        resourceId: String,
        details: Map<String, Any> = emptyMap()
    ): Result<Unit> {
        return logSecurityEvent(
            eventType = SecurityEventType.DATA_ACCESS,
            userId = userId,
            userRole = userRole,
            details = details + mapOf(
                "dataEventType" to eventType.name,
                "resourceType" to resourceType,
                "resourceId" to resourceId,
                "eventDescription" to eventType.description
            ),
            severity = when (eventType) {
                DataAccessEventType.READ -> SecuritySeverity.INFO
                DataAccessEventType.WRITE -> SecuritySeverity.INFO
                DataAccessEventType.DELETE -> SecuritySeverity.WARNING
                DataAccessEventType.UNAUTHORIZED_ACCESS -> SecuritySeverity.CRITICAL
                DataAccessEventType.BULK_OPERATION -> SecuritySeverity.INFO
            }
        )
    }
    
    suspend fun logGradeModificationEvent(
        eventType: GradeModificationEventType,
        userId: String,
        userRole: String,
        gradeId: String,
        studentId: String,
        subjectId: String,
        oldValue: Double? = null,
        newValue: Double? = null,
        details: Map<String, Any> = emptyMap()
    ): Result<Unit> {
        return logSecurityEvent(
            eventType = SecurityEventType.GRADE_MODIFICATION,
            userId = userId,
            userRole = userRole,
            details = details + mapOf(
                "gradeEventType" to eventType.name,
                "gradeId" to gradeId,
                "studentId" to studentId,
                "subjectId" to subjectId,
                "oldValue" to (oldValue ?: ""),
                "newValue" to (newValue ?: ""),
                "eventDescription" to eventType.description
            ),
            severity = when (eventType) {
                GradeModificationEventType.GRADE_CREATED -> SecuritySeverity.INFO
                GradeModificationEventType.GRADE_UPDATED -> SecuritySeverity.INFO
                GradeModificationEventType.GRADE_DELETED -> SecuritySeverity.WARNING
                GradeModificationEventType.BULK_GRADE_UPDATE -> SecuritySeverity.INFO
                GradeModificationEventType.GRADE_OVERWRITE -> SecuritySeverity.WARNING
            }
        )
    }
    
    suspend fun getSecurityEvents(
        userId: String? = null,
        eventType: SecurityEventType? = null,
        severity: SecuritySeverity? = null,
        limit: Int = 100
    ): Result<List<SecurityEvent>> {
        return try {
            var query = auditCollection.limit(limit.toLong())
            
            if (userId != null) {
                query = query.whereEqualTo("userId", userId)
            }
            if (eventType != null) {
                query = query.whereEqualTo("eventType", eventType.name)
            }
            if (severity != null) {
                query = query.whereEqualTo("severity", severity.name)
            }
            
            val snapshot = query.orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING).get().await()
            val events = snapshot.toObjects(SecurityEvent::class.java)
            Result.success(events)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class SecurityEvent(
    val id: String = "",
    val eventType: SecurityEventType = SecurityEventType.UNKNOWN,
    val userId: String = "",
    val userRole: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val severity: SecuritySeverity = SecuritySeverity.INFO,
    val details: Map<String, Any> = emptyMap(),
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val sessionId: String? = null
)

enum class SecurityEventType(val displayName: String) {
    AUTHENTICATION("Authentication Event"),
    DATA_ACCESS("Data Access Event"),
    GRADE_MODIFICATION("Grade Modification Event"),
    SYSTEM_EVENT("System Event"),
    SECURITY_VIOLATION("Security Violation"),
    UNKNOWN("Unknown Event")
}

enum class AuthenticationEventType(val description: String) {
    LOGIN_SUCCESS("User successfully logged in"),
    LOGIN_FAILED("Failed login attempt"),
    LOGOUT("User logged out"),
    PASSWORD_CHANGE("Password changed"),
    ACCOUNT_LOCKED("Account locked due to security"),
    SUSPICIOUS_ACTIVITY("Suspicious activity detected")
}

enum class DataAccessEventType(val description: String) {
    READ("Data read operation"),
    WRITE("Data write operation"),
    DELETE("Data delete operation"),
    UNAUTHORIZED_ACCESS("Unauthorized access attempt"),
    BULK_OPERATION("Bulk data operation")
}

enum class GradeModificationEventType(val description: String) {
    GRADE_CREATED("New grade created"),
    GRADE_UPDATED("Grade updated"),
    GRADE_DELETED("Grade deleted"),
    BULK_GRADE_UPDATE("Multiple grades updated"),
    GRADE_OVERWRITE("Grade overwritten")
}

enum class SecuritySeverity {
    INFO,
    WARNING,
    CRITICAL,
    EMERGENCY
}
