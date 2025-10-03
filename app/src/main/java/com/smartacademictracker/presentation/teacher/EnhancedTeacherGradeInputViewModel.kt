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
import com.smartacademictracker.data.repository.OfflineGradeRepository
import com.smartacademictracker.data.network.NetworkMonitor
import com.smartacademictracker.data.utils.GradeCalculationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EnhancedTeacherGradeInputViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val gradeRepository: GradeRepository,
    private val userRepository: UserRepository,
    private val offlineGradeRepository: OfflineGradeRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(EnhancedTeacherGradeInputUiState())
    val uiState: StateFlow<EnhancedTeacherGradeInputUiState> = _uiState.asStateFlow()

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
    
    private val _batchMode = MutableStateFlow(false)
    val batchMode: StateFlow<Boolean> = _batchMode.asStateFlow()
    
    private val _offlineMode = MutableStateFlow(false)
    val offlineMode: StateFlow<Boolean> = _offlineMode.asStateFlow()
    
    private val _pendingGrades = MutableStateFlow<Map<String, PendingGrade>>(emptyMap())
    val pendingGrades: StateFlow<Map<String, PendingGrade>> = _pendingGrades.asStateFlow()

    init {
        // Monitor network status
        viewModelScope.launch {
            networkMonitor.isOnline().collect { isOnline ->
                _offlineMode.value = !isOnline
            }
        }
    }

    fun loadSubjectAndStudents(subjectId: String) {
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
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load enrollments"
                    )
                    return@launch
                }
                
                // Load grades
                loadGrades(subjectId)
                
                _uiState.value = _uiState.value.copy(isLoading = false)
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load data"
                )
            }
        }
    }
    
    private suspend fun loadGrades(subjectId: String) {
        try {
            val currentUserResult = userRepository.getCurrentUser()
            currentUserResult.onSuccess { user ->
                if (user != null) {
                    // Load grades for all students in this subject
                    val enrollments = _enrollments.value
                    val allGrades = mutableListOf<Grade>()
                    
                    for (enrollment in enrollments) {
                if (_offlineMode.value) {
                    // Offline mode - use Flow
                    offlineGradeRepository.getGradesByStudent(enrollment.studentId)
                        .collect { studentGrades ->
                            val subjectGrades = studentGrades.filter { it.subjectId == subjectId }
                            allGrades.addAll(subjectGrades)
                        }
                } else {
                    // Online mode - use Result
                    val gradesResult = gradeRepository.getGradesByStudent(enrollment.studentId)
                    if (gradesResult.isSuccess) {
                        val studentGrades = gradesResult.getOrNull() ?: emptyList()
                        val subjectGrades = studentGrades.filter { it.subjectId == subjectId }
                        allGrades.addAll(subjectGrades)
                    }
                }
            }
                    
                    _grades.value = allGrades
                    
                    // Create grade aggregates
                    val aggregates = createGradeAggregates(allGrades, enrollments)
                    _gradeAggregates.value = aggregates
                }
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = e.message ?: "Failed to load grades"
            )
        }
    }
    
    private fun createGradeAggregates(
        grades: List<Grade>,
        enrollments: List<Enrollment>
    ): List<StudentGradeAggregate> {
        val groupedGrades = grades.groupBy { "${it.studentId}_${it.subjectId}" }
        
        return groupedGrades.mapNotNull { (key, studentGrades) ->
            val firstGrade = studentGrades.firstOrNull() ?: return@mapNotNull null
            val enrollment = enrollments.find {
                it.studentId == firstGrade.studentId && it.subjectId == firstGrade.subjectId
            } ?: return@mapNotNull null
            
            val prelimGrade = studentGrades.find { it.gradePeriod == GradePeriod.PRELIM }
            val midtermGrade = studentGrades.find { it.gradePeriod == GradePeriod.MIDTERM }
            val finalGrade = studentGrades.find { it.gradePeriod == GradePeriod.FINAL }
            
            val finalAverage = GradeCalculationEngine.calculateFinalAverage(
                prelimGrade?.percentage,
                midtermGrade?.percentage,
                finalGrade?.percentage
            )
            
            val status = GradeCalculationEngine.determineGradeStatus(finalAverage)
            val letterGrade = GradeCalculationEngine.calculateLetterGrade(finalAverage ?: 0.0)
            
            StudentGradeAggregate(
                studentId = firstGrade.studentId,
                studentName = enrollment.studentName,
                subjectId = firstGrade.subjectId,
                subjectName = firstGrade.subjectName,
                academicYear = firstGrade.academicYear,
                semester = firstGrade.semester,
                prelimGrade = prelimGrade?.percentage,
                midtermGrade = midtermGrade?.percentage,
                finalGrade = finalGrade?.percentage,
                finalAverage = finalAverage,
                status = status,
                letterGrade = letterGrade
            )
        }
    }

    fun setSelectedPeriod(period: GradePeriod) {
        _selectedPeriod.value = period
    }
    
    fun toggleBatchMode() {
        _batchMode.value = !_batchMode.value
    }
    
    fun updateGrade(studentId: String, period: GradePeriod, score: Double, maxScore: Double) {
        val key = "${studentId}_${period.name}"
        val pendingGrade = PendingGrade(
            studentId = studentId,
            subjectId = _subject.value?.id ?: "",
            gradePeriod = period,
            score = score,
            maxScore = maxScore,
            percentage = if (maxScore > 0) (score / maxScore) * 100 else 0.0,
            letterGrade = GradeCalculationEngine.calculateLetterGrade((score / maxScore) * 100),
            dateRecorded = System.currentTimeMillis()
        )
        
        _pendingGrades.value = _pendingGrades.value + (key to pendingGrade)
    }
    
    fun saveGrade(studentId: String, period: GradePeriod) {
        val key = "${studentId}_${period.name}"
        val pendingGrade = _pendingGrades.value[key]
        
        if (pendingGrade != null) {
            viewModelScope.launch {
                try {
                    val grade = Grade(
                        id = "${studentId}_${period.name}_${System.currentTimeMillis()}",
                        studentId = studentId,
                        studentName = _enrollments.value.find { it.studentId == studentId }?.studentName ?: "",
                        subjectId = pendingGrade.subjectId,
                        subjectName = _subject.value?.name ?: "",
                        teacherId = _subject.value?.teacherId ?: "",
                        gradePeriod = period,
                        score = pendingGrade.score,
                        maxScore = pendingGrade.maxScore,
                        percentage = pendingGrade.percentage,
                        letterGrade = pendingGrade.letterGrade,
                        description = "",
                        dateRecorded = pendingGrade.dateRecorded,
                        semester = _subject.value?.semester ?: "",
                        academicYear = _subject.value?.academicYear ?: ""
                    )
                    
                    val result = if (_offlineMode.value) {
                        offlineGradeRepository.createGrade(grade)
                    } else {
                        gradeRepository.createGrade(grade)
                    }
                    
                    result.onSuccess {
                        // Remove from pending
                        _pendingGrades.value = _pendingGrades.value - key
                        // Reload grades
                        _subject.value?.id?.let { loadGrades(it) }
                    }.onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            error = exception.message ?: "Failed to save grade"
                        )
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Failed to save grade"
                    )
                }
            }
        }
    }
    
    fun validateGrade(score: Double, maxScore: Double): ValidationResult {
        return when {
            score < 0 -> ValidationResult.Invalid("Score cannot be negative")
            score > maxScore -> ValidationResult.Invalid("Score cannot exceed maximum")
            maxScore <= 0 -> ValidationResult.Invalid("Maximum score must be greater than 0")
            score > 100 && maxScore == 100.0 -> ValidationResult.Invalid("Score cannot exceed 100")
            else -> ValidationResult.Valid
        }
    }
    
    fun saveAllPendingGrades() {
        viewModelScope.launch {
            try {
                val pendingGradesList = _pendingGrades.value.values.toList()
                var successCount = 0
                var failureCount = 0
                
                for (pendingGrade in pendingGradesList) {
                    val grade = Grade(
                        id = "${pendingGrade.studentId}_${pendingGrade.gradePeriod.name}_${System.currentTimeMillis()}",
                        studentId = pendingGrade.studentId,
                        studentName = _enrollments.value.find { it.studentId == pendingGrade.studentId }?.studentName ?: "",
                        subjectId = pendingGrade.subjectId,
                        subjectName = _subject.value?.name ?: "",
                        teacherId = _subject.value?.teacherId ?: "",
                        gradePeriod = pendingGrade.gradePeriod,
                        score = pendingGrade.score,
                        maxScore = pendingGrade.maxScore,
                        percentage = pendingGrade.percentage,
                        letterGrade = pendingGrade.letterGrade,
                        description = "",
                        dateRecorded = pendingGrade.dateRecorded,
                        semester = _subject.value?.semester ?: "",
                        academicYear = _subject.value?.academicYear ?: ""
                    )
                    
                    val result = if (_offlineMode.value) {
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
                
                // Clear pending grades
                _pendingGrades.value = emptyMap()
                
                // Reload grades
                _subject.value?.id?.let { loadGrades(it) }
                
                _uiState.value = _uiState.value.copy(
                    message = "Saved $successCount grades successfully. $failureCount failed."
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to save grades"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}

data class EnhancedTeacherGradeInputUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

data class PendingGrade(
    val studentId: String,
    val subjectId: String,
    val gradePeriod: GradePeriod,
    val score: Double,
    val maxScore: Double,
    val percentage: Double,
    val letterGrade: String,
    val dateRecorded: Long
)
