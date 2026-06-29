package com.example.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class CloudSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d("CloudSyncWorker", "Starting background cloud synchronization...")
        return try {
            val syncEngine = SyncEngine(applicationContext)
            val success = syncEngine.syncAll()
            if (success) {
                Log.d("CloudSyncWorker", "Background sync completed successfully.")
                Result.success()
            } else {
                Log.d("CloudSyncWorker", "Background sync completed with errors or Firebase unavailable.")
                Result.success() // Succeed anyway, or retry if network transient issues exist
            }
        } catch (e: Exception) {
            Log.e("CloudSyncWorker", "Background sync failed: ${e.message}", e)
            Result.retry()
        }
    }
}
