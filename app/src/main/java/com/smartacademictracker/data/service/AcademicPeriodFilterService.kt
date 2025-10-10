package com.smartacademictracker.data.service

import com.smartacademictracker.data.model.AcademicPeriod
import com.smartacademictracker.data.repository.AcademicPeriodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service to manage academic period filtering across all data operations
 * Ensures all academic data is filtered by the current active academic period
 */
@Singleton
class AcademicPeriodFilterService @Inject constructor(
    private val academicPeriodRepository: AcademicPeriodRepository
) {
    private val _activeAcademicPeriod = MutableStateFlow<AcademicPeriod?>(null)
    val activeAcademicPeriod: StateFlow<AcademicPeriod?> = _activeAcademicPeriod.asStateFlow()

    suspend fun getActiveAcademicPeriod(): AcademicPeriod? {
        return try {
            val result = academicPeriodRepository.getActiveAcademicPeriod()
            result.onSuccess { period ->
                _activeAcademicPeriod.value = period
                println("DEBUG: AcademicPeriodFilterService - Active period: ${period?.name ?: "None"}")
                return period
            }.onFailure { exception ->
                println("DEBUG: AcademicPeriodFilterService - Error loading active period: ${exception.message}")
                return null
            }
            null
        } catch (e: Exception) {
            println("DEBUG: AcademicPeriodFilterService - Exception loading active period: ${e.message}")
            null
        }
    }

    suspend fun refreshActiveAcademicPeriod() {
        getActiveAcademicPeriod()
    }

    /**
     * Get the current active academic period ID
     * Returns empty string if no active period is found
     */
    suspend fun getActiveAcademicPeriodId(): String {
        val period = getActiveAcademicPeriod()
        return period?.id ?: ""
    }

    /**
     * Check if there's an active academic period
     */
    suspend fun hasActiveAcademicPeriod(): Boolean {
        return getActiveAcademicPeriod() != null
    }

    /**
     * Get academic period context for data operations
     */
    suspend fun getAcademicPeriodContext(): AcademicPeriodContext {
        val activePeriod = getActiveAcademicPeriod()
        return AcademicPeriodContext(
            periodId = activePeriod?.id ?: "",
            academicYear = activePeriod?.academicYear ?: "",
            semester = activePeriod?.semester?.displayName ?: "",
            isActive = activePeriod != null
        )
    }
}

data class AcademicPeriodContext(
    val periodId: String,
    val academicYear: String,
    val semester: String,
    val isActive: Boolean
)
