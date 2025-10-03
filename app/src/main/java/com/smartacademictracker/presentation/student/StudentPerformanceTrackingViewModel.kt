package com.smartacademictracker.presentation.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.repository.GradeRepository
import com.smartacademictracker.data.repository.UserRepository
import com.smartacademictracker.data.model.Grade
import com.smartacademictracker.data.model.GradePeriod
import com.smartacademictracker.data.model.GradeStatus
import com.smartacademictracker.data.model.StudentGradeAggregate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentPerformanceTrackingViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val gradeRepository: GradeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentPerformanceTrackingUiState())
    val uiState: StateFlow<StudentPerformanceTrackingUiState> = _uiState.asStateFlow()

    private val _performanceData = MutableStateFlow(PerformanceData(
        overallAverage = 0.0,
        trend = PerformanceTrend.STABLE,
        improvement = 0.0,
        subjectPerformance = emptyList(),
        periodTrends = emptyList()
    ))
    val performanceData: StateFlow<PerformanceData> = _performanceData.asStateFlow()

    fun loadPerformanceData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        val gradesResult = gradeRepository.getGradesByStudent(user.id)
                        gradesResult.onSuccess { grades ->
                            val performanceData = calculatePerformanceData(grades)
                            _performanceData.value = performanceData
                            _uiState.value = _uiState.value.copy(isLoading = false)
                        }.onFailure { exception ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to load grades"
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
                    error = e.message ?: "Failed to load performance data"
                )
            }
        }
    }

    private fun calculatePerformanceData(grades: List<Grade>): PerformanceData {
        if (grades.isEmpty()) {
            return PerformanceData(
                overallAverage = 0.0,
                trend = PerformanceTrend.STABLE,
                improvement = 0.0,
                subjectPerformance = emptyList(),
                periodTrends = emptyList()
            )
        }

        // Calculate overall average
        val overallAverage = grades.map { it.percentage }.average()

        // Group grades by subject
        val gradesBySubject = grades.groupBy { it.subjectName }

        // Calculate subject performance
        val subjectPerformance = gradesBySubject.map { (subjectName, subjectGrades) ->
            val currentAverage = subjectGrades.map { it.percentage }.average()
            val sortedGrades = subjectGrades.sortedBy { it.dateRecorded }
            val previousAverage = if (sortedGrades.size > 1) {
                sortedGrades.take(sortedGrades.size - 1).map { it.percentage }.average()
            } else null

            val trend = when {
                previousAverage == null -> PerformanceTrend.STABLE
                currentAverage > previousAverage + 2 -> PerformanceTrend.IMPROVING
                currentAverage < previousAverage - 2 -> PerformanceTrend.DECLINING
                else -> PerformanceTrend.STABLE
            }

            val status = when {
                currentAverage >= 90 -> GradeStatus.PASSING
                currentAverage >= 75 -> GradeStatus.AT_RISK
                else -> GradeStatus.FAILING
            }

            SubjectPerformance(
                subjectName = subjectName,
                currentAverage = currentAverage,
                previousAverage = previousAverage,
                trend = trend,
                status = status
            )
        }

        // Calculate period trends
        val periodTrends = GradePeriod.values().map { period ->
            val periodGrades = grades.filter { it.gradePeriod == period }
            val average = if (periodGrades.isNotEmpty()) {
                periodGrades.map { it.percentage }.average()
            } else 0.0

            val trend = when {
                average >= 90 -> PerformanceTrend.IMPROVING
                average >= 75 -> PerformanceTrend.STABLE
                else -> PerformanceTrend.DECLINING
            }

            PeriodTrend(
                period = period,
                average = average,
                trend = trend,
                count = periodGrades.size
            )
        }

        // Calculate overall trend
        val recentGrades = grades.sortedByDescending { it.dateRecorded }.take(5)
        val olderGrades = grades.sortedByDescending { it.dateRecorded }.drop(5)
        
        val recentAverage = if (recentGrades.isNotEmpty()) {
            recentGrades.map { it.percentage }.average()
        } else 0.0
        
        val olderAverage = if (olderGrades.isNotEmpty()) {
            olderGrades.map { it.percentage }.average()
        } else recentAverage

        val overallTrend = when {
            recentAverage > olderAverage + 2 -> PerformanceTrend.IMPROVING
            recentAverage < olderAverage - 2 -> PerformanceTrend.DECLINING
            else -> PerformanceTrend.STABLE
        }

        val improvement = recentAverage - olderAverage

        return PerformanceData(
            overallAverage = overallAverage,
            trend = overallTrend,
            improvement = improvement,
            subjectPerformance = subjectPerformance,
            periodTrends = periodTrends
        )
    }

    fun refreshData() {
        loadPerformanceData()
    }
}

data class StudentPerformanceTrackingUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
