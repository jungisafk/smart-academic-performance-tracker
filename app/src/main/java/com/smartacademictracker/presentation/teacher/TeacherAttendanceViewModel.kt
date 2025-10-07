package com.smartacademictracker.presentation.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.Attendance
import com.smartacademictracker.data.model.AttendanceStatus
import com.smartacademictracker.data.model.AttendanceSummary
import com.smartacademictracker.data.model.SessionType
import com.smartacademictracker.data.repository.AttendanceRepository
import com.smartacademictracker.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeacherAttendanceViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherAttendanceUiState())
    val uiState: StateFlow<TeacherAttendanceUiState> = _uiState.asStateFlow()

    fun loadAttendanceSummary(subjectId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = attendanceRepository.getAttendanceSummary(subjectId)
                result.onSuccess { summaries ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        attendanceSummaries = summaries,
                        currentSubjectId = subjectId
                    )
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load attendance"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load attendance"
                )
            }
        }
    }

    fun loadAttendanceByDate(subjectId: String, date: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = attendanceRepository.getAttendanceByDate(subjectId, date)
                result.onSuccess { attendance ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        dailyAttendance = attendance,
                        selectedDate = date
                    )
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load daily attendance"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load daily attendance"
                )
            }
        }
    }

    fun recordAttendance(
        studentId: String,
        studentName: String,
        subjectId: String,
        subjectName: String,
        status: AttendanceStatus,
        sessionType: SessionType = SessionType.REGULAR,
        notes: String = ""
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val currentUserResult = userRepository.getCurrentUser()
                currentUserResult.onSuccess { currentUser ->
                    if (currentUser == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "User not found"
                        )
                        return@onSuccess
                    }
                    
                    val attendance = Attendance(
                        studentId = studentId,
                        studentName = studentName,
                        subjectId = subjectId,
                        subjectName = subjectName,
                        teacherId = currentUser.id,
                        teacherName = "${currentUser.firstName} ${currentUser.lastName}",
                        status = status,
                        sessionType = sessionType,
                        notes = notes
                    )
                    
                    val result = attendanceRepository.recordAttendance(attendance)
                    result.onSuccess {
                        // Reload attendance summary
                        loadAttendanceSummary(subjectId)
                    }.onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to record attendance"
                        )
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to get current user"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to record attendance"
                )
            }
        }
    }

    fun updateAttendanceStatus(
        attendanceId: String,
        status: AttendanceStatus,
        notes: String = ""
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = attendanceRepository.updateAttendanceStatus(
                    attendanceId = attendanceId,
                    status = status,
                    notes = notes
                )
                
                result.onSuccess {
                    // Reload attendance data
                    val currentSubjectId = _uiState.value.currentSubjectId
                    if (currentSubjectId.isNotEmpty()) {
                        loadAttendanceSummary(currentSubjectId)
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to update attendance"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to update attendance"
                )
            }
        }
    }

    fun bulkRecordAttendance(
        subjectId: String,
        date: Long,
        attendanceRecords: List<Attendance>
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = attendanceRepository.bulkRecordAttendance(
                    subjectId = subjectId,
                    date = date,
                    attendanceRecords = attendanceRecords
                )
                
                result.onSuccess {
                    // Reload attendance summary
                    loadAttendanceSummary(subjectId)
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to record bulk attendance"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to record bulk attendance"
                )
            }
        }
    }

    fun filterAttendanceByStatus(status: AttendanceStatus?) {
        val currentSummaries = _uiState.value.allAttendanceSummaries
        val filteredSummaries = if (status != null) {
            currentSummaries.filter { summary ->
                when (status) {
                    AttendanceStatus.PRESENT -> summary.attendanceRate >= 80.0
                    AttendanceStatus.ABSENT -> summary.attendanceRate < 50.0
                    AttendanceStatus.LATE -> summary.lateCount > 0
                    AttendanceStatus.EXCUSED -> summary.excusedCount > 0
                    AttendanceStatus.TARDY -> summary.lateCount > 0
                }
            }
        } else {
            currentSummaries
        }
        
        _uiState.value = _uiState.value.copy(
            attendanceSummaries = filteredSummaries,
            selectedFilter = status
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class TeacherAttendanceUiState(
    val isLoading: Boolean = false,
    val attendanceSummaries: List<AttendanceSummary> = emptyList(),
    val allAttendanceSummaries: List<AttendanceSummary> = emptyList(),
    val dailyAttendance: List<Attendance> = emptyList(),
    val selectedDate: Long = System.currentTimeMillis(),
    val selectedFilter: AttendanceStatus? = null,
    val currentSubjectId: String = "",
    val error: String? = null
)
