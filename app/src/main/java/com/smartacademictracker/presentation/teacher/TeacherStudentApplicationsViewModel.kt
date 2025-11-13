package com.smartacademictracker.presentation.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.StudentApplication
import com.smartacademictracker.data.model.StudentApplicationStatus
import com.smartacademictracker.data.repository.StudentApplicationRepository
import com.smartacademictracker.data.repository.SubjectRepository
import com.smartacademictracker.data.repository.UserRepository
import com.smartacademictracker.data.repository.StudentEnrollmentRepository
import com.smartacademictracker.data.model.StudentEnrollment
import com.smartacademictracker.data.model.EnrollmentStatus
import com.smartacademictracker.data.manager.RealtimeDataManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeacherStudentApplicationsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val studentApplicationRepository: StudentApplicationRepository,
    private val subjectApplicationRepository: com.smartacademictracker.data.repository.SubjectApplicationRepository,
    private val subjectRepository: SubjectRepository,
    private val studentEnrollmentRepository: StudentEnrollmentRepository,
    private val sectionAssignmentRepository: com.smartacademictracker.data.repository.SectionAssignmentRepository,
    private val realtimeDataManager: RealtimeDataManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherStudentApplicationsUiState())
    val uiState: StateFlow<TeacherStudentApplicationsUiState> = _uiState.asStateFlow()

    private val _applications = MutableStateFlow<List<StudentApplication>>(emptyList())
    val applications: StateFlow<List<StudentApplication>> = _applications.asStateFlow()

    // Tracks which backing collection each displayed application came from
    private val applicationSourceById = mutableMapOf<String, ApplicationSource>()

    private enum class ApplicationSource { LEGACY_STUDENT, CURRENT_SUBJECT }

    fun loadApplications() {
        viewModelScope.launch {
            println("DEBUG: TeacherStudentApplicationsViewModel - loadApplications called")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Get current user (teacher)
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        println("DEBUG: TeacherStudentApplicationsViewModel - Current teacher: ${user.id} (${user.firstName} ${user.lastName})")
                        // Get subjects that this teacher actually teaches (owns)
                        val subjectsResult = subjectRepository.getSubjectsByTeacher(user.id)
                        subjectsResult.onSuccess { subjects ->
                            val subjectIds = subjects.map { it.id }
                            
                            println("DEBUG: TeacherStudentApplicationsViewModel - Found ${subjects.size} subjects taught by teacher ${user.id}")
                            subjects.forEach { subject ->
                                println("DEBUG: Teacher Subject - ID: ${subject.id}, Name: ${subject.name}, TeacherId: ${subject.teacherId}")
                            }
                            println("DEBUG: TeacherStudentApplicationsViewModel - Subject IDs: $subjectIds")
                            
                            if (subjectIds.isEmpty()) {
                                _applications.value = emptyList()
                                _uiState.value = _uiState.value.copy(isLoading = false)
                                println("DEBUG: TeacherStudentApplicationsViewModel - No subjects found for teacher")
                                return@onSuccess
                            }
                            
                            // Now get applications for these subjects from both legacy and current collections
                            println("DEBUG: TeacherStudentApplicationsViewModel - Getting legacy applications for subjects: $subjectIds")
                            val legacyResult = studentApplicationRepository.getApplicationsForTeacherSubjects(user.id, subjectIds)
                            val legacyList = legacyResult.getOrElse { emptyList<com.smartacademictracker.data.model.StudentApplication>() }
                            println("DEBUG: TeacherStudentApplicationsViewModel - Found ${legacyList.size} legacy applications")
                            
                            println("DEBUG: TeacherStudentApplicationsViewModel - Getting current applications for subjects: $subjectIds")
                            val currentResult = subjectApplicationRepository.getApplicationsForTeacherSubjects(subjectIds)
                            val currentList = currentResult.getOrElse { emptyList<com.smartacademictracker.data.model.SubjectApplication>() }.map { sa ->
                                com.smartacademictracker.data.model.StudentApplication(
                                    id = sa.id,
                                    studentId = sa.studentId,
                                    studentName = sa.studentName,
                                    subjectId = sa.subjectId,
                                    subjectName = sa.subjectName,
                                    subjectCode = "",
                                    status = when (sa.status) {
                                        com.smartacademictracker.data.model.ApplicationStatus.PENDING -> com.smartacademictracker.data.model.StudentApplicationStatus.PENDING
                                        com.smartacademictracker.data.model.ApplicationStatus.APPROVED -> com.smartacademictracker.data.model.StudentApplicationStatus.APPROVED
                                        com.smartacademictracker.data.model.ApplicationStatus.REJECTED -> com.smartacademictracker.data.model.StudentApplicationStatus.REJECTED
                                        com.smartacademictracker.data.model.ApplicationStatus.WITHDRAWN -> com.smartacademictracker.data.model.StudentApplicationStatus.REJECTED
                                    },
                                    appliedAt = sa.appliedDate,
                                    teacherComments = sa.remarks
                                )
                            }
                            // Update source mapping
                            applicationSourceById.clear()
                            legacyList.forEach { applicationSourceById[it.id] = ApplicationSource.LEGACY_STUDENT }
                            currentList.forEach { applicationSourceById[it.id] = ApplicationSource.CURRENT_SUBJECT }

                            val combined = (legacyList + currentList).sortedByDescending { it.appliedAt }
                            _applications.value = combined
                            _uiState.value = _uiState.value.copy(isLoading = false)
                            println("DEBUG: TeacherStudentApplicationsViewModel - Loaded ${combined.size} applications for ${subjectIds.size} subjects")
                            combined.forEach { app ->
                                println("DEBUG: TeacherStudentApplicationsViewModel - Application: ${app.studentName} for ${app.subjectName} - Status: ${app.status}")
                            }
                            
                        }.onFailure { exception ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to load teacher subjects"
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
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load applications"
                )
            }
        }
    }

    fun updateApplicationStatus(
        applicationId: String,
        status: StudentApplicationStatus,
        comments: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                processingApplications = _uiState.value.processingApplications + applicationId,
                error = null
            )
            
            try {
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        val source = applicationSourceById[applicationId] ?: ApplicationSource.LEGACY_STUDENT
                        val updateResult = when (source) {
                            ApplicationSource.LEGACY_STUDENT ->
                                studentApplicationRepository.updateApplicationStatus(
                                    applicationId = applicationId,
                                    status = status,
                                    teacherId = user.id,
                                    comments = comments
                                )
                            ApplicationSource.CURRENT_SUBJECT ->
                                {
                                    val mapped = when (status) {
                                        StudentApplicationStatus.PENDING -> com.smartacademictracker.data.model.ApplicationStatus.PENDING
                                        StudentApplicationStatus.APPROVED -> com.smartacademictracker.data.model.ApplicationStatus.APPROVED
                                        StudentApplicationStatus.REJECTED -> com.smartacademictracker.data.model.ApplicationStatus.REJECTED
                                    }
                                    subjectApplicationRepository.updateApplicationStatus(
                                        applicationId = applicationId,
                                        status = mapped,
                                        processedBy = user.id,
                                        remarks = comments
                                    ).map { Unit }
                                }
                        }
                        
                        updateResult.onSuccess {
                            println("DEBUG: TeacherStudentApplicationsViewModel - Application status updated successfully")
                            // If application is approved, create enrollment
                            if (status == StudentApplicationStatus.APPROVED) {
                                println("DEBUG: TeacherStudentApplicationsViewModel - Creating enrollment for approved application")
                                // Get the application details from the correct collection
                                when (applicationSourceById[applicationId] ?: ApplicationSource.LEGACY_STUDENT) {
                                    ApplicationSource.LEGACY_STUDENT -> {
                                        val applicationResult = studentApplicationRepository.getApplicationById(applicationId)
                                        applicationResult.onSuccess { application ->
                                            if (application != null) {
                                                println("DEBUG: TeacherStudentApplicationsViewModel - Application details (legacy): studentId=${application.studentId}, subjectId=${application.subjectId}")
                                                createStudentEnrollment(application)
                                            }
                                        }.onFailure { appException ->
                                            println("DEBUG: TeacherStudentApplicationsViewModel - Failed to get legacy application details: ${appException.message}")
                                        }
                                    }
                                    ApplicationSource.CURRENT_SUBJECT -> {
                                        val applicationResult = subjectApplicationRepository.getApplicationById(applicationId)
                                        applicationResult.onSuccess { sa ->
                                            println("DEBUG: TeacherStudentApplicationsViewModel - Application details (current): studentId=${sa.studentId}, subjectId=${sa.subjectId}")
                                            createStudentEnrollmentFromSubjectApplication(sa)
                                        }.onFailure { appException ->
                                            println("DEBUG: TeacherStudentApplicationsViewModel - Failed to get subject application details: ${appException.message}")
                                        }
                                    }
                                }
                            }
                            
                            _uiState.value = _uiState.value.copy(
                                processingApplications = _uiState.value.processingApplications - applicationId
                            )
                            // Reload applications to update UI
                            loadApplications()
                            
                            println("DEBUG: TeacherStudentApplicationsViewModel - Application status updated successfully")
                        }.onFailure { exception ->
                            _uiState.value = _uiState.value.copy(
                                processingApplications = _uiState.value.processingApplications - applicationId,
                                error = exception.message ?: "Failed to update application status"
                            )
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            processingApplications = _uiState.value.processingApplications - applicationId,
                            error = "User not found"
                        )
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        processingApplications = _uiState.value.processingApplications - applicationId,
                        error = exception.message ?: "Failed to get user data"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    processingApplications = _uiState.value.processingApplications - applicationId,
                    error = e.message ?: "Failed to update application status"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun refreshApplications() {
        loadApplications()
    }

    fun approveApplication(applicationId: String) {
        updateApplicationStatus(applicationId, StudentApplicationStatus.APPROVED)
    }

    fun rejectApplication(applicationId: String) {
        updateApplicationStatus(applicationId, StudentApplicationStatus.REJECTED)
    }

    private suspend fun createStudentEnrollment(application: StudentApplication) {
        try {
            println("DEBUG: TeacherStudentApplicationsViewModel - Creating enrollment for legacy application: ${application.id}")
            
            // Get student information
            val studentResult = userRepository.getUserById(application.studentId)
            studentResult.onSuccess { student ->
                // Get subject information
                val subjectResult = subjectRepository.getSubjectById(application.subjectId)
                subjectResult.onSuccess { subject ->
                    // Create enrollment
                    val enrollment = StudentEnrollment(
                        studentId = application.studentId,
                        studentName = application.studentName,
                        subjectId = application.subjectId,
                        subjectName = application.subjectName,
                        sectionName = "A", // Default section for legacy applications
                        courseId = application.courseId,
                        courseName = application.courseName,
                        yearLevelId = application.yearLevelId,
                        yearLevelName = application.yearLevelName,
                        academicPeriodId = "", // Will be populated by repository
                        enrollmentDate = System.currentTimeMillis(),
                        status = EnrollmentStatus.ACTIVE
                    )

                    println("DEBUG: TeacherStudentApplicationsViewModel - Calling studentEnrollmentRepository.enrollStudent")
                    val enrollmentResult = studentEnrollmentRepository.enrollStudent(enrollment)
                    enrollmentResult.onSuccess { enrollmentId ->
                        println("DEBUG: TeacherStudentApplicationsViewModel - Successfully created enrollment for student ${application.studentName} with ID: $enrollmentId")
                        realtimeDataManager.loadAllData()
                    }.onFailure { exception ->
                        println("DEBUG: TeacherStudentApplicationsViewModel - Error creating enrollment: ${exception.message}")
                    }
                }.onFailure { exception ->
                    println("DEBUG: TeacherStudentApplicationsViewModel - Error getting subject: ${exception.message}")
                }
            }.onFailure { exception ->
                println("DEBUG: TeacherStudentApplicationsViewModel - Error getting student: ${exception.message}")
            }
        } catch (e: Exception) {
            println("DEBUG: TeacherStudentApplicationsViewModel - Exception creating enrollment: ${e.message}")
        }
    }

    private suspend fun createStudentEnrollmentFromSubjectApplication(application: com.smartacademictracker.data.model.SubjectApplication) {
        try {
            println("DEBUG: TeacherStudentApplicationsViewModel - Creating enrollment for subject application: ${application.id}")
            
            // Get current teacher's section assignment for this subject
            val currentUserResult = userRepository.getCurrentUser()
            currentUserResult.onSuccess { teacher ->
                if (teacher != null) {
                    val sectionAssignmentsResult = sectionAssignmentRepository.getSectionAssignmentsByTeacher(teacher.id)
                    sectionAssignmentsResult.onSuccess { assignments ->
                        val teacherSection = assignments.find { it.subjectId == application.subjectId }?.sectionName
                        if (teacherSection != null) {
                            println("DEBUG: TeacherStudentApplicationsViewModel - Using teacher's assigned section: $teacherSection")
                            
                            // Get student information
                            val studentResult = userRepository.getUserById(application.studentId)
                            studentResult.onSuccess { student ->
                                // Get subject information
                                val subjectResult = subjectRepository.getSubjectById(application.subjectId)
                                subjectResult.onSuccess { subject ->
                                    // Create enrollment with teacher's assigned section
                                    val enrollment = StudentEnrollment(
                                        studentId = application.studentId,
                                        studentName = application.studentName,
                                        subjectId = application.subjectId,
                                        subjectName = application.subjectName,
                                        sectionName = teacherSection, // Use teacher's assigned section
                                        teacherId = teacher.id,
                                        teacherName = "${teacher.firstName} ${teacher.lastName}",
                                        teacherEmail = teacher.email,
                                        courseId = application.courseId,
                                        courseName = application.courseName,
                                        yearLevelId = application.yearLevelId,
                                        yearLevelName = application.yearLevelName,
                                        academicPeriodId = application.academicPeriodId,
                                        enrollmentDate = System.currentTimeMillis(),
                                        status = EnrollmentStatus.ACTIVE
                                    )

                                    println("DEBUG: TeacherStudentApplicationsViewModel - Calling studentEnrollmentRepository.enrollStudent")
                                    val enrollmentResult = studentEnrollmentRepository.enrollStudent(enrollment)
                                    enrollmentResult.onSuccess { enrollmentId ->
                                        println("DEBUG: TeacherStudentApplicationsViewModel - Successfully created enrollment for student ${application.studentName} with ID: $enrollmentId")
                                        realtimeDataManager.loadAllData()
                                    }.onFailure { exception ->
                                        println("DEBUG: TeacherStudentApplicationsViewModel - Error creating enrollment: ${exception.message}")
                                    }
                                }.onFailure { exception ->
                                    println("DEBUG: TeacherStudentApplicationsViewModel - Error getting subject: ${exception.message}")
                                }
                            }.onFailure { exception ->
                                println("DEBUG: TeacherStudentApplicationsViewModel - Error getting student: ${exception.message}")
                            }
                        } else {
                            println("DEBUG: TeacherStudentApplicationsViewModel - No section assignment found for teacher in subject ${application.subjectId}")
                        }
                    }.onFailure { exception ->
                        println("DEBUG: TeacherStudentApplicationsViewModel - Error getting section assignments: ${exception.message}")
                    }
                }
            }.onFailure { exception ->
                println("DEBUG: TeacherStudentApplicationsViewModel - Error getting current teacher: ${exception.message}")
            }
        } catch (e: Exception) {
            println("DEBUG: TeacherStudentApplicationsViewModel - Exception creating enrollment: ${e.message}")
        }
    }
}

data class TeacherStudentApplicationsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val processingApplications: Set<String> = emptySet()
)
