package com.smartacademictracker.presentation.teacher

import android.util.Log
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
    private val enrollmentNotificationService: EnrollmentNotificationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherStudentManagementUiState())
    val uiState: StateFlow<TeacherStudentManagementUiState> = _uiState.asStateFlow()

    private val _sections = MutableStateFlow<List<SectionAssignment>>(emptyList())
    val sections: StateFlow<List<SectionAssignment>> = _sections.asStateFlow()

    private val _selectedSectionStudents = MutableStateFlow<List<StudentEnrollment>>(emptyList())
    val selectedSectionStudents: StateFlow<List<StudentEnrollment>> = _selectedSectionStudents.asStateFlow()

    fun loadTeacherSections() {
        viewModelScope.launch {
            Log.d("TeacherStudentManagement", "Loading teacher sections...")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { currentUser ->
                    if (currentUser == null) {
                        Log.e("TeacherStudentManagement", "Current user is null")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "User not found"
                        )
                        return@onSuccess
                    }
                    
                    Log.d("TeacherStudentManagement", "Loading sections for teacher: ${currentUser.id} (${currentUser.firstName} ${currentUser.lastName})")
                    val result = sectionAssignmentRepository.getSectionAssignmentsByTeacher(currentUser.id)
                    result.onSuccess { assignments ->
                        Log.d("TeacherStudentManagement", "Found ${assignments.size} section assignments for teacher")
                        assignments.forEach { assignment ->
                            Log.d("TeacherStudentManagement", "Section: ${assignment.sectionName} for Subject: ${assignment.subjectId}")
                        }
                        _sections.value = assignments
                        
                        // Automatically select the first section if only one section is available
                        if (assignments.size == 1) {
                            Log.d("TeacherStudentManagement", "Auto-selecting single section: ${assignments[0].id}")
                            selectSection(assignments[0].id)
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = null
                        )
                    }.onFailure { exception ->
                        Log.e("TeacherStudentManagement", "Failed to load section assignments: ${exception.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load sections"
                        )
                    }
                }.onFailure { exception ->
                    Log.e("TeacherStudentManagement", "Failed to get current user: ${exception.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to get user information"
                    )
                }
            } catch (e: Exception) {
                Log.e("TeacherStudentManagement", "Exception in loadTeacherSections: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load sections"
                )
            }
        }
    }

    fun selectSection(sectionId: String) {
        viewModelScope.launch {
            Log.d("TeacherStudentManagement", "Selecting section: $sectionId")
            Log.d("TeacherStudentManagement", "Available sections: ${_sections.value.map { "${it.id} (${it.sectionName})" }}")
            _uiState.value = _uiState.value.copy(
                selectedSectionId = sectionId,
                isLoading = true,
                error = null
            )
            
            try {
                val section = _sections.value.find { it.id == sectionId }
                if (section != null) {
                    Log.d("TeacherStudentManagement", "Found section: ${section.sectionName} for subject: ${section.subjectId}")
                    Log.d("TeacherStudentManagement", "Loading students for section: ${section.sectionName} in subject: ${section.subjectId}")
                    val result = enrollmentRepository.getStudentsBySection(
                        subjectId = section.subjectId,
                        sectionName = section.sectionName
                    )
                    
                    result.onSuccess { students ->
                        Log.d("TeacherStudentManagement", "Found ${students.size} students in section ${section.sectionName}")
                        students.forEach { student ->
                            Log.d("TeacherStudentManagement", "Student: ${student.studentName} (${student.studentId}) - Status: ${student.status}")
                        }
                        _selectedSectionStudents.value = students
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = null
                        )
                    }.onFailure { exception ->
                        Log.e("TeacherStudentManagement", "Failed to load students for section ${section.sectionName}: ${exception.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load students"
                        )
                    }
                } else {
                    Log.e("TeacherStudentManagement", "Section not found: $sectionId")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Section not found"
                    )
                }
            } catch (e: Exception) {
                Log.e("TeacherStudentManagement", "Exception in selectSection: ${e.message}")
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
