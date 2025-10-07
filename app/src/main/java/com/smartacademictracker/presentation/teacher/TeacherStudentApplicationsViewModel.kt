package com.smartacademictracker.presentation.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.StudentApplication
import com.smartacademictracker.data.model.StudentApplicationStatus
import com.smartacademictracker.data.repository.StudentApplicationRepository
import com.smartacademictracker.data.repository.SubjectRepository
import com.smartacademictracker.data.repository.UserRepository
import com.smartacademictracker.data.repository.EnrollmentRepository
import com.smartacademictracker.data.manager.RealtimeDataManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeacherStudentApplicationsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val studentApplicationRepository: StudentApplicationRepository,
    private val subjectApplicationRepository: com.smartacademictracker.data.repository.SubjectApplicationRepository,
    private val subjectRepository: SubjectRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val realtimeDataManager: RealtimeDataManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherStudentApplicationsUiState())
    val uiState: StateFlow<TeacherStudentApplicationsUiState> = _uiState.asStateFlow()

    private val _applications = MutableStateFlow<List<StudentApplication>>(emptyList())
    val applications: StateFlow<List<StudentApplication>> = _applications.asStateFlow()

    // Tracks which backing collection each displayed application came from
    private val applicationSourceById = mutableMapOf<String, ApplicationSource>()

    private enum class ApplicationSource { LEGACY_STUDENT, CURRENT_SUBJECT }

    fun loadApplications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Get current user (teacher)
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        // First get all subjects taught by this teacher
                        val subjectsResult = subjectRepository.getAllSubjects()
                        subjectsResult.onSuccess { subjectsList ->
                            val teacherSubjects = subjectsList.filter { it.teacherId == user.id }
                            val subjectIds = teacherSubjects.map { it.id }
                            
                            println("DEBUG: TeacherStudentApplicationsViewModel - Found ${teacherSubjects.size} subjects for teacher ${user.id}")
                            teacherSubjects.forEach { subject ->
                                println("DEBUG: Teacher Subject - ID: ${subject.id}, Name: ${subject.name}, TeacherId: ${subject.teacherId}")
                            }
                            println("DEBUG: TeacherStudentApplicationsViewModel - Subject IDs: $subjectIds")
                            
                            // Now get applications for these subjects from both legacy and current collections
                            val legacyResult = studentApplicationRepository.getApplicationsForTeacherSubjects(user.id, subjectIds)
                            val legacyList = legacyResult.getOrElse { emptyList<com.smartacademictracker.data.model.StudentApplication>() }
                            val currentResult = subjectApplicationRepository.getApplicationsForTeacherSubjects(subjectIds)
                            val currentList = currentResult.getOrElse { emptyList<com.smartacademictracker.data.model.SubjectApplication>() }.map { sa ->
                                com.smartacademictracker.data.model.StudentApplication(
                                    id = sa.id,
                                    studentId = sa.studentId,
                                    studentName = sa.studentName,
                                    subjectId = sa.subjectId,
                                    subjectName = sa.subjectName,
                                    subjectCode = "",
                                    status = when (sa.status) {
                                        com.smartacademictracker.data.model.ApplicationStatus.PENDING -> com.smartacademictracker.data.model.StudentApplicationStatus.PENDING
                                        com.smartacademictracker.data.model.ApplicationStatus.APPROVED -> com.smartacademictracker.data.model.StudentApplicationStatus.APPROVED
                                        com.smartacademictracker.data.model.ApplicationStatus.REJECTED -> com.smartacademictracker.data.model.StudentApplicationStatus.REJECTED
                                        com.smartacademictracker.data.model.ApplicationStatus.WITHDRAWN -> com.smartacademictracker.data.model.StudentApplicationStatus.REJECTED
                                    },
                                    appliedAt = sa.appliedDate,
                                    teacherComments = sa.remarks
                                )
                            }
                            // Update source mapping
                            applicationSourceById.clear()
                            legacyList.forEach { applicationSourceById[it.id] = ApplicationSource.LEGACY_STUDENT }
                            currentList.forEach { applicationSourceById[it.id] = ApplicationSource.CURRENT_SUBJECT }

                            val combined = (legacyList + currentList).sortedByDescending { it.appliedAt }
                            _applications.value = combined
                                _uiState.value = _uiState.value.copy(isLoading = false)
                                println("DEBUG: TeacherStudentApplicationsViewModel - Loaded ${combined.size} applications for ${subjectIds.size} subjects")
                            
                        }.onFailure { exception ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to load subjects"
                            )
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "User not found"
                        )
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load user data"
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

    fun updateApplicationStatus(
        applicationId: String,
        status: StudentApplicationStatus,
        comments: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                processingApplications = _uiState.value.processingApplications + applicationId,
                error = null
            )
            
            try {
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        val source = applicationSourceById[applicationId] ?: ApplicationSource.LEGACY_STUDENT
                        val updateResult = when (source) {
                            ApplicationSource.LEGACY_STUDENT ->
                                studentApplicationRepository.updateApplicationStatus(
                                    applicationId = applicationId,
                                    status = status,
                                    teacherId = user.id,
                                    comments = comments
                                )
                            ApplicationSource.CURRENT_SUBJECT ->
                                {
                                    val mapped = when (status) {
                                        StudentApplicationStatus.PENDING -> com.smartacademictracker.data.model.ApplicationStatus.PENDING
                                        StudentApplicationStatus.APPROVED -> com.smartacademictracker.data.model.ApplicationStatus.APPROVED
                                        StudentApplicationStatus.REJECTED -> com.smartacademictracker.data.model.ApplicationStatus.REJECTED
                                    }
                                    subjectApplicationRepository.updateApplicationStatus(
                                        applicationId = applicationId,
                                        status = mapped,
                                        processedBy = user.id,
                                        remarks = comments
                                    ).map { Unit }
                                }
                        }
                        
                        updateResult.onSuccess {
                            println("DEBUG: TeacherStudentApplicationsViewModel - Application status updated successfully")
                            // If application is approved, create enrollment
                            if (status == StudentApplicationStatus.APPROVED) {
                                println("DEBUG: TeacherStudentApplicationsViewModel - Creating enrollment for approved application")
                                // Get the application details from the correct collection
                                when (applicationSourceById[applicationId] ?: ApplicationSource.LEGACY_STUDENT) {
                                    ApplicationSource.LEGACY_STUDENT -> {
                                        val applicationResult = studentApplicationRepository.getApplicationById(applicationId)
                                        applicationResult.onSuccess { application ->
                                            if (application != null) {
                                                println("DEBUG: TeacherStudentApplicationsViewModel - Application details (legacy): studentId=${application.studentId}, subjectId=${application.subjectId}")
                                                val enrollmentResult = enrollmentRepository.enrollStudent(
                                                    studentId = application.studentId,
                                                    studentName = application.studentName,
                                                    subjectId = application.subjectId,
                                                    subjectName = application.subjectName,
                                                    subjectCode = application.subjectCode,
                                                    semester = "Fall 2025",
                                                    academicYear = "2025-2026"
                                                )
                                                enrollmentResult.onSuccess { enrollment ->
                                                    println("DEBUG: TeacherStudentApplicationsViewModel - Enrollment created successfully: ${enrollment.id}")
                                                    kotlinx.coroutines.delay(1000)
                                                    realtimeDataManager.loadAllData()
                                                }.onFailure { enrollmentException ->
                                                    println("DEBUG: TeacherStudentApplicationsViewModel - Failed to create enrollment: ${enrollmentException.message}")
                                                }
                                            }
                                        }.onFailure { appException ->
                                            println("DEBUG: TeacherStudentApplicationsViewModel - Failed to get legacy application details: ${appException.message}")
                                        }
                                    }
                                    ApplicationSource.CURRENT_SUBJECT -> {
                                        val applicationResult = subjectApplicationRepository.getApplicationById(applicationId)
                                        applicationResult.onSuccess { sa ->
                                            // Fetch subject to get subjectCode if needed
                                            viewModelScope.launch {
                                                val subjectCode = try {
                                                    val subj = subjectRepository.getSubjectById(sa.subjectId).getOrNull()
                                                    subj?.code ?: ""
                                                } catch (_: Exception) { "" }
                                                val enrollmentResult = enrollmentRepository.enrollStudent(
                                                    studentId = sa.studentId,
                                                    studentName = sa.studentName,
                                                    subjectId = sa.subjectId,
                                                    subjectName = sa.subjectName,
                                                    subjectCode = subjectCode,
                                                    semester = "Fall 2025",
                                                    academicYear = "2025-2026"
                                                )
                                                enrollmentResult.onSuccess { enrollment ->
                                                    println("DEBUG: TeacherStudentApplicationsViewModel - Enrollment created successfully for subject application: ${enrollment.id}")
                                                    kotlinx.coroutines.delay(1000)
                                                    realtimeDataManager.loadAllData()
                                                }.onFailure { enrollmentException ->
                                                    println("DEBUG: TeacherStudentApplicationsViewModel - Failed to create enrollment (subject application): ${enrollmentException.message}")
                                                }
                                            }
                                        }.onFailure { appException ->
                                            println("DEBUG: TeacherStudentApplicationsViewModel - Failed to get subject application details: ${appException.message}")
                                        }
                                    }
                                }
                            }
                            
                            _uiState.value = _uiState.value.copy(
                                processingApplications = _uiState.value.processingApplications - applicationId
                            )
                            // Reload applications to update UI
                            loadApplications()
                            
                            println("DEBUG: TeacherStudentApplicationsViewModel - Application status updated successfully")
                        }.onFailure { exception ->
                            _uiState.value = _uiState.value.copy(
                                processingApplications = _uiState.value.processingApplications - applicationId,
                                error = exception.message ?: "Failed to update application status"
                            )
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            processingApplications = _uiState.value.processingApplications - applicationId,
                            error = "User not found"
                        )
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        processingApplications = _uiState.value.processingApplications - applicationId,
                        error = exception.message ?: "Failed to get user data"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    processingApplications = _uiState.value.processingApplications - applicationId,
                    error = e.message ?: "Failed to update application status"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun refreshApplications() {
        loadApplications()
    }
}

data class TeacherStudentApplicationsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val processingApplications: Set<String> = emptySet()
)
