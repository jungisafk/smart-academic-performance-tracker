package com.smartacademictracker.data.migration

import com.google.firebase.firestore.FirebaseFirestore
import com.smartacademictracker.data.model.Course
import com.smartacademictracker.data.model.YearLevel
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseMigrationService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    
    suspend fun migrateYearLevelsToCourses(): Result<String> {
        return try {
            println("DEBUG: DatabaseMigrationService - Starting year level migration...")
            
            // Get all courses
            val coursesSnapshot = firestore.collection("courses").get().await()
            val courses = coursesSnapshot.toObjects(Course::class.java)
            
            if (courses.isEmpty()) {
                return Result.failure(Exception("No courses found. Please create courses first."))
            }
            
            println("DEBUG: DatabaseMigrationService - Found ${courses.size} courses")
            
            // Get all year levels without courseId
            val yearLevelsSnapshot = firestore.collection("year_levels").get().await()
            val yearLevels = yearLevelsSnapshot.toObjects(YearLevel::class.java)
                .filter { it.courseId.isEmpty() }
            
            if (yearLevels.isEmpty()) {
                return Result.success("No year levels need migration")
            }
            
            println("DEBUG: DatabaseMigrationService - Found ${yearLevels.size} year levels without courseId")
            
            // Try to intelligently assign year levels to courses based on name matching
            // First, try to match year level names to course names
            val courseMap = courses.associateBy { it.name.lowercase() }
            
            // Common course name patterns
            val coursePatterns = mapOf(
                "it" to listOf("information technology", "it", "ict"),
                "chtm" to listOf("chtm", "hospitality", "tourism"),
                "bsit" to listOf("bsit", "bachelor of science in information technology"),
                "bsba" to listOf("bsba", "bachelor of science in business administration")
            )
            
            val defaultCourse = courses.first()
            println("DEBUG: DatabaseMigrationService - Found courses: ${courses.map { "${it.name} (${it.id})" }}")
            println("DEBUG: DatabaseMigrationService - Default course: ${defaultCourse.name} (${defaultCourse.id})")
            
            var migratedCount = 0
            for (yearLevel in yearLevels) {
                // Try to find the best course match
                var targetCourse = defaultCourse
                
                // Look for exact course name match
                val exactMatch = courseMap[yearLevel.name.lowercase()]
                if (exactMatch != null) {
                    targetCourse = exactMatch
                } else {
                    // Look for pattern matches
                    for ((courseKey, patterns) in coursePatterns) {
                        val matchingCourse = courses.find { it.name.lowercase().contains(courseKey) }
                        if (matchingCourse != null && patterns.any { yearLevel.name.lowercase().contains(it) }) {
                            targetCourse = matchingCourse
                            break
                        }
                    }
                }
                
                val updatedYearLevel = yearLevel.copy(courseId = targetCourse.id)
                firestore.collection("year_levels")
                    .document(yearLevel.id)
                    .set(updatedYearLevel)
                    .await()
                migratedCount++
                println("DEBUG: DatabaseMigrationService - Migrated year level: ${yearLevel.name} to course: ${targetCourse.name} (${targetCourse.id})")
            }
            
            Result.success("Successfully migrated $migratedCount year levels to course: ${defaultCourse.name}")
            
        } catch (e: Exception) {
            println("DEBUG: DatabaseMigrationService - Migration failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun migrateSubjectsToYearLevels(): Result<String> {
        return try {
            println("DEBUG: DatabaseMigrationService - Starting subject migration...")
            
            // Get all year levels
            val yearLevelsSnapshot = firestore.collection("year_levels").get().await()
            val yearLevels = yearLevelsSnapshot.toObjects(YearLevel::class.java)
            
            if (yearLevels.isEmpty()) {
                return Result.failure(Exception("No year levels found. Please create year levels first."))
            }
            
            // Get all subjects without yearLevelId
            val subjectsSnapshot = firestore.collection("subjects").get().await()
            val subjects = try {
                subjectsSnapshot.toObjects(com.smartacademictracker.data.model.Subject::class.java)
                    .filter { it.yearLevelId.isEmpty() }
            } catch (e: Exception) {
                println("DEBUG: DatabaseMigrationService - Error deserializing subjects: ${e.message}")
                // Try to handle subjects with invalid semester values
                emptyList()
            }
            
            if (subjects.isEmpty()) {
                return Result.success("No subjects need migration")
            }
            
            println("DEBUG: DatabaseMigrationService - Found ${subjects.size} subjects without yearLevelId")
            
            // For each subject, assign it to the first available year level
            // In a real scenario, you might want to ask the user which year level to assign to
            val defaultYearLevel = yearLevels.first()
            println("DEBUG: DatabaseMigrationService - Assigning all subjects to year level: ${defaultYearLevel.name} (${defaultYearLevel.id})")
            
            var migratedCount = 0
            for (subject in subjects) {
                val updatedSubject = subject.copy(
                    yearLevelId = defaultYearLevel.id,
                    courseId = defaultYearLevel.courseId
                )
                firestore.collection("subjects")
                    .document(subject.id)
                    .set(updatedSubject)
                    .await()
                migratedCount++
                println("DEBUG: DatabaseMigrationService - Migrated subject: ${subject.name} to year level: ${defaultYearLevel.name}")
            }
            
            Result.success("Successfully migrated $migratedCount subjects to year level: ${defaultYearLevel.name}")
            
        } catch (e: Exception) {
            println("DEBUG: DatabaseMigrationService - Subject migration failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun redistributeYearLevels(): Result<String> {
        return try {
            println("DEBUG: DatabaseMigrationService - Starting year level redistribution...")
            
            // Get all courses
            val coursesSnapshot = firestore.collection("courses").get().await()
            val courses = coursesSnapshot.toObjects(Course::class.java)
            
            if (courses.isEmpty()) {
                return Result.failure(Exception("No courses found."))
            }
            
            // Get all year levels
            val yearLevelsSnapshot = firestore.collection("year_levels").get().await()
            val yearLevels = yearLevelsSnapshot.toObjects(YearLevel::class.java)
            
            println("DEBUG: DatabaseMigrationService - Found ${courses.size} courses and ${yearLevels.size} year levels")
            
            // Find the main courses (CHTM and INFORMATION TECHNOLOGY)
            val chtmCourse = courses.find { it.name.uppercase().contains("CHTM") }
            val itCourse = courses.find { it.name.uppercase().contains("INFORMATION TECHNOLOGY") || it.name.uppercase().contains("IT") }
            
            if (chtmCourse == null || itCourse == null) {
                return Result.failure(Exception("Required courses (CHTM and IT) not found"))
            }
            
            println("DEBUG: DatabaseMigrationService - CHTM Course: ${chtmCourse.name} (${chtmCourse.id})")
            println("DEBUG: DatabaseMigrationService - IT Course: ${itCourse.name} (${itCourse.id})")
            
            // Redistribute year levels - assign half to CHTM and half to IT
            val halfPoint = yearLevels.size / 2
            var migratedCount = 0
            
            for (i in yearLevels.indices) {
                val yearLevel = yearLevels[i]
                val targetCourse = if (i < halfPoint) chtmCourse else itCourse
                
                val updatedYearLevel = yearLevel.copy(courseId = targetCourse.id)
                firestore.collection("year_levels")
                    .document(yearLevel.id)
                    .set(updatedYearLevel)
                    .await()
                migratedCount++
                println("DEBUG: DatabaseMigrationService - Redistributed year level: ${yearLevel.name} to course: ${targetCourse.name} (${targetCourse.id})")
            }
            
            Result.success("Successfully redistributed $migratedCount year levels between CHTM and IT courses")
            
        } catch (e: Exception) {
            println("DEBUG: DatabaseMigrationService - Redistribution failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun runFullMigration(): Result<String> {
        return try {
            val yearLevelResult = migrateYearLevelsToCourses()
            if (yearLevelResult.isFailure) {
                return yearLevelResult
            }
            
            val subjectResult = migrateSubjectsToYearLevels()
            if (subjectResult.isFailure) {
                return subjectResult
            }
            
            Result.success("Full migration completed successfully")
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
