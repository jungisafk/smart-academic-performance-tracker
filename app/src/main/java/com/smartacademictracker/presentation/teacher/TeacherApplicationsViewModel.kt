package com.smartacademictracker.presentation.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.TeacherApplication
import com.smartacademictracker.data.repository.TeacherApplicationRepository
import com.smartacademictracker.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeacherApplicationsViewModel @Inject constructor(
    private val teacherApplicationRepository: TeacherApplicationRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherApplicationsUiState())
    val uiState: StateFlow<TeacherApplicationsUiState> = _uiState.asStateFlow()

    private val _applications = MutableStateFlow<List<TeacherApplication>>(emptyList())
    val applications: StateFlow<List<TeacherApplication>> = _applications.asStateFlow()

    fun loadApplications() {
        viewModelScope.launch {
            println("DEBUG: TeacherApplicationsViewModel - Loading applications...")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Get current user
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        println("DEBUG: TeacherApplicationsViewModel - User found: ${user.email}")
                        // Load teacher's applications
                        val applicationsResult = teacherApplicationRepository.getApplicationsByTeacher(user.id)
                        applicationsResult.onSuccess { applicationsList ->
                            println("DEBUG: TeacherApplicationsViewModel - Applications loaded: ${applicationsList.size}")
                            applicationsList.forEach { app ->
                                println("DEBUG: TeacherApplicationsViewModel - Application: ${app.subjectName} - ${app.status}")
                            }
                            _applications.value = applicationsList
                            _uiState.value = _uiState.value.copy(isLoading = false)
                        }.onFailure { exception ->
                            println("DEBUG: TeacherApplicationsViewModel - Failed to load applications: ${exception.message}")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to load applications"
                            )
                        }
                    } else {
                        println("DEBUG: TeacherApplicationsViewModel - User not found")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "User not found"
                        )
                    }
                }.onFailure { exception ->
                    println("DEBUG: TeacherApplicationsViewModel - Failed to get user: ${exception.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load user data"
                    )
                }
            } catch (e: Exception) {
                println("DEBUG: TeacherApplicationsViewModel - Exception: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load applications"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class TeacherApplicationsUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
