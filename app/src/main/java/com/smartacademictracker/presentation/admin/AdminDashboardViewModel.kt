package com.smartacademictracker.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.repository.UserRepository
import com.smartacademictracker.data.repository.SubjectRepository
import com.smartacademictracker.data.repository.EnrollmentRepository
import com.smartacademictracker.data.repository.StudentEnrollmentRepository
import com.smartacademictracker.data.repository.SubjectApplicationRepository
import com.smartacademictracker.data.repository.TeacherApplicationRepository
import com.smartacademictracker.data.repository.GradeRepository
import com.smartacademictracker.data.service.AcademicPeriodFilterService
import com.smartacademictracker.data.manager.AdminDataCache
import com.smartacademictracker.data.model.EnrollmentStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.Job
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
    private val subjectApplicationRepository: SubjectApplicationRepository,
    private val teacherApplicationRepository: TeacherApplicationRepository,
    private val gradeRepository: GradeRepository,
    private val academicPeriodFilterService: AcademicPeriodFilterService,
    private val adminDataCache: AdminDataCache
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    private val _subjects = MutableStateFlow<List<com.smartacademictracker.data.model.Subject>>(emptyList())
    private val _enrollments = MutableStateFlow<List<com.smartacademictracker.data.model.StudentEnrollment>>(emptyList())
    private val _studentApplications = MutableStateFlow<List<com.smartacademictracker.data.model.SubjectApplication>>(emptyList())
    private val _teacherApplications = MutableStateFlow<List<com.smartacademictracker.data.model.TeacherApplication>>(emptyList())
    private val _users = MutableStateFlow<List<com.smartacademictracker.data.model.User>>(emptyList())
    private val _gradeEditRequests = MutableStateFlow<List<com.smartacademictracker.data.model.Grade>>(emptyList())
    
    private var subjectsFlowJob: Job? = null
    private var usersFlowJob: Job? = null
    private var teacherApplicationsFlowJob: Job? = null

    init {
        // Load cached data immediately if available
        loadCachedData()
        
        // Set up real-time listeners
        setupRealtimeListeners()
        
        // Set up real-time data flow for admin dashboard
        viewModelScope.launch {
            combine(
                _subjects,
                _enrollments,
                _studentApplications,
                _teacherApplications,
                _users
            ) { subjects, enrollments, studentApplications, teacherApplications, users ->
                DataTuple(subjects, enrollments, studentApplications, teacherApplications, users)
            }.flatMapLatest { dataTuple ->
                combine(_gradeEditRequests) { gradeEditRequestsArray ->
                    val gradeEditRequests = gradeEditRequestsArray[0]
                    
                    val totalSubjects = dataTuple.subjects.size
                    val activeSubjects = dataTuple.subjects.count { it.active }
                    val totalStudents = dataTuple.users.count { it.role == "STUDENT" }
                    val totalTeachers = dataTuple.users.count { it.role == "TEACHER" }
                    val totalEnrollments = dataTuple.enrollments.count { it.status == EnrollmentStatus.ACTIVE }
                    val pendingStudentApplications = dataTuple.studentApplications.count { it.status == com.smartacademictracker.data.model.ApplicationStatus.PENDING }
                    val pendingTeacherApplications = dataTuple.teacherApplications.count { it.status == com.smartacademictracker.data.model.ApplicationStatus.PENDING }
                    val pendingApplications = pendingStudentApplications + pendingTeacherApplications
                    val pendingGradeEditRequests = gradeEditRequests.size
                    
                    _uiState.value = _uiState.value.copy(
                        totalSubjects = totalSubjects,
                        activeSubjects = activeSubjects,
                        totalStudents = totalStudents,
                        totalTeachers = totalTeachers,
                        totalEnrollments = totalEnrollments,
                        pendingApplications = pendingApplications,
                        pendingTeacherApplications = pendingTeacherApplications,
                        pendingGradeEditRequests = pendingGradeEditRequests
                    )
                }
            }.collect { }
        }
    }
    
    private fun setupRealtimeListeners() {
        // Set up real-time listener for subjects
        subjectsFlowJob = viewModelScope.launch {
            subjectRepository.getAllSubjectsFlow()
                .catch { exception ->
                    // Fallback to one-time query on error
                    val result = subjectRepository.getAllSubjects()
                    result.onSuccess { subjects ->
                        _subjects.value = subjects
                        adminDataCache.updateSubjects(subjects)
                    }
                }
                .collect { subjects ->
                    _subjects.value = subjects
                    adminDataCache.updateSubjects(subjects)
                }
        }
        
        // Set up real-time listener for users
        usersFlowJob = viewModelScope.launch {
            userRepository.getAllUsersFlow()
                .catch { exception ->
                    // Fallback to one-time query on error
                    val result = userRepository.getAllUsers()
                    result.onSuccess { users ->
                        _users.value = users
                        adminDataCache.updateUsers(users)
                    }
                }
                .collect { users ->
                    _users.value = users
                    adminDataCache.updateUsers(users)
                }
        }
        
        // Set up real-time listener for teacher applications
        teacherApplicationsFlowJob = viewModelScope.launch {
            teacherApplicationRepository.getAllApplicationsFlow()
                .catch { exception ->
                    // Fallback to one-time query on error
                    val result = teacherApplicationRepository.getAllApplications()
                    result.onSuccess { applications ->
                        _teacherApplications.value = applications
                        adminDataCache.updateTeacherApplications(applications)
                    }
                }
                .collect { applications ->
                    _teacherApplications.value = applications
                    adminDataCache.updateTeacherApplications(applications)
                }
        }
    }
    
    private fun loadCachedData() {
        // Load cached data immediately if available
        val cachedSubjects = adminDataCache.cachedSubjects.value
        val cachedEnrollments = adminDataCache.cachedEnrollments.value
        val cachedStudentApplications = adminDataCache.cachedStudentApplications.value
        val cachedTeacherApplications = adminDataCache.cachedTeacherApplications.value
        val cachedUsers = adminDataCache.cachedUsers.value
        val cachedGradeEditRequests = adminDataCache.cachedGradeEditRequests.value
        
        if (adminDataCache.hasCachedData() && adminDataCache.isCacheValid()) {
            _subjects.value = cachedSubjects
            _enrollments.value = cachedEnrollments
            _studentApplications.value = cachedStudentApplications
            _teacherApplications.value = cachedTeacherApplications
            _users.value = cachedUsers
            _gradeEditRequests.value = cachedGradeEditRequests
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun loadDashboardData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            // Load cached data first if available and not forcing refresh
            if (!forceRefresh && adminDataCache.hasCachedData() && adminDataCache.isCacheValid()) {
                loadCachedData()
            } else {
                // Only show loading if we don't have cached data
                if (!adminDataCache.hasCachedData()) {
                    _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                }
            }
            
            // Always refresh data in background, but don't block UI if cache exists
            
            try {
                val academicContext = academicPeriodFilterService.getAcademicPeriodContext()
                
                // Load all data in parallel with individual timeouts
                coroutineScope {
                    val subjectsDeferred = async<Result<List<com.smartacademictracker.data.model.Subject>>> {
                        try {
                            withTimeout(15000) {
                                subjectRepository.getAllSubjects()
                            }
                        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                            Result.failure(Exception("Timed out loading subjects"))
                        } catch (e: Exception) {
                            Result.failure(e)
                        }
                    }
                    
                    val enrollmentsDeferred = async<Result<List<com.smartacademictracker.data.model.StudentEnrollment>>> {
                        try {
                            withTimeout(15000) {
                                studentEnrollmentRepository.getAllActiveEnrollments()
                            }
                        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                            Result.failure(Exception("Timed out loading enrollments"))
                        } catch (e: Exception) {
                            Result.failure(e)
                        }
                    }
                    
                    val studentApplicationsDeferred = async<Result<List<com.smartacademictracker.data.model.SubjectApplication>>> {
                        try {
                            withTimeout(15000) {
                                subjectApplicationRepository.getAllApplications()
                            }
                        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                            Result.failure(Exception("Timed out loading student applications"))
                        } catch (e: Exception) {
                            Result.failure(e)
                        }
                    }
                    
                    val teacherApplicationsDeferred = async<Result<List<com.smartacademictracker.data.model.TeacherApplication>>> {
                        try {
                            withTimeout(15000) {
                                teacherApplicationRepository.getAllApplications()
                            }
                        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                            Result.failure(Exception("Timed out loading teacher applications"))
                        } catch (e: Exception) {
                            Result.failure(e)
                        }
                    }
                    
                    val usersDeferred = async<Result<List<com.smartacademictracker.data.model.User>>> {
                        try {
                            withTimeout(15000) {
                                userRepository.getAllUsers()
                            }
                        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                            Result.failure(Exception("Timed out loading users"))
                        } catch (e: Exception) {
                            Result.failure(e)
                        }
                    }
                    
                    val gradeEditRequestsDeferred = async<Result<List<com.smartacademictracker.data.model.Grade>>> {
                        try {
                            withTimeout(15000) {
                                gradeRepository.getGradesWithEditRequests()
                            }
                        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                            Result.failure(Exception("Timed out loading grade edit requests"))
                        } catch (e: Exception) {
                            Result.failure(e)
                        }
                    }
                    
                    // Wait for all operations to complete
                    val subjectsResult = subjectsDeferred.await()
                    val enrollmentsResult = enrollmentsDeferred.await()
                    val studentApplicationsResult = studentApplicationsDeferred.await()
                    val teacherApplicationsResult = teacherApplicationsDeferred.await()
                    val usersResult = usersDeferred.await()
                    val gradeEditRequestsResult = gradeEditRequestsDeferred.await()
                    
                    // Process results and update state and cache
                    var hasData = false
                    
                    subjectsResult.onSuccess { subjectsList ->
                        _subjects.value = subjectsList
                        adminDataCache.updateSubjects(subjectsList)
                        hasData = true
                    }
                    
                    enrollmentsResult.onSuccess { enrollmentsList ->
                        _enrollments.value = enrollmentsList
                        adminDataCache.updateEnrollments(enrollmentsList)
                        hasData = true
                    }
                    
                    studentApplicationsResult.onSuccess { applicationsList ->
                        _studentApplications.value = applicationsList
                        adminDataCache.updateStudentApplications(applicationsList)
                        hasData = true
                    }
                    
                    teacherApplicationsResult.onSuccess { applicationsList ->
                        _teacherApplications.value = applicationsList
                        adminDataCache.updateTeacherApplications(applicationsList)
                        hasData = true
                    }
                    
                    usersResult.onSuccess { usersList ->
                        _users.value = usersList
                        adminDataCache.updateUsers(usersList)
                        hasData = true
                    }
                    
                    gradeEditRequestsResult.onSuccess { requestsList ->
                        _gradeEditRequests.value = requestsList
                        adminDataCache.updateGradeEditRequests(requestsList)
                        hasData = true
                    }
                    
                    // Update cache timestamp
                    if (hasData) {
                        adminDataCache.updateAll(
                            _subjects.value,
                            _enrollments.value,
                            _studentApplications.value,
                            _teacherApplications.value,
                            _users.value,
                            _gradeEditRequests.value
                        )
                    }
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    activeAcademicPeriod = academicContext.periodId,
                    currentSemester = academicContext.semester,
                    currentAcademicYear = academicContext.academicYear
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load dashboard data"
                )
            }
        }
    }

    fun refreshData() {
        loadDashboardData(forceRefresh = true)
    }
    
    fun updateSubjects(subjects: List<com.smartacademictracker.data.model.Subject>) {
        _subjects.value = subjects
        adminDataCache.updateSubjects(subjects)
    }
    
    fun updateEnrollments(enrollments: List<com.smartacademictracker.data.model.StudentEnrollment>) {
        _enrollments.value = enrollments
        adminDataCache.updateEnrollments(enrollments)
    }
    
    fun updateStudentApplications(applications: List<com.smartacademictracker.data.model.SubjectApplication>) {
        _studentApplications.value = applications
        adminDataCache.updateStudentApplications(applications)
    }
    
    fun updateTeacherApplications(applications: List<com.smartacademictracker.data.model.TeacherApplication>) {
        _teacherApplications.value = applications
        adminDataCache.updateTeacherApplications(applications)
    }
    
    fun updateUsers(users: List<com.smartacademictracker.data.model.User>) {
        _users.value = users
        adminDataCache.updateUsers(users)
    }

    // Helper data class for combining flows
    private data class DataTuple(
        val subjects: List<com.smartacademictracker.data.model.Subject>,
        val enrollments: List<com.smartacademictracker.data.model.StudentEnrollment>,
        val studentApplications: List<com.smartacademictracker.data.model.SubjectApplication>,
        val teacherApplications: List<com.smartacademictracker.data.model.TeacherApplication>,
        val users: List<com.smartacademictracker.data.model.User>
    )

    fun cleanupDuplicateApplications() {
        viewModelScope.launch {
            try {
                val result = teacherApplicationRepository.removeDuplicateApplications()
                result.onSuccess {
                    loadDashboardData(forceRefresh = true)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error cleaning up duplicates: ${e.message}"
                )
            }
        }
    }
}

data class AdminDashboardUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalSubjects: Int = 0,
    val activeSubjects: Int = 0,
    val totalStudents: Int = 0,
    val totalTeachers: Int = 0,
    val totalEnrollments: Int = 0,
    val pendingApplications: Int = 0,
    val pendingTeacherApplications: Int = 0,
    val pendingGradeEditRequests: Int = 0,
    val activeAcademicPeriod: String = "",
    val currentSemester: String = "",
    val currentAcademicYear: String = ""
)
