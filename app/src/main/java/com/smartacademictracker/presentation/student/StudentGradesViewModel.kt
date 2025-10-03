package com.smartacademictracker.presentation.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.Grade
import com.smartacademictracker.data.model.StudentGradeAggregate
import com.smartacademictracker.data.model.GradePeriod
import com.smartacademictracker.data.repository.GradeRepository
import com.smartacademictracker.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentGradesViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val gradeRepository: GradeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentGradesUiState())
    val uiState: StateFlow<StudentGradesUiState> = _uiState.asStateFlow()

    private val _grades = MutableStateFlow<List<Grade>>(emptyList())
    val grades: StateFlow<List<Grade>> = _grades.asStateFlow()
    
    private val _gradeAggregates = MutableStateFlow<List<StudentGradeAggregate>>(emptyList())
    val gradeAggregates: StateFlow<List<StudentGradeAggregate>> = _gradeAggregates.asStateFlow()

    private val _studentId = MutableStateFlow<String?>(null)

    init {
        // Set up real-time data flow for grade updates
        viewModelScope.launch {
            combine(_studentId, _grades, _gradeAggregates) { studentId, grades, aggregates ->
                if (studentId != null && !_uiState.value.isLoading) {
                    val studentGrades = grades.filter { it.studentId == studentId }
                    val studentAggregates = aggregates.filter { it.studentId == studentId }
                    
                    _grades.value = studentGrades
                    _gradeAggregates.value = studentAggregates
                    
                    println("DEBUG: StudentGradesViewModel - Real-time update: ${studentGrades.size} grades, ${studentAggregates.size} aggregates")
                }
            }.collect { }
        }
    }

    fun loadGrades() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Get current user
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        _studentId.value = user.id
                        
                        // Load student's individual grades
                        val gradesResult = gradeRepository.getGradesByStudent(user.id)
                        gradesResult.onSuccess { gradesList ->
                            _grades.value = gradesList
                            println("DEBUG: StudentGradesViewModel - Loaded ${gradesList.size} individual grades")
                        }
                        
                        // Load student's grade aggregates (calculated averages per subject)
                        val aggregatesResult = gradeRepository.getStudentGradeAggregatesByStudent(user.id)
                        aggregatesResult.onSuccess { aggregatesList ->
                            _gradeAggregates.value = aggregatesList
                            println("DEBUG: StudentGradesViewModel - Loaded ${aggregatesList.size} grade aggregates")
                            _uiState.value = _uiState.value.copy(isLoading = false)
                        }.onFailure { exception ->
                            // If aggregates fail, still show individual grades
                            println("DEBUG: StudentGradesViewModel - Failed to load aggregates: ${exception.message}")
                            _uiState.value = _uiState.value.copy(isLoading = false)
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
        loadGrades()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun updateGrades(grades: List<Grade>) {
        _grades.value = grades
        println("DEBUG: StudentGradesViewModel - Updated grades: ${grades.size} total")
    }
    
    fun updateGradeAggregates(aggregates: List<StudentGradeAggregate>) {
        _gradeAggregates.value = aggregates
        println("DEBUG: StudentGradesViewModel - Updated grade aggregates: ${aggregates.size} total")
    }
}

data class StudentGradesUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
