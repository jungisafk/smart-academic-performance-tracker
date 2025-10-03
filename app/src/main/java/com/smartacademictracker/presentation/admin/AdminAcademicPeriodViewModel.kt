package com.smartacademictracker.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.repository.AcademicPeriodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminAcademicPeriodViewModel @Inject constructor(
    private val academicPeriodRepository: AcademicPeriodRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminAcademicPeriodUiState())
    val uiState: StateFlow<AdminAcademicPeriodUiState> = _uiState.asStateFlow()

    private val _academicPeriods = MutableStateFlow<List<com.smartacademictracker.data.model.AcademicPeriod>>(emptyList())
    val academicPeriods: StateFlow<List<com.smartacademictracker.data.model.AcademicPeriod>> = _academicPeriods.asStateFlow()

    fun loadAcademicPeriods() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = academicPeriodRepository.getAllAcademicPeriods()
                result.onSuccess { periodsList ->
                    _academicPeriods.value = periodsList
                    
                    // Find current period
                    val currentPeriod = periodsList.find { it.isCurrent }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currentPeriod = currentPeriod?.name,
                        currentPeriodId = currentPeriod?.id
                    )
                    
                    println("DEBUG: AdminAcademicPeriodViewModel - Loaded ${periodsList.size} academic periods")
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load academic periods"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load academic periods"
                )
            }
        }
    }

    fun setCurrentPeriod(periodId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                processingPeriods = _uiState.value.processingPeriods + periodId,
                error = null
            )
            
            try {
                val result = academicPeriodRepository.setCurrentPeriod(periodId)
                result.onSuccess {
                    // Reload periods to reflect changes
                    loadAcademicPeriods()
                    _uiState.value = _uiState.value.copy(
                        processingPeriods = _uiState.value.processingPeriods - periodId
                    )
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        processingPeriods = _uiState.value.processingPeriods - periodId,
                        error = exception.message ?: "Failed to set current period"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    processingPeriods = _uiState.value.processingPeriods - periodId,
                    error = e.message ?: "Failed to set current period"
                )
            }
        }
    }

    fun createAcademicPeriod(
        name: String,
        semester: String,
        academicYear: String,
        startDate: Long,
        endDate: Long
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isCreating = true,
                error = null
            )
            
            try {
                val result = academicPeriodRepository.createAcademicPeriod(
                    name = name,
                    semester = semester,
                    academicYear = academicYear,
                    startDate = startDate,
                    endDate = endDate
                )
                result.onSuccess {
                    // Reload periods to reflect changes
                    loadAcademicPeriods()
                    _uiState.value = _uiState.value.copy(isCreating = false)
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isCreating = false,
                        error = exception.message ?: "Failed to create academic period"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isCreating = false,
                    error = e.message ?: "Failed to create academic period"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun refreshAcademicPeriods() {
        loadAcademicPeriods()
    }
}

data class AdminAcademicPeriodUiState(
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val error: String? = null,
    val currentPeriod: String? = null,
    val currentPeriodId: String? = null,
    val processingPeriods: Set<String> = emptySet()
)
