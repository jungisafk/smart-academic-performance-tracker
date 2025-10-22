package com.smartacademictracker.presentation.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.Course
import com.smartacademictracker.data.model.YearLevel
import com.smartacademictracker.data.model.Subject
import com.smartacademictracker.data.model.SubjectApplication
import com.smartacademictracker.data.model.ApplicationStatus
import com.smartacademictracker.data.repository.CourseRepository
import com.smartacademictracker.data.repository.YearLevelRepository
import com.smartacademictracker.data.repository.SubjectRepository
import com.smartacademictracker.data.repository.SubjectApplicationRepository
import com.smartacademictracker.data.repository.UserRepository
import com.smartacademictracker.data.repository.StudentEnrollmentRepository
import com.smartacademictracker.data.model.EnrollmentStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HierarchicalStudentSubjectApplicationViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val yearLevelRepository: YearLevelRepository,
    private val subjectRepository: SubjectRepository,
    private val subjectApplicationRepository: SubjectApplicationRepository,
    private val userRepository: UserRepository,
    private val studentEnrollmentRepository: StudentEnrollmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HierarchicalStudentSubjectApplicationUiState())
    val uiState: StateFlow<HierarchicalStudentSubjectApplicationUiState> = _uiState.asStateFlow()

    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()

    private val _yearLevels = MutableStateFlow<List<YearLevel>>(emptyList())
    val yearLevels: StateFlow<List<YearLevel>> = _yearLevels.asStateFlow()

    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    val subjects: StateFlow<List<Subject>> = _subjects.asStateFlow()

    private val _myApplications = MutableStateFlow<List<SubjectApplication>>(emptyList())
    val myApplications: StateFlow<List<SubjectApplication>> = _myApplications.asStateFlow()

    private val _selectedCourseId = MutableStateFlow<String?>(null)
    val selectedCourseId: StateFlow<String?> = _selectedCourseId.asStateFlow()

    private val _selectedYearLevelId = MutableStateFlow<String?>(null)
    val selectedYearLevelId: StateFlow<String?> = _selectedYearLevelId.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Get current user to filter subjects by their course and year level
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { currentUser ->
                    if (currentUser == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "User not found"
                        )
                        return@onSuccess
                    }

                    // Load only the student's assigned course
                    if (currentUser.courseId != null) {
                        val courseResult = courseRepository.getCourseById(currentUser.courseId)
                        courseResult.onSuccess { course ->
                            if (course != null) {
                                _courses.value = listOf(course)
                                // Auto-select the student's course
                                _selectedCourseId.value = course.id
                            } else {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = "Your assigned course was not found"
                                )
                                return@onSuccess
                            }
                        }.onFailure { exception ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Failed to load your course: ${exception.message}"
                            )
                            return@onSuccess
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "No course assigned to your account. Please contact your administrator."
                        )
                        return@onSuccess
                    }

                    // Load only the student's assigned year level
                    if (currentUser.yearLevelId != null) {
                        val yearLevelResult = yearLevelRepository.getYearLevelById(currentUser.yearLevelId)
                        yearLevelResult.onSuccess { yearLevel ->
                            if (yearLevel != null) {
                                _yearLevels.value = listOf(yearLevel)
                                // Auto-select the student's year level
                                _selectedYearLevelId.value = yearLevel.id
                            } else {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = "Your assigned year level was not found"
                                )
                                return@onSuccess
                            }
                        }.onFailure { exception ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Failed to load your year level: ${exception.message}"
                            )
                            return@onSuccess
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "No year level assigned to your account. Please contact your administrator."
                        )
                        return@onSuccess
                    }

                    // Load subjects filtered by student's course and year level
                    val subjectsResult = subjectRepository.getAllSubjects()
                    subjectsResult.onSuccess { subjectsList ->
                        println("DEBUG: HierarchicalStudentSubjectApplicationViewModel - Loaded ${subjectsList.size} total subjects")
                        
                        // Filter subjects by student's course and year level
                        val filteredSubjects = subjectsList.filter { subject ->
                            subject.courseId == currentUser.courseId && 
                            subject.yearLevelId == currentUser.yearLevelId
                        }
                        
                        println("DEBUG: HierarchicalStudentSubjectApplicationViewModel - Filtered to ${filteredSubjects.size} subjects for course: ${currentUser.courseId}, year level: ${currentUser.yearLevelId}")
                        _subjects.value = filteredSubjects
                        
                        // Update UI state with filtering info
                        _uiState.value = _uiState.value.copy(
                            successMessage = "Showing subjects for your course and year level (${filteredSubjects.size} subjects available)"
                        )
                    }.onFailure { exception ->
                        println("DEBUG: HierarchicalStudentSubjectApplicationViewModel - Failed to load subjects: ${exception.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to load subjects: ${exception.message}"
                        )
                        return@onSuccess
                    }

                    // Load my applications
                    loadMyApplications()

                    _uiState.value = _uiState.value.copy(isLoading = false)
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to get user information: ${exception.message}"
                    )
                }
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

    fun applyForSubject(subjectId: String, sectionName: String = "") {
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
                            error = "Subject not found"
                        )
                        return@onSuccess
                    }
                    
                    // Allow re-apply if no ACTIVE enrollment exists even if there was an approved application before
                    val existingApplication = _myApplications.value.find { 
                        it.subjectId == subjectId && it.status == ApplicationStatus.APPROVED 
                    }
                    if (existingApplication != null) {
                        // Check current enrollments in new section-based collection
                        val studentEnrollmentsResult = studentEnrollmentRepository.getStudentsBySubject(subjectId)
                        val hasActiveEnrollment = if (studentEnrollmentsResult.isSuccess) {
                            val list = studentEnrollmentsResult.getOrNull().orEmpty()
                            list.any { it.studentId == currentUser.id && it.status == EnrollmentStatus.ACTIVE }
                        } else {
                            false
                        }
                        if (hasActiveEnrollment) {
                            _uiState.value = _uiState.value.copy(
                                applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                                error = "You are already enrolled in this subject"
                            )
                            return@onSuccess
                        }
                        // else, let them apply again
                    }
                    
                    // Create subject application
                    val application = SubjectApplication(
                        studentId = currentUser.id,
                        studentName = "${currentUser.firstName} ${currentUser.lastName}",
                        subjectId = subjectId,
                        subjectName = subject.name,
                        sectionName = sectionName,
                        courseId = subject.courseId,
                        courseName = subject.courseName,
                        yearLevelId = subject.yearLevelId,
                        yearLevelName = subject.yearLevelName,
                        semester = subject.semester,
                        academicYear = subject.academicYear,
                        appliedDate = System.currentTimeMillis(),
                        status = ApplicationStatus.PENDING
                    )
                    
                    val result = subjectApplicationRepository.createApplication(application)
                    result.onSuccess {
                        _uiState.value = _uiState.value.copy(
                            applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                            isApplicationSuccess = true
                        )
                        // Reload applications to show the new one
                        loadMyApplications()
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
                println("DEBUG: HierarchicalStudentSubjectApplicationViewModel - Loading my applications...")
                // Get current user to load their applications
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { currentUser ->
                    if (currentUser != null) {
                        println("DEBUG: HierarchicalStudentSubjectApplicationViewModel - Current user: ${currentUser.id}")
                        val result = subjectApplicationRepository.getApplicationsByStudentId(currentUser.id)
                        result.onSuccess { applications ->
                            println("DEBUG: HierarchicalStudentSubjectApplicationViewModel - Loaded ${applications.size} applications")
                            _myApplications.value = applications
                        }.onFailure { exception ->
                            println("DEBUG: HierarchicalStudentSubjectApplicationViewModel - Failed to load applications: ${exception.message}")
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

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}

data class HierarchicalStudentSubjectApplicationUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val isApplicationSuccess: Boolean = false,
    val applyingSubjects: Set<String> = emptySet()
)
