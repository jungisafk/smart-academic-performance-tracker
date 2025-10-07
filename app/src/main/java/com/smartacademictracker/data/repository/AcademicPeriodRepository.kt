package com.smartacademictracker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.smartacademictracker.data.model.AcademicPeriod
import com.smartacademictracker.data.model.AcademicPeriodSummary
import com.smartacademictracker.data.model.Semester
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
            
            // If this period is being set as active, deactivate all other periods
            if (academicPeriod.isActive) {
                deactivateAllPeriods()
            }
            
            val docRef = academicPeriodsCollection.add(academicPeriod).await()
            val createdPeriod = academicPeriod.copy(id = docRef.id)
            academicPeriodsCollection.document(docRef.id).set(createdPeriod).await()
            
            println("DEBUG: AcademicPeriodRepository - Academic period created successfully with ID: ${createdPeriod.id}")
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
            // If this period is being set as active, deactivate all other periods
            if (academicPeriod.isActive) {
                deactivateAllPeriods()
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
                .whereEqualTo("isActive", true)
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
            // First deactivate all periods
            deactivateAllPeriods()
            
            // Then activate the selected period
            val updates = mapOf("isActive" to true)
            academicPeriodsCollection.document(periodId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAcademicPeriodSummary(): Result<AcademicPeriodSummary> {
        return try {
            val periodsResult = getAllAcademicPeriods()
            val activePeriodResult = getActiveAcademicPeriod()
            
            if (periodsResult.isSuccess && activePeriodResult.isSuccess) {
                val periods = periodsResult.getOrNull() ?: emptyList()
                val activePeriod = activePeriodResult.getOrNull()
                
                val summary = AcademicPeriodSummary(
                    totalPeriods = periods.size,
                    activePeriod = activePeriod,
                    currentSemester = activePeriod?.semester?.displayName ?: "",
                    currentAcademicYear = activePeriod?.academicYear ?: ""
                )
                Result.success(summary)
            } else {
                val error = periodsResult.exceptionOrNull() ?: activePeriodResult.exceptionOrNull()
                Result.failure(error ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun deactivateAllPeriods() {
        try {
            val snapshot = academicPeriodsCollection
                .whereEqualTo("isActive", true)
                .get()
                .await()
            
            for (document in snapshot.documents) {
                val updates = mapOf("isActive" to false)
                academicPeriodsCollection.document(document.id).update(updates).await()
            }
        } catch (e: Exception) {
            println("DEBUG: AcademicPeriodRepository - Error deactivating periods: ${e.message}")
        }
    }
}