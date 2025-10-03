package com.smartacademictracker.data.monitoring

import com.smartacademictracker.data.model.Grade
import com.smartacademictracker.data.repository.GradeRepository
import com.smartacademictracker.data.repository.UserRepository
import com.smartacademictracker.data.notification.GradeNotificationService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GradeMonitoringService @Inject constructor(
    private val gradeRepository: GradeRepository,
    private val userRepository: UserRepository,
    private val notificationService: GradeNotificationService
) {
    
    private val lastKnownGrades = mutableMapOf<String, List<Grade>>()
    
    /**
     * Monitor grade changes for the current user
     */
    suspend fun monitorGradeChanges(): Flow<List<Grade>> = flow {
        val currentUserResult = userRepository.getCurrentUser()
        if (currentUserResult.isSuccess) {
            val user = currentUserResult.getOrNull()
            if (user != null) {
                val gradesResult = gradeRepository.getGradesByStudent(user.id)
                if (gradesResult.isSuccess) {
                    val grades = gradesResult.getOrNull() ?: emptyList()
                    val userId = user.id
                    val previousGrades = lastKnownGrades[userId] ?: emptyList()
                    
                    // Find new grades
                    val newGrades = grades.filter { newGrade ->
                        !previousGrades.any { existingGrade ->
                            existingGrade.id == newGrade.id
                        }
                    }
                    
                    // Find updated grades
                    val updatedGrades = grades.filter { newGrade ->
                        previousGrades.any { existingGrade ->
                            existingGrade.id == newGrade.id && 
                            existingGrade.dateRecorded != newGrade.dateRecorded
                        }
                    }
                    
                    val allNewGrades = newGrades + updatedGrades
                    
                    if (allNewGrades.isNotEmpty()) {
                        // Send notifications
                        if (allNewGrades.size == 1) {
                            notificationService.showGradeUpdateNotification(allNewGrades.first())
                        } else {
                            notificationService.showMultipleGradesNotification(allNewGrades)
                        }
                    }
                    
                    // Update last known grades
                    lastKnownGrades[userId] = grades
                    
                    emit(allNewGrades)
                }
            }
        }
    }
    
    /**
     * Get grade change notifications for a specific user
     */
    suspend fun getGradeChangeNotifications(userId: String): Flow<List<Grade>> = flow {
        val currentUserResult = userRepository.getCurrentUser()
        if (currentUserResult.isSuccess) {
            val currentUser = currentUserResult.getOrNull()
            if (currentUser?.id == userId) {
                val gradesResult = gradeRepository.getGradesByStudent(userId)
                if (gradesResult.isSuccess) {
                    val grades = gradesResult.getOrNull() ?: emptyList()
                    val previousGrades = lastKnownGrades[userId] ?: emptyList()
                    
                    val newGrades = grades.filter { newGrade ->
                        !previousGrades.any { existingGrade ->
                            existingGrade.id == newGrade.id
                        }
                    }
                    
                    val updatedGrades = grades.filter { newGrade ->
                        previousGrades.any { existingGrade ->
                            existingGrade.id == newGrade.id && 
                            existingGrade.dateRecorded != newGrade.dateRecorded
                        }
                    }
                    
                    val allNewGrades = newGrades + updatedGrades
                    
                    if (allNewGrades.isNotEmpty()) {
                        if (allNewGrades.size == 1) {
                            notificationService.showGradeUpdateNotification(allNewGrades.first())
                        } else {
                            notificationService.showMultipleGradesNotification(allNewGrades)
                        }
                    }
                    
                    lastKnownGrades[userId] = grades
                    emit(allNewGrades)
                }
            }
        }
    }
    
    /**
     * Clear monitoring data for a user
     */
    fun clearMonitoringData(userId: String) {
        lastKnownGrades.remove(userId)
    }
    
    /**
     * Clear all monitoring data
     */
    fun clearAllMonitoringData() {
        lastKnownGrades.clear()
    }
}
