package com.ranzan.morphebuilds.data.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkManagerScheduler {

    private const val CLEANUP_WORK_NAME = "morphe_auto_cleanup_work"

    fun scheduleCleanup(context: Context, intervalDays: Int) {
        val workManager = WorkManager.getInstance(context)

        if (intervalDays <= 0) {
            workManager.cancelUniqueWork(CLEANUP_WORK_NAME)
            return
        }

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val cleanupRequest = PeriodicWorkRequestBuilder<AutoCleanupWorker>(
            intervalDays.toLong(),
            TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            CLEANUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            cleanupRequest
        )
    }
}
