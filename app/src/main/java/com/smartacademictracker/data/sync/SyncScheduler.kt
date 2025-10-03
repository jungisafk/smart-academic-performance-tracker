package com.smartacademictracker.data.sync

import android.content.Context
import androidx.work.*
import com.smartacademictracker.data.network.NetworkMonitor
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncScheduler @Inject constructor(
    private val context: Context,
    private val networkMonitor: NetworkMonitor
) {
    
    private val workManager = WorkManager.getInstance(context)
    
    /**
     * Schedule periodic sync when device is online
     */
    fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES, // Minimum interval
            5, TimeUnit.MINUTES   // Flex interval
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                1, TimeUnit.MINUTES
            )
            .addTag("grade_sync")
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            "grade_sync_work",
            ExistingPeriodicWorkPolicy.REPLACE,
            syncRequest
        )
        
        println("DEBUG: SyncScheduler - Scheduled periodic sync")
    }
    
    /**
     * Schedule immediate sync when network becomes available
     */
    suspend fun scheduleImmediateSync() {
        if (!networkMonitor.isCurrentlyConnected()) {
            println("DEBUG: SyncScheduler - No network connection, skipping immediate sync")
            return
        }
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30, TimeUnit.SECONDS
            )
            .addTag("immediate_sync")
            .build()
        
        workManager.enqueueUniqueWork(
            "immediate_sync_work",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
        
        println("DEBUG: SyncScheduler - Scheduled immediate sync")
    }
    
    /**
     * Cancel all sync work
     */
    fun cancelAllSync() {
        workManager.cancelAllWorkByTag("grade_sync")
        workManager.cancelAllWorkByTag("immediate_sync")
        println("DEBUG: SyncScheduler - Cancelled all sync work")
    }
    
    /**
     * Monitor network changes and trigger sync when online
     */
    suspend fun startNetworkMonitoring() {
        networkMonitor.isOnline().collect { isOnline ->
            if (isOnline) {
                println("DEBUG: SyncScheduler - Network available, scheduling immediate sync")
                scheduleImmediateSync()
            } else {
                println("DEBUG: SyncScheduler - Network unavailable")
            }
        }
    }
    
    /**
     * Get sync work status
     */
    fun getSyncStatus(): WorkInfo.State? {
        val workInfos = workManager.getWorkInfosByTag("grade_sync").get()
        return workInfos.firstOrNull()?.state
    }
}
