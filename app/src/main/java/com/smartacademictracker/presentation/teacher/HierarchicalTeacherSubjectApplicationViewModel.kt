package com.smartacademictracker.presentation.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.Course
import com.smartacademictracker.data.model.YearLevel
import com.smartacademictracker.data.model.Subject
import com.smartacademictracker.data.model.TeacherApplication
import com.smartacademictracker.data.model.ApplicationStatus
import com.smartacademictracker.data.repository.CourseRepository
import com.smartacademictracker.data.repository.YearLevelRepository
import com.smartacademictracker.data.repository.SubjectRepository
import com.smartacademictracker.data.repository.TeacherApplicationRepository
import com.smartacademictracker.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HierarchicalTeacherSubjectApplicationViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val yearLevelRepository: YearLevelRepository,
    private val subjectRepository: SubjectRepository,
    private val teacherApplicationRepository: TeacherApplicationRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HierarchicalTeacherSubjectApplicationUiState())
    val uiState: StateFlow<HierarchicalTeacherSubjectApplicationUiState> = _uiState.asStateFlow()

    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()

    private val _yearLevels = MutableStateFlow<List<YearLevel>>(emptyList())
    val yearLevels: StateFlow<List<YearLevel>> = _yearLevels.asStateFlow()

    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    val subjects: StateFlow<List<Subject>> = _subjects.asStateFlow()

    private val _myApplications = MutableStateFlow<List<TeacherApplication>>(emptyList())
    val myApplications: StateFlow<List<TeacherApplication>> = _myApplications.asStateFlow()

    private val _selectedCourseId = MutableStateFlow<String?>(null)
    val selectedCourseId: StateFlow<String?> = _selectedCourseId.asStateFlow()

    private val _selectedYearLevelId = MutableStateFlow<String?>(null)
    val selectedYearLevelId: StateFlow<String?> = _selectedYearLevelId.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Load courses
                val coursesResult = courseRepository.getAllCourses()
                coursesResult.onSuccess { coursesList ->
                    _courses.value = coursesList
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load courses: ${exception.message}"
                    )
                    return@launch
                }

                // Load year levels
                val yearLevelsResult = yearLevelRepository.getAllYearLevels()
                yearLevelsResult.onSuccess { yearLevelsList ->
                    _yearLevels.value = yearLevelsList
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load year levels: ${exception.message}"
                    )
                    return@launch
                }

                // Load subjects and filter based on teacher's department
                val subjectsResult = subjectRepository.getAllSubjects()
                subjectsResult.onSuccess { subjectsList ->
                    // Get current user to filter by department
                    val currentUserResult = userRepository.getCurrentUser()
                    currentUserResult.onSuccess { currentUser ->
                        if (currentUser != null) {
                            // Filter subjects based on department and subject type
                            val filteredSubjects = subjectsList.filter { subject ->
                                when (subject.subjectType) {
                                    com.smartacademictracker.data.model.SubjectType.MAJOR -> {
                                        // MAJOR subjects: only visible to teachers of the same course/department
                                        currentUser.departmentCourseId != null && subject.courseId == currentUser.departmentCourseId
                                    }
                                    com.smartacademictracker.data.model.SubjectType.MINOR -> {
                                        // MINOR subjects: visible to all teachers
                                        true
                                    }
                                }
                            }
                            _subjects.value = filteredSubjects
                        } else {
                            _subjects.value = subjectsList
                        }
                    }.onFailure {
                        _subjects.value = subjectsList
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load subjects: ${exception.message}"
                    )
                    return@launch
                }

                // Load my applications
                loadMyApplications()

                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load data"
                )
            }
        }
    }

    fun selectCourse(courseId: String?) {
        _selectedCourseId.value = courseId
        _selectedYearLevelId.value = null // Reset year level selection
    }

    fun selectYearLevel(yearLevelId: String?) {
        _selectedYearLevelId.value = yearLevelId
    }

    fun applyForSubject(subjectId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                applyingSubjects = _uiState.value.applyingSubjects + subjectId,
                error = null
            )
            
            try {
                // Get current user to create application
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { currentUser ->
                    if (currentUser == null) {
                        _uiState.value = _uiState.value.copy(
                            applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                            error = "User not found"
                        )
                        return@onSuccess
                    }
                    
                    // Find the subject to get details
                    val subject = _subjects.value.find { it.id == subjectId }
                    if (subject == null) {
                        _uiState.value = _uiState.value.copy(
                            applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                            error = "Subject not found or you don't have permission to apply for this subject"
                        )
                        return@onSuccess
                    }
                    
                    // Validate that teacher can apply for this subject based on department and type
                    val canApply = when (subject.subjectType) {
                        com.smartacademictracker.data.model.SubjectType.MAJOR -> {
                            // MAJOR subjects: only teachers of the same course/department can apply
                            currentUser.departmentCourseId != null && subject.courseId == currentUser.departmentCourseId
                        }
                        com.smartacademictracker.data.model.SubjectType.MINOR -> {
                            // MINOR subjects: any teacher can apply
                            true
                        }
                    }
                    
                    if (!canApply) {
                        _uiState.value = _uiState.value.copy(
                            applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                            error = "You don't have permission to apply for this subject. MAJOR subjects are only available to teachers in the same department."
                        )
                        return@onSuccess
                    }
                    
                    // Check if teacher has an active application (PENDING or APPROVED) for this subject
                    // Allow reapplication if previous application was REJECTED
                    val hasActiveResult = teacherApplicationRepository.hasTeacherActiveApplication(currentUser.id, subjectId)
                    hasActiveResult.onSuccess { hasActive ->
                        if (hasActive) {
                            _uiState.value = _uiState.value.copy(
                                applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                                error = "You have already applied for this subject. Please check your applications."
                            )
                            return@onSuccess
                        }
                    }.onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                            error = exception.message ?: "Failed to check application status"
                        )
                        return@onSuccess
                    }
                    
                    // Create teacher application
                    val application = TeacherApplication(
                        teacherId = currentUser.id,
                        teacherName = "${currentUser.firstName} ${currentUser.lastName}",
                        teacherEmail = currentUser.email,
                        subjectId = subjectId,
                        subjectName = subject.name,
                        subjectCode = subject.code,
                        applicationReason = "Application to teach ${subject.name}",
                        appliedAt = System.currentTimeMillis(),
                        status = ApplicationStatus.PENDING
                    )
                    
                    val result = teacherApplicationRepository.createApplication(application)
                    result.onSuccess {
                        _uiState.value = _uiState.value.copy(
                            applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                            isApplicationSuccess = true
                        )
                        // Reload applications to show the new one (non-blocking)
                        viewModelScope.launch {
                            loadMyApplications()
                        }
                    }.onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                            error = exception.message ?: "Failed to apply for subject"
                        )
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                        error = exception.message ?: "Failed to get user information"
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

    fun loadMyApplications() {
        viewModelScope.launch {
            try {
                // Get current user to load their applications
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { currentUser ->
                    if (currentUser != null) {
                        val result = teacherApplicationRepository.getApplicationsByTeacher(currentUser.id)
                        result.onSuccess { applications ->
                            _myApplications.value = applications
                        }.onFailure { exception ->
                            _uiState.value = _uiState.value.copy(
                                error = "Failed to load applications: ${exception.message}"
                            )
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            error = "User not found"
                        )
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to get user information: ${exception.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load applications: ${e.message}"
                )
            }
        }
    }

    fun refreshData() {
        loadData()
    }

    fun clearApplicationSuccess() {
        _uiState.value = _uiState.value.copy(isApplicationSuccess = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class HierarchicalTeacherSubjectApplicationUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isApplicationSuccess: Boolean = false,
    val applyingSubjects: Set<String> = emptySet()
)
