package com.smartacademictracker.presentation.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.manager.TeacherDataCache
import com.smartacademictracker.data.repository.UserRepository
import com.smartacademictracker.data.repository.SubjectRepository
import com.smartacademictracker.data.repository.EnrollmentRepository
import com.smartacademictracker.data.repository.StudentEnrollmentRepository
import com.smartacademictracker.data.repository.SectionAssignmentRepository
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
    private val studentEnrollmentRepository: StudentEnrollmentRepository,
    private val sectionAssignmentRepository: SectionAssignmentRepository,
    private val teacherDataCache: TeacherDataCache
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherDashboardUiState())
    val uiState: StateFlow<TeacherDashboardUiState> = _uiState.asStateFlow()

    private val _teacherId = MutableStateFlow<String?>(null)
    private val _sectionAssignments = MutableStateFlow<List<com.smartacademictracker.data.model.SectionAssignment>>(emptyList())
    private val _enrollments = MutableStateFlow<List<com.smartacademictracker.data.model.Enrollment>>(emptyList())

    init {
        // Set up real-time data flow
        viewModelScope.launch {
            combine(_teacherId, _sectionAssignments, _enrollments) { teacherId, assignments, enrollments ->
                if (teacherId != null) {
                    // Count unique subjects from section assignments (not from subject.teacherId)
                    val assignedSubjectIds = assignments.map { it.subjectId }.toSet()
                    val activeSubjectsCount = assignedSubjectIds.size
                    
                    // Get enrollments for assigned subjects
                    val teacherEnrollments = enrollments.filter { it.subjectId in assignedSubjectIds }
                    val totalStudents = teacherEnrollments.distinctBy { it.studentId }.size
                    
                    _uiState.value = _uiState.value.copy(
                        activeSubjects = activeSubjectsCount,
                        totalStudents = totalStudents,
                        isLoading = false
                    )
                }
            }.collect { }
        }
    }

    fun loadDashboardData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            // Check cache first
            val cachedEnrollments = teacherDataCache.cachedEnrollments.value
            
            // Only show loading if no cached data or cache is invalid
            if (forceRefresh || !teacherDataCache.isCacheValid() || cachedEnrollments.isEmpty()) {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }
            
            try {
                // Get current user (teacher)
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        _teacherId.value = user.id
                        
                        // Use cached data immediately if available and valid
                        if (!forceRefresh && teacherDataCache.isCacheValid() && cachedEnrollments.isNotEmpty()) {
                            val legacyMapped = cachedEnrollments.map { e ->
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
                            _uiState.value = _uiState.value.copy(isLoading = false)
                        }
                        
                        // Load fresh data in background
                        // Load section assignments to get teacher's assigned subjects
                        val assignmentsResult = sectionAssignmentRepository.getSectionAssignmentsByTeacher(user.id)
                        val studentEnrollmentsResult = studentEnrollmentRepository.getActiveEnrollmentsByTeacher(user.id)
                        
                        assignmentsResult.onSuccess { assignments ->
                            _sectionAssignments.value = assignments
                        }.onFailure { exception ->
                            if (!teacherDataCache.isCacheValid()) {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = exception.message ?: "Failed to load section assignments"
                                )
                            }
                        }
                        
                        if (studentEnrollmentsResult.isSuccess) {
                            val se = studentEnrollmentsResult.getOrNull().orEmpty()
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
                            teacherDataCache.updateEnrollments(se)
                        } else {
                            if (!teacherDataCache.isCacheValid()) {
                                _enrollments.value = emptyList()
                            }
                        }
                        
                        if (!teacherDataCache.isCacheValid()) {
                            _uiState.value = _uiState.value.copy(isLoading = false)
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
    
    fun updateEnrollments(enrollments: List<com.smartacademictracker.data.model.Enrollment>) {
        _enrollments.value = enrollments
    }
    
    fun updateSectionAssignments(assignments: List<com.smartacademictracker.data.model.SectionAssignment>) {
        _sectionAssignments.value = assignments
    }
}

data class TeacherDashboardUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val activeSubjects: Int = 0,
    val totalStudents: Int = 0
)
