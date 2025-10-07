package com.smartacademictracker.presentation.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.Grade
import com.smartacademictracker.data.model.GradePeriod
import com.smartacademictracker.data.repository.GradeRepository
import com.smartacademictracker.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentGradeHistoryViewModel @Inject constructor(
    private val gradeRepository: GradeRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentGradeHistoryUiState())
    val uiState: StateFlow<StudentGradeHistoryUiState> = _uiState.asStateFlow()

    fun loadGradeHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { currentUser ->
                    if (currentUser == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "User not found"
                        )
                        return@onSuccess
                    }
                    
                    val result = gradeRepository.getGradesByStudent(currentUser.id)
                    result.onSuccess { grades ->
                        val gradeHistory = grades.map { grade ->
                            GradeHistoryEntry(
                                subjectName = grade.subjectName,
                                gradePeriod = grade.gradePeriod,
                                score = grade.score,
                                maxScore = grade.maxScore,
                                percentage = grade.percentage,
                                letterGrade = grade.letterGrade,
                                dateRecorded = grade.dateRecorded
                            )
                        }.sortedByDescending { it.dateRecorded }
                        
                        val averageGrade = if (gradeHistory.isNotEmpty()) {
                            gradeHistory.map { it.percentage }.average()
                        } else 0.0
                        
                        val improvementTrend = calculateImprovementTrend(gradeHistory)
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            gradeHistory = gradeHistory,
                            averageGrade = averageGrade,
                            improvementTrend = improvementTrend
                        )
                    }.onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load grade history"
                        )
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to get current user"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load grade history"
                )
            }
        }
    }

    private fun calculateImprovementTrend(gradeHistory: List<GradeHistoryEntry>): String {
        if (gradeHistory.size < 2) return "Insufficient Data"
        
        val recentGrades = gradeHistory.take(5) // Last 5 grades
        val olderGrades = gradeHistory.drop(5).take(5) // Previous 5 grades
        
        if (olderGrades.isEmpty()) return "Insufficient Data"
        
        val recentAverage = recentGrades.map { it.percentage }.average()
        val olderAverage = olderGrades.map { it.percentage }.average()
        
        val improvement = recentAverage - olderAverage
        
        return when {
            improvement > 5 -> "Improving"
            improvement > 0 -> "Slightly Improving"
            improvement > -5 -> "Stable"
            else -> "Declining"
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class StudentGradeHistoryUiState(
    val isLoading: Boolean = false,
    val gradeHistory: List<GradeHistoryEntry> = emptyList(),
    val averageGrade: Double = 0.0,
    val improvementTrend: String = "",
    val error: String? = null
)
