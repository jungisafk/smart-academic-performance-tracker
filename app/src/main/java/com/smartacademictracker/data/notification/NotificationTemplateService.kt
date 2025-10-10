package com.smartacademictracker.data.notification

import com.google.firebase.Timestamp
import com.smartacademictracker.data.model.Notification
import com.smartacademictracker.data.model.NotificationTemplate
import com.smartacademictracker.data.model.NotificationType
import com.smartacademictracker.data.model.NotificationPriority
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationTemplateService @Inject constructor() {
    
    private val templates = mapOf(
        NotificationType.GRADE_UPDATE to NotificationTemplate(
            name = "Grade Update Template",
            type = NotificationType.GRADE_UPDATE,
            titleTemplate = "Grade Update - {subjectName}",
            messageTemplate = "You received a {gradePeriod} grade of {score}/{maxScore} ({percentage}%) in {subjectName}",
            variables = listOf("subjectName", "gradePeriod", "score", "maxScore", "percentage")
        ),
        NotificationType.APPLICATION_APPROVED to NotificationTemplate(
            name = "Application Approved Template",
            type = NotificationType.APPLICATION_APPROVED,
            titleTemplate = "Application Approved",
            messageTemplate = "Your {applicationType} application for {subjectName} has been approved",
            variables = listOf("applicationType", "subjectName")
        ),
        NotificationType.APPLICATION_REJECTED to NotificationTemplate(
            name = "Application Rejected Template",
            type = NotificationType.APPLICATION_REJECTED,
            titleTemplate = "Application Rejected",
            messageTemplate = "Your {applicationType} application for {subjectName} has been rejected. Reason: {reason}",
            variables = listOf("applicationType", "subjectName", "reason")
        ),
        NotificationType.DEADLINE_REMINDER to NotificationTemplate(
            name = "Deadline Reminder Template",
            type = NotificationType.DEADLINE_REMINDER,
            titleTemplate = "Deadline Reminder",
            messageTemplate = "{deadlineType} deadline for {subjectName} is on {deadlineDate}",
            variables = listOf("deadlineType", "subjectName", "deadlineDate")
        ),
        NotificationType.SYSTEM_ANNOUNCEMENT to NotificationTemplate(
            name = "System Announcement Template",
            type = NotificationType.SYSTEM_ANNOUNCEMENT,
            titleTemplate = "System Announcement",
            messageTemplate = "{message}",
            variables = listOf("message")
        ),
        NotificationType.PERFORMANCE_ALERT to NotificationTemplate(
            name = "Performance Alert Template",
            type = NotificationType.PERFORMANCE_ALERT,
            titleTemplate = "Performance Alert",
            messageTemplate = "Your performance in {subjectName} is {performance}. Consider seeking help.",
            variables = listOf("subjectName", "performance")
        ),
        NotificationType.GRADE_SUBMISSION_DEADLINE to NotificationTemplate(
            name = "Grade Submission Deadline Template",
            type = NotificationType.GRADE_SUBMISSION_DEADLINE,
            titleTemplate = "Grade Submission Deadline",
            messageTemplate = "Grade submission deadline for {subjectName} is on {deadlineDate}",
            variables = listOf("subjectName", "deadlineDate")
        ),
        NotificationType.ACADEMIC_PERIOD_ACTIVATED to NotificationTemplate(
            name = "Academic Period Activated Template",
            type = NotificationType.ACADEMIC_PERIOD_ACTIVATED,
            titleTemplate = "New Academic Period",
            messageTemplate = "Academic period {periodName} has been activated",
            variables = listOf("periodName")
        ),
        NotificationType.TEACHER_APPLICATION_APPROVED to NotificationTemplate(
            name = "Teacher Application Approved Template",
            type = NotificationType.TEACHER_APPLICATION_APPROVED,
            titleTemplate = "Application Approved",
            messageTemplate = "Your {applicationType} application for {subjectName} has been approved",
            variables = listOf("applicationType", "subjectName")
        ),
        NotificationType.TEACHER_APPLICATION_REJECTED to NotificationTemplate(
            name = "Teacher Application Rejected Template",
            type = NotificationType.TEACHER_APPLICATION_REJECTED,
            titleTemplate = "Application Rejected",
            messageTemplate = "Your {applicationType} application for {subjectName} has been rejected. Reason: {reason}",
            variables = listOf("applicationType", "subjectName", "reason")
        ),
        NotificationType.SUBJECT_APPLICATION_APPROVED to NotificationTemplate(
            name = "Subject Application Approved Template",
            type = NotificationType.SUBJECT_APPLICATION_APPROVED,
            titleTemplate = "Application Approved",
            messageTemplate = "Your {applicationType} application for {subjectName} has been approved",
            variables = listOf("applicationType", "subjectName")
        ),
        NotificationType.SUBJECT_APPLICATION_REJECTED to NotificationTemplate(
            name = "Subject Application Rejected Template",
            type = NotificationType.SUBJECT_APPLICATION_REJECTED,
            titleTemplate = "Application Rejected",
            messageTemplate = "Your {applicationType} application for {subjectName} has been rejected. Reason: {reason}",
            variables = listOf("applicationType", "subjectName", "reason")
        )
    )
    
    fun getTemplate(type: NotificationType): NotificationTemplate? {
        return templates[type]
    }
    
    fun getAllTemplates(): List<NotificationTemplate> {
        return templates.values.toList()
    }
    
    fun createNotificationFromTemplate(
        type: NotificationType,
        userId: String,
        variables: Map<String, String>,
        priority: NotificationPriority = NotificationPriority.NORMAL
    ): Notification {
        val template = getTemplate(type) ?: return createDefaultNotification(type, userId, variables)
        
        val title = replaceVariables(template.titleTemplate, variables)
        val message = replaceVariables(template.messageTemplate, variables)
        
        return Notification(
            userId = userId,
            title = title,
            message = message,
            type = type,
            priority = priority,
            createdAt = Timestamp.now()
        )
    }
    
    private fun replaceVariables(template: String, variables: Map<String, String>): String {
        var result = template
        variables.forEach { (key, value) ->
            result = result.replace("{$key}", value)
        }
        return result
    }
    
    private fun createDefaultNotification(
        type: NotificationType,
        userId: String,
        variables: Map<String, String>
    ): Notification {
        val title = when (type) {
            NotificationType.GRADE_UPDATE -> "Grade Update"
            NotificationType.APPLICATION_APPROVED -> "Application Approved"
            NotificationType.APPLICATION_REJECTED -> "Application Rejected"
            NotificationType.DEADLINE_REMINDER -> "Deadline Reminder"
            NotificationType.SYSTEM_ANNOUNCEMENT -> "System Announcement"
            NotificationType.PERFORMANCE_ALERT -> "Performance Alert"
            NotificationType.GRADE_SUBMISSION_DEADLINE -> "Grade Submission Deadline"
            NotificationType.ACADEMIC_PERIOD_ACTIVATED -> "Academic Period Activated"
            NotificationType.TEACHER_APPLICATION_APPROVED -> "Teacher Application Approved"
            NotificationType.TEACHER_APPLICATION_REJECTED -> "Teacher Application Rejected"
            NotificationType.SUBJECT_APPLICATION_APPROVED -> "Subject Application Approved"
            NotificationType.SUBJECT_APPLICATION_REJECTED -> "Subject Application Rejected"
            else -> "Notification"
        }
        
        val message = when (type) {
            NotificationType.TEACHER_APPLICATION_APPROVED -> {
                val appType = variables["applicationType"] ?: "Teacher Application"
                val subjectName = variables["subjectName"] ?: "subject"
                "Your $appType application for $subjectName has been approved"
            }
            NotificationType.TEACHER_APPLICATION_REJECTED -> {
                val appType = variables["applicationType"] ?: "Teacher Application"
                val subjectName = variables["subjectName"] ?: "subject"
                val reason = variables["reason"] ?: "No reason provided"
                "Your $appType application for $subjectName has been rejected. Reason: $reason"
            }
            NotificationType.SUBJECT_APPLICATION_APPROVED -> {
                val appType = variables["applicationType"] ?: "Subject Application"
                val subjectName = variables["subjectName"] ?: "subject"
                "Your $appType application for $subjectName has been approved"
            }
            NotificationType.SUBJECT_APPLICATION_REJECTED -> {
                val appType = variables["applicationType"] ?: "Subject Application"
                val subjectName = variables["subjectName"] ?: "subject"
                val reason = variables["reason"] ?: "No reason provided"
                "Your $appType application for $subjectName has been rejected. Reason: $reason"
            }
            else -> variables["message"] ?: "You have a new notification"
        }
        
        return Notification(
            userId = userId,
            title = title,
            message = message,
            type = type,
            priority = NotificationPriority.NORMAL,
            createdAt = Timestamp.now()
        )
    }
    
    fun validateTemplate(template: NotificationTemplate): Boolean {
        return template.titleTemplate.isNotBlank() && 
               template.messageTemplate.isNotBlank() &&
               template.name.isNotBlank()
    }
    
    fun getRequiredVariables(type: NotificationType): List<String> {
        return getTemplate(type)?.variables ?: emptyList()
    }
}
