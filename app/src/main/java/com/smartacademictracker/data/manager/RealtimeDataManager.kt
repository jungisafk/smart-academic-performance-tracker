package com.smartacademictracker.data.manager

import com.smartacademictracker.data.model.Enrollment
import com.smartacademictracker.data.model.StudentApplication
import com.smartacademictracker.data.model.Subject
import com.smartacademictracker.data.model.User
import com.smartacademictracker.data.repository.EnrollmentRepository
import com.smartacademictracker.data.repository.StudentApplicationRepository
import com.smartacademictracker.data.repository.SubjectRepository
import com.smartacademictracker.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealtimeDataManager @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val studentApplicationRepository: StudentApplicationRepository,
    private val userRepository: UserRepository
) {
    
    // Global state for real-time data
    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    val subjects: StateFlow<List<Subject>> = _subjects.asStateFlow()
    
    private val _enrollments = MutableStateFlow<List<Enrollment>>(emptyList())
    val enrollments: StateFlow<List<Enrollment>> = _enrollments.asStateFlow()
    
    private val _applications = MutableStateFlow<List<StudentApplication>>(emptyList())
    val applications: StateFlow<List<StudentApplication>> = _applications.asStateFlow()
    
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()
    
    // Listeners for real-time updates
    private val listeners = mutableSetOf<RealtimeDataListener>()
    
    interface RealtimeDataListener {
        fun onSubjectsUpdated(subjects: List<Subject>)
        fun onEnrollmentsUpdated(enrollments: List<Enrollment>)
        fun onApplicationsUpdated(applications: List<StudentApplication>)
        fun onUsersUpdated(users: List<User>)
    }
    
    fun addListener(listener: RealtimeDataListener) {
        listeners.add(listener)
    }
    
    fun removeListener(listener: RealtimeDataListener) {
        listeners.remove(listener)
    }
    
    suspend fun loadAllData() {
        // Load subjects
        subjectRepository.getAllSubjects().onSuccess { subjects ->
            _subjects.value = subjects
            notifySubjectsUpdated(subjects)
        }
        
        // Load enrollments
        enrollmentRepository.getAllEnrollments().onSuccess { enrollments ->
            _enrollments.value = enrollments
            notifyEnrollmentsUpdated(enrollments)
        }
        
        // Load applications
        studentApplicationRepository.getAllApplications().onSuccess { applications ->
            _applications.value = applications
            notifyApplicationsUpdated(applications)
        }
        
        // Load users
        userRepository.getAllUsers().onSuccess { users ->
            _users.value = users
            notifyUsersUpdated(users)
        }
    }
    
    fun updateSubjects(subjects: List<Subject>) {
        _subjects.value = subjects
        notifySubjectsUpdated(subjects)
    }
    
    fun updateEnrollments(enrollments: List<Enrollment>) {
        _enrollments.value = enrollments
        notifyEnrollmentsUpdated(enrollments)
    }
    
    fun updateApplications(applications: List<StudentApplication>) {
        _applications.value = applications
        notifyApplicationsUpdated(applications)
    }
    
    fun updateUsers(users: List<User>) {
        _users.value = users
        notifyUsersUpdated(users)
    }
    
    private fun notifySubjectsUpdated(subjects: List<Subject>) {
        listeners.forEach { it.onSubjectsUpdated(subjects) }
    }
    
    private fun notifyEnrollmentsUpdated(enrollments: List<Enrollment>) {
        listeners.forEach { it.onEnrollmentsUpdated(enrollments) }
    }
    
    private fun notifyApplicationsUpdated(applications: List<StudentApplication>) {
        listeners.forEach { it.onApplicationsUpdated(applications) }
    }
    
    private fun notifyUsersUpdated(users: List<User>) {
        listeners.forEach { it.onUsersUpdated(users) }
    }
}
