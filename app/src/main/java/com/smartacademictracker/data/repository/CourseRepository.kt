package com.smartacademictracker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.smartacademictracker.data.model.Course
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CourseRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val coursesCollection = firestore.collection("courses")

    suspend fun createCourse(course: Course): Result<Course> {
        return try {
            val docRef = coursesCollection.add(course).await()
            val createdCourse = course.copy(id = docRef.id)
            coursesCollection.document(docRef.id).set(createdCourse).await()
            Result.success(createdCourse)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllCourses(): Result<List<Course>> {
        return try {
            val snapshot = coursesCollection
                .whereEqualTo("active", true)
                .orderBy("name")
                .get()
                .await()
            val courses = snapshot.toObjects(Course::class.java)
            Result.success(courses)
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
}
