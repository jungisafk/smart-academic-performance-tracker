package com.smartacademictracker.presentation.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.StudentGradeAggregate
import com.smartacademictracker.data.model.GradeStatus
import com.smartacademictracker.data.repository.GradeRepository
import com.smartacademictracker.data.repository.UserRepository
import com.smartacademictracker.data.manager.StudentDataCache
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentAnalyticsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val gradeRepository: GradeRepository,
    private val studentDataCache: StudentDataCache
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentAnalyticsUiState())
    val uiState: StateFlow<StudentAnalyticsUiState> = _uiState.asStateFlow()

    private val _gradeAggregates = MutableStateFlow<List<StudentGradeAggregate>>(emptyList())
    val gradeAggregates: StateFlow<List<StudentGradeAggregate>> = _gradeAggregates.asStateFlow()

    fun loadAnalyticsData(forceRefresh: Boolean = false) {
        // Prevent duplicate loads if already loading
        if (_uiState.value.isLoading && !forceRefresh) {
            return
        }
        
        viewModelScope.launch {
            // Check cache first
            val cachedAggregates = studentDataCache.cachedGradeAggregates.value
            
            // Only show loading if no cached data or cache is invalid
            if (forceRefresh || !studentDataCache.isCacheValid() || cachedAggregates.isEmpty()) {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }
            
            try {
                // Get current user
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        // Use cached data immediately if available and valid
                        if (!forceRefresh && studentDataCache.isCacheValid() && cachedAggregates.isNotEmpty()) {
                            val userAggregates = cachedAggregates.filter { it.studentId == user.id }
                            if (userAggregates.isNotEmpty()) {
                                _gradeAggregates.value = userAggregates
                                val analytics = calculateAnalytics(userAggregates)
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    overallAverage = analytics.overallAverage,
                                    passingSubjects = analytics.passingSubjects,
                                    atRiskSubjects = analytics.atRiskSubjects,
                                    failingSubjects = analytics.failingSubjects
                                )
                            }
                        }
                        
                        // Try to load student's grade aggregates first
                        val aggregatesResult = gradeRepository.getStudentGradeAggregatesByStudent(user.id)
                        aggregatesResult.onSuccess { aggregatesList ->
                            if (aggregatesList.isNotEmpty()) {
                                // Use aggregates if available
                                _gradeAggregates.value = aggregatesList
                                studentDataCache.updateGradeAggregates(aggregatesList)
                                
                                // Calculate analytics
                                val analytics = calculateAnalytics(aggregatesList)
                                
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    overallAverage = analytics.overallAverage,
                                    passingSubjects = analytics.passingSubjects,
                                    atRiskSubjects = analytics.atRiskSubjects,
                                    failingSubjects = analytics.failingSubjects
                                )
                            } else {
                                // Fallback: Load individual grades and create aggregates on-the-fly
                                loadAnalyticsFromIndividualGrades(user.id)
                            }
                        }.onFailure { exception ->
                            // Fallback: Load individual grades if aggregates fail
                            loadAnalyticsFromIndividualGrades(user.id)
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "User not found"
                        )
                    }
                }.onFailure { exception ->
                    // If cache exists, use it; otherwise show error
                    if (cachedAggregates.isNotEmpty() && studentDataCache.isCacheValid()) {
                        val analytics = calculateAnalytics(cachedAggregates)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            overallAverage = analytics.overallAverage,
                            passingSubjects = analytics.passingSubjects,
                            atRiskSubjects = analytics.atRiskSubjects,
                            failingSubjects = analytics.failingSubjects
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
                    error = e.message ?: "Failed to load analytics data"
                )
            }
        }
    }
    
    private suspend fun loadAnalyticsFromIndividualGrades(studentId: String) {
        try {
            // Check cache first
            val cachedGrades = studentDataCache.cachedGrades.value
            val studentGrades = if (cachedGrades.isNotEmpty() && studentDataCache.isCacheValid()) {
                cachedGrades.filter { it.studentId == studentId }
            } else {
                emptyList()
            }
            
            // Load individual grades if not in cache
            val gradesResult = if (studentGrades.isEmpty()) {
                gradeRepository.getGradesByStudent(studentId)
            } else {
                kotlin.Result.success(studentGrades)
            }
            
            gradesResult.onSuccess { gradesList ->
                if (gradesList.isNotEmpty()) {
                    // Update cache
                    studentDataCache.updateGrades(gradesList)
                    
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
                    studentDataCache.updateGradeAggregates(aggregates)
                    
                    // Calculate analytics
                    val analytics = calculateAnalytics(aggregates)
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        overallAverage = analytics.overallAverage,
                        passingSubjects = analytics.passingSubjects,
                        atRiskSubjects = analytics.atRiskSubjects,
                        failingSubjects = analytics.failingSubjects
                    )
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
        
        // Calculate final average using weighted formula (30% prelim, 30% midterm, 40% final)
        val finalAverage = com.smartacademictracker.data.utils.GradeCalculationEngine.calculateFinalAverage(
            prelimGrade?.percentage,
            midtermGrade?.percentage,
            finalGrade?.percentage
        )
        
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
        loadAnalyticsData(forceRefresh = true)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun setSelectedSubject(subjectId: String?) {
        _uiState.value = _uiState.value.copy(selectedSubjectId = subjectId)
        // Recalculate analytics for the selected subject
        val filteredAggregates = if (subjectId == null) {
            _gradeAggregates.value
        } else {
            _gradeAggregates.value.filter { it.subjectId == subjectId }
        }
        val analytics = calculateAnalytics(filteredAggregates)
        _uiState.value = _uiState.value.copy(
            overallAverage = analytics.overallAverage,
            passingSubjects = analytics.passingSubjects,
            atRiskSubjects = analytics.atRiskSubjects,
            failingSubjects = analytics.failingSubjects
        )
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
    val failingSubjects: Int = 0,
    val selectedSubjectId: String? = null // null = overall, otherwise specific subject ID
)

private data class AnalyticsData(
    val overallAverage: Double?,
    val passingSubjects: Int,
    val atRiskSubjects: Int,
    val failingSubjects: Int
)
