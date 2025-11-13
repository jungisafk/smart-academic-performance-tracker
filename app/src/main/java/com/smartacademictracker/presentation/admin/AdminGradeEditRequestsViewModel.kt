package com.smartacademictracker.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.Grade
import com.smartacademictracker.data.repository.GradeRepository
import com.smartacademictracker.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminGradeEditRequestsViewModel @Inject constructor(
    private val gradeRepository: GradeRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminGradeEditRequestsUiState())
    val uiState: StateFlow<AdminGradeEditRequestsUiState> = _uiState.asStateFlow()

    private val _gradeRequests = MutableStateFlow<List<Grade>>(emptyList())
    val gradeRequests: StateFlow<List<Grade>> = _gradeRequests.asStateFlow()

    init {
        // Set up real-time listener for grade edit requests
        viewModelScope.launch {
            gradeRepository.getGradesWithEditRequestsFlow()
                .onStart {
                    _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                }
                .catch { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load grade edit requests"
                    )
                }
                .collect { grades ->
                    println("DEBUG: AdminGradeEditRequestsViewModel - Real-time update: ${grades.size} grade edit requests")
                    _gradeRequests.value = grades
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        requestCount = grades.size,
                        error = null
                    )
                }
        }
    }

    fun loadGradeEditRequests() {
        // Real-time updates are handled automatically via Flow
        // This method is kept for backward compatibility but does nothing
        // as the Flow in init already handles all updates
    }

    fun approveEditRequest(gradeId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val currentUser = userRepository.getCurrentUser().getOrNull()
            val adminId = currentUser?.id ?: ""
            
            if (adminId.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Admin user not found"
                )
                return@launch
            }
            
            val result = gradeRepository.unlockGradeForEdit(gradeId, adminId)
            
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Grade unlocked for editing"
                )
                // Real-time Flow will automatically update the list
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = exception.message ?: "Failed to approve edit request"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}

data class AdminGradeEditRequestsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val requestCount: Int = 0
)

