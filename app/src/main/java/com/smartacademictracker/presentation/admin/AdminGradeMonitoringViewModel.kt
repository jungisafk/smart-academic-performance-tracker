package com.smartacademictracker.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.repository.GradeRepository
import com.smartacademictracker.data.repository.EnrollmentRepository
import com.smartacademictracker.data.repository.StudentEnrollmentRepository
import com.smartacademictracker.data.repository.SubjectRepository
import com.smartacademictracker.data.repository.CourseRepository
import com.smartacademictracker.data.repository.YearLevelRepository
import com.smartacademictracker.data.model.GradeStatus
import com.smartacademictracker.data.model.StudentGradeAggregate
import com.smartacademictracker.data.model.Grade
import com.smartacademictracker.data.model.GradePeriod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminGradeMonitoringViewModel @Inject constructor(
    private val gradeRepository: GradeRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val studentEnrollmentRepository: StudentEnrollmentRepository,
    private val subjectRepository: SubjectRepository,
    private val courseRepository: CourseRepository,
    private val yearLevelRepository: YearLevelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminGradeMonitoringUiState())
    val uiState: StateFlow<AdminGradeMonitoringUiState> = _uiState.asStateFlow()

    private val _gradeAggregates = MutableStateFlow<List<StudentGradeAggregate>>(emptyList())
    val gradeAggregates: StateFlow<List<StudentGradeAggregate>> = _gradeAggregates.asStateFlow()
    
    private val _allGradeAggregates = MutableStateFlow<List<StudentGradeAggregate>>(emptyList())
    
    // Cache enrollments and subjects to avoid database calls during filtering
    private val _cachedEnrollments = MutableStateFlow<List<com.smartacademictracker.data.model.StudentEnrollment>>(emptyList())
    private val _cachedSubjects = MutableStateFlow<List<com.smartacademictracker.data.model.Subject>>(emptyList())
    
    // Cache courses to map course display name to course ID
    private val _cachedCourses = MutableStateFlow<List<com.smartacademictracker.data.model.Course>>(emptyList())

    fun loadGradeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // Load filter options first
                loadFilterOptions()
                
                // Load all grades and create aggregates
                val gradesResult = gradeRepository.getAllGrades()
                val enrollmentsResult = studentEnrollmentRepository.getAllActiveEnrollments()

                gradesResult.onSuccess { gradesList ->
                    enrollmentsResult.onSuccess { enrollmentsList ->
                        // Cache enrollments for fast filtering
                        _cachedEnrollments.value = enrollmentsList
                        
                        // Cache subjects for fast filtering
                        val subjectsResult = subjectRepository.getAllSubjects()
                        subjectsResult.onSuccess { subjects ->
                            _cachedSubjects.value = subjects
                            // Update filtered options after caching subjects
                            updateFilteredOptions()
                        }
                        
                        // Create grade aggregates from individual grades
                        val aggregates = createGradeAggregates(gradesList, enrollmentsList)
                        _allGradeAggregates.value = aggregates
                        
                        // Apply filters
                        applyFilters()

                        println("DEBUG: AdminGradeMonitoringViewModel - Loaded ${aggregates.size} grade aggregates")
                    }.onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load enrollments"
                        )
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load grades"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load grade data"
                )
            }
        }
    }
    
    private suspend fun loadFilterOptions() {
        // Load courses
        courseRepository.getAllCourses().onSuccess { courses ->
            // Cache courses for ID lookup
            _cachedCourses.value = courses
            val courseNames = courses.map { "${it.code} - ${it.name}" }.distinct().sorted()
            _uiState.value = _uiState.value.copy(availableCourses = courseNames)
        }
        
        // Load year levels
        yearLevelRepository.getAllYearLevels().onSuccess { yearLevels ->
            val yearLevelNames = yearLevels.map { it.name }.distinct().sorted()
            _uiState.value = _uiState.value.copy(availableYearLevels = yearLevelNames)
        }
        
        // Note: Subjects and sections will be set by updateFilteredOptions() after caching
        // This ensures they are filtered based on course/year level selections
    }
    
    fun setYearLevelFilter(yearLevel: String?) {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            selectedYearLevel = yearLevel,
            // Clear dependent filters
            selectedSubject = null,
            selectedSection = null
        )
        updateFilteredOptions()
        applyFilters()
    }
    
    fun setCourseFilter(course: String?) {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            selectedCourse = course,
            // Clear dependent filters
            selectedSubject = null,
            selectedSection = null
        )
        updateFilteredOptions()
        applyFilters()
    }
    
    fun setSubjectFilter(subject: String?) {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            selectedSubject = subject,
            // Clear dependent filter
            selectedSection = null
        )
        updateFilteredOptions()
        applyFilters()
    }
    
    fun setSectionFilter(section: String?) {
        _uiState.value = _uiState.value.copy(selectedSection = section)
        applyFilters()
    }
    
    fun clearAllFilters() {
        _uiState.value = _uiState.value.copy(
            selectedYearLevel = null,
            selectedCourse = null,
            selectedSubject = null,
            selectedSection = null
        )
        updateFilteredOptions()
        applyFilters()
    }
    
    private fun updateFilteredOptions() {
        val currentState = _uiState.value
        val allSubjects = _cachedSubjects.value
        val allEnrollments = _cachedEnrollments.value
        
        // If no subjects or enrollments cached yet, don't update
        if (allSubjects.isEmpty() && allEnrollments.isEmpty()) {
            return
        }
        
        // Filter subjects based on course and year level
        var filteredSubjects = allSubjects
        
        // Filter by course - match by course ID for accurate filtering
        if (currentState.selectedCourse != null) {
            // Find the course ID from the selected course string (format: "CODE - NAME")
            val selectedCourseCode = currentState.selectedCourse.split(" - ").getOrNull(0) ?: ""
            val selectedCourseName = currentState.selectedCourse.split(" - ").getOrNull(1) ?: currentState.selectedCourse
            
            val matchingCourse = _cachedCourses.value.find { course ->
                "${course.code} - ${course.name}" == currentState.selectedCourse ||
                course.code == selectedCourseCode ||
                course.name == selectedCourseName
            }
            
            if (matchingCourse != null) {
                // Filter subjects by courseId (most accurate) or course name/code as fallback
                filteredSubjects = filteredSubjects.filter { subject ->
                    subject.courseId == matchingCourse.id ||
                    subject.courseCode == matchingCourse.code ||
                    (subject.courseName == matchingCourse.name && subject.courseCode == matchingCourse.code)
                }
            } else {
                // Fallback: filter by name/code if course not found in cache
                filteredSubjects = filteredSubjects.filter { subject ->
                    subject.courseName == selectedCourseName || 
                    subject.courseCode == selectedCourseCode ||
                    "${subject.courseCode} - ${subject.courseName}" == currentState.selectedCourse
                }
            }
        }
        
        // Filter by year level
        if (currentState.selectedYearLevel != null) {
            filteredSubjects = filteredSubjects.filter { subject ->
                subject.yearLevelName == currentState.selectedYearLevel
            }
        }
        
        // Get available subjects
        val availableSubjectNames = filteredSubjects.map { it.name }.distinct().sorted()
        
        // Filter sections based on filtered subjects and enrollments
        val filteredSubjectIds = filteredSubjects.map { it.id }.toSet()
        val filteredEnrollments = if (filteredSubjectIds.isNotEmpty()) {
            allEnrollments.filter { it.subjectId in filteredSubjectIds }
        } else {
            allEnrollments
        }
        
        // Get sections from both subjects and enrollments
        val sectionsFromSubjects = filteredSubjects.flatMap { it.sections }
        val sectionsFromEnrollments = filteredEnrollments.map { it.sectionName }.distinct()
        val availableSections = (sectionsFromSubjects + sectionsFromEnrollments).distinct().sorted()
        
        _uiState.value = currentState.copy(
            availableSubjects = availableSubjectNames,
            availableSections = availableSections
        )
    }
    
    private fun applyFilters() {
        // Use cached data for fast in-memory filtering - no database calls!
        val currentState = _uiState.value
        var filteredAggregates = _allGradeAggregates.value
        val allEnrollments = _cachedEnrollments.value
        val subjects = _cachedSubjects.value
        
        // Filter by course - match by course ID for accurate filtering
        if (currentState.selectedCourse != null) {
            // Find the course ID from the selected course string (format: "CODE - NAME")
            val selectedCourseCode = currentState.selectedCourse.split(" - ").getOrNull(0) ?: ""
            val selectedCourseName = currentState.selectedCourse.split(" - ").getOrNull(1) ?: currentState.selectedCourse
            
            val matchingCourse = _cachedCourses.value.find { course ->
                "${course.code} - ${course.name}" == currentState.selectedCourse ||
                course.code == selectedCourseCode ||
                course.name == selectedCourseName
            }
            
            if (matchingCourse != null) {
                // Filter enrollments by courseId (most accurate)
                val matchingEnrollments = allEnrollments.filter { 
                    it.courseId == matchingCourse.id ||
                    it.courseName == matchingCourse.name
                }
                val matchingSubjectIds = matchingEnrollments.map { it.subjectId }.distinct().toSet()
                filteredAggregates = filteredAggregates.filter { 
                    it.subjectId in matchingSubjectIds 
                }
            } else {
                // Fallback: filter by name if course not found in cache
                val matchingEnrollments = allEnrollments.filter { 
                    it.courseName == selectedCourseName || 
                    it.courseName == currentState.selectedCourse
                }
                val matchingSubjectIds = matchingEnrollments.map { it.subjectId }.distinct().toSet()
                filteredAggregates = filteredAggregates.filter { 
                    it.subjectId in matchingSubjectIds 
                }
            }
        }
        
        // Filter by year level
        if (currentState.selectedYearLevel != null) {
            val matchingEnrollments = allEnrollments.filter { 
                it.yearLevelName == currentState.selectedYearLevel 
            }
            val matchingSubjectIds = matchingEnrollments.map { it.subjectId }.distinct().toSet()
            filteredAggregates = filteredAggregates.filter { 
                it.subjectId in matchingSubjectIds 
            }
        }
        
        // Filter by subject
        if (currentState.selectedSubject != null) {
            val matchingSubject = subjects.find { it.name == currentState.selectedSubject }
            if (matchingSubject != null) {
                filteredAggregates = filteredAggregates.filter { 
                    it.subjectId == matchingSubject.id 
                }
            }
        }
        
        // Filter by section
        if (currentState.selectedSection != null) {
            val matchingEnrollments = allEnrollments.filter { 
                it.sectionName == currentState.selectedSection 
            }
            val matchingStudentIds = matchingEnrollments.map { it.studentId }.distinct().toSet()
            filteredAggregates = filteredAggregates.filter { 
                it.studentId in matchingStudentIds 
            }
        }
        
        _gradeAggregates.value = filteredAggregates
        
        // Calculate statistics from filtered aggregates
        val totalStudents = filteredAggregates.size
        val atRiskStudents = filteredAggregates.count { it.status == GradeStatus.AT_RISK }
        val passingStudents = filteredAggregates.count { it.status == GradeStatus.PASSING }
        val failingStudents = filteredAggregates.count { it.status == GradeStatus.FAILING }
        
        _uiState.value = currentState.copy(
            isLoading = false,
            totalStudents = totalStudents,
            atRiskStudents = atRiskStudents,
            passingStudents = passingStudents,
            failingStudents = failingStudents
        )
    }

    private fun createGradeAggregates(
        grades: List<Grade>,
        enrollments: List<com.smartacademictracker.data.model.StudentEnrollment>
    ): List<StudentGradeAggregate> {
        // Group grades by student and subject
        val groupedGrades = grades.groupBy { grade ->
            "${grade.studentId}_${grade.subjectId}"
        }

        return groupedGrades.mapNotNull { (key, studentGrades) ->
            val firstGrade = studentGrades.firstOrNull() ?: return@mapNotNull null
            val enrollment = enrollments.find {
                it.studentId == firstGrade.studentId && it.subjectId == firstGrade.subjectId
            }

            // Extract grades by period
            val prelimGrade = studentGrades.find { it.gradePeriod == GradePeriod.PRELIM }?.score
            val midtermGrade = studentGrades.find { it.gradePeriod == GradePeriod.MIDTERM }?.score
            val finalGrade = studentGrades.find { it.gradePeriod == GradePeriod.FINAL }?.score

            // Create aggregate
            StudentGradeAggregate(
                id = key,
                studentId = firstGrade.studentId,
                studentName = firstGrade.studentName,
                subjectId = firstGrade.subjectId,
                subjectName = firstGrade.subjectName,
                teacherId = firstGrade.teacherId,
                prelimGrade = prelimGrade,
                midtermGrade = midtermGrade,
                finalGrade = finalGrade,
                finalAverage = null, // Will be calculated
                status = GradeStatus.INCOMPLETE, // Will be determined
                letterGrade = "",
                semester = firstGrade.semester,
                academicYear = firstGrade.academicYear,
                lastUpdated = studentGrades.maxOfOrNull { it.dateRecorded } ?: System.currentTimeMillis()
            ).let { aggregate ->
                // Calculate final average and status
                val finalAverage = aggregate.calculateFinalAverage()
                val status = aggregate.determineGradeStatus()
                val letterGrade = aggregate.calculateLetterGrade()

                aggregate.copy(
                    finalAverage = finalAverage,
                    status = status,
                    letterGrade = letterGrade
                )
            }
        }
    }

    fun refreshGradeData() {
        loadGradeData()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class AdminGradeMonitoringUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalStudents: Int = 0,
    val atRiskStudents: Int = 0,
    val passingStudents: Int = 0,
    val failingStudents: Int = 0,
    // Filter properties
    val selectedYearLevel: String? = null,
    val selectedCourse: String? = null,
    val selectedSubject: String? = null,
    val selectedSection: String? = null,
    val availableYearLevels: List<String> = emptyList(),
    val availableCourses: List<String> = emptyList(),
    val availableSubjects: List<String> = emptyList(),
    val availableSections: List<String> = emptyList()
)