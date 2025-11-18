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
import com.smartacademictracker.data.manager.AdminDataCache
import com.smartacademictracker.data.notification.NotificationSenderService
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
    private val yearLevelRepository: YearLevelRepository,
    private val adminDataCache: AdminDataCache,
    private val notificationSenderService: NotificationSenderService
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

    init {
        // Load cached data immediately if available
        val cachedSubjects = adminDataCache.cachedSubjects.value
        val cachedYearLevels = adminDataCache.cachedYearLevels.value
        val cachedTeacherApplications = adminDataCache.cachedTeacherApplications.value
        
        if (cachedSubjects.isNotEmpty() && cachedYearLevels.isNotEmpty() && 
            cachedTeacherApplications.isNotEmpty() && adminDataCache.isCacheValid()) {
            _subjects.value = cachedSubjects
            _yearLevels.value = cachedYearLevels
            _teacherApplications.value = cachedTeacherApplications
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun loadData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            // Load cached data first if available and not forcing refresh
            if (!forceRefresh && adminDataCache.cachedSubjects.value.isNotEmpty() && 
                adminDataCache.cachedYearLevels.value.isNotEmpty() &&
                adminDataCache.cachedTeacherApplications.value.isNotEmpty() &&
                adminDataCache.isCacheValid()) {
                _subjects.value = adminDataCache.cachedSubjects.value
                _yearLevels.value = adminDataCache.cachedYearLevels.value
                _teacherApplications.value = adminDataCache.cachedTeacherApplications.value
                _uiState.value = _uiState.value.copy(isLoading = false)
            } else {
                // Only show loading if we don't have cached data
                if (adminDataCache.cachedSubjects.value.isEmpty() || 
                    adminDataCache.cachedYearLevels.value.isEmpty() ||
                    adminDataCache.cachedTeacherApplications.value.isEmpty()) {
                    _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                }
            }
            
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
                        adminDataCache.updateSubjects(subjectsList)
                    }.onFailure { exception ->
                        // Error handling
                    }

                    assignmentsResult.onSuccess { assignmentsList ->
                        _sectionAssignments.value = assignmentsList
                    }.onFailure { exception ->
                        // Error handling
                    }

                    applicationsResult.onSuccess { applicationsList ->
                        _teacherApplications.value = applicationsList
                        adminDataCache.updateTeacherApplications(applicationsList)
                    }.onFailure { exception ->
                        // Error handling
                    }

                    yearLevelsResult.onSuccess { yearLevelsList ->
                        _yearLevels.value = yearLevelsList
                        adminDataCache.updateYearLevels(yearLevelsList)
                    }.onFailure { exception ->
                        // Error handling
                    }

                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                
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
                        // Get current admin user for notification
                        val currentAdmin = userRepository.getCurrentUser().getOrNull()
                        val adminName = currentAdmin?.let { "${it.firstName} ${it.lastName}" } ?: "Admin"
                        
                        // Notify teacher about assignment
                        notificationSenderService.sendTeacherAssignedToSectionNotification(
                            teacherId = teacherId,
                            subjectName = subject?.name ?: "Unknown Subject",
                            sectionName = sectionName,
                            assignedBy = adminName
                        )
                        
                        // Update the teacher application status to approved
                        updateTeacherApplicationStatus(teacherId, subjectId, sectionName)
                        // Refresh data
                        loadData(forceRefresh = true)
                    }.onFailure { exception ->
                        
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to assign teacher: ${exception.message}"
                        )
                    }
                }.onFailure { exception ->
                    
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to get teacher information: ${exception.message}"
                    )
                }
            } catch (e: Exception) {
                
                _uiState.value = _uiState.value.copy(
                    error = "Failed to assign teacher: ${e.message}"
                )
            }
        }
    }

    fun removeSectionAssignment(assignmentId: String) {
        viewModelScope.launch {
            try {
                // Get assignment details before deletion for notification
                val assignment = _sectionAssignments.value.find { it.id == assignmentId }
                
                sectionAssignmentRepository.deleteSectionAssignment(assignmentId).onSuccess {
                    // Get current admin user for notification
                    val currentAdmin = userRepository.getCurrentUser().getOrNull()
                    val adminName = currentAdmin?.let { "${it.firstName} ${it.lastName}" } ?: "Admin"
                    
                    // Notify teacher about removal
                    if (assignment != null) {
                        val subject = _subjects.value.find { it.id == assignment.subjectId }
                        notificationSenderService.sendTeacherRemovedFromSectionNotification(
                            teacherId = assignment.teacherId,
                            subjectName = subject?.name ?: assignment.subjectId,
                            sectionName = assignment.sectionName,
                            removedBy = adminName
                        )
                    }
                    
                    // Refresh data
                    loadData()
                }.onFailure { exception ->
                    
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to remove assignment: ${exception.message}"
                    )
                }
            } catch (e: Exception) {
                
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
                    
                }.onFailure { exception ->
                    
                }
            }
        } catch (e: Exception) {
            
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
