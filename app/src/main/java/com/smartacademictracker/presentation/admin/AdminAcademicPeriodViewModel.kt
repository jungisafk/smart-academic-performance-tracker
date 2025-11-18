package com.smartacademictracker.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.AcademicPeriod
import com.smartacademictracker.data.model.AcademicPeriodOverview
import com.smartacademictracker.data.repository.AcademicPeriodRepository
import com.smartacademictracker.data.service.AcademicPeriodFilterService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminAcademicPeriodViewModel @Inject constructor(
    private val academicPeriodRepository: AcademicPeriodRepository,
    private val academicPeriodFilterService: AcademicPeriodFilterService,
    private val yearLevelProgressionService: com.smartacademictracker.data.service.YearLevelProgressionService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminAcademicPeriodUiState())
    val uiState: StateFlow<AdminAcademicPeriodUiState> = _uiState.asStateFlow()

    private val _academicPeriods = MutableStateFlow<List<AcademicPeriod>>(emptyList())
    val academicPeriods: StateFlow<List<AcademicPeriod>> = _academicPeriods.asStateFlow()

    private val _activePeriod = MutableStateFlow<AcademicPeriod?>(null)
    val activePeriod: StateFlow<AcademicPeriod?> = _activePeriod.asStateFlow()

    private val _summary = MutableStateFlow(AcademicPeriodOverview())
    val summary: StateFlow<AcademicPeriodOverview> = _summary.asStateFlow()

    fun loadAcademicPeriods() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // First, ensure only one active period exists (cleanup any inconsistencies)
                academicPeriodRepository.ensureSingleActivePeriod()
                
                // Load all academic periods
                academicPeriodRepository.getAllAcademicPeriods().onSuccess { periods ->
                    _academicPeriods.value = periods
                    
                }.onFailure { exception ->
                    
                    if (exception.message?.contains("PERMISSION_DENIED") == true) {
                        _uiState.value = _uiState.value.copy(
                            error = "Permission denied. Please check Firestore security rules for academic_periods collection."
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to load academic periods: ${exception.message}"
                        )
                    }
                }

                // Load active period
                academicPeriodRepository.getActiveAcademicPeriod().onSuccess { activePeriod ->
                    _activePeriod.value = activePeriod
                    
                }.onFailure { exception ->
                    
                }

                // Load summary
                academicPeriodRepository.getAcademicPeriodSummary().onSuccess { summary ->
                    _summary.value = summary
                    
                }.onFailure { exception ->
                    
                }

                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load data: ${e.message}"
                )
            }
        }
    }

    fun setActivePeriod(periodId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Get the previous active period to check if this is a new period
                val previousActivePeriod = _activePeriod.value
                
                academicPeriodRepository.setActivePeriod(periodId).onSuccess {
                    // Refresh the academic period filter service to update all subject queries
                    academicPeriodFilterService.refreshActiveAcademicPeriod()
                    
                    // Get the new period to check if we should trigger progression
                    val newPeriodResult = academicPeriodRepository.getAcademicPeriodById(periodId)
                    val newPeriod = newPeriodResult.getOrNull()
                    
                    // Process year level progression ONLY when:
                    // 1. There was a previous active period (not first time setup)
                    // 2. Previous period was SUMMER_CLASS
                    // 3. New period is FIRST_SEMESTER
                    // 4. New period's academic year is different (new academic year)
                    val shouldProceed = previousActivePeriod != null && 
                                       previousActivePeriod.id != periodId &&
                                       newPeriod != null &&
                                       previousActivePeriod.semester == com.smartacademictracker.data.model.Semester.SUMMER_CLASS &&
                                       newPeriod.semester == com.smartacademictracker.data.model.Semester.FIRST_SEMESTER &&
                                       previousActivePeriod.academicYear != newPeriod.academicYear
                    
                    if (shouldProceed) {
                        android.util.Log.d("AdminAcademicPeriodViewModel", "Transitioning from ${previousActivePeriod.academicYear} ${previousActivePeriod.semester.displayName} to ${newPeriod.academicYear} ${newPeriod.semester.displayName} - processing year level progression")
                        val progressionResult = yearLevelProgressionService.processYearLevelProgression(periodId)
                        progressionResult.onSuccess { result ->
                            android.util.Log.d("AdminAcademicPeriodViewModel", "Year level progression completed: ${result.getSummaryMessage()}")
                            // Reload data after setting active period
                            loadAcademicPeriods()
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                successMessage = "Academic period activated successfully. ${result.advanced} student(s) advanced to next year level. Subjects will now show for this semester."
                            )
                        }.onFailure { exception ->
                            android.util.Log.e("AdminAcademicPeriodViewModel", "Year level progression failed: ${exception.message}", exception)
                            // Still reload data even if progression failed
                            loadAcademicPeriods()
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                successMessage = "Academic period activated successfully, but year level progression encountered issues: ${exception.message}. Subjects will now show for this semester."
                            )
                        }
                    } else {
                        // Not a progression trigger point - just reload
                        if (previousActivePeriod != null && newPeriod != null) {
                            android.util.Log.d("AdminAcademicPeriodViewModel", "Academic period changed but not a progression trigger point. Previous: ${previousActivePeriod.academicYear} ${previousActivePeriod.semester.displayName}, New: ${newPeriod.academicYear} ${newPeriod.semester.displayName}")
                        }
                        loadAcademicPeriods()
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            successMessage = "Academic period activated successfully. Subjects will now show for this semester."
                        )
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to activate period: ${exception.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to activate period: ${e.message}"
                )
            }
        }
    }

    fun deleteAcademicPeriod(periodId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                academicPeriodRepository.deleteAcademicPeriod(periodId).onSuccess {
                    // Reload data after deletion
                    loadAcademicPeriods()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Academic period deleted successfully"
                    )
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to delete period: ${exception.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to delete period: ${e.message}"
                )
            }
        }
    }

    fun refreshData() {
        loadAcademicPeriods()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
    
    fun updateAcademicPeriod(
        periodId: String,
        name: String,
        description: String,
        isActive: Boolean
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Get the current period
                val periodResult = academicPeriodRepository.getAcademicPeriodById(periodId)
                val currentPeriod = periodResult.getOrNull()
                    ?: run {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Academic period not found"
                        )
                        return@launch
                    }
                
                // Update only the allowed fields: name, description, and isActive
                val updatedPeriod = currentPeriod.copy(
                    name = name.trim(),
                    description = description.trim(),
                    isActive = isActive
                )
                
                academicPeriodRepository.updateAcademicPeriod(updatedPeriod).onSuccess {
                    // If setting as active, refresh the filter service
                    if (isActive) {
                        academicPeriodFilterService.refreshActiveAcademicPeriod()
                    }
                    
                    // Reload data after update
                    loadAcademicPeriods()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Academic period updated successfully"
                    )
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to update period: ${exception.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to update period: ${e.message}"
                )
            }
        }
    }
}

data class AdminAcademicPeriodUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
