package com.smartacademictracker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.smartacademictracker.data.model.AcademicPeriod
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AcademicPeriodRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val collection = firestore.collection("academic_periods")

    suspend fun getAllAcademicPeriods(): Result<List<AcademicPeriod>> {
        return try {
            val snapshot = collection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val periods = snapshot.documents.mapNotNull { document ->
                try {
                    document.toObject(AcademicPeriod::class.java)?.copy(id = document.id)
                } catch (e: Exception) {
                    println("DEBUG: AcademicPeriodRepository - Error parsing document ${document.id}: ${e.message}")
                    null
                }
            }
            
            println("DEBUG: AcademicPeriodRepository - Loaded ${periods.size} academic periods")
            Result.success(periods)
        } catch (e: Exception) {
            println("DEBUG: AcademicPeriodRepository - Error loading academic periods: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getCurrentAcademicPeriod(): Result<AcademicPeriod?> {
        return try {
            val snapshot = collection
                .whereEqualTo("isCurrent", true)
                .limit(1)
                .get()
                .await()
            
            val currentPeriod = if (snapshot.isEmpty) {
                null
            } else {
                val document = snapshot.documents.first()
                document.toObject(AcademicPeriod::class.java)?.copy(id = document.id)
            }
            
            println("DEBUG: AcademicPeriodRepository - Current period: ${currentPeriod?.name}")
            Result.success(currentPeriod)
        } catch (e: Exception) {
            println("DEBUG: AcademicPeriodRepository - Error loading current period: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun createAcademicPeriod(
        name: String,
        semester: String,
        academicYear: String,
        startDate: Long,
        endDate: Long,
        createdBy: String = ""
    ): Result<AcademicPeriod> {
        return try {
            val period = AcademicPeriod(
                name = name,
                semester = semester,
                academicYear = academicYear,
                startDate = startDate,
                endDate = endDate,
                isCurrent = false, // Will be set separately
                createdBy = createdBy
            )
            
            val documentRef = collection.add(period).await()
            val createdPeriod = period.copy(id = documentRef.id)
            
            println("DEBUG: AcademicPeriodRepository - Created academic period: ${createdPeriod.name}")
            Result.success(createdPeriod)
        } catch (e: Exception) {
            println("DEBUG: AcademicPeriodRepository - Error creating academic period: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun setCurrentPeriod(periodId: String): Result<Unit> {
        return try {
            // First, set all periods to not current
            val allPeriodsSnapshot = collection.get().await()
            val batch = firestore.batch()
            
            allPeriodsSnapshot.documents.forEach { document ->
                batch.update(document.reference, "isCurrent", false)
            }
            
            // Then set the specified period as current
            val periodRef = collection.document(periodId)
            batch.update(periodRef, "isCurrent", true)
            
            batch.commit().await()
            
            println("DEBUG: AcademicPeriodRepository - Set current period: $periodId")
            Result.success(Unit)
        } catch (e: Exception) {
            println("DEBUG: AcademicPeriodRepository - Error setting current period: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateAcademicPeriod(
        periodId: String,
        name: String? = null,
        semester: String? = null,
        academicYear: String? = null,
        startDate: Long? = null,
        endDate: Long? = null
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>()
            
            name?.let { updates["name"] = it }
            semester?.let { updates["semester"] = it }
            academicYear?.let { updates["academicYear"] = it }
            startDate?.let { updates["startDate"] = it }
            endDate?.let { updates["endDate"] = it }
            
            if (updates.isNotEmpty()) {
                collection.document(periodId).update(updates).await()
                println("DEBUG: AcademicPeriodRepository - Updated academic period: $periodId")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            println("DEBUG: AcademicPeriodRepository - Error updating academic period: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteAcademicPeriod(periodId: String): Result<Unit> {
        return try {
            collection.document(periodId).delete().await()
            println("DEBUG: AcademicPeriodRepository - Deleted academic period: $periodId")
            Result.success(Unit)
        } catch (e: Exception) {
            println("DEBUG: AcademicPeriodRepository - Error deleting academic period: ${e.message}")
            Result.failure(e)
        }
    }
}
