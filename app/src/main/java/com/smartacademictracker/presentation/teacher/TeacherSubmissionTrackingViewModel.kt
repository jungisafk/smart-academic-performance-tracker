package com.smartacademictracker.presentation.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.AssignmentSubmission
import com.smartacademictracker.data.model.SubmissionStatus
import com.smartacademictracker.data.repository.SubmissionStatistics
import com.smartacademictracker.data.repository.AssignmentSubmissionRepository
import com.smartacademictracker.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeacherSubmissionTrackingViewModel @Inject constructor(
    private val submissionRepository: AssignmentSubmissionRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherSubmissionTrackingUiState())
    val uiState: StateFlow<TeacherSubmissionTrackingUiState> = _uiState.asStateFlow()

    fun loadSubmissions(subjectId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { currentUser ->
                    if (currentUser == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "User not found"
                        )
                        return@onSuccess
                    }
                    
                    val submissionsResult = submissionRepository.getSubmissionsBySubject(subjectId)
                    val statisticsResult = submissionRepository.getSubmissionStatistics(subjectId)
                    
                    submissionsResult.onSuccess { submissions ->
                        statisticsResult.onSuccess { statistics ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                submissions = submissions,
                                statistics = statistics
                            )
                        }.onFailure { exception ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                submissions = submissions,
                                error = exception.message ?: "Failed to load statistics"
                            )
                        }
                    }.onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load submissions"
                        )
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to get current user"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load submissions"
                )
            }
        }
    }

    fun filterSubmissionsByStatus(status: SubmissionStatus?) {
        val currentSubmissions = _uiState.value.allSubmissions
        val filteredSubmissions = if (status != null) {
            currentSubmissions.filter { it.status == status }
        } else {
            currentSubmissions
        }
        
        _uiState.value = _uiState.value.copy(
            submissions = filteredSubmissions,
            selectedFilter = status
        )
    }

    fun gradeSubmission(submissionId: String, grade: Double, feedback: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = submissionRepository.updateSubmissionStatus(
                    submissionId = submissionId,
                    status = SubmissionStatus.GRADED,
                    feedback = feedback,
                    grade = grade
                )
                
                result.onSuccess {
                    // Reload submissions to reflect changes
                    val currentSubjectId = _uiState.value.currentSubjectId
                    if (currentSubjectId.isNotEmpty()) {
                        loadSubmissions(currentSubjectId)
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to grade submission"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to grade submission"
                )
            }
        }
    }

    fun markAsLate(submissionId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = submissionRepository.updateSubmissionStatus(
                    submissionId = submissionId,
                    status = SubmissionStatus.LATE
                )
                
                result.onSuccess {
                    // Reload submissions to reflect changes
                    val currentSubjectId = _uiState.value.currentSubjectId
                    if (currentSubjectId.isNotEmpty()) {
                        loadSubmissions(currentSubjectId)
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to mark as late"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to mark as late"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class TeacherSubmissionTrackingUiState(
    val isLoading: Boolean = false,
    val submissions: List<AssignmentSubmission> = emptyList(),
    val allSubmissions: List<AssignmentSubmission> = emptyList(),
    val statistics: SubmissionStatistics? = null,
    val selectedFilter: SubmissionStatus? = null,
    val currentSubjectId: String = "",
    val error: String? = null
)
