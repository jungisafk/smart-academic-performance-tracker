package com.smartacademictracker.data.local.dao

import androidx.room.*
import com.smartacademictracker.data.local.entity.OfflineGrade
import com.smartacademictracker.data.local.entity.SyncStatus
import com.smartacademictracker.data.model.GradePeriod
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineGradeDao {
    
    @Query("SELECT * FROM offline_grades WHERE studentId = :studentId ORDER BY lastModified DESC")
    fun getGradesByStudent(studentId: String): Flow<List<OfflineGrade>>
    
    @Query("SELECT * FROM offline_grades WHERE subjectId = :subjectId ORDER BY lastModified DESC")
    fun getGradesBySubject(subjectId: String): Flow<List<OfflineGrade>>
    
    @Query("SELECT * FROM offline_grades WHERE studentId = :studentId AND subjectId = :subjectId ORDER BY lastModified DESC")
    fun getGradesByStudentAndSubject(studentId: String, subjectId: String): Flow<List<OfflineGrade>>
    
    @Query("SELECT * FROM offline_grades WHERE studentId = :studentId AND subjectId = :subjectId AND gradePeriod = :gradePeriod LIMIT 1")
    suspend fun getGradeByStudentSubjectAndPeriod(
        studentId: String, 
        subjectId: String, 
        gradePeriod: GradePeriod
    ): OfflineGrade?
    
    @Query("SELECT * FROM offline_grades WHERE syncStatus = :syncStatus ORDER BY lastModified ASC")
    fun getGradesBySyncStatus(syncStatus: SyncStatus): Flow<List<OfflineGrade>>
    
    @Query("SELECT * FROM offline_grades WHERE syncStatus IN (:syncStatuses) ORDER BY lastModified ASC")
    fun getGradesBySyncStatuses(syncStatuses: List<SyncStatus>): Flow<List<OfflineGrade>>
    
    @Query("SELECT * FROM offline_grades WHERE teacherId = :teacherId ORDER BY lastModified DESC")
    fun getGradesByTeacher(teacherId: String): Flow<List<OfflineGrade>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrade(grade: OfflineGrade)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrades(grades: List<OfflineGrade>)
    
    @Update
    suspend fun updateGrade(grade: OfflineGrade)
    
    @Update
    suspend fun updateGrades(grades: List<OfflineGrade>)
    
    @Delete
    suspend fun deleteGrade(grade: OfflineGrade)
    
    @Query("DELETE FROM offline_grades WHERE id = :gradeId")
    suspend fun deleteGradeById(gradeId: String)
    
    @Query("DELETE FROM offline_grades WHERE studentId = :studentId AND subjectId = :subjectId")
    suspend fun deleteGradesByStudentAndSubject(studentId: String, subjectId: String)
    
    @Query("UPDATE offline_grades SET syncStatus = :syncStatus WHERE id = :gradeId")
    suspend fun updateSyncStatus(gradeId: String, syncStatus: SyncStatus)
    
    @Query("UPDATE offline_grades SET syncStatus = :syncStatus WHERE id IN (:gradeIds)")
    suspend fun updateSyncStatusForGrades(gradeIds: List<String>, syncStatus: SyncStatus)
    
    @Query("SELECT COUNT(*) FROM offline_grades WHERE syncStatus = :syncStatus")
    suspend fun getCountBySyncStatus(syncStatus: SyncStatus): Int
    
    @Query("SELECT COUNT(*) FROM offline_grades WHERE syncStatus = :syncStatus AND teacherId = :teacherId")
    suspend fun getCountBySyncStatusAndTeacher(syncStatus: SyncStatus, teacherId: String): Int
    
    // @Query("SELECT * FROM offline_grades WHERE conflictResolution IS NOT NULL")
    // fun getGradesWithConflicts(): Flow<List<OfflineGrade>> // Temporarily disabled
    
    @Query("SELECT * FROM offline_grades WHERE lastModified >= :since ORDER BY lastModified DESC")
    fun getGradesModifiedSince(since: Long): Flow<List<OfflineGrade>>
    
    @Query("DELETE FROM offline_grades WHERE syncStatus = :syncStatus AND lastModified < :before")
    suspend fun deleteOldSyncedGrades(syncStatus: SyncStatus, before: Long)
}
