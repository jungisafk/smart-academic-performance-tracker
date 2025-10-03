package com.smartacademictracker.data.repository

import com.smartacademictracker.data.local.database.AppDatabase
import com.smartacademictracker.data.local.entity.OfflineGrade
import com.smartacademictracker.data.local.entity.SyncStatus
import com.smartacademictracker.data.model.Grade
import com.smartacademictracker.data.model.GradePeriod
import com.smartacademictracker.data.network.NetworkMonitor
import com.smartacademictracker.data.sync.GradeSyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineGradeRepository @Inject constructor(
    private val database: AppDatabase,
    private val gradeRepository: GradeRepository,
    private val networkMonitor: NetworkMonitor,
    private val gradeSyncManager: GradeSyncManager
) {
    
    private val offlineGradeDao = database.offlineGradeDao()
    
    /**
     * Create grade - saves locally if offline, syncs if online
     */
    suspend fun createGrade(grade: Grade): Result<Grade> {
        return try {
            if (networkMonitor.isCurrentlyConnected()) {
                // Online - save to server first
                val result = gradeRepository.createGrade(grade)
                if (result.isSuccess) {
                    // Also save locally for offline access
                    val offlineGrade = convertToOfflineGrade(grade, SyncStatus.SYNCED)
                    offlineGradeDao.insertGrade(offlineGrade)
                    result
                } else {
                    // Server failed - save locally for later sync
                    val offlineGrade = convertToOfflineGrade(grade, SyncStatus.PENDING)
                    offlineGradeDao.insertGrade(offlineGrade)
                    Result.success(grade)
                }
            } else {
                // Offline - save locally only
                val offlineGrade = convertToOfflineGrade(grade, SyncStatus.PENDING)
                offlineGradeDao.insertGrade(offlineGrade)
                Result.success(grade)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update grade - updates locally if offline, syncs if online
     */
    suspend fun updateGrade(grade: Grade): Result<Grade> {
        return try {
            if (networkMonitor.isCurrentlyConnected()) {
                // Online - update server first
                val result = gradeRepository.updateGrade(grade)
                if (result.isSuccess) {
                    // Also update locally
                    val offlineGrade = convertToOfflineGrade(grade, SyncStatus.SYNCED)
                    offlineGradeDao.updateGrade(offlineGrade)
                    Result.success(grade)
                } else {
                    // Server failed - update locally for later sync
                    val offlineGrade = convertToOfflineGrade(grade, SyncStatus.PENDING)
                    offlineGradeDao.updateGrade(offlineGrade)
                    Result.success(grade)
                }
            } else {
                // Offline - update locally only
                val offlineGrade = convertToOfflineGrade(grade, SyncStatus.PENDING)
                offlineGradeDao.updateGrade(offlineGrade)
                Result.success(grade)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get grades for student - combines local and remote data
     */
    fun getGradesByStudent(studentId: String): Flow<List<Grade>> {
        return combine(
            offlineGradeDao.getGradesByStudent(studentId),
            networkMonitor.isOnline()
        ) { offlineGrades, isOnline ->
            if (isOnline) {
                // Online - get from server and merge with local
                val serverGradesResult = gradeRepository.getGradesByStudent(studentId)
                if (serverGradesResult.isSuccess) {
                    val serverGrades = serverGradesResult.getOrNull() ?: emptyList()
                    mergeGrades(serverGrades, offlineGrades)
                } else {
                    // Server failed - use local only
                    offlineGrades.map { convertToServerGrade(it) }
                }
            } else {
                // Offline - use local only
                offlineGrades.map { convertToServerGrade(it) }
            }
        }
    }
    
    /**
     * Get grades for subject - combines local and remote data
     */
    fun getGradesBySubject(subjectId: String): Flow<List<Grade>> {
        return combine(
            offlineGradeDao.getGradesBySubject(subjectId),
            networkMonitor.isOnline()
        ) { offlineGrades, isOnline ->
            if (isOnline) {
                // Online - get from server and merge with local
                val serverGradesResult = gradeRepository.getGradesBySubject(subjectId)
                if (serverGradesResult.isSuccess) {
                    val serverGrades = serverGradesResult.getOrNull() ?: emptyList()
                    mergeGrades(serverGrades, offlineGrades)
                } else {
                    // Server failed - use local only
                    offlineGrades.map { convertToServerGrade(it) }
                }
            } else {
                // Offline - use local only
                offlineGrades.map { convertToServerGrade(it) }
            }
        }
    }
    
    /**
     * Get grades for student and subject - combines local and remote data
     */
    fun getGradesByStudentAndSubject(studentId: String, subjectId: String): Flow<List<Grade>> {
        return combine(
            offlineGradeDao.getGradesByStudentAndSubject(studentId, subjectId),
            networkMonitor.isOnline()
        ) { offlineGrades, isOnline ->
            if (isOnline) {
                // Online - get from server and merge with local
                val serverGradesResult = gradeRepository.getGradesByStudentAndSubject(studentId, subjectId)
                if (serverGradesResult.isSuccess) {
                    val serverGrades = serverGradesResult.getOrNull() ?: emptyList()
                    mergeGrades(serverGrades, offlineGrades)
                } else {
                    // Server failed - use local only
                    offlineGrades.map { convertToServerGrade(it) }
                }
            } else {
                // Offline - use local only
                offlineGrades.map { convertToServerGrade(it) }
            }
        }
    }
    
    /**
     * Get all grades - combines local and remote data
     */
    fun getAllGrades(): Flow<List<Grade>> {
        return combine(
            offlineGradeDao.getGradesBySyncStatuses(
                listOf(SyncStatus.PENDING, SyncStatus.FAILED, SyncStatus.CONFLICT)
            ),
            networkMonitor.isOnline()
        ) { offlineGrades, isOnline ->
            if (isOnline) {
                // Online - get from server and merge with local
                val serverGradesResult = gradeRepository.getAllGrades()
                if (serverGradesResult.isSuccess) {
                    val serverGrades = serverGradesResult.getOrNull() ?: emptyList()
                    mergeGrades(serverGrades, offlineGrades)
                } else {
                    // Server failed - use local only
                    offlineGrades.map { convertToServerGrade(it) }
                }
            } else {
                // Offline - use local only
                offlineGrades.map { convertToServerGrade(it) }
            }
        }
    }
    
    /**
     * Get sync status
     */
    fun getSyncStatus(): Flow<com.smartacademictracker.data.sync.SyncStatusInfo> {
        return gradeSyncManager.getSyncStatus()
    }
    
    /**
     * Manually trigger sync
     */
    suspend fun syncGrades(): com.smartacademictracker.data.sync.SyncResult {
        return gradeSyncManager.syncPendingGrades()
    }
    
    /**
     * Get grades with conflicts - temporarily disabled
     */
    // fun getGradesWithConflicts(): Flow<List<Grade>> {
    //     return offlineGradeDao.getGradesWithConflicts().map { offlineGrades ->
    //         offlineGrades.map { convertToServerGrade(it) }
    //     }
    // }
    
    private fun convertToOfflineGrade(grade: Grade, syncStatus: SyncStatus): OfflineGrade {
        return OfflineGrade(
            id = grade.id,
            studentId = grade.studentId,
            studentName = grade.studentName,
            subjectId = grade.subjectId,
            subjectName = grade.subjectName,
            teacherId = grade.teacherId,
            gradePeriod = grade.gradePeriod,
            score = grade.score,
            maxScore = grade.maxScore,
            percentage = grade.percentage,
            letterGrade = grade.letterGrade,
            description = grade.description,
            dateRecorded = grade.dateRecorded,
            semester = grade.semester,
            academicYear = grade.academicYear,
            syncStatus = syncStatus
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
    
    private fun mergeGrades(serverGrades: List<Grade>, offlineGrades: List<OfflineGrade>): List<Grade> {
        val serverGradeMap = serverGrades.associateBy { "${it.studentId}_${it.subjectId}_${it.gradePeriod}" }
        val offlineGradeMap = offlineGrades.associateBy { "${it.studentId}_${it.subjectId}_${it.gradePeriod}" }
        
        val mergedGrades = mutableListOf<Grade>()
        
        // Add all server grades
        mergedGrades.addAll(serverGrades)
        
        // Add offline grades that don't exist on server or are newer
        offlineGrades.forEach { offlineGrade ->
            val key = "${offlineGrade.studentId}_${offlineGrade.subjectId}_${offlineGrade.gradePeriod}"
            val serverGrade = serverGradeMap[key]
            
            if (serverGrade == null || offlineGrade.dateRecorded > serverGrade.dateRecorded) {
                mergedGrades.add(convertToServerGrade(offlineGrade))
            }
        }
        
        return mergedGrades.distinctBy { "${it.studentId}_${it.subjectId}_${it.gradePeriod}" }
    }
}
