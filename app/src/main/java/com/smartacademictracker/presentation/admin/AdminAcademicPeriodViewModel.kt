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
    private val academicPeriodFilterService: AcademicPeriodFilterService
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
                    println("DEBUG: AdminAcademicPeriodViewModel - Loaded ${periods.size} academic periods")
                }.onFailure { exception ->
                    println("DEBUG: AdminAcademicPeriodViewModel - Error loading academic periods: ${exception.message}")
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
                    println("DEBUG: AdminAcademicPeriodViewModel - Active period: ${activePeriod?.name ?: "None"}")
                }.onFailure { exception ->
                    println("DEBUG: AdminAcademicPeriodViewModel - Error loading active period: ${exception.message}")
                }

                // Load summary
                academicPeriodRepository.getAcademicPeriodSummary().onSuccess { summary ->
                    _summary.value = summary
                    println("DEBUG: AdminAcademicPeriodViewModel - Summary: ${summary.totalPeriods} periods, active: ${summary.activePeriod?.name}")
                }.onFailure { exception ->
                    println("DEBUG: AdminAcademicPeriodViewModel - Error loading summary: ${exception.message}")
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
                academicPeriodRepository.setActivePeriod(periodId).onSuccess {
                    // Refresh the academic period filter service to update all subject queries
                    academicPeriodFilterService.refreshActiveAcademicPeriod()
                    
                    // Reload data after setting active period
                    loadAcademicPeriods()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Academic period activated successfully. Subjects will now show for this semester."
                    )
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
}

data class AdminAcademicPeriodUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)