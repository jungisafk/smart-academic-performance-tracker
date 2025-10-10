package com.smartacademictracker.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.repository.UserRepository
import com.smartacademictracker.data.repository.SubjectRepository
import com.smartacademictracker.data.repository.EnrollmentRepository
import com.smartacademictracker.data.repository.StudentEnrollmentRepository
import com.smartacademictracker.data.repository.StudentApplicationRepository
import com.smartacademictracker.data.repository.TeacherApplicationRepository
import com.smartacademictracker.data.repository.GradeRepository
import com.smartacademictracker.data.service.AcademicPeriodFilterService
import com.smartacademictracker.data.model.EnrollmentStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val subjectRepository: SubjectRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val studentEnrollmentRepository: StudentEnrollmentRepository,
    private val studentApplicationRepository: StudentApplicationRepository,
    private val teacherApplicationRepository: TeacherApplicationRepository,
    private val gradeRepository: GradeRepository,
    private val academicPeriodFilterService: AcademicPeriodFilterService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    private val _subjects = MutableStateFlow<List<com.smartacademictracker.data.model.Subject>>(emptyList())
    private val _enrollments = MutableStateFlow<List<com.smartacademictracker.data.model.StudentEnrollment>>(emptyList())
    private val _studentApplications = MutableStateFlow<List<com.smartacademictracker.data.model.StudentApplication>>(emptyList())
    private val _teacherApplications = MutableStateFlow<List<com.smartacademictracker.data.model.TeacherApplication>>(emptyList())
    private val _users = MutableStateFlow<List<com.smartacademictracker.data.model.User>>(emptyList())

    init {
        // Set up real-time data flow for admin dashboard
        viewModelScope.launch {
            combine(_subjects, _enrollments, _studentApplications, _teacherApplications, _users) { subjects, enrollments, studentApplications, teacherApplications, users ->
                val totalSubjects = subjects.size
                val activeSubjects = subjects.count { it.active }
                val totalStudents = users.count { it.role == "STUDENT" }
                val totalTeachers = users.count { it.role == "TEACHER" }
                val totalEnrollments = enrollments.count { it.status == EnrollmentStatus.ACTIVE }
                val pendingStudentApplications = studentApplications.count { it.status == com.smartacademictracker.data.model.StudentApplicationStatus.PENDING }
                val pendingTeacherApplications = teacherApplications.count { it.status == com.smartacademictracker.data.model.ApplicationStatus.PENDING }
                val pendingApplications = pendingStudentApplications + pendingTeacherApplications
                
                _uiState.value = _uiState.value.copy(
                    totalSubjects = totalSubjects,
                    activeSubjects = activeSubjects,
                    totalStudents = totalStudents,
                    totalTeachers = totalTeachers,
                    totalEnrollments = totalEnrollments,
                    pendingApplications = pendingApplications,
                    pendingTeacherApplications = pendingTeacherApplications
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
                // Load academic period information first
                println("DEBUG: AdminDashboardViewModel - Loading academic period...")
                val academicContext = academicPeriodFilterService.getAcademicPeriodContext()
                
                // Load all data in parallel with individual timeouts
                // Each operation has its own timeout so one failure doesn't block others
                coroutineScope {
                    val subjectsDeferred = async<Result<List<com.smartacademictracker.data.model.Subject>>> {
                        try {
                            withTimeout(15000) { // 15 second timeout per operation
                                println("DEBUG: AdminDashboardViewModel - Loading subjects...")
                                subjectRepository.getAllSubjects()
                            }
                        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                            println("DEBUG: AdminDashboardViewModel - Timeout loading subjects")
                            Result.failure(Exception("Timed out loading subjects"))
                        } catch (e: Exception) {
                            println("DEBUG: AdminDashboardViewModel - Error loading subjects: ${e.message}")
                            Result.failure(e)
                        }
                    }
                    
                    val enrollmentsDeferred = async<Result<List<com.smartacademictracker.data.model.StudentEnrollment>>> {
                        try {
                            withTimeout(15000) {
                                println("DEBUG: AdminDashboardViewModel - Loading student enrollments...")
                                studentEnrollmentRepository.getAllActiveEnrollments()
                            }
                        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                            println("DEBUG: AdminDashboardViewModel - Timeout loading enrollments")
                            Result.failure(Exception("Timed out loading enrollments"))
                        } catch (e: Exception) {
                            println("DEBUG: AdminDashboardViewModel - Error loading enrollments: ${e.message}")
                            Result.failure(e)
                        }
                    }
                    
                    val studentApplicationsDeferred = async<Result<List<com.smartacademictracker.data.model.StudentApplication>>> {
                        try {
                            withTimeout(15000) {
                                println("DEBUG: AdminDashboardViewModel - Loading student applications...")
                                studentApplicationRepository.getAllApplications()
                            }
                        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                            println("DEBUG: AdminDashboardViewModel - Timeout loading student applications")
                            Result.failure(Exception("Timed out loading student applications"))
                        } catch (e: Exception) {
                            println("DEBUG: AdminDashboardViewModel - Error loading student applications: ${e.message}")
                            Result.failure(e)
                        }
                    }
                    
                    val teacherApplicationsDeferred = async<Result<List<com.smartacademictracker.data.model.TeacherApplication>>> {
                        try {
                            withTimeout(15000) {
                                println("DEBUG: AdminDashboardViewModel - Loading teacher applications...")
                                teacherApplicationRepository.getAllApplications()
                            }
                        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                            println("DEBUG: AdminDashboardViewModel - Timeout loading teacher applications")
                            Result.failure(Exception("Timed out loading teacher applications"))
                        } catch (e: Exception) {
                            println("DEBUG: AdminDashboardViewModel - Error loading teacher applications: ${e.message}")
                            Result.failure(e)
                        }
                    }
                    
                    val usersDeferred = async<Result<List<com.smartacademictracker.data.model.User>>> {
                        try {
                            withTimeout(15000) {
                                println("DEBUG: AdminDashboardViewModel - Loading users...")
                                userRepository.getAllUsers()
                            }
                        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                            println("DEBUG: AdminDashboardViewModel - Timeout loading users")
                            Result.failure(Exception("Timed out loading users"))
                        } catch (e: Exception) {
                            println("DEBUG: AdminDashboardViewModel - Error loading users: ${e.message}")
                            Result.failure(e)
                        }
                    }
                    
                    // Wait for all operations to complete (or timeout)
                    val subjectsResult = subjectsDeferred.await()
                    val enrollmentsResult = enrollmentsDeferred.await()
                    val studentApplicationsResult = studentApplicationsDeferred.await()
                    val teacherApplicationsResult = teacherApplicationsDeferred.await()
                    val usersResult = usersDeferred.await()
                    
                    // Process results and update state (continue even if some fail)
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
                    
                    studentApplicationsResult.onSuccess { applicationsList ->
                        _studentApplications.value = applicationsList
                        println("DEBUG: AdminDashboardViewModel - Loaded ${applicationsList.size} student applications")
                    }.onFailure { exception ->
                        println("DEBUG: AdminDashboardViewModel - Error loading student applications: ${exception.message}")
                    }
                    
                    teacherApplicationsResult.onSuccess { applicationsList ->
                        _teacherApplications.value = applicationsList
                        println("DEBUG: AdminDashboardViewModel - Loaded ${applicationsList.size} teacher applications")
                    }.onFailure { exception ->
                        println("DEBUG: AdminDashboardViewModel - Error loading teacher applications: ${exception.message}")
                    }
                    
                    usersResult.onSuccess { usersList ->
                        _users.value = usersList
                        println("DEBUG: AdminDashboardViewModel - Loaded ${usersList.size} users")
                    }.onFailure { exception ->
                        println("DEBUG: AdminDashboardViewModel - Error loading users: ${exception.message}")
                    }
                }
                
                // Set loading to false after all data loading attempts complete
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    activeAcademicPeriod = academicContext.periodId,
                    currentSemester = academicContext.semester,
                    currentAcademicYear = academicContext.academicYear
                )
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
    
    fun updateEnrollments(enrollments: List<com.smartacademictracker.data.model.StudentEnrollment>) {
        _enrollments.value = enrollments
        println("DEBUG: AdminDashboardViewModel - Updated enrollments: ${enrollments.size} total")
    }
    
    fun updateStudentApplications(applications: List<com.smartacademictracker.data.model.StudentApplication>) {
        _studentApplications.value = applications
        println("DEBUG: AdminDashboardViewModel - Updated student applications: ${applications.size} total")
    }
    
    fun updateTeacherApplications(applications: List<com.smartacademictracker.data.model.TeacherApplication>) {
        _teacherApplications.value = applications
        println("DEBUG: AdminDashboardViewModel - Updated teacher applications: ${applications.size} total")
    }
    
    fun updateUsers(users: List<com.smartacademictracker.data.model.User>) {
        _users.value = users
        println("DEBUG: AdminDashboardViewModel - Updated users: ${users.size} total")
    }

    fun cleanupDuplicateApplications() {
        viewModelScope.launch {
            try {
                val result = teacherApplicationRepository.removeDuplicateApplications()
                result.onSuccess { deletedCount ->
                    println("DEBUG: AdminDashboardViewModel - Cleaned up $deletedCount duplicate applications")
                    // Reload data to reflect changes
                    loadDashboardData()
                }.onFailure { exception ->
                    println("DEBUG: AdminDashboardViewModel - Error cleaning up duplicates: ${exception.message}")
                }
            } catch (e: Exception) {
                println("DEBUG: AdminDashboardViewModel - Exception during cleanup: ${e.message}")
            }
        }
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
    val pendingApplications: Int = 0,
    val pendingTeacherApplications: Int = 0,
    val activeAcademicPeriod: String = "",
    val currentSemester: String = "",
    val currentAcademicYear: String = ""
)