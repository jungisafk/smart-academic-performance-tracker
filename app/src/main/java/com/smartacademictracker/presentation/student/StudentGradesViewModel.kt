package com.smartacademictracker.presentation.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.Grade
import com.smartacademictracker.data.model.StudentGradeAggregate
import com.smartacademictracker.data.model.GradePeriod
import com.smartacademictracker.data.repository.GradeRepository
import com.smartacademictracker.data.repository.UserRepository
import com.smartacademictracker.data.manager.StudentDataCache
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentGradesViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val gradeRepository: GradeRepository,
    private val studentDataCache: StudentDataCache
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentGradesUiState())
    val uiState: StateFlow<StudentGradesUiState> = _uiState.asStateFlow()

    private val _grades = MutableStateFlow<List<Grade>>(emptyList())
    val grades: StateFlow<List<Grade>> = _grades.asStateFlow()
    
    private val _gradeAggregates = MutableStateFlow<List<StudentGradeAggregate>>(emptyList())
    val gradeAggregates: StateFlow<List<StudentGradeAggregate>> = _gradeAggregates.asStateFlow()

    private val _studentId = MutableStateFlow<String?>(null)
    private var gradesFlowJob: Job? = null

    init {
        // Load cached data immediately if available
        val cachedGrades = studentDataCache.cachedGrades.value
        val cachedAggregates = studentDataCache.cachedGradeAggregates.value
        if (cachedGrades.isNotEmpty() && studentDataCache.isCacheValid()) {
            _grades.value = cachedGrades
        }
        if (cachedAggregates.isNotEmpty() && studentDataCache.isCacheValid()) {
            _gradeAggregates.value = cachedAggregates
        }
    }

    fun loadGrades(forceRefresh: Boolean = false) {
        // Prevent duplicate loads if already loading
        if (_uiState.value.isLoading && !forceRefresh) {
            return
        }
        
        viewModelScope.launch {
            // Check cache first
            val cachedGrades = studentDataCache.cachedGrades.value
            val cachedAggregates = studentDataCache.cachedGradeAggregates.value
            
            // Only show loading if no cached data or cache is invalid
            if (forceRefresh || !studentDataCache.isCacheValid() || cachedGrades.isEmpty()) {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }
            
            try {
                // Get current user
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        _studentId.value = user.id
                        
                        // Use cached data immediately if available and valid
                        if (!forceRefresh && studentDataCache.isCacheValid() && cachedGrades.isNotEmpty()) {
                            _grades.value = cachedGrades.filter { it.studentId == user.id }
                            if (cachedAggregates.isNotEmpty()) {
                                _gradeAggregates.value = cachedAggregates.filter { it.studentId == user.id }
                            }
                            _uiState.value = _uiState.value.copy(isLoading = false)
                        }
                        
                        // Cancel any existing flow job
                        gradesFlowJob?.cancel()
                        
                        // Set up real-time listener for grades
                        gradesFlowJob = viewModelScope.launch {
                            gradeRepository.getGradesByStudentFlow(user.id)
                                .catch { exception ->
                                    _uiState.value = _uiState.value.copy(
                                        error = exception.message ?: "Failed to load grades"
                                    )
                                }
                                .collect { gradesList ->
                                    _grades.value = gradesList
                                    studentDataCache.updateGrades(gradesList)
                                }
                        }
                        
                        // Load student's grade aggregates (calculated averages per subject)
                        val aggregatesResult = gradeRepository.getStudentGradeAggregatesByStudent(user.id)
                        aggregatesResult.onSuccess { aggregatesList ->
                            _gradeAggregates.value = aggregatesList
                            studentDataCache.updateGradeAggregates(aggregatesList)
                            _uiState.value = _uiState.value.copy(isLoading = false)
                        }.onFailure { exception ->
                            // If aggregates fail, still show individual grades
                            if (!studentDataCache.isCacheValid()) {
                                _uiState.value = _uiState.value.copy(isLoading = false)
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
                    if (cachedGrades.isNotEmpty() && studentDataCache.isCacheValid()) {
                        _grades.value = cachedGrades
                        if (cachedAggregates.isNotEmpty()) {
                            _gradeAggregates.value = cachedAggregates
                        }
                        _uiState.value = _uiState.value.copy(isLoading = false)
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
                    error = e.message ?: "Failed to load grades"
                )
            }
        }
    }
    
    fun getGradesBySubject(subjectId: String): List<Grade> {
        return _grades.value.filter { it.subjectId == subjectId }
    }
    
    fun getGradeForSubjectAndPeriod(subjectId: String, period: GradePeriod): Grade? {
        return _grades.value.find { 
            it.subjectId == subjectId && it.gradePeriod == period 
        }
    }
    
    fun getGradeAggregateForSubject(subjectId: String): StudentGradeAggregate? {
        return _gradeAggregates.value.find { it.subjectId == subjectId }
    }

    fun refreshGrades() {
        loadGrades(forceRefresh = true)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun updateGrades(grades: List<Grade>) {
        _grades.value = grades
        studentDataCache.updateGrades(grades)
    }
    
    fun updateGradeAggregates(aggregates: List<StudentGradeAggregate>) {
        _gradeAggregates.value = aggregates
        studentDataCache.updateGradeAggregates(aggregates)
    }
}

data class StudentGradesUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
