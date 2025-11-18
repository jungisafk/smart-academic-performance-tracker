package com.smartacademictracker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.smartacademictracker.data.model.AcademicPeriod
import com.smartacademictracker.data.model.AcademicPeriodOverview
import com.smartacademictracker.data.model.Semester
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AcademicPeriodRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val academicPeriodsCollection = firestore.collection("academic_periods")

    suspend fun createAcademicPeriod(academicPeriod: AcademicPeriod): Result<AcademicPeriod> {
        return try {
            // If this period is being set as active, deactivate all other periods first
            if (academicPeriod.isActive) {
                deactivateAllPeriods()
                // Small delay to ensure deactivation completes
                delay(100)
            }
            
            val docRef = academicPeriodsCollection.add(academicPeriod).await()
            val createdPeriod = academicPeriod.copy(id = docRef.id)
            academicPeriodsCollection.document(docRef.id).set(createdPeriod).await()
            
            Result.success(createdPeriod)
        } catch (e: Exception) {
            if (e.message?.contains("PERMISSION_DENIED") == true) {
                Result.failure(Exception("Permission denied. Please check Firestore security rules for academic_periods collection."))
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun updateAcademicPeriod(academicPeriod: AcademicPeriod): Result<Unit> {
        return try {
            // If this period is being set as active, deactivate all other periods first
            if (academicPeriod.isActive) {
                deactivateAllPeriods()
                // Small delay to ensure deactivation completes
                kotlinx.coroutines.delay(100)
            }
            
            academicPeriodsCollection.document(academicPeriod.id).set(academicPeriod).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAcademicPeriod(periodId: String): Result<Unit> {
        return try {
            academicPeriodsCollection.document(periodId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllAcademicPeriods(): Result<List<AcademicPeriod>> {
        return try {
            val snapshot = academicPeriodsCollection
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            val periods = snapshot.toObjects(AcademicPeriod::class.java)
            Result.success(periods)
        } catch (e: Exception) {
            if (e.message?.contains("PERMISSION_DENIED") == true) {
                Result.success(emptyList())
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun getActiveAcademicPeriod(): Result<AcademicPeriod?> {
        return try {
            val snapshot = academicPeriodsCollection
                .whereEqualTo("active", true)
                .limit(1)
                .get()
                .await()
            
            val activePeriod = if (snapshot.isEmpty) {
                null
            } else {
                snapshot.documents.first().toObject(AcademicPeriod::class.java)
            }
            
            Result.success(activePeriod)
        } catch (e: Exception) {
            if (e.message?.contains("PERMISSION_DENIED") == true) {
                Result.success(null)
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun getAcademicPeriodById(periodId: String): Result<AcademicPeriod> {
        return try {
            val document = academicPeriodsCollection.document(periodId).get().await()
            val period = document.toObject(AcademicPeriod::class.java)
                ?: throw Exception("Academic period not found")
            Result.success(period)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setActivePeriod(periodId: String): Result<Unit> {
        return try {
            // First deactivate all periods using batch write for atomic operation
            deactivateAllPeriods()
            
            // Small delay to ensure deactivation completes
            delay(100)
            
            // Then activate the selected period
            val updates = mapOf("active" to true)
            academicPeriodsCollection.document(periodId).update(updates).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAcademicPeriodSummary(): Result<AcademicPeriodOverview> {
        return try {
            val periodsResult = getAllAcademicPeriods()
            val activePeriodResult = getActiveAcademicPeriod()
            
            if (periodsResult.isSuccess && activePeriodResult.isSuccess) {
                val periods = periodsResult.getOrNull() ?: emptyList()
                val activePeriod = activePeriodResult.getOrNull()
                
                val summary = AcademicPeriodOverview(
                    totalPeriods = periods.size,
                    activePeriod = activePeriod,
                    currentSemester = activePeriod?.semester?.displayName ?: "",
                    currentAcademicYear = activePeriod?.academicYear ?: ""
                )
                Result.success(summary)
            } else {
                val error = periodsResult.exceptionOrNull() ?: activePeriodResult.exceptionOrNull()
                Result.failure<AcademicPeriodOverview>(error ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure<AcademicPeriodOverview>(e)
        }
    }

    private suspend fun deactivateAllPeriods() {
        try {
            // Get ALL periods first (not just active ones) to ensure we catch everything
            val allPeriodsSnapshot = academicPeriodsCollection.get().await()
            
            // Use batch write for atomic operation
            val batch = firestore.batch()
            var deactivatedCount = 0
            
            for (document in allPeriodsSnapshot.documents) {
                val data = document.data
                val isActive = data?.get("active") as? Boolean ?: false
                
                if (isActive) {
                    batch.update(document.reference, "active", false)
                    deactivatedCount++
                }
            }
            
            if (deactivatedCount > 0) {
                batch.commit().await()
            }
        } catch (e: Exception) {
            throw e // Re-throw to ensure caller knows if deactivation failed
        }
    }
    
    /**
     * Cleanup function to ensure only one active period exists
     * This can be called to fix any data inconsistencies
     */
    suspend fun ensureSingleActivePeriod(): Result<Unit> {
        return try {
            // Get all periods
            val allPeriods = getAllAcademicPeriods().getOrNull() ?: emptyList()
            val activePeriods = allPeriods.filter { it.isActive }
            
            if (activePeriods.size > 1) {
                // Sort by creation date, keep the most recent one active
                val sortedActive = activePeriods.sortedByDescending { it.createdAt }
                val keepActive = sortedActive.first()
                
                // Deactivate all
                deactivateAllPeriods()
                delay(100)
                
                // Activate only the most recent one
                val updates = mapOf("active" to true)
                academicPeriodsCollection.document(keepActive.id).update(updates).await()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}