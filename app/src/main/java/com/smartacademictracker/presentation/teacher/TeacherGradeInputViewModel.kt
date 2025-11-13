package com.smartacademictracker.presentation.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.Subject
import com.smartacademictracker.data.model.Enrollment
import com.smartacademictracker.data.model.Grade
import com.smartacademictracker.data.model.GradePeriod
import com.smartacademictracker.data.model.StudentGradeAggregate
import com.smartacademictracker.data.repository.SubjectRepository
import com.smartacademictracker.data.repository.EnrollmentRepository
import com.smartacademictracker.data.repository.StudentEnrollmentRepository
import com.smartacademictracker.data.repository.GradeRepository
import com.smartacademictracker.data.repository.UserRepository
import com.smartacademictracker.data.repository.SectionAssignmentRepository
import com.smartacademictracker.data.utils.GradeCalculationEngine
import com.smartacademictracker.data.notification.GradeCompletionNotificationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeacherGradeInputViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val studentEnrollmentRepository: StudentEnrollmentRepository,
    private val gradeRepository: GradeRepository,
    private val userRepository: UserRepository,
    private val sectionAssignmentRepository: SectionAssignmentRepository,
    private val gradeCompletionNotificationService: GradeCompletionNotificationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherGradeInputUiState())
    val uiState: StateFlow<TeacherGradeInputUiState> = _uiState.asStateFlow()

    private val _subject = MutableStateFlow<Subject?>(null)
    val subject: StateFlow<Subject?> = _subject.asStateFlow()

    private val _enrollments = MutableStateFlow<List<Enrollment>>(emptyList())
    val enrollments: StateFlow<List<Enrollment>> = _enrollments.asStateFlow()

    private val _grades = MutableStateFlow<List<Grade>>(emptyList())
    val grades: StateFlow<List<Grade>> = _grades.asStateFlow()
    
    private val _gradeAggregates = MutableStateFlow<List<StudentGradeAggregate>>(emptyList())
    val gradeAggregates: StateFlow<List<StudentGradeAggregate>> = _gradeAggregates.asStateFlow()
    
    private val _selectedPeriod = MutableStateFlow(GradePeriod.PRELIM)
    val selectedPeriod: StateFlow<GradePeriod> = _selectedPeriod.asStateFlow()
    
    private var currentSubjectId: String? = null
    private var gradeListenerJob: Job? = null

    fun loadSubjectAndStudents(subjectId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Load subject details
                val subjectResult = subjectRepository.getSubjectById(subjectId)
                subjectResult.onSuccess { subject ->
                    _subject.value = subject
                }

                // Load enrollments for this subject (new section-based enrollments)
                val currentUser = userRepository.getCurrentUser().getOrNull()
                val teacherId = currentUser?.id ?: ""
                println("DEBUG: TeacherGradeInputViewModel - Loading students for subject: $subjectId, teacher: $teacherId")
                
                val studentEnrollmentsResult = studentEnrollmentRepository.getStudentsBySubject(subjectId)
                studentEnrollmentsResult.onSuccess { seList ->
                    println("DEBUG: TeacherGradeInputViewModel - Found ${seList.size} student enrollments for subject $subjectId")
                    // Determine teacher's section for this subject
                    var teacherSection: String? = null
                    if (teacherId.isNotEmpty()) {
                        sectionAssignmentRepository.getSectionAssignmentsByTeacher(teacherId).onSuccess { assignments ->
                            teacherSection = assignments.firstOrNull { it.subjectId == subjectId }?.sectionName
                        }
                    }
                    seList.forEach { se ->
                        println("DEBUG: StudentEnrollment - Student: ${se.studentName}, TeacherId: ${se.teacherId}, Section: ${se.sectionName}, Status: ${se.status}")
                    }
                    
                    // Filter by teacher's section if available; otherwise by teacherId; else use all
                    val filtered = when {
                        teacherSection != null -> seList.filter { it.sectionName == teacherSection }
                        teacherId.isNotEmpty() -> seList.filter { it.teacherId == teacherId }
                        else -> seList
                    }
                    println("DEBUG: TeacherGradeInputViewModel - After filtering (section=$teacherSection teacher=$teacherId): ${filtered.size} students")
                    
                    val mapped = filtered.map { se ->
                        Enrollment(
                            id = se.id,
                            studentId = se.studentId,
                            studentName = se.studentName,
                            subjectId = se.subjectId,
                            subjectName = se.subjectName,
                            subjectCode = se.subjectCode,
                            enrolledAt = se.enrollmentDate,
                            semester = se.semester.name,
                            academicYear = se.academicYear,
                            active = se.status.name == "ACTIVE"
                        )
                    }
                    println("DEBUG: TeacherGradeInputViewModel - Final mapped enrollments: ${mapped.size}")
                    _enrollments.value = mapped
                }.onFailure { error ->
                    println("DEBUG: TeacherGradeInputViewModel - Error loading student enrollments: ${error.message}")
                    // Fallback to legacy enrollments if needed
                    val enrollmentsResult = enrollmentRepository.getEnrollmentsBySubject(subjectId)
                    enrollmentsResult.onSuccess { enrollmentsList ->
                        println("DEBUG: TeacherGradeInputViewModel - Fallback to legacy enrollments: ${enrollmentsList.size}")
                        _enrollments.value = enrollmentsList
                    }
                }

                // Load existing grades for this subject (initial load)
                val gradesResult = gradeRepository.getGradesBySubject(subjectId)
                gradesResult.onSuccess { gradesList ->
                    _grades.value = gradesList
                }
                
                // Cancel any existing grade listener
                gradeListenerJob?.cancel()
                
                // Set up real-time listener for grade updates
                currentSubjectId = subjectId
                gradeListenerJob = viewModelScope.launch {
                    gradeRepository.getGradesBySubjectFlow(subjectId)
                        .catch { exception ->
                            println("DEBUG: TeacherGradeInputViewModel - Error in real-time grade listener: ${exception.message}")
                        }
                        .collect { grades ->
                            println("DEBUG: TeacherGradeInputViewModel - Real-time grade update: ${grades.size} grades for subject $subjectId")
                            // Only update if this is still the current subject
                            if (currentSubjectId == subjectId) {
                                _grades.value = grades
                            }
                        }
                }
                
                // Load student grade aggregates for this subject
                val aggregatesResult = gradeRepository.getStudentGradeAggregatesBySubject(subjectId)
                aggregatesResult.onSuccess { aggregatesList ->
                    _gradeAggregates.value = aggregatesList
                }

                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load subject data"
                )
            }
        }
    }
    
    fun setSelectedPeriod(period: GradePeriod) {
        _selectedPeriod.value = period
    }

    fun updateGradeForPeriod(studentId: String, gradePeriod: GradePeriod, value: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val subject = _subject.value
                if (subject != null) {
                    // Validate grade input
                    if (!GradeCalculationEngine.isValidGrade(value)) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Grade must be between 0 and 100"
                        )
                        return@launch
                    }
                    
                    // Get current user info
                    val currentUser = userRepository.getCurrentUser().getOrNull()
                    val userId = currentUser?.id ?: ""
                    val userRole = currentUser?.role ?: "TEACHER"
                    
                    // Get student information
                    val enrollment = _enrollments.value.find { it.studentId == studentId }
                    val studentName = enrollment?.studentName ?: "Unknown Student"
                    
                    // Check if grade already exists
                    val existingGradeResult = gradeRepository.getGradesByStudentSubjectAndPeriod(
                        studentId, subject.id, gradePeriod
                    )
                    
                    val existingGrade = existingGradeResult.getOrNull()
                    
                    // Check if existing grade is locked
                    // A grade is considered locked if it exists and hasn't been unlocked by admin
                    // This handles both new grades with lock fields and old grades without lock fields
                    if (existingGrade != null && existingGrade.id.isNotEmpty() && userRole != "ADMIN") {
                        if (existingGrade.unlockedBy == null) {
                            // Grade exists and is locked (either explicitly locked or old grade without lock field)
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Grade is locked. Please request admin permission to edit."
                            )
                            return@launch
                        }
                    }
                    
                    val grade = if (existingGrade != null) {
                        // Update existing grade - preserve all fields except score-related ones
                        existingGrade.copy(
                            score = value,
                            percentage = value, // Since maxScore is 100, percentage equals score
                            letterGrade = GradeCalculationEngine.calculateLetterGrade(value),
                            dateRecorded = System.currentTimeMillis() // Update timestamp
                            // Lock fields will be preserved/updated by repository
                        )
                    } else {
                        // Create new grade
                        Grade(
                            studentId = studentId,
                            studentName = studentName,
                            subjectId = subject.id,
                            subjectName = subject.name,
                            teacherId = subject.teacherId ?: "",
                            gradePeriod = gradePeriod,
                            score = value,
                            maxScore = 100.0,
                            percentage = value,
                            letterGrade = GradeCalculationEngine.calculateLetterGrade(value),
                            semester = "Fall 2025", // TODO: Get from current academic period
                            academicYear = "2025-2026" // TODO: Get from current academic period
                        )
                    }

                    val result = if (existingGrade != null) {
                        gradeRepository.updateGrade(grade, userId, userRole)
                    } else {
                        gradeRepository.createGrade(grade, userId, userRole)
                    }
                    // Note: Re-locking is now handled in updateGrade() method

                    result.onSuccess {
                        // Reload all grades from Firestore to get the actual saved state with lock status
                        // This ensures the UI immediately reflects the locked status after saving
                        val refreshedGradesResult = gradeRepository.getGradesBySubject(subject.id)
                        refreshedGradesResult.onSuccess { refreshedGrades ->
                            _grades.value = refreshedGrades
                            println("DEBUG: TeacherGradeInputViewModel - Reloaded ${refreshedGrades.size} grades after save")
                        }.onFailure {
                            // Fallback: manually update the grade in the list
                            if (existingGrade != null) {
                                val updatedGrades = _grades.value.map { 
                                    if (it.id == grade.id) {
                                        // Mark as locked after save
                                        grade.copy(
                                            isLocked = true,
                                            lockedAt = System.currentTimeMillis(),
                                            lockedBy = userId,
                                            editRequested = false,
                                            unlockedBy = null,
                                            unlockedAt = null
                                        )
                                    } else it 
                                }
                                _grades.value = updatedGrades
                            } else {
                                // New grade - add it with lock fields
                                val lockedGrade = grade.copy(
                                    isLocked = true,
                                    lockedAt = System.currentTimeMillis(),
                                    lockedBy = userId
                                )
                                _grades.value = _grades.value + lockedGrade
                            }
                        }
                        
                        // Update or create student grade aggregate
                        updateStudentGradeAggregate(studentId, studentName, subject)

                        // Check if all grades are completed for this subject and period
                        gradeCompletionNotificationService.checkAndNotifyGradeCompletion(
                            subjectId = subject.id,
                            subjectName = subject.name,
                            teacherId = subject.teacherId ?: "",
                            teacherName = "", // Should get from user repository
                            gradePeriod = gradePeriod
                        )

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            successMessage = "${gradePeriod.displayName} grade saved successfully!"
                        )
                    }.onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to save grade"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Subject not found"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to update grade"
                )
            }
        }
    }
    
    private suspend fun updateStudentGradeAggregate(studentId: String, studentName: String, subject: Subject) {
        try {
            val aggregateResult = gradeRepository.createOrUpdateStudentGradeAggregate(
                studentId = studentId,
                subjectId = subject.id,
                studentName = studentName,
                subjectName = subject.name,
                teacherId = subject.teacherId ?: "",
                semester = "Fall 2025", // TODO: Get from current academic period
                academicYear = "2025-2026" // TODO: Get from current academic period
            )
            
            aggregateResult.onSuccess { updatedAggregate ->
                // Update local aggregates list
                val updatedAggregates = _gradeAggregates.value.toMutableList()
                val existingIndex = updatedAggregates.indexOfFirst { it.id == updatedAggregate.id }
                
                if (existingIndex >= 0) {
                    updatedAggregates[existingIndex] = updatedAggregate
                } else {
                    updatedAggregates.add(updatedAggregate)
                }
                
                _gradeAggregates.value = updatedAggregates
            }
        } catch (e: Exception) {
            // Log error but don't fail the main operation
            println("Failed to update grade aggregate: ${e.message}")
        }
    }
    
    fun getGradeForStudentAndPeriod(studentId: String, gradePeriod: GradePeriod): Grade? {
        return _grades.value.find { 
            it.studentId == studentId && it.gradePeriod == gradePeriod 
        }
    }
    
    fun getStudentGradeAggregate(studentId: String): StudentGradeAggregate? {
        return _gradeAggregates.value.find { it.studentId == studentId }
    }

    fun refreshData() {
        _subject.value?.let { subject ->
            loadSubjectAndStudents(subject.id)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
    
    /**
     * Request permission to edit a locked grade
     */
    fun requestGradeEdit(gradeId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val result = gradeRepository.requestGradeEdit(gradeId)
            
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Edit permission requested. Waiting for admin approval."
                )
                // Refresh grades to show updated status (editRequested flag)
                _subject.value?.id?.let { 
                    // Reload the specific grade to get updated editRequested status
                    val gradeResult = gradeRepository.getGradeById(gradeId)
                    gradeResult.onSuccess { updatedGrade ->
                        val updatedGrades = _grades.value.map { 
                            if (it.id == updatedGrade.id) updatedGrade else it 
                        }
                        _grades.value = updatedGrades
                        println("DEBUG: TeacherGradeInputViewModel - Updated grade ${updatedGrade.id} with editRequested=${updatedGrade.editRequested}")
                    }
                    // Also reload all data to ensure consistency
                    loadSubjectAndStudents(it) 
                }
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = exception.message ?: "Failed to request edit permission"
                )
            }
        }
    }
}

data class TeacherGradeInputUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val savingGrades: Set<String> = emptySet() // Track which student grades are being saved
)
