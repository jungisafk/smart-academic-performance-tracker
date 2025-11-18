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
            messageTemplate = "{message}",
            variables = listOf("subjectName", "gradePeriod", "score", "maxScore", "percentage", "gradePeriods", "message")
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
        ),
        NotificationType.STUDENT_ENROLLED to NotificationTemplate(
            name = "Student Enrolled Template",
            type = NotificationType.STUDENT_ENROLLED,
            titleTemplate = "Enrolled in {subjectName}",
            messageTemplate = "You have been enrolled in {subjectName}, Section {sectionName} by {teacherName}",
            variables = listOf("subjectName", "sectionName", "teacherName")
        ),
        NotificationType.STUDENT_DROPPED to NotificationTemplate(
            name = "Student Dropped Template",
            type = NotificationType.STUDENT_DROPPED,
            titleTemplate = "Dropped from {subjectName}",
            messageTemplate = "You have been dropped from {subjectName}, Section {sectionName} by {droppedBy}.{reason}",
            variables = listOf("subjectName", "sectionName", "droppedBy", "reason")
        ),
        NotificationType.TEACHER_ASSIGNED_TO_SECTION to NotificationTemplate(
            name = "Teacher Assigned to Section Template",
            type = NotificationType.TEACHER_ASSIGNED_TO_SECTION,
            titleTemplate = "Assigned to {subjectName}",
            messageTemplate = "You have been assigned to teach {subjectName}, Section {sectionName} by {assignedBy}",
            variables = listOf("subjectName", "sectionName", "assignedBy")
        ),
        NotificationType.TEACHER_REMOVED_FROM_SECTION to NotificationTemplate(
            name = "Teacher Removed from Section Template",
            type = NotificationType.TEACHER_REMOVED_FROM_SECTION,
            titleTemplate = "Removed from {subjectName}",
            messageTemplate = "You have been removed from teaching {subjectName}, Section {sectionName} by {removedBy}",
            variables = listOf("subjectName", "sectionName", "removedBy")
        ),
        NotificationType.GRADE_EDIT_REQUEST_APPROVED to NotificationTemplate(
            name = "Grade Edit Request Approved Template",
            type = NotificationType.GRADE_EDIT_REQUEST_APPROVED,
            titleTemplate = "Grade Edit Request Approved",
            messageTemplate = "Your grade edit request for {studentName} in {subjectName} ({gradePeriod}) has been approved",
            variables = listOf("studentName", "subjectName", "gradePeriod")
        ),
        NotificationType.GRADE_EDIT_REQUEST_REJECTED to NotificationTemplate(
            name = "Grade Edit Request Rejected Template",
            type = NotificationType.GRADE_EDIT_REQUEST_REJECTED,
            titleTemplate = "Grade Edit Request Rejected",
            messageTemplate = "Your grade edit request for {studentName} in {subjectName} ({gradePeriod}) has been rejected.{reason}",
            variables = listOf("studentName", "subjectName", "gradePeriod", "reason")
        ),
        NotificationType.USER_STATUS_CHANGED to NotificationTemplate(
            name = "User Status Changed Template",
            type = NotificationType.USER_STATUS_CHANGED,
            titleTemplate = "Account Status Changed",
            messageTemplate = "Your account status has been changed to {status} by {changedBy}",
            variables = listOf("status", "changedBy")
        ),
        NotificationType.USER_ROLE_CHANGED to NotificationTemplate(
            name = "User Role Changed Template",
            type = NotificationType.USER_ROLE_CHANGED,
            titleTemplate = "Account Role Changed",
            messageTemplate = "Your account role has been changed to {newRole} by {changedBy}",
            variables = listOf("newRole", "changedBy")
        ),
        NotificationType.SUBJECT_CREATED to NotificationTemplate(
            name = "Subject Created Template",
            type = NotificationType.SUBJECT_CREATED,
            titleTemplate = "New Subject Created",
            messageTemplate = "A new subject {subjectName} ({subjectCode}) has been created",
            variables = listOf("subjectName", "subjectCode")
        ),
        NotificationType.SUBJECT_UPDATED to NotificationTemplate(
            name = "Subject Updated Template",
            type = NotificationType.SUBJECT_UPDATED,
            titleTemplate = "Subject Updated",
            messageTemplate = "Subject {subjectName} ({subjectCode}) has been updated",
            variables = listOf("subjectName", "subjectCode")
        ),
        NotificationType.SUBJECT_DELETED to NotificationTemplate(
            name = "Subject Deleted Template",
            type = NotificationType.SUBJECT_DELETED,
            titleTemplate = "Subject Deleted",
            messageTemplate = "Subject {subjectName} ({subjectCode}) has been deleted",
            variables = listOf("subjectName", "subjectCode")
        ),
        NotificationType.COURSE_CREATED to NotificationTemplate(
            name = "Course Created Template",
            type = NotificationType.COURSE_CREATED,
            titleTemplate = "New Course Created",
            messageTemplate = "A new course {courseName} ({courseCode}) has been created",
            variables = listOf("courseName", "courseCode")
        ),
        NotificationType.COURSE_UPDATED to NotificationTemplate(
            name = "Course Updated Template",
            type = NotificationType.COURSE_UPDATED,
            titleTemplate = "Course Updated",
            messageTemplate = "Course {courseName} ({courseCode}) has been updated",
            variables = listOf("courseName", "courseCode")
        ),
        NotificationType.COURSE_DELETED to NotificationTemplate(
            name = "Course Deleted Template",
            type = NotificationType.COURSE_DELETED,
            titleTemplate = "Course Deleted",
            messageTemplate = "Course {courseName} ({courseCode}) has been deleted",
            variables = listOf("courseName", "courseCode")
        ),
        NotificationType.YEAR_LEVEL_CREATED to NotificationTemplate(
            name = "Year Level Created Template",
            type = NotificationType.YEAR_LEVEL_CREATED,
            titleTemplate = "New Year Level Created",
            messageTemplate = "A new year level {yearLevelName} has been created",
            variables = listOf("yearLevelName")
        ),
        NotificationType.YEAR_LEVEL_UPDATED to NotificationTemplate(
            name = "Year Level Updated Template",
            type = NotificationType.YEAR_LEVEL_UPDATED,
            titleTemplate = "Year Level Updated",
            messageTemplate = "Year level {yearLevelName} has been updated",
            variables = listOf("yearLevelName")
        ),
        NotificationType.YEAR_LEVEL_DELETED to NotificationTemplate(
            name = "Year Level Deleted Template",
            type = NotificationType.YEAR_LEVEL_DELETED,
            titleTemplate = "Year Level Deleted",
            messageTemplate = "Year level {yearLevelName} has been deleted",
            variables = listOf("yearLevelName")
        ),
        NotificationType.STUDENT_LEFT_CLASS to NotificationTemplate(
            name = "Student Left Class Template",
            type = NotificationType.STUDENT_LEFT_CLASS,
            titleTemplate = "Student Left Class",
            messageTemplate = "A student has left your {subjectName} class",
            variables = listOf("subjectName")
        ),
        NotificationType.STUDENT_KICKED_FROM_CLASS to NotificationTemplate(
            name = "Student Kicked from Class Template",
            type = NotificationType.STUDENT_KICKED_FROM_CLASS,
            titleTemplate = "Student Removed",
            messageTemplate = "A student has been removed from your {subjectName} class",
            variables = listOf("subjectName")
        ),
        NotificationType.TEACHER_KICKED_STUDENT to NotificationTemplate(
            name = "Teacher Kicked Student Template",
            type = NotificationType.TEACHER_KICKED_STUDENT,
            titleTemplate = "Removed from Class",
            messageTemplate = "You have been removed from {subjectName} by your teacher",
            variables = listOf("subjectName")
        ),
        NotificationType.ALL_GRADES_SUBMITTED to NotificationTemplate(
            name = "All Grades Submitted Template",
            type = NotificationType.ALL_GRADES_SUBMITTED,
            titleTemplate = "All Grades Submitted",
            messageTemplate = "All grades for {subjectName} have been submitted",
            variables = listOf("subjectName")
        ),
        NotificationType.GRADE_COMPLETION_NOTIFICATION to NotificationTemplate(
            name = "Grade Completion Notification Template",
            type = NotificationType.GRADE_COMPLETION_NOTIFICATION,
            titleTemplate = "Grade Completion",
            messageTemplate = "All grades for {subjectName} have been completed",
            variables = listOf("subjectName")
        ),
        NotificationType.TEACHER_APPLICATION_SUBMITTED to NotificationTemplate(
            name = "Teacher Application Submitted Template",
            type = NotificationType.TEACHER_APPLICATION_SUBMITTED,
            titleTemplate = "New Teacher Application",
            messageTemplate = "A new teacher application has been submitted",
            variables = listOf()
        ),
        NotificationType.STUDENT_APPLICATION_SUBMITTED to NotificationTemplate(
            name = "Student Application Submitted Template",
            type = NotificationType.STUDENT_APPLICATION_SUBMITTED,
            titleTemplate = "New Student Application",
            messageTemplate = "A new student application for {subjectName} has been submitted",
            variables = listOf("subjectName")
        ),
        NotificationType.GRADE_REMINDER to NotificationTemplate(
            name = "Grade Reminder Template",
            type = NotificationType.GRADE_REMINDER,
            titleTemplate = "Grade Reminder",
            messageTemplate = "Reminder: Check your grades for {subjectName}",
            variables = listOf("subjectName")
        ),
        NotificationType.GENERAL to NotificationTemplate(
            name = "General Notification Template",
            type = NotificationType.GENERAL,
            titleTemplate = "Notification",
            messageTemplate = "{message}",
            variables = listOf("message")
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
        
        // Debug logging
        android.util.Log.d("NotificationTemplateService", "Creating notification - Type: $type")
        android.util.Log.d("NotificationTemplateService", "Template message: ${template.messageTemplate}")
        android.util.Log.d("NotificationTemplateService", "Variables: $variables")
        android.util.Log.d("NotificationTemplateService", "Final message: $message")
        
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
        // Replace variables in order, ensuring all occurrences are replaced
        variables.forEach { (key, value) ->
            val placeholder = "{$key}"
            // Replace all occurrences of the placeholder
            while (result.contains(placeholder)) {
                result = result.replace(placeholder, value)
            }
        }
        // If there are still unreplaced variables, log a warning
        if (result.contains("{") && result.contains("}")) {
            android.util.Log.w("NotificationTemplateService", "Unreplaced variables found in template: $result")
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
            NotificationType.STUDENT_ENROLLED -> {
                val subjectName = variables["subjectName"] ?: "subject"
                val sectionName = variables["sectionName"] ?: "section"
                val teacherName = variables["teacherName"] ?: "teacher"
                "You have been enrolled in $subjectName, Section $sectionName by $teacherName"
            }
            NotificationType.STUDENT_DROPPED -> {
                val subjectName = variables["subjectName"] ?: "subject"
                val sectionName = variables["sectionName"] ?: "section"
                val droppedBy = variables["droppedBy"] ?: "admin"
                val reason = variables["reason"]?.takeIf { it.isNotBlank() }?.let { " Reason: $it" } ?: ""
                "You have been dropped from $subjectName, Section $sectionName by $droppedBy.$reason"
            }
            NotificationType.TEACHER_ASSIGNED_TO_SECTION -> {
                val subjectName = variables["subjectName"] ?: "subject"
                val sectionName = variables["sectionName"] ?: "section"
                val assignedBy = variables["assignedBy"] ?: "admin"
                "You have been assigned to teach $subjectName, Section $sectionName by $assignedBy"
            }
            NotificationType.TEACHER_REMOVED_FROM_SECTION -> {
                val subjectName = variables["subjectName"] ?: "subject"
                val sectionName = variables["sectionName"] ?: "section"
                val removedBy = variables["removedBy"] ?: "admin"
                "You have been removed from teaching $subjectName, Section $sectionName by $removedBy"
            }
            NotificationType.GRADE_EDIT_REQUEST_APPROVED -> {
                val studentName = variables["studentName"] ?: "student"
                val subjectName = variables["subjectName"] ?: "subject"
                val gradePeriod = variables["gradePeriod"] ?: "grade period"
                "Your grade edit request for $studentName in $subjectName ($gradePeriod) has been approved"
            }
            NotificationType.GRADE_EDIT_REQUEST_REJECTED -> {
                val studentName = variables["studentName"] ?: "student"
                val subjectName = variables["subjectName"] ?: "subject"
                val gradePeriod = variables["gradePeriod"] ?: "grade period"
                val reason = variables["reason"]?.takeIf { it.isNotBlank() }?.let { " Reason: $it" } ?: ""
                "Your grade edit request for $studentName in $subjectName ($gradePeriod) has been rejected.$reason"
            }
            NotificationType.USER_STATUS_CHANGED -> {
                val status = variables["status"] ?: "unknown"
                val changedBy = variables["changedBy"] ?: "admin"
                "Your account status has been changed to $status by $changedBy"
            }
            NotificationType.USER_ROLE_CHANGED -> {
                val newRole = variables["newRole"] ?: "unknown"
                val changedBy = variables["changedBy"] ?: "admin"
                "Your account role has been changed to $newRole by $changedBy"
            }
            NotificationType.SUBJECT_CREATED -> {
                val subjectName = variables["subjectName"] ?: "subject"
                val subjectCode = variables["subjectCode"] ?: "code"
                "A new subject $subjectName ($subjectCode) has been created"
            }
            NotificationType.SUBJECT_UPDATED -> {
                val subjectName = variables["subjectName"] ?: "subject"
                val subjectCode = variables["subjectCode"] ?: "code"
                "Subject $subjectName ($subjectCode) has been updated"
            }
            NotificationType.SUBJECT_DELETED -> {
                val subjectName = variables["subjectName"] ?: "subject"
                val subjectCode = variables["subjectCode"] ?: "code"
                "Subject $subjectName ($subjectCode) has been deleted"
            }
            NotificationType.COURSE_CREATED -> {
                val courseName = variables["courseName"] ?: "course"
                val courseCode = variables["courseCode"] ?: "code"
                "A new course $courseName ($courseCode) has been created"
            }
            NotificationType.COURSE_UPDATED -> {
                val courseName = variables["courseName"] ?: "course"
                val courseCode = variables["courseCode"] ?: "code"
                "Course $courseName ($courseCode) has been updated"
            }
            NotificationType.COURSE_DELETED -> {
                val courseName = variables["courseName"] ?: "course"
                val courseCode = variables["courseCode"] ?: "code"
                "Course $courseName ($courseCode) has been deleted"
            }
            NotificationType.YEAR_LEVEL_CREATED -> {
                val yearLevelName = variables["yearLevelName"] ?: "year level"
                "A new year level $yearLevelName has been created"
            }
            NotificationType.YEAR_LEVEL_UPDATED -> {
                val yearLevelName = variables["yearLevelName"] ?: "year level"
                "Year level $yearLevelName has been updated"
            }
            NotificationType.YEAR_LEVEL_DELETED -> {
                val yearLevelName = variables["yearLevelName"] ?: "year level"
                "Year level $yearLevelName has been deleted"
            }
            NotificationType.STUDENT_LEFT_CLASS -> {
                val subjectName = variables["subjectName"] ?: "subject"
                "A student has left your $subjectName class"
            }
            NotificationType.STUDENT_KICKED_FROM_CLASS -> {
                val subjectName = variables["subjectName"] ?: "subject"
                "A student has been removed from your $subjectName class"
            }
            NotificationType.TEACHER_KICKED_STUDENT -> {
                val subjectName = variables["subjectName"] ?: "subject"
                "You have been removed from $subjectName by your teacher"
            }
            NotificationType.ALL_GRADES_SUBMITTED -> {
                val subjectName = variables["subjectName"] ?: "subject"
                "All grades for $subjectName have been submitted"
            }
            NotificationType.GRADE_COMPLETION_NOTIFICATION -> {
                val subjectName = variables["subjectName"] ?: "subject"
                "All grades for $subjectName have been completed"
            }
            NotificationType.TEACHER_APPLICATION_SUBMITTED -> {
                "A new teacher application has been submitted"
            }
            NotificationType.STUDENT_APPLICATION_SUBMITTED -> {
                val subjectName = variables["subjectName"] ?: "subject"
                "A new student application for $subjectName has been submitted"
            }
            NotificationType.GRADE_REMINDER -> {
                val subjectName = variables["subjectName"] ?: "subject"
                "Reminder: Check your grades for $subjectName"
            }
            NotificationType.GENERAL -> {
                variables["message"] ?: "You have a new notification"
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
