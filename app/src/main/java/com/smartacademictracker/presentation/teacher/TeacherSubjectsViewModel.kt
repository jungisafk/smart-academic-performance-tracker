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
import com.smartacademictracker.data.repository.SectionAssignmentRepository
import com.smartacademictracker.data.repository.StudentEnrollmentRepository
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
    private val enrollmentRepository: EnrollmentRepository,
    private val sectionAssignmentRepository: SectionAssignmentRepository,
    private val studentEnrollmentRepository: StudentEnrollmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherSubjectsUiState())
    val uiState: StateFlow<TeacherSubjectsUiState> = _uiState.asStateFlow()

    private val _availableSubjects = MutableStateFlow<List<Subject>>(emptyList())
    val availableSubjects: StateFlow<List<Subject>> = _availableSubjects.asStateFlow()

    private val _mySubjects = MutableStateFlow<List<Subject>>(emptyList())
    val mySubjects: StateFlow<List<Subject>> = _mySubjects.asStateFlow()

    private val _appliedSubjects = MutableStateFlow<List<Subject>>(emptyList())
    val appliedSubjects: StateFlow<List<Subject>> = _appliedSubjects.asStateFlow()

    private val _applications = MutableStateFlow<List<TeacherApplication>>(emptyList())
    val applications: StateFlow<List<TeacherApplication>> = _applications.asStateFlow()

    private val _approvedApplications = MutableStateFlow<List<TeacherApplication>>(emptyList())
    val approvedApplications: StateFlow<List<TeacherApplication>> = _approvedApplications.asStateFlow()

    fun loadSubjects() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Get current user
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        println("DEBUG: TeacherSubjectsViewModel - Loading subjects for teacher: ${user.id}")
                        
                        // Load ALL subjects first (like admin side does)
                        val allSubjectsResult = subjectRepository.getAllSubjects()
                        allSubjectsResult.onSuccess { allSubjects ->
                            println("DEBUG: TeacherSubjectsViewModel - Loaded ${allSubjects.size} total subjects")
                            allSubjects.forEach { subject ->
                                println("DEBUG: Subject - ID: ${subject.id}, Name: ${subject.name}, Code: ${subject.code}, TeacherId: ${subject.teacherId}")
                                println("DEBUG: Subject ${subject.name} - Sections: ${subject.sections}")
                            }
                            
                            // Filter available subjects (subjects with available sections)
                            filterAvailableSubjects(user.id, allSubjects)

                            // Load teacher's assigned subjects (from section assignments)
                            loadMySubjects(user.id, allSubjects)

                            // Load teacher's applied subjects
                            loadAppliedSubjects(user.id)

                            _uiState.value = _uiState.value.copy(isLoading = false)
                        }.onFailure { exception ->
                            println("DEBUG: TeacherSubjectsViewModel - Failed to load all subjects: ${exception.message}")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to load subjects"
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
                        
                        // Check if teacher has already applied for this subject
                        val hasAppliedResult = teacherApplicationRepository.hasTeacherAppliedForSubjectAnyStatus(user.id, subjectId)
                        hasAppliedResult.onSuccess { hasApplied ->
                            if (hasApplied) {
                                println("DEBUG: TeacherSubjectsViewModel - Teacher has already applied for this subject")
                                _uiState.value = _uiState.value.copy(
                                    applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                                    error = "You have already applied for this subject. Please check your applications."
                                )
                                return@onSuccess
                            }
                            
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
                        }.onFailure { exception ->
                            println("DEBUG: TeacherSubjectsViewModel - Failed to check application status: ${exception.message}")
                            _uiState.value = _uiState.value.copy(
                                applyingSubjects = _uiState.value.applyingSubjects - subjectId,
                                error = exception.message ?: "Failed to check application status"
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

    fun cancelApplication(applicationId: String) {
        viewModelScope.launch {
            println("DEBUG: TeacherSubjectsViewModel - Cancelling application: $applicationId")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                teacherApplicationRepository.cancelApplication(applicationId).onSuccess {
                    println("DEBUG: TeacherSubjectsViewModel - Application cancelled successfully")
                    // Reload all data to reflect the change
                    loadSubjects()
                }.onFailure { exception ->
                    println("DEBUG: TeacherSubjectsViewModel - Failed to cancel application: ${exception.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to cancel application"
                    )
                }
            } catch (e: Exception) {
                println("DEBUG: TeacherSubjectsViewModel - Exception cancelling application: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to cancel application"
                )
            }
        }
    }
    
    suspend fun getStudentCountForSubject(subjectId: String): Int {
        return try {
            // Use the new StudentEnrollmentRepository to get active students
            val currentUserResult = userRepository.getCurrentUser()
            currentUserResult.onSuccess { user ->
                if (user != null) {
                    // Get teacher's assigned sections for this subject
                    val assignmentsResult = sectionAssignmentRepository.getSectionAssignmentsByTeacher(user.id)
                    assignmentsResult.onSuccess { assignments ->
                        val subjectAssignments = assignments.filter { it.subjectId == subjectId }
                        if (subjectAssignments.isNotEmpty()) {
                            // Get students from assigned sections
                            val sectionNames = subjectAssignments.map { it.sectionName }
                            val studentEnrollmentsResult = studentEnrollmentRepository.getStudentsBySubject(subjectId)
                            studentEnrollmentsResult.onSuccess { enrollments ->
                                // Filter by teacher's assigned sections
                                val filteredEnrollments = enrollments.filter { 
                                    it.sectionName in sectionNames && it.status.name == "ACTIVE"
                                }
                                println("DEBUG: TeacherSubjectsViewModel - Found ${filteredEnrollments.size} students in sections $sectionNames for subject $subjectId")
                                return filteredEnrollments.size
                            }.onFailure { exception ->
                                println("DEBUG: TeacherSubjectsViewModel - Error getting student enrollments: ${exception.message}")
                                return 0
                            }
                        } else {
                            println("DEBUG: TeacherSubjectsViewModel - No section assignments found for teacher ${user.id} in subject $subjectId")
                            return 0
                        }
                    }.onFailure { exception ->
                        println("DEBUG: TeacherSubjectsViewModel - Error getting section assignments: ${exception.message}")
                        return 0
                    }
                }
            }.onFailure { exception ->
                println("DEBUG: TeacherSubjectsViewModel - Error getting current user: ${exception.message}")
                return 0
            }
            0
        } catch (e: Exception) {
            println("DEBUG: TeacherSubjectsViewModel - Error getting student count for subject $subjectId: ${e.message}")
            0
        }
    }

    suspend fun getSectionAvailability(subjectId: String): Map<String, Boolean> {
        return try {
            println("DEBUG: Getting section availability for subject $subjectId")
            val assignmentsResult = sectionAssignmentRepository.getSectionAssignmentsBySubject(subjectId)
            assignmentsResult.onSuccess { assignments ->
                println("DEBUG: Found ${assignments.size} assignments for subject $subjectId")
                assignments.forEach { assignment ->
                    println("DEBUG: Assignment - Section: ${assignment.sectionName}, Teacher: ${assignment.teacherId}")
                }
                
                // Get the subject to know all its sections
                val subjectResult = subjectRepository.getSubjectById(subjectId)
                subjectResult.onSuccess { subject ->
                    val assignedSections = assignments.map { it.sectionName }.toSet()
                    val allSections = subject.sections.toSet()
                    
                    println("DEBUG: Subject $subjectId - All sections: $allSections")
                    println("DEBUG: Subject $subjectId - Assigned sections: $assignedSections")
                    
                    // Return map of section name to availability (true = available, false = assigned)
                    val availability = allSections.associateWith { sectionName ->
                        !assignedSections.contains(sectionName)
                    }
                    println("DEBUG: Subject $subjectId - Section availability: $availability")
                    return availability
                }.onFailure { exception ->
                    println("DEBUG: Failed to get subject $subjectId: ${exception.message}")
                    return emptyMap()
                }
            }.onFailure { exception ->
                println("DEBUG: Failed to get assignments for subject $subjectId: ${exception.message}")
                return emptyMap()
            }
            emptyMap()
        } catch (e: Exception) {
            println("DEBUG: TeacherSubjectsViewModel - Error getting section availability for subject $subjectId: ${e.message}")
            emptyMap()
        }
    }

    suspend fun getAssignedSectionsForSubject(subjectId: String): List<String> {
        return try {
            val currentUserResult = userRepository.getCurrentUser()
            currentUserResult.onSuccess { user ->
                if (user != null) {
                    val assignmentsResult = sectionAssignmentRepository.getSectionAssignmentsByTeacher(user.id)
                    assignmentsResult.onSuccess { assignments ->
                        // Filter assignments for this specific subject
                        val subjectAssignments = assignments.filter { it.subjectId == subjectId }
                        return subjectAssignments.map { it.sectionName }
                    }.onFailure {
                        return emptyList()
                    }
                }
            }.onFailure {
                return emptyList()
            }
            emptyList()
        } catch (e: Exception) {
            println("DEBUG: TeacherSubjectsViewModel - Error getting assigned sections for subject $subjectId: ${e.message}")
            emptyList()
        }
    }

    private suspend fun loadMySubjects(teacherId: String, allSubjects: List<Subject>) {
        try {
            // Get section assignments for this teacher
            val assignmentsResult = sectionAssignmentRepository.getSectionAssignmentsByTeacher(teacherId)
            assignmentsResult.onSuccess { assignments ->
                println("DEBUG: TeacherSubjectsViewModel - Found ${assignments.size} section assignments for teacher")
                
                // Get unique subject IDs from assignments
                val assignedSubjectIds = assignments.map { it.subjectId }.toSet()
                println("DEBUG: TeacherSubjectsViewModel - Assigned subject IDs: $assignedSubjectIds")
                
                // Filter subjects that teacher is assigned to
                val mySubjects = allSubjects.filter { it.id in assignedSubjectIds }
                _mySubjects.value = mySubjects
                println("DEBUG: TeacherSubjectsViewModel - Found ${mySubjects.size} subjects assigned to teacher")
                
                mySubjects.forEach { subject ->
                    println("DEBUG: My Subject - ID: ${subject.id}, Name: ${subject.name}, Code: ${subject.code}")
                }
            }.onFailure { exception ->
                println("DEBUG: TeacherSubjectsViewModel - Failed to load section assignments: ${exception.message}")
                _mySubjects.value = emptyList()
            }
        } catch (e: Exception) {
            println("DEBUG: TeacherSubjectsViewModel - Error loading my subjects: ${e.message}")
            _mySubjects.value = emptyList()
        }
    }

    private suspend fun filterAvailableSubjects(teacherId: String, subjects: List<Subject>) {
        try {
            println("DEBUG: TeacherSubjectsViewModel - Filtering ${subjects.size} subjects for teacher $teacherId")
            subjects.forEach { subject ->
                println("DEBUG: Available Subject - ID: ${subject.id}, Name: ${subject.name}, Code: ${subject.code}, TeacherId: ${subject.teacherId}")
            }
            
            // Get teacher's applications
            val applicationsResult = teacherApplicationRepository.getApplicationsByTeacher(teacherId)
            applicationsResult.onSuccess { applications ->
                println("DEBUG: TeacherSubjectsViewModel - Found ${applications.size} applications for teacher")
                applications.forEach { app ->
                    println("DEBUG: Application - SubjectId: ${app.subjectId}, Status: ${app.status}")
                }
                
                // Get subject IDs that teacher has already applied for
                val appliedSubjectIds = applications
                    .filter { it.status == ApplicationStatus.PENDING }
                    .map { it.subjectId }
                    .toSet()
                
                println("DEBUG: TeacherSubjectsViewModel - Applied subject IDs: $appliedSubjectIds")
                
                // Filter subjects that teacher hasn't applied for AND have available sections
                val availableSubjects = mutableListOf<Subject>()
                for (subject in subjects) {
                    val notApplied = subject.id !in appliedSubjectIds
                    println("DEBUG: Checking subject ${subject.name} (${subject.id}) - Not Applied: $notApplied")
                    
                    if (notApplied) {
                        // Check if this subject has any available sections
                        val sectionAvailability = getSectionAvailability(subject.id)
                        val hasAvailableSections = sectionAvailability.values.any { it }
                        
                        println("DEBUG: Subject ${subject.name} - Section Availability: $sectionAvailability")
                        println("DEBUG: Subject ${subject.name} - Has Available Sections: $hasAvailableSections")
                        
                        if (hasAvailableSections) {
                            availableSubjects.add(subject)
                            println("DEBUG: Added ${subject.name} to available subjects")
                        } else {
                            println("DEBUG: ${subject.name} has no available sections, skipping")
                        }
                    } else {
                        println("DEBUG: ${subject.name} already applied, skipping")
                    }
                }
                _availableSubjects.value = availableSubjects
                
                println("DEBUG: TeacherSubjectsViewModel - Final available subjects: ${availableSubjects.size}")
                availableSubjects.forEach { subject ->
                    println("DEBUG: Final Available - ID: ${subject.id}, Name: ${subject.name}, Code: ${subject.code}")
                }
            }.onFailure { exception ->
                println("DEBUG: TeacherSubjectsViewModel - Failed to load applications: ${exception.message}")
                // If we can't load applications, show only unassigned subjects
                val availableSubjects = subjects.filter { it.teacherId == null }
                _availableSubjects.value = availableSubjects
                println("DEBUG: TeacherSubjectsViewModel - Fallback: showing ${availableSubjects.size} unassigned subjects")
            }
        } catch (e: Exception) {
            println("DEBUG: TeacherSubjectsViewModel - Exception filtering subjects: ${e.message}")
            // If there's an exception, show only unassigned subjects
            val availableSubjects = subjects.filter { it.teacherId == null }
            _availableSubjects.value = availableSubjects
            println("DEBUG: TeacherSubjectsViewModel - Exception fallback: showing ${availableSubjects.size} unassigned subjects")
        }
    }

    private suspend fun loadAppliedSubjects(teacherId: String) {
        try {
            // Get teacher's applications
            val applicationsResult = teacherApplicationRepository.getApplicationsByTeacher(teacherId)
            applicationsResult.onSuccess { applications ->
                println("DEBUG: TeacherSubjectsViewModel - Found ${applications.size} applications for teacher")
                
                // Store all applications
                _applications.value = applications
                
                // Store approved applications separately
                val approvedApps = applications.filter { it.status == ApplicationStatus.APPROVED }
                _approvedApplications.value = approvedApps
                println("DEBUG: TeacherSubjectsViewModel - Found ${approvedApps.size} approved applications")
                
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
