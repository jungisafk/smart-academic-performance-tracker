package com.smartacademictracker.presentation.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.Subject
import com.smartacademictracker.data.model.Enrollment
import com.smartacademictracker.data.model.Grade
import com.smartacademictracker.data.model.GradePeriod
import com.smartacademictracker.data.model.StudentGradeAggregate
import com.smartacademictracker.data.repository.SubjectRepository
import com.smartacademictracker.data.repository.EnrollmentRepository
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
class TeacherGradeInputViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val gradeRepository: GradeRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherGradeInputUiState())
    val uiState: StateFlow<TeacherGradeInputUiState> = _uiState.asStateFlow()

    private val _subject = MutableStateFlow<Subject?>(null)
    val subject: StateFlow<Subject?> = _subject.asStateFlow()

    private val _enrollments = MutableStateFlow<List<Enrollment>>(emptyList())
    val enrollments: StateFlow<List<Enrollment>> = _enrollments.asStateFlow()

    private val _grades = MutableStateFlow<List<Grade>>(emptyList())
    val grades: StateFlow<List<Grade>> = _grades.asStateFlow()
    
    private val _gradeAggregates = MutableStateFlow<List<StudentGradeAggregate>>(emptyList())
    val gradeAggregates: StateFlow<List<StudentGradeAggregate>> = _gradeAggregates.asStateFlow()
    
    private val _selectedPeriod = MutableStateFlow(GradePeriod.PRELIM)
    val selectedPeriod: StateFlow<GradePeriod> = _selectedPeriod.asStateFlow()

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
                
                // Load student grade aggregates for this subject
                val aggregatesResult = gradeRepository.getStudentGradeAggregatesBySubject(subjectId)
                aggregatesResult.onSuccess { aggregatesList ->
                    _gradeAggregates.value = aggregatesList
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
    
    fun setSelectedPeriod(period: GradePeriod) {
        _selectedPeriod.value = period
    }

    fun updateGradeForPeriod(studentId: String, gradePeriod: GradePeriod, value: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val subject = _subject.value
                if (subject != null) {
                    // Validate grade input
                    if (!GradeCalculationEngine.isValidGrade(value)) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Grade must be between 0 and 100"
                        )
                        return@launch
                    }
                    
                    // Get student information
                    val enrollment = _enrollments.value.find { it.studentId == studentId }
                    val studentName = enrollment?.studentName ?: "Unknown Student"
                    
                    // Check if grade already exists
                    val existingGradeResult = gradeRepository.getGradesByStudentSubjectAndPeriod(
                        studentId, subject.id, gradePeriod
                    )
                    
                    val existingGrade = existingGradeResult.getOrNull()
                    
                    val grade = if (existingGrade != null) {
                        // Update existing grade
                        existingGrade.copy(
                            score = value,
                            percentage = value, // Since maxScore is 100, percentage equals score
                            letterGrade = GradeCalculationEngine.calculateLetterGrade(value)
                        )
                    } else {
                        // Create new grade
                        Grade(
                            studentId = studentId,
                            studentName = studentName,
                            subjectId = subject.id,
                            subjectName = subject.name,
                            teacherId = subject.teacherId ?: "",
                            gradePeriod = gradePeriod,
                            score = value,
                            maxScore = 100.0,
                            percentage = value,
                            letterGrade = GradeCalculationEngine.calculateLetterGrade(value),
                            semester = "Fall 2025", // TODO: Get from current academic period
                            academicYear = "2025-2026" // TODO: Get from current academic period
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
                        
                        // Update or create student grade aggregate
                        updateStudentGradeAggregate(studentId, studentName, subject)

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            successMessage = "${gradePeriod.displayName} grade saved successfully!"
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
    
    private suspend fun updateStudentGradeAggregate(studentId: String, studentName: String, subject: Subject) {
        try {
            val aggregateResult = gradeRepository.createOrUpdateStudentGradeAggregate(
                studentId = studentId,
                subjectId = subject.id,
                studentName = studentName,
                subjectName = subject.name,
                teacherId = subject.teacherId ?: "",
                semester = "Fall 2025", // TODO: Get from current academic period
                academicYear = "2025-2026" // TODO: Get from current academic period
            )
            
            aggregateResult.onSuccess { updatedAggregate ->
                // Update local aggregates list
                val updatedAggregates = _gradeAggregates.value.toMutableList()
                val existingIndex = updatedAggregates.indexOfFirst { it.id == updatedAggregate.id }
                
                if (existingIndex >= 0) {
                    updatedAggregates[existingIndex] = updatedAggregate
                } else {
                    updatedAggregates.add(updatedAggregate)
                }
                
                _gradeAggregates.value = updatedAggregates
            }
        } catch (e: Exception) {
            // Log error but don't fail the main operation
            println("Failed to update grade aggregate: ${e.message}")
        }
    }
    
    fun getGradeForStudentAndPeriod(studentId: String, gradePeriod: GradePeriod): Grade? {
        return _grades.value.find { 
            it.studentId == studentId && it.gradePeriod == gradePeriod 
        }
    }
    
    fun getStudentGradeAggregate(studentId: String): StudentGradeAggregate? {
        return _gradeAggregates.value.find { it.studentId == studentId }
    }

    fun refreshData() {
        _subject.value?.let { subject ->
            loadSubjectAndStudents(subject.id)
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
    val successMessage: String? = null,
    val savingGrades: Set<String> = emptySet() // Track which student grades are being saved
)
