package com.smartacademictracker.presentation.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.StudentGradeAggregate
import com.smartacademictracker.data.model.GradeStatus
import com.smartacademictracker.data.repository.GradeRepository
import com.smartacademictracker.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentAnalyticsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val gradeRepository: GradeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentAnalyticsUiState())
    val uiState: StateFlow<StudentAnalyticsUiState> = _uiState.asStateFlow()

    private val _gradeAggregates = MutableStateFlow<List<StudentGradeAggregate>>(emptyList())
    val gradeAggregates: StateFlow<List<StudentGradeAggregate>> = _gradeAggregates.asStateFlow()

    fun loadAnalyticsData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Get current user
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        // Load student's grade aggregates
                        val aggregatesResult = gradeRepository.getStudentGradeAggregatesByStudent(user.id)
                        aggregatesResult.onSuccess { aggregatesList ->
                            _gradeAggregates.value = aggregatesList
                            
                            // Calculate analytics
                            val analytics = calculateAnalytics(aggregatesList)
                            
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                overallAverage = analytics.overallAverage,
                                passingSubjects = analytics.passingSubjects,
                                atRiskSubjects = analytics.atRiskSubjects,
                                failingSubjects = analytics.failingSubjects
                            )
                            
                            println("DEBUG: StudentAnalyticsViewModel - Loaded ${aggregatesList.size} grade aggregates")
                        }.onFailure { exception ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to load grade data"
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
                    error = e.message ?: "Failed to load analytics data"
                )
            }
        }
    }

    fun refreshData() {
        loadAnalyticsData()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun calculateAnalytics(aggregates: List<StudentGradeAggregate>): AnalyticsData {
        if (aggregates.isEmpty()) {
            return AnalyticsData(
                overallAverage = null,
                passingSubjects = 0,
                atRiskSubjects = 0,
                failingSubjects = 0
            )
        }

        val validAverages = aggregates.mapNotNull { it.finalAverage }
        val overallAverage = if (validAverages.isNotEmpty()) {
            validAverages.average()
        } else null

        val passingSubjects = aggregates.count { 
            it.finalAverage != null && it.status == GradeStatus.PASSING 
        }
        val atRiskSubjects = aggregates.count { 
            it.finalAverage != null && it.status == GradeStatus.AT_RISK 
        }
        val failingSubjects = aggregates.count { 
            it.finalAverage != null && it.status == GradeStatus.FAILING 
        }

        return AnalyticsData(
            overallAverage = overallAverage,
            passingSubjects = passingSubjects,
            atRiskSubjects = atRiskSubjects,
            failingSubjects = failingSubjects
        )
    }
}

data class StudentAnalyticsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val overallAverage: Double? = null,
    val passingSubjects: Int = 0,
    val atRiskSubjects: Int = 0,
    val failingSubjects: Int = 0
)

private data class AnalyticsData(
    val overallAverage: Double?,
    val passingSubjects: Int,
    val atRiskSubjects: Int,
    val failingSubjects: Int
)
