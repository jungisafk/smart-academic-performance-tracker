package com.smartacademictracker.presentation.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.Grade
import com.smartacademictracker.data.repository.GradeRepository
import com.smartacademictracker.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentGradesViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val gradeRepository: GradeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentGradesUiState())
    val uiState: StateFlow<StudentGradesUiState> = _uiState.asStateFlow()

    private val _grades = MutableStateFlow<List<Grade>>(emptyList())
    val grades: StateFlow<List<Grade>> = _grades.asStateFlow()

    fun loadGrades() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Get current user
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        // Load student's grades
                        val gradesResult = gradeRepository.getGradesByStudent(user.id)
                        gradesResult.onSuccess { gradesList ->
                            _grades.value = gradesList
                            _uiState.value = _uiState.value.copy(isLoading = false)
                            println("DEBUG: StudentGradesViewModel - Loaded ${gradesList.size} grades")
                        }.onFailure { exception ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to load grades"
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
                    error = e.message ?: "Failed to load grades"
                )
            }
        }
    }

    fun refreshGrades() {
        loadGrades()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class StudentGradesUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
