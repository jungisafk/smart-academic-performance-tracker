package com.smartacademictracker.presentation.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.SubjectApplication
import com.smartacademictracker.data.repository.SubjectApplicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentApplicationDetailViewModel @Inject constructor(
    private val applicationRepository: SubjectApplicationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentApplicationDetailUiState())
    val uiState: StateFlow<StudentApplicationDetailUiState> = _uiState.asStateFlow()

    fun loadApplicationDetail(applicationId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = applicationRepository.getApplicationById(applicationId)
                result.onSuccess { application ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        application = application
                    )
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load application details"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load application details"
                )
            }
        }
    }

    fun withdrawApplication(applicationId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = applicationRepository.withdrawApplication(applicationId)
                result.onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Application withdrawn successfully"
                    )
                    // Reload application details
                    loadApplicationDetail(applicationId)
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to withdraw application"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to withdraw application"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}

data class StudentApplicationDetailUiState(
    val isLoading: Boolean = false,
    val application: SubjectApplication? = null,
    val error: String? = null,
    val message: String? = null
)
