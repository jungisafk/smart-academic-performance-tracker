package com.smartacademictracker.presentation.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.Subject
import com.smartacademictracker.data.model.StudentApplication
import com.smartacademictracker.data.model.StudentApplicationStatus
import com.smartacademictracker.data.repository.StudentApplicationRepository
import com.smartacademictracker.data.repository.SubjectRepository
import com.smartacademictracker.data.repository.UserRepository
import com.smartacademictracker.data.repository.YearLevelRepository
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
    private val yearLevelRepository: YearLevelRepository,
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
                        // Load all year levels to create a map for level number matching
                        val allYearLevelsResult = yearLevelRepository.getAllYearLevels()
                        allYearLevelsResult.onSuccess { allYearLevels ->
                            // Create a map of yearLevelId -> level number
                            val yearLevelIdToLevelMap = allYearLevels.associateBy({ it.id }, { it.level })
                            
                            // Get student's year level number
                            val studentYearLevelNumber = user.yearLevelId?.let { yearLevelIdToLevelMap[it] }
                            
                            println("DEBUG: StudentSubjectApplicationViewModel - Student YearLevelId: ${user.yearLevelId}, YearLevelNumber: $studentYearLevelNumber")
                            
                            // Load all active subjects
                            val subjectsResult = subjectRepository.getAllSubjects()
                            subjectsResult.onSuccess { subjectsList ->
                                println("DEBUG: StudentSubjectApplicationViewModel - Loaded ${subjectsList.size} total subjects")
                                
                                // Debug: Log ALL subjects before filtering
                                println("DEBUG: StudentSubjectApplicationViewModel - === ALL SUBJECTS BEFORE FILTERING ===")
                                subjectsList.forEachIndexed { index, subject ->
                                    val subjectLevel = yearLevelIdToLevelMap[subject.yearLevelId] ?: 0
                                    println("DEBUG: StudentSubjectApplicationViewModel - Subject[$index]: ${subject.name} (${subject.code}) - Type: ${subject.subjectType}, YearLevelId: ${subject.yearLevelId} (Level: $subjectLevel, ${subject.yearLevelName}), CourseId: ${subject.courseId}, Active: ${subject.active}")
                                }
                                
                                // Debug: Log student's year level and course
                                println("DEBUG: StudentSubjectApplicationViewModel - Student YearLevelId: ${user.yearLevelId}, YearLevelName: ${user.yearLevelName}, YearLevelNumber: $studentYearLevelNumber")
                                println("DEBUG: StudentSubjectApplicationViewModel - Student CourseId: ${user.courseId}, CourseCode: ${user.courseCode}")
                                
                                // Debug: Count minor subjects in all subjects
                                val allMinorSubjects = subjectsList.filter { it.subjectType == com.smartacademictracker.data.model.SubjectType.MINOR }
                                println("DEBUG: StudentSubjectApplicationViewModel - Total minor subjects in database: ${allMinorSubjects.size}")
                                allMinorSubjects.forEachIndexed { index, subject ->
                                    val subjectLevel = yearLevelIdToLevelMap[subject.yearLevelId] ?: 0
                                    println("DEBUG: StudentSubjectApplicationViewModel - All Minor[$index]: ${subject.name} (${subject.code}) - YearLevelId: ${subject.yearLevelId} (Level: $subjectLevel, ${subject.yearLevelName}), CourseId: ${subject.courseId}, Active: ${subject.active}")
                                }
                                
                                // Filter subjects based on user's year level and course
                                // - Major subjects: must match both year level ID AND course
                                // - Minor subjects: must match year level NUMBER (not ID) to allow cross-course matching
                                val filteredSubjects = subjectsList.filter { subject ->
                                    val subjectLevel = yearLevelIdToLevelMap[subject.yearLevelId] ?: 0
                                    val isActive = subject.active
                                    val matchesYearLevelById = user.yearLevelId == null || subject.yearLevelId == user.yearLevelId
                                    val matchesYearLevelByNumber = studentYearLevelNumber != null && subjectLevel == studentYearLevelNumber
                                    val isMinor = subject.subjectType == com.smartacademictracker.data.model.SubjectType.MINOR
                                    val matchesCourse = user.courseId == null || subject.courseId == user.courseId
                                    
                                    // For major subjects: match by year level ID and course
                                    // For minor subjects: match by year level NUMBER (allows cross-course matching)
                                    val passesFilter = if (isMinor) {
                                        isActive && matchesYearLevelByNumber
                                    } else {
                                        isActive && matchesYearLevelById && matchesCourse
                                    }
                                    
                                    // Debug each minor subject's filter evaluation
                                    if (subject.subjectType == com.smartacademictracker.data.model.SubjectType.MINOR) {
                                        println("DEBUG: StudentSubjectApplicationViewModel - Minor Subject Filter Check: ${subject.name} (${subject.code})")
                                        println("DEBUG: StudentSubjectApplicationViewModel -   - Is Active: $isActive")
                                        println("DEBUG: StudentSubjectApplicationViewModel -   - Subject Level: $subjectLevel, Student Level: $studentYearLevelNumber")
                                        println("DEBUG: StudentSubjectApplicationViewModel -   - Matches YearLevel by Number: $matchesYearLevelByNumber")
                                        println("DEBUG: StudentSubjectApplicationViewModel -   - Is Minor: $isMinor")
                                        println("DEBUG: StudentSubjectApplicationViewModel -   - Passes Filter: $passesFilter")
                                    }
                                    
                                    passesFilter
                                }
                                
                                _availableSubjects.value = filteredSubjects
                                _selectedYearLevel.value = user.yearLevelName ?: ""
                                _selectedCourse.value = user.courseCode ?: ""
                                _uiState.value = _uiState.value.copy(isLoading = false)
                                
                                val majorCount = filteredSubjects.count { it.subjectType == com.smartacademictracker.data.model.SubjectType.MAJOR }
                                val minorCount = filteredSubjects.count { it.subjectType == com.smartacademictracker.data.model.SubjectType.MINOR }
                                println("DEBUG: StudentSubjectApplicationViewModel - Loaded ${filteredSubjects.size} subjects for ${user.yearLevelName} ${user.courseCode} (${majorCount} major, ${minorCount} minor)")
                                
                                // Debug: Log details of minor subjects
                                val minorSubjects = filteredSubjects.filter { it.subjectType == com.smartacademictracker.data.model.SubjectType.MINOR }
                                println("DEBUG: StudentSubjectApplicationViewModel - Minor subjects found: ${minorSubjects.size}")
                                minorSubjects.forEachIndexed { index, subject ->
                                    println("DEBUG: StudentSubjectApplicationViewModel - Minor[$index]: ${subject.name} (${subject.code}) - YearLevel: ${subject.yearLevelId} (${subject.yearLevelName}), CourseId: ${subject.courseId}")
                                }
                                
                                // Debug: Log details of major subjects
                                val majorSubjects = filteredSubjects.filter { it.subjectType == com.smartacademictracker.data.model.SubjectType.MAJOR }
                                println("DEBUG: StudentSubjectApplicationViewModel - Major subjects found: ${majorSubjects.size}")
                                majorSubjects.forEachIndexed { index, subject ->
                                    println("DEBUG: StudentSubjectApplicationViewModel - Major[$index]: ${subject.name} (${subject.code}) - YearLevel: ${subject.yearLevelId} (${subject.yearLevelName}), CourseId: ${subject.courseId}")
                                }
                            }.onFailure { exception ->
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = exception.message ?: "Failed to load subjects"
                                )
                            }
                        }.onFailure { exception ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to load year levels"
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
                    // Filter subjects based on selected year level and course
                    // - Major subjects: must match both year level AND course
                    // - Minor subjects: must match year level (regardless of course)
                    val filteredSubjects = subjectsList.filter { subject ->
                        subject.active &&
                        (selectedYearLevel.value.isEmpty() || subject.yearLevelName == selectedYearLevel.value) &&
                        (subject.subjectType == com.smartacademictracker.data.model.SubjectType.MINOR || 
                         selectedCourse.value.isEmpty() || 
                         subject.courseCode == selectedCourse.value)
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
