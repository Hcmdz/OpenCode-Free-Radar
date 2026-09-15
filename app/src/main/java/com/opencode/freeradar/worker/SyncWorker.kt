/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.opencode.freeradar.domain.repository.OfferRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SyncWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params), KoinComponent {

    private val repository: OfferRepository by inject()

    override suspend fun doWork(): Result {
        return try {
            repository.refresh(SOURCE_ID)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount >= MAX_ATTEMPTS) Result.failure() else Result.retry()
        }
    }

    companion object {
        const val SOURCE_ID = "opencode-data"
        const val MAX_ATTEMPTS = 3
    }
}
