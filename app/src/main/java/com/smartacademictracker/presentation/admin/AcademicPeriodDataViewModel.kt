package com.smartacademictracker.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.AcademicPeriod
import com.smartacademictracker.data.model.AcademicPeriodData
import com.smartacademictracker.data.model.AcademicPeriodSummary
import com.smartacademictracker.data.repository.AcademicPeriodDataRepository
import com.smartacademictracker.data.repository.AcademicPeriodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

@HiltViewModel
class AcademicPeriodDataViewModel @Inject constructor(
    private val academicPeriodDataRepository: AcademicPeriodDataRepository,
    private val academicPeriodRepository: AcademicPeriodRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AcademicPeriodDataUiState())
    val uiState: StateFlow<AcademicPeriodDataUiState> = _uiState.asStateFlow()

    private val _academicPeriods = MutableStateFlow<List<AcademicPeriod>>(emptyList())
    val academicPeriods: StateFlow<List<AcademicPeriod>> = _academicPeriods.asStateFlow()

    private val _periodSummaries = MutableStateFlow<List<AcademicPeriodSummary>>(emptyList())
    val periodSummaries: StateFlow<List<AcademicPeriodSummary>> = _periodSummaries.asStateFlow()

    private val _selectedPeriodData = MutableStateFlow<AcademicPeriodData?>(null)
    val selectedPeriodData: StateFlow<AcademicPeriodData?> = _selectedPeriodData.asStateFlow()

    fun loadAcademicPeriods() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val periodsResult = academicPeriodRepository.getAllAcademicPeriods()
                periodsResult.onSuccess { periods ->
                    _academicPeriods.value = periods
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    println("DEBUG: AcademicPeriodDataViewModel - Loaded ${periods.size} academic periods")
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

    fun loadPeriodSummaries() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val summariesResult = academicPeriodDataRepository.getAllAcademicPeriodSummaries()
                summariesResult.onSuccess { summaries ->
                    _periodSummaries.value = summaries
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    println("DEBUG: AcademicPeriodDataViewModel - Loaded ${summaries.size} period summaries")
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load period summaries"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load period summaries"
                )
            }
        }
    }

    fun loadAcademicPeriodsAndSummaries() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Load both in parallel for faster loading
                coroutineScope {
                    val periodsDeferred = async { academicPeriodRepository.getAllAcademicPeriods() }
                    val summariesDeferred = async { academicPeriodDataRepository.getAllAcademicPeriodSummaries() }
                    
                    val periodsResult = periodsDeferred.await()
                    val summariesResult = summariesDeferred.await()
                    
                    periodsResult.onSuccess { periods ->
                        _academicPeriods.value = periods
                        println("DEBUG: AcademicPeriodDataViewModel - Loaded ${periods.size} academic periods")
                    }.onFailure { exception ->
                        println("DEBUG: AcademicPeriodDataViewModel - Error loading periods: ${exception.message}")
                    }
                    
                    summariesResult.onSuccess { summaries ->
                        _periodSummaries.value = summaries
                        println("DEBUG: AcademicPeriodDataViewModel - Loaded ${summaries.size} period summaries")
                    }.onFailure { exception ->
                        println("DEBUG: AcademicPeriodDataViewModel - Error loading summaries: ${exception.message}")
                    }
                    
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load data"
                )
            }
        }
    }
    
    fun loadPeriodData(periodId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val dataResult = academicPeriodDataRepository.getAcademicPeriodData(periodId)
                dataResult.onSuccess { data ->
                    _selectedPeriodData.value = data
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    println("DEBUG: AcademicPeriodDataViewModel - Loaded data for period: ${data.academicPeriod?.name}")
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load period data"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load period data"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSelectedPeriodData() {
        _selectedPeriodData.value = null
    }
}

data class AcademicPeriodDataUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
