package com.smartacademictracker.presentation.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.StudentGradeAggregate
import com.smartacademictracker.data.model.GradeStatus
import com.smartacademictracker.data.model.Grade
import com.smartacademictracker.data.repository.GradeRepository
import com.smartacademictracker.data.repository.UserRepository
import com.smartacademictracker.data.repository.SubjectRepository
import com.smartacademictracker.data.manager.TeacherDataCache
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeacherAnalyticsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val gradeRepository: GradeRepository,
    private val subjectRepository: SubjectRepository,
    private val teacherDataCache: TeacherDataCache
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherAnalyticsUiState())
    val uiState: StateFlow<TeacherAnalyticsUiState> = _uiState.asStateFlow()

    private val _classPerformance = MutableStateFlow<List<SubjectPerformanceData>>(emptyList())
    val classPerformance: StateFlow<List<SubjectPerformanceData>> = _classPerformance.asStateFlow()

    fun loadAnalyticsData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            // Prevent duplicate loading
            if (_uiState.value.isLoading && !forceRefresh) return@launch
            
            // Check cache first
            val cachedSubjects = teacherDataCache.cachedSubjects.value
            if (!forceRefresh && cachedSubjects.isNotEmpty() && teacherDataCache.isCacheValid()) {
                // Load filter options from cached subjects immediately
                loadFilterOptions(cachedSubjects)
                // Try to load performance data from cache if available
                // Note: Analytics data is computed, so we still need to process it
                // But we can show loading only if no cached data exists
                if (_classPerformance.value.isEmpty()) {
                    _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                }
            } else {
                if (cachedSubjects.isEmpty() || !teacherDataCache.isCacheValid()) {
                    _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                }
            }
            
            try {
                // Get current user (teacher)
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        // Get subjects taught by this teacher
                        val subjectsResult = subjectRepository.getSubjectsByTeacher(user.id)
                        subjectsResult.onSuccess { subjects ->
                            // Update cache
                            teacherDataCache.updateSubjects(subjects)
                            // Load available filter options
                            loadFilterOptions(subjects)
                            // Load grade aggregates for all subjects
                            loadClassPerformanceData(subjects)
                        }.onFailure { exception ->
                            // If cache exists, use it; otherwise show error
                            if (cachedSubjects.isNotEmpty() && teacherDataCache.isCacheValid()) {
                                loadFilterOptions(cachedSubjects)
                                loadClassPerformanceData(cachedSubjects)
                            } else {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = sanitizeFirebaseError(exception.message ?: "Failed to load subjects")
                                )
                            }
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "User not found"
                        )
                    }
                }.onFailure { exception ->
                    // If cache exists, use it; otherwise show error
                    if (cachedSubjects.isNotEmpty() && teacherDataCache.isCacheValid()) {
                        loadFilterOptions(cachedSubjects)
                        loadClassPerformanceData(cachedSubjects)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = sanitizeFirebaseError(exception.message ?: "Failed to load user data")
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = sanitizeFirebaseError(e.message ?: "Failed to load analytics data")
                )
            }
        }
    }

    private suspend fun loadClassPerformanceData(subjects: List<com.smartacademictracker.data.model.Subject>) {
        val subjectPerformanceList = mutableListOf<SubjectPerformanceData>()
        var totalStudents = 0
        var totalPassingStudents = 0
        var totalAtRiskStudents = 0
        val allGrades = mutableListOf<Double>()
        var hasError = false
        var errorMessage: String? = null

        for (subject in subjects) {
            // Try to get existing aggregates first
            val aggregatesResult = gradeRepository.getStudentGradeAggregatesBySubject(subject.id)
            aggregatesResult.onSuccess { aggregates ->
                val validAggregates = aggregates.filter { it.finalAverage != null }
                if (validAggregates.isNotEmpty()) {
                    // Use existing aggregates
                    val result = processSubjectPerformance(subject, validAggregates)
                    if (result != null) {
                        subjectPerformanceList.add(result)
                        totalStudents += result.totalStudents
                        val passingCount = validAggregates.count { it.status == GradeStatus.PASSING }
                        val atRiskCount = validAggregates.count { it.status == GradeStatus.AT_RISK }
                        totalPassingStudents += passingCount
                        totalAtRiskStudents += atRiskCount
                        allGrades.addAll(validAggregates.mapNotNull { it.finalAverage })
                    }
                } else {
                    // No aggregates exist, try to get individual grades
                    val gradesResult = gradeRepository.getGradesBySubject(subject.id)
                    gradesResult.onSuccess { grades ->
                        if (grades.isNotEmpty()) {
                            // Group grades by student and create aggregates
                            val gradesByStudent = grades.groupBy { it.studentId }
                            val studentAggregates = mutableListOf<StudentGradeAggregate>()
                            
                            for ((studentId, studentGrades) in gradesByStudent) {
                                val aggregate = createAggregateFromGrades(studentId, subject, studentGrades)
                                if (aggregate != null) {
                                    studentAggregates.add(aggregate)
                                }
                            }
                            
                            if (studentAggregates.isNotEmpty()) {
                                val result = processSubjectPerformance(subject, studentAggregates)
                                if (result != null) {
                                    subjectPerformanceList.add(result)
                                    totalStudents += result.totalStudents
                                    val passingCount = studentAggregates.count { it.status == GradeStatus.PASSING }
                                    val atRiskCount = studentAggregates.count { it.status == GradeStatus.AT_RISK }
                                    totalPassingStudents += passingCount
                                    totalAtRiskStudents += atRiskCount
                                    allGrades.addAll(studentAggregates.mapNotNull { it.finalAverage })
                                }
                            }
                        }
                    }
                }
            }.onFailure { exception ->
                val sanitizedError = sanitizeFirebaseError(exception.message)
                if (!sanitizedError.contains("Unable to load") && !sanitizedError.contains("permission")) {
                    // Only set error if it's not a composite index or permission error (those are handled globally)
                    hasError = true
                    errorMessage = sanitizedError
                }
                // Try individual grades as fallback
                val gradesResult = gradeRepository.getGradesBySubject(subject.id)
                gradesResult.onSuccess { grades ->
                    if (grades.isNotEmpty()) {
                        val gradesByStudent = grades.groupBy { it.studentId }
                        val studentAggregates = mutableListOf<StudentGradeAggregate>()
                        
                        for ((studentId, studentGrades) in gradesByStudent) {
                            val aggregate = createAggregateFromGrades(studentId, subject, studentGrades)
                            if (aggregate != null) {
                                studentAggregates.add(aggregate)
                            }
                        }
                        
                        if (studentAggregates.isNotEmpty()) {
                            val result = processSubjectPerformance(subject, studentAggregates)
                            if (result != null) {
                                subjectPerformanceList.add(result)
                                totalStudents += result.totalStudents
                                val passingCount = studentAggregates.count { it.status == GradeStatus.PASSING }
                                val atRiskCount = studentAggregates.count { it.status == GradeStatus.AT_RISK }
                                totalPassingStudents += passingCount
                                totalAtRiskStudents += atRiskCount
                                allGrades.addAll(studentAggregates.mapNotNull { it.finalAverage })
                            }
                        }
                    }
                }.onFailure { fallbackException ->
                    // If fallback also fails, log but don't block the entire operation
                    if (!hasError) {
                        hasError = true
                        errorMessage = sanitizeFirebaseError(fallbackException.message)
                    }
                }
            }
        }

        _classPerformance.value = subjectPerformanceList
        
        val classAverage = if (allGrades.isNotEmpty()) allGrades.average() else null
        
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            totalStudents = totalStudents,
            classAverage = classAverage,
            passingStudents = totalPassingStudents,
            atRiskStudents = totalAtRiskStudents,
            error = if (hasError && subjectPerformanceList.isEmpty()) errorMessage else null
        )
        
    }


    private fun processSubjectPerformance(
        subject: com.smartacademictracker.data.model.Subject,
        aggregates: List<StudentGradeAggregate>
    ): SubjectPerformanceData? {
        if (aggregates.isEmpty()) return null
        
        val averageGrade = aggregates.mapNotNull { it.finalAverage }.average()
        val passingStudents = aggregates.count { it.status == GradeStatus.PASSING }
        val atRiskStudents = aggregates.count { it.status == GradeStatus.AT_RISK }
        val passingRate = (passingStudents.toDouble() / aggregates.size) * 100
        
        val gradeDistribution = calculateGradeDistribution(aggregates)
        
        return SubjectPerformanceData(
            subjectName = subject.name,
            totalStudents = aggregates.size,
            averageGrade = averageGrade,
            passingRate = passingRate,
            gradeDistribution = gradeDistribution
        )
    }


    private fun createAggregateFromGrades(
        studentId: String,
        subject: com.smartacademictracker.data.model.Subject,
        grades: List<Grade>
    ): StudentGradeAggregate? {
        if (grades.isEmpty()) return null
        
        // Get period grades
        val prelimGrade = grades.find { it.gradePeriod == com.smartacademictracker.data.model.GradePeriod.PRELIM }
        val midtermGrade = grades.find { it.gradePeriod == com.smartacademictracker.data.model.GradePeriod.MIDTERM }
        val finalGrade = grades.find { it.gradePeriod == com.smartacademictracker.data.model.GradePeriod.FINAL }
        
        // Calculate final average using weighted formula (30% prelim, 30% midterm, 40% final)
        val finalAverage = com.smartacademictracker.data.utils.GradeCalculationEngine.calculateFinalAverage(
            prelimGrade?.percentage,
            midtermGrade?.percentage,
            finalGrade?.percentage
        )
        
        if (finalAverage == null) return null
        val status = when {
            finalAverage >= 90 -> GradeStatus.PASSING
            finalAverage >= 80 -> GradeStatus.PASSING
            finalAverage >= 70 -> GradeStatus.AT_RISK
            finalAverage > 0 -> GradeStatus.FAILING
            else -> GradeStatus.INCOMPLETE
        }
        
        return StudentGradeAggregate(
            id = "${studentId}_${subject.id}_${grades.first().semester}_${grades.first().academicYear}",
            studentId = studentId,
            studentName = grades.first().studentName,
            prelimGrade = prelimGrade?.percentage,
            midtermGrade = midtermGrade?.percentage,
            finalGrade = finalGrade?.percentage,
            subjectId = subject.id,
            subjectName = subject.name,
            teacherId = subject.teacherId ?: "",
            semester = grades.first().semester,
            academicYear = grades.first().academicYear,
            finalAverage = finalAverage,
            status = status,
            lastUpdated = System.currentTimeMillis()
        )
    }

    private fun calculateGradeDistribution(aggregates: List<StudentGradeAggregate>): Map<String, Int> {
        val distribution = mutableMapOf<String, Int>()
        
        aggregates.forEach { aggregate ->
            val grade = aggregate.finalAverage ?: return@forEach
            val gradeRange = when {
                grade >= 75 -> "75+"
                else -> "<75"
            }
            distribution[gradeRange] = (distribution[gradeRange] ?: 0) + 1
        }
        
        return distribution
    }

    fun refreshData() {
        loadAnalyticsData(forceRefresh = true)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun updateYearLevelFilter(yearLevel: String?) {
        _uiState.value = _uiState.value.copy(selectedYearLevel = yearLevel)
        loadFilteredAnalyticsData()
    }

    fun updateCourseFilter(course: String?) {
        _uiState.value = _uiState.value.copy(selectedCourse = course)
        loadFilteredAnalyticsData()
    }

    fun updateSubjectFilter(subject: String?) {
        _uiState.value = _uiState.value.copy(selectedSubject = subject)
        loadFilteredAnalyticsData()
    }

    fun updateSectionFilter(section: String?) {
        _uiState.value = _uiState.value.copy(selectedSection = section)
        loadFilteredAnalyticsData()
    }

    fun clearAllFilters() {
        _uiState.value = _uiState.value.copy(
            selectedYearLevel = null,
            selectedCourse = null,
            selectedSubject = null,
            selectedSection = null
        )
        loadFilteredAnalyticsData()
    }

    private fun loadFilteredAnalyticsData() {
        // This will be called when filters change to reload data with applied filters
        loadAnalyticsData()
    }

    private fun loadFilterOptions(subjects: List<com.smartacademictracker.data.model.Subject>) {
        val yearLevels = subjects.mapNotNull { it.yearLevelName }.distinct().sorted()
        val courses = subjects.mapNotNull { it.courseName }.distinct().sorted()
        val subjectNames = subjects.map { it.name }.distinct().sorted()
        // Extract sections from all subjects
        val sections = subjects.flatMap { it.sections }.distinct().sorted()

        _uiState.value = _uiState.value.copy(
            availableYearLevels = yearLevels,
            availableCourses = courses,
            availableSubjects = subjectNames,
            availableSections = sections
        )
    }

    /**
     * Sanitizes Firebase error messages to provide user-friendly error messages.
     * Removes Firebase console URLs and technical details that are not helpful to users.
     */
    private fun sanitizeFirebaseError(errorMessage: String?): String {
        if (errorMessage == null) return "An error occurred"
        
        // Check if error contains Firebase console URL (composite index error)
        if (errorMessage.contains("console.firebase.google.com") || 
            errorMessage.contains("create_composite") ||
            errorMessage.contains("indexes?create_composite")) {
            return "Unable to load analytics data. Please try again later."
        }
        
        // Check for permission denied errors
        if (errorMessage.contains("PERMISSION_DENIED", ignoreCase = true) ||
            errorMessage.contains("permission denied", ignoreCase = true)) {
            return "You don't have permission to access this data."
        }
        
        // Check for network errors
        if (errorMessage.contains("network", ignoreCase = true) ||
            errorMessage.contains("unavailable", ignoreCase = true)) {
            return "Network error. Please check your connection and try again."
        }
        
        // Return original message if it's already user-friendly
        return errorMessage
    }
}

data class TeacherAnalyticsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalStudents: Int = 0,
    val classAverage: Double? = null,
    val passingStudents: Int = 0,
    val atRiskStudents: Int = 0,
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

data class SubjectPerformanceData(
    val subjectName: String,
    val totalStudents: Int,
    val averageGrade: Double,
    val passingRate: Double,
    val gradeDistribution: Map<String, Int>
)
