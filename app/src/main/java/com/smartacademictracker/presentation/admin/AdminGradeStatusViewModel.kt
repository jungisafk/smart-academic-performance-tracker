package com.smartacademictracker.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.repository.GradeRepository
import com.smartacademictracker.data.repository.StudentEnrollmentRepository
import com.smartacademictracker.data.repository.SubjectRepository
import com.smartacademictracker.data.repository.CourseRepository
import com.smartacademictracker.data.manager.AdminDataCache
import com.smartacademictracker.data.model.Grade
import com.smartacademictracker.data.model.GradePeriod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminGradeStatusViewModel @Inject constructor(
    private val gradeRepository: GradeRepository,
    private val studentEnrollmentRepository: StudentEnrollmentRepository,
    private val subjectRepository: SubjectRepository,
    private val courseRepository: CourseRepository,
    private val adminDataCache: AdminDataCache
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminGradeStatusUiState())
    val uiState: StateFlow<AdminGradeStatusUiState> = _uiState.asStateFlow()

    private val _gradeStatuses = MutableStateFlow<List<SubjectSectionGradeStatus>>(emptyList())
    val gradeStatuses: StateFlow<List<SubjectSectionGradeStatus>> = _gradeStatuses.asStateFlow()

    init {
        // Load cached data immediately if available
        val cachedEnrollments = adminDataCache.cachedEnrollments.value
        val cachedSubjects = adminDataCache.cachedSubjects.value
        val cachedCourses = adminDataCache.cachedCourses.value
        
        if (cachedEnrollments.isNotEmpty() && cachedSubjects.isNotEmpty() && 
            cachedCourses.isNotEmpty() && adminDataCache.isCacheValid()) {
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun loadGradeStatus(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            // Load cached data first if available and not forcing refresh
            if (!forceRefresh && adminDataCache.cachedEnrollments.value.isNotEmpty() && 
                adminDataCache.cachedSubjects.value.isNotEmpty() &&
                adminDataCache.cachedCourses.value.isNotEmpty() &&
                adminDataCache.isCacheValid()) {
                // Only show loading if we don't have cached data
                if (_gradeStatuses.value.isEmpty()) {
                    _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                }
            } else {
                // Only show loading if we don't have cached data
                if (adminDataCache.cachedEnrollments.value.isEmpty() || 
                    adminDataCache.cachedSubjects.value.isEmpty() ||
                    adminDataCache.cachedCourses.value.isEmpty()) {
                    _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                }
            }

            try {
                // Load all data
                val enrollmentsResult = studentEnrollmentRepository.getAllActiveEnrollments()
                val gradesResult = gradeRepository.getAllGrades()
                val subjectsResult = subjectRepository.getAllSubjects()
                val coursesResult = courseRepository.getAllCourses()

                enrollmentsResult.onSuccess { enrollments ->
                    gradesResult.onSuccess { grades ->
                        subjectsResult.onSuccess { subjects ->
                            coursesResult.onSuccess { courses ->
                                // Update cache
                                adminDataCache.updateEnrollments(enrollments)
                                adminDataCache.updateSubjects(subjects)
                                adminDataCache.updateCourses(courses)

                                // Calculate grade statuses
                                val statuses = calculateGradeStatuses(
                                    enrollments = enrollments,
                                    grades = grades,
                                    subjects = subjects,
                                    courses = courses
                                )

                                _gradeStatuses.value = statuses
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = null,
                                    totalSections = statuses.size,
                                    completedSections = statuses.count { it.completionPercentage == 100 },
                                    incompleteSections = statuses.count { it.completionPercentage < 100 }
                                )
                            }.onFailure { exception ->
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = exception.message ?: "Failed to load courses"
                                )
                            }
                        }.onFailure { exception ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to load subjects"
                            )
                        }
                    }.onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load grades"
                        )
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load enrollments"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load grade status data"
                )
            }
        }
    }

    private fun calculateGradeStatuses(
        enrollments: List<com.smartacademictracker.data.model.StudentEnrollment>,
        grades: List<Grade>,
        subjects: List<com.smartacademictracker.data.model.Subject>,
        courses: List<com.smartacademictracker.data.model.Course>
    ): List<SubjectSectionGradeStatus> {
        // Group enrollments by subject and section
        val enrollmentsBySubjectSection = enrollments.groupBy { 
            "${it.subjectId}_${it.sectionName}" 
        }

        // Group grades by student and subject
        val gradesByStudentSubject = grades.groupBy { 
            "${it.studentId}_${it.subjectId}" 
        }

        val statuses = mutableListOf<SubjectSectionGradeStatus>()

        enrollmentsBySubjectSection.forEach { (key, sectionEnrollments) ->
            val firstEnrollment = sectionEnrollments.first()
            val subject = subjects.find { it.id == firstEnrollment.subjectId }
            val course = courses.find { it.id == firstEnrollment.courseId }

            if (subject != null) {
                // Check if this is a minor subject (no courseId or subjectType is MINOR)
                val isMinorSubject = subject.courseId.isEmpty() || subject.subjectType == com.smartacademictracker.data.model.SubjectType.MINOR
                
                // For minor subjects, we don't need a course
                // For major subjects, we need a course
                if (isMinorSubject || course != null) {
                    // Count total students in this section
                    val totalStudents = sectionEnrollments.size

                    // Count students with complete grades (all 3 periods)
                    var studentsWithCompleteGrades = 0
                    var studentsWithPartialGrades = 0

                    sectionEnrollments.forEach { enrollment ->
                        val studentGrades = gradesByStudentSubject["${enrollment.studentId}_${enrollment.subjectId}"] 
                            ?: emptyList()
                        
                        val hasPrelim = studentGrades.any { it.gradePeriod == GradePeriod.PRELIM }
                        val hasMidterm = studentGrades.any { it.gradePeriod == GradePeriod.MIDTERM }
                        val hasFinal = studentGrades.any { it.gradePeriod == GradePeriod.FINAL }

                        when {
                            hasPrelim && hasMidterm && hasFinal -> studentsWithCompleteGrades++
                            hasPrelim || hasMidterm || hasFinal -> studentsWithPartialGrades++
                        }
                    }

                    val completionPercentage = if (totalStudents > 0) {
                        ((studentsWithCompleteGrades.toDouble() / totalStudents) * 100).toInt()
                    } else {
                        0
                    }

                    val status = when {
                        completionPercentage == 100 -> GradeCompletionStatus.COMPLETE
                        completionPercentage > 0 -> GradeCompletionStatus.PARTIAL
                        else -> GradeCompletionStatus.INCOMPLETE
                    }

                    statuses.add(
                        SubjectSectionGradeStatus(
                            subjectId = firstEnrollment.subjectId,
                            subjectName = firstEnrollment.subjectName,
                            subjectCode = firstEnrollment.subjectCode,
                            sectionName = firstEnrollment.sectionName,
                            courseId = firstEnrollment.courseId,
                            courseName = if (isMinorSubject) "" else (course?.name ?: ""),
                            courseCode = if (isMinorSubject) "" else (course?.code ?: ""),
                            teacherId = firstEnrollment.teacherId,
                            teacherName = firstEnrollment.teacherName,
                            isMinorSubject = isMinorSubject,
                            totalStudents = totalStudents,
                            studentsWithCompleteGrades = studentsWithCompleteGrades,
                            studentsWithPartialGrades = studentsWithPartialGrades,
                            studentsWithNoGrades = totalStudents - studentsWithCompleteGrades - studentsWithPartialGrades,
                            completionPercentage = completionPercentage,
                            status = status
                        )
                    )
                }
            }
        }

        // Sort: minor subjects first, then by course name, then subject name, then section name
        return statuses.sortedWith(
            compareBy<SubjectSectionGradeStatus>(
                { !it.isMinorSubject } // Minor subjects first (false < true)
            ).thenBy { it.courseName }
             .thenBy { it.subjectName }
             .thenBy { it.sectionName }
        )
    }

    fun refreshGradeStatus() {
        loadGradeStatus(forceRefresh = true)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class AdminGradeStatusUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalSections: Int = 0,
    val completedSections: Int = 0,
    val incompleteSections: Int = 0
)

data class SubjectSectionGradeStatus(
    val subjectId: String,
    val subjectName: String,
    val subjectCode: String,
    val sectionName: String,
    val courseId: String,
    val courseName: String,
    val courseCode: String,
    val teacherId: String,
    val teacherName: String,
    val isMinorSubject: Boolean = false,
    val totalStudents: Int,
    val studentsWithCompleteGrades: Int,
    val studentsWithPartialGrades: Int,
    val studentsWithNoGrades: Int,
    val completionPercentage: Int,
    val status: GradeCompletionStatus
)

enum class GradeCompletionStatus {
    COMPLETE,
    PARTIAL,
    INCOMPLETE
}

