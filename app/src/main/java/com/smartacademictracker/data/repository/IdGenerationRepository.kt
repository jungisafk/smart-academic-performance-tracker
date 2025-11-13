package com.smartacademictracker.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.smartacademictracker.util.IdValidator
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for auto-generating IDs based on active academic period
 * IDs follow format: {year}-{sequential} for students or {prefix}-{year}-{sequential} for teachers/admins
 * Sequential numbers start from 001 with no upper limit
 */
@Singleton
class IdGenerationRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    
    private val TAG = "IdGenerationRepo"
    
    /**
     * Generate next available student ID
     * Format: YYYY-NNN (e.g., 2025-001, 2025-002, ...)
     * Year is taken from active academic period
     */
    suspend fun generateNextStudentId(): Result<String> {
        return try {
            // Get active academic period
            val activeYear = getActiveAcademicYear() ?: run {
                return Result.failure(Exception("No active academic period found. Please set an active academic period first."))
            }
            
            Log.d(TAG, "Generating student ID for year: $activeYear")
            
            // Find highest sequential number for this year
            val highestNumber = findHighestStudentSequentialNumber(activeYear)
            
            Log.d(TAG, "Highest student number for $activeYear: $highestNumber")
            
            // Generate next ID
            val nextId = IdValidator.generateNextStudentId(activeYear, highestNumber)
            
            Log.d(TAG, "Generated student ID: $nextId")
            
            Result.success(nextId)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating student ID", e)
            Result.failure(e)
        }
    }
    
    /**
     * Generate next available teacher ID
     * Format: T-YYYY-NNN (e.g., T-2025-001, T-2025-002, ...)
     * Year is taken from active academic period
     */
    suspend fun generateNextTeacherId(): Result<String> {
        return try {
            // Get active academic period
            val activeYear = getActiveAcademicYear() ?: run {
                return Result.failure(Exception("No active academic period found. Please set an active academic period first."))
            }
            
            Log.d(TAG, "Generating teacher ID for year: $activeYear")
            
            // Find highest sequential number for this year
            val highestNumber = findHighestTeacherSequentialNumber(activeYear)
            
            Log.d(TAG, "Highest teacher number for $activeYear: $highestNumber")
            
            // Generate next ID
            val nextId = IdValidator.generateNextTeacherId(activeYear, highestNumber)
            
            Log.d(TAG, "Generated teacher ID: $nextId")
            
            Result.success(nextId)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating teacher ID", e)
            Result.failure(e)
        }
    }
    
    /**
     * Generate next available admin ID
     * Format: A-YYYY-NNN (e.g., A-2025-001, A-2025-002, ...)
     * Year is taken from active academic period
     */
    suspend fun generateNextAdminId(): Result<String> {
        return try {
            // Get active academic period
            val activeYear = getActiveAcademicYear() ?: run {
                return Result.failure(Exception("No active academic period found. Please set an active academic period first."))
            }
            
            Log.d(TAG, "Generating admin ID for year: $activeYear")
            
            // Find highest sequential number for this year
            val highestNumber = findHighestAdminSequentialNumber(activeYear)
            
            Log.d(TAG, "Highest admin number for $activeYear: $highestNumber")
            
            // Generate next ID
            val nextId = IdValidator.generateNextAdminId(activeYear, highestNumber)
            
            Log.d(TAG, "Generated admin ID: $nextId")
            
            Result.success(nextId)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating admin ID", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get the year from the active academic period
     * For academic year "2025-2026", returns 2025
     */
    private suspend fun getActiveAcademicYear(): Int? {
        return try {
            val snapshot = firestore.collection("academicPeriods")
                .whereEqualTo("isActive", true)
                .limit(1)
                .get()
                .await()
            
            if (snapshot.documents.isNotEmpty()) {
                val academicYear = snapshot.documents[0].getString("academicYear")
                // Extract first year from format "2025-2026"
                val year = academicYear?.split("-")?.firstOrNull()?.toIntOrNull()
                Log.d(TAG, "Active academic year: $academicYear, extracted year: $year")
                year
            } else {
                Log.w(TAG, "No active academic period found")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting active academic year", e)
            null
        }
    }
    
    /**
     * Find the highest sequential number for student IDs in a given year
     */
    private suspend fun findHighestStudentSequentialNumber(year: Int): Int {
        return try {
            // Query all users with student IDs starting with the year
            val yearPrefix = "$year-"
            
            val usersSnapshot = firestore.collection("users")
                .whereEqualTo("role", "Student")
                .get()
                .await()
            
            val preRegisteredSnapshot = firestore.collection("preRegisteredStudents")
                .get()
                .await()
            
            var highestNumber = 0
            
            // Check activated students
            usersSnapshot.documents.forEach { doc ->
                val studentId = doc.getString("studentId") ?: return@forEach
                if (studentId.startsWith(yearPrefix)) {
                    val sequentialNumber = IdValidator.extractSequentialNumberFromStudentId(studentId)
                    if (sequentialNumber != null && sequentialNumber > highestNumber) {
                        highestNumber = sequentialNumber
                    }
                }
            }
            
            // Check pre-registered students
            preRegisteredSnapshot.documents.forEach { doc ->
                val studentId = doc.getString("studentId") ?: return@forEach
                if (studentId.startsWith(yearPrefix)) {
                    val sequentialNumber = IdValidator.extractSequentialNumberFromStudentId(studentId)
                    if (sequentialNumber != null && sequentialNumber > highestNumber) {
                        highestNumber = sequentialNumber
                    }
                }
            }
            
            Log.d(TAG, "Highest student sequential number for $year: $highestNumber")
            highestNumber
        } catch (e: Exception) {
            Log.e(TAG, "Error finding highest student sequential number", e)
            0
        }
    }
    
    /**
     * Find the highest sequential number for teacher IDs in a given year
     */
    private suspend fun findHighestTeacherSequentialNumber(year: Int): Int {
        return try {
            val yearPrefix = "T-$year-"
            
            val usersSnapshot = firestore.collection("users")
                .whereEqualTo("role", "Teacher")
                .get()
                .await()
            
            val preRegisteredSnapshot = firestore.collection("preRegisteredTeachers")
                .get()
                .await()
            
            var highestNumber = 0
            
            // Check activated teachers
            usersSnapshot.documents.forEach { doc ->
                val teacherId = doc.getString("teacherId") ?: return@forEach
                if (teacherId.startsWith(yearPrefix)) {
                    val sequentialNumber = IdValidator.extractSequentialNumberFromTeacherId(teacherId)
                    if (sequentialNumber != null && sequentialNumber > highestNumber) {
                        highestNumber = sequentialNumber
                    }
                }
            }
            
            // Check pre-registered teachers
            preRegisteredSnapshot.documents.forEach { doc ->
                val teacherId = doc.getString("teacherId") ?: return@forEach
                if (teacherId.startsWith(yearPrefix)) {
                    val sequentialNumber = IdValidator.extractSequentialNumberFromTeacherId(teacherId)
                    if (sequentialNumber != null && sequentialNumber > highestNumber) {
                        highestNumber = sequentialNumber
                    }
                }
            }
            
            Log.d(TAG, "Highest teacher sequential number for $year: $highestNumber")
            highestNumber
        } catch (e: Exception) {
            Log.e(TAG, "Error finding highest teacher sequential number", e)
            0
        }
    }
    
    /**
     * Find the highest sequential number for admin IDs in a given year
     */
    private suspend fun findHighestAdminSequentialNumber(year: Int): Int {
        return try {
            val yearPrefix = "A-$year-"
            
            val usersSnapshot = firestore.collection("users")
                .whereEqualTo("role", "Admin")
                .get()
                .await()
            
            var highestNumber = 0
            
            usersSnapshot.documents.forEach { doc ->
                val adminId = doc.getString("adminId") ?: return@forEach
                if (adminId.startsWith(yearPrefix)) {
                    val sequentialNumber = IdValidator.extractSequentialNumberFromAdminId(adminId)
                    if (sequentialNumber != null && sequentialNumber > highestNumber) {
                        highestNumber = sequentialNumber
                    }
                }
            }
            
            Log.d(TAG, "Highest admin sequential number for $year: $highestNumber")
            highestNumber
        } catch (e: Exception) {
            Log.e(TAG, "Error finding highest admin sequential number", e)
            0
        }
    }
}

