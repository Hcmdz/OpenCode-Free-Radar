/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.opencode.freeradar.domain.error.RefreshResult
import com.opencode.freeradar.domain.repository.OfferRepository
import com.opencode.freeradar.notifications.SyncNotifier
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SyncWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params), KoinComponent {

    private val repository: OfferRepository by inject()
    private val gate: SyncNotifier by inject()

    override suspend fun doWork(): Result {
        return try {
            val watermark = gate.beforeSync()
            when (repository.refreshAll()) {
                RefreshResult.Ok, is RefreshResult.Partial -> {
                    gate.afterSync(watermark)
                    Result.success()
                }
                is RefreshResult.Failed -> {
                    if (runAttemptCount >= MAX_ATTEMPTS) Result.failure() else Result.retry()
                }
            }
        } catch (e: Exception) {
            if (runAttemptCount >= MAX_ATTEMPTS) Result.failure() else Result.retry()
        }
    }

    companion object {
        const val MAX_ATTEMPTS = 3
    }
}
