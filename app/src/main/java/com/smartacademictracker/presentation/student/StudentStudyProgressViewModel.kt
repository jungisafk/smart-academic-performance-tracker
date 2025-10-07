package com.smartacademictracker.presentation.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.repository.GradeRepository
import com.smartacademictracker.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentStudyProgressViewModel @Inject constructor(
    private val gradeRepository: GradeRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentStudyProgressUiState())
    val uiState: StateFlow<StudentStudyProgressUiState> = _uiState.asStateFlow()

    fun loadStudyProgress() {
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
                    
                    val result = gradeRepository.getGradesByStudent(currentUser.id)
                    result.onSuccess { grades ->
                        val milestones = createStudyMilestones(grades)
                        val completedMilestones = milestones.count { it.isCompleted }
                        val overallProgress = if (milestones.isNotEmpty()) {
                            completedMilestones.toDouble() / milestones.size
                        } else 0.0
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            milestones = milestones,
                            completedMilestones = completedMilestones,
                            overallProgress = overallProgress
                        )
                    }.onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load study progress"
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
                    error = e.message ?: "Failed to load study progress"
                )
            }
        }
    }

    private fun createStudyMilestones(grades: List<com.smartacademictracker.data.model.Grade>): List<StudyMilestone> {
        val milestones = mutableListOf<StudyMilestone>()
        val currentTime = System.currentTimeMillis()
        
        // Group grades by subject
        val subjectGroups = grades.groupBy { it.subjectId }
        
        subjectGroups.forEach { (subjectId, subjectGrades) ->
            val subjectName = subjectGrades.firstOrNull()?.subjectName ?: "Unknown Subject"
            
            // Create milestones based on grade periods
            val prelimGrade = subjectGrades.find { it.gradePeriod == com.smartacademictracker.data.model.GradePeriod.PRELIM }
            val midtermGrade = subjectGrades.find { it.gradePeriod == com.smartacademictracker.data.model.GradePeriod.MIDTERM }
            val finalGrade = subjectGrades.find { it.gradePeriod == com.smartacademictracker.data.model.GradePeriod.FINAL }
            
            // Prelim milestone
            milestones.add(
                StudyMilestone(
                    id = "${subjectId}_prelim",
                    title = "Complete Prelim Requirements",
                    description = "Submit all prelim assignments and take prelim exam for $subjectName",
                    subjectName = subjectName,
                    dueDate = currentTime + (30 * 24 * 60 * 60 * 1000L), // 30 days from now
                    isCompleted = prelimGrade != null,
                    completedDate = prelimGrade?.dateRecorded
                )
            )
            
            // Midterm milestone
            milestones.add(
                StudyMilestone(
                    id = "${subjectId}_midterm",
                    title = "Complete Midterm Requirements",
                    description = "Submit all midterm assignments and take midterm exam for $subjectName",
                    subjectName = subjectName,
                    dueDate = currentTime + (60 * 24 * 60 * 60 * 1000L), // 60 days from now
                    isCompleted = midtermGrade != null,
                    completedDate = midtermGrade?.dateRecorded
                )
            )
            
            // Final milestone
            milestones.add(
                StudyMilestone(
                    id = "${subjectId}_final",
                    title = "Complete Final Requirements",
                    description = "Submit all final assignments and take final exam for $subjectName",
                    subjectName = subjectName,
                    dueDate = currentTime + (90 * 24 * 60 * 60 * 1000L), // 90 days from now
                    isCompleted = finalGrade != null,
                    completedDate = finalGrade?.dateRecorded
                )
            )
            
            // Study habit milestones
            milestones.add(
                StudyMilestone(
                    id = "${subjectId}_study_habit_1",
                    title = "Establish Study Routine",
                    description = "Create and maintain a consistent study schedule for $subjectName",
                    subjectName = subjectName,
                    dueDate = currentTime + (14 * 24 * 60 * 60 * 1000L), // 14 days from now
                    isCompleted = false // This would need to be tracked separately
                )
            )
            
            milestones.add(
                StudyMilestone(
                    id = "${subjectId}_study_habit_2",
                    title = "Complete Practice Problems",
                    description = "Solve at least 50 practice problems for $subjectName",
                    subjectName = subjectName,
                    dueDate = currentTime + (45 * 24 * 60 * 60 * 1000L), // 45 days from now
                    isCompleted = false // This would need to be tracked separately
                )
            )
        }
        
        return milestones.sortedBy { it.dueDate }
    }

    fun markMilestoneCompleted(milestoneId: String) {
        viewModelScope.launch {
            val currentMilestones = _uiState.value.milestones.toMutableList()
            val milestoneIndex = currentMilestones.indexOfFirst { it.id == milestoneId }
            
            if (milestoneIndex != -1) {
                val milestone = currentMilestones[milestoneIndex]
                currentMilestones[milestoneIndex] = milestone.copy(
                    isCompleted = true,
                    completedDate = System.currentTimeMillis()
                )
                
                val completedMilestones = currentMilestones.count { it.isCompleted }
                val overallProgress = if (currentMilestones.isNotEmpty()) {
                    completedMilestones.toDouble() / currentMilestones.size
                } else 0.0
                
                _uiState.value = _uiState.value.copy(
                    milestones = currentMilestones,
                    completedMilestones = completedMilestones,
                    overallProgress = overallProgress
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class StudentStudyProgressUiState(
    val isLoading: Boolean = false,
    val milestones: List<StudyMilestone> = emptyList(),
    val completedMilestones: Int = 0,
    val overallProgress: Double = 0.0,
    val error: String? = null
)
