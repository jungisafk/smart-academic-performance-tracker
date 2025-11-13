package com.smartacademictracker.presentation.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.repository.UserRepository
import com.smartacademictracker.data.repository.SubjectRepository
import com.smartacademictracker.data.repository.EnrollmentRepository
import com.smartacademictracker.data.repository.StudentEnrollmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeacherDashboardViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val subjectRepository: SubjectRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val studentEnrollmentRepository: StudentEnrollmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherDashboardUiState())
    val uiState: StateFlow<TeacherDashboardUiState> = _uiState.asStateFlow()

    private val _teacherId = MutableStateFlow<String?>(null)
    private val _subjects = MutableStateFlow<List<com.smartacademictracker.data.model.Subject>>(emptyList())
    private val _enrollments = MutableStateFlow<List<com.smartacademictracker.data.model.Enrollment>>(emptyList())

    init {
        // Set up real-time data flow
        viewModelScope.launch {
            combine(_teacherId, _subjects, _enrollments) { teacherId, subjects, enrollments ->
                println("DEBUG: TeacherDashboardViewModel - Combine triggered: teacherId=$teacherId, subjects=${subjects.size}, enrollments=${enrollments.size}, isLoading=${_uiState.value.isLoading}")
                
                if (teacherId != null) {
                    val teacherSubjects = subjects.filter { it.teacherId == teacherId && it.active }
                    val subjectIds = teacherSubjects.map { it.id }
                    val teacherEnrollments = enrollments.filter { it.subjectId in subjectIds }
                    val totalStudents = teacherEnrollments.distinctBy { it.studentId }.size
                    
                    val previousState = _uiState.value
                    val newState = previousState.copy(
                        activeSubjects = teacherSubjects.size,
                        totalStudents = totalStudents,
                        isLoading = false
                    )
                    
                    println("DEBUG: TeacherDashboardViewModel - Real-time update:")
                    println("  - Previous state: activeSubjects=${previousState.activeSubjects}, totalStudents=${previousState.totalStudents}")
                    println("  - New state: activeSubjects=${newState.activeSubjects}, totalStudents=${newState.totalStudents}")
                    println("  - Teacher subjects: ${teacherSubjects.size} (IDs: ${teacherSubjects.map { it.id }})")
                    println("  - Subject IDs: $subjectIds")
                    println("  - Teacher enrollments: ${teacherEnrollments.size}")
                    println("  - Unique students: $totalStudents")
                    
                    _uiState.value = newState
                } else {
                    println("DEBUG: TeacherDashboardViewModel - Skipping update: teacherId is null")
                }
            }.collect { }
        }
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Get current user (teacher)
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        _teacherId.value = user.id
                        println("DEBUG: TeacherDashboardViewModel - Loading data for teacher: ${user.id}")
                        
                        // Load subjects and enrollments in parallel
                        val subjectsResult = subjectRepository.getAllSubjects()
                        // Use new student_enrollments collection for counts
                        val studentEnrollmentsResult = studentEnrollmentRepository.getActiveEnrollmentsByTeacher(user.id)
                        
                        // Process subjects
                        subjectsResult.onSuccess { subjectsList ->
                            println("DEBUG: TeacherDashboardViewModel - SubjectRepository returned ${subjectsList.size} subjects")
                            _subjects.value = subjectsList
                            println("DEBUG: TeacherDashboardViewModel - Updated _subjects StateFlow with ${subjectsList.size} subjects")
                            
                            // Calculate teacher-specific data
                            val teacherSubjects = subjectsList.filter { it.teacherId == user.id && it.active }
                            println("DEBUG: TeacherDashboardViewModel - Teacher has ${teacherSubjects.size} active subjects")
                            println("DEBUG: TeacherDashboardViewModel - Active subject details: ${teacherSubjects.map { "${it.name} (${it.id})" }}")
                        }.onFailure { exception ->
                            println("DEBUG: TeacherDashboardViewModel - Error loading subjects: ${exception.message}")
                            println("DEBUG: TeacherDashboardViewModel - Exception: ${exception.stackTraceToString()}")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to load subjects"
                            )
                        }
                        
                        // Process enrollments - use new StudentEnrollmentRepository
                        if (studentEnrollmentsResult.isSuccess) {
                            val se = studentEnrollmentsResult.getOrNull().orEmpty()
                            println("DEBUG: TeacherDashboardViewModel - StudentEnrollmentRepository returned ${se.size} enrollments")
                            val legacyMapped = se.map { e ->
                                com.smartacademictracker.data.model.Enrollment(
                                    id = e.id,
                                    studentId = e.studentId,
                                    studentName = e.studentName,
                                    subjectId = e.subjectId,
                                    subjectName = e.subjectName,
                                    subjectCode = e.subjectCode,
                                    enrolledAt = e.enrollmentDate,
                                    semester = e.semester.name,
                                    academicYear = e.academicYear,
                                    active = e.status.name == "ACTIVE"
                                )
                            }
                            _enrollments.value = legacyMapped
                            println("DEBUG: TeacherDashboardViewModel - Updated _enrollments StateFlow with ${legacyMapped.size} enrollments")
                            println("DEBUG: TeacherDashboardViewModel - Enrollment details: ${legacyMapped.map { "${it.studentName} -> ${it.subjectName}" }}")
                        } else {
                            // If student enrollments fail to load, set empty list
                            _enrollments.value = emptyList()
                            val error = studentEnrollmentsResult.exceptionOrNull()
                            println("DEBUG: TeacherDashboardViewModel - Error loading enrollments (student_enrollments): ${error?.message}")
                            println("DEBUG: TeacherDashboardViewModel - Exception: ${error?.stackTraceToString()}")
                        }
                        
                        // Set loading to false after both operations complete
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "User not found"
                        )
                    }
                }.onFailure { exception ->
                    println("DEBUG: TeacherDashboardViewModel - Error loading user: ${exception.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load user data"
                    )
                }
            } catch (e: Exception) {
                println("DEBUG: TeacherDashboardViewModel - Error loading dashboard data: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load dashboard data"
                )
            }
        }
    }

    fun refreshData() {
        loadDashboardData()
    }
    
    fun updateEnrollments(enrollments: List<com.smartacademictracker.data.model.Enrollment>) {
        _enrollments.value = enrollments
        println("DEBUG: TeacherDashboardViewModel - Updated enrollments: ${enrollments.size} total")
    }
    
    fun updateSubjects(subjects: List<com.smartacademictracker.data.model.Subject>) {
        _subjects.value = subjects
        println("DEBUG: TeacherDashboardViewModel - Updated subjects: ${subjects.size} total")
    }
}

data class TeacherDashboardUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val activeSubjects: Int = 0,
    val totalStudents: Int = 0
)
