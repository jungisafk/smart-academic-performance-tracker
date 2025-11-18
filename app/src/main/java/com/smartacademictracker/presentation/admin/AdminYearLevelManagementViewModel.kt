package com.smartacademictracker.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.YearLevel
import com.smartacademictracker.data.repository.YearLevelRepository
import com.smartacademictracker.data.manager.AdminDataCache
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminYearLevelManagementViewModel @Inject constructor(
    private val yearLevelRepository: YearLevelRepository,
    private val adminDataCache: AdminDataCache
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminYearLevelManagementUiState())
    val uiState: StateFlow<AdminYearLevelManagementUiState> = _uiState.asStateFlow()

    private val _yearLevels = MutableStateFlow<List<YearLevel>>(emptyList())
    val yearLevels: StateFlow<List<YearLevel>> = _yearLevels.asStateFlow()

    init {
        // Load cached data immediately if available
        val cachedYearLevels = adminDataCache.cachedYearLevels.value
        if (cachedYearLevels.isNotEmpty() && adminDataCache.isCacheValid()) {
            _yearLevels.value = cachedYearLevels
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun loadYearLevels(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            // Load cached data first if available and not forcing refresh
            if (!forceRefresh && adminDataCache.cachedYearLevels.value.isNotEmpty() && adminDataCache.isCacheValid()) {
                _yearLevels.value = adminDataCache.cachedYearLevels.value
                _uiState.value = _uiState.value.copy(isLoading = false)
            } else {
                // Only show loading if we don't have cached data
                if (adminDataCache.cachedYearLevels.value.isEmpty()) {
                    _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                }
            }
            
            try {
                val yearLevelsResult = yearLevelRepository.getAllYearLevels()
                yearLevelsResult.onSuccess { yearLevelsList ->
                    _yearLevels.value = yearLevelsList
                    adminDataCache.updateYearLevels(yearLevelsList)
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load year levels"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load year levels"
                )
            }
        }
    }

    fun deleteYearLevel(yearLevelId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                deletingYearLevels = _uiState.value.deletingYearLevels + yearLevelId,
                error = null
            )
            
            try {
                val deleteResult = yearLevelRepository.deleteYearLevel(yearLevelId)
                deleteResult.onSuccess {
                    _uiState.value = _uiState.value.copy(
                        deletingYearLevels = _uiState.value.deletingYearLevels - yearLevelId
                    )
                    // Reload year levels to update UI
                    loadYearLevels(forceRefresh = true)
                    
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        deletingYearLevels = _uiState.value.deletingYearLevels - yearLevelId,
                        error = exception.message ?: "Failed to delete year level"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    deletingYearLevels = _uiState.value.deletingYearLevels - yearLevelId,
                    error = e.message ?: "Failed to delete year level"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class AdminYearLevelManagementUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val deletingYearLevels: Set<String> = emptySet()
)
