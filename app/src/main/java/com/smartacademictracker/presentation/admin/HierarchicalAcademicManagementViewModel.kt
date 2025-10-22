package com.smartacademictracker.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.Course
import com.smartacademictracker.data.model.YearLevel
import com.smartacademictracker.data.model.Subject
import com.smartacademictracker.data.repository.CourseRepository
import com.smartacademictracker.data.repository.YearLevelRepository
import com.smartacademictracker.data.repository.SubjectRepository
import com.smartacademictracker.data.migration.DatabaseMigrationService
import com.smartacademictracker.data.service.AcademicPeriodFilterService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HierarchicalAcademicManagementViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val yearLevelRepository: YearLevelRepository,
    private val subjectRepository: SubjectRepository,
    private val migrationService: DatabaseMigrationService,
    private val academicPeriodFilterService: AcademicPeriodFilterService
) : ViewModel() {

    private val _uiState = MutableStateFlow(HierarchicalAcademicManagementUiState())
    val uiState: StateFlow<HierarchicalAcademicManagementUiState> = _uiState.asStateFlow()

    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()

    private val _yearLevels = MutableStateFlow<List<YearLevel>>(emptyList())
    val yearLevels: StateFlow<List<YearLevel>> = _yearLevels.asStateFlow()

    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    val subjects: StateFlow<List<Subject>> = _subjects.asStateFlow()

    fun loadAllData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Check if there's an active academic period
                val hasActivePeriod = academicPeriodFilterService.hasActiveAcademicPeriod()
                if (!hasActivePeriod) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "No active academic period found. Please create an academic period first."
                    )
                    return@launch
                }
                // First, clean up corrupted subjects
                println("DEBUG: HierarchicalAcademicManagementViewModel - Cleaning up corrupted subjects...")
                subjectRepository.cleanupCorruptedSubjects().onSuccess { corruptedCount ->
                    if (corruptedCount > 0) {
                        println("DEBUG: HierarchicalAcademicManagementViewModel - Cleaned up $corruptedCount corrupted subjects")
                    }
                }
                
                // Load courses
                courseRepository.getAllCourses().onSuccess { coursesList ->
                    _courses.value = coursesList
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to load courses: ${exception.message}"
                    )
                }

                // Load year levels
                yearLevelRepository.getAllYearLevels().onSuccess { yearLevelsList ->
                    _yearLevels.value = yearLevelsList
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to load year levels: ${exception.message}"
                    )
                }

                // Load subjects
                subjectRepository.getAllSubjects().onSuccess { subjectsList ->
                    _subjects.value = subjectsList
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to load subjects: ${exception.message}"
                    )
                }

                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load data: ${e.message}"
                )
            }
        }
    }

    fun refreshAllData() {
        loadAllData()
    }
    
    fun refreshData() {
        viewModelScope.launch {
            // First, clean up corrupted subjects
            println("DEBUG: HierarchicalAcademicManagementViewModel - Cleaning up corrupted subjects in refresh...")
            subjectRepository.cleanupCorruptedSubjects().onSuccess { corruptedCount ->
                if (corruptedCount > 0) {
                    println("DEBUG: HierarchicalAcademicManagementViewModel - Cleaned up $corruptedCount corrupted subjects")
                }
            }
            
            // Load courses
            courseRepository.getAllCourses().onSuccess { coursesList ->
                _courses.value = coursesList
            }
            
            // Load year levels
            yearLevelRepository.getAllYearLevels().onSuccess { yearLevelsList ->
                _yearLevels.value = yearLevelsList
                println("DEBUG: HierarchicalAcademicManagementViewModel - Loaded ${yearLevelsList.size} year levels")
                yearLevelsList.forEach { yearLevel ->
                    println("DEBUG: Year Level - ID: ${yearLevel.id}, CourseId: ${yearLevel.courseId}, Name: ${yearLevel.name}")
                }
            }
            
            // Load subjects
            subjectRepository.getAllSubjects().onSuccess { subjectsList ->
                _subjects.value = subjectsList
            }
        }
    }
    
    fun cleanupOrphanedYearLevels() {
        viewModelScope.launch {
            try {
                // Get all year levels with empty courseId
                val orphanedYearLevels = _yearLevels.value.filter { it.courseId.isEmpty() }
                
                if (orphanedYearLevels.isNotEmpty()) {
                    println("DEBUG: HierarchicalAcademicManagementViewModel - Found ${orphanedYearLevels.size} orphaned year levels, cleaning up...")
                    
                    // Delete orphaned year levels
                    for (yearLevel in orphanedYearLevels) {
                        yearLevelRepository.deleteYearLevel(yearLevel.id)
                        println("DEBUG: HierarchicalAcademicManagementViewModel - Deleted orphaned year level: ${yearLevel.name}")
                    }
                    
                    // Refresh data after cleanup
                    refreshData()
                }
            } catch (e: Exception) {
                println("DEBUG: HierarchicalAcademicManagementViewModel - Error cleaning up orphaned year levels: ${e.message}")
            }
        }
    }

    fun deleteCourse(courseId: String) {
        viewModelScope.launch {
            courseRepository.deleteCourse(courseId).onSuccess {
                // Remove from local list
                _courses.value = _courses.value.filter { it.id != courseId }
                // Also remove associated year levels and subjects
                _yearLevels.value = _yearLevels.value.filter { it.courseId != courseId }
                _subjects.value = _subjects.value.filter { subject ->
                    _yearLevels.value.none { it.id == subject.yearLevelId }
                }
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete course: ${exception.message}"
                )
            }
        }
    }

    fun deleteYearLevel(yearLevelId: String) {
        viewModelScope.launch {
            yearLevelRepository.deleteYearLevel(yearLevelId).onSuccess {
                // Remove from local list
                _yearLevels.value = _yearLevels.value.filter { it.id != yearLevelId }
                // Also remove associated subjects
                _subjects.value = _subjects.value.filter { it.yearLevelId != yearLevelId }
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete year level: ${exception.message}"
                )
            }
        }
    }

    fun deleteSubject(subjectId: String) {
        viewModelScope.launch {
            subjectRepository.deleteSubject(subjectId).onSuccess {
                // Remove from local list
                _subjects.value = _subjects.value.filter { it.id != subjectId }
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete subject: ${exception.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun runMigration() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // First try redistribution to fix the current issue
                val redistributeResult = migrationService.redistributeYearLevels()
                if (redistributeResult.isSuccess) {
                    println("DEBUG: Redistribution successful: ${redistributeResult.getOrNull()}")
                    // Refresh data after redistribution
                    refreshData()
                    _uiState.value = _uiState.value.copy(isLoading = false)
                } else {
                    // If redistribution fails, try full migration
                    val result = migrationService.runFullMigration()
                    if (result.isSuccess) {
                        println("DEBUG: Migration successful: ${result.getOrNull()}")
                        refreshData()
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Migration failed: ${result.exceptionOrNull()?.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Migration error: ${e.message}"
                )
            }
        }
    }
}

data class HierarchicalAcademicManagementUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
