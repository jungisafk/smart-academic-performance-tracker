package com.smartacademictracker.data.local.dao

import androidx.room.*
import com.smartacademictracker.data.local.entity.OfflineUser
import com.smartacademictracker.data.local.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineUserDao {
    
    @Query("SELECT * FROM offline_users ORDER BY lastModified DESC")
    fun getAllUsers(): Flow<List<OfflineUser>>
    
    @Query("SELECT * FROM offline_users WHERE id = :userId")
    suspend fun getUserById(userId: String): OfflineUser?
    
    @Query("SELECT * FROM offline_users WHERE email = :email")
    suspend fun getUserByEmail(email: String): OfflineUser?
    
    @Query("SELECT * FROM offline_users WHERE role = :role ORDER BY lastModified DESC")
    fun getUsersByRole(role: String): Flow<List<OfflineUser>>
    
    @Query("SELECT * FROM offline_users WHERE syncStatus = :syncStatus ORDER BY lastModified ASC")
    fun getUsersBySyncStatus(syncStatus: SyncStatus): Flow<List<OfflineUser>>
    
    @Query("SELECT * FROM offline_users WHERE active = :active ORDER BY lastModified DESC")
    fun getUsersByActiveStatus(active: Boolean): Flow<List<OfflineUser>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: OfflineUser)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<OfflineUser>)
    
    @Update
    suspend fun updateUser(user: OfflineUser)
    
    @Update
    suspend fun updateUsers(users: List<OfflineUser>)
    
    @Delete
    suspend fun deleteUser(user: OfflineUser)
    
    @Query("DELETE FROM offline_users WHERE id = :userId")
    suspend fun deleteUserById(userId: String)
    
    @Query("UPDATE offline_users SET syncStatus = :syncStatus WHERE id = :userId")
    suspend fun updateSyncStatus(userId: String, syncStatus: SyncStatus)
    
    @Query("UPDATE offline_users SET syncStatus = :syncStatus WHERE id IN (:userIds)")
    suspend fun updateSyncStatusForUsers(userIds: List<String>, syncStatus: SyncStatus)
    
    @Query("SELECT COUNT(*) FROM offline_users WHERE syncStatus = :syncStatus")
    suspend fun getCountBySyncStatus(syncStatus: SyncStatus): Int
    
    @Query("SELECT COUNT(*) FROM offline_users WHERE role = :role")
    suspend fun getCountByRole(role: String): Int
}
