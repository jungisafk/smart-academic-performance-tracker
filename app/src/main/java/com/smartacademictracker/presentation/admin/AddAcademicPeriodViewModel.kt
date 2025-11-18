package com.smartacademictracker.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.AcademicPeriod
import com.smartacademictracker.data.model.Semester
import com.smartacademictracker.data.repository.AcademicPeriodRepository
import com.smartacademictracker.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddAcademicPeriodViewModel @Inject constructor(
    private val academicPeriodRepository: AcademicPeriodRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddAcademicPeriodUiState())
    val uiState: StateFlow<AddAcademicPeriodUiState> = _uiState.asStateFlow()

    fun setPeriodName(name: String) {
        _uiState.value = _uiState.value.copy(
            periodName = name,
            periodNameError = if (name.isBlank()) "Period name is required" else null
        )
    }

    fun setAcademicYear(academicYear: String) {
        // Only allow numbers and hyphen, and limit to academic year format
        val filteredInput = academicYear.filter { it.isDigit() || it == '-' }
        
        // Limit length to prevent invalid formats (e.g., "2024-2025" = 9 characters)
        val limitedInput = if (filteredInput.length > 9) {
            filteredInput.take(9)
        } else {
            filteredInput
        }
        
        val error = when {
            limitedInput.isBlank() -> "Academic year is required"
            !isValidAcademicYearFormat(limitedInput) -> "Academic year must be in format YYYY-YYYY (e.g., 2024-2025)"
            else -> null
        }
        
        _uiState.value = _uiState.value.copy(
            academicYear = limitedInput,
            academicYearError = error
        )
    }
    
    private fun isValidAcademicYearFormat(academicYear: String): Boolean {
        // Check if format matches YYYY-YYYY
        val pattern = Regex("^\\d{4}-\\d{4}$")
        return pattern.matches(academicYear)
    }

    fun setSemester(semester: Semester) {
        _uiState.value = _uiState.value.copy(selectedSemester = semester)
    }

    fun setStartDate(startDate: Long) {
        _uiState.value = _uiState.value.copy(
            startDate = startDate,
            startDateError = if (startDate <= 0) "Start date is required" else null
        )
    }

    fun setEndDate(endDate: Long) {
        _uiState.value = _uiState.value.copy(
            endDate = endDate,
            endDateError = if (endDate <= 0) "End date is required" else null
        )
    }

    fun setDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun setAsActive(isActive: Boolean) {
        
        _uiState.value = _uiState.value.copy(isActive = isActive)
    }

    fun addAcademicPeriod() {
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
                // Get current user info
                val currentUserResult = userRepository.getCurrentUser()
                val currentUser = currentUserResult.getOrNull()
                
                val academicPeriod = AcademicPeriod(
                    name = _uiState.value.periodName.trim(),
                    academicYear = _uiState.value.academicYear.trim(),
                    semester = _uiState.value.selectedSemester,
                    startDate = _uiState.value.startDate,
                    endDate = _uiState.value.endDate,
                    isActive = _uiState.value.isActive,
                    description = _uiState.value.description.trim(),
                    createdBy = currentUser?.id ?: "",
                    createdByName = "${currentUser?.firstName ?: ""} ${currentUser?.lastName ?: ""}".trim().ifEmpty { "Unknown" }
                )
                
                
                
                
                val createResult = academicPeriodRepository.createAcademicPeriod(academicPeriod)
                createResult.onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true
                    )
                    
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to create academic period"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to create academic period"
                )
            }
        }
    }

    private fun validateInput(): String? {
        val state = _uiState.value
        
        return when {
            state.periodName.isBlank() -> "Period name is required"
            state.academicYear.isBlank() -> "Academic year is required"
            !isValidAcademicYearFormat(state.academicYear) -> "Academic year must be in format YYYY-YYYY (e.g., 2024-2025)"
            state.startDate <= 0 -> "Start date is required"
            state.endDate <= 0 -> "End date is required"
            state.startDate >= state.endDate -> "End date must be after start date"
            else -> null
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(isSuccess = false)
    }
}

data class AddAcademicPeriodUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val periodName: String = "",
    val periodNameError: String? = null,
    val academicYear: String = "",
    val academicYearError: String? = null,
    val selectedSemester: Semester = Semester.FIRST_SEMESTER,
    val startDate: Long = 0L,
    val startDateError: String? = null,
    val endDate: Long = 0L,
    val endDateError: String? = null,
    val description: String = "",
    val isActive: Boolean = false
)
