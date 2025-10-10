package com.smartacademictracker.utils

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

/**
 * Firestore Data Clearing Utility
 * This utility will delete ALL data from your Firestore database
 * WARNING: This action is irreversible!
 */
class FirestoreDataClearer {
    
    private val firestore = FirebaseFirestore.getInstance()
    
    // List of ALL collections to clear (including system/structural data)
    private val collectionsToClear = listOf(
        "users",                    // All user accounts (including admin)
        "subjects",                 // Subject definitions created by users
        "grades",                   // All grades entered by teachers
        "enrollments",              // Student enrollments
        "student_applications",     // Student applications
        "subject_applications",     // Subject applications
        "teacher_applications",     // Teacher applications
        "courses",                  // Course definitions created by users
        "year_levels",             // Year level definitions created by users
        "grade_aggregates",        // Calculated grade aggregates
        "academic_periods",        // Academic period definitions created by users
        "audit_trails",           // User activity logs
        "system_config",          // System configuration (will be reset)
        "notifications",           // User notifications
        "notification_preferences", // User notification settings
        "notification_stats",     // User notification statistics
        "fcm_tokens"              // User device tokens
    )
    
    /**
     * Clear all data from Firestore database
     */
    suspend fun clearAllData(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            println("🚀 Starting Firestore data clearing process...")
            println("⚠️  WARNING: This will delete ALL data from your Firestore database!")
            println("⚠️  This includes ALL user accounts, subjects, courses, academic periods, and system data!")
            println("⚠️  This action is IRREVERSIBLE!\n")
            
            var totalDeleted = 0
            val startTime = System.currentTimeMillis()
            
            for (collectionName in collectionsToClear) {
                val deleted = deleteCollection(collectionName)
                totalDeleted += deleted
            }
            
            val endTime = System.currentTimeMillis()
            val duration = (endTime - startTime) / 1000.0
            
            println("\n🎉 Firestore data clearing completed successfully!")
            println("📊 Total documents deleted: $totalDeleted")
            println("⏱️  Time taken: ${String.format("%.2f", duration)} seconds")
            
            Result.success(totalDeleted)
        } catch (e: Exception) {
            println("\n❌ Error during data clearing: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Delete all documents from a specific collection
     */
    private suspend fun deleteCollection(collectionName: String): Int {
        println("🗑️  Clearing collection: $collectionName")
        
        return try {
            val collectionRef = firestore.collection(collectionName)
            val snapshot = collectionRef.get().await()
            
            if (snapshot.isEmpty) {
                println("   ✅ Collection $collectionName is already empty")
                return 0
            }
            
            println("   📄 Found ${snapshot.size()} documents in $collectionName")
            
            var deletedCount = 0
            val batch = firestore.batch()
            var batchCount = 0
            
            // Delete documents in batches (Firestore batch limit is 500)
            for (document in snapshot.documents) {
                batch.delete(document.reference)
                deletedCount++
                batchCount++
                
                // Commit batch when it reaches 500 operations or at the end
                if (batchCount >= 500 || deletedCount == snapshot.size()) {
                    batch.commit().await()
                    println("   ✅ Deleted $batchCount documents from $collectionName")
                    batchCount = 0
                }
            }
            
            println("   🎉 Successfully cleared $collectionName: $deletedCount documents deleted")
            deletedCount
            
        } catch (e: Exception) {
            println("   ❌ Error clearing $collectionName: ${e.message}")
            throw e
        }
    }
    
    /**
     * Clear specific collections only
     */
    suspend fun clearSpecificCollections(collections: List<String>): Result<Int> = withContext(Dispatchers.IO) {
        try {
            println("🚀 Starting selective Firestore data clearing...")
            println("⚠️  WARNING: This will delete data from specified collections!")
            println("⚠️  This action is IRREVERSIBLE!\n")
            
            var totalDeleted = 0
            val startTime = System.currentTimeMillis()
            
            for (collectionName in collections) {
                if (collectionsToClear.contains(collectionName)) {
                    val deleted = deleteCollection(collectionName)
                    totalDeleted += deleted
                } else {
                    println("   ⚠️  Collection $collectionName not found in allowed list")
                }
            }
            
            val endTime = System.currentTimeMillis()
            val duration = (endTime - startTime) / 1000.0
            
            println("\n🎉 Selective Firestore data clearing completed!")
            println("📊 Total documents deleted: $totalDeleted")
            println("⏱️  Time taken: ${String.format("%.2f", duration)} seconds")
            
            Result.success(totalDeleted)
        } catch (e: Exception) {
            println("\n❌ Error during selective data clearing: ${e.message}")
            Result.failure(e)
        }
    }
}

/**
 * Usage example:
 * 
 * // Clear all data
 * val clearer = FirestoreDataClearer()
 * clearer.clearAllData()
 * 
 * // Clear specific collections
 * clearer.clearSpecificCollections(listOf("users", "grades", "subjects"))
 */
