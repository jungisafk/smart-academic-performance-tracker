package com.smartacademictracker.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val gradeSyncManager: GradeSyncManager
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            println("DEBUG: SyncWorker - Starting background sync")
            
            val syncResult = gradeSyncManager.syncPendingGrades()
            
            when (syncResult) {
                is SyncResult.SUCCESS -> {
                    println("DEBUG: SyncWorker - Sync successful: ${syncResult.message}")
                    Result.success()
                }
                is SyncResult.FAILED -> {
                    println("DEBUG: SyncWorker - Sync failed: ${syncResult.message}")
                    Result.retry()
                }
                is SyncResult.PARTIAL_SUCCESS -> {
                    println("DEBUG: SyncWorker - Partial sync: ${syncResult.message}")
                    if (syncResult.failureCount > 0) {
                        Result.retry()
                    } else {
                        Result.success()
                    }
                }
            }
        } catch (e: Exception) {
            println("DEBUG: SyncWorker - Sync exception: ${e.message}")
            Result.failure()
        }
    }
}
