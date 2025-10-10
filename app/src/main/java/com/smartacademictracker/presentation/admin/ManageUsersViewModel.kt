package com.smartacademictracker.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

@HiltViewModel
class ManageUsersViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val courseRepository: com.smartacademictracker.data.repository.CourseRepository,
    private val sectionAssignmentRepository: com.smartacademictracker.data.repository.SectionAssignmentRepository,
    private val subjectRepository: com.smartacademictracker.data.repository.SubjectRepository,
    private val studentEnrollmentRepository: com.smartacademictracker.data.repository.StudentEnrollmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManageUsersUiState())
    val uiState: StateFlow<ManageUsersUiState> = _uiState.asStateFlow()

    private val _users = MutableStateFlow<List<com.smartacademictracker.data.model.User>>(emptyList())
    val users: StateFlow<List<com.smartacademictracker.data.model.User>> = _users.asStateFlow()

    private val _courses = MutableStateFlow<List<com.smartacademictracker.data.model.Course>>(emptyList())
    val courses: StateFlow<List<com.smartacademictracker.data.model.Course>> = _courses.asStateFlow()

    // Map of teacherId -> List of section assignments with subject names
    private val _teacherAssignments = MutableStateFlow<Map<String, List<TeacherAssignmentInfo>>>(emptyMap())
    val teacherAssignments: StateFlow<Map<String, List<TeacherAssignmentInfo>>> = _teacherAssignments.asStateFlow()

    // Map of studentId -> List of enrollments
    private val _studentEnrollments = MutableStateFlow<Map<String, List<StudentEnrollmentInfo>>>(emptyMap())
    val studentEnrollments: StateFlow<Map<String, List<StudentEnrollmentInfo>>> = _studentEnrollments.asStateFlow()

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Load all data in parallel for faster loading
                coroutineScope {
                    val coursesDeferred = async { courseRepository.getAllCourses() }
                    val assignmentsDeferred = async { sectionAssignmentRepository.getAllSectionAssignments() }
                    val subjectsDeferred = async { subjectRepository.getAllSubjects() }
                    val enrollmentsDeferred = async { studentEnrollmentRepository.getAllActiveEnrollments() }
                    val usersDeferred = async { userRepository.getAllUsers() }
                    
                    // Wait for all operations to complete
                    val coursesResult = coursesDeferred.await()
                    val assignmentsResult = assignmentsDeferred.await()
                    val subjectsResult = subjectsDeferred.await()
                    val enrollmentsResult = enrollmentsDeferred.await()
                    val usersResult = usersDeferred.await()
                    
                    // Process results
                    coursesResult.onSuccess { coursesList ->
                        _courses.value = coursesList
                    }
                    
                    // Process assignments and subjects together
                    assignmentsResult.onSuccess { assignments ->
                        subjectsResult.onSuccess { subjects ->
                            val assignmentsMap = mutableMapOf<String, MutableList<TeacherAssignmentInfo>>()
                            assignments.forEach { assignment ->
                                val subject = subjects.find { it.id == assignment.subjectId }
                                val info = TeacherAssignmentInfo(
                                    assignmentId = assignment.id,
                                    subjectId = assignment.subjectId,
                                    subjectName = subject?.name ?: "Unknown Subject",
                                    subjectCode = subject?.code ?: "",
                                    sectionName = assignment.sectionName
                                )
                                assignmentsMap.getOrPut(assignment.teacherId) { mutableListOf() }.add(info)
                            }
                            _teacherAssignments.value = assignmentsMap
                        }
                    }
                    
                    // Process enrollments
                    enrollmentsResult.onSuccess { enrollments ->
                        val enrollmentsMap = mutableMapOf<String, MutableList<StudentEnrollmentInfo>>()
                        enrollments.forEach { enrollment ->
                            val info = StudentEnrollmentInfo(
                                enrollmentId = enrollment.id,
                                subjectId = enrollment.subjectId,
                                subjectName = enrollment.subjectName,
                                subjectCode = enrollment.subjectCode,
                                sectionName = enrollment.sectionName,
                                courseName = enrollment.courseName,
                                courseId = enrollment.courseId
                            )
                            enrollmentsMap.getOrPut(enrollment.studentId) { mutableListOf() }.add(info)
                        }
                        _studentEnrollments.value = enrollmentsMap
                    }
                    
                    // Process users with computed fields
                    usersResult.onSuccess { usersList ->
                        coursesResult.onSuccess { coursesList ->
                            val usersWithComputedFields = usersList.map { user ->
                                if (user.role == "TEACHER" && user.departmentCourseId != null) {
                                    val departmentCourse = coursesList.find { it.id == user.departmentCourseId }
                                    user.copy(
                                        departmentCourseName = departmentCourse?.name,
                                        departmentCourseCode = departmentCourse?.code
                                    )
                                } else {
                                    user
                                }
                            }
                            _users.value = usersWithComputedFields
                            _uiState.value = _uiState.value.copy(isLoading = false)
                            println("DEBUG: ManageUsersViewModel - Loaded ${usersList.size} users")
                        }
                    }.onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load users"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load users"
                )
            }
        }
    }

    fun updateUserStatus(userId: String, active: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                processingUsers = _uiState.value.processingUsers + userId,
                error = null
            )
            
            try {
                val result = userRepository.updateUserStatus(userId, active)
                result.onSuccess {
                    // Reload users to reflect changes
                    loadUsers()
                    _uiState.value = _uiState.value.copy(
                        processingUsers = _uiState.value.processingUsers - userId
                    )
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        processingUsers = _uiState.value.processingUsers - userId,
                        error = exception.message ?: "Failed to update user status"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    processingUsers = _uiState.value.processingUsers - userId,
                    error = e.message ?: "Failed to update user status"
                )
            }
        }
    }

    fun updateUserRole(userId: String, newRole: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                processingUsers = _uiState.value.processingUsers + userId,
                error = null
            )
            
            try {
                val result = userRepository.updateUserRole(userId, newRole)
                result.onSuccess {
                    // Reload users to reflect changes
                    loadUsers()
                    _uiState.value = _uiState.value.copy(
                        processingUsers = _uiState.value.processingUsers - userId
                    )
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        processingUsers = _uiState.value.processingUsers - userId,
                        error = exception.message ?: "Failed to update user role"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    processingUsers = _uiState.value.processingUsers - userId,
                    error = e.message ?: "Failed to update user role"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun refreshUsers() {
        loadUsers()
    }

    fun updateTeacherDepartment(userId: String, departmentCourseId: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                processingUsers = _uiState.value.processingUsers + userId,
                error = null
            )
            
            try {
                val result = userRepository.updateTeacherDepartment(userId, departmentCourseId)
                result.onSuccess {
                    // Reload users to reflect changes
                    loadUsers()
                    _uiState.value = _uiState.value.copy(
                        processingUsers = _uiState.value.processingUsers - userId
                    )
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        processingUsers = _uiState.value.processingUsers - userId,
                        error = exception.message ?: "Failed to update teacher department"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    processingUsers = _uiState.value.processingUsers - userId,
                    error = e.message ?: "Failed to update teacher department"
                )
            }
        }
    }
}

data class ManageUsersUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val processingUsers: Set<String> = emptySet()
)

// Data class for teacher assignment information
data class TeacherAssignmentInfo(
    val assignmentId: String,
    val subjectId: String,
    val subjectName: String,
    val subjectCode: String,
    val sectionName: String
)

// Data class for student enrollment information
data class StudentEnrollmentInfo(
    val enrollmentId: String,
    val subjectId: String,
    val subjectName: String,
    val subjectCode: String,
    val sectionName: String,
    val courseName: String,
    val courseId: String
)
