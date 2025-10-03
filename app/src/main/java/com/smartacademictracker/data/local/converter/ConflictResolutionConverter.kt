package com.smartacademictracker.data.local.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.smartacademictracker.data.local.entity.ConflictResolution

class ConflictResolutionConverter {
    
    private val gson = Gson()
    
    @TypeConverter
    fun fromConflictResolution(conflictResolution: ConflictResolution?): String? {
        return conflictResolution?.let { gson.toJson(it) }
    }
    
    @TypeConverter
    fun toConflictResolution(conflictResolutionString: String?): ConflictResolution? {
        return conflictResolutionString?.let {
            gson.fromJson(it, ConflictResolution::class.java)
        }
    }
}
