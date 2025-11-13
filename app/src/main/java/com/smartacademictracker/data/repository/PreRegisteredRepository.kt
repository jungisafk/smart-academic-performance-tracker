package com.smartacademictracker.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.smartacademictracker.data.model.PreRegisteredStudent
import com.smartacademictracker.data.model.PreRegisteredTeacher
import com.smartacademictracker.data.model.UserRole
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing pre-registered students and teachers
 * Handles CRUD operations for institutional pre-registration
 */
@Singleton
class PreRegisteredRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val studentsCollection = firestore.collection("pre_registered_students")
    private val teachersCollection = firestore.collection("pre_registered_teachers")
    
    /**
     * Convert Firestore document to PreRegisteredStudent
     * Handles field name mapping (isRegistered vs registered)
     */
    private fun convertDocumentToPreRegisteredStudent(doc: DocumentSnapshot): PreRegisteredStudent {
        val data = doc.data ?: emptyMap()
        
        // DEBUG: Log raw Firestore data
        val rawIsRegistered = data["isRegistered"]
        val rawRegistered = data["registered"]
        val studentId = data["studentId"] as? String ?: ""
        val firebaseUserId = data["firebaseUserId"] as? String
        
        android.util.Log.d("PreRegisteredRepo", "=== Converting Student Document ===")
        android.util.Log.d("PreRegisteredRepo", "Document ID: ${doc.id}")
        android.util.Log.d("PreRegisteredRepo", "Student ID: $studentId")
        android.util.Log.d("PreRegisteredRepo", "Raw data['isRegistered']: $rawIsRegistered (type: ${rawIsRegistered?.javaClass?.simpleName})")
        android.util.Log.d("PreRegisteredRepo", "Raw data['registered']: $rawRegistered (type: ${rawRegistered?.javaClass?.simpleName})")
        android.util.Log.d("PreRegisteredRepo", "Raw data['firebaseUserId']: $firebaseUserId")
        android.util.Log.d("PreRegisteredRepo", "All registration-related fields: ${data.filterKeys { it.contains("register", ignoreCase = true) || it.contains("firebase", ignoreCase = true) }}")
        
        // Determine isRegistered value
        // Priority: firebaseUserId exists > isRegistered field > registered field
        val isRegisteredValue = when {
            // If firebaseUserId exists, account is definitely registered
            !firebaseUserId.isNullOrBlank() -> {
                android.util.Log.d("PreRegisteredRepo", "Using firebaseUserId to determine registered: true (firebaseUserId=$firebaseUserId)")
                true
            }
            rawIsRegistered is Boolean -> {
                android.util.Log.d("PreRegisteredRepo", "Using isRegistered (Boolean): $rawIsRegistered")
                rawIsRegistered
            }
            rawRegistered is Boolean -> {
                android.util.Log.d("PreRegisteredRepo", "Using registered (Boolean): $rawRegistered")
                rawRegistered
            }
            else -> {
                android.util.Log.w("PreRegisteredRepo", "No registration field found, defaulting to false")
                false
            }
        }
        
        android.util.Log.d("PreRegisteredRepo", "Final isRegistered value: $isRegisteredValue (firebaseUserId: $firebaseUserId)")
        
        return PreRegisteredStudent(
            id = doc.id,
            studentId = data["studentId"] as? String ?: "",
            firstName = data["firstName"] as? String ?: "",
            lastName = data["lastName"] as? String ?: "",
            middleName = data["middleName"] as? String,
            suffix = data["suffix"] as? String,
            courseId = data["courseId"] as? String ?: "",
            courseName = data["courseName"] as? String ?: "",
            courseCode = data["courseCode"] as? String ?: "",
            yearLevelId = data["yearLevelId"] as? String ?: "",
            yearLevelName = data["yearLevelName"] as? String ?: "",
            section = data["section"] as? String,
            enrollmentYear = data["enrollmentYear"] as? String ?: "",
            email = data["email"] as? String,
            phoneNumber = data["phoneNumber"] as? String,
            dateOfBirth = data["dateOfBirth"] as? String,
            address = data["address"] as? String,
            // Handle both "isRegistered" and "registered" field names
            isRegistered = isRegisteredValue,
            registeredAt = (data["registeredAt"] as? Long) ?: (data["registeredAt"] as? Number)?.toLong(),
            firebaseUserId = firebaseUserId,
            createdBy = data["createdBy"] as? String ?: "",
            createdByName = data["createdByName"] as? String ?: "",
            createdAt = (data["createdAt"] as? Long) ?: (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            updatedAt = (data["updatedAt"] as? Long) ?: (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            updatedBy = data["updatedBy"] as? String,
            notes = data["notes"] as? String ?: "",
            active = (data["active"] as? Boolean) ?: true,
            emailSent = (data["emailSent"] as? Boolean) ?: false,
            emailSentAt = (data["emailSentAt"] as? Long) ?: (data["emailSentAt"] as? Number)?.toLong()
        )
    }
    
    /**
     * Convert Firestore document to PreRegisteredTeacher
     * Handles field name mapping (isRegistered vs registered)
     */
    private fun convertDocumentToPreRegisteredTeacher(doc: DocumentSnapshot): PreRegisteredTeacher {
        val data = doc.data ?: emptyMap()
        
        // DEBUG: Log raw Firestore data
        val rawIsRegistered = data["isRegistered"]
        val rawRegistered = data["registered"]
        val teacherId = data["teacherId"] as? String ?: ""
        val firebaseUserId = data["firebaseUserId"] as? String
        
        android.util.Log.d("PreRegisteredRepo", "=== Converting Teacher Document ===")
        android.util.Log.d("PreRegisteredRepo", "Document ID: ${doc.id}")
        android.util.Log.d("PreRegisteredRepo", "Teacher ID: $teacherId")
        android.util.Log.d("PreRegisteredRepo", "Raw data['isRegistered']: $rawIsRegistered (type: ${rawIsRegistered?.javaClass?.simpleName})")
        android.util.Log.d("PreRegisteredRepo", "Raw data['registered']: $rawRegistered (type: ${rawRegistered?.javaClass?.simpleName})")
        android.util.Log.d("PreRegisteredRepo", "Raw data['firebaseUserId']: $firebaseUserId")
        
        // Determine isRegistered value
        // Priority: firebaseUserId exists > isRegistered field > registered field
        val isRegisteredValue = when {
            // If firebaseUserId exists, account is definitely registered
            !firebaseUserId.isNullOrBlank() -> {
                android.util.Log.d("PreRegisteredRepo", "Using firebaseUserId to determine registered: true (firebaseUserId=$firebaseUserId)")
                true
            }
            rawIsRegistered is Boolean -> {
                android.util.Log.d("PreRegisteredRepo", "Using isRegistered (Boolean): $rawIsRegistered")
                rawIsRegistered
            }
            rawRegistered is Boolean -> {
                android.util.Log.d("PreRegisteredRepo", "Using registered (Boolean): $rawRegistered")
                rawRegistered
            }
            else -> {
                android.util.Log.w("PreRegisteredRepo", "No registration field found, defaulting to false")
                false
            }
        }
        
        android.util.Log.d("PreRegisteredRepo", "Final isRegistered value: $isRegisteredValue (firebaseUserId: $firebaseUserId)")
        
        val employmentTypeStr = data["employmentType"] as? String ?: "FULL_TIME"
        val employmentType = try {
            com.smartacademictracker.data.model.EmploymentType.valueOf(employmentTypeStr)
        } catch (e: Exception) {
            com.smartacademictracker.data.model.EmploymentType.FULL_TIME
        }
        
        return PreRegisteredTeacher(
            id = doc.id,
            teacherId = data["teacherId"] as? String ?: "",
            firstName = data["firstName"] as? String ?: "",
            lastName = data["lastName"] as? String ?: "",
            middleName = data["middleName"] as? String,
            suffix = data["suffix"] as? String,
            departmentCourseId = data["departmentCourseId"] as? String ?: "",
            departmentCourseName = data["departmentCourseName"] as? String ?: "",
            departmentCourseCode = data["departmentCourseCode"] as? String ?: "",
            employmentType = employmentType,
            position = data["position"] as? String,
            specialization = data["specialization"] as? String,
            email = data["email"] as? String,
            phoneNumber = data["phoneNumber"] as? String,
            dateOfBirth = data["dateOfBirth"] as? String,
            address = data["address"] as? String,
            dateHired = data["dateHired"] as? String,
            employeeNumber = data["employeeNumber"] as? String,
            // Handle both "isRegistered" and "registered" field names
            isRegistered = isRegisteredValue,
            registeredAt = (data["registeredAt"] as? Long) ?: (data["registeredAt"] as? Number)?.toLong(),
            firebaseUserId = firebaseUserId,
            createdBy = data["createdBy"] as? String ?: "",
            createdByName = data["createdByName"] as? String ?: "",
            createdAt = (data["createdAt"] as? Long) ?: (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            updatedAt = (data["updatedAt"] as? Long) ?: (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            updatedBy = data["updatedBy"] as? String,
            notes = data["notes"] as? String ?: "",
            active = (data["active"] as? Boolean) ?: true,
            emailSent = (data["emailSent"] as? Boolean) ?: false,
            emailSentAt = (data["emailSentAt"] as? Long) ?: (data["emailSentAt"] as? Number)?.toLong()
        )
    }
    
    // ==================== STUDENT OPERATIONS ====================
    
    /**
     * Get pre-registered student by student ID
     */
    suspend fun getPreRegisteredStudent(studentId: String): Result<PreRegisteredStudent> {
        return try {
            val snapshot = studentsCollection
                .whereEqualTo("studentId", studentId)
                .limit(1)
                .get()
                .await()
            
            if (snapshot.isEmpty) {
                return Result.failure(Exception("Student ID not found. Please contact administration."))
            }
            
            val student = convertDocumentToPreRegisteredStudent(snapshot.documents[0])
            
            Result.success(student)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get pre-registered student by document ID
     */
    suspend fun getPreRegisteredStudentById(docId: String): Result<PreRegisteredStudent> {
        return try {
            val doc = studentsCollection.document(docId).get().await()
            
            if (!doc.exists()) {
                return Result.failure(Exception("Student not found"))
            }
            
            val student = convertDocumentToPreRegisteredStudent(doc)
            
            Result.success(student)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all pre-registered students
     */
    suspend fun getAllPreRegisteredStudents(): Result<List<PreRegisteredStudent>> {
        return try {
            android.util.Log.d("PreRegisteredRepo", "=== getAllPreRegisteredStudents ===")
            
            val snapshot = studentsCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            android.util.Log.d("PreRegisteredRepo", "Query returned ${snapshot.size()} documents")
            
            val students = snapshot.documents.map { doc ->
                val student = convertDocumentToPreRegisteredStudent(doc)
                android.util.Log.d("PreRegisteredRepo", "Student ${student.studentId}: isRegistered=${student.isRegistered}, firebaseUserId=${student.firebaseUserId}")
                student
            }
            
            val registeredCount = students.count { it.isRegistered }
            val pendingCount = students.count { !it.isRegistered }
            android.util.Log.d("PreRegisteredRepo", "Total: ${students.size}, Registered: $registeredCount, Pending: $pendingCount")
            
            Result.success(students)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get pre-registered students by course
     */
    suspend fun getPreRegisteredStudentsByCourse(courseId: String): Result<List<PreRegisteredStudent>> {
        return try {
            val snapshot = studentsCollection
                .whereEqualTo("courseId", courseId)
                .orderBy("studentId", Query.Direction.ASCENDING)
                .get()
                .await()
            
            val students = snapshot.documents.map { convertDocumentToPreRegisteredStudent(it) }
            Result.success(students)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get pre-registered students by registration status
     * Note: Fetches all students and filters client-side to avoid index issues
     */
    suspend fun getPreRegisteredStudentsByStatus(isRegistered: Boolean): Result<List<PreRegisteredStudent>> {
        return try {
            android.util.Log.d("PreRegisteredRepo", "=== getPreRegisteredStudentsByStatus ===")
            android.util.Log.d("PreRegisteredRepo", "Filtering for isRegistered: $isRegistered")
            
            // Fetch all students and filter client-side to avoid index issues with "registered" field
            val allStudentsResult = getAllPreRegisteredStudents()
            
            if (allStudentsResult.isFailure) {
                return allStudentsResult.map { emptyList() }
            }
            
            val allStudents = allStudentsResult.getOrThrow()
            val filteredStudents = allStudents.filter { it.isRegistered == isRegistered }
            
            android.util.Log.d("PreRegisteredRepo", "Total students: ${allStudents.size}, Filtered: ${filteredStudents.size}")
            android.util.Log.d("PreRegisteredRepo", "Filtered students:")
            filteredStudents.forEach { student ->
                android.util.Log.d("PreRegisteredRepo", "  - ${student.studentId}: isRegistered=${student.isRegistered}, firebaseUserId=${student.firebaseUserId}")
            }
            
            Result.success(filteredStudents)
        } catch (e: Exception) {
            android.util.Log.e("PreRegisteredRepo", "Error in getPreRegisteredStudentsByStatus: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Add a new pre-registered student
     */
    suspend fun addPreRegisteredStudent(student: PreRegisteredStudent): Result<String> {
        return try {
            // Check if student ID already exists
            val existing = studentsCollection
                .whereEqualTo("studentId", student.studentId)
                .limit(1)
                .get()
                .await()
            
            if (!existing.isEmpty) {
                return Result.failure(Exception("Student ID ${student.studentId} already exists"))
            }
            
            val docRef = studentsCollection.document()
            val studentWithId = student.copy(id = docRef.id)
            docRef.set(studentWithId).await()
            
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update pre-registered student
     */
    suspend fun updatePreRegisteredStudent(student: PreRegisteredStudent): Result<Unit> {
        return try {
            val updated = student.copy(updatedAt = System.currentTimeMillis())
            studentsCollection.document(student.id).set(updated).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete pre-registered student
     */
    suspend fun deletePreRegisteredStudent(docId: String): Result<Unit> {
        return try {
            studentsCollection.document(docId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Mark student as registered
     */
    suspend fun markStudentAsRegistered(
        studentId: String,
        firebaseUserId: String
    ): Result<Unit> {
        return try {
            val snapshot = studentsCollection
                .whereEqualTo("studentId", studentId)
                .limit(1)
                .get()
                .await()
            
            if (snapshot.isEmpty) {
                return Result.failure(Exception("Student not found"))
            }
            
            val docId = snapshot.documents[0].id
            android.util.Log.d("PreRegisteredRepo", "=== markStudentAsRegistered ===")
            android.util.Log.d("PreRegisteredRepo", "Student ID: $studentId, Firebase UID: $firebaseUserId")
            
            // Update both field names for backward compatibility
            studentsCollection.document(docId)
                .update(
                    mapOf(
                        "isRegistered" to true,
                        "registered" to true,  // Also update old field name
                        "registeredAt" to System.currentTimeMillis(),
                        "firebaseUserId" to firebaseUserId
                    )
                )
                .await()
            
            android.util.Log.d("PreRegisteredRepo", "Successfully marked student as registered")
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Bulk add pre-registered students
     */
    suspend fun bulkAddPreRegisteredStudents(students: List<PreRegisteredStudent>): Result<BulkImportResult> {
        return try {
            var successCount = 0
            var failureCount = 0
            val errors = mutableListOf<String>()
            
            // Use batch writes for better performance (max 500 per batch)
            students.chunked(500).forEach { chunk ->
                val batch = firestore.batch()
                
                chunk.forEach { student ->
                    try {
                        // Check for duplicate student ID
                        val existing = studentsCollection
                            .whereEqualTo("studentId", student.studentId)
                            .limit(1)
                            .get()
                            .await()
                        
                        if (existing.isEmpty) {
                            val docRef = studentsCollection.document()
                            val studentWithId = student.copy(id = docRef.id)
                            batch.set(docRef, studentWithId)
                            successCount++
                        } else {
                            failureCount++
                            errors.add("Duplicate student ID: ${student.studentId}")
                        }
                    } catch (e: Exception) {
                        failureCount++
                        errors.add("Error adding ${student.studentId}: ${e.message}")
                    }
                }
                
                batch.commit().await()
            }
            
            Result.success(BulkImportResult(successCount, failureCount, errors))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== TEACHER OPERATIONS ====================
    
    /**
     * Get pre-registered teacher by teacher ID
     */
    suspend fun getPreRegisteredTeacher(teacherId: String): Result<PreRegisteredTeacher> {
        return try {
            // Trim and normalize the teacher ID
            val normalizedTeacherId = teacherId.trim()
            
            android.util.Log.d("PreRegisteredRepo", "=== getPreRegisteredTeacher ===")
            android.util.Log.d("PreRegisteredRepo", "Looking for teacher ID: '$normalizedTeacherId'")
            
            val snapshot = teachersCollection
                .whereEqualTo("teacherId", normalizedTeacherId)
                .limit(1)
                .get()
                .await()
            
            android.util.Log.d("PreRegisteredRepo", "Query returned ${snapshot.size()} documents")
            
            if (snapshot.isEmpty) {
                // Try case-insensitive search as fallback
                android.util.Log.w("PreRegisteredRepo", "No exact match found, trying case-insensitive search")
                val allTeachersResult = getAllPreRegisteredTeachers()
                allTeachersResult.onSuccess { allTeachers ->
                    val matchingTeacher = allTeachers.find { 
                        it.teacherId.trim().equals(normalizedTeacherId, ignoreCase = true) 
                    }
                    if (matchingTeacher != null) {
                        android.util.Log.d("PreRegisteredRepo", "Found teacher with case-insensitive match: ${matchingTeacher.teacherId}")
                        return Result.success(matchingTeacher)
                    }
                }
                
                android.util.Log.e("PreRegisteredRepo", "Teacher ID '$normalizedTeacherId' not found in database")
                return Result.failure(Exception("Teacher ID not found. Please contact administration."))
            }
            
            val teacher = convertDocumentToPreRegisteredTeacher(snapshot.documents[0])
            android.util.Log.d("PreRegisteredRepo", "Successfully found teacher: ${teacher.teacherId}")
            
            Result.success(teacher)
        } catch (e: Exception) {
            android.util.Log.e("PreRegisteredRepo", "Error getting pre-registered teacher: ${e.message}", e)
            android.util.Log.e("PreRegisteredRepo", "Exception type: ${e.javaClass.simpleName}")
            android.util.Log.e("PreRegisteredRepo", "Stack trace: ${e.stackTraceToString()}")
            
            // If it's a permission error, provide a more helpful message
            if (e.message?.contains("PERMISSION_DENIED") == true || 
                e.message?.contains("permission") == true) {
                return Result.failure(Exception("Permission denied. Please ensure the teacher record exists and is not yet activated."))
            }
            
            Result.failure(e)
        }
    }
    
    /**
     * Get pre-registered teacher by document ID
     */
    suspend fun getPreRegisteredTeacherById(docId: String): Result<PreRegisteredTeacher> {
        return try {
            val doc = teachersCollection.document(docId).get().await()
            
            if (!doc.exists()) {
                return Result.failure(Exception("Teacher not found"))
            }
            
            val teacher = convertDocumentToPreRegisteredTeacher(doc)
            
            Result.success(teacher)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all pre-registered teachers
     */
    suspend fun getAllPreRegisteredTeachers(): Result<List<PreRegisteredTeacher>> {
        return try {
            val snapshot = teachersCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val teachers = snapshot.documents.map { convertDocumentToPreRegisteredTeacher(it) }
            Result.success(teachers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get pre-registered teachers by department
     */
    suspend fun getPreRegisteredTeachersByDepartment(departmentCourseId: String): Result<List<PreRegisteredTeacher>> {
        return try {
            val snapshot = teachersCollection
                .whereEqualTo("departmentCourseId", departmentCourseId)
                .orderBy("teacherId", Query.Direction.ASCENDING)
                .get()
                .await()
            
            val teachers = snapshot.documents.map { convertDocumentToPreRegisteredTeacher(it) }
            Result.success(teachers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get pre-registered teachers by registration status
     * Note: Fetches all teachers and filters client-side to avoid index issues
     */
    suspend fun getPreRegisteredTeachersByStatus(isRegistered: Boolean): Result<List<PreRegisteredTeacher>> {
        return try {
            android.util.Log.d("PreRegisteredRepo", "=== getPreRegisteredTeachersByStatus ===")
            android.util.Log.d("PreRegisteredRepo", "Filtering for isRegistered: $isRegistered")
            
            // Fetch all teachers and filter client-side to avoid index issues with "registered" field
            val allTeachersResult = getAllPreRegisteredTeachers()
            
            if (allTeachersResult.isFailure) {
                return allTeachersResult.map { emptyList() }
            }
            
            val allTeachers = allTeachersResult.getOrThrow()
            val filteredTeachers = allTeachers.filter { it.isRegistered == isRegistered }
            
            android.util.Log.d("PreRegisteredRepo", "Total teachers: ${allTeachers.size}, Filtered: ${filteredTeachers.size}")
            
            Result.success(filteredTeachers)
        } catch (e: Exception) {
            android.util.Log.e("PreRegisteredRepo", "Error in getPreRegisteredTeachersByStatus: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Add a new pre-registered teacher
     */
    suspend fun addPreRegisteredTeacher(teacher: PreRegisteredTeacher): Result<String> {
        return try {
            // Check if teacher ID already exists
            val existing = teachersCollection
                .whereEqualTo("teacherId", teacher.teacherId)
                .limit(1)
                .get()
                .await()
            
            if (!existing.isEmpty) {
                return Result.failure(Exception("Teacher ID ${teacher.teacherId} already exists"))
            }
            
            val docRef = teachersCollection.document()
            val teacherWithId = teacher.copy(id = docRef.id)
            docRef.set(teacherWithId).await()
            
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update pre-registered teacher
     */
    suspend fun updatePreRegisteredTeacher(teacher: PreRegisteredTeacher): Result<Unit> {
        return try {
            val updated = teacher.copy(updatedAt = System.currentTimeMillis())
            teachersCollection.document(teacher.id).set(updated).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete pre-registered teacher
     */
    suspend fun deletePreRegisteredTeacher(docId: String): Result<Unit> {
        return try {
            teachersCollection.document(docId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Mark teacher as registered
     */
    suspend fun markTeacherAsRegistered(
        teacherId: String,
        firebaseUserId: String
    ): Result<Unit> {
        return try {
            val snapshot = teachersCollection
                .whereEqualTo("teacherId", teacherId)
                .limit(1)
                .get()
                .await()
            
            if (snapshot.isEmpty) {
                return Result.failure(Exception("Teacher not found"))
            }
            
            val docId = snapshot.documents[0].id
            android.util.Log.d("PreRegisteredRepo", "=== markTeacherAsRegistered ===")
            android.util.Log.d("PreRegisteredRepo", "Teacher ID: $teacherId, Firebase UID: $firebaseUserId")
            
            // Update both field names for backward compatibility
            teachersCollection.document(docId)
                .update(
                    mapOf(
                        "isRegistered" to true,
                        "registered" to true,  // Also update old field name
                        "registeredAt" to System.currentTimeMillis(),
                        "firebaseUserId" to firebaseUserId
                    )
                )
                .await()
            
            android.util.Log.d("PreRegisteredRepo", "Successfully marked teacher as registered")
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Bulk add pre-registered teachers
     */
    suspend fun bulkAddPreRegisteredTeachers(teachers: List<PreRegisteredTeacher>): Result<BulkImportResult> {
        return try {
            var successCount = 0
            var failureCount = 0
            val errors = mutableListOf<String>()
            
            // Use batch writes for better performance (max 500 per batch)
            teachers.chunked(500).forEach { chunk ->
                val batch = firestore.batch()
                
                chunk.forEach { teacher ->
                    try {
                        // Check for duplicate teacher ID
                        val existing = teachersCollection
                            .whereEqualTo("teacherId", teacher.teacherId)
                            .limit(1)
                            .get()
                            .await()
                        
                        if (existing.isEmpty) {
                            val docRef = teachersCollection.document()
                            val teacherWithId = teacher.copy(id = docRef.id)
                            batch.set(docRef, teacherWithId)
                            successCount++
                        } else {
                            failureCount++
                            errors.add("Duplicate teacher ID: ${teacher.teacherId}")
                        }
                    } catch (e: Exception) {
                        failureCount++
                        errors.add("Error adding ${teacher.teacherId}: ${e.message}")
                    }
                }
                
                batch.commit().await()
            }
            
            Result.success(BulkImportResult(successCount, failureCount, errors))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== COMMON OPERATIONS ====================
    
    /**
     * Check if a user ID exists in pre-registration (for either students or teachers)
     */
    suspend fun checkIdExists(userId: String, userType: UserRole): Result<Boolean> {
        return try {
            val collection = when (userType) {
                UserRole.STUDENT -> studentsCollection
                UserRole.TEACHER -> teachersCollection
                else -> return Result.success(false)
            }
            
            val field = when (userType) {
                UserRole.STUDENT -> "studentId"
                UserRole.TEACHER -> "teacherId"
                else -> return Result.success(false)
            }
            
            val snapshot = collection
                .whereEqualTo(field, userId)
                .limit(1)
                .get()
                .await()
            
            Result.success(!snapshot.isEmpty)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Result of bulk import operation
 */
data class BulkImportResult(
    val successCount: Int,
    val failureCount: Int,
    val errors: List<String>
) {
    val totalProcessed: Int
        get() = successCount + failureCount
    
    val hasErrors: Boolean
        get() = failureCount > 0
}

