package com.smartacademictracker.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.smartacademictracker.data.local.dao.*
import com.smartacademictracker.data.local.entity.*
import com.smartacademictracker.data.local.converter.GradePeriodConverter
import com.smartacademictracker.data.local.converter.SyncStatusConverter
import com.smartacademictracker.data.local.converter.ConflictResolutionConverter

@Database(
    entities = [
        OfflineGrade::class,
        OfflineUser::class,
        OfflineSubject::class,
        OfflineEnrollment::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(
    GradePeriodConverter::class,
    SyncStatusConverter::class
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun offlineGradeDao(): OfflineGradeDao
    abstract fun offlineUserDao(): OfflineUserDao
    abstract fun offlineSubjectDao(): OfflineSubjectDao
    abstract fun offlineEnrollmentDao(): OfflineEnrollmentDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_academic_tracker_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
