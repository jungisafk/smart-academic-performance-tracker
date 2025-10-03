package com.smartacademictracker.data.local.dao

import androidx.room.*
import com.smartacademictracker.data.local.entity.OfflineEnrollment
import com.smartacademictracker.data.local.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineEnrollmentDao {
    
    @Query("SELECT * FROM offline_enrollments ORDER BY lastModified DESC")
    fun getAllEnrollments(): Flow<List<OfflineEnrollment>>
    
    @Query("SELECT * FROM offline_enrollments WHERE id = :enrollmentId")
    suspend fun getEnrollmentById(enrollmentId: String): OfflineEnrollment?
    
    @Query("SELECT * FROM offline_enrollments WHERE studentId = :studentId ORDER BY lastModified DESC")
    fun getEnrollmentsByStudent(studentId: String): Flow<List<OfflineEnrollment>>
    
    @Query("SELECT * FROM offline_enrollments WHERE subjectId = :subjectId ORDER BY lastModified DESC")
    fun getEnrollmentsBySubject(subjectId: String): Flow<List<OfflineEnrollment>>
    
    @Query("SELECT * FROM offline_enrollments WHERE teacherId = :teacherId ORDER BY lastModified DESC")
    fun getEnrollmentsByTeacher(teacherId: String): Flow<List<OfflineEnrollment>>
    
    @Query("SELECT * FROM offline_enrollments WHERE studentId = :studentId AND subjectId = :subjectId")
    suspend fun getEnrollmentByStudentAndSubject(studentId: String, subjectId: String): OfflineEnrollment?
    
    @Query("SELECT * FROM offline_enrollments WHERE active = :active ORDER BY lastModified DESC")
    fun getEnrollmentsByActiveStatus(active: Boolean): Flow<List<OfflineEnrollment>>
    
    @Query("SELECT * FROM offline_enrollments WHERE syncStatus = :syncStatus ORDER BY lastModified ASC")
    fun getEnrollmentsBySyncStatus(syncStatus: SyncStatus): Flow<List<OfflineEnrollment>>
    
    @Query("SELECT * FROM offline_enrollments WHERE semester = :semester AND academicYear = :academicYear ORDER BY lastModified DESC")
    fun getEnrollmentsBySemesterAndYear(semester: String, academicYear: String): Flow<List<OfflineEnrollment>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEnrollment(enrollment: OfflineEnrollment)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEnrollments(enrollments: List<OfflineEnrollment>)
    
    @Update
    suspend fun updateEnrollment(enrollment: OfflineEnrollment)
    
    @Update
    suspend fun updateEnrollments(enrollments: List<OfflineEnrollment>)
    
    @Delete
    suspend fun deleteEnrollment(enrollment: OfflineEnrollment)
    
    @Query("DELETE FROM offline_enrollments WHERE id = :enrollmentId")
    suspend fun deleteEnrollmentById(enrollmentId: String)
    
    @Query("DELETE FROM offline_enrollments WHERE studentId = :studentId AND subjectId = :subjectId")
    suspend fun deleteEnrollmentByStudentAndSubject(studentId: String, subjectId: String)
    
    @Query("UPDATE offline_enrollments SET syncStatus = :syncStatus WHERE id = :enrollmentId")
    suspend fun updateSyncStatus(enrollmentId: String, syncStatus: SyncStatus)
    
    @Query("UPDATE offline_enrollments SET syncStatus = :syncStatus WHERE id IN (:enrollmentIds)")
    suspend fun updateSyncStatusForEnrollments(enrollmentIds: List<String>, syncStatus: SyncStatus)
    
    @Query("SELECT COUNT(*) FROM offline_enrollments WHERE syncStatus = :syncStatus")
    suspend fun getCountBySyncStatus(syncStatus: SyncStatus): Int
    
    @Query("SELECT COUNT(*) FROM offline_enrollments WHERE active = :active")
    suspend fun getCountByActiveStatus(active: Boolean): Int
}
