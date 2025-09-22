package com.smartacademictracker.presentation.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.Subject
import com.smartacademictracker.data.model.Enrollment
import com.smartacademictracker.data.model.Grade
import com.smartacademictracker.data.model.GradeType
import com.smartacademictracker.data.repository.SubjectRepository
import com.smartacademictracker.data.repository.EnrollmentRepository
import com.smartacademictracker.data.repository.GradeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeacherGradeInputViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val gradeRepository: GradeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherGradeInputUiState())
    val uiState: StateFlow<TeacherGradeInputUiState> = _uiState.asStateFlow()

    private val _subject = MutableStateFlow<Subject?>(null)
    val subject: StateFlow<Subject?> = _subject.asStateFlow()

    private val _enrollments = MutableStateFlow<List<Enrollment>>(emptyList())
    val enrollments: StateFlow<List<Enrollment>> = _enrollments.asStateFlow()

    private val _grades = MutableStateFlow<List<Grade>>(emptyList())
    val grades: StateFlow<List<Grade>> = _grades.asStateFlow()

    fun loadSubjectAndStudents(subjectId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Load subject details
                val subjectResult = subjectRepository.getSubjectById(subjectId)
                subjectResult.onSuccess { subject ->
                    _subject.value = subject
                }

                // Load enrollments for this subject
                val enrollmentsResult = enrollmentRepository.getEnrollmentsBySubject(subjectId)
                enrollmentsResult.onSuccess { enrollmentsList ->
                    _enrollments.value = enrollmentsList
                }

                // Load existing grades for this subject
                val gradesResult = gradeRepository.getGradesBySubject(subjectId)
                gradesResult.onSuccess { gradesList ->
                    _grades.value = gradesList
                }

                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load subject data"
                )
            }
        }
    }

    fun updateGrade(studentId: String, gradeType: GradeType, value: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val subject = _subject.value
                if (subject != null) {
                    // Check if grade already exists
                    val existingGrade = _grades.value.find { 
                        it.studentId == studentId && it.gradeType == gradeType 
                    }

                    val grade = if (existingGrade != null) {
                        // Update existing grade
                        existingGrade.copy(score = value)
                    } else {
                        // Create new grade
                        Grade(
                            studentId = studentId,
                            subjectId = subject.id,
                            gradeType = gradeType,
                            score = value,
                            maxScore = 100.0
                        )
                    }

                    val result = if (existingGrade != null) {
                        gradeRepository.updateGrade(grade)
                    } else {
                        gradeRepository.createGrade(grade)
                    }

                    result.onSuccess {
                        // Update local grades list
                        val updatedGrades = if (existingGrade != null) {
                            _grades.value.map { if (it.id == grade.id) grade else it }
                        } else {
                            _grades.value + grade
                        }
                        _grades.value = updatedGrades

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            successMessage = "Grade saved successfully!"
                        )
                    }.onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to save grade"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Subject not found"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to update grade"
                )
            }
        }
    }

    private fun getGradeWeight(gradeType: GradeType): Double {
        return when (gradeType) {
            GradeType.QUIZ -> 0.2
            GradeType.ACTIVITY -> 0.3
            GradeType.EXAM -> 0.5
            GradeType.PROJECT -> 0.3
            GradeType.HOMEWORK -> 0.1
            GradeType.PARTICIPATION -> 0.1
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}

data class TeacherGradeInputUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
