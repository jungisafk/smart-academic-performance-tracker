package com.smartacademictracker.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.Course
import com.smartacademictracker.data.repository.CourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminCourseManagementViewModel @Inject constructor(
    private val courseRepository: CourseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminCourseManagementUiState())
    val uiState: StateFlow<AdminCourseManagementUiState> = _uiState.asStateFlow()

    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()

    fun loadCourses() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val coursesResult = courseRepository.getAllCourses()
                coursesResult.onSuccess { coursesList ->
                    _courses.value = coursesList
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    println("DEBUG: AdminCourseManagementViewModel - Loaded ${coursesList.size} courses")
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load courses"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load courses"
                )
            }
        }
    }

    fun deleteCourse(courseId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                deletingCourses = _uiState.value.deletingCourses + courseId,
                error = null
            )
            
            try {
                val deleteResult = courseRepository.deleteCourse(courseId)
                deleteResult.onSuccess {
                    _uiState.value = _uiState.value.copy(
                        deletingCourses = _uiState.value.deletingCourses - courseId
                    )
                    // Reload courses to update UI
                    loadCourses()
                    println("DEBUG: AdminCourseManagementViewModel - Course deleted successfully")
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        deletingCourses = _uiState.value.deletingCourses - courseId,
                        error = exception.message ?: "Failed to delete course"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    deletingCourses = _uiState.value.deletingCourses - courseId,
                    error = e.message ?: "Failed to delete course"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class AdminCourseManagementUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val deletingCourses: Set<String> = emptySet()
)
