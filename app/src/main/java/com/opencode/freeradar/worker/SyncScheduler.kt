/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.opencode.freeradar.data.local.AutoSync
import java.time.Duration
import java.util.concurrent.TimeUnit

object SyncScheduler {
    const val DAILY_WORK = "daily-radar"
    const val MANUAL_WORK = "manual-refresh"

    /**
     * The network and power axes live in the WorkManager constraint, not in the
     * worker: an unsatisfied constraint costs no wakeup, whereas a runtime check
     * wakes the device, runs, and then skips every source.
     */
    internal fun constraints(mode: AutoSync): Constraints = Constraints.Builder()
        .setRequiredNetworkType(
            if (mode.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
        )
        .setRequiresCharging(mode.requiresCharging)
        .build()

    private fun periodicRequest(mode: AutoSync, hours: Int) =
        PeriodicWorkRequestBuilder<SyncWorker>(hours.toLong(), TimeUnit.HOURS)
            .setConstraints(constraints(mode))
            // Without this the first run fires the moment the constraint is met,
            // so a settings toggle would sync immediately instead of in `hours`.
            .setInitialDelay(hours.toLong(), TimeUnit.HOURS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofMinutes(10))
            .build()

    /**
     * @param force false on app start: enqueue only when absent, so a cold start
     *   cannot push the period forward and starve the sync. true after a settings
     *   change, where the existing schedule is stale and must be replaced.
     */
    fun schedule(context: Context, mode: AutoSync, hours: Int, force: Boolean) {
        val work = WorkManager.getInstance(context)
        if (force) {
            work.enqueueUniquePeriodicWork(
                DAILY_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicRequest(mode, hours)
            )
            return
        }
        if (work.getWorkInfosForUniqueWork(DAILY_WORK).get().isEmpty()) {
            work.enqueueUniquePeriodicWork(
                DAILY_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest(mode, hours)
            )
        }
    }

    fun refreshNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(MANUAL_WORK, ExistingWorkPolicy.REPLACE, request)
    }
}
