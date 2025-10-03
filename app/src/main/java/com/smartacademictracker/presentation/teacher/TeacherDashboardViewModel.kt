package com.smartacademictracker.presentation.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.repository.UserRepository
import com.smartacademictracker.data.repository.SubjectRepository
import com.smartacademictracker.data.repository.EnrollmentRepository
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
    private val enrollmentRepository: EnrollmentRepository
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
                if (teacherId != null && !_uiState.value.isLoading) {
                    val teacherSubjects = subjects.filter { it.teacherId == teacherId && it.active }
                    val subjectIds = teacherSubjects.map { it.id }
                    val teacherEnrollments = enrollments.filter { it.subjectId in subjectIds }
                    val totalStudents = teacherEnrollments.distinctBy { it.studentId }.size
                    
                    _uiState.value = _uiState.value.copy(
                        activeSubjects = teacherSubjects.size,
                        totalStudents = totalStudents,
                        isLoading = false
                    )
                    
                    println("DEBUG: TeacherDashboardViewModel - Real-time update: ${teacherSubjects.size} subjects, ${totalStudents} students")
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
                        
                        // Load subjects
                        val subjectsResult = subjectRepository.getAllSubjects()
                        subjectsResult.onSuccess { subjectsList ->
                            _subjects.value = subjectsList
                            println("DEBUG: TeacherDashboardViewModel - Loaded ${subjectsList.size} subjects")
                        }.onFailure { exception ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to load subjects"
                            )
                        }
                        
                        // Load enrollments
                        val enrollmentsResult = enrollmentRepository.getAllEnrollments()
                        enrollmentsResult.onSuccess { enrollmentsList ->
                            _enrollments.value = enrollmentsList
                            println("DEBUG: TeacherDashboardViewModel - Loaded ${enrollmentsList.size} enrollments")
                        }.onFailure { exception ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to load enrollments"
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
