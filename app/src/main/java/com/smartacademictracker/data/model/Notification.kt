package com.smartacademictracker.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import java.util.Date

data class Notification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: NotificationType = NotificationType.GRADE_UPDATE,
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    @PropertyName("read")
    val isRead: Boolean = false,
    @PropertyName("delivered")
    val isDelivered: Boolean = false,
    val createdAt: Timestamp = Timestamp.now(),
    val readAt: Timestamp? = null,
    val data: Map<String, String> = emptyMap(),
    val actionUrl: String? = null,
    val academicPeriodId: String = "" // Reference to active academic period
)

enum class NotificationType {
    GRADE_UPDATE,
    GRADE_REMINDER,
    APPLICATION_APPROVED,
    APPLICATION_REJECTED,
    DEADLINE_REMINDER,
    SYSTEM_ANNOUNCEMENT,
    SUBJECT_APPLICATION_APPROVED,
    SUBJECT_APPLICATION_REJECTED,
    TEACHER_APPLICATION_APPROVED,
    TEACHER_APPLICATION_REJECTED,
    TEACHER_APPLICATION_SUBMITTED,
    STUDENT_APPLICATION_SUBMITTED,
    ACADEMIC_PERIOD_ACTIVATED,
    GRADE_SUBMISSION_DEADLINE,
    PERFORMANCE_ALERT,
    // Enrollment actions
    STUDENT_LEFT_CLASS,
    STUDENT_KICKED_FROM_CLASS,
    TEACHER_KICKED_STUDENT,
    STUDENT_ENROLLED,
    STUDENT_DROPPED,
    // Admin actions
    ALL_GRADES_SUBMITTED,
    GRADE_COMPLETION_NOTIFICATION,
    GRADE_EDIT_REQUEST_APPROVED,
    GRADE_EDIT_REQUEST_REJECTED,
    TEACHER_ASSIGNED_TO_SECTION,
    TEACHER_REMOVED_FROM_SECTION,
    USER_STATUS_CHANGED,
    USER_ROLE_CHANGED,
    SUBJECT_CREATED,
    SUBJECT_UPDATED,
    SUBJECT_DELETED,
    COURSE_CREATED,
    COURSE_UPDATED,
    COURSE_DELETED,
    YEAR_LEVEL_CREATED,
    YEAR_LEVEL_UPDATED,
    YEAR_LEVEL_DELETED,
    GENERAL
}

enum class NotificationPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}

data class NotificationTemplate(
    val id: String = "",
    val name: String = "",
    val type: NotificationType = NotificationType.GENERAL,
    val titleTemplate: String = "",
    val messageTemplate: String = "",
    val isActive: Boolean = true,
    val createdAt: Timestamp = Timestamp.now(),
    val variables: List<String> = emptyList()
)

data class NotificationPreferences(
    val userId: String = "",
    val emailNotifications: Boolean = true,
    val pushNotifications: Boolean = true,
    val inAppNotifications: Boolean = true,
    val gradeUpdateNotifications: Boolean = true,
    val applicationStatusNotifications: Boolean = true,
    val deadlineReminderNotifications: Boolean = true,
    val systemAnnouncementNotifications: Boolean = true,
    val performanceAlertNotifications: Boolean = true,
    val quietHoursStart: String? = null, // Format: "HH:mm"
    val quietHoursEnd: String? = null, // Format: "HH:mm"
    val updatedAt: Timestamp = Timestamp.now()
)

data class NotificationStats(
    val userId: String = "",
    val totalNotifications: Int = 0,
    val unreadNotifications: Int = 0,
    val readNotifications: Int = 0,
    val lastNotificationAt: Timestamp? = null,
    val notificationTypes: Map<NotificationType, Int> = emptyMap()
)
