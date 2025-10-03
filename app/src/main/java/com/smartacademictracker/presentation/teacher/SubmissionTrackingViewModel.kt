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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubmissionTrackingViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val gradeRepository: GradeRepository,
    private val userRepository: UserRepository,
    private val offlineGradeRepository: OfflineGradeRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubmissionTrackingUiState())
    val uiState: StateFlow<SubmissionTrackingUiState> = _uiState.asStateFlow()

    private val _subject = MutableStateFlow<Subject?>(null)
    val subject: StateFlow<Subject?> = _subject.asStateFlow()

    private val _submissionStatus = MutableStateFlow(SubmissionStatusSummary(
        totalStudents = 0,
        submittedCount = 0,
        pendingCount = 0,
        students = emptyList()
    ))
    val submissionStatus: StateFlow<SubmissionStatusSummary> = _submissionStatus.asStateFlow()
    
    private val _selectedPeriod = MutableStateFlow(GradePeriod.PRELIM)
    val selectedPeriod: StateFlow<GradePeriod> = _selectedPeriod.asStateFlow()
    
    private val _filterStatus = MutableStateFlow(FilterStatus())
    val filterStatus: StateFlow<FilterStatus> = _filterStatus.asStateFlow()

    fun loadSubmissionStatus(subjectId: String) {
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
                    loadSubmissionStatusForStudents(subjectId, enrollments)
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load enrollments"
                    )
                    return@launch
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load submission status"
                )
            }
        }
    }
    
    private suspend fun loadSubmissionStatusForStudents(subjectId: String, enrollments: List<Enrollment>) {
        try {
            val selectedPeriod = _selectedPeriod.value
            val studentSubmissionStatuses = mutableListOf<StudentSubmissionStatus>()
            var submittedCount = 0
            var pendingCount = 0
            
            for (enrollment in enrollments) {
                // Check if grade exists for this student and period
                if (!networkMonitor.isCurrentlyConnected()) {
                    // Offline mode - use Flow
                    offlineGradeRepository.getGradesByStudent(enrollment.studentId)
                        .collect { grades ->
                            val periodGrade = grades.find { 
                                it.subjectId == subjectId && it.gradePeriod == selectedPeriod 
                            }
                            
                            val isSubmitted = periodGrade != null && periodGrade.score > 0
                            val gradePercentage = periodGrade?.percentage ?: 0.0
                            val lastUpdated = periodGrade?.dateRecorded ?: 0L
                    
                    if (isSubmitted) {
                        submittedCount++
                    } else {
                        pendingCount++
                    }
                    
                    studentSubmissionStatuses.add(
                        StudentSubmissionStatus(
                            studentId = enrollment.studentId,
                            studentName = enrollment.studentName,
                            isSubmitted = isSubmitted,
                            gradePercentage = gradePercentage,
                            lastUpdated = lastUpdated,
                            hasError = false
                        )
                    )
                }
                } else {
                    // Online mode - use Result
                    val gradesResult = gradeRepository.getGradesByStudent(enrollment.studentId)
                    if (gradesResult.isSuccess) {
                        val grades = gradesResult.getOrNull() ?: emptyList()
                        val periodGrade = grades.find { 
                            it.subjectId == subjectId && it.gradePeriod == selectedPeriod 
                        }
                        
                        val isSubmitted = periodGrade != null && periodGrade.score > 0
                        val gradePercentage = periodGrade?.percentage ?: 0.0
                        val lastUpdated = periodGrade?.dateRecorded ?: 0L
                        
                        if (isSubmitted) {
                            submittedCount++
                        } else {
                            pendingCount++
                        }
                        
                        studentSubmissionStatuses.add(
                            StudentSubmissionStatus(
                                studentId = enrollment.studentId,
                                studentName = enrollment.studentName,
                                isSubmitted = isSubmitted,
                                gradePercentage = gradePercentage,
                                lastUpdated = lastUpdated,
                                hasError = false
                            )
                        )
                    }
                }
            }
            
            _submissionStatus.value = SubmissionStatusSummary(
                totalStudents = enrollments.size,
                submittedCount = submittedCount,
                pendingCount = pendingCount,
                students = studentSubmissionStatuses
            )
            
            _uiState.value = _uiState.value.copy(isLoading = false)
            
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = e.message ?: "Failed to load submission status"
            )
        }
    }
    
    fun setSelectedPeriod(period: GradePeriod) {
        _selectedPeriod.value = period
        _subject.value?.id?.let { loadSubmissionStatus(it) }
    }
    
    fun toggleFilterMenu() {
        _filterStatus.value = _filterStatus.value.copy(
            showFilterMenu = !_filterStatus.value.showFilterMenu
        )
    }
    
    fun setStatusFilter(filter: SubmissionStatusFilter) {
        _filterStatus.value = _filterStatus.value.copy(
            statusFilter = filter,
            showFilterMenu = false
        )
    }
    
    fun retrySubmission(studentId: String) {
        viewModelScope.launch {
            try {
                // This would typically retry a failed submission
                // For now, just refresh the status
                _subject.value?.id?.let { loadSubmissionStatus(it) }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to retry submission"
                )
            }
        }
    }
    
    fun refreshSubmissionStatus() {
        _subject.value?.id?.let { loadSubmissionStatus(it) }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class SubmissionTrackingUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
