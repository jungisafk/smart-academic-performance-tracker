package com.smartacademictracker.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.Course
import com.smartacademictracker.data.model.YearLevel
import com.smartacademictracker.data.model.User
import com.smartacademictracker.data.model.UserRole
import com.smartacademictracker.data.repository.CourseRepository
import com.smartacademictracker.data.repository.YearLevelRepository
import com.smartacademictracker.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val courseRepository: CourseRepository,
    private val yearLevelRepository: YearLevelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        // Don't load courses immediately - only load when student role is selected
    }

    fun loadCourses() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingCourses = true, error = null)
            try {
                val result = courseRepository.getAllCourses()
                result.onSuccess { courses ->
                    _uiState.value = _uiState.value.copy(
                        courses = courses,
                        isLoadingCourses = false
                    )
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingCourses = false,
                        error = exception.message ?: "Failed to load courses"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingCourses = false,
                    error = e.message ?: "Failed to load courses"
                )
            }
        }
    }

    fun loadYearLevelsForCourse(courseId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingYearLevels = true, error = null)
            try {
                val result = yearLevelRepository.getYearLevelsByCourse(courseId)
                result.onSuccess { yearLevels ->
                    _uiState.value = _uiState.value.copy(
                        yearLevels = yearLevels,
                        isLoadingYearLevels = false
                    )
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingYearLevels = false,
                        error = exception.message ?: "Failed to load year levels"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingYearLevels = false,
                    error = e.message ?: "Failed to load year levels"
                )
            }
        }
    }

    fun signUp(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        role: UserRole,
        courseId: String? = null,
        yearLevelId: String? = null,
        departmentCourseId: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = userRepository.createUser(
                    email, password, firstName, lastName, role, 
                    courseId, yearLevelId, departmentCourseId
                )
                result.onSuccess { user ->
                    _currentUser.value = user
                    _uiState.value = _uiState.value.copy(isLoading = false, isSignUpSuccess = true)
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Sign up failed"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Sign up failed"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSignUpSuccess() {
        _uiState.value = _uiState.value.copy(isSignUpSuccess = false)
    }

    fun clearCourseData() {
        _uiState.value = _uiState.value.copy(
            courses = emptyList(),
            yearLevels = emptyList(),
            isLoadingCourses = false,
            isLoadingYearLevels = false,
            error = null
        )
    }
}

data class SignUpUiState(
    val isLoading: Boolean = false,
    val isSignUpSuccess: Boolean = false,
    val isLoadingCourses: Boolean = false,
    val isLoadingYearLevels: Boolean = false,
    val courses: List<Course> = emptyList(),
    val yearLevels: List<YearLevel> = emptyList(),
    val error: String? = null
)
