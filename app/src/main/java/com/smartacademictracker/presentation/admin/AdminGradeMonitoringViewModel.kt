package com.smartacademictracker.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.repository.GradeRepository
import com.smartacademictracker.data.repository.EnrollmentRepository
import com.smartacademictracker.data.model.GradeStatus
import com.smartacademictracker.data.model.StudentGradeAggregate
import com.smartacademictracker.data.model.Grade
import com.smartacademictracker.data.model.Enrollment
import com.smartacademictracker.data.model.GradePeriod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminGradeMonitoringViewModel @Inject constructor(
    private val gradeRepository: GradeRepository,
    private val enrollmentRepository: EnrollmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminGradeMonitoringUiState())
    val uiState: StateFlow<AdminGradeMonitoringUiState> = _uiState.asStateFlow()

    private val _gradeAggregates = MutableStateFlow<List<StudentGradeAggregate>>(emptyList())
    val gradeAggregates: StateFlow<List<StudentGradeAggregate>> = _gradeAggregates.asStateFlow()

    fun loadGradeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // Load all grades and create aggregates
                val gradesResult = gradeRepository.getAllGrades()
                val enrollmentsResult = enrollmentRepository.getAllEnrollments()

                gradesResult.onSuccess { gradesList ->
                    enrollmentsResult.onSuccess { enrollmentsList ->
                        // Create grade aggregates from individual grades
                        val aggregates = createGradeAggregates(gradesList, enrollmentsList)
                        _gradeAggregates.value = aggregates

                        // Calculate statistics
                        val totalStudents = aggregates.size
                        val atRiskStudents = aggregates.count { it.status == GradeStatus.AT_RISK }
                        val passingStudents = aggregates.count { it.status == GradeStatus.PASSING }
                        val failingStudents = aggregates.count { it.status == GradeStatus.FAILING }

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            totalStudents = totalStudents,
                            atRiskStudents = atRiskStudents,
                            passingStudents = passingStudents,
                            failingStudents = failingStudents
                        )

                        println("DEBUG: AdminGradeMonitoringViewModel - Loaded $totalStudents grade aggregates")
                    }.onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load enrollments"
                        )
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load grades"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load grade data"
                )
            }
        }
    }

    private fun createGradeAggregates(
        grades: List<Grade>,
        enrollments: List<Enrollment>
    ): List<StudentGradeAggregate> {
        // Group grades by student and subject
        val groupedGrades = grades.groupBy { grade ->
            "${grade.studentId}_${grade.subjectId}"
        }

        return groupedGrades.mapNotNull { (key, studentGrades) ->
            val firstGrade = studentGrades.firstOrNull() ?: return@mapNotNull null
            val enrollment = enrollments.find {
                it.studentId == firstGrade.studentId && it.subjectId == firstGrade.subjectId
            }

            // Extract grades by period
            val prelimGrade = studentGrades.find { it.gradePeriod == GradePeriod.PRELIM }?.score
            val midtermGrade = studentGrades.find { it.gradePeriod == GradePeriod.MIDTERM }?.score
            val finalGrade = studentGrades.find { it.gradePeriod == GradePeriod.FINAL }?.score

            // Create aggregate
            StudentGradeAggregate(
                id = key,
                studentId = firstGrade.studentId,
                studentName = firstGrade.studentName,
                subjectId = firstGrade.subjectId,
                subjectName = firstGrade.subjectName,
                teacherId = firstGrade.teacherId,
                prelimGrade = prelimGrade,
                midtermGrade = midtermGrade,
                finalGrade = finalGrade,
                finalAverage = null, // Will be calculated
                status = GradeStatus.INCOMPLETE, // Will be determined
                letterGrade = "",
                semester = firstGrade.semester,
                academicYear = firstGrade.academicYear,
                lastUpdated = studentGrades.maxOfOrNull { it.dateRecorded } ?: System.currentTimeMillis()
            ).let { aggregate ->
                // Calculate final average and status
                val finalAverage = aggregate.calculateFinalAverage()
                val status = aggregate.determineGradeStatus()
                val letterGrade = aggregate.calculateLetterGrade()

                aggregate.copy(
                    finalAverage = finalAverage,
                    status = status,
                    letterGrade = letterGrade
                )
            }
        }
    }

    fun refreshGradeData() {
        loadGradeData()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class AdminGradeMonitoringUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalStudents: Int = 0,
    val atRiskStudents: Int = 0,
    val passingStudents: Int = 0,
    val failingStudents: Int = 0
)