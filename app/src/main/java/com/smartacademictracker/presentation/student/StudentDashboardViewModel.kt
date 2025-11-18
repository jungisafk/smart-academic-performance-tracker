package com.smartacademictracker.presentation.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.repository.StudentEnrollmentRepository
import com.smartacademictracker.data.repository.GradeRepository
import com.smartacademictracker.data.repository.UserRepository
import com.smartacademictracker.data.model.Grade
import com.smartacademictracker.data.model.StudentGradeAggregate
import com.smartacademictracker.data.utils.GradeCalculationEngine
import com.smartacademictracker.data.manager.StudentDataCache
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
    private val gradeRepository: GradeRepository,
    private val studentDataCache: StudentDataCache
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentDashboardUiState())
    val uiState: StateFlow<StudentDashboardUiState> = _uiState.asStateFlow()

    fun loadDashboardData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            // Check cache first
            val cachedEnrollments = studentDataCache.cachedEnrollments.value
            val cachedGrades = studentDataCache.cachedGrades.value
            
            // Only show loading if no cached data or cache is invalid
            if (forceRefresh || !studentDataCache.isCacheValid() || cachedEnrollments.isEmpty()) {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }
            
            try {
                // Get current user
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        // Use cached data immediately if available and valid
                        if (!forceRefresh && studentDataCache.isCacheValid() && cachedEnrollments.isNotEmpty() && cachedGrades.isNotEmpty()) {
                            val totalSubjectsPassed = calculateTotalSubjectsPassed(cachedGrades)
                            val recentGrades = cachedGrades.sortedByDescending { it.dateRecorded }.take(5)
                            
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                enrolledSubjects = cachedEnrollments.size,
                                recentGrades = recentGrades,
                                totalSubjectsPassed = totalSubjectsPassed
                            )
                        }
                        
                        // Set up real-time listener for enrollments in a separate coroutine
                        launch {
                            studentEnrollmentRepository.getEnrollmentsByStudentFlow(user.id)
                                .catch { exception ->
                                    if (!studentDataCache.isCacheValid()) {
                                        _uiState.value = _uiState.value.copy(
                                            isLoading = false,
                                            error = exception.message ?: "Failed to load enrollments"
                                        )
                                    }
                                }
                                .collect { enrollments ->
                                    // Update cache
                                    studentDataCache.updateEnrollments(enrollments)
                                    
                                    // Load recent grades
                                    launch {
                                        val gradesResult = gradeRepository.getGradesByStudent(user.id)
                                        gradesResult.onSuccess { grades ->
                                            // Update cache
                                            studentDataCache.updateGrades(grades)
                                            
                                            // Calculate total subjects passed
                                            val totalSubjectsPassed = calculateTotalSubjectsPassed(grades)
                                            
                                            // Get recent grades (last 5)
                                            val recentGrades = grades.sortedByDescending { it.dateRecorded }.take(5)
                                            
                                            _uiState.value = _uiState.value.copy(
                                                isLoading = false,
                                                enrolledSubjects = enrollments.size,
                                                recentGrades = recentGrades,
                                                totalSubjectsPassed = totalSubjectsPassed
                                            )
                                        }.onFailure { gradeException ->
                                            if (!studentDataCache.isCacheValid()) {
                                                _uiState.value = _uiState.value.copy(
                                                    isLoading = false,
                                                    enrolledSubjects = enrollments.size,
                                                    recentGrades = emptyList(),
                                                    totalSubjectsPassed = 0
                                                )
                                            }
                                        }
                                    }
                                }
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "User not found"
                        )
                    }
                }.onFailure { exception ->
                    // If cache exists, use it; otherwise show error
                    if (cachedEnrollments.isNotEmpty() && studentDataCache.isCacheValid()) {
                        val totalSubjectsPassed = calculateTotalSubjectsPassed(cachedGrades)
                        val recentGrades = cachedGrades.sortedByDescending { it.dateRecorded }.take(5)
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            enrolledSubjects = cachedEnrollments.size,
                            recentGrades = recentGrades,
                            totalSubjectsPassed = totalSubjectsPassed
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load user data"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load dashboard data"
                )
            }
        }
    }
    
    private fun calculateTotalSubjectsPassed(grades: List<Grade>): Int {
        return if (grades.isNotEmpty()) {
            // Group grades by subject
            val gradesBySubject = grades.groupBy { it.subjectId }
            
            // Count subjects with passing grade (>= 75)
            gradesBySubject.count { (_, subjectGrades) ->
                // Get grades for each period
                val prelimGrade = subjectGrades.find { it.gradePeriod == com.smartacademictracker.data.model.GradePeriod.PRELIM }?.percentage
                val midtermGrade = subjectGrades.find { it.gradePeriod == com.smartacademictracker.data.model.GradePeriod.MIDTERM }?.percentage
                val finalGrade = subjectGrades.find { it.gradePeriod == com.smartacademictracker.data.model.GradePeriod.FINAL }?.percentage
                
                // Calculate final average using standard formula
                val finalAverage = GradeCalculationEngine.calculateFinalAverage(
                    prelimGrade, midtermGrade, finalGrade
                )
                
                // Check if passing (>= 75)
                finalAverage != null && finalAverage >= com.smartacademictracker.data.model.GradeStatus.PASSING.threshold
            }
        } else 0
    }

    fun refreshData() {
        loadDashboardData(forceRefresh = true)
    }
}

data class StudentDashboardUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val enrolledSubjects: Int = 0,
    val recentGrades: List<Grade> = emptyList(),
    val totalSubjectsPassed: Int = 0
)
