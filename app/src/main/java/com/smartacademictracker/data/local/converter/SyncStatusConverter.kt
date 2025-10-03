package com.smartacademictracker.data.local.converter

import androidx.room.TypeConverter
import com.smartacademictracker.data.local.entity.SyncStatus

class SyncStatusConverter {
    
    @TypeConverter
    fun fromSyncStatus(syncStatus: SyncStatus): String {
        return syncStatus.name
    }
    
    @TypeConverter
    fun toSyncStatus(syncStatusString: String): SyncStatus {
        return SyncStatus.valueOf(syncStatusString)
    }
}
