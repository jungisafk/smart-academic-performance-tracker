package com.smartacademictracker.data.notification

import android.util.Log
import com.smartacademictracker.data.model.Notification
import com.smartacademictracker.data.model.NotificationType
import com.smartacademictracker.data.model.NotificationPriority
import com.smartacademictracker.data.repository.NotificationRepository
import com.smartacademictracker.data.repository.UserRepository
import com.smartacademictracker.data.repository.GradeRepository
import com.smartacademictracker.data.repository.EnrollmentRepository
import com.smartacademictracker.data.repository.SubjectRepository
import com.smartacademictracker.data.model.GradePeriod
import com.smartacademictracker.data.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for handling notifications when all grades are submitted for a class
 */
@Singleton
class GradeCompletionNotificationService @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
    private val gradeRepository: GradeRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val subjectRepository: SubjectRepository
) {
    
    /**
     * Check if all students in a class have grades for a specific period
     * and notify admin if complete
     */
    suspend fun checkAndNotifyGradeCompletion(
        subjectId: String,
        subjectName: String,
        teacherId: String,
        teacherName: String,
        gradePeriod: GradePeriod
    ): Result<Unit> {
        return try {
            // Get all enrolled students for this subject
            val enrollmentsResult = enrollmentRepository.getEnrollmentsBySubject(subjectId)
            if (enrollmentsResult.isFailure) {
                return Result.failure(enrollmentsResult.exceptionOrNull() ?: Exception("Failed to get enrollments"))
            }
            
            val enrollments = enrollmentsResult.getOrNull() ?: emptyList()
            val activeEnrollments = enrollments.filter { 
                it.active == true
            }
            
            if (activeEnrollments.isEmpty()) {
                return Result.success(Unit) // No students to check
            }
            
            // Check if all students have grades for this period
            var studentsWithGrades = 0
            var totalStudents = activeEnrollments.size
            
            for (enrollment in activeEnrollments) {
                val gradesResult = gradeRepository.getGradesByStudent(enrollment.studentId)
                if (gradesResult.isSuccess) {
                    val grades = gradesResult.getOrNull() ?: emptyList()
                    val hasGradeForPeriod = grades.any { 
                        it.subjectId == subjectId && it.gradePeriod == gradePeriod 
                    }
                    if (hasGradeForPeriod) {
                        studentsWithGrades++
                    }
                }
            }
            
            // If all students have grades, notify admin
            if (studentsWithGrades == totalStudents && totalStudents > 0) {
                notifyAdminGradeCompletion(
                    subjectId = subjectId,
                    subjectName = subjectName,
                    teacherId = teacherId,
                    teacherName = teacherName,
                    gradePeriod = gradePeriod,
                    totalStudents = totalStudents
                )
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Notify admin when all grades are submitted for a class
     */
    private suspend fun notifyAdminGradeCompletion(
        subjectId: String,
        subjectName: String,
        teacherId: String,
        teacherName: String,
        gradePeriod: GradePeriod,
        totalStudents: Int
    ) {
        try {
            // Get all admin users
            val adminUsers = userRepository.getUsersByRole(UserRole.ADMIN)
            adminUsers.onSuccess { admins ->
                admins.forEach { admin ->
                    val notification = Notification(
                        userId = admin.id,
                        title = "All Grades Submitted",
                        message = "$teacherName has submitted grades for all $totalStudents students in $subjectName for ${gradePeriod.displayName}.",
                        type = NotificationType.ALL_GRADES_SUBMITTED,
                        priority = NotificationPriority.HIGH,
                        data = mapOf(
                            "subjectId" to subjectId,
                            "subjectName" to subjectName,
                            "teacherId" to teacherId,
                            "teacherName" to teacherName,
                            "gradePeriod" to gradePeriod.name,
                            "totalStudents" to totalStudents.toString(),
                            "completionDate" to System.currentTimeMillis().toString()
                        )
                    )
                    
                    CoroutineScope(Dispatchers.IO).launch {
                        notificationRepository.createNotification(notification)
                    }
                }
            }
            
            // Also notify the teacher about completion
            val teacherNotification = Notification(
                userId = teacherId,
                title = "Grades Submitted Successfully",
                message = "You have completed grading all $totalStudents students in $subjectName for ${gradePeriod.displayName}.",
                type = NotificationType.GRADE_COMPLETION_NOTIFICATION,
                priority = NotificationPriority.NORMAL,
                data = mapOf(
                    "subjectId" to subjectId,
                    "subjectName" to subjectName,
                    "gradePeriod" to gradePeriod.name,
                    "totalStudents" to totalStudents.toString()
                )
            )
            
            CoroutineScope(Dispatchers.IO).launch {
                notificationRepository.createNotification(teacherNotification)
            }
            
        } catch (e: Exception) {
            // Log error but don't fail the entire operation
            Log.e("GradeCompletionNotification", "Error notifying grade completion: ${e.message}")
            // Consider logging to a proper logging service in production
        }
    }
    
    /**
     * Check grade completion for multiple subjects (batch operation)
     */
    suspend fun checkGradeCompletionForMultipleSubjects(
        subjectIds: List<String>,
        gradePeriod: GradePeriod
    ): Result<Unit> {
        return try {
            for (subjectId in subjectIds) {
                // Get subject details from repository
                val subjectResult = subjectRepository.getSubjectById(subjectId)
                subjectResult.onSuccess { subject ->
                    // Get teacher information if teacherId exists
                    val teacherId = subject.teacherId ?: ""
                    val teacherName = subject.teacherName ?: ""
                    
                    // If teacherId exists but teacherName is missing, fetch teacher details
                    val finalTeacherName = if (teacherId.isNotEmpty() && teacherName.isEmpty()) {
                        val teacherResult = userRepository.getUserById(teacherId)
                        teacherResult.getOrNull()?.let { teacher ->
                            "${teacher.firstName} ${teacher.lastName}".trim()
                        } ?: ""
                    } else {
                        teacherName
                    }
                    
                    checkAndNotifyGradeCompletion(
                        subjectId = subjectId,
                        subjectName = subject.name,
                        teacherId = teacherId,
                        teacherName = finalTeacherName,
                        gradePeriod = gradePeriod
                    )
                }.onFailure { exception ->
                    // Log error but continue with other subjects
                    Log.e("GradeCompletionNotification", "Error fetching subject $subjectId: ${exception.message}")
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
