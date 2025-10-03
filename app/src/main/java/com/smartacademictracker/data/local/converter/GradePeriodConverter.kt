package com.smartacademictracker.data.local.converter

import androidx.room.TypeConverter
import com.smartacademictracker.data.model.GradePeriod

class GradePeriodConverter {
    
    @TypeConverter
    fun fromGradePeriod(gradePeriod: GradePeriod): String {
        return gradePeriod.name
    }
    
    @TypeConverter
    fun toGradePeriod(gradePeriodString: String): GradePeriod {
        return GradePeriod.valueOf(gradePeriodString)
    }
}
