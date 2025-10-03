package com.smartacademictracker.presentation.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.Subject
import com.smartacademictracker.data.model.Enrollment
import com.smartacademictracker.data.model.Grade
import com.smartacademictracker.data.model.GradePeriod
import com.smartacademictracker.data.repository.SubjectRepository
import com.smartacademictracker.data.repository.EnrollmentRepository
import com.smartacademictracker.data.repository.GradeRepository
import com.smartacademictracker.data.repository.UserRepository
import com.smartacademictracker.data.repository.OfflineGradeRepository
import com.smartacademictracker.data.network.NetworkMonitor
import com.smartacademictracker.data.utils.GradeCalculationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BatchGradeInputViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val gradeRepository: GradeRepository,
    private val userRepository: UserRepository,
    private val offlineGradeRepository: OfflineGradeRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(BatchGradeInputUiState())
    val uiState: StateFlow<BatchGradeInputUiState> = _uiState.asStateFlow()

    private val _subject = MutableStateFlow<Subject?>(null)
    val subject: StateFlow<Subject?> = _subject.asStateFlow()

    private val _enrollments = MutableStateFlow<List<Enrollment>>(emptyList())
    val enrollments: StateFlow<List<Enrollment>> = _enrollments.asStateFlow()

    private val _batchGrades = MutableStateFlow<Map<String, BatchGrade>>(emptyMap())
    val batchGrades: StateFlow<Map<String, BatchGrade>> = _batchGrades.asStateFlow()
    
    private val _validationResults = MutableStateFlow<Map<String, ValidationResult>>(emptyMap())
    val validationResults: StateFlow<Map<String, ValidationResult>> = _validationResults.asStateFlow()

    fun loadSubjectAndStudents(subjectId: String, gradePeriod: GradePeriod) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Load subject
                val subjectResult = subjectRepository.getSubjectById(subjectId)
                subjectResult.onSuccess { subject ->
                    _subject.value = subject
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load subject"
                    )
                    return@launch
                }
                
                // Load enrollments
                val enrollmentsResult = enrollmentRepository.getEnrollmentsBySubject(subjectId)
                enrollmentsResult.onSuccess { enrollments ->
                    _enrollments.value = enrollments
                    
                    // Initialize batch grades
                    val initialBatchGrades = enrollments.associate { enrollment ->
                        enrollment.studentId to BatchGrade(
                            studentId = enrollment.studentId,
                            score = 0.0,
                            maxScore = 100.0,
                            percentage = 0.0,
                            letterGrade = ""
                        )
                    }
                    _batchGrades.value = initialBatchGrades
                    
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load enrollments"
                    )
                    return@launch
                }
                
                _uiState.value = _uiState.value.copy(isLoading = false)
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load data"
                )
            }
        }
    }
    
    fun updateBatchGrade(studentId: String, score: Double, maxScore: Double) {
        val percentage = if (maxScore > 0) (score / maxScore) * 100 else 0.0
        val letterGrade = GradeCalculationEngine.calculateLetterGrade(percentage)
        
        val batchGrade = BatchGrade(
            studentId = studentId,
            score = score,
            maxScore = maxScore,
            percentage = percentage,
            letterGrade = letterGrade
        )
        
        _batchGrades.value = _batchGrades.value + (studentId to batchGrade)
        
        // Validate the grade
        val validationResult = validateGrade(score, maxScore)
        _validationResults.value = _validationResults.value + (studentId to validationResult)
    }
    
    fun setBatchScore(score: String) {
        _uiState.value = _uiState.value.copy(batchScore = score)
    }
    
    fun setBatchMaxScore(maxScore: String) {
        _uiState.value = _uiState.value.copy(batchMaxScore = maxScore)
    }
    
    fun applyBatchScores() {
        val score = _uiState.value.batchScore.toDoubleOrNull() ?: 0.0
        val maxScore = _uiState.value.batchMaxScore.toDoubleOrNull() ?: 100.0
        
        if (score >= 0 && maxScore > 0) {
            val enrollments = _enrollments.value
            val updatedBatchGrades = enrollments.associate { enrollment ->
                enrollment.studentId to BatchGrade(
                    studentId = enrollment.studentId,
                    score = score,
                    maxScore = maxScore,
                    percentage = if (maxScore > 0) (score / maxScore) * 100 else 0.0,
                    letterGrade = GradeCalculationEngine.calculateLetterGrade((score / maxScore) * 100)
                )
            }
            _batchGrades.value = updatedBatchGrades
            
            // Validate all grades
            val validationResults = enrollments.associate { enrollment ->
                enrollment.studentId to validateGrade(score, maxScore)
            }
            _validationResults.value = validationResults
        }
    }
    
    fun setAllToPassing() {
        applyBatchScore(75.0, 100.0)
    }
    
    fun setAllToExcellent() {
        applyBatchScore(90.0, 100.0)
    }
    
    private fun applyBatchScore(score: Double, maxScore: Double) {
        val enrollments = _enrollments.value
        val updatedBatchGrades = enrollments.associate { enrollment ->
            enrollment.studentId to BatchGrade(
                studentId = enrollment.studentId,
                score = score,
                maxScore = maxScore,
                percentage = (score / maxScore) * 100,
                letterGrade = GradeCalculationEngine.calculateLetterGrade((score / maxScore) * 100)
            )
        }
        _batchGrades.value = updatedBatchGrades
        
        // Validate all grades
        val validationResults = enrollments.associate { enrollment ->
            enrollment.studentId to validateGrade(score, maxScore)
        }
        _validationResults.value = validationResults
    }
    
    fun clearAllGrades() {
        val enrollments = _enrollments.value
        val clearedBatchGrades = enrollments.associate { enrollment ->
            enrollment.studentId to BatchGrade(
                studentId = enrollment.studentId,
                score = 0.0,
                maxScore = 100.0,
                percentage = 0.0,
                letterGrade = ""
            )
        }
        _batchGrades.value = clearedBatchGrades
        _validationResults.value = emptyMap()
    }
    
    fun saveAllGrades() {
        viewModelScope.launch {
            try {
                val batchGradesList = _batchGrades.value.values.toList()
                val validGrades = batchGradesList.filter { 
                    it.score > 0 && _validationResults.value[it.studentId]?.isValid != false 
                }
                
                if (validGrades.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        error = "No valid grades to save"
                    )
                    return@launch
                }
                
                val subject = _subject.value
                val gradePeriod = GradePeriod.PRELIM // This should be passed as parameter
                
                if (subject == null) {
                    _uiState.value = _uiState.value.copy(
                        error = "Subject not loaded"
                    )
                    return@launch
                }
                
                var successCount = 0
                var failureCount = 0
                
                for (batchGrade in validGrades) {
                    val grade = Grade(
                        id = "${batchGrade.studentId}_${gradePeriod.name}_${System.currentTimeMillis()}",
                        studentId = batchGrade.studentId,
                        studentName = _enrollments.value.find { it.studentId == batchGrade.studentId }?.studentName ?: "",
                        subjectId = subject.id,
                        subjectName = subject.name,
                        teacherId = subject.teacherId ?: "",
                        gradePeriod = gradePeriod,
                        score = batchGrade.score,
                        maxScore = batchGrade.maxScore,
                        percentage = batchGrade.percentage,
                        letterGrade = batchGrade.letterGrade,
                        description = "Batch input",
                        dateRecorded = System.currentTimeMillis(),
                        semester = subject.semester.displayName,
                        academicYear = subject.academicYear
                    )
                    
                    val result = if (!networkMonitor.isCurrentlyConnected()) {
                        offlineGradeRepository.createGrade(grade)
                    } else {
                        gradeRepository.createGrade(grade)
                    }
                    
                    if (result.isSuccess) {
                        successCount++
                    } else {
                        failureCount++
                    }
                }
                
                _uiState.value = _uiState.value.copy(
                    message = "Saved $successCount grades successfully. $failureCount failed."
                )
                
                // Clear batch grades after successful save
                if (failureCount == 0) {
                    clearAllGrades()
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to save grades"
                )
            }
        }
    }
    
    private fun validateGrade(score: Double, maxScore: Double): ValidationResult {
        return when {
            score < 0 -> ValidationResult.Invalid("Score cannot be negative")
            score > maxScore -> ValidationResult.Invalid("Score cannot exceed maximum")
            maxScore <= 0 -> ValidationResult.Invalid("Maximum score must be greater than 0")
            score > 100 && maxScore == 100.0 -> ValidationResult.Invalid("Score cannot exceed 100")
            else -> ValidationResult.Valid
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}

data class BatchGradeInputUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val batchScore: String = "",
    val batchMaxScore: String = "100"
)
