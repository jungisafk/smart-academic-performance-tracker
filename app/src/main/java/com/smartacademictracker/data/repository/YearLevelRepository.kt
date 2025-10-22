package com.smartacademictracker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.smartacademictracker.data.model.YearLevel
import com.smartacademictracker.data.service.AcademicPeriodFilterService
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YearLevelRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val academicPeriodFilterService: AcademicPeriodFilterService
) {
    private val yearLevelsCollection = firestore.collection("year_levels")

    suspend fun createYearLevel(yearLevel: YearLevel): Result<YearLevel> {
        return try {
            // Check if there's an active academic period
            if (!academicPeriodFilterService.hasActiveAcademicPeriod()) {
                return Result.failure(Exception("No active academic period found. Please create an academic period first."))
            }
            
            // Get the active academic period ID
            val activePeriodId = academicPeriodFilterService.getActiveAcademicPeriodId()
            
            // Create year level with active academic period
            val yearLevelWithPeriod = yearLevel.copy(academicPeriodId = activePeriodId)
            val docRef = yearLevelsCollection.add(yearLevelWithPeriod).await()
            val createdYearLevel = yearLevelWithPeriod.copy(id = docRef.id)
            yearLevelsCollection.document(docRef.id).set(createdYearLevel).await()
            Result.success(createdYearLevel)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllYearLevels(): Result<List<YearLevel>> {
        return try {
            val snapshot = yearLevelsCollection
                .whereEqualTo("active", true)
                .get()
                .await()
            val yearLevels = snapshot.toObjects(YearLevel::class.java)
            // Sort in memory to avoid composite index requirement
            val sortedYearLevels = yearLevels.sortedBy { it.level }
            Result.success(sortedYearLevels)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getYearLevelById(yearLevelId: String): Result<YearLevel?> {
        return try {
            val document = yearLevelsCollection.document(yearLevelId).get().await()
            if (document.exists()) {
                val yearLevel = document.toObject(YearLevel::class.java)
                Result.success(yearLevel)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateYearLevel(yearLevel: YearLevel): Result<YearLevel> {
        return try {
            yearLevelsCollection.document(yearLevel.id).set(yearLevel).await()
            Result.success(yearLevel)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteYearLevel(yearLevelId: String): Result<Unit> {
        return try {
            yearLevelsCollection.document(yearLevelId).update("active", false).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getYearLevelByLevel(level: Int): Result<YearLevel?> {
        return try {
            val snapshot = yearLevelsCollection
                .whereEqualTo("level", level)
                .whereEqualTo("active", true)
                .get()
                .await()
            
            if (snapshot.isEmpty) {
                Result.success(null)
            } else {
                val yearLevel = snapshot.documents.first().toObject(YearLevel::class.java)
                Result.success(yearLevel)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getYearLevelsByCourse(courseId: String): Result<List<YearLevel>> {
        return try {
            val snapshot = yearLevelsCollection
                .whereEqualTo("courseId", courseId)
                .whereEqualTo("active", true)
                .get()
                .await()
            val yearLevels = snapshot.toObjects(YearLevel::class.java)
            // Sort by level
            val sortedYearLevels = yearLevels.sortedBy { it.level }
            Result.success(sortedYearLevels)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
