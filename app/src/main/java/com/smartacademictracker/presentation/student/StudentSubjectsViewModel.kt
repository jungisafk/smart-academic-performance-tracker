package com.smartacademictracker.presentation.student

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.StudentEnrollment
import com.smartacademictracker.data.repository.StudentEnrollmentRepository
import com.smartacademictracker.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentSubjectsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val studentEnrollmentRepository: StudentEnrollmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentSubjectsUiState())
    val uiState: StateFlow<StudentSubjectsUiState> = _uiState.asStateFlow()

    private val _enrollments = MutableStateFlow<List<StudentEnrollment>>(emptyList())
    val enrollments: StateFlow<List<StudentEnrollment>> = _enrollments.asStateFlow()

    fun loadEnrollments() {
        viewModelScope.launch {
            Log.d("StudentSubjectsViewModel", "=== STARTING LOAD ENROLLMENTS ===")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Get current user
                Log.d("StudentSubjectsViewModel", "Step 1: Getting current user...")
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        Log.d("StudentSubjectsViewModel", "Step 2: User found - ID: ${user.id}, Name: ${user.firstName} ${user.lastName}, StudentId: ${user.studentId}")
                        
                        // Try loading from student_enrollments collection
                        Log.d("StudentSubjectsViewModel", "Step 3: Loading enrollments from student_enrollments collection...")
                        val enrollmentsResult = studentEnrollmentRepository.getEnrollmentsByStudent(user.id)
                        enrollmentsResult.onSuccess { enrollmentsList ->
                            Log.d("StudentSubjectsViewModel", "Step 4: Successfully loaded ${enrollmentsList.size} enrollments")
                            enrollmentsList.forEachIndexed { index, enrollment ->
                                Log.d("StudentSubjectsViewModel", "  Enrollment[$index]: ID=${enrollment.id}, Subject=${enrollment.subjectName} (${enrollment.subjectCode}), Section=${enrollment.sectionName}, Status=${enrollment.status}")
                            }
                            _enrollments.value = enrollmentsList
                            _uiState.value = _uiState.value.copy(isLoading = false)
                            Log.d("StudentSubjectsViewModel", "Step 5: UI State updated - isLoading=false, enrollments.size=${enrollmentsList.size}")
                        }.onFailure { exception ->
                            Log.e("StudentSubjectsViewModel", "Step 4 ERROR: Failed to load enrollments - ${exception.message}", exception)
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to load enrollments"
                            )
                        }
                    } else {
                        Log.e("StudentSubjectsViewModel", "Step 2 ERROR: User is null")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "User not found"
                        )
                    }
                }.onFailure { exception ->
                    Log.e("StudentSubjectsViewModel", "Step 1 ERROR: Failed to get current user - ${exception.message}", exception)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load user data"
                    )
                }
            } catch (e: Exception) {
                Log.e("StudentSubjectsViewModel", "EXCEPTION in loadEnrollments: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load enrollments"
                )
            }
            Log.d("StudentSubjectsViewModel", "=== FINISHED LOAD ENROLLMENTS ===")
        }
    }

    fun refreshEnrollments() {
        Log.d("StudentSubjectsViewModel", "Refreshing enrollments...")
        loadEnrollments()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class StudentSubjectsUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
