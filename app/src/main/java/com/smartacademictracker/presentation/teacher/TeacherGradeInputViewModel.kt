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
import com.smartacademictracker.data.utils.GradeCsvParser
import com.smartacademictracker.data.utils.GradeRow
import com.smartacademictracker.data.notification.GradeCompletionNotificationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class TeacherGradeInputViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val studentEnrollmentRepository: StudentEnrollmentRepository,
    private val gradeRepository: GradeRepository,
    private val userRepository: UserRepository,
    private val sectionAssignmentRepository: SectionAssignmentRepository,
    private val gradeCompletionNotificationService: GradeCompletionNotificationService,
    private val notificationSenderService: com.smartacademictracker.data.notification.NotificationSenderService
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
                
                val studentEnrollmentsResult = studentEnrollmentRepository.getStudentsBySubject(subjectId)
                studentEnrollmentsResult.onSuccess { seList ->
                    // Get ALL sections the teacher is assigned to for this subject
                    if (teacherId.isNotEmpty()) {
                        sectionAssignmentRepository.getSectionAssignmentsByTeacher(teacherId).onSuccess { assignments ->
                            // Get all section names for this subject
                            val teacherAssignedSections = assignments
                                .filter { it.subjectId == subjectId && it.status == com.smartacademictracker.data.model.AssignmentStatus.ACTIVE }
                                .map { it.sectionName }
                                .toSet()
                            
                            // Filter students: show those in teacher's assigned sections OR those with matching teacherId
                            // This ensures students enrolled without teacherId still show up if they're in the teacher's section
                            val filtered = when {
                                teacherAssignedSections.isNotEmpty() -> {
                                    // Show students in any of the teacher's assigned sections, regardless of teacherId
                                    seList.filter { 
                                        it.sectionName in teacherAssignedSections || 
                                        (teacherId.isNotEmpty() && it.teacherId == teacherId)
                                    }
                                }
                                teacherId.isNotEmpty() -> seList.filter { it.teacherId == teacherId }
                                else -> seList
                            }
                            
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
                            _enrollments.value = mapped
                        }.onFailure {
                            // If we can't get section assignments, fall back to filtering by teacherId
                            val filtered = if (teacherId.isNotEmpty()) {
                                seList.filter { it.teacherId == teacherId }
                            } else {
                                seList
                            }
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
                            _enrollments.value = mapped
                        }
                    } else {
                        // No teacher ID, show all students
                        val mapped = seList.map { se ->
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
                        _enrollments.value = mapped
                    }
                }.onFailure { error ->
                    // Fallback to legacy enrollments if needed
                    val enrollmentsResult = enrollmentRepository.getEnrollmentsBySubject(subjectId)
                    enrollmentsResult.onSuccess { enrollmentsList ->
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
                        .catch { }
                        .collect { grades ->
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
                        // Set teacherId to current user's ID (teacher creating the grade)
                        // This ensures the grade is associated with the correct teacher
                        Grade(
                            studentId = studentId,
                            studentName = studentName,
                            subjectId = subject.id,
                            subjectName = subject.name,
                            teacherId = userId, // Use current teacher's ID, not subject.teacherId
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
                        // Get teacher name for notification
                        val currentUser = userRepository.getCurrentUser().getOrNull()
                        val teacherName = currentUser?.let { "${it.firstName} ${it.lastName}" } ?: ""
                        
                        gradeCompletionNotificationService.checkAndNotifyGradeCompletion(
                            subjectId = subject.id,
                            subjectName = subject.name,
                            teacherId = userId, // Use current teacher's ID
                            teacherName = teacherName,
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
            // Get current user ID for aggregate
            val currentUser = userRepository.getCurrentUser().getOrNull()
            val currentUserId = currentUser?.id ?: ""
            
            val aggregateResult = gradeRepository.createOrUpdateStudentGradeAggregate(
                studentId = studentId,
                subjectId = subject.id,
                studentName = studentName,
                subjectName = subject.name,
                teacherId = currentUserId, // Use current teacher's ID
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
    
    fun setError(error: String?) {
        _uiState.value = _uiState.value.copy(error = error)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
    
    /**
     * Parse CSV file for grade import
     */
    fun parseGradeCsv(inputStream: InputStream, fileName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val parseResult = if (fileName.endsWith(".csv", ignoreCase = true)) {
                    GradeCsvParser.parseGradeCsv(inputStream)
                } else {
                    Result.failure(Exception("Unsupported file format. Please use CSV (.csv) files."))
                }
                
                parseResult.onSuccess { gradeRows ->
                    // Validate that all students in CSV match exactly with teacher's student list
                    val csvStudentNames = gradeRows.map { it.studentName.trim().lowercase() }.toSet()
                    val enrolledStudentNames = _enrollments.value.map { 
                        it.studentName.trim().lowercase() 
                    }.toSet()
                    
                    // Check if CSV has all enrolled students
                    val missingInCsv = enrolledStudentNames.filter { it !in csvStudentNames }
                    val extraInCsv = csvStudentNames.filter { it !in enrolledStudentNames }
                    
                    val validationErrors = mutableListOf<String>()
                    if (missingInCsv.isNotEmpty()) {
                        validationErrors.add("Missing students in CSV: ${missingInCsv.joinToString(", ")}")
                    }
                    if (extraInCsv.isNotEmpty()) {
                        validationErrors.add("Extra students in CSV (not enrolled): ${extraInCsv.joinToString(", ")}")
                    }
                    
                    if (validationErrors.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "CSV validation failed:\n${validationErrors.joinToString("\n")}\n\n" +
                                    "The CSV file must contain exactly the same students as your enrolled student list."
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            parsedGradeRows = gradeRows
                        )
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to parse CSV file. Please check the file format."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error reading file: ${e.message ?: "Unknown error"}"
                )
            } finally {
                try {
                    inputStream.close()
                } catch (e: Exception) {
                    // Ignore close errors
                }
            }
        }
    }
    
    /**
     * Import grades from parsed CSV data
     */
    fun importGradesFromCsv() {
        viewModelScope.launch {
            val parsedRows = _uiState.value.parsedGradeRows
            if (parsedRows.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    error = "No grades to import. Please parse a CSV file first."
                )
                return@launch
            }
            
            val subject = _subject.value
            if (subject == null) {
                _uiState.value = _uiState.value.copy(
                    error = "Subject not loaded. Please try again."
                )
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val currentUser = userRepository.getCurrentUser().getOrNull()
                val userId = currentUser?.id ?: ""
                val userRole = currentUser?.role ?: "TEACHER"
                
                var successCount = 0
                var failureCount = 0
                val errors = mutableListOf<String>()
                
                // Track submitted periods per student for notification purposes
                val studentSubmittedPeriods = mutableMapOf<String, MutableSet<GradePeriod>>()
                
                // Create a map of student names to enrollments (case-insensitive)
                val enrollmentMap = _enrollments.value.associateBy { 
                    it.studentName.trim().lowercase() 
                }
                
                for (gradeRow in parsedRows) {
                    try {
                        val enrollment = enrollmentMap[gradeRow.studentName.trim().lowercase()]
                        if (enrollment == null) {
                            failureCount++
                            errors.add("Student not found: ${gradeRow.studentName}")
                            continue
                        }
                        
                        val studentId = enrollment.studentId
                        val studentName = enrollment.studentName
                        
                        // Import Prelim grade
                        gradeRow.prelim?.let { prelim ->
                            if (GradeCalculationEngine.isValidGrade(prelim)) {
                                val existingGradeResult = gradeRepository.getGradesByStudentSubjectAndPeriod(
                                    studentId, subject.id, GradePeriod.PRELIM
                                )
                                val existingGrade = existingGradeResult.getOrNull()
                                
                                // Check if grade is locked
                                if (existingGrade != null && existingGrade.id.isNotEmpty() && 
                                    userRole != "ADMIN" && existingGrade.unlockedBy == null) {
                                    errors.add("$studentName - Prelim grade is locked")
                                    failureCount++
                                } else {
                                    val grade = existingGrade?.copy(
                                        score = prelim,
                                        percentage = prelim,
                                        letterGrade = GradeCalculationEngine.calculateLetterGrade(prelim),
                                        dateRecorded = System.currentTimeMillis()
                                    ) ?: Grade(
                                        studentId = studentId,
                                        studentName = studentName,
                                        subjectId = subject.id,
                                        subjectName = subject.name,
                                        teacherId = userId, // Use current teacher's ID
                                        gradePeriod = GradePeriod.PRELIM,
                                        score = prelim,
                                        maxScore = 100.0,
                                        percentage = prelim,
                                        letterGrade = GradeCalculationEngine.calculateLetterGrade(prelim),
                                        semester = "Fall 2025",
                                        academicYear = "2025-2026"
                                    )
                                    
                                    val result = if (existingGrade != null) {
                                        gradeRepository.updateGrade(grade, userId, userRole, skipNotification = true)
                                    } else {
                                        gradeRepository.createGrade(grade, userId, userRole, skipNotification = true)
                                    }
                                    
                                    result.onSuccess { 
                                        successCount++
                                        // Track that Prelim was submitted for this student
                                        studentSubmittedPeriods.getOrPut(studentId) { mutableSetOf() }.add(GradePeriod.PRELIM)
                                    }
                                        .onFailure { 
                                            failureCount++
                                            errors.add("$studentName - Prelim: ${it.message}")
                                        }
                                }
                            } else {
                                errors.add("$studentName - Invalid Prelim grade: $prelim")
                                failureCount++
                            }
                        }
                        
                        // Import Midterm grade
                        gradeRow.midterm?.let { midterm ->
                            if (GradeCalculationEngine.isValidGrade(midterm)) {
                                val existingGradeResult = gradeRepository.getGradesByStudentSubjectAndPeriod(
                                    studentId, subject.id, GradePeriod.MIDTERM
                                )
                                val existingGrade = existingGradeResult.getOrNull()
                                
                                if (existingGrade != null && existingGrade.id.isNotEmpty() && 
                                    userRole != "ADMIN" && existingGrade.unlockedBy == null) {
                                    errors.add("$studentName - Midterm grade is locked")
                                    failureCount++
                                } else {
                                    val grade = existingGrade?.copy(
                                        score = midterm,
                                        percentage = midterm,
                                        letterGrade = GradeCalculationEngine.calculateLetterGrade(midterm),
                                        dateRecorded = System.currentTimeMillis()
                                    ) ?: Grade(
                                        studentId = studentId,
                                        studentName = studentName,
                                        subjectId = subject.id,
                                        subjectName = subject.name,
                                        teacherId = userId, // Use current teacher's ID
                                        gradePeriod = GradePeriod.MIDTERM,
                                        score = midterm,
                                        maxScore = 100.0,
                                        percentage = midterm,
                                        letterGrade = GradeCalculationEngine.calculateLetterGrade(midterm),
                                        semester = "Fall 2025",
                                        academicYear = "2025-2026"
                                    )
                                    
                                    val result = if (existingGrade != null) {
                                        gradeRepository.updateGrade(grade, userId, userRole, skipNotification = true)
                                    } else {
                                        gradeRepository.createGrade(grade, userId, userRole, skipNotification = true)
                                    }
                                    
                                    result.onSuccess { 
                                        successCount++
                                        // Track that Midterm was submitted for this student
                                        studentSubmittedPeriods.getOrPut(studentId) { mutableSetOf() }.add(GradePeriod.MIDTERM)
                                    }
                                        .onFailure { 
                                            failureCount++
                                            errors.add("$studentName - Midterm: ${it.message}")
                                        }
                                }
                            } else {
                                errors.add("$studentName - Invalid Midterm grade: $midterm")
                                failureCount++
                            }
                        }
                        
                        // Import Final grade
                        gradeRow.final?.let { final ->
                            if (GradeCalculationEngine.isValidGrade(final)) {
                                val existingGradeResult = gradeRepository.getGradesByStudentSubjectAndPeriod(
                                    studentId, subject.id, GradePeriod.FINAL
                                )
                                val existingGrade = existingGradeResult.getOrNull()
                                
                                if (existingGrade != null && existingGrade.id.isNotEmpty() && 
                                    userRole != "ADMIN" && existingGrade.unlockedBy == null) {
                                    errors.add("$studentName - Final grade is locked")
                                    failureCount++
                                } else {
                                    val grade = existingGrade?.copy(
                                        score = final,
                                        percentage = final,
                                        letterGrade = GradeCalculationEngine.calculateLetterGrade(final),
                                        dateRecorded = System.currentTimeMillis()
                                    ) ?: Grade(
                                        studentId = studentId,
                                        studentName = studentName,
                                        subjectId = subject.id,
                                        subjectName = subject.name,
                                        teacherId = userId, // Use current teacher's ID
                                        gradePeriod = GradePeriod.FINAL,
                                        score = final,
                                        maxScore = 100.0,
                                        percentage = final,
                                        letterGrade = GradeCalculationEngine.calculateLetterGrade(final),
                                        semester = "Fall 2025",
                                        academicYear = "2025-2026"
                                    )
                                    
                                    val result = if (existingGrade != null) {
                                        gradeRepository.updateGrade(grade, userId, userRole, skipNotification = true)
                                    } else {
                                        gradeRepository.createGrade(grade, userId, userRole, skipNotification = true)
                                    }
                                    
                                    result.onSuccess { 
                                        successCount++
                                        // Track that Final was submitted for this student
                                        studentSubmittedPeriods.getOrPut(studentId) { mutableSetOf() }.add(GradePeriod.FINAL)
                                    }
                                        .onFailure { 
                                            failureCount++
                                            errors.add("$studentName - Final: ${it.message}")
                                        }
                                }
                            } else {
                                errors.add("$studentName - Invalid Final grade: $final")
                                failureCount++
                            }
                        }
                    } catch (e: Exception) {
                        failureCount++
                        errors.add("${gradeRow.studentName}: ${e.message}")
                    }
                }
                
                // Send notifications for each student based on submitted periods
                // Note: Individual grade notifications are disabled in GradeRepository for CSV imports
                // We send a single consolidated notification per student here
                studentSubmittedPeriods.forEach { (studentId, periods) ->
                    if (periods.isNotEmpty()) {
                        notificationSenderService.sendMultipleGradeUpdateNotification(
                            userId = studentId,
                            subjectName = subject.name,
                            submittedPeriods = periods.toList()
                        )
                    }
                }
                
                // Reload grades after import
                val refreshedGradesResult = gradeRepository.getGradesBySubject(subject.id)
                refreshedGradesResult.onSuccess { refreshedGrades ->
                    _grades.value = refreshedGrades
                    
                    // Update grade aggregates for all students who had grades imported
                    // This recalculates the final average based on prelim, midterm, and final grades
                    studentSubmittedPeriods.keys.forEach { studentId ->
                        val enrollment = enrollmentMap.values.find { it.studentId == studentId }
                        if (enrollment != null) {
                            updateStudentGradeAggregate(
                                studentId = studentId,
                                studentName = enrollment.studentName,
                                subject = subject
                            )
                        }
                    }
                }
                
                val message = if (failureCount == 0) {
                    "Successfully imported grades for ${successCount} student(s)!"
                } else {
                    "Imported ${successCount} grade(s). ${failureCount} failed.\n" +
                    if (errors.size <= 5) errors.joinToString("\n") else errors.take(5).joinToString("\n") + "\n... and ${errors.size - 5} more"
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = message,
                    parsedGradeRows = emptyList() // Clear parsed rows after import
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error during import: ${e.message}"
                )
            }
        }
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
    val savingGrades: Set<String> = emptySet(), // Track which student grades are being saved
    val parsedGradeRows: List<GradeRow> = emptyList() // Parsed CSV grade rows
)
