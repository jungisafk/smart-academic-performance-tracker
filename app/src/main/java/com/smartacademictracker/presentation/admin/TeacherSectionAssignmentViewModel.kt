package com.smartacademictracker.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.Subject
import com.smartacademictracker.data.model.SectionAssignment
import com.smartacademictracker.data.model.TeacherApplication
import com.smartacademictracker.data.model.ApplicationStatus
import com.smartacademictracker.data.model.YearLevel
import com.smartacademictracker.data.repository.SubjectRepository
import com.smartacademictracker.data.repository.SectionAssignmentRepository
import com.smartacademictracker.data.repository.TeacherApplicationRepository
import com.smartacademictracker.data.repository.UserRepository
import com.smartacademictracker.data.repository.YearLevelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

@HiltViewModel
class TeacherSectionAssignmentViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val sectionAssignmentRepository: SectionAssignmentRepository,
    private val teacherApplicationRepository: TeacherApplicationRepository,
    private val userRepository: UserRepository,
    private val yearLevelRepository: YearLevelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherSectionAssignmentUiState())
    val uiState: StateFlow<TeacherSectionAssignmentUiState> = _uiState.asStateFlow()

    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    val subjects: StateFlow<List<Subject>> = _subjects.asStateFlow()

    private val _sectionAssignments = MutableStateFlow<List<SectionAssignment>>(emptyList())
    val sectionAssignments: StateFlow<List<SectionAssignment>> = _sectionAssignments.asStateFlow()

    private val _teacherApplications = MutableStateFlow<List<TeacherApplication>>(emptyList())
    val teacherApplications: StateFlow<List<TeacherApplication>> = _teacherApplications.asStateFlow()
    
    private val _yearLevels = MutableStateFlow<List<YearLevel>>(emptyList())
    val yearLevels: StateFlow<List<YearLevel>> = _yearLevels.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Load all data in parallel for faster loading
                coroutineScope {
                    val subjectsDeferred = async { subjectRepository.getAllSubjects() }
                    val assignmentsDeferred = async { sectionAssignmentRepository.getAllSectionAssignments() }
                    val applicationsDeferred = async { teacherApplicationRepository.getAllApplications() }
                    val yearLevelsDeferred = async { yearLevelRepository.getAllYearLevels() }
                    
                    // Wait for all operations to complete
                    val subjectsResult = subjectsDeferred.await()
                    val assignmentsResult = assignmentsDeferred.await()
                    val applicationsResult = applicationsDeferred.await()
                    val yearLevelsResult = yearLevelsDeferred.await()
                    
                    // Process results
                    subjectsResult.onSuccess { subjectsList ->
                        _subjects.value = subjectsList
                        println("DEBUG: TeacherSectionAssignmentViewModel - Loaded ${subjectsList.size} subjects")
                    }.onFailure { exception ->
                        println("DEBUG: TeacherSectionAssignmentViewModel - Error loading subjects: ${exception.message}")
                    }

                    assignmentsResult.onSuccess { assignmentsList ->
                        _sectionAssignments.value = assignmentsList
                        println("DEBUG: TeacherSectionAssignmentViewModel - Loaded ${assignmentsList.size} section assignments")
                    }.onFailure { exception ->
                        println("DEBUG: TeacherSectionAssignmentViewModel - Error loading section assignments: ${exception.message}")
                    }

                    applicationsResult.onSuccess { applicationsList ->
                        _teacherApplications.value = applicationsList
                        println("DEBUG: TeacherSectionAssignmentViewModel - Loaded ${applicationsList.size} teacher applications")
                    }.onFailure { exception ->
                        println("DEBUG: TeacherSectionAssignmentViewModel - Error loading teacher applications: ${exception.message}")
                    }

                    yearLevelsResult.onSuccess { yearLevelsList ->
                        _yearLevels.value = yearLevelsList
                        println("DEBUG: TeacherSectionAssignmentViewModel - Loaded ${yearLevelsList.size} year levels")
                    }.onFailure { exception ->
                        println("DEBUG: TeacherSectionAssignmentViewModel - Error loading year levels: ${exception.message}")
                    }

                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                println("DEBUG: TeacherSectionAssignmentViewModel - Error loading data: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load data"
                )
            }
        }
    }

    fun assignTeacherToSection(subjectId: String, sectionName: String, teacherId: String) {
        viewModelScope.launch {
            try {
                // First, check if the teacher has applied for this subject AND is approved
                val approvedApplication = _teacherApplications.value.find { 
                    it.teacherId == teacherId && 
                    it.subjectId == subjectId && 
                    it.status == ApplicationStatus.APPROVED
                }
                
                if (approvedApplication == null) {
                    // Check if teacher has applied but not approved
                    val hasApplied = _teacherApplications.value.any { 
                        it.teacherId == teacherId && it.subjectId == subjectId 
                    }
                    
                    if (!hasApplied) {
                        _uiState.value = _uiState.value.copy(
                            error = "This teacher has not applied for this subject. Only teachers who have applied can be assigned."
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            error = "This teacher's application is not yet approved. Please approve the application first before assigning to a section."
                        )
                    }
                    return@launch
                }
                
                // Get teacher information
                val teacherResult = userRepository.getUserById(teacherId)
                teacherResult.onSuccess { teacher ->
                    // Get subject to get courseId
                    val subject = _subjects.value.find { it.id == subjectId }
                    val courseId = subject?.courseId ?: ""
                    
                    // Create section assignment
                    val assignment = SectionAssignment(
                        subjectId = subjectId,
                        courseId = courseId,
                        sectionName = sectionName,
                        teacherId = teacherId,
                        teacherName = "${teacher.firstName} ${teacher.lastName}",
                        teacherEmail = teacher.email,
                        assignedBy = "admin", // TODO: Get actual admin user ID
                        assignedByName = "Admin" // TODO: Get actual admin name
                    )

                    sectionAssignmentRepository.createSectionAssignment(assignment).onSuccess {
                        println("DEBUG: TeacherSectionAssignmentViewModel - Successfully assigned teacher to section")
                        // Update the teacher application status to approved
                        updateTeacherApplicationStatus(teacherId, subjectId, sectionName)
                        // Refresh data
                        loadData()
                    }.onFailure { exception ->
                        println("DEBUG: TeacherSectionAssignmentViewModel - Error assigning teacher: ${exception.message}")
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to assign teacher: ${exception.message}"
                        )
                    }
                }.onFailure { exception ->
                    println("DEBUG: TeacherSectionAssignmentViewModel - Error getting teacher info: ${exception.message}")
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to get teacher information: ${exception.message}"
                    )
                }
            } catch (e: Exception) {
                println("DEBUG: TeacherSectionAssignmentViewModel - Error in assignTeacherToSection: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    error = "Failed to assign teacher: ${e.message}"
                )
            }
        }
    }

    fun removeSectionAssignment(assignmentId: String) {
        viewModelScope.launch {
            try {
                sectionAssignmentRepository.deleteSectionAssignment(assignmentId).onSuccess {
                    println("DEBUG: TeacherSectionAssignmentViewModel - Successfully removed section assignment")
                    // Refresh data
                    loadData()
                }.onFailure { exception ->
                    println("DEBUG: TeacherSectionAssignmentViewModel - Error removing assignment: ${exception.message}")
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to remove assignment: ${exception.message}"
                    )
                }
            } catch (e: Exception) {
                println("DEBUG: TeacherSectionAssignmentViewModel - Error in removeSectionAssignment: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    error = "Failed to remove assignment: ${e.message}"
                )
            }
        }
    }

    fun getAvailableTeachersForSubject(subjectId: String): List<TeacherApplication> {
        return _teacherApplications.value.filter { it.subjectId == subjectId }
    }

    fun canAssignTeacherToSubject(teacherId: String, subjectId: String): Boolean {
        return _teacherApplications.value.any { 
            it.teacherId == teacherId && it.subjectId == subjectId 
        }
    }

    private suspend fun updateTeacherApplicationStatus(teacherId: String, subjectId: String, sectionName: String) {
        try {
            // Find the teacher application for this subject and section
            val application = _teacherApplications.value.find { 
                it.teacherId == teacherId && it.subjectId == subjectId && it.sectionName == sectionName 
            }
            
            if (application != null) {
                // Update the application status to approved
                val updatedApplication = application.copy(
                    status = com.smartacademictracker.data.model.ApplicationStatus.APPROVED,
                    reviewedAt = System.currentTimeMillis(),
                    reviewedBy = "admin" // TODO: Get actual admin user ID
                )
                
                teacherApplicationRepository.updateApplication(updatedApplication).onSuccess {
                    println("DEBUG: TeacherSectionAssignmentViewModel - Updated teacher application status to approved")
                }.onFailure { exception ->
                    println("DEBUG: TeacherSectionAssignmentViewModel - Error updating application status: ${exception.message}")
                }
            }
        } catch (e: Exception) {
            println("DEBUG: TeacherSectionAssignmentViewModel - Error updating application status: ${e.message}")
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class TeacherSectionAssignmentUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
