package com.smartacademictracker.data.manager

import com.smartacademictracker.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory cache for admin dashboard data
 * Reduces Firestore queries by caching frequently accessed data
 */
@Singleton
class AdminDataCache @Inject constructor() {
    
    private var lastCacheTime: Long = 0
    private val CACHE_DURATION_MS = 5 * 60 * 1000L // 5 minutes
    
    private val _cachedSubjects = MutableStateFlow<List<Subject>>(emptyList())
    val cachedSubjects: StateFlow<List<Subject>> = _cachedSubjects.asStateFlow()
    
    private val _cachedEnrollments = MutableStateFlow<List<StudentEnrollment>>(emptyList())
    val cachedEnrollments: StateFlow<List<StudentEnrollment>> = _cachedEnrollments.asStateFlow()
    
    private val _cachedStudentApplications = MutableStateFlow<List<com.smartacademictracker.data.model.SubjectApplication>>(emptyList())
    val cachedStudentApplications: StateFlow<List<com.smartacademictracker.data.model.SubjectApplication>> = _cachedStudentApplications.asStateFlow()
    
    private val _cachedTeacherApplications = MutableStateFlow<List<com.smartacademictracker.data.model.TeacherApplication>>(emptyList())
    val cachedTeacherApplications: StateFlow<List<com.smartacademictracker.data.model.TeacherApplication>> = _cachedTeacherApplications.asStateFlow()
    
    private val _cachedUsers = MutableStateFlow<List<User>>(emptyList())
    val cachedUsers: StateFlow<List<User>> = _cachedUsers.asStateFlow()
    
    private val _cachedGradeEditRequests = MutableStateFlow<List<Grade>>(emptyList())
    val cachedGradeEditRequests: StateFlow<List<Grade>> = _cachedGradeEditRequests.asStateFlow()
    
    private val _cachedCourses = MutableStateFlow<List<com.smartacademictracker.data.model.Course>>(emptyList())
    val cachedCourses: StateFlow<List<com.smartacademictracker.data.model.Course>> = _cachedCourses.asStateFlow()
    
    private val _cachedYearLevels = MutableStateFlow<List<com.smartacademictracker.data.model.YearLevel>>(emptyList())
    val cachedYearLevels: StateFlow<List<com.smartacademictracker.data.model.YearLevel>> = _cachedYearLevels.asStateFlow()
    
    fun isCacheValid(): Boolean {
        return (System.currentTimeMillis() - lastCacheTime) < CACHE_DURATION_MS
    }
    
    fun hasCachedData(): Boolean {
        return _cachedSubjects.value.isNotEmpty() || 
               _cachedEnrollments.value.isNotEmpty() ||
               _cachedUsers.value.isNotEmpty()
    }
    
    fun updateSubjects(subjects: List<Subject>) {
        _cachedSubjects.value = subjects
        lastCacheTime = System.currentTimeMillis()
    }
    
    fun updateEnrollments(enrollments: List<StudentEnrollment>) {
        _cachedEnrollments.value = enrollments
        lastCacheTime = System.currentTimeMillis()
    }
    
    
    fun updateUsers(users: List<User>) {
        _cachedUsers.value = users
        lastCacheTime = System.currentTimeMillis()
    }
    
    fun updateGradeEditRequests(requests: List<Grade>) {
        _cachedGradeEditRequests.value = requests
        lastCacheTime = System.currentTimeMillis()
    }
    
    fun updateStudentApplications(applications: List<com.smartacademictracker.data.model.SubjectApplication>) {
        _cachedStudentApplications.value = applications
        lastCacheTime = System.currentTimeMillis()
    }
    
    fun updateTeacherApplications(applications: List<com.smartacademictracker.data.model.TeacherApplication>) {
        _cachedTeacherApplications.value = applications
        lastCacheTime = System.currentTimeMillis()
    }
    
    fun updateCourses(courses: List<com.smartacademictracker.data.model.Course>) {
        _cachedCourses.value = courses
        lastCacheTime = System.currentTimeMillis()
    }
    
    fun updateYearLevels(yearLevels: List<com.smartacademictracker.data.model.YearLevel>) {
        _cachedYearLevels.value = yearLevels
        lastCacheTime = System.currentTimeMillis()
    }
    
    fun updateAll(
        subjects: List<Subject>,
        enrollments: List<StudentEnrollment>,
        studentApplications: List<com.smartacademictracker.data.model.SubjectApplication>,
        teacherApplications: List<com.smartacademictracker.data.model.TeacherApplication>,
        users: List<User>,
        gradeEditRequests: List<Grade>
    ) {
        _cachedSubjects.value = subjects
        _cachedEnrollments.value = enrollments
        _cachedStudentApplications.value = studentApplications
        _cachedTeacherApplications.value = teacherApplications
        _cachedUsers.value = users
        _cachedGradeEditRequests.value = gradeEditRequests
        lastCacheTime = System.currentTimeMillis()
    }
    
    fun clearCache() {
        _cachedSubjects.value = emptyList()
        _cachedEnrollments.value = emptyList()
        _cachedStudentApplications.value = emptyList()
        _cachedTeacherApplications.value = emptyList()
        _cachedUsers.value = emptyList()
        _cachedGradeEditRequests.value = emptyList()
        _cachedCourses.value = emptyList()
        _cachedYearLevels.value = emptyList()
        lastCacheTime = 0
    }
}

