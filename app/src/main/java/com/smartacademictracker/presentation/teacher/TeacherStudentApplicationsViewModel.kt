package com.smartacademictracker.presentation.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.StudentApplication
import com.smartacademictracker.data.model.StudentApplicationStatus
import com.smartacademictracker.data.repository.StudentApplicationRepository
import com.smartacademictracker.data.repository.SubjectRepository
import com.smartacademictracker.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeacherStudentApplicationsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val studentApplicationRepository: StudentApplicationRepository,
    private val subjectRepository: SubjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherStudentApplicationsUiState())
    val uiState: StateFlow<TeacherStudentApplicationsUiState> = _uiState.asStateFlow()

    private val _applications = MutableStateFlow<List<StudentApplication>>(emptyList())
    val applications: StateFlow<List<StudentApplication>> = _applications.asStateFlow()

    fun loadApplications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Get current user (teacher)
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        // First get all subjects taught by this teacher
                        val subjectsResult = subjectRepository.getAllSubjects()
                        subjectsResult.onSuccess { subjectsList ->
                            val teacherSubjects = subjectsList.filter { it.teacherId == user.id }
                            val subjectIds = teacherSubjects.map { it.id }
                            
                            println("DEBUG: TeacherStudentApplicationsViewModel - Found ${teacherSubjects.size} subjects for teacher ${user.id}")
                            teacherSubjects.forEach { subject ->
                                println("DEBUG: Teacher Subject - ID: ${subject.id}, Name: ${subject.name}, TeacherId: ${subject.teacherId}")
                            }
                            println("DEBUG: TeacherStudentApplicationsViewModel - Subject IDs: $subjectIds")
                            
                            // Now get applications for these subjects
                            val applicationsResult = studentApplicationRepository.getApplicationsForTeacherSubjects(user.id, subjectIds)
                            applicationsResult.onSuccess { applicationsList ->
                                _applications.value = applicationsList
                                _uiState.value = _uiState.value.copy(isLoading = false)
                                println("DEBUG: TeacherStudentApplicationsViewModel - Loaded ${applicationsList.size} applications for ${subjectIds.size} subjects")
                            }.onFailure { exception ->
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = exception.message ?: "Failed to load applications"
                                )
                            }
                        }.onFailure { exception ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to load subjects"
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
                    error = e.message ?: "Failed to load applications"
                )
            }
        }
    }

    fun updateApplicationStatus(
        applicationId: String,
        status: StudentApplicationStatus,
        comments: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                processingApplications = _uiState.value.processingApplications + applicationId,
                error = null
            )
            
            try {
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        val updateResult = studentApplicationRepository.updateApplicationStatus(
                            applicationId = applicationId,
                            status = status,
                            teacherId = user.id,
                            comments = comments
                        )
                        
                        updateResult.onSuccess {
                            _uiState.value = _uiState.value.copy(
                                processingApplications = _uiState.value.processingApplications - applicationId
                            )
                            // Reload applications to update UI
                            loadApplications()
                            println("DEBUG: TeacherStudentApplicationsViewModel - Application status updated successfully")
                        }.onFailure { exception ->
                            _uiState.value = _uiState.value.copy(
                                processingApplications = _uiState.value.processingApplications - applicationId,
                                error = exception.message ?: "Failed to update application status"
                            )
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            processingApplications = _uiState.value.processingApplications - applicationId,
                            error = "User not found"
                        )
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        processingApplications = _uiState.value.processingApplications - applicationId,
                        error = exception.message ?: "Failed to get user data"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    processingApplications = _uiState.value.processingApplications - applicationId,
                    error = e.message ?: "Failed to update application status"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun refreshApplications() {
        loadApplications()
    }
}

data class TeacherStudentApplicationsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val processingApplications: Set<String> = emptySet()
)
