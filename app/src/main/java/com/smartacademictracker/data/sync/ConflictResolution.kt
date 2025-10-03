package com.smartacademictracker.data.sync

import com.smartacademictracker.data.model.GradePeriod
import com.smartacademictracker.data.local.entity.ResolutionStrategy

data class ConflictResolution(
    val studentId: String,
    val subjectId: String,
    val gradePeriod: GradePeriod,
    val resolutionStrategy: ResolutionStrategy
)
