package com.smartacademictracker.presentation.student

import android.util.Log
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
                        // Try to load student's grade aggregates first
                        val aggregatesResult = gradeRepository.getStudentGradeAggregatesByStudent(user.id)
                        aggregatesResult.onSuccess { aggregatesList ->
                            if (aggregatesList.isNotEmpty()) {
                                // Use aggregates if available
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
                                
                                Log.d("StudentAnalytics", "Loaded ${aggregatesList.size} grade aggregates")
                            } else {
                                // Fallback: Load individual grades and create aggregates on-the-fly
                                Log.d("StudentAnalytics", "No aggregates found, loading individual grades")
                                loadAnalyticsFromIndividualGrades(user.id)
                            }
                        }.onFailure { exception ->
                            // Fallback: Load individual grades if aggregates fail
                            Log.d("StudentAnalytics", "Aggregates failed, loading individual grades: ${exception.message}")
                            loadAnalyticsFromIndividualGrades(user.id)
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
    
    private suspend fun loadAnalyticsFromIndividualGrades(studentId: String) {
        try {
            // Load individual grades
            val gradesResult = gradeRepository.getGradesByStudent(studentId)
            gradesResult.onSuccess { gradesList ->
                if (gradesList.isNotEmpty()) {
                    Log.d("StudentAnalytics", "Loaded ${gradesList.size} individual grades")
                    
                    // Group grades by subject and create aggregates on-the-fly
                    val subjectGroups = gradesList.groupBy { it.subjectId }
                    val aggregates = subjectGroups.map { (subjectId, subjectGrades) ->
                        val firstGrade = subjectGrades.first()
                        createAggregateFromGrades(
                            studentId = studentId,
                            subjectId = subjectId,
                            subjectName = firstGrade.subjectName,
                            grades = subjectGrades
                        )
                    }
                    
                    _gradeAggregates.value = aggregates
                    
                    // Calculate analytics
                    val analytics = calculateAnalytics(aggregates)
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        overallAverage = analytics.overallAverage,
                        passingSubjects = analytics.passingSubjects,
                        atRiskSubjects = analytics.atRiskSubjects,
                        failingSubjects = analytics.failingSubjects
                    )
                    
                    Log.d("StudentAnalytics", "Created ${aggregates.size} aggregates from individual grades")
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "No grades found"
                    )
                }
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = exception.message ?: "Failed to load individual grades"
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = e.message ?: "Failed to process individual grades"
            )
        }
    }
    
    private fun createAggregateFromGrades(
        studentId: String,
        subjectId: String,
        subjectName: String,
        grades: List<com.smartacademictracker.data.model.Grade>
    ): StudentGradeAggregate {
        val firstGrade = grades.first()
        
        // Calculate period grades
        val prelimGrade = grades.find { it.gradePeriod == com.smartacademictracker.data.model.GradePeriod.PRELIM }
        val midtermGrade = grades.find { it.gradePeriod == com.smartacademictracker.data.model.GradePeriod.MIDTERM }
        val finalGrade = grades.find { it.gradePeriod == com.smartacademictracker.data.model.GradePeriod.FINAL }
        
        // Calculate final average
        val validGrades = listOfNotNull(prelimGrade, midtermGrade, finalGrade)
        val finalAverage = if (validGrades.isNotEmpty()) {
            validGrades.map { it.percentage }.average()
        } else null
        
        // Determine status
        val status = when {
            finalAverage == null -> com.smartacademictracker.data.model.GradeStatus.INCOMPLETE
            finalAverage >= 90 -> com.smartacademictracker.data.model.GradeStatus.PASSING
            finalAverage >= 80 -> com.smartacademictracker.data.model.GradeStatus.PASSING
            finalAverage >= 70 -> com.smartacademictracker.data.model.GradeStatus.AT_RISK
            else -> com.smartacademictracker.data.model.GradeStatus.FAILING
        }
        
        return StudentGradeAggregate(
            id = "${studentId}_${subjectId}_${firstGrade.semester}_${firstGrade.academicYear}",
            studentId = studentId,
            studentName = firstGrade.studentName,
            subjectId = subjectId,
            subjectName = subjectName,
            teacherId = firstGrade.teacherId,
            semester = firstGrade.semester,
            academicYear = firstGrade.academicYear,
            prelimGrade = prelimGrade?.percentage,
            midtermGrade = midtermGrade?.percentage,
            finalGrade = finalGrade?.percentage,
            finalAverage = finalAverage,
            status = status,
            lastUpdated = System.currentTimeMillis()
        )
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
