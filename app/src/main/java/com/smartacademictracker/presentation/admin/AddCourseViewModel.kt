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
class AddCourseViewModel @Inject constructor(
    private val courseRepository: CourseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddCourseUiState())
    val uiState: StateFlow<AddCourseUiState> = _uiState.asStateFlow()

    fun setCourseName(name: String) {
        _uiState.value = _uiState.value.copy(
            courseName = name,
            courseNameError = null
        )
    }

    fun setCourseCode(code: String) {
        _uiState.value = _uiState.value.copy(
            courseCode = code,
            courseCodeError = null
        )
    }

    fun setDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun setDuration(duration: Int) {
        _uiState.value = _uiState.value.copy(
            duration = duration,
            durationError = null
        )
    }

    fun addCourse() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            // Validate input
            val validationResult = validateInput()
            if (validationResult != null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = validationResult
                )
                return@launch
            }
            
            try {
                val course = Course(
                    name = _uiState.value.courseName.trim(),
                    code = _uiState.value.courseCode.trim().uppercase(),
                    description = _uiState.value.description.trim(),
                    duration = _uiState.value.duration
                )
                
                val createResult = courseRepository.createCourse(course)
                createResult.onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true
                    )
                    println("DEBUG: AddCourseViewModel - Course created successfully")
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to create course"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to create course"
                )
            }
        }
    }

    private fun validateInput(): String? {
        val state = _uiState.value
        
        if (state.courseName.isBlank()) {
            _uiState.value = _uiState.value.copy(courseNameError = "Course name is required")
            return "Please fill in all required fields"
        }
        
        if (state.courseCode.isBlank()) {
            _uiState.value = _uiState.value.copy(courseCodeError = "Course code is required")
            return "Please fill in all required fields"
        }
        
        if (state.duration < 1 || state.duration > 10) {
            _uiState.value = _uiState.value.copy(durationError = "Duration must be between 1 and 10 years")
            return "Please enter a valid duration"
        }
        
        return null
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class AddCourseUiState(
    val courseName: String = "",
    val courseCode: String = "",
    val description: String = "",
    val duration: Int = 4,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val courseNameError: String? = null,
    val courseCodeError: String? = null,
    val durationError: String? = null
)
