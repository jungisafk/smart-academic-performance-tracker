package com.smartacademictracker.data.local.dao

import androidx.room.*
import com.smartacademictracker.data.local.entity.OfflineSubject
import com.smartacademictracker.data.local.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineSubjectDao {
    
    @Query("SELECT * FROM offline_subjects ORDER BY lastModified DESC")
    fun getAllSubjects(): Flow<List<OfflineSubject>>
    
    @Query("SELECT * FROM offline_subjects WHERE id = :subjectId")
    suspend fun getSubjectById(subjectId: String): OfflineSubject?
    
    @Query("SELECT * FROM offline_subjects WHERE code = :code")
    suspend fun getSubjectByCode(code: String): OfflineSubject?
    
    @Query("SELECT * FROM offline_subjects WHERE teacherId = :teacherId ORDER BY lastModified DESC")
    fun getSubjectsByTeacher(teacherId: String): Flow<List<OfflineSubject>>
    
    @Query("SELECT * FROM offline_subjects WHERE active = :active ORDER BY lastModified DESC")
    fun getSubjectsByActiveStatus(active: Boolean): Flow<List<OfflineSubject>>
    
    @Query("SELECT * FROM offline_subjects WHERE syncStatus = :syncStatus ORDER BY lastModified ASC")
    fun getSubjectsBySyncStatus(syncStatus: SyncStatus): Flow<List<OfflineSubject>>
    
    @Query("SELECT * FROM offline_subjects WHERE yearLevelId = :yearLevelId ORDER BY lastModified DESC")
    fun getSubjectsByYearLevel(yearLevelId: String): Flow<List<OfflineSubject>>
    
    @Query("SELECT * FROM offline_subjects WHERE courseId = :courseId ORDER BY lastModified DESC")
    fun getSubjectsByCourse(courseId: String): Flow<List<OfflineSubject>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: OfflineSubject)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<OfflineSubject>)
    
    @Update
    suspend fun updateSubject(subject: OfflineSubject)
    
    @Update
    suspend fun updateSubjects(subjects: List<OfflineSubject>)
    
    @Delete
    suspend fun deleteSubject(subject: OfflineSubject)
    
    @Query("DELETE FROM offline_subjects WHERE id = :subjectId")
    suspend fun deleteSubjectById(subjectId: String)
    
    @Query("UPDATE offline_subjects SET syncStatus = :syncStatus WHERE id = :subjectId")
    suspend fun updateSyncStatus(subjectId: String, syncStatus: SyncStatus)
    
    @Query("UPDATE offline_subjects SET syncStatus = :syncStatus WHERE id IN (:subjectIds)")
    suspend fun updateSyncStatusForSubjects(subjectIds: List<String>, syncStatus: SyncStatus)
    
    @Query("SELECT COUNT(*) FROM offline_subjects WHERE syncStatus = :syncStatus")
    suspend fun getCountBySyncStatus(syncStatus: SyncStatus): Int
    
    @Query("SELECT COUNT(*) FROM offline_subjects WHERE active = :active")
    suspend fun getCountByActiveStatus(active: Boolean): Int
}
