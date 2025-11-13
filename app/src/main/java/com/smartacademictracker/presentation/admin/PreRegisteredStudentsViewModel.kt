package com.smartacademictracker.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.PreRegisteredStudent
import com.smartacademictracker.data.repository.PreRegisteredRepository
import com.smartacademictracker.data.repository.CourseRepository
import com.smartacademictracker.data.repository.YearLevelRepository
import com.smartacademictracker.data.model.Course
import com.smartacademictracker.data.model.YearLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PreRegisteredStudentsViewModel @Inject constructor(
    private val preRegisteredRepository: PreRegisteredRepository,
    private val courseRepository: CourseRepository,
    private val yearLevelRepository: YearLevelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PreRegisteredStudentsUiState())
    val uiState: StateFlow<PreRegisteredStudentsUiState> = _uiState.asStateFlow()

    init {
        loadPreRegisteredStudents()
        loadCourses()
    }

    fun loadPreRegisteredStudents() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val result = preRegisteredRepository.getAllPreRegisteredStudents()
                result.onSuccess { students ->
                    _uiState.value = _uiState.value.copy(
                        students = students,
                        filteredStudents = students,
                        isLoading = false
                    )
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load students"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load students"
                )
            }
        }
    }

    fun loadCourses() {
        viewModelScope.launch {
            try {
                val result = courseRepository.getAllCourses()
                result.onSuccess { courses ->
                    _uiState.value = _uiState.value.copy(courses = courses)
                }
            } catch (e: Exception) {
                // Silently fail - not critical
            }
        }
    }

    fun loadYearLevels(courseId: String) {
        viewModelScope.launch {
            try {
                val result = yearLevelRepository.getYearLevelsByCourse(courseId)
                result.onSuccess { yearLevels ->
                    _uiState.value = _uiState.value.copy(yearLevels = yearLevels)
                }
            } catch (e: Exception) {
                // Silently fail - not critical
            }
        }
    }

    fun filterStudents(filter: StudentFilter) {
        val filtered = when (filter) {
            StudentFilter.ALL -> _uiState.value.students
            StudentFilter.REGISTERED -> _uiState.value.students.filter { it.isRegistered }
            StudentFilter.PENDING -> _uiState.value.students.filter { !it.isRegistered }
        }
        _uiState.value = _uiState.value.copy(
            filteredStudents = filtered,
            currentFilter = filter
        )
    }

    fun searchStudents(query: String) {
        val filtered = if (query.isBlank()) {
            when (_uiState.value.currentFilter) {
                StudentFilter.ALL -> _uiState.value.students
                StudentFilter.REGISTERED -> _uiState.value.students.filter { it.isRegistered }
                StudentFilter.PENDING -> _uiState.value.students.filter { !it.isRegistered }
            }
        } else {
            _uiState.value.students.filter {
                it.studentId.contains(query, ignoreCase = true) ||
                it.firstName.contains(query, ignoreCase = true) ||
                it.lastName.contains(query, ignoreCase = true) ||
                it.courseName.contains(query, ignoreCase = true)
            }
        }
        _uiState.value = _uiState.value.copy(filteredStudents = filtered)
    }

    fun addStudent(student: PreRegisteredStudent, adminId: String, adminName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val studentWithAdmin = student.copy(
                    createdBy = adminId,
                    createdByName = adminName
                )
                val result = preRegisteredRepository.addPreRegisteredStudent(studentWithAdmin)
                result.onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Student added successfully"
                    )
                    loadPreRegisteredStudents()
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to add student"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to add student"
                )
            }
        }
    }

    fun updateStudent(student: PreRegisteredStudent) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val result = preRegisteredRepository.updatePreRegisteredStudent(student)
                result.onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Student updated successfully"
                    )
                    loadPreRegisteredStudents()
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to update student"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to update student"
                )
            }
        }
    }

    fun deleteStudent(docId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val result = preRegisteredRepository.deletePreRegisteredStudent(docId)
                result.onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Student deleted successfully"
                    )
                    loadPreRegisteredStudents()
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to delete student"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to delete student"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            error = null,
            successMessage = null
        )
    }
}

data class PreRegisteredStudentsUiState(
    val students: List<PreRegisteredStudent> = emptyList(),
    val filteredStudents: List<PreRegisteredStudent> = emptyList(),
    val courses: List<Course> = emptyList(),
    val yearLevels: List<YearLevel> = emptyList(),
    val currentFilter: StudentFilter = StudentFilter.ALL,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

enum class StudentFilter {
    ALL,
    REGISTERED,
    PENDING
}

