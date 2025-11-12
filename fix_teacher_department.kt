/**
 * Script to fix a specific teacher's department in Firestore
 * 
 * This script updates a teacher's departmentCourseId to the CHTM course.
 * 
 * Usage:
 * 1. Update the teacherEmail variable with the teacher's email
 * 2. Run this script using Firebase Admin SDK or Firebase CLI
 */

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

suspend fun fixTeacherDepartment(teacherEmail: String) {
    val firestore = FirebaseFirestore.getInstance()
    val usersCollection = firestore.collection("users")
    val coursesCollection = firestore.collection("courses")
    
    try {
        // Find the teacher by email
        val teacherQuery = usersCollection
            .whereEqualTo("email", teacherEmail)
            .whereEqualTo("role", "TEACHER")
            .get()
            .await()
        
        if (teacherQuery.isEmpty) {
            println("ERROR: Teacher with email '$teacherEmail' not found")
            return
        }
        
        val teacherDoc = teacherQuery.documents.first()
        val teacherId = teacherDoc.id
        val teacherName = "${teacherDoc.getString("firstName")} ${teacherDoc.getString("lastName")}"
        
        println("Found teacher: $teacherName (ID: $teacherId)")
        println("Current departmentCourseId: ${teacherDoc.getString("departmentCourseId")}")
        
        // Find CHTM course
        val coursesSnapshot = coursesCollection.get().await()
        val chtmCourse = coursesSnapshot.documents.find { doc ->
            val courseName = doc.getString("name") ?: ""
            val courseCode = doc.getString("code") ?: ""
            courseName.uppercase().contains("CHTM") || 
            courseCode.uppercase().contains("CHTM") ||
            courseName.uppercase().contains("HOSPITALITY") ||
            courseName.uppercase().contains("TOURISM")
        }
        
        if (chtmCourse == null) {
            println("ERROR: CHTM course not found. Available courses:")
            coursesSnapshot.documents.forEach { doc ->
                println("  - ${doc.getString("name")} (${doc.getString("code")}) - ID: ${doc.id}")
            }
            return
        }
        
        val chtmCourseId = chtmCourse.id
        val chtmCourseName = chtmCourse.getString("name") ?: "Unknown"
        val chtmCourseCode = chtmCourse.getString("code") ?: "Unknown"
        
        println("Found CHTM course: $chtmCourseName ($chtmCourseCode) - ID: $chtmCourseId")
        
        // Update teacher's departmentCourseId
        teacherDoc.reference.update("departmentCourseId", chtmCourseId).await()
        
        println("\n✅ Successfully updated teacher '$teacherName' ($teacherEmail)")
        println("   Department set to: $chtmCourseName ($chtmCourseCode)")
        println("   departmentCourseId: $chtmCourseId")
        
    } catch (e: Exception) {
        println("ERROR: Failed to update teacher department: ${e.message}")
        e.printStackTrace()
    }
}

// Example usage:
// fixTeacherDepartment("teacher@example.com")

