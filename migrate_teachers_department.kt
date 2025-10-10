/**
 * Migration Script: Update Existing Teachers with Department Course ID
 * 
 * This script updates all existing teachers in Firestore to set their departmentCourseId.
 * 
 * IMPORTANT: 
 * - Run this script ONCE after deploying the new code
 * - You need to manually set the departmentCourseId for each teacher based on their department
 * - This script provides a template - you'll need to customize it based on your actual data
 * 
 * Usage:
 * 1. Open Firebase Console
 * 2. Go to Firestore Database
 * 3. Run this script in Firebase Functions or use the Admin SDK
 * 4. Or manually update each teacher document with departmentCourseId field
 * 
 * Example: If a teacher teaches BSIT subjects, set their departmentCourseId to the BSIT course ID
 */

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

suspend fun migrateTeachersDepartment() {
    val firestore = FirebaseFirestore.getInstance()
    val usersCollection = firestore.collection("users")
    
    try {
        // Get all teachers
        val teachersSnapshot = usersCollection
            .whereEqualTo("role", "TEACHER")
            .get()
            .await()
        
        println("Found ${teachersSnapshot.documents.size} teachers to migrate")
        
        // Map of teacher emails to their department course IDs
        // TODO: Update this map with actual teacher-to-department mappings
        val teacherDepartmentMap = mapOf(
            // Example:
            // "teacher1@example.com" to "bsit_course_id",
            // "teacher2@example.com" to "nursing_course_id",
            // Add all your teachers here
        )
        
        var updatedCount = 0
        var skippedCount = 0
        
        for (document in teachersSnapshot.documents) {
            val teacherEmail = document.getString("email") ?: continue
            val currentDepartmentId = document.getString("departmentCourseId")
            
            // Skip if already has departmentCourseId
            if (currentDepartmentId != null && currentDepartmentId.isNotEmpty()) {
                println("Skipping ${teacherEmail}: already has departmentCourseId")
                skippedCount++
                continue
            }
            
            // Get department from map
            val departmentCourseId = teacherDepartmentMap[teacherEmail]
            
            if (departmentCourseId != null) {
                // Update teacher with departmentCourseId
                document.reference.update("departmentCourseId", departmentCourseId).await()
                println("Updated ${teacherEmail} with departmentCourseId: $departmentCourseId")
                updatedCount++
            } else {
                println("WARNING: No department mapping found for ${teacherEmail}. Please set manually.")
                skippedCount++
            }
        }
        
        println("\nMigration complete!")
        println("Updated: $updatedCount")
        println("Skipped: $skippedCount")
        
    } catch (e: Exception) {
        println("Error during migration: ${e.message}")
        throw e
    }
}

/**
 * Alternative: Manual Migration Instructions
 * 
 * To manually update teachers in Firebase Console:
 * 1. Go to Firestore Database
 * 2. Navigate to "users" collection
 * 3. Filter by role = "TEACHER"
 * 4. For each teacher:
 *    - Click on the document
 *    - Add field: "departmentCourseId" (type: string)
 *    - Set value to the course ID of their department (e.g., BSIT course ID, Nursing course ID)
 *    - Save
 */

