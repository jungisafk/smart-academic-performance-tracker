package com.smartacademictracker.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.TeacherApplication
import com.smartacademictracker.data.model.ApplicationStatus
import com.smartacademictracker.data.repository.TeacherApplicationRepository
import com.smartacademictracker.data.repository.SubjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminApplicationsViewModel @Inject constructor(
    private val teacherApplicationRepository: TeacherApplicationRepository,
    private val subjectRepository: SubjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminApplicationsUiState())
    val uiState: StateFlow<AdminApplicationsUiState> = _uiState.asStateFlow()

    private val _applications = MutableStateFlow<List<TeacherApplication>>(emptyList())
    val applications: StateFlow<List<TeacherApplication>> = _applications.asStateFlow()

    fun loadApplications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = teacherApplicationRepository.getAllApplications()
                result.onSuccess { applicationsList ->
                    _applications.value = applicationsList
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load applications"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load applications"
                )
            }
        }
    }

    fun approveApplication(applicationId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // First, get the application details
                val applicationResult = teacherApplicationRepository.getApplicationById(applicationId)
                applicationResult.onSuccess { application ->
                    // Update application status
                    val statusResult = teacherApplicationRepository.updateApplicationStatus(applicationId, ApplicationStatus.APPROVED.name)
                    statusResult.onSuccess {
                        // Assign teacher to subject
                        val assignResult = subjectRepository.assignTeacherToSubject(
                            application.subjectId,
                            application.teacherId,
                            application.teacherName
                        )
                        assignResult.onSuccess {
                            println("DEBUG: AdminApplicationsViewModel - Teacher assigned to subject successfully")
                            // Reload applications after status update
                            loadApplications()
                        }.onFailure { exception ->
                            println("DEBUG: AdminApplicationsViewModel - Failed to assign teacher: ${exception.message}")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Application approved but failed to assign teacher: ${exception.message}"
                            )
                        }
                    }.onFailure { exception ->
                        println("DEBUG: AdminApplicationsViewModel - Failed to update application status: ${exception.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to approve application"
                        )
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to get application details"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to approve application"
                )
            }
        }
    }

    fun rejectApplication(applicationId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = teacherApplicationRepository.updateApplicationStatus(applicationId, ApplicationStatus.REJECTED.name)
                result.onSuccess {
                    // Reload applications after status update
                    loadApplications()
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to reject application"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to reject application"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class AdminApplicationsUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
