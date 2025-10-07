package com.smartacademictracker.presentation.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.Grade
import com.smartacademictracker.data.model.GradePeriod
import com.smartacademictracker.data.model.GradeStatus
import com.smartacademictracker.data.repository.GradeRepository
import com.smartacademictracker.data.repository.UserRepository
import com.smartacademictracker.data.utils.GradeCalculationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentGradeComparisonViewModel @Inject constructor(
    private val gradeRepository: GradeRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentGradeComparisonUiState())
    val uiState: StateFlow<StudentGradeComparisonUiState> = _uiState.asStateFlow()

    fun loadSubjectComparisons() {
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
                        val subjectComparisons = createSubjectComparisons(grades)
                        val overallAverage = if (subjectComparisons.isNotEmpty()) {
                            subjectComparisons.map { it.averageGrade }.average()
                        } else 0.0
                        
                        val bestSubject = subjectComparisons.maxByOrNull { it.averageGrade }?.subjectName ?: "N/A"
                        val worstSubject = subjectComparisons.minByOrNull { it.averageGrade }?.subjectName ?: "N/A"
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            subjectComparisons = subjectComparisons,
                            overallAverage = overallAverage,
                            bestSubject = bestSubject,
                            worstSubject = worstSubject
                        )
                    }.onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load subject comparisons"
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
                    error = e.message ?: "Failed to load subject comparisons"
                )
            }
        }
    }

    private fun createSubjectComparisons(grades: List<Grade>): List<SubjectComparison> {
        val groupedGrades = grades.groupBy { it.subjectId }
        
        return groupedGrades.map { (subjectId, subjectGrades) ->
            val subjectName = subjectGrades.firstOrNull()?.subjectName ?: "Unknown Subject"
            
            val prelimGrade = subjectGrades.find { it.gradePeriod == GradePeriod.PRELIM }?.percentage
            val midtermGrade = subjectGrades.find { it.gradePeriod == GradePeriod.MIDTERM }?.percentage
            val finalGrade = subjectGrades.find { it.gradePeriod == GradePeriod.FINAL }?.percentage
            
            val finalAverage = GradeCalculationEngine.calculateFinalAverage(
                prelimGrade, midtermGrade, finalGrade
            ) ?: 0.0
            
            val status = GradeCalculationEngine.determineGradeStatus(finalAverage)
            
            SubjectComparison(
                subjectName = subjectName,
                averageGrade = finalAverage,
                prelimGrade = prelimGrade,
                midtermGrade = midtermGrade,
                finalGrade = finalGrade,
                status = status.displayName,
                rank = 0 // Will be set after sorting
            )
        }.sortedByDescending { it.averageGrade }
         .mapIndexed { index, comparison ->
             comparison.copy(rank = index + 1)
         }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class StudentGradeComparisonUiState(
    val isLoading: Boolean = false,
    val subjectComparisons: List<SubjectComparison> = emptyList(),
    val overallAverage: Double = 0.0,
    val bestSubject: String = "",
    val worstSubject: String = "",
    val error: String? = null
)
