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
            println("DEBUG: AcademicPeriodRepository - Creating academic period: ${academicPeriod.name}")
            println("DEBUG: AcademicPeriodRepository - isActive flag: ${academicPeriod.isActive}")
            
            // If this period is being set as active, deactivate all other periods first
            if (academicPeriod.isActive) {
                println("DEBUG: AcademicPeriodRepository - Period is marked as active, deactivating other periods")
                deactivateAllPeriods()
                // Small delay to ensure deactivation completes
                delay(100)
            } else {
                println("DEBUG: AcademicPeriodRepository - Period is not marked as active, skipping deactivation")
            }
            
            val docRef = academicPeriodsCollection.add(academicPeriod).await()
            val createdPeriod = academicPeriod.copy(id = docRef.id)
            academicPeriodsCollection.document(docRef.id).set(createdPeriod).await()
            
            println("DEBUG: AcademicPeriodRepository - Academic period created successfully with ID: ${createdPeriod.id}")
            println("DEBUG: AcademicPeriodRepository - Final isActive status: ${createdPeriod.isActive}")
            Result.success(createdPeriod)
        } catch (e: Exception) {
            println("DEBUG: AcademicPeriodRepository - Error creating academic period: ${e.message}")
            if (e.message?.contains("PERMISSION_DENIED") == true) {
                Result.failure(Exception("Permission denied. Please check Firestore security rules for academic_periods collection."))
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun updateAcademicPeriod(academicPeriod: AcademicPeriod): Result<Unit> {
        return try {
            println("DEBUG: AcademicPeriodRepository - Updating academic period: ${academicPeriod.name}, isActive: ${academicPeriod.isActive}")
            
            // If this period is being set as active, deactivate all other periods first
            if (academicPeriod.isActive) {
                deactivateAllPeriods()
                // Small delay to ensure deactivation completes
                kotlinx.coroutines.delay(100)
            }
            
            academicPeriodsCollection.document(academicPeriod.id).set(academicPeriod).await()
            println("DEBUG: AcademicPeriodRepository - Successfully updated academic period: ${academicPeriod.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            println("DEBUG: AcademicPeriodRepository - Error updating academic period: ${e.message}")
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
            println("DEBUG: AcademicPeriodRepository - Loaded ${periods.size} academic periods")
            Result.success(periods)
        } catch (e: Exception) {
            println("DEBUG: AcademicPeriodRepository - Error loading academic periods: ${e.message}")
            if (e.message?.contains("PERMISSION_DENIED") == true) {
                println("DEBUG: AcademicPeriodRepository - Permission denied, returning empty list")
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
            
            println("DEBUG: AcademicPeriodRepository - Active period: ${activePeriod?.name ?: "None"}")
            Result.success(activePeriod)
        } catch (e: Exception) {
            println("DEBUG: AcademicPeriodRepository - Error loading active period: ${e.message}")
            if (e.message?.contains("PERMISSION_DENIED") == true) {
                println("DEBUG: AcademicPeriodRepository - Permission denied, returning null")
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
            println("DEBUG: AcademicPeriodRepository - Setting period $periodId as active")
            
            // First deactivate all periods using batch write for atomic operation
            deactivateAllPeriods()
            
            // Small delay to ensure deactivation completes
            delay(100)
            
            // Then activate the selected period
            val updates = mapOf("active" to true)
            academicPeriodsCollection.document(periodId).update(updates).await()
            
            println("DEBUG: AcademicPeriodRepository - Successfully set period $periodId as active")
            Result.success(Unit)
        } catch (e: Exception) {
            println("DEBUG: AcademicPeriodRepository - Error setting active period: ${e.message}")
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
            println("DEBUG: AcademicPeriodRepository - Deactivating all existing active periods")
            
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
                    println("DEBUG: AcademicPeriodRepository - Marking period ${document.id} for deactivation")
                }
            }
            
            if (deactivatedCount > 0) {
                batch.commit().await()
                println("DEBUG: AcademicPeriodRepository - Successfully deactivated $deactivatedCount active periods")
            } else {
                println("DEBUG: AcademicPeriodRepository - No active periods found to deactivate")
            }
        } catch (e: Exception) {
            println("DEBUG: AcademicPeriodRepository - Error deactivating periods: ${e.message}")
            throw e // Re-throw to ensure caller knows if deactivation failed
        }
    }
    
    /**
     * Cleanup function to ensure only one active period exists
     * This can be called to fix any data inconsistencies
     */
    suspend fun ensureSingleActivePeriod(): Result<Unit> {
        return try {
            println("DEBUG: AcademicPeriodRepository - Ensuring only one active period exists")
            
            // Get all periods
            val allPeriods = getAllAcademicPeriods().getOrNull() ?: emptyList()
            val activePeriods = allPeriods.filter { it.isActive }
            
            if (activePeriods.size > 1) {
                println("DEBUG: AcademicPeriodRepository - Found ${activePeriods.size} active periods, deactivating all except the most recent")
                
                // Sort by creation date, keep the most recent one active
                val sortedActive = activePeriods.sortedByDescending { it.createdAt }
                val keepActive = sortedActive.first()
                
                // Deactivate all
                deactivateAllPeriods()
                delay(100)
                
                // Activate only the most recent one
                val updates = mapOf("active" to true)
                academicPeriodsCollection.document(keepActive.id).update(updates).await()
                
                println("DEBUG: AcademicPeriodRepository - Kept period ${keepActive.id} as active, deactivated ${activePeriods.size - 1} others")
            } else if (activePeriods.isEmpty()) {
                println("DEBUG: AcademicPeriodRepository - No active periods found")
            } else {
                println("DEBUG: AcademicPeriodRepository - Only one active period exists (${activePeriods.first().id})")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            println("DEBUG: AcademicPeriodRepository - Error ensuring single active period: ${e.message}")
            Result.failure(e)
        }
    }
}