package com.smartacademictracker.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.AcademicPeriod
import com.smartacademictracker.data.repository.SubjectRepository
import com.smartacademictracker.data.repository.AcademicPeriodRepository
import com.smartacademictracker.data.service.AcademicPeriodFilterService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddSubjectViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val academicPeriodRepository: AcademicPeriodRepository,
    private val academicPeriodFilterService: AcademicPeriodFilterService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddSubjectUiState())
    val uiState: StateFlow<AddSubjectUiState> = _uiState.asStateFlow()
    
    private val _academicPeriods = MutableStateFlow<List<AcademicPeriod>>(emptyList())
    val academicPeriods: StateFlow<List<AcademicPeriod>> = _academicPeriods.asStateFlow()
    
    private var _courseId: String = ""
    private var _yearLevelId: String = ""
    
    init {
        loadAcademicPeriods()
    }
    
    fun loadAcademicPeriods() {
        viewModelScope.launch {
            academicPeriodRepository.getAllAcademicPeriods().onSuccess { periods ->
                _academicPeriods.value = periods.sortedByDescending { it.createdAt }
            }.onFailure {
                // If loading fails, still allow using active period
                println("DEBUG: AddSubjectViewModel - Failed to load academic periods: ${it.message}")
            }
        }
    }
    
    fun setCourseId(courseId: String) {
        println("DEBUG: AddSubjectViewModel - Setting courseId: '$courseId'")
        _courseId = courseId
    }
    
    fun setYearLevelId(yearLevelId: String) {
        println("DEBUG: AddSubjectViewModel - Setting yearLevelId: '$yearLevelId'")
        _yearLevelId = yearLevelId
    }

    fun addSubject(
        name: String,
        code: String,
        description: String,
        credits: Int,
        numberOfSections: Int = 1,
        subjectType: com.smartacademictracker.data.model.SubjectType = com.smartacademictracker.data.model.SubjectType.MAJOR,
        selectedAcademicPeriodId: String? = null // If null, uses active period
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            println("DEBUG: AddSubjectViewModel - Adding subject with courseId: '$_courseId', yearLevelId: '$_yearLevelId', academicPeriodId: '$selectedAcademicPeriodId'")
            
            try {
                val semester: String
                val academicYear: String
                val academicPeriodId: String
                
                if (selectedAcademicPeriodId != null && selectedAcademicPeriodId.isNotEmpty()) {
                    // Use selected academic period
                    val periodResult = academicPeriodRepository.getAcademicPeriodById(selectedAcademicPeriodId)
                    val academicPeriod = periodResult.getOrNull()
                    if (academicPeriod == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Selected academic period not found. Please select a valid period."
                        )
                        return@launch
                    }
                    semester = academicPeriod.semester.displayName
                    academicYear = academicPeriod.academicYear
                    academicPeriodId = selectedAcademicPeriodId
                } else {
                    // Use active academic period (default behavior)
                    val academicContext = academicPeriodFilterService.getAcademicPeriodContext()
                    if (!academicContext.isActive) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "No active academic period found. Please select an academic period or set an active period first."
                        )
                        return@launch
                    }
                    semester = academicContext.semester
                    academicYear = academicContext.academicYear
                    academicPeriodId = academicContext.periodId
                }
                
                val result = subjectRepository.addSubjectForPeriod(
                    name = name,
                    code = code,
                    description = description,
                    credits = credits,
                    semester = semester,
                    academicYear = academicYear,
                    courseId = _courseId,
                    yearLevelId = _yearLevelId,
                    numberOfSections = numberOfSections,
                    subjectType = subjectType,
                    academicPeriodId = academicPeriodId
                )
                result.onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true
                    )
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to add subject"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to add subject"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(isSuccess = false)
    }
}

data class AddSubjectUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)
