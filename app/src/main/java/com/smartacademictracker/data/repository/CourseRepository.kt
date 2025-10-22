package com.smartacademictracker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.smartacademictracker.data.model.Course
import com.smartacademictracker.data.model.YearLevel
import com.smartacademictracker.data.service.AcademicPeriodFilterService
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CourseRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val academicPeriodFilterService: AcademicPeriodFilterService,
    private val yearLevelRepository: YearLevelRepository
) {
    private val coursesCollection = firestore.collection("courses")

    suspend fun createCourse(course: Course): Result<Course> {
        return try {
            // Check if there's an active academic period
            if (!academicPeriodFilterService.hasActiveAcademicPeriod()) {
                return Result.failure(Exception("No active academic period found. Please create an academic period first."))
            }
            
            // Get the active academic period ID
            val activePeriodId = academicPeriodFilterService.getActiveAcademicPeriodId()
            
            // Create course with active academic period
            val courseWithPeriod = course.copy(academicPeriodId = activePeriodId)
            val docRef = coursesCollection.add(courseWithPeriod).await()
            val createdCourse = courseWithPeriod.copy(id = docRef.id)
            coursesCollection.document(docRef.id).set(createdCourse).await()
            
            // Automatically create year levels based on course duration
            createYearLevelsForCourse(createdCourse)
            
            Result.success(createdCourse)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllCourses(): Result<List<Course>> {
        return try {
            val snapshot = coursesCollection
                .whereEqualTo("active", true)
                .get()
                .await()
            val courses = snapshot.toObjects(Course::class.java)
            // Sort in memory to avoid composite index requirement
            val sortedCourses = courses.sortedBy { it.name }
            Result.success(sortedCourses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCourseById(courseId: String): Result<Course?> {
        return try {
            val document = coursesCollection.document(courseId).get().await()
            if (document.exists()) {
                val course = document.toObject(Course::class.java)
                Result.success(course)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCourse(course: Course): Result<Course> {
        return try {
            coursesCollection.document(course.id).set(course).await()
            Result.success(course)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCourse(courseId: String): Result<Unit> {
        return try {
            coursesCollection.document(courseId).update("active", false).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCourseByCode(code: String): Result<Course?> {
        return try {
            val snapshot = coursesCollection
                .whereEqualTo("code", code)
                .whereEqualTo("active", true)
                .get()
                .await()
            
            if (snapshot.isEmpty) {
                Result.success(null)
            } else {
                val course = snapshot.documents.first().toObject(Course::class.java)
                Result.success(course)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Automatically create year levels for a course based on its duration
     */
    private suspend fun createYearLevelsForCourse(course: Course) {
        try {
            println("DEBUG: CourseRepository - Creating ${course.duration} year levels for course: ${course.name}")
            
            for (year in 1..course.duration) {
                val yearLevel = YearLevel(
                    courseId = course.id,
                    name = getYearLevelName(year),
                    level = year,
                    description = "Year $year of ${course.name}",
                    hasSummerClass = year < course.duration, // Summer classes for all years except the last
                    academicPeriodId = course.academicPeriodId
                )
                
                val result = yearLevelRepository.createYearLevel(yearLevel)
                result.onSuccess {
                    println("DEBUG: CourseRepository - Created year level: ${it.name} for course: ${course.name}")
                }.onFailure { exception ->
                    println("DEBUG: CourseRepository - Failed to create year level $year for course ${course.name}: ${exception.message}")
                }
            }
            
            println("DEBUG: CourseRepository - Successfully created ${course.duration} year levels for course: ${course.name}")
        } catch (e: Exception) {
            println("DEBUG: CourseRepository - Error creating year levels for course ${course.name}: ${e.message}")
        }
    }
    
    /**
     * Get the display name for a year level
     */
    private fun getYearLevelName(year: Int): String {
        return when (year) {
            1 -> "1st Year"
            2 -> "2nd Year"
            3 -> "3rd Year"
            4 -> "4th Year"
            5 -> "5th Year"
            else -> "${year}th Year"
        }
    }
}
