package com.smartacademictracker.presentation.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.GradeCurve
import com.smartacademictracker.data.model.CurveType
import com.smartacademictracker.data.model.CurveApplication
import com.smartacademictracker.data.model.CurveStatistics
import com.smartacademictracker.data.model.GradePeriod
import com.smartacademictracker.data.repository.GradeCurveRepository
import com.smartacademictracker.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeacherGradeCurveViewModel @Inject constructor(
    private val curveRepository: GradeCurveRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherGradeCurveUiState())
    val uiState: StateFlow<TeacherGradeCurveUiState> = _uiState.asStateFlow()

    fun loadCurves(subjectId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = curveRepository.getCurvesBySubject(subjectId)
                result.onSuccess { curves ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        curves = curves,
                        currentSubjectId = subjectId
                    )
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load curves"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load curves"
                )
            }
        }
    }

    fun loadCurveStatistics(subjectId: String, gradePeriod: GradePeriod) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = curveRepository.calculateCurveStatistics(subjectId, gradePeriod)
                result.onSuccess { statistics ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        statistics = statistics,
                        selectedGradePeriod = gradePeriod
                    )
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load statistics"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load statistics"
                )
            }
        }
    }

    fun previewCurve(
        subjectId: String,
        gradePeriod: GradePeriod,
        curve: GradeCurve
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = curveRepository.previewCurve(subjectId, gradePeriod, curve)
                result.onSuccess { (curveApplications, statistics) ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        curvePreview = curveApplications,
                        previewStatistics = statistics,
                        selectedCurve = curve
                    )
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to preview curve"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to preview curve"
                )
            }
        }
    }

    fun applyCurve(
        subjectId: String,
        gradePeriod: GradePeriod,
        curve: GradeCurve
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
                    
                    val curveWithTeacher = curve.copy(
                        teacherId = currentUser.id
                    )
                    
                    val createResult = curveRepository.createCurve(curveWithTeacher)
                    createResult.onSuccess {
                        val applyResult = curveRepository.applyCurveToGrades(
                            subjectId = subjectId,
                            gradePeriod = gradePeriod,
                            curve = curveWithTeacher
                        )
                        
                        applyResult.onSuccess { curveApplications ->
                            val saveResult = curveRepository.saveCurveApplication(
                                subjectId = subjectId,
                                gradePeriod = gradePeriod,
                                curveApplications = curveApplications
                            )
                            
                            saveResult.onSuccess {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    curveApplied = true,
                                    appliedCurve = curveWithTeacher
                                )
                                // Reload curves to show the new one
                                loadCurves(subjectId)
                            }.onFailure { exception ->
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = exception.message ?: "Failed to save curve application"
                                )
                            }
                        }.onFailure { exception ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to apply curve"
                            )
                        }
                    }.onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to create curve"
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
                    error = e.message ?: "Failed to apply curve"
                )
            }
        }
    }

    fun setCurveType(curveType: CurveType) {
        _uiState.value = _uiState.value.copy(selectedCurveType = curveType)
    }

    fun setGradePeriod(gradePeriod: GradePeriod) {
        _uiState.value = _uiState.value.copy(selectedGradePeriod = gradePeriod)
    }

    fun updateCurveParameters(
        adjustmentFactor: Double,
        targetAverage: Double,
        maxGrade: Double,
        minGrade: Double
    ) {
        _uiState.value = _uiState.value.copy(
            adjustmentFactor = adjustmentFactor,
            targetAverage = targetAverage,
            maxGrade = maxGrade,
            minGrade = minGrade
        )
    }

    fun clearPreview() {
        _uiState.value = _uiState.value.copy(
            curvePreview = emptyList(),
            previewStatistics = null,
            selectedCurve = null
        )
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            curveApplied = false,
            appliedCurve = null,
            error = null
        )
    }
}

data class TeacherGradeCurveUiState(
    val isLoading: Boolean = false,
    val curves: List<GradeCurve> = emptyList(),
    val statistics: CurveStatistics? = null,
    val curvePreview: List<CurveApplication> = emptyList(),
    val previewStatistics: CurveStatistics? = null,
    val selectedCurveType: CurveType = CurveType.LINEAR,
    val selectedGradePeriod: GradePeriod = GradePeriod.PRELIM,
    val adjustmentFactor: Double = 0.0,
    val targetAverage: Double = 75.0,
    val maxGrade: Double = 100.0,
    val minGrade: Double = 0.0,
    val selectedCurve: GradeCurve? = null,
    val curveApplied: Boolean = false,
    val appliedCurve: GradeCurve? = null,
    val currentSubjectId: String = "",
    val error: String? = null
)
