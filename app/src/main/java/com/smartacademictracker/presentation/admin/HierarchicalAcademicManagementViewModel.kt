package com.smartacademictracker.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.Course
import com.smartacademictracker.data.model.YearLevel
import com.smartacademictracker.data.model.Subject
import com.smartacademictracker.data.repository.CourseRepository
import com.smartacademictracker.data.repository.YearLevelRepository
import com.smartacademictracker.data.repository.SubjectRepository
import com.smartacademictracker.data.repository.UserRepository
import com.smartacademictracker.data.migration.DatabaseMigrationService
import com.smartacademictracker.data.service.AcademicPeriodFilterService
import com.smartacademictracker.data.manager.AdminDataCache
import com.smartacademictracker.data.notification.NotificationSenderService
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
    private val academicPeriodFilterService: AcademicPeriodFilterService,
    private val adminDataCache: AdminDataCache,
    private val userRepository: UserRepository,
    private val notificationSenderService: NotificationSenderService
) : ViewModel() {

    private val _uiState = MutableStateFlow(HierarchicalAcademicManagementUiState())
    val uiState: StateFlow<HierarchicalAcademicManagementUiState> = _uiState.asStateFlow()

    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()

    private val _yearLevels = MutableStateFlow<List<YearLevel>>(emptyList())
    val yearLevels: StateFlow<List<YearLevel>> = _yearLevels.asStateFlow()

    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    val subjects: StateFlow<List<Subject>> = _subjects.asStateFlow()

    init {
        // Load cached data immediately if available
        val cachedCourses = adminDataCache.cachedCourses.value
        val cachedYearLevels = adminDataCache.cachedYearLevels.value
        val cachedSubjects = adminDataCache.cachedSubjects.value
        
        if (cachedCourses.isNotEmpty() && cachedYearLevels.isNotEmpty() && 
            cachedSubjects.isNotEmpty() && adminDataCache.isCacheValid()) {
            _courses.value = cachedCourses
            _yearLevels.value = cachedYearLevels
            _subjects.value = cachedSubjects
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun loadAllData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            // Load cached data first if available and not forcing refresh
            if (!forceRefresh && adminDataCache.cachedCourses.value.isNotEmpty() && 
                adminDataCache.cachedYearLevels.value.isNotEmpty() &&
                adminDataCache.cachedSubjects.value.isNotEmpty() &&
                adminDataCache.isCacheValid()) {
                _courses.value = adminDataCache.cachedCourses.value
                _yearLevels.value = adminDataCache.cachedYearLevels.value
                _subjects.value = adminDataCache.cachedSubjects.value
                _uiState.value = _uiState.value.copy(isLoading = false)
            } else {
                // Only show loading if we don't have cached data
                if (adminDataCache.cachedCourses.value.isEmpty() || 
                    adminDataCache.cachedYearLevels.value.isEmpty() ||
                    adminDataCache.cachedSubjects.value.isEmpty()) {
                    _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                }
            }
            
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
                
                subjectRepository.cleanupCorruptedSubjects().onSuccess { corruptedCount ->
                    if (corruptedCount > 0) {
                        
                    }
                }
                
                // Load courses
                courseRepository.getAllCourses().onSuccess { coursesList ->
                    _courses.value = coursesList
                    adminDataCache.updateCourses(coursesList)
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to load courses: ${exception.message}"
                    )
                }

                // Load year levels
                yearLevelRepository.getAllYearLevels().onSuccess { yearLevelsList ->
                    _yearLevels.value = yearLevelsList
                    adminDataCache.updateYearLevels(yearLevelsList)
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to load year levels: ${exception.message}"
                    )
                }

                // Load subjects
                subjectRepository.getAllSubjects().onSuccess { subjectsList ->
                    _subjects.value = subjectsList
                    adminDataCache.updateSubjects(subjectsList)
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
        loadAllData(forceRefresh = true)
    }
    
    fun refreshData() {
        viewModelScope.launch {
            // First, clean up corrupted subjects
            
            subjectRepository.cleanupCorruptedSubjects().onSuccess { corruptedCount ->
                if (corruptedCount > 0) {
                    
                }
            }
            
            // Load courses
            courseRepository.getAllCourses().onSuccess { coursesList ->
                _courses.value = coursesList
                adminDataCache.updateCourses(coursesList)
            }
            
            // Load year levels
            yearLevelRepository.getAllYearLevels().onSuccess { yearLevelsList ->
                _yearLevels.value = yearLevelsList
                adminDataCache.updateYearLevels(yearLevelsList)
            }
            
            // Load subjects
            subjectRepository.getAllSubjects().onSuccess { subjectsList ->
                _subjects.value = subjectsList
                adminDataCache.updateSubjects(subjectsList)
            }
        }
    }
    
    fun cleanupOrphanedYearLevels() {
        viewModelScope.launch {
            try {
                // Get all year levels with empty courseId
                val orphanedYearLevels = _yearLevels.value.filter { it.courseId.isEmpty() }
                
                if (orphanedYearLevels.isNotEmpty()) {
                    
                    
                    // Delete orphaned year levels
                    for (yearLevel in orphanedYearLevels) {
                        yearLevelRepository.deleteYearLevel(yearLevel.id)
                        
                    }
                    
                    // Refresh data after cleanup
                    refreshData()
                }
            } catch (e: Exception) {
                
            }
        }
    }

    fun deleteCourse(courseId: String) {
        viewModelScope.launch {
            // Get course details before deletion for notification
            val course = _courses.value.find { it.id == courseId }
            
            courseRepository.deleteCourse(courseId).onSuccess {
                // Get all users to notify them
                val allUsersResult = userRepository.getAllUsers()
                allUsersResult.onSuccess { users ->
                    val userIds = users.map { it.id }
                    if (course != null) {
                        notificationSenderService.sendCourseDeletedNotification(
                            userIds = userIds,
                            courseName = course.name,
                            courseCode = course.code
                        )
                    }
                }
                
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
            // Get year level details before deletion for notification
            val yearLevel = _yearLevels.value.find { it.id == yearLevelId }
            
            yearLevelRepository.deleteYearLevel(yearLevelId).onSuccess {
                // Get all users to notify them
                val allUsersResult = userRepository.getAllUsers()
                allUsersResult.onSuccess { users ->
                    val userIds = users.map { it.id }
                    if (yearLevel != null) {
                        notificationSenderService.sendYearLevelDeletedNotification(
                            userIds = userIds,
                            yearLevelName = yearLevel.name
                        )
                    }
                }
                
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
            // Get subject details before deletion for notification
            val subject = _subjects.value.find { it.id == subjectId }
            
            subjectRepository.deleteSubject(subjectId).onSuccess {
                // Get all users to notify them
                val allUsersResult = userRepository.getAllUsers()
                allUsersResult.onSuccess { users ->
                    val userIds = users.map { it.id }
                    if (subject != null) {
                        notificationSenderService.sendSubjectDeletedNotification(
                            userIds = userIds,
                            subjectName = subject.name,
                            subjectCode = subject.code
                        )
                    }
                }
                
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
                    // Refresh data after redistribution
                    refreshData()
                    _uiState.value = _uiState.value.copy(isLoading = false)
                } else {
                    // If redistribution fails, try full migration
                    val result = migrationService.runFullMigration()
                    if (result.isSuccess) {
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
