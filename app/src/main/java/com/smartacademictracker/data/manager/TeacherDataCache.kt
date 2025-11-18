package com.smartacademictracker.data.manager

import com.smartacademictracker.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory cache for teacher dashboard data
 * Reduces Firestore queries by caching frequently accessed data
 */
@Singleton
class TeacherDataCache @Inject constructor() {
    
    private var lastCacheTime: Long = 0
    private val CACHE_DURATION_MS = 5 * 60 * 1000L // 5 minutes
    
    private val _cachedSubjects = MutableStateFlow<List<Subject>>(emptyList())
    val cachedSubjects: StateFlow<List<Subject>> = _cachedSubjects.asStateFlow()
    
    private val _cachedEnrollments = MutableStateFlow<List<StudentEnrollment>>(emptyList())
    val cachedEnrollments: StateFlow<List<StudentEnrollment>> = _cachedEnrollments.asStateFlow()
    
    private val _cachedStudentApplications = MutableStateFlow<List<StudentApplication>>(emptyList())
    val cachedStudentApplications: StateFlow<List<StudentApplication>> = _cachedStudentApplications.asStateFlow()
    
    private val _cachedGrades = MutableStateFlow<List<Grade>>(emptyList())
    val cachedGrades: StateFlow<List<Grade>> = _cachedGrades.asStateFlow()
    
    private val _cachedSections = MutableStateFlow<List<SectionAssignment>>(emptyList())
    val cachedSections: StateFlow<List<SectionAssignment>> = _cachedSections.asStateFlow()
    
    fun isCacheValid(): Boolean {
        return (System.currentTimeMillis() - lastCacheTime) < CACHE_DURATION_MS
    }
    
    fun hasCachedData(): Boolean {
        return _cachedSubjects.value.isNotEmpty() || 
               _cachedEnrollments.value.isNotEmpty() ||
               _cachedStudentApplications.value.isNotEmpty()
    }
    
    fun updateSubjects(subjects: List<Subject>) {
        _cachedSubjects.value = subjects
        lastCacheTime = System.currentTimeMillis()
    }
    
    fun updateEnrollments(enrollments: List<StudentEnrollment>) {
        _cachedEnrollments.value = enrollments
        lastCacheTime = System.currentTimeMillis()
    }
    
    fun updateStudentApplications(applications: List<StudentApplication>) {
        _cachedStudentApplications.value = applications
        lastCacheTime = System.currentTimeMillis()
    }
    
    fun updateGrades(grades: List<Grade>) {
        _cachedGrades.value = grades
        lastCacheTime = System.currentTimeMillis()
    }
    
    fun updateSections(sections: List<SectionAssignment>) {
        _cachedSections.value = sections
        lastCacheTime = System.currentTimeMillis()
    }
    
    fun clearCache() {
        _cachedSubjects.value = emptyList()
        _cachedEnrollments.value = emptyList()
        _cachedStudentApplications.value = emptyList()
        _cachedGrades.value = emptyList()
        _cachedSections.value = emptyList()
        lastCacheTime = 0
    }
}

