package com.smartacademictracker.presentation.student

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.StudentEnrollment
import com.smartacademictracker.data.model.EnrollmentStatus
import com.smartacademictracker.data.repository.StudentEnrollmentRepository
import com.smartacademictracker.data.repository.UserRepository
import com.smartacademictracker.data.manager.StudentDataCache
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentSubjectsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val studentEnrollmentRepository: StudentEnrollmentRepository,
    private val studentDataCache: StudentDataCache
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentSubjectsUiState())
    val uiState: StateFlow<StudentSubjectsUiState> = _uiState.asStateFlow()

    private val _enrollments = MutableStateFlow<List<StudentEnrollment>>(emptyList())
    val enrollments: StateFlow<List<StudentEnrollment>> = _enrollments.asStateFlow()
    
    private var enrollmentFlowJob: kotlinx.coroutines.Job? = null

    init {
        // Load cached data immediately if available, but filter to only ACTIVE enrollments
        val cachedEnrollments = studentDataCache.cachedEnrollments.value
        if (cachedEnrollments.isNotEmpty() && studentDataCache.isCacheValid()) {
            // Filter to only show ACTIVE enrollments (in case cache has stale data)
            val activeEnrollments = cachedEnrollments.filter { it.status == EnrollmentStatus.ACTIVE }
            _enrollments.value = activeEnrollments
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun loadEnrollments(forceRefresh: Boolean = false) {
        // Cancel any existing flow collection
        enrollmentFlowJob?.cancel()
        
        viewModelScope.launch {
            // Check cache first
            val cachedEnrollments = studentDataCache.cachedEnrollments.value
            
            // Only show loading if no cached data or cache is invalid
            if (forceRefresh || !studentDataCache.isCacheValid() || cachedEnrollments.isEmpty()) {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }
            
            try {
                // Get current user
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { user ->
                    if (user != null) {
                        // Use cached data immediately if available and valid, but filter to ACTIVE only
                        if (!forceRefresh && studentDataCache.isCacheValid() && cachedEnrollments.isNotEmpty()) {
                            val activeCached = cachedEnrollments.filter { 
                                it.studentId == user.id && it.status == EnrollmentStatus.ACTIVE 
                            }
                            _enrollments.value = activeCached
                            _uiState.value = _uiState.value.copy(isLoading = false)
                        }
                        
                        // Set up real-time listener for enrollments
                        enrollmentFlowJob = studentEnrollmentRepository.getEnrollmentsByStudentFlow(user.id)
                            .catch { exception ->
                                Log.e("StudentSubjectsVM", "Real-time listener error: ${exception.message}", exception)
                                // If real-time listener fails, fall back to one-time fetch
                                if (!studentDataCache.isCacheValid()) {
                                    _uiState.value = _uiState.value.copy(
                                        isLoading = false,
                                        error = exception.message ?: "Failed to load enrollments"
                                    )
                                }
                                
                                // Try one-time fetch as fallback
                                val enrollmentsResult = studentEnrollmentRepository.getEnrollmentsByStudent(user.id)
                                enrollmentsResult.onSuccess { enrollmentsList ->
                                    _enrollments.value = enrollmentsList
                                    studentDataCache.updateEnrollments(enrollmentsList)
                                    _uiState.value = _uiState.value.copy(isLoading = false)
                                }.onFailure { fetchException ->
                                    Log.e("StudentSubjectsVM", "One-time fetch failed: ${fetchException.message}", fetchException)
                                }
                            }
                            .onEach { enrollmentsList ->
                                // Real-time update: filter to only ACTIVE enrollments
                                val activeEnrollments = enrollmentsList.filter { it.status == EnrollmentStatus.ACTIVE }
                                _enrollments.value = activeEnrollments
                                studentDataCache.updateEnrollments(activeEnrollments)
                                _uiState.value = _uiState.value.copy(isLoading = false)
                            }
                            .launchIn(this)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "User not found"
                        )
                    }
                }.onFailure { exception ->
                    // If cache exists, use it; otherwise show error
                    if (cachedEnrollments.isNotEmpty() && studentDataCache.isCacheValid()) {
                        val activeCached = cachedEnrollments.filter { it.status == EnrollmentStatus.ACTIVE }
                        _enrollments.value = activeCached
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load user data"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load enrollments"
                )
            }
        }
    }

    fun refreshEnrollments() {
        loadEnrollments(forceRefresh = true)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class StudentSubjectsUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
