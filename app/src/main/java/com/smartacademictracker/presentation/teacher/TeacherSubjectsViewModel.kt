package com.smartacademictracker.presentation.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.Subject
import com.smartacademictracker.data.model.TeacherApplication
import com.smartacademictracker.data.model.ApplicationStatus
import com.smartacademictracker.data.repository.SubjectRepository
import com.smartacademictracker.data.repository.TeacherApplicationRepository
import com.smartacademictracker.data.repository.UserRepository
import com.smartacademictracker.data.repository.EnrollmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeacherSubjectsViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val teacherApplicationRepository: TeacherApplicationRepository,
    private val userRepository: UserRepository,
    private val enrollmentRepository: EnrollmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherSubjectsUiState())
    val uiState: StateFlow<TeacherSubjectsUiState> = _uiState.asStateFlow()

    private val _availableSubjects = MutableStateFlow<List<Subject>>(emptyList())
    val availableSubjects: StateFlow<List<Subject>> = _availableSubjects.asStateFlow()

    private val _mySubjects = MutableStateFlow<List<Subject>>(emptyList())
    val mySubjects: StateFlow<List<Subject>> = _mySubjects.asStateFlow()

    private val _appliedSubjects = MutableStateFlow<List<Subject>>(emptyList())
    val appliedSubjects: StateFlow<List<Subject>> = _appliedSubjects.asStateFlow()

    fun loadSubjects() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Get current user
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        // Load available subjects (subjects without teachers)
                        val availableResult = subjectRepository.getAvailableSubjectsForTeacher(user.id)
                        availableResult.onSuccess { subjects ->
                            // Filter out subjects the teacher has already applied for
                            filterAvailableSubjects(user.id, subjects)
                        }

                        // Load teacher's assigned subjects
                        val mySubjectsResult = subjectRepository.getSubjectsByTeacher(user.id)
                        mySubjectsResult.onSuccess { subjects ->
                            _mySubjects.value = subjects
                        }

                        // Load teacher's applied subjects
                        loadAppliedSubjects(user.id)

                        _uiState.value = _uiState.value.copy(isLoading = false)
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
                    error = e.message ?: "Failed to load subjects"
                )
            }
        }
    }

    fun applyForSubject(subjectId: String) {
        viewModelScope.launch {
            println("DEBUG: TeacherSubjectsViewModel - Applying for subject: $subjectId")
            _uiState.value = _uiState.value.copy(
                applyingSubjects = _uiState.value.applyingSubjects + subjectId,
                error = null
            )
            
            try {
                // Get current user
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        println("DEBUG: TeacherSubjectsViewModel - User found: ${user.email}")
                        // Get subject details
                        val subjectResult = subjectRepository.getSubjectById(subjectId)
                        subjectResult.onSuccess { subject ->
                            println("DEBUG: TeacherSubjectsViewModel - Subject found: ${subject.name}")
                            // Create application
                            val application = TeacherApplication(
                                teacherId = user.id,
                                teacherName = "${user.firstName} ${user.lastName}",
                                teacherEmail = user.email,
                                subjectId = subject.id,
                                subjectName = subject.name,
                                subjectCode = subject.code,
                                applicationReason = "I would like to teach this subject based on my expertise and experience.",
                                status = ApplicationStatus.PENDING
                            )

                            println("DEBUG: TeacherSubjectsViewModel - Creating application for: ${application.subjectName}")
                            // Submit application
                            val applicationResult = teacherApplicationRepository.createApplication(application)
                            applicationResult.onSuccess {
                                println("DEBUG: TeacherSubjectsViewModel - Application created successfully!")
                                _uiState.value = _uiState.value.copy(
                                    applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                                    successMessage = "Application submitted successfully!"
                                )
                                // Reload subjects to update the list
                                loadSubjects()
                                // Also reload applied subjects
                                loadAppliedSubjects(user.id)
                            }.onFailure { exception ->
                                println("DEBUG: TeacherSubjectsViewModel - Application creation failed: ${exception.message}")
                                _uiState.value = _uiState.value.copy(
                                    applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                                    error = exception.message ?: "Failed to submit application"
                                )
                            }
                        }.onFailure { exception ->
                            println("DEBUG: TeacherSubjectsViewModel - Subject not found: ${exception.message}")
                            _uiState.value = _uiState.value.copy(
                                applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                                error = exception.message ?: "Subject not found"
                            )
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                            error = "User not found"
                        )
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                        error = exception.message ?: "Failed to get user data"
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

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun refreshSubjects() {
        loadSubjects()
    }
    
    suspend fun getStudentCountForSubject(subjectId: String): Int {
        return try {
            val enrollmentsResult = enrollmentRepository.getEnrollmentsBySubject(subjectId)
            enrollmentsResult.getOrNull()?.size ?: 0
        } catch (e: Exception) {
            println("DEBUG: TeacherSubjectsViewModel - Error getting student count for subject $subjectId: ${e.message}")
            0
        }
    }

    private suspend fun filterAvailableSubjects(teacherId: String, subjects: List<Subject>) {
        try {
            // Get teacher's applications
            val applicationsResult = teacherApplicationRepository.getApplicationsByTeacher(teacherId)
            applicationsResult.onSuccess { applications ->
                // Get subject IDs that teacher has already applied for
                val appliedSubjectIds = applications
                    .filter { it.status == ApplicationStatus.PENDING }
                    .map { it.subjectId }
                    .toSet()
                
                // Filter out subjects the teacher has already applied for AND subjects that are already assigned to teachers
                val availableSubjects = subjects.filter { subject ->
                    subject.id !in appliedSubjectIds && subject.teacherId == null
                }
                _availableSubjects.value = availableSubjects
                
                println("DEBUG: TeacherSubjectsViewModel - Available subjects after filtering: ${availableSubjects.size}, Applied for: ${appliedSubjectIds.size}, Total subjects: ${subjects.size}")
            }.onFailure { exception ->
                println("DEBUG: TeacherSubjectsViewModel - Failed to load applications: ${exception.message}")
                // If we can't load applications, show only unassigned subjects
                val availableSubjects = subjects.filter { it.teacherId == null }
                _availableSubjects.value = availableSubjects
            }
        } catch (e: Exception) {
            println("DEBUG: TeacherSubjectsViewModel - Exception filtering subjects: ${e.message}")
            // If there's an exception, show only unassigned subjects
            val availableSubjects = subjects.filter { it.teacherId == null }
            _availableSubjects.value = availableSubjects
        }
    }

    private suspend fun loadAppliedSubjects(teacherId: String) {
        try {
            // Get teacher's applications
            val applicationsResult = teacherApplicationRepository.getApplicationsByTeacher(teacherId)
            applicationsResult.onSuccess { applications ->
                // Get subject IDs that teacher has applied for
                val appliedSubjectIds = applications
                    .filter { it.status == ApplicationStatus.PENDING }
                    .map { it.subjectId }
                    .toSet()
                
                // Load subject details for applied subjects
                val appliedSubjectsList = mutableListOf<Subject>()
                for (subjectId in appliedSubjectIds) {
                    val subjectResult = subjectRepository.getSubjectById(subjectId)
                    subjectResult.onSuccess { subject ->
                        appliedSubjectsList.add(subject)
                    }
                }
                _appliedSubjects.value = appliedSubjectsList
                
                println("DEBUG: TeacherSubjectsViewModel - Applied subjects loaded: ${appliedSubjectsList.size}")
            }.onFailure { exception ->
                println("DEBUG: TeacherSubjectsViewModel - Failed to load applied subjects: ${exception.message}")
                _appliedSubjects.value = emptyList()
            }
        } catch (e: Exception) {
            println("DEBUG: TeacherSubjectsViewModel - Exception loading applied subjects: ${e.message}")
            _appliedSubjects.value = emptyList()
        }
    }
}

data class TeacherSubjectsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val applyingSubjects: Set<String> = emptySet()
)
