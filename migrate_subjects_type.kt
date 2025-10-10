/**
 * Migration Script: Set Subject Type for Existing Subjects
 * 
 * This script updates all existing subjects in Firestore to set their subjectType.
 * By default, all subjects will be set to MAJOR unless specified otherwise.
 * 
 * IMPORTANT: 
 * - Run this script ONCE after deploying the new code
 * - Subjects default to MAJOR type (only teachers of that course can see/apply)
 * - MINOR subjects (visible to all teachers) need to be identified and updated
 * 
 * Usage:
 * 1. Open Firebase Console
 * 2. Go to Firestore Database
 * 3. Run this script in Firebase Functions or use the Admin SDK
 * 4. Or manually update each subject document with subjectType field
 */

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

suspend fun migrateSubjectsType() {
    val firestore = FirebaseFirestore.getInstance()
    val subjectsCollection = firestore.collection("subjects")
    
    try {
        // Get all subjects
        val subjectsSnapshot = subjectsCollection
            .get()
            .await()
        
        println("Found ${subjectsSnapshot.documents.size} subjects to migrate")
        
        // List of subject codes/names that should be MINOR (visible to all teachers)
        // TODO: Update this list with actual minor subjects (e.g., general education subjects)
        val minorSubjectCodes = setOf(
            // Example:
            // "GE101", // General Education subjects
            // "PE101", // Physical Education
            // Add your minor subjects here
        )
        
        var updatedCount = 0
        var skippedCount = 0
        
        for (document in subjectsSnapshot.documents) {
            val subjectCode = document.getString("code") ?: ""
            val currentSubjectType = document.getString("subjectType")
            
            // Skip if already has subjectType
            if (currentSubjectType != null && currentSubjectType.isNotEmpty()) {
                println("Skipping ${subjectCode}: already has subjectType")
                skippedCount++
                continue
            }
            
            // Determine subject type
            val subjectType = if (minorSubjectCodes.contains(subjectCode)) {
                "MINOR"
            } else {
                "MAJOR" // Default to MAJOR
            }
            
            // Update subject with subjectType
            document.reference.update("subjectType", subjectType).await()
            println("Updated ${subjectCode} with subjectType: $subjectType")
            updatedCount++
        }
        
        println("\nMigration complete!")
        println("Updated: $updatedCount")
        println("Skipped: $skippedCount")
        println("\nNote: All subjects defaulted to MAJOR. Review and update MINOR subjects as needed.")
        
    } catch (e: Exception) {
        println("Error during migration: ${e.message}")
        throw e
    }
}

/**
 * Alternative: Manual Migration Instructions
 * 
 * To manually update subjects in Firebase Console:
 * 1. Go to Firestore Database
 * 2. Navigate to "subjects" collection
 * 3. For each subject:
 *    - Click on the document
 *    - Add field: "subjectType" (type: string)
 *    - Set value to:
 *      - "MAJOR" for course-specific subjects (default)
 *      - "MINOR" for general education subjects (visible to all teachers)
 *    - Save
 * 
 * Examples:
 * - BSIT Programming subjects → "MAJOR"
 * - Nursing Anatomy subjects → "MAJOR"
 * - General Education (English, Math, PE) → "MINOR"
 */

