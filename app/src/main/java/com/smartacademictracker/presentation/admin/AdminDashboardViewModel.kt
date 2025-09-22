package com.smartacademictracker.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.UserRole
import com.smartacademictracker.data.repository.UserRepository
import com.smartacademictracker.data.repository.SubjectRepository
import com.smartacademictracker.data.repository.TeacherApplicationRepository
import com.smartacademictracker.data.repository.EnrollmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val subjectRepository: SubjectRepository,
    private val teacherApplicationRepository: TeacherApplicationRepository,
    private val enrollmentRepository: EnrollmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Load all statistics in parallel
                val studentsResult = userRepository.getUsersByRole(UserRole.STUDENT)
                val teachersResult = userRepository.getUsersByRole(UserRole.TEACHER)
                val subjectsResult = subjectRepository.getAllSubjects()
                val applicationsResult = teacherApplicationRepository.getApplicationsByStatus(
                    com.smartacademictracker.data.model.ApplicationStatus.PENDING
                )
                val enrollmentsResult = enrollmentRepository.getAllEnrollments()

                // Process results
                val students = studentsResult.getOrNull()?.size ?: 0
                val teachers = teachersResult.getOrNull()?.size ?: 0
                val activeSubjects = subjectsResult.getOrNull()?.filter { it.active }?.size ?: 0
                val pendingApplications = applicationsResult.getOrNull()?.size ?: 0
                val totalEnrollments = enrollmentsResult.getOrNull()?.size ?: 0

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    totalStudents = students,
                    totalTeachers = teachers,
                    activeSubjects = activeSubjects,
                    pendingApplications = pendingApplications,
                    totalEnrollments = totalEnrollments
                )

                println("DEBUG: AdminDashboardViewModel - Dashboard data loaded: Students=$students, Teachers=$teachers, Subjects=$activeSubjects, Applications=$pendingApplications")
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
}

data class AdminDashboardUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalStudents: Int = 0,
    val totalTeachers: Int = 0,
    val activeSubjects: Int = 0,
    val pendingApplications: Int = 0,
    val totalEnrollments: Int = 0
)
