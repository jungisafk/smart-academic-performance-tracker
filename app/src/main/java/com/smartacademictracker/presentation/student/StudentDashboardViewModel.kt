package com.smartacademictracker.presentation.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.repository.EnrollmentRepository
import com.smartacademictracker.data.repository.GradeRepository
import com.smartacademictracker.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentDashboardViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val gradeRepository: GradeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentDashboardUiState())
    val uiState: StateFlow<StudentDashboardUiState> = _uiState.asStateFlow()

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Get current user
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        // Load student's enrollments
                        val enrollmentsResult = enrollmentRepository.getEnrollmentsByStudent(user.id)
                        enrollmentsResult.onSuccess { enrollments ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                enrolledSubjects = enrollments.size,
                                recentGrades = emptyList() // TODO: Load recent grades
                            )
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
                    error = e.message ?: "Failed to load dashboard data"
                )
            }
        }
    }

    fun refreshData() {
        loadDashboardData()
    }
}

data class StudentDashboardUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val enrolledSubjects: Int = 0,
    val recentGrades: List<Any> = emptyList() // TODO: Replace with proper Grade type
)
