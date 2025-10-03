package com.smartacademictracker.presentation.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.StudentGradeAggregate
import com.smartacademictracker.data.model.GradeStatus
import com.smartacademictracker.data.repository.GradeRepository
import com.smartacademictracker.data.repository.UserRepository
import com.smartacademictracker.data.repository.SubjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeacherAnalyticsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val gradeRepository: GradeRepository,
    private val subjectRepository: SubjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherAnalyticsUiState())
    val uiState: StateFlow<TeacherAnalyticsUiState> = _uiState.asStateFlow()

    private val _classPerformance = MutableStateFlow<List<SubjectPerformanceData>>(emptyList())
    val classPerformance: StateFlow<List<SubjectPerformanceData>> = _classPerformance.asStateFlow()

    fun loadAnalyticsData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Get current user (teacher)
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        // Get subjects taught by this teacher
                        val subjectsResult = subjectRepository.getSubjectsByTeacher(user.id)
                        subjectsResult.onSuccess { subjects ->
                            // Load grade aggregates for all subjects
                            loadClassPerformanceData(subjects)
                        }.onFailure { exception ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to load subjects"
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

    private suspend fun loadClassPerformanceData(subjects: List<com.smartacademictracker.data.model.Subject>) {
        val subjectPerformanceList = mutableListOf<SubjectPerformanceData>()
        var totalStudents = 0
        var totalPassingStudents = 0
        var totalAtRiskStudents = 0
        var allGrades = mutableListOf<Double>()

        for (subject in subjects) {
            val aggregatesResult = gradeRepository.getStudentGradeAggregatesBySubject(subject.id)
            aggregatesResult.onSuccess { aggregates ->
                val validAggregates = aggregates.filter { it.finalAverage != null }
                if (validAggregates.isNotEmpty()) {
                    val averageGrade = validAggregates.mapNotNull { it.finalAverage }.average()
                    val passingStudents = validAggregates.count { it.status == GradeStatus.PASSING }
                    val atRiskStudents = validAggregates.count { it.status == GradeStatus.AT_RISK }
                    val passingRate = (passingStudents.toDouble() / validAggregates.size) * 100
                    
                    val gradeDistribution = calculateGradeDistribution(validAggregates)
                    
                    subjectPerformanceList.add(
                        SubjectPerformanceData(
                            subjectName = subject.name,
                            totalStudents = validAggregates.size,
                            averageGrade = averageGrade,
                            passingRate = passingRate,
                            gradeDistribution = gradeDistribution
                        )
                    )
                    
                    totalStudents += validAggregates.size
                    totalPassingStudents += passingStudents
                    totalAtRiskStudents += atRiskStudents
                    allGrades.addAll(validAggregates.mapNotNull { it.finalAverage })
                }
            }
        }

        _classPerformance.value = subjectPerformanceList
        
        val classAverage = if (allGrades.isNotEmpty()) allGrades.average() else null
        
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            totalStudents = totalStudents,
            classAverage = classAverage,
            passingStudents = totalPassingStudents,
            atRiskStudents = totalAtRiskStudents
        )
        
        println("DEBUG: TeacherAnalyticsViewModel - Loaded performance data for ${subjects.size} subjects")
    }

    private fun calculateGradeDistribution(aggregates: List<StudentGradeAggregate>): Map<String, Int> {
        val distribution = mutableMapOf<String, Int>()
        
        aggregates.forEach { aggregate ->
            val grade = aggregate.finalAverage ?: return@forEach
            val letterGrade = when {
                grade >= 97 -> "A+"
                grade >= 93 -> "A"
                grade >= 90 -> "A-"
                grade >= 87 -> "B+"
                grade >= 83 -> "B"
                grade >= 80 -> "B-"
                grade >= 77 -> "C+"
                grade >= 73 -> "C"
                grade >= 70 -> "C-"
                grade >= 67 -> "D+"
                grade >= 65 -> "D"
                else -> "F"
            }
            distribution[letterGrade] = (distribution[letterGrade] ?: 0) + 1
        }
        
        return distribution
    }

    fun refreshData() {
        loadAnalyticsData()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class TeacherAnalyticsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalStudents: Int = 0,
    val classAverage: Double? = null,
    val passingStudents: Int = 0,
    val atRiskStudents: Int = 0
)

data class SubjectPerformanceData(
    val subjectName: String,
    val totalStudents: Int,
    val averageGrade: Double,
    val passingRate: Double,
    val gradeDistribution: Map<String, Int>
)
