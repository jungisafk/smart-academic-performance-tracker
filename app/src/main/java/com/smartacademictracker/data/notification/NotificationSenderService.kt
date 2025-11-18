package com.smartacademictracker.data.notification

import com.google.firebase.Timestamp
import com.smartacademictracker.data.model.Notification
import com.smartacademictracker.data.model.NotificationType
import com.smartacademictracker.data.model.NotificationPriority
import com.smartacademictracker.data.repository.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationSenderService @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val localNotificationService: LocalNotificationService,
    private val templateService: NotificationTemplateService,
    private val userRepository: com.smartacademictracker.data.repository.UserRepository
) {

    fun sendGradeUpdateNotification(
        userId: String,
        subjectName: String,
        grade: String,
        period: String,
        score: Double,
        maxScore: Double
    ) {
        val percentage = String.format("%.1f", (score / maxScore) * 100)
        val message = "Your $period grade for $subjectName: $score/$maxScore ($percentage%)"
        
        val variables = mapOf(
            "subjectName" to subjectName,
            "gradePeriod" to period,
            "score" to score.toString(),
            "maxScore" to maxScore.toString(),
            "percentage" to percentage,
            "message" to message
        )

        sendNotification(
            userId = userId,
            type = NotificationType.GRADE_UPDATE,
            variables = variables,
            priority = NotificationPriority.HIGH
        )
    }
    
    /**
     * Send notification for multiple grade periods submitted at once
     * @param userId Student ID to notify
     * @param subjectName Subject name
     * @param submittedPeriods List of grade periods that were submitted (e.g., [PRELIM, MIDTERM])
     */
    fun sendMultipleGradeUpdateNotification(
        userId: String,
        subjectName: String,
        submittedPeriods: List<com.smartacademictracker.data.model.GradePeriod>
    ) {
        val periodNames = submittedPeriods.map { it.displayName.lowercase() }
        val message = when {
            submittedPeriods.size == 1 -> {
                "Your ${periodNames[0]} grade is available for viewing"
            }
            submittedPeriods.size == 2 -> {
                "Your ${periodNames[0]} and ${periodNames[1]} grades are available for viewing"
            }
            submittedPeriods.contains(com.smartacademictracker.data.model.GradePeriod.FINAL) -> {
                "Your final grade is available for viewing"
            }
            else -> {
                "Your ${periodNames.joinToString(" and ")} grades are available for viewing"
            }
        }
        
        val variables = mapOf(
            "subjectName" to subjectName,
            "gradePeriods" to submittedPeriods.joinToString(", ") { it.displayName },
            "message" to message
        )

        sendNotification(
            userId = userId,
            type = NotificationType.GRADE_UPDATE,
            variables = variables,
            priority = NotificationPriority.HIGH
        )
    }

    fun sendApplicationStatusNotification(
        userId: String,
        applicationType: String,
        status: String,
        subjectName: String? = null,
        reason: String? = null
    ) {
        val variables = mutableMapOf(
            "applicationType" to applicationType,
            "subjectName" to (subjectName ?: ""),
            "reason" to (reason ?: "")
        )

        val type = when {
            status == "approved" && applicationType.contains("Teacher", ignoreCase = true) -> NotificationType.TEACHER_APPLICATION_APPROVED
            status == "rejected" && applicationType.contains("Teacher", ignoreCase = true) -> NotificationType.TEACHER_APPLICATION_REJECTED
            status == "approved" && applicationType.contains("Subject", ignoreCase = true) -> NotificationType.SUBJECT_APPLICATION_APPROVED
            status == "rejected" && applicationType.contains("Subject", ignoreCase = true) -> NotificationType.SUBJECT_APPLICATION_REJECTED
            status == "approved" -> NotificationType.APPLICATION_APPROVED
            else -> NotificationType.APPLICATION_REJECTED
        }

        sendNotification(
            userId = userId,
            type = type,
            variables = variables,
            priority = NotificationPriority.HIGH
        )
    }

    fun sendDeadlineReminderNotification(
        userId: String,
        deadlineType: String,
        deadlineDate: String,
        subjectName: String? = null
    ) {
        val variables = mapOf(
            "deadlineType" to deadlineType,
            "subjectName" to (subjectName ?: ""),
            "deadlineDate" to deadlineDate
        )

        sendNotification(
            userId = userId,
            type = NotificationType.DEADLINE_REMINDER,
            variables = variables,
            priority = NotificationPriority.NORMAL
        )
    }

    fun sendSystemAnnouncementNotification(
        userId: String,
        title: String,
        message: String
    ) {
        val variables = mapOf(
            "title" to title,
            "message" to message
        )

        sendNotification(
            userId = userId,
            type = NotificationType.SYSTEM_ANNOUNCEMENT,
            variables = variables,
            priority = NotificationPriority.NORMAL
        )
    }

    fun sendPerformanceAlertNotification(
        userId: String,
        subjectName: String,
        performance: String
    ) {
        val variables = mapOf(
            "subjectName" to subjectName,
            "performance" to performance
        )

        sendNotification(
            userId = userId,
            type = NotificationType.PERFORMANCE_ALERT,
            variables = variables,
            priority = NotificationPriority.HIGH
        )
    }

    fun sendGradeSubmissionDeadlineNotification(
        userId: String,
        subjectName: String,
        deadlineDate: String
    ) {
        val variables = mapOf(
            "subjectName" to subjectName,
            "deadlineDate" to deadlineDate
        )

        sendNotification(
            userId = userId,
            type = NotificationType.GRADE_SUBMISSION_DEADLINE,
            variables = variables,
            priority = NotificationPriority.NORMAL
        )
    }

    fun sendAcademicPeriodActivatedNotification(
        userId: String,
        periodName: String
    ) {
        val variables = mapOf(
            "periodName" to periodName
        )

        sendNotification(
            userId = userId,
            type = NotificationType.ACADEMIC_PERIOD_ACTIVATED,
            variables = variables,
            priority = NotificationPriority.NORMAL
        )
    }

    fun sendNotification(
        userId: String,
        type: NotificationType,
        variables: Map<String, String>,
        priority: NotificationPriority = NotificationPriority.NORMAL
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Create notification from template
                val notification = templateService.createNotificationFromTemplate(
                    type = type,
                    userId = userId,
                    variables = variables,
                    priority = priority
                )

                // Save to database
                android.util.Log.d("NotificationSenderService", "Saving notification to database - Type: $type, UserId: $userId")
                val result = notificationRepository.createNotification(notification)
                result.fold(
                    onSuccess = { savedNotification ->
                        android.util.Log.d("NotificationSenderService", "Notification saved successfully, showing local notification")
                        // Show local notification
                        showLocalNotification(savedNotification)
                    },
                    onFailure = { error ->
                        // Handle error silently or log it
                        android.util.Log.e("NotificationSenderService", "Failed to save notification: ${error.message}", error)
                        // Still try to show notification even if save fails
                        android.util.Log.d("NotificationSenderService", "Attempting to show notification despite save failure")
                        showLocalNotification(notification)
                    }
                )
            } catch (e: Exception) {
                // Handle error silently or log it
                android.util.Log.e("NotificationSenderService", "Exception in sendNotification: ${e.message}", e)
                e.printStackTrace()
            }
        }
    }

    private fun showLocalNotification(notification: Notification) {
        android.util.Log.d("NotificationSenderService", "showLocalNotification called - Title: ${notification.title}, Type: ${notification.type}, UserId: ${notification.userId}")
        
        // Only show notification locally if it's for the current logged-in user
        // Otherwise, the notification will be picked up by the intended user's device via real-time listeners
        CoroutineScope(Dispatchers.IO).launch {
            val currentUserResult = userRepository.getCurrentUser()
            currentUserResult.onSuccess { currentUser ->
                if (currentUser != null && currentUser.id == notification.userId) {
                    android.util.Log.d("NotificationSenderService", "Notification is for current user, showing locally")
                    localNotificationService.showNotification(
                        title = notification.title,
                        message = notification.message,
                        type = notification.type,
                        priority = notification.priority,
                        actionUrl = notification.actionUrl,
                        data = notification.data
                    )
                } else {
                    android.util.Log.d("NotificationSenderService", "Notification is for user ${notification.userId}, current user is ${currentUser?.id}. Not showing locally - will be picked up by intended user's device.")
                }
            }.onFailure { error ->
                android.util.Log.w("NotificationSenderService", "Failed to get current user, showing notification anyway: ${error.message}")
                // If we can't get current user, show notification anyway (fallback)
                localNotificationService.showNotification(
                    title = notification.title,
                    message = notification.message,
                    type = notification.type,
                    priority = notification.priority,
                    actionUrl = notification.actionUrl,
                    data = notification.data
                )
            }
        }
    }

    fun sendBulkNotification(
        userIds: List<String>,
        type: NotificationType,
        variables: Map<String, String>,
        priority: NotificationPriority = NotificationPriority.NORMAL
    ) {
        userIds.forEach { userId ->
            sendNotification(userId, type, variables, priority)
        }
    }

    fun sendNotificationToAllUsers(
        type: NotificationType,
        variables: Map<String, String>,
        priority: NotificationPriority = NotificationPriority.NORMAL
    ) {
        // This would typically get all user IDs from the database
        // For now, we'll implement a placeholder
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get all user IDs from the database
                // This is a placeholder - in a real implementation,
                // you would query the users collection
                val allUserIds = listOf<String>() // Placeholder
                
                allUserIds.forEach { userId ->
                    sendNotification(userId, type, variables, priority)
                }
            } catch (e: Exception) {
                println("Failed to send bulk notification: ${e.message}")
            }
        }
    }
    
    // ========== NEW NOTIFICATION METHODS ==========
    
    fun sendTeacherAssignedToSectionNotification(
        teacherId: String,
        subjectName: String,
        sectionName: String,
        assignedBy: String
    ) {
        val variables = mapOf(
            "subjectName" to subjectName,
            "sectionName" to sectionName,
            "assignedBy" to assignedBy
        )
        sendNotification(
            userId = teacherId,
            type = NotificationType.TEACHER_ASSIGNED_TO_SECTION,
            variables = variables,
            priority = NotificationPriority.HIGH
        )
    }
    
    fun sendTeacherRemovedFromSectionNotification(
        teacherId: String,
        subjectName: String,
        sectionName: String,
        removedBy: String
    ) {
        val variables = mapOf(
            "subjectName" to subjectName,
            "sectionName" to sectionName,
            "removedBy" to removedBy
        )
        sendNotification(
            userId = teacherId,
            type = NotificationType.TEACHER_REMOVED_FROM_SECTION,
            variables = variables,
            priority = NotificationPriority.HIGH
        )
    }
    
    fun sendStudentEnrolledNotification(
        studentId: String,
        subjectName: String,
        sectionName: String,
        teacherName: String
    ) {
        val variables = mapOf(
            "subjectName" to subjectName,
            "sectionName" to sectionName,
            "teacherName" to teacherName
        )
        sendNotification(
            userId = studentId,
            type = NotificationType.STUDENT_ENROLLED,
            variables = variables,
            priority = NotificationPriority.HIGH
        )
    }
    
    fun sendStudentDroppedNotification(
        studentId: String,
        subjectName: String,
        sectionName: String,
        droppedBy: String,
        reason: String? = null
    ) {
        val reasonText = if (reason.isNullOrBlank()) "" else " Reason: $reason"
        val variables = mapOf(
            "subjectName" to subjectName,
            "sectionName" to sectionName,
            "droppedBy" to droppedBy,
            "reason" to reasonText
        )
        sendNotification(
            userId = studentId,
            type = NotificationType.STUDENT_DROPPED,
            variables = variables,
            priority = NotificationPriority.HIGH
        )
    }
    
    fun sendGradeEditRequestApprovedNotification(
        teacherId: String,
        studentName: String,
        subjectName: String,
        gradePeriod: String
    ) {
        val variables = mapOf(
            "studentName" to studentName,
            "subjectName" to subjectName,
            "gradePeriod" to gradePeriod
        )
        sendNotification(
            userId = teacherId,
            type = NotificationType.GRADE_EDIT_REQUEST_APPROVED,
            variables = variables,
            priority = NotificationPriority.HIGH
        )
    }
    
    fun sendGradeEditRequestRejectedNotification(
        teacherId: String,
        studentName: String,
        subjectName: String,
        gradePeriod: String,
        reason: String? = null
    ) {
        val reasonText = if (reason.isNullOrBlank()) "" else " Reason: $reason"
        val variables = mapOf(
            "studentName" to studentName,
            "subjectName" to subjectName,
            "gradePeriod" to gradePeriod,
            "reason" to reasonText
        )
        sendNotification(
            userId = teacherId,
            type = NotificationType.GRADE_EDIT_REQUEST_REJECTED,
            variables = variables,
            priority = NotificationPriority.NORMAL
        )
    }
    
    fun sendUserStatusChangedNotification(
        userId: String,
        status: String,
        changedBy: String
    ) {
        val variables = mapOf(
            "status" to status,
            "changedBy" to changedBy
        )
        sendNotification(
            userId = userId,
            type = NotificationType.USER_STATUS_CHANGED,
            variables = variables,
            priority = NotificationPriority.HIGH
        )
    }
    
    fun sendUserRoleChangedNotification(
        userId: String,
        newRole: String,
        changedBy: String
    ) {
        val variables = mapOf(
            "newRole" to newRole,
            "changedBy" to changedBy
        )
        sendNotification(
            userId = userId,
            type = NotificationType.USER_ROLE_CHANGED,
            variables = variables,
            priority = NotificationPriority.HIGH
        )
    }
    
    fun sendSubjectCreatedNotification(
        userIds: List<String>,
        subjectName: String,
        subjectCode: String
    ) {
        val variables = mapOf(
            "subjectName" to subjectName,
            "subjectCode" to subjectCode
        )
        sendBulkNotification(
            userIds = userIds,
            type = NotificationType.SUBJECT_CREATED,
            variables = variables,
            priority = NotificationPriority.NORMAL
        )
    }
    
    fun sendSubjectUpdatedNotification(
        userIds: List<String>,
        subjectName: String,
        subjectCode: String
    ) {
        val variables = mapOf(
            "subjectName" to subjectName,
            "subjectCode" to subjectCode
        )
        sendBulkNotification(
            userIds = userIds,
            type = NotificationType.SUBJECT_UPDATED,
            variables = variables,
            priority = NotificationPriority.NORMAL
        )
    }
    
    fun sendSubjectDeletedNotification(
        userIds: List<String>,
        subjectName: String,
        subjectCode: String
    ) {
        val variables = mapOf(
            "subjectName" to subjectName,
            "subjectCode" to subjectCode
        )
        sendBulkNotification(
            userIds = userIds,
            type = NotificationType.SUBJECT_DELETED,
            variables = variables,
            priority = NotificationPriority.HIGH
        )
    }
    
    fun sendCourseCreatedNotification(
        userIds: List<String>,
        courseName: String,
        courseCode: String
    ) {
        val variables = mapOf(
            "courseName" to courseName,
            "courseCode" to courseCode
        )
        sendBulkNotification(
            userIds = userIds,
            type = NotificationType.COURSE_CREATED,
            variables = variables,
            priority = NotificationPriority.NORMAL
        )
    }
    
    fun sendCourseUpdatedNotification(
        userIds: List<String>,
        courseName: String,
        courseCode: String
    ) {
        val variables = mapOf(
            "courseName" to courseName,
            "courseCode" to courseCode
        )
        sendBulkNotification(
            userIds = userIds,
            type = NotificationType.COURSE_UPDATED,
            variables = variables,
            priority = NotificationPriority.NORMAL
        )
    }
    
    fun sendCourseDeletedNotification(
        userIds: List<String>,
        courseName: String,
        courseCode: String
    ) {
        val variables = mapOf(
            "courseName" to courseName,
            "courseCode" to courseCode
        )
        sendBulkNotification(
            userIds = userIds,
            type = NotificationType.COURSE_DELETED,
            variables = variables,
            priority = NotificationPriority.HIGH
        )
    }
    
    fun sendYearLevelCreatedNotification(
        userIds: List<String>,
        yearLevelName: String
    ) {
        val variables = mapOf(
            "yearLevelName" to yearLevelName
        )
        sendBulkNotification(
            userIds = userIds,
            type = NotificationType.YEAR_LEVEL_CREATED,
            variables = variables,
            priority = NotificationPriority.NORMAL
        )
    }
    
    fun sendYearLevelUpdatedNotification(
        userIds: List<String>,
        yearLevelName: String
    ) {
        val variables = mapOf(
            "yearLevelName" to yearLevelName
        )
        sendBulkNotification(
            userIds = userIds,
            type = NotificationType.YEAR_LEVEL_UPDATED,
            variables = variables,
            priority = NotificationPriority.NORMAL
        )
    }
    
    fun sendYearLevelDeletedNotification(
        userIds: List<String>,
        yearLevelName: String
    ) {
        val variables = mapOf(
            "yearLevelName" to yearLevelName
        )
        sendBulkNotification(
            userIds = userIds,
            type = NotificationType.YEAR_LEVEL_DELETED,
            variables = variables,
            priority = NotificationPriority.HIGH
        )
    }
}
