package com.smartacademictracker.presentation.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.GradeExport
import com.smartacademictracker.data.model.ExportFormat
import com.smartacademictracker.data.model.ExportType
import com.smartacademictracker.data.repository.GradeExportRepository
import com.smartacademictracker.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeacherGradeExportViewModel @Inject constructor(
    private val exportRepository: GradeExportRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherGradeExportUiState())
    val uiState: StateFlow<TeacherGradeExportUiState> = _uiState.asStateFlow()

    fun generateExport(
        subjectId: String,
        exportType: ExportType,
        exportFormat: ExportFormat,
        academicYear: String,
        semester: String
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
                    
                    val result = when (exportType) {
                        ExportType.SUBJECT_GRADES -> {
                            exportRepository.generateSubjectGradeExport(
                                subjectId = subjectId,
                                academicYear = academicYear,
                                semester = semester
                            )
                        }
                        ExportType.CLASS_SUMMARY -> {
                            exportRepository.generateClassSummaryExport(
                                teacherId = currentUser.id,
                                academicYear = academicYear,
                                semester = semester
                            )
                        }
                        ExportType.INDIVIDUAL_REPORTS -> {
                            exportRepository.generateIndividualStudentReport(
                                studentId = subjectId, // Using subjectId as studentId for individual reports
                                academicYear = academicYear,
                                semester = semester
                            )
                        }
                        ExportType.COMPARATIVE_ANALYSIS -> {
                            exportRepository.generateSubjectGradeExport(
                                subjectId = subjectId,
                                academicYear = academicYear,
                                semester = semester
                            )
                        }
                    }
                    
                    result.onSuccess { exportData ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            exportData = exportData,
                            exportFormat = exportFormat,
                            exportType = exportType
                        )
                    }.onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to generate export"
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
                    error = e.message ?: "Failed to generate export"
                )
            }
        }
    }

    fun downloadExport() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDownloading = true, error = null)
            
            try {
                // Simulate download process
                kotlinx.coroutines.delay(2000)
                
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    downloadComplete = true,
                    downloadMessage = "Export downloaded successfully"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    error = e.message ?: "Failed to download export"
                )
            }
        }
    }

    fun emailExport() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isEmailing = true, error = null)
            
            try {
                // Simulate email process
                kotlinx.coroutines.delay(1500)
                
                _uiState.value = _uiState.value.copy(
                    isEmailing = false,
                    emailSent = true,
                    emailMessage = "Export sent via email successfully"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isEmailing = false,
                    error = e.message ?: "Failed to send email"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            downloadComplete = false,
            downloadMessage = null,
            emailSent = false,
            emailMessage = null,
            error = null
        )
    }

    fun setExportFormat(format: ExportFormat) {
        _uiState.value = _uiState.value.copy(selectedFormat = format)
    }

    fun setExportType(type: ExportType) {
        _uiState.value = _uiState.value.copy(selectedType = type)
    }
}

data class TeacherGradeExportUiState(
    val isLoading: Boolean = false,
    val isDownloading: Boolean = false,
    val isEmailing: Boolean = false,
    val exportData: Any? = null, // Can be GradeExport or List<GradeExport>
    val selectedFormat: ExportFormat = ExportFormat.EXCEL,
    val selectedType: ExportType = ExportType.SUBJECT_GRADES,
    val exportFormat: ExportFormat = ExportFormat.EXCEL,
    val exportType: ExportType = ExportType.SUBJECT_GRADES,
    val downloadComplete: Boolean = false,
    val downloadMessage: String? = null,
    val emailSent: Boolean = false,
    val emailMessage: String? = null,
    val error: String? = null
)
