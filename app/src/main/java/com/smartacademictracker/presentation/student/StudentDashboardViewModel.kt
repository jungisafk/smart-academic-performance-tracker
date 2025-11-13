package com.smartacademictracker.presentation.student

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.repository.StudentEnrollmentRepository
import com.smartacademictracker.data.repository.GradeRepository
import com.smartacademictracker.data.repository.UserRepository
import com.smartacademictracker.data.model.Grade
import com.smartacademictracker.data.model.StudentGradeAggregate
import com.smartacademictracker.data.utils.GradeCalculationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentDashboardViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val studentEnrollmentRepository: StudentEnrollmentRepository,
    private val gradeRepository: GradeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentDashboardUiState())
    val uiState: StateFlow<StudentDashboardUiState> = _uiState.asStateFlow()

    fun loadDashboardData() {
        viewModelScope.launch {
            Log.d("StudentDashboard", "=== Starting loadDashboardData() ===")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Get current user
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        Log.d("StudentDashboard", "Loading enrollments for user: ${user.id} (${user.studentId})")
                        
                        // Set up real-time listener for enrollments in a separate coroutine
                        launch {
                            studentEnrollmentRepository.getEnrollmentsByStudentFlow(user.id)
                                .catch { exception ->
                                    Log.e("StudentDashboard", "Error in enrollment flow: ${exception.message}")
                                    _uiState.value = _uiState.value.copy(
                                        isLoading = false,
                                        error = exception.message ?: "Failed to load enrollments"
                                    )
                                }
                                .collect { enrollments ->
                                    Log.d("StudentDashboard", "=== Real-time enrollment update ===")
                                    Log.d("StudentDashboard", "Loaded ${enrollments.size} enrollments")
                                    enrollments.forEachIndexed { index, enrollment ->
                                        Log.d("StudentDashboard", "  Enrollment[$index]: ${enrollment.subjectName} (${enrollment.subjectCode}) - Section: ${enrollment.sectionName}, Status: ${enrollment.status}")
                                    }
                                    
                                    // Load recent grades
                                    launch {
                                        val gradesResult = gradeRepository.getGradesByStudent(user.id)
                                        gradesResult.onSuccess { grades ->
                                            Log.d("StudentDashboard", "Loaded ${grades.size} grades")
                                            
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
                                            Log.d("StudentDashboard", "UI State updated - enrolledSubjects: ${enrollments.size}, averageGrade: $averageGrade")
                                        }.onFailure { gradeException ->
                                            Log.e("StudentDashboard", "Error loading grades: ${gradeException.message}")
                                            _uiState.value = _uiState.value.copy(
                                                isLoading = false,
                                                enrolledSubjects = enrollments.size,
                                                recentGrades = emptyList(),
                                                averageGrade = 0.0
                                            )
                                        }
                                    }
                                }
                        }
                    } else {
                        Log.e("StudentDashboard", "User not found")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "User not found"
                        )
                    }
                }.onFailure { exception ->
                    Log.e("StudentDashboard", "Error getting current user: ${exception.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load user data"
                    )
                }
            } catch (e: Exception) {
                Log.e("StudentDashboard", "Exception in loadDashboardData: ${e.message}", e)
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
    val recentGrades: List<Grade> = emptyList(),
    val averageGrade: Double = 0.0
)
