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
import kotlinx.coroutines.flow.catch
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

    init {
        setupRealtimeUserListener()
        loadProfile()
    }
    
    /**
     * Set up real-time listener for current user changes
     * This ensures profile screen updates automatically when user data changes (e.g., year level progression)
     */
    private fun setupRealtimeUserListener() {
        viewModelScope.launch {
            userRepository.getCurrentUserFlow()
                .catch { exception ->
                    android.util.Log.e("StudentProfileViewModel", "Error in current user flow: ${exception.message}", exception)
                    // Fallback to one-time query on error
                    loadProfile()
                }
                .collect { user ->
                    _currentUser.value = user
                    // Reload enrollments when user changes
                    if (user != null) {
                        loadEnrollmentsCount(user.id)
                    }
                }
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Get current user
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        _currentUser.value = user
                        loadEnrollmentsCount(user.id)
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
    
    private fun loadEnrollmentsCount(userId: String) {
        viewModelScope.launch {
            val enrollmentsResult = enrollmentRepository.getEnrollmentsByStudent(userId)
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
