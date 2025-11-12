package com.smartacademictracker.presentation.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.Subject
import com.smartacademictracker.data.model.StudentApplication
import com.smartacademictracker.data.model.StudentApplicationStatus
import com.smartacademictracker.data.repository.StudentApplicationRepository
import com.smartacademictracker.data.repository.SubjectRepository
import com.smartacademictracker.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentSubjectApplicationViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val subjectRepository: SubjectRepository,
    private val studentApplicationRepository: StudentApplicationRepository,
    private val notificationSenderService: com.smartacademictracker.data.notification.NotificationSenderService
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentSubjectApplicationUiState())
    val uiState: StateFlow<StudentSubjectApplicationUiState> = _uiState.asStateFlow()

    private val _availableSubjects = MutableStateFlow<List<Subject>>(emptyList())
    val availableSubjects: StateFlow<List<Subject>> = _availableSubjects.asStateFlow()

    private val _myApplications = MutableStateFlow<List<StudentApplication>>(emptyList())
    val myApplications: StateFlow<List<StudentApplication>> = _myApplications.asStateFlow()

    private val _selectedYearLevel = MutableStateFlow("")
    val selectedYearLevel: StateFlow<String> = _selectedYearLevel.asStateFlow()

    private val _selectedCourse = MutableStateFlow("")
    val selectedCourse: StateFlow<String> = _selectedCourse.asStateFlow()

    fun loadAvailableSubjects() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Get current user to filter by year level and course
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        // Load all active subjects
                        val subjectsResult = subjectRepository.getAllSubjects()
                        subjectsResult.onSuccess { subjectsList ->
                            // Filter subjects based on user's year level and course
                            val filteredSubjects = subjectsList.filter { subject ->
                                subject.active && 
                                (user.yearLevelId == null || subject.yearLevelId == user.yearLevelId) &&
                                (user.courseId == null || subject.courseId == user.courseId)
                            }
                            
                            _availableSubjects.value = filteredSubjects
                            _selectedYearLevel.value = user.yearLevelName ?: ""
                            _selectedCourse.value = user.courseCode ?: ""
                            _uiState.value = _uiState.value.copy(isLoading = false)
                            
                            println("DEBUG: StudentSubjectApplicationViewModel - Loaded ${filteredSubjects.size} subjects for ${user.yearLevelName} ${user.courseCode}")
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
                    error = e.message ?: "Failed to load subjects"
                )
            }
        }
    }

    fun loadMyApplications() {
        viewModelScope.launch {
            try {
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        println("DEBUG: StudentSubjectApplicationViewModel - Loading applications for user: ${user.id}")
                        val applicationsResult = studentApplicationRepository.getApplicationsByStudent(user.id)
                        applicationsResult.onSuccess { applicationsList ->
                            _myApplications.value = applicationsList
                            println("DEBUG: StudentSubjectApplicationViewModel - Loaded ${applicationsList.size} applications")
                            applicationsList.forEach { app ->
                                println("DEBUG: Application - ID: ${app.id}, Subject: ${app.subjectName}, Status: ${app.status}")
                            }
                        }.onFailure { exception ->
                            println("DEBUG: StudentSubjectApplicationViewModel - Failed to load applications: ${exception.message}")
                            _uiState.value = _uiState.value.copy(error = "Failed to load applications: ${exception.message}")
                        }
                    } else {
                        println("DEBUG: StudentSubjectApplicationViewModel - User is null")
                    }
                }.onFailure { exception ->
                    println("DEBUG: StudentSubjectApplicationViewModel - Failed to get current user: ${exception.message}")
                    _uiState.value = _uiState.value.copy(error = "Failed to get user: ${exception.message}")
                }
            } catch (e: Exception) {
                println("DEBUG: StudentSubjectApplicationViewModel - Exception loading applications: ${e.message}")
                _uiState.value = _uiState.value.copy(error = "Exception loading applications: ${e.message}")
            }
        }
    }

    fun applyForSubject(subjectId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                applyingSubjects = _uiState.value.applyingSubjects + subjectId,
                error = null
            )
            
            try {
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        // Check if already applied
                        val hasAppliedResult = studentApplicationRepository.hasStudentAppliedForSubject(user.id, subjectId)
                        hasAppliedResult.onSuccess { hasApplied ->
                            if (hasApplied) {
                                _uiState.value = _uiState.value.copy(
                                    applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                                    error = "You have already applied for this subject"
                                )
                            } else {
                                // Get subject details
                                val subjectResult = subjectRepository.getSubjectById(subjectId)
                                subjectResult.onSuccess { subject ->
                                    // Validate that the subject has an assigned teacher
                                    if (subject.teacherId == null || subject.teacherId.isEmpty()) {
                                        _uiState.value = _uiState.value.copy(
                                            applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                                            error = "This subject does not have an assigned teacher yet. Please wait for a teacher to be assigned before applying."
                                        )
                                        return@onSuccess
                                    }
                                    
                                    // Create application
                                    val application = StudentApplication(
                                        studentId = user.id,
                                        studentName = "${user.firstName} ${user.lastName}",
                                        studentEmail = user.email,
                                        subjectId = subject.id,
                                        subjectName = subject.name,
                                        subjectCode = subject.code,
                                        applicationReason = "I would like to enroll in this subject to further my studies in ${subject.courseName}.",
                                        status = StudentApplicationStatus.PENDING,
                                        yearLevelId = user.yearLevelId ?: "",
                                        courseId = user.courseId ?: "",
                                        yearLevelName = user.yearLevelName ?: "",
                                        courseName = user.courseName ?: "",
                                        courseCode = user.courseCode ?: ""
                                    )

                                    val createResult = studentApplicationRepository.createApplication(application)
                                    createResult.onSuccess { createdApplication ->
                                        // Notify teacher about the new student application
                                        notifyTeacherOfStudentApplication(createdApplication, subject)
                                        
                                        _uiState.value = _uiState.value.copy(
                                            applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                                            isApplicationSuccess = true
                                        )
                                        println("DEBUG: StudentSubjectApplicationViewModel - Application created successfully with ID: ${createdApplication.id}")
                                        
                                        // Add a small delay to ensure the application is saved before reloading
                                        kotlinx.coroutines.delay(500)
                                        
                                        // Reload applications to update UI
                                        loadMyApplications()
                                    }.onFailure { exception ->
                                        _uiState.value = _uiState.value.copy(
                                            applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                                            error = exception.message ?: "Failed to create application"
                                        )
                                        println("DEBUG: StudentSubjectApplicationViewModel - Failed to create application: ${exception.message}")
                                    }
                                }.onFailure { exception ->
                                    _uiState.value = _uiState.value.copy(
                                        applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                                        error = exception.message ?: "Failed to get subject details"
                                    )
                                }
                            }
                        }.onFailure { exception ->
                            _uiState.value = _uiState.value.copy(
                                applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                                error = exception.message ?: "Failed to check application status"
                            )
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                            error = "User not found"
                        )
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                        error = exception.message ?: "Failed to get user data"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                    error = e.message ?: "Failed to apply for subject"
                )
            }
        }
    }

    fun setSelectedYearLevel(yearLevel: String) {
        _selectedYearLevel.value = yearLevel
        // Filter subjects based on new year level
        filterSubjects()
    }

    fun setSelectedCourse(course: String) {
        _selectedCourse.value = course
        // Filter subjects based on new course
        filterSubjects()
    }

    private fun filterSubjects() {
        viewModelScope.launch {
            try {
                val subjectsResult = subjectRepository.getAllSubjects()
                subjectsResult.onSuccess { subjectsList ->
                    val filteredSubjects = subjectsList.filter { subject ->
                        subject.active &&
                        (selectedYearLevel.value.isEmpty() || subject.yearLevelName == selectedYearLevel.value) &&
                        (selectedCourse.value.isEmpty() || subject.courseCode == selectedCourse.value)
                    }
                    _availableSubjects.value = filteredSubjects
                }
            } catch (e: Exception) {
                println("DEBUG: StudentSubjectApplicationViewModel - Error filtering subjects: ${e.message}")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun clearApplicationSuccess() {
        _uiState.value = _uiState.value.copy(isApplicationSuccess = false)
    }
    
    fun refreshData() {
        loadAvailableSubjects()
        loadMyApplications()
    }
    
    /**
     * Notify teacher when a student applies for their subject
     */
    private suspend fun notifyTeacherOfStudentApplication(application: StudentApplication, subject: Subject) {
        try {
            val teacherId = subject.teacherId
            if (teacherId != null && teacherId.isNotEmpty()) {
                notificationSenderService.sendNotification(
                    userId = teacherId,
                    type = com.smartacademictracker.data.model.NotificationType.STUDENT_APPLICATION_SUBMITTED,
                    variables = mapOf(
                        "studentName" to application.studentName,
                        "subjectName" to application.subjectName,
                        "applicationId" to application.id
                    ),
                    priority = com.smartacademictracker.data.model.NotificationPriority.NORMAL
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("StudentApplication", "Failed to notify teacher of student application: ${e.message}")
        }
    }
}

data class StudentSubjectApplicationUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val applyingSubjects: Set<String> = emptySet(),
    val isApplicationSuccess: Boolean = false
)
