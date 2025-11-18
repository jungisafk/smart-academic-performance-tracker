package com.smartacademictracker.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.TeacherApplication
import com.smartacademictracker.data.model.ApplicationStatus
import com.smartacademictracker.data.repository.TeacherApplicationRepository
import com.smartacademictracker.data.repository.SubjectRepository
import com.smartacademictracker.data.manager.AdminDataCache
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminApplicationsViewModel @Inject constructor(
    private val teacherApplicationRepository: TeacherApplicationRepository,
    private val subjectRepository: SubjectRepository,
    private val notificationSenderService: com.smartacademictracker.data.notification.NotificationSenderService,
    private val adminDataCache: AdminDataCache
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminApplicationsUiState())
    val uiState: StateFlow<AdminApplicationsUiState> = _uiState.asStateFlow()

    private val _applications = MutableStateFlow<List<TeacherApplication>>(emptyList())
    val applications: StateFlow<List<TeacherApplication>> = _applications.asStateFlow()

    init {
        // Load cached data immediately if available
        val cachedApplications = adminDataCache.cachedTeacherApplications.value
        if (cachedApplications.isNotEmpty() && adminDataCache.isCacheValid()) {
            _applications.value = cachedApplications
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
        
        // Set up real-time listener
        setupRealtimeListeners()
    }

    private fun setupRealtimeListeners() {
        viewModelScope.launch {
            try {
                teacherApplicationRepository.getAllApplicationsFlow()
                    .collect { applications ->
                        _applications.value = applications
                        adminDataCache.updateTeacherApplications(applications)
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to setup real-time listeners"
                )
            }
        }
    }

    fun loadApplications(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            // Load cached data first if available and not forcing refresh
            if (!forceRefresh && adminDataCache.cachedTeacherApplications.value.isNotEmpty() && adminDataCache.isCacheValid()) {
                _applications.value = adminDataCache.cachedTeacherApplications.value
                _uiState.value = _uiState.value.copy(isLoading = false)
            } else {
                // Only show loading if we don't have cached data
                if (adminDataCache.cachedTeacherApplications.value.isEmpty()) {
                    _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                }
            }
            
            try {
                val result = teacherApplicationRepository.getAllApplications()
                
                result.onSuccess { applicationsList ->
                    _applications.value = applicationsList
                    adminDataCache.updateTeacherApplications(applicationsList)
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load applications"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load applications"
                )
            }
        }
    }

    fun approveApplication(applicationId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // First, get the application details
                val applicationResult = teacherApplicationRepository.getApplicationById(applicationId)
                applicationResult.onSuccess { application ->
                    // Update application status
                    val statusResult = teacherApplicationRepository.updateApplicationStatus(applicationId, ApplicationStatus.APPROVED.name)
                    statusResult.onSuccess {
                        // Assign teacher to subject
                        val assignResult = subjectRepository.assignTeacherToSubject(
                            application.subjectId,
                            application.teacherId,
                            application.teacherName
                        )
                        assignResult.onSuccess {
                            
                            
                            // Notify teacher that their application was approved
                            notificationSenderService.sendApplicationStatusNotification(
                                userId = application.teacherId,
                                applicationType = "Teacher Application",
                                status = "approved",
                                subjectName = application.subjectName
                            )
                            
                            // Real-time listener will automatically update UI, no need to reload
                            _uiState.value = _uiState.value.copy(isLoading = false)
                        }.onFailure { exception ->
                            
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Application approved but failed to assign teacher: ${exception.message}"
                            )
                        }
                    }.onFailure { exception ->
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to approve application"
                        )
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to get application details"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to approve application"
                )
            }
        }
    }

    fun rejectApplication(applicationId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Get application details first to notify teacher
                val applicationResult = teacherApplicationRepository.getApplicationById(applicationId)
                applicationResult.onSuccess { application ->
                    val result = teacherApplicationRepository.updateApplicationStatus(applicationId, ApplicationStatus.REJECTED.name)
                    result.onSuccess {
                        // Notify teacher that their application was rejected
                        notificationSenderService.sendApplicationStatusNotification(
                            userId = application.teacherId,
                            applicationType = "Teacher Application",
                            status = "rejected",
                            subjectName = application.subjectName,
                            reason = application.adminComments
                        )
                        
                        // Reload applications after status update
                        loadApplications(forceRefresh = true)
                    }.onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to reject application"
                        )
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to get application details"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to reject application"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class AdminApplicationsUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
