package com.smartacademictracker.presentation.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.repository.EnrollmentRepository
import com.smartacademictracker.data.repository.GradeRepository
import com.smartacademictracker.data.repository.UserRepository
import com.smartacademictracker.data.model.Grade
import com.smartacademictracker.data.model.StudentGradeAggregate
import com.smartacademictracker.data.utils.GradeCalculationEngine
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
            println("DEBUG: StudentDashboardViewModel - Starting loadDashboardData()")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Get current user
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        println("DEBUG: StudentDashboardViewModel - Loading enrollments for user: ${user.id}")
                        // Load student's enrollments
                        val enrollmentsResult = enrollmentRepository.getEnrollmentsByStudent(user.id)
                        enrollmentsResult.onSuccess { enrollments ->
                            println("DEBUG: StudentDashboardViewModel - Loaded ${enrollments.size} enrollments")
                            
                            // Load recent grades
                            val gradesResult = gradeRepository.getGradesByStudent(user.id)
                            gradesResult.onSuccess { grades ->
                                println("DEBUG: StudentDashboardViewModel - Loaded ${grades.size} grades")
                                
                                // Calculate average grade
                                val averageGrade = if (grades.isNotEmpty()) {
                                    grades.map { it.percentage }.average()
                                } else 0.0
                                
                                // Get recent grades (last 5)
                                val recentGrades = grades.sortedByDescending { it.dateRecorded }.take(5)
                                
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    enrolledSubjects = enrollments.size,
                                    recentGrades = recentGrades,
                                    averageGrade = averageGrade
                                )
                            }.onFailure { gradeException ->
                                println("DEBUG: StudentDashboardViewModel - Error loading grades: ${gradeException.message}")
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    enrolledSubjects = enrollments.size,
                                    recentGrades = emptyList(),
                                    averageGrade = 0.0
                                )
                            }
                        }.onFailure { exception ->
                            println("DEBUG: StudentDashboardViewModel - Error loading enrollments: ${exception.message}")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to load enrollments"
                            )
                        }
                    } else {
                        println("DEBUG: StudentDashboardViewModel - User not found")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "User not found"
                        )
                    }
                }.onFailure { exception ->
                    println("DEBUG: StudentDashboardViewModel - Error getting current user: ${exception.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load user data"
                    )
                }
            } catch (e: Exception) {
                println("DEBUG: StudentDashboardViewModel - Exception in loadDashboardData: ${e.message}")
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
    
    fun updateEnrollments(enrollments: List<com.smartacademictracker.data.model.Enrollment>) {
        _uiState.value = _uiState.value.copy(
            enrolledSubjects = enrollments.size
        )
        println("DEBUG: StudentDashboardViewModel - Updated enrollments count: ${enrollments.size}")
    }
}

data class StudentDashboardUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val enrolledSubjects: Int = 0,
    val recentGrades: List<Grade> = emptyList(),
    val averageGrade: Double = 0.0
)
