package com.smartacademictracker.presentation.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.SectionAssignment
import com.smartacademictracker.data.model.StudentEnrollment
import com.smartacademictracker.data.repository.SectionAssignmentRepository
import com.smartacademictracker.data.repository.StudentEnrollmentRepository
import com.smartacademictracker.data.repository.UserRepository
import com.smartacademictracker.data.notification.NotificationSenderService
import com.smartacademictracker.data.notification.EnrollmentNotificationService
import com.smartacademictracker.data.model.NotificationType
import com.smartacademictracker.data.manager.TeacherDataCache
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeacherStudentManagementViewModel @Inject constructor(
    private val sectionAssignmentRepository: SectionAssignmentRepository,
    private val enrollmentRepository: StudentEnrollmentRepository,
    private val userRepository: UserRepository,
    private val notificationSenderService: NotificationSenderService,
    private val enrollmentNotificationService: EnrollmentNotificationService,
    private val teacherDataCache: TeacherDataCache
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherStudentManagementUiState())
    val uiState: StateFlow<TeacherStudentManagementUiState> = _uiState.asStateFlow()

    private val _sections = MutableStateFlow<List<SectionAssignment>>(emptyList())
    val sections: StateFlow<List<SectionAssignment>> = _sections.asStateFlow()

    private val _selectedSectionStudents = MutableStateFlow<List<StudentEnrollment>>(emptyList())
    val selectedSectionStudents: StateFlow<List<StudentEnrollment>> = _selectedSectionStudents.asStateFlow()

    fun loadTeacherSections(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            // Load cached sections if available
            val cachedSections = teacherDataCache.cachedSections.value
            if (!forceRefresh && cachedSections.isNotEmpty() && teacherDataCache.isCacheValid()) {
                _sections.value = cachedSections
                if (cachedSections.size == 1) {
                    selectSection(cachedSections[0].id)
                }
                _uiState.value = _uiState.value.copy(isLoading = false)
            } else {
                if (cachedSections.isEmpty()) {
                    _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                }
            }
            
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
                    
                    val result = sectionAssignmentRepository.getSectionAssignmentsByTeacher(currentUser.id)
                    result.onSuccess { assignments ->
                        _sections.value = assignments
                        teacherDataCache.updateSections(assignments)
                        
                        // Automatically select the first section if only one section is available
                        if (assignments.size == 1) {
                            selectSection(assignments[0].id)
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = null
                        )
                    }.onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load sections"
                        )
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to get user information"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load sections"
                )
            }
        }
    }

    fun selectSection(sectionId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                selectedSectionId = sectionId,
                isLoading = true,
                error = null
            )
            
            try {
                val section = _sections.value.find { it.id == sectionId }
                if (section != null) {
                    val result = enrollmentRepository.getStudentsBySection(
                        subjectId = section.subjectId,
                        sectionName = section.sectionName
                    )
                    
                    result.onSuccess { students ->
                        _selectedSectionStudents.value = students
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = null
                        )
                    }.onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load students"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Section not found"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load students"
                )
            }
        }
    }

    fun kickStudent(enrollmentId: String, reason: String = "") {
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
                    
                    val result = enrollmentRepository.updateEnrollmentStatus(
                        enrollmentId = enrollmentId,
                        status = com.smartacademictracker.data.model.EnrollmentStatus.KICKED,
                        updatedBy = currentUser.id,
                        updatedByName = "${currentUser.firstName} ${currentUser.lastName}",
                        notes = if (reason.isNotBlank()) reason else "Removed by teacher"
                    )
                    
                    result.onSuccess {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            successMessage = "Student removed successfully"
                        )
                        
                        // Get enrollment details for notifications
                        enrollmentRepository.getEnrollmentById(enrollmentId).onSuccess { enrollment ->
                            if (enrollment != null) {
                                // Send comprehensive notifications using the new service
                                enrollmentNotificationService.notifyStudentKickedFromClass(
                                    studentId = enrollment.studentId,
                                    studentName = enrollment.studentName,
                                    subjectId = enrollment.subjectId,
                                    subjectName = enrollment.subjectName,
                                    teacherId = currentUser.id,
                                    teacherName = "${currentUser.firstName} ${currentUser.lastName}",
                                    reason = if (reason.isNotBlank()) reason else "Removed by teacher"
                                )
                            }
                        }
                        
                        // Reload students for the selected section
                        _uiState.value.selectedSectionId?.let { sectionId ->
                            selectSection(sectionId)
                        }
                    }.onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to remove student"
                        )
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to get user information"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to remove student"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}

data class TeacherStudentManagementUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val selectedSectionId: String? = null
)
