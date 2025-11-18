package com.smartacademictracker.data.service

import android.util.Log
import com.smartacademictracker.data.model.AcademicPeriod
import com.smartacademictracker.data.model.User
import com.smartacademictracker.data.model.UserRole
import com.smartacademictracker.data.model.YearLevel
import com.smartacademictracker.data.notification.NotificationSenderService
import com.smartacademictracker.data.repository.AcademicPeriodRepository
import com.smartacademictracker.data.repository.UserRepository
import com.smartacademictracker.data.repository.YearLevelRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service to handle automatic year level progression for students
 * when academic periods change (e.g., first year to second year)
 * 
 * IMPORTANT: Year level progression only occurs when transitioning from:
 * - Previous period: SUMMER_CLASS of academic year (e.g., "2025-2026")
 * - New period: FIRST_SEMESTER of the NEXT academic year (e.g., "2026-2027")
 * 
 * This ensures students only advance after completing a full academic year
 * (First Semester → Second Semester → Summer → Next Year First Semester)
 */
@Singleton
class YearLevelProgressionService @Inject constructor(
    private val userRepository: UserRepository,
    private val yearLevelRepository: YearLevelRepository,
    private val academicPeriodRepository: AcademicPeriodRepository,
    private val notificationSenderService: NotificationSenderService
) {
    
    /**
     * Process year level progression for all students when transitioning to a new academic year
     * This should be called when setActivePeriod is called and the transition is:
     * SUMMER_CLASS (previous year) → FIRST_SEMESTER (new year)
     * 
     * @param newPeriodId The ID of the newly activated academic period (must be FIRST_SEMESTER of new academic year)
     * @return Result containing the number of students advanced
     */
    suspend fun processYearLevelProgression(newPeriodId: String): Result<YearLevelProgressionResult> {
        return try {
            Log.d("YearLevelProgressionService", "Processing year level progression for period: $newPeriodId")
            
            // Get the new academic period
            val newPeriodResult = academicPeriodRepository.getAcademicPeriodById(newPeriodId)
            val newPeriod = newPeriodResult.getOrNull()
                ?: return Result.failure(Exception("Academic period not found: $newPeriodId"))
            
            // Get all active students
            val studentsResult = userRepository.getUsersByRole(UserRole.STUDENT)
            val students = studentsResult.getOrNull()?.filter { it.active } ?: emptyList()
            
            Log.d("YearLevelProgressionService", "Found ${students.size} active students to process")
            
            var advancedCount = 0
            var skippedCount = 0
            var errorCount = 0
            val errors = mutableListOf<String>()
            
            // Process students in batches to avoid overwhelming Firestore
            val batchSize = 50
            students.chunked(batchSize).forEach { batch ->
                coroutineScope {
                    val deferredResults = batch.map { student ->
                        async {
                            processStudentProgression(student, newPeriod)
                        }
                    }
                    val results = deferredResults.awaitAll()
                    
                    results.forEach { result ->
                        when (result) {
                            is ProgressionResult.Advanced -> advancedCount++
                            is ProgressionResult.Skipped -> skippedCount++
                            is ProgressionResult.Error -> {
                                errorCount++
                                errors.add(result.message)
                            }
                        }
                    }
                }
            }
            
            val progressionResult = YearLevelProgressionResult(
                totalStudents = students.size,
                advanced = advancedCount,
                skipped = skippedCount,
                errors = errorCount,
                errorMessages = errors
            )
            
            Log.d("YearLevelProgressionService", "Year level progression completed: $progressionResult")
            Result.success(progressionResult)
            
        } catch (e: Exception) {
            Log.e("YearLevelProgressionService", "Error processing year level progression: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Process progression for a single student
     */
    private suspend fun processStudentProgression(
        student: User,
        newPeriod: AcademicPeriod
    ): ProgressionResult {
        return try {
            // Skip if student doesn't have a year level or course
            if (student.yearLevelId == null || student.courseId == null) {
                Log.d("YearLevelProgressionService", "Skipping student ${student.studentId}: missing year level or course")
                return ProgressionResult.Skipped("Student ${student.studentId} has no year level or course assigned")
            }
            
            // Get current year level
            val currentYearLevelResult = yearLevelRepository.getYearLevelById(student.yearLevelId)
            val currentYearLevel = currentYearLevelResult.getOrNull()
                ?: return ProgressionResult.Error("Year level not found: ${student.yearLevelId}")
            
            // Check if student was already in this academic period
            if (student.lastAcademicPeriodId == newPeriod.id) {
                // Student was already processed, but check if yearLevelName matches yearLevelId
                // This fixes cases where progression ran but yearLevelName wasn't updated (due to previous bug)
                if (student.yearLevelName != currentYearLevel.name) {
                    Log.w("YearLevelProgressionService", "Student ${student.studentId} was processed but yearLevelName (${student.yearLevelName}) doesn't match yearLevelId (${currentYearLevel.name}). Fixing...")
                    // Update yearLevelName to match the current yearLevelId
                    val fixResult = userRepository.updateStudentYearLevel(
                        studentId = student.id,
                        newYearLevelId = student.yearLevelId,
                        newYearLevelName = currentYearLevel.name,
                        academicPeriodId = newPeriod.id
                    )
                    if (fixResult.isSuccess) {
                        Log.d("YearLevelProgressionService", "Fixed yearLevelName for student ${student.studentId}: ${student.yearLevelName} -> ${currentYearLevel.name}")
                        return ProgressionResult.Advanced(
                            studentId = student.studentId ?: student.id,
                            oldYearLevel = student.yearLevelName ?: "Unknown",
                            newYearLevel = currentYearLevel.name
                        )
                    } else {
                        return ProgressionResult.Error("Failed to fix yearLevelName: ${fixResult.exceptionOrNull()?.message}")
                    }
                } else {
                    Log.d("YearLevelProgressionService", "Skipping student ${student.studentId}: already processed for this period and yearLevelName is correct")
                    return ProgressionResult.Skipped("Student ${student.studentId} already processed for period ${newPeriod.id}")
                }
            }
            
            // Check if student is already at maximum year level (typically 4th year)
            // Don't advance students who are already at the highest level
            val maxLevel = 4 // Assuming 4-year program
            if (currentYearLevel.level >= maxLevel) {
                Log.d("YearLevelProgressionService", "Skipping student ${student.studentId}: already at max level ${currentYearLevel.level}")
                // Still update lastAcademicPeriodId even if not advancing
                userRepository.updateStudentAcademicPeriod(student.id, newPeriod.id)
                return ProgressionResult.Skipped("Student ${student.studentId} already at maximum year level")
            }
            
            // Get next year level
            val nextLevel = currentYearLevel.level + 1
            val nextYearLevelResult = yearLevelRepository.getYearLevelsByCourse(student.courseId)
            val nextYearLevel = nextYearLevelResult.getOrNull()
                ?.find { it.level == nextLevel && it.courseId == student.courseId }
            
            if (nextYearLevel == null) {
                Log.w("YearLevelProgressionService", "Next year level not found for student ${student.studentId}: level $nextLevel")
                // Still update lastAcademicPeriodId
                userRepository.updateStudentAcademicPeriod(student.id, newPeriod.id)
                return ProgressionResult.Error("Next year level (level $nextLevel) not found for course ${student.courseId}")
            }
            
            // Advance student to next year level
            val updateResult = userRepository.updateStudentYearLevel(
                studentId = student.id,
                newYearLevelId = nextYearLevel.id,
                newYearLevelName = nextYearLevel.name,
                academicPeriodId = newPeriod.id
            )
            
            if (updateResult.isSuccess) {
                // Send notification to student
                val studentName = "${student.firstName} ${student.lastName}"
                val message = "Congratulations! You have completed the academic year and have been advanced from ${currentYearLevel.name} to ${nextYearLevel.name} for Academic Year ${newPeriod.academicYear}."
                notificationSenderService.sendNotification(
                    userId = student.id,
                    type = com.smartacademictracker.data.model.NotificationType.GENERAL,
                    variables = mapOf(
                        "message" to message
                    ),
                    priority = com.smartacademictracker.data.model.NotificationPriority.HIGH
                )
                
                Log.d("YearLevelProgressionService", "Advanced student ${student.studentId} from ${currentYearLevel.name} to ${nextYearLevel.name}")
                ProgressionResult.Advanced(
                    studentId = student.studentId ?: student.id,
                    oldYearLevel = currentYearLevel.name,
                    newYearLevel = nextYearLevel.name
                )
            } else {
                ProgressionResult.Error("Failed to update year level: ${updateResult.exceptionOrNull()?.message}")
            }
            
        } catch (e: Exception) {
            Log.e("YearLevelProgressionService", "Error processing student ${student.studentId}: ${e.message}", e)
            ProgressionResult.Error("Error: ${e.message}")
        }
    }
    
    /**
     * Result of processing a single student
     */
    private sealed class ProgressionResult {
        data class Advanced(
            val studentId: String,
            val oldYearLevel: String,
            val newYearLevel: String
        ) : ProgressionResult()
        
        data class Skipped(val reason: String) : ProgressionResult()
        data class Error(val message: String) : ProgressionResult()
    }
    
    /**
     * Result of year level progression process
     */
    data class YearLevelProgressionResult(
        val totalStudents: Int,
        val advanced: Int,
        val skipped: Int,
        val errors: Int,
        val errorMessages: List<String>
    ) {
        fun getSummaryMessage(): String {
            return "Year level progression completed:\n" +
                    "• Total students: $totalStudents\n" +
                    "• Advanced: $advanced\n" +
                    "• Skipped: $skipped\n" +
                    "• Errors: $errors"
        }
    }
}

