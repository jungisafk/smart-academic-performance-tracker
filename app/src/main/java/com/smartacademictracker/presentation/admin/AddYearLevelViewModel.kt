package com.smartacademictracker.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.YearLevel
import com.smartacademictracker.data.repository.YearLevelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddYearLevelViewModel @Inject constructor(
    private val yearLevelRepository: YearLevelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddYearLevelUiState())
    val uiState: StateFlow<AddYearLevelUiState> = _uiState.asStateFlow()
    
    private var _courseId: String = ""
    
    fun setCourseId(courseId: String) {
        _courseId = courseId
        println("DEBUG: AddYearLevelViewModel - Set courseId: '$courseId'")
    }

    fun setYearLevelName(name: String) {
        _uiState.value = _uiState.value.copy(
            yearLevelName = name,
            yearLevelNameError = null
        )
    }

    fun setLevel(level: Int) {
        _uiState.value = _uiState.value.copy(
            level = level,
            levelError = null
        )
    }

    fun setDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun addYearLevel() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            // Validate input
            val validationResult = validateInput()
            if (validationResult != null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = validationResult
                )
                return@launch
            }
            
            try {
                val yearLevel = YearLevel(
                    courseId = _courseId,
                    name = _uiState.value.yearLevelName.trim(),
                    level = _uiState.value.level,
                    description = _uiState.value.description.trim()
                )
                println("DEBUG: AddYearLevelViewModel - Creating year level with courseId: '$_courseId'")
                
                val createResult = yearLevelRepository.createYearLevel(yearLevel)
                createResult.onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true
                    )
                    println("DEBUG: AddYearLevelViewModel - Year level created successfully")
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to create year level"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to create year level"
                )
            }
        }
    }

    private fun validateInput(): String? {
        val state = _uiState.value
        
        if (state.yearLevelName.isBlank()) {
            _uiState.value = _uiState.value.copy(yearLevelNameError = "Year level name is required")
            return "Please fill in all required fields"
        }
        
        if (state.level < 1 || state.level > 10) {
            _uiState.value = _uiState.value.copy(levelError = "Level must be between 1 and 10")
            return "Please enter a valid level number"
        }
        
        return null
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class AddYearLevelUiState(
    val yearLevelName: String = "",
    val level: Int = 1,
    val description: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val yearLevelNameError: String? = null,
    val levelError: String? = null
)
