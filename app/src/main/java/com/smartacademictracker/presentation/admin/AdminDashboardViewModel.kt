package com.smartacademictracker.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.repository.UserRepository
import com.smartacademictracker.data.repository.SubjectRepository
import com.smartacademictracker.data.repository.EnrollmentRepository
import com.smartacademictracker.data.repository.StudentApplicationRepository
import com.smartacademictracker.data.repository.GradeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val subjectRepository: SubjectRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val studentApplicationRepository: StudentApplicationRepository,
    private val gradeRepository: GradeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    private val _subjects = MutableStateFlow<List<com.smartacademictracker.data.model.Subject>>(emptyList())
    private val _enrollments = MutableStateFlow<List<com.smartacademictracker.data.model.Enrollment>>(emptyList())
    private val _applications = MutableStateFlow<List<com.smartacademictracker.data.model.StudentApplication>>(emptyList())
    private val _users = MutableStateFlow<List<com.smartacademictracker.data.model.User>>(emptyList())

    init {
        // Set up real-time data flow for admin dashboard
        viewModelScope.launch {
            combine(_subjects, _enrollments, _applications, _users) { subjects, enrollments, applications, users ->
                val totalSubjects = subjects.size
                val activeSubjects = subjects.count { it.active }
                val totalStudents = users.count { it.role == "STUDENT" }
                val totalTeachers = users.count { it.role == "TEACHER" }
                val totalEnrollments = enrollments.count { it.active }
                val pendingApplications = applications.count { it.status == com.smartacademictracker.data.model.StudentApplicationStatus.PENDING }
                
                _uiState.value = _uiState.value.copy(
                    totalSubjects = totalSubjects,
                    activeSubjects = activeSubjects,
                    totalStudents = totalStudents,
                    totalTeachers = totalTeachers,
                    totalEnrollments = totalEnrollments,
                    pendingApplications = pendingApplications
                    // Don't override isLoading here - let loadDashboardData() handle it
                )
                
                println("DEBUG: AdminDashboardViewModel - Real-time update: $totalSubjects subjects, $totalStudents students, $pendingApplications pending applications")
            }.collect { }
        }
        
        // Backup timeout to ensure loading state is always resolved
        viewModelScope.launch {
            kotlinx.coroutines.delay(15000) // 15 second backup timeout
            if (_uiState.value.isLoading) {
                println("DEBUG: AdminDashboardViewModel - Backup timeout triggered, setting loading to false")
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            println("DEBUG: AdminDashboardViewModel - Starting loadDashboardData()")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Add timeout to prevent infinite loading
                withTimeout(10000) { // 10 second timeout
                    // Load all data in parallel
                    println("DEBUG: AdminDashboardViewModel - Loading subjects...")
                    val subjectsResult = subjectRepository.getAllSubjects()
                    println("DEBUG: AdminDashboardViewModel - Loading enrollments...")
                    val enrollmentsResult = enrollmentRepository.getAllEnrollments()
                    println("DEBUG: AdminDashboardViewModel - Loading applications...")
                    val applicationsResult = studentApplicationRepository.getAllApplications()
                    println("DEBUG: AdminDashboardViewModel - Loading users...")
                    val usersResult = userRepository.getAllUsers()
                    
                    // Process results and update state
                    subjectsResult.onSuccess { subjectsList ->
                        _subjects.value = subjectsList
                        println("DEBUG: AdminDashboardViewModel - Loaded ${subjectsList.size} subjects")
                    }.onFailure { exception ->
                        println("DEBUG: AdminDashboardViewModel - Error loading subjects: ${exception.message}")
                    }
                    
                    enrollmentsResult.onSuccess { enrollmentsList ->
                        _enrollments.value = enrollmentsList
                        println("DEBUG: AdminDashboardViewModel - Loaded ${enrollmentsList.size} enrollments")
                    }.onFailure { exception ->
                        println("DEBUG: AdminDashboardViewModel - Error loading enrollments: ${exception.message}")
                    }
                    
                    applicationsResult.onSuccess { applicationsList ->
                        _applications.value = applicationsList
                        println("DEBUG: AdminDashboardViewModel - Loaded ${applicationsList.size} applications")
                    }.onFailure { exception ->
                        println("DEBUG: AdminDashboardViewModel - Error loading applications: ${exception.message}")
                    }
                    
                    usersResult.onSuccess { usersList ->
                        _users.value = usersList
                        println("DEBUG: AdminDashboardViewModel - Loaded ${usersList.size} users")
                    }.onFailure { exception ->
                        println("DEBUG: AdminDashboardViewModel - Error loading users: ${exception.message}")
                    }
                }
                
                // Set loading to false after all data loading attempts complete
                _uiState.value = _uiState.value.copy(isLoading = false)
                println("DEBUG: AdminDashboardViewModel - All data loading completed, loading set to false")
                
            } catch (e: Exception) {
                println("DEBUG: AdminDashboardViewModel - Error loading dashboard data: ${e.message}")
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
    
    fun updateSubjects(subjects: List<com.smartacademictracker.data.model.Subject>) {
        _subjects.value = subjects
        println("DEBUG: AdminDashboardViewModel - Updated subjects: ${subjects.size} total")
    }
    
    fun updateEnrollments(enrollments: List<com.smartacademictracker.data.model.Enrollment>) {
        _enrollments.value = enrollments
        println("DEBUG: AdminDashboardViewModel - Updated enrollments: ${enrollments.size} total")
    }
    
    fun updateApplications(applications: List<com.smartacademictracker.data.model.StudentApplication>) {
        _applications.value = applications
        println("DEBUG: AdminDashboardViewModel - Updated applications: ${applications.size} total")
    }
    
    fun updateUsers(users: List<com.smartacademictracker.data.model.User>) {
        _users.value = users
        println("DEBUG: AdminDashboardViewModel - Updated users: ${users.size} total")
    }
}

data class AdminDashboardUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val totalSubjects: Int = 0,
    val activeSubjects: Int = 0,
    val totalStudents: Int = 0,
    val totalTeachers: Int = 0,
    val totalEnrollments: Int = 0,
    val pendingApplications: Int = 0
)