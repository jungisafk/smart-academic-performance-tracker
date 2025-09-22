package com.smartacademictracker.presentation.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.Enrollment
import com.smartacademictracker.data.repository.EnrollmentRepository
import com.smartacademictracker.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentSubjectsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val enrollmentRepository: EnrollmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentSubjectsUiState())
    val uiState: StateFlow<StudentSubjectsUiState> = _uiState.asStateFlow()

    private val _enrollments = MutableStateFlow<List<Enrollment>>(emptyList())
    val enrollments: StateFlow<List<Enrollment>> = _enrollments.asStateFlow()

    fun loadEnrollments() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Get current user
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        // Load student's enrollments
                        val enrollmentsResult = enrollmentRepository.getEnrollmentsByStudent(user.id)
                        enrollmentsResult.onSuccess { enrollmentsList ->
                            _enrollments.value = enrollmentsList
                            _uiState.value = _uiState.value.copy(isLoading = false)
                            println("DEBUG: StudentSubjectsViewModel - Loaded ${enrollmentsList.size} enrollments")
                        }.onFailure { exception ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to load enrollments"
                            )
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "User not found"
                        )
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load user data"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load enrollments"
                )
            }
        }
    }

    fun refreshEnrollments() {
        loadEnrollments()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class StudentSubjectsUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
