package com.smartacademictracker.presentation.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.repository.UserRepository
import com.smartacademictracker.data.repository.SubjectRepository
import com.smartacademictracker.data.repository.EnrollmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeacherDashboardViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val subjectRepository: SubjectRepository,
    private val enrollmentRepository: EnrollmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherDashboardUiState())
    val uiState: StateFlow<TeacherDashboardUiState> = _uiState.asStateFlow()

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Get current user (teacher)
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        // Load subjects taught by this teacher
                        val subjectsResult = subjectRepository.getAllSubjects()
                        subjectsResult.onSuccess { subjectsList ->
                            val teacherSubjects = subjectsList.filter { it.teacherId == user.id && it.active }
                            val subjectIds = teacherSubjects.map { it.id }
                            
                            // Load enrollments for these subjects
                            val enrollmentsResult = enrollmentRepository.getAllEnrollments()
                            enrollmentsResult.onSuccess { enrollmentsList ->
                                val teacherEnrollments = enrollmentsList.filter { it.subjectId in subjectIds }
                                val totalStudents = teacherEnrollments.distinctBy { it.studentId }.size
                                
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    activeSubjects = teacherSubjects.size,
                                    totalStudents = totalStudents
                                )
                                
                                println("DEBUG: TeacherDashboardViewModel - Loaded ${teacherSubjects.size} subjects and ${totalStudents} students for teacher ${user.id}")
                            }.onFailure { exception ->
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = exception.message ?: "Failed to load enrollments"
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
                println("DEBUG: TeacherDashboardViewModel - Error loading dashboard data: ${e.message}")
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

data class TeacherDashboardUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val activeSubjects: Int = 0,
    val totalStudents: Int = 0
)
