package com.smartacademictracker.data.notification

import com.smartacademictracker.data.model.Notification
import com.smartacademictracker.data.model.NotificationType
import com.smartacademictracker.data.model.NotificationPriority
import com.smartacademictracker.data.repository.NotificationRepository
import com.smartacademictracker.data.repository.UserRepository
import com.smartacademictracker.data.repository.EnrollmentRepository
import com.smartacademictracker.data.repository.AuditTrailRepository
import com.smartacademictracker.data.model.AuditAction
import com.smartacademictracker.data.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for handling notifications related to enrollment actions
 * (student leaving class, teacher kicking student, etc.)
 */
@Singleton
class EnrollmentNotificationService @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val auditTrailRepository: AuditTrailRepository
) {
    
    /**
     * Send notifications when a student leaves a class
     */
    suspend fun notifyStudentLeftClass(
        studentId: String,
        studentName: String,
        subjectId: String,
        subjectName: String,
        teacherId: String,
        teacherName: String,
        reason: String = "Student voluntarily left the class"
    ): Result<Unit> {
        return try {
            // Notify the student
            val studentNotification = Notification(
                userId = studentId,
                title = "You Left a Class",
                message = "You have left $subjectName. You may re-apply anytime.",
                type = NotificationType.STUDENT_LEFT_CLASS,
                priority = NotificationPriority.NORMAL,
                data = mapOf(
                    "subjectId" to subjectId,
                    "subjectName" to subjectName,
                    "teacherId" to teacherId,
                    "teacherName" to teacherName,
                    "reason" to reason
                )
            )
            
            // Notify the teacher
            val teacherNotification = Notification(
                userId = teacherId,
                title = "Student Left Your Class",
                message = "$studentName has left $subjectName.",
                type = NotificationType.STUDENT_LEFT_CLASS,
                priority = NotificationPriority.NORMAL,
                data = mapOf(
                    "studentId" to studentId,
                    "studentName" to studentName,
                    "subjectId" to subjectId,
                    "subjectName" to subjectName,
                    "reason" to reason
                )
            )
            
            // Create notifications
            notificationRepository.createNotification(studentNotification)
            notificationRepository.createNotification(teacherNotification)
            
            // Log the event for admin audit
            logEnrollmentEvent(
                action = "STUDENT_LEFT_CLASS",
                studentId = studentId,
                studentName = studentName,
                teacherId = teacherId,
                teacherName = teacherName,
                subjectId = subjectId,
                subjectName = subjectName,
                details = mapOf(
                    "reason" to reason,
                    "actionBy" to "STUDENT"
                )
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Send notifications when a teacher kicks a student from class
     */
    suspend fun notifyStudentKickedFromClass(
        studentId: String,
        studentName: String,
        subjectId: String,
        subjectName: String,
        teacherId: String,
        teacherName: String,
        reason: String = "Removed by teacher"
    ): Result<Unit> {
        return try {
            // Notify the student
            val studentNotification = Notification(
                userId = studentId,
                title = "Removed from Class",
                message = "You have been removed from $subjectName by $teacherName.",
                type = NotificationType.STUDENT_KICKED_FROM_CLASS,
                priority = NotificationPriority.HIGH,
                data = mapOf(
                    "subjectId" to subjectId,
                    "subjectName" to subjectName,
                    "teacherId" to teacherId,
                    "teacherName" to teacherName,
                    "reason" to reason
                )
            )
            
            // Notify the teacher
            val teacherNotification = Notification(
                userId = teacherId,
                title = "Student Removed",
                message = "You have removed $studentName from $subjectName.",
                type = NotificationType.TEACHER_KICKED_STUDENT,
                priority = NotificationPriority.NORMAL,
                data = mapOf(
                    "studentId" to studentId,
                    "studentName" to studentName,
                    "subjectId" to subjectId,
                    "subjectName" to subjectName,
                    "reason" to reason
                )
            )
            
            // Create notifications
            notificationRepository.createNotification(studentNotification)
            notificationRepository.createNotification(teacherNotification)
            
            // Log the event for admin audit
            logEnrollmentEvent(
                action = "STUDENT_KICKED_FROM_CLASS",
                studentId = studentId,
                studentName = studentName,
                teacherId = teacherId,
                teacherName = teacherName,
                subjectId = subjectId,
                subjectName = subjectName,
                details = mapOf(
                    "reason" to reason,
                    "actionBy" to "TEACHER"
                )
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Log enrollment events for admin audit trail
     */
    private suspend fun logEnrollmentEvent(
        action: String,
        studentId: String,
        studentName: String,
        teacherId: String,
        teacherName: String,
        subjectId: String,
        subjectName: String,
        details: Map<String, String>
    ) {
        try {
            // Get admin users to notify them
            val adminUsers = userRepository.getUsersByRole(UserRole.ADMIN)
            adminUsers.onSuccess { admins ->
                // Send notification to all admins
                admins.forEach { admin ->
                    val adminNotification = Notification(
                        userId = admin.id,
                        title = "Enrollment Action",
                        message = when (action) {
                            "STUDENT_LEFT_CLASS" -> "$studentName left $subjectName"
                            "STUDENT_KICKED_FROM_CLASS" -> "$teacherName removed $studentName from $subjectName"
                            else -> "Enrollment action occurred"
                        },
                        type = NotificationType.GENERAL,
                        priority = NotificationPriority.NORMAL,
                        data = mapOf(
                            "action" to action,
                            "studentId" to studentId,
                            "studentName" to studentName,
                            "teacherId" to teacherId,
                            "teacherName" to teacherName,
                            "subjectId" to subjectId,
                            "subjectName" to subjectName
                        ) + details
                    )
                    
                    CoroutineScope(Dispatchers.IO).launch {
                        notificationRepository.createNotification(adminNotification)
                    }
                }
            }
            
            // Create audit trail entry
            val auditTrail = com.smartacademictracker.data.model.AuditTrail(
                id = java.util.UUID.randomUUID().toString(),
                gradeId = "", // Not applicable for enrollment events
                studentId = studentId,
                studentName = studentName,
                subjectId = subjectId,
                subjectName = subjectName,
                teacherId = teacherId,
                teacherName = teacherName,
                action = when (action) {
                    "STUDENT_LEFT_CLASS" -> AuditAction.DELETED
                    "STUDENT_KICKED_FROM_CLASS" -> AuditAction.REJECTED
                    else -> AuditAction.UPDATED
                },
                reason = details["reason"] ?: "",
                timestamp = System.currentTimeMillis()
            )
            
            auditTrailRepository.createAuditEntry(
                grade = com.smartacademictracker.data.model.Grade(
                    id = "",
                    studentId = studentId,
                    studentName = studentName,
                    subjectId = subjectId,
                    subjectName = subjectName,
                    teacherId = teacherId
                ),
                action = auditTrail.action,
                reason = auditTrail.reason,
                teacherName = teacherName
            )
            
        } catch (e: Exception) {
            // Log error but don't fail the main operation
            println("Error logging enrollment event: ${e.message}")
        }
    }
}
