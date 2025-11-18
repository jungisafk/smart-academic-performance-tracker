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
import com.smartacademictracker.data.repository.SectionAssignmentRepository
import com.smartacademictracker.data.model.EnrollmentStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

@HiltViewModel
class HierarchicalStudentSubjectApplicationViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val yearLevelRepository: YearLevelRepository,
    private val subjectRepository: SubjectRepository,
    private val subjectApplicationRepository: SubjectApplicationRepository,
    private val userRepository: UserRepository,
    private val studentEnrollmentRepository: StudentEnrollmentRepository,
    private val sectionAssignmentRepository: SectionAssignmentRepository,
    private val notificationSenderService: com.smartacademictracker.data.notification.NotificationSenderService
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

    private val _sectionAssignments = MutableStateFlow<List<com.smartacademictracker.data.model.SectionAssignment>>(emptyList())
    val sectionAssignments: StateFlow<List<com.smartacademictracker.data.model.SectionAssignment>> = _sectionAssignments.asStateFlow()

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

                    // Load all year levels first (needed for filtering and UI display)
                    val allYearLevelsResult = yearLevelRepository.getAllYearLevels()
                    allYearLevelsResult.onSuccess { allYearLevels ->
                        // Store all year levels for UI component to use
                        _yearLevels.value = allYearLevels
                        
                        // Load and auto-select the student's assigned year level
                        if (currentUser.yearLevelId != null) {
                            val studentYearLevel = allYearLevels.find { it.id == currentUser.yearLevelId }
                            if (studentYearLevel != null) {
                                // Auto-select the student's year level
                                _selectedYearLevelId.value = studentYearLevel.id
                            } else {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = "Your assigned year level was not found"
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
                        // Create a map of yearLevelId -> level number
                        val yearLevelIdToLevelMap = allYearLevels.associateBy({ it.id }, { it.level })
                        
                        // Get student's year level number
                        val studentYearLevelNumber = currentUser.yearLevelId?.let { yearLevelIdToLevelMap[it] }
                        
                        // Helper function to filter subjects
                        fun filterSubjectsList(subjects: List<Subject>): List<Subject> {
                            return subjects.filter { subject ->
                                val subjectLevel = yearLevelIdToLevelMap[subject.yearLevelId] ?: 0
                                val matchesYearLevelById = subject.yearLevelId == currentUser.yearLevelId
                                val matchesYearLevelByNumber = studentYearLevelNumber != null && subjectLevel == studentYearLevelNumber
                                val isMinor = subject.subjectType == com.smartacademictracker.data.model.SubjectType.MINOR
                                val matchesCourse = subject.courseId == currentUser.courseId
                                
                                // For major subjects: match by year level ID and course
                                // For minor subjects: match by year level NUMBER (allows cross-course matching)
                                if (isMinor) {
                                    matchesYearLevelByNumber
                                } else {
                                    matchesYearLevelById && matchesCourse
                                }
                            }
                        }
                        
                        // Load subjects filtered by student's course and year level
                        val subjectsResult = subjectRepository.getAllSubjects()
                        subjectsResult.onSuccess { subjectsList ->
                            // Filter subjects before displaying
                            val filteredSubjects = filterSubjectsList(subjectsList)
                            
                            // Load section assignments to filter out subjects without teachers
                            val studentCourseId = currentUser.courseId
                            val allAssignments = mutableListOf<com.smartacademictracker.data.model.SectionAssignment>()
                            
                            // Load assignments for each filtered subject in parallel
                            coroutineScope {
                                val assignmentDeferreds = filteredSubjects.map { subject ->
                                    async {
                                        val isMinor = subject.subjectType == com.smartacademictracker.data.model.SubjectType.MINOR
                                        val result = if (isMinor) {
                                            sectionAssignmentRepository.getSectionAssignmentsBySubject(subject.id)
                                        } else {
                                            if (studentCourseId != null) {
                                                sectionAssignmentRepository.getSectionAssignmentsBySubjectAndCourse(subject.id, studentCourseId)
                                            } else {
                                                return@async emptyList()
                                            }
                                        }
                                        result.getOrNull() ?: emptyList()
                                    }
                                }
                                
                                // Wait for all assignments to load
                                val assignmentResults = assignmentDeferreds.map { it.await() }
                                allAssignments.addAll(assignmentResults.flatten())
                            }
                            
                            // Filter subjects to only include those with at least one active teacher assignment
                            val subjectsWithTeachers = filteredSubjects.filter { subject ->
                                allAssignments.any { assignment ->
                                    assignment.subjectId == subject.id &&
                                    assignment.teacherId.isNotEmpty() &&
                                    assignment.status == com.smartacademictracker.data.model.AssignmentStatus.ACTIVE
                                }
                            }
                            
                            _subjects.value = subjectsWithTeachers
                            
                            // Store all assignments for later use
                            _sectionAssignments.value = allAssignments
                            
                            // Update UI state with filtering info
                            _uiState.value = _uiState.value.copy(
                                successMessage = "Showing subjects for your course and year level (${subjectsWithTeachers.size} subjects available)"
                            )
                        }.onFailure { exception ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Failed to load subjects: ${exception.message}"
                            )
                            return@onSuccess
                        }
                    }.onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to load year levels: ${exception.message}"
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
                    
                    // Validate that the section has an assigned teacher
                    // Students can only apply to sections that have active teacher assignments
                    // For minor subjects: query by subjectId only (no courseId filter)
                    // For major subjects: query by both subjectId and courseId
                    val studentCourseId = currentUser.courseId
                    val isMinor = subject.subjectType == com.smartacademictracker.data.model.SubjectType.MINOR
                    
                    if (sectionName.isNotEmpty()) {
                        val sectionAssignmentsResult = if (isMinor) {
                            // For minor subjects: query by subjectId only
                            sectionAssignmentRepository.getSectionAssignmentsBySubject(subjectId)
                        } else {
                            // For major subjects: query by both subjectId and courseId
                            if (studentCourseId != null) {
                                sectionAssignmentRepository.getSectionAssignmentsBySubjectAndCourse(subjectId, studentCourseId)
                            } else {
                                _uiState.value = _uiState.value.copy(
                                    applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                                    error = "Student course information is missing"
                                )
                                return@onSuccess
                            }
                        }
                        
                        var hasAssignedTeacher = false
                        sectionAssignmentsResult.onSuccess { assignments ->
                            hasAssignedTeacher = assignments.any { assignment ->
                                assignment.subjectId == subjectId &&
                                assignment.sectionName == sectionName &&
                                assignment.teacherId.isNotEmpty() &&
                                assignment.status == com.smartacademictracker.data.model.AssignmentStatus.ACTIVE
                            }
                        }
                        
                        // Only allow application if the specific section has an assigned teacher
                        if (!hasAssignedTeacher) {
                            _uiState.value = _uiState.value.copy(
                                applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                                error = "This section does not have an assigned teacher yet. Please wait for a teacher to be assigned before applying."
                            )
                            return@onSuccess
                        }
                    } else {
                        // If no section specified, check if subject has a teacher via section assignments
                        // A subject is considered to have a teacher if at least one section has an assigned teacher
                        val sectionAssignmentsResult = if (isMinor) {
                            // For minor subjects: query by subjectId only
                            sectionAssignmentRepository.getSectionAssignmentsBySubject(subjectId)
                        } else {
                            // For major subjects: query by both subjectId and courseId
                            if (studentCourseId != null) {
                                sectionAssignmentRepository.getSectionAssignmentsBySubjectAndCourse(subjectId, studentCourseId)
                            } else {
                                _uiState.value = _uiState.value.copy(
                                    applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                                    error = "Student course information is missing"
                                )
                                return@onSuccess
                            }
                        }
                        
                        var hasAnyAssignedTeacher = false
                        sectionAssignmentsResult.onSuccess { assignments ->
                            hasAnyAssignedTeacher = assignments.any { assignment ->
                                assignment.subjectId == subjectId &&
                                assignment.teacherId.isNotEmpty() &&
                                assignment.status == com.smartacademictracker.data.model.AssignmentStatus.ACTIVE
                            }
                        }
                        
                        if (!hasAnyAssignedTeacher) {
                            _uiState.value = _uiState.value.copy(
                                applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                                error = "This subject does not have any sections with assigned teachers yet. Please wait for a teacher to be assigned before applying."
                            )
                            return@onSuccess
                        }
                    }
                    
                    // Check if student is already enrolled in the section
                    // This check should happen BEFORE checking applications to prevent duplicate enrollments
                    if (sectionName.isNotEmpty()) {
                        // Check if student is already enrolled in this specific section
                        val isEnrolledResult = studentEnrollmentRepository.isStudentEnrolled(
                            currentUser.id,
                            subjectId,
                            sectionName
                        )
                        val isEnrolled = if (isEnrolledResult.isSuccess) {
                            isEnrolledResult.getOrNull() ?: false
                        } else {
                            false
                        }
                        if (isEnrolled) {
                            _uiState.value = _uiState.value.copy(
                                applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                                error = "You are already enrolled in section $sectionName of this subject"
                            )
                            return@onSuccess
                        }
                    } else {
                        // If no section specified, check if student is enrolled in any section of this subject
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
                    }
                    
                    // Check for existing applications
                    // Block if there's a PENDING application for the same subject and section
                    val pendingApplication = if (sectionName.isNotEmpty()) {
                        _myApplications.value.find { 
                            it.subjectId == subjectId && 
                            it.sectionName == sectionName && 
                            it.status == ApplicationStatus.PENDING 
                        }
                    } else {
                        _myApplications.value.find { 
                            it.subjectId == subjectId && 
                            it.status == ApplicationStatus.PENDING 
                        }
                    }
                    if (pendingApplication != null) {
                        val errorMsg = if (sectionName.isNotEmpty()) {
                            "You already have a pending application for section $sectionName of this subject"
                        } else {
                            "You already have a pending application for this subject"
                        }
                        _uiState.value = _uiState.value.copy(
                            applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                            error = errorMsg
                        )
                        return@onSuccess
                    }
                    
                    // Check for APPROVED application - only block if student is actively enrolled
                    // If student was KICKED or DROPPED, allow reapplication even if there's an APPROVED application
                    val approvedApplication = if (sectionName.isNotEmpty()) {
                        _myApplications.value.find { 
                            it.subjectId == subjectId && 
                            it.sectionName == sectionName && 
                            it.status == ApplicationStatus.APPROVED 
                        }
                    } else {
                        _myApplications.value.find { 
                            it.subjectId == subjectId && 
                            it.status == ApplicationStatus.APPROVED 
                        }
                    }
                    if (approvedApplication != null) {
                        // Check if student has an ACTIVE enrollment
                        // If not ACTIVE (e.g., KICKED, DROPPED), allow reapplication
                        val hasActiveEnrollment = if (sectionName.isNotEmpty()) {
                            val isEnrolledResult = studentEnrollmentRepository.isStudentEnrolled(
                                currentUser.id,
                                subjectId,
                                sectionName
                            )
                            isEnrolledResult.getOrNull() ?: false
                        } else {
                            // Use getStudentEnrollmentsBySubject to check ALL enrollments (including KICKED/DROPPED)
                            val enrollmentsResult = studentEnrollmentRepository.getStudentEnrollmentsBySubject(currentUser.id, subjectId)
                            if (enrollmentsResult.isSuccess) {
                                val enrollments = enrollmentsResult.getOrNull().orEmpty()
                                enrollments.any { it.status == EnrollmentStatus.ACTIVE }
                            } else {
                                false
                            }
                        }
                        if (hasActiveEnrollment) {
                            _uiState.value = _uiState.value.copy(
                                applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                                error = if (sectionName.isNotEmpty()) {
                                    "You are already enrolled in section $sectionName of this subject"
                                } else {
                                    "You are already enrolled in this subject"
                                }
                            )
                            return@onSuccess
                        }
                        // If there's an APPROVED application but no ACTIVE enrollment,
                        // the student was likely KICKED or DROPPED - allow reapplication
                    }
                    
                    // Allow reapplication if previous application was WITHDRAWN or REJECTED
                    // (No need to check - they can always reapply if not PENDING or enrolled)
                    
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
                        // Notify teacher about the new student application
                        notifyTeacherOfStudentApplication(application)
                        
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
                // Get current user to load their applications
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { currentUser ->
                    if (currentUser != null) {
                        val result = subjectApplicationRepository.getApplicationsByStudentId(currentUser.id)
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

    fun cancelApplication(applicationId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = subjectApplicationRepository.withdrawApplication(applicationId)
                result.onSuccess {
                    // Reload applications to update UI
                    loadMyApplications()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Application cancelled successfully"
                    )
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to cancel application"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to cancel application"
                )
            }
        }
    }

    fun clearApplicationSuccess() {
        _uiState.value = _uiState.value.copy(isApplicationSuccess = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }

    private fun loadSectionAssignments() {
        viewModelScope.launch {
            try {
                // Get current user to get their courseId
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { currentUser ->
                    if (currentUser == null) {
                        return@onSuccess
                    }
                    
                    val studentCourseId = currentUser.courseId
                    
                    // Load section assignments for each subject individually
                    // For minor subjects: query by subjectId only (no courseId filter)
                    // For major subjects: query by both subjectId and courseId
                    val allAssignments = mutableListOf<com.smartacademictracker.data.model.SectionAssignment>()
                    val subjects = _subjects.value
                    
                    // Load assignments for each subject
                    subjects.forEach { subject ->
                        val isMinor = subject.subjectType == com.smartacademictracker.data.model.SubjectType.MINOR
                        val result = if (isMinor) {
                            // For minor subjects: query by subjectId only (they don't have courseId)
                            sectionAssignmentRepository.getSectionAssignmentsBySubject(subject.id)
                        } else {
                            // For major subjects: query by both subjectId and courseId
                            if (studentCourseId != null) {
                                sectionAssignmentRepository.getSectionAssignmentsBySubjectAndCourse(subject.id, studentCourseId)
                            } else {
                                return@forEach
                            }
                        }
                        
                        result.onSuccess { assignments ->
                            allAssignments.addAll(assignments)
                            // Update the list after each successful load
                            _sectionAssignments.value = allAssignments.toList()
                        }
                    }
                }
            } catch (e: Exception) {
                // Silent failure
            }
        }
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
    
    /**
     * Notify teacher when a student applies for their subject/section
     */
    private suspend fun notifyTeacherOfStudentApplication(application: SubjectApplication) {
        try {
            // Get subject to check if it's minor or major
            val subjectResult = subjectRepository.getSubjectById(application.subjectId)
            subjectResult.onSuccess { subject ->
                if (subject == null) {
                    return@onSuccess
                }
                
                // Get teacher ID from section assignments (not from subject.teacherId)
                val isMinor = subject.subjectType == com.smartacademictracker.data.model.SubjectType.MINOR
                val sectionAssignmentsResult = if (isMinor) {
                    sectionAssignmentRepository.getSectionAssignmentsBySubject(application.subjectId)
                } else {
                    if (application.courseId.isNotEmpty()) {
                        sectionAssignmentRepository.getSectionAssignmentsBySubjectAndCourse(application.subjectId, application.courseId)
                    } else {
                        return@onSuccess
                    }
                }
                
                sectionAssignmentsResult.onSuccess { assignments ->
                    // Find the teacher assigned to the section the student applied for
                    val matchingAssignment = if (application.sectionName.isNotEmpty()) {
                        assignments.find { assignment ->
                            assignment.subjectId == application.subjectId &&
                            (assignment.sectionName == application.sectionName ||
                             assignment.sectionName.endsWith(application.sectionName) ||
                             application.sectionName.endsWith(assignment.sectionName.takeLast(1))) &&
                            assignment.teacherId.isNotEmpty() &&
                            assignment.status == com.smartacademictracker.data.model.AssignmentStatus.ACTIVE
                        }
                    } else {
                        // If no section specified, get the first active assignment with a teacher
                        assignments.firstOrNull { assignment ->
                            assignment.subjectId == application.subjectId &&
                            assignment.teacherId.isNotEmpty() &&
                            assignment.status == com.smartacademictracker.data.model.AssignmentStatus.ACTIVE
                        }
                    }
                    
                    val teacherId = matchingAssignment?.teacherId
                    if (teacherId != null && teacherId.isNotEmpty()) {
                        notificationSenderService.sendNotification(
                            userId = teacherId,
                            type = com.smartacademictracker.data.model.NotificationType.STUDENT_APPLICATION_SUBMITTED,
                            variables = mapOf(
                                "studentName" to application.studentName,
                                "subjectName" to application.subjectName,
                                "sectionName" to application.sectionName,
                                "applicationId" to application.id
                            ),
                            priority = com.smartacademictracker.data.model.NotificationPriority.NORMAL
                        )
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("StudentApplication", "Failed to notify teacher of student application: ${e.message}")
        }
    }
}

data class HierarchicalStudentSubjectApplicationUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val isApplicationSuccess: Boolean = false,
    val applyingSubjects: Set<String> = emptySet()
)
