package com.smartacademictracker.data.sync

import com.smartacademictracker.data.local.database.AppDatabase
import com.smartacademictracker.data.local.entity.OfflineGrade
import com.smartacademictracker.data.local.entity.SyncStatus
import com.smartacademictracker.data.local.entity.ResolutionStrategy
import com.smartacademictracker.data.repository.GradeRepository
import com.smartacademictracker.data.network.NetworkMonitor
import com.smartacademictracker.data.model.Grade
import com.smartacademictracker.data.model.GradePeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GradeSyncManager @Inject constructor(
    private val database: AppDatabase,
    private val gradeRepository: GradeRepository,
    private val networkMonitor: NetworkMonitor
) {
    
    private val offlineGradeDao = database.offlineGradeDao()
    
    /**
     * Monitor sync status and automatically sync when online
     */
    fun getSyncStatus(): Flow<SyncStatusInfo> = combine(
        networkMonitor.isOnline(),
        offlineGradeDao.getGradesBySyncStatus(SyncStatus.PENDING),
        offlineGradeDao.getGradesBySyncStatus(SyncStatus.FAILED),
        offlineGradeDao.getGradesBySyncStatus(SyncStatus.CONFLICT)
    ) { isOnline, pendingGrades, failedGrades, conflictGrades ->
        SyncStatusInfo(
            isOnline = isOnline,
            pendingCount = pendingGrades.size,
            failedCount = failedGrades.size,
            conflictCount = conflictGrades.size,
            totalPending = pendingGrades.size + failedGrades.size + conflictGrades.size
        )
    }.distinctUntilChanged()
    
    /**
     * Sync all pending grades to server
     */
    suspend fun syncPendingGrades(): SyncResult {
        if (!networkMonitor.isCurrentlyConnected()) {
            return SyncResult.FAILED("No internet connection")
        }
        
        try {
            val pendingGrades = offlineGradeDao.getGradesBySyncStatus(SyncStatus.PENDING).first()
            val failedGrades = offlineGradeDao.getGradesBySyncStatus(SyncStatus.FAILED).first()
            val allGradesToSync = pendingGrades + failedGrades
            
            if (allGradesToSync.isEmpty()) {
                return SyncResult.SUCCESS("No grades to sync")
            }
            
            var successCount = 0
            var failureCount = 0
            val conflicts = mutableListOf<OfflineGrade>()
            
            for (offlineGrade in allGradesToSync) {
                try {
                    // Mark as syncing
                    offlineGradeDao.updateSyncStatus(offlineGrade.id, SyncStatus.SYNCING)
                    
                    // Convert to server grade format
                    val serverGrade = convertToServerGrade(offlineGrade)
                    
                    // Check for conflicts by trying to get existing grade
                    val existingGradeResult = gradeRepository.getGradeById(offlineGrade.id)
                    
                    if (existingGradeResult.isSuccess) {
                        val existingGrade = existingGradeResult.getOrNull()
                        if (existingGrade != null && existingGrade.dateRecorded > offlineGrade.dateRecorded) {
                            // Server has newer version - conflict
                            val conflictGrade = offlineGrade.copy(
                                syncStatus = SyncStatus.CONFLICT
                            )
                            offlineGradeDao.updateGrade(conflictGrade)
                            conflicts.add(conflictGrade)
                            continue
                        }
                    }
                    
                    // Create or update grade on server
                    val result = if (existingGradeResult.isSuccess && existingGradeResult.getOrNull() != null) {
                        gradeRepository.updateGrade(serverGrade)
                    } else {
                        gradeRepository.createGrade(serverGrade)
                    }
                    
                    if (result.isSuccess) {
                        // Mark as synced
                        offlineGradeDao.updateSyncStatus(offlineGrade.id, SyncStatus.SYNCED)
                        successCount++
                    } else {
                        // Mark as failed
                        offlineGradeDao.updateSyncStatus(offlineGrade.id, SyncStatus.FAILED)
                        failureCount++
                    }
                    
                } catch (e: Exception) {
                    offlineGradeDao.updateSyncStatus(offlineGrade.id, SyncStatus.FAILED)
                    failureCount++
                }
            }
            
            return SyncResult.PARTIAL_SUCCESS(
                successCount = successCount,
                failureCount = failureCount,
                conflictCount = conflicts.size,
                message = "Synced $successCount grades, $failureCount failed, ${conflicts.size} conflicts"
            )
            
        } catch (e: Exception) {
            return SyncResult.FAILED("Sync failed: ${e.message}")
        }
    }
    
    /**
     * Resolve conflicts by applying resolution strategy
     */
    suspend fun resolveConflicts(resolutions: List<ConflictResolution>): SyncResult {
        if (!networkMonitor.isCurrentlyConnected()) {
            return SyncResult.FAILED("No internet connection")
        }
        
        try {
            var successCount = 0
            var failureCount = 0
            
            for (resolution in resolutions) {
                try {
                    val offlineGrade = offlineGradeDao.getGradeByStudentSubjectAndPeriod(
                        resolution.studentId,
                        resolution.subjectId,
                        resolution.gradePeriod
                    ) ?: continue
                    
                    when (resolution.resolutionStrategy) {
                        ResolutionStrategy.USE_LOCAL -> {
                            // Use local version - sync to server
                            val serverGrade = convertToServerGrade(offlineGrade)
                            val result = gradeRepository.createGrade(serverGrade)
                            if (result.isSuccess) {
                                offlineGradeDao.updateSyncStatus(offlineGrade.id, SyncStatus.SYNCED)
                                successCount++
                            } else {
                                failureCount++
                            }
                        }
                        ResolutionStrategy.USE_SERVER -> {
                            // Use server version - update local
                            val serverGradeResult = gradeRepository.getGradeById(offlineGrade.id)
                            if (serverGradeResult.isSuccess) {
                                val serverGrade = serverGradeResult.getOrNull()
                                if (serverGrade != null) {
                                    val updatedOfflineGrade = convertFromServerGrade(serverGrade)
                                    offlineGradeDao.updateGrade(updatedOfflineGrade)
                                    offlineGradeDao.updateSyncStatus(offlineGrade.id, SyncStatus.SYNCED)
                                    successCount++
                                } else {
                                    failureCount++
                                }
                            } else {
                                failureCount++
                            }
                        }
                        ResolutionStrategy.MERGE -> {
                            // Merge both versions - use the latest
                            val serverGradeResult = gradeRepository.getGradeById(offlineGrade.id)
                            if (serverGradeResult.isSuccess) {
                                val serverGrade = serverGradeResult.getOrNull()
                                if (serverGrade != null) {
                                    val latestGrade = if (serverGrade.dateRecorded > offlineGrade.dateRecorded) {
                                        convertFromServerGrade(serverGrade)
                                    } else {
                                        offlineGrade
                                    }
                                    offlineGradeDao.updateGrade(latestGrade)
                                    offlineGradeDao.updateSyncStatus(offlineGrade.id, SyncStatus.SYNCED)
                                    successCount++
                                } else {
                                    failureCount++
                                }
                            } else {
                                failureCount++
                            }
                        }
                        else -> {
                            // Manual resolution - skip for now
                            failureCount++
                        }
                    }
                } catch (e: Exception) {
                    failureCount++
                }
            }
            
            return SyncResult.PARTIAL_SUCCESS(
                successCount = successCount,
                failureCount = failureCount,
                conflictCount = 0,
                message = "Resolved $successCount conflicts, $failureCount failed"
            )
            
        } catch (e: Exception) {
            return SyncResult.FAILED("Conflict resolution failed: ${e.message}")
        }
    }
    
    /**
     * Save grade offline when device is offline
     */
    suspend fun saveGradeOffline(grade: Grade): Result<OfflineGrade> {
        return try {
            val offlineGrade = convertFromServerGrade(grade)
            offlineGradeDao.insertGrade(offlineGrade)
            Result.success(offlineGrade)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get offline grades for a specific student and subject
     */
    fun getOfflineGrades(studentId: String, subjectId: String): Flow<List<OfflineGrade>> {
        return offlineGradeDao.getGradesByStudentAndSubject(studentId, subjectId)
    }
    
    /**
     * Get all offline grades
     */
    fun getAllOfflineGrades(): Flow<List<OfflineGrade>> {
        return offlineGradeDao.getGradesBySyncStatuses(
            listOf(SyncStatus.PENDING, SyncStatus.FAILED, SyncStatus.CONFLICT)
        )
    }
    
    private fun convertToServerGrade(offlineGrade: OfflineGrade): Grade {
        return Grade(
            id = offlineGrade.id,
            studentId = offlineGrade.studentId,
            studentName = offlineGrade.studentName,
            subjectId = offlineGrade.subjectId,
            subjectName = offlineGrade.subjectName,
            teacherId = offlineGrade.teacherId,
            gradePeriod = offlineGrade.gradePeriod,
            score = offlineGrade.score,
            maxScore = offlineGrade.maxScore,
            percentage = offlineGrade.percentage,
            letterGrade = offlineGrade.letterGrade,
            description = offlineGrade.description,
            dateRecorded = offlineGrade.dateRecorded,
            semester = offlineGrade.semester,
            academicYear = offlineGrade.academicYear
        )
    }
    
    private fun convertFromServerGrade(serverGrade: Grade): OfflineGrade {
        return OfflineGrade(
            id = serverGrade.id,
            studentId = serverGrade.studentId,
            studentName = serverGrade.studentName,
            subjectId = serverGrade.subjectId,
            subjectName = serverGrade.subjectName,
            teacherId = serverGrade.teacherId,
            gradePeriod = serverGrade.gradePeriod,
            score = serverGrade.score,
            maxScore = serverGrade.maxScore,
            percentage = serverGrade.percentage,
            letterGrade = serverGrade.letterGrade,
            description = serverGrade.description,
            dateRecorded = serverGrade.dateRecorded,
            semester = serverGrade.semester,
            academicYear = serverGrade.academicYear,
            syncStatus = SyncStatus.SYNCED
        )
    }
}

data class SyncStatusInfo(
    val isOnline: Boolean,
    val pendingCount: Int,
    val failedCount: Int,
    val conflictCount: Int,
    val totalPending: Int
)

sealed class SyncResult {
    data class SUCCESS(val message: String) : SyncResult()
    data class FAILED(val message: String) : SyncResult()
    data class PARTIAL_SUCCESS(
        val successCount: Int,
        val failureCount: Int,
        val conflictCount: Int,
        val message: String
    ) : SyncResult()
}

