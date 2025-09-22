package com.smartacademictracker.presentation.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.User
import com.smartacademictracker.data.repository.EnrollmentRepository
import com.smartacademictracker.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val enrollmentRepository: EnrollmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentProfileUiState())
    val uiState: StateFlow<StudentProfileUiState> = _uiState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Get current user
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        _currentUser.value = user
                        
                        // Load student's enrollments count
                        val enrollmentsResult = enrollmentRepository.getEnrollmentsByStudent(user.id)
                        enrollmentsResult.onSuccess { enrollments ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                enrolledSubjects = enrollments.size
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
                    error = e.message ?: "Failed to load profile"
                )
            }
        }
    }

    fun refreshProfile() {
        loadProfile()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class StudentProfileUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val enrolledSubjects: Int = 0
)
