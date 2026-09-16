/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.testing.TestWorkerBuilder
import com.opencode.freeradar.domain.error.RefreshResult
import com.opencode.freeradar.domain.model.ChangeEvent
import com.opencode.freeradar.domain.model.Offer
import com.opencode.freeradar.domain.model.SourceHealth
import com.opencode.freeradar.domain.model.SyncRun
import com.opencode.freeradar.domain.repository.OfferRepository
import com.opencode.freeradar.notifications.SyncNotifier
import com.opencode.freeradar.worker.SyncWorker
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/**
 * Pillar 1: a cancelled CoroutineWorker must die, never retry.
 * Cancelling the job running doWork must surface CancellationException;
 * swallowing it schedules a zombie retry (e.g. every REPLACE-cancelled
 * manual refresh re-syncs for nothing and burns rate limit).
 */
@RunWith(AndroidJUnit4::class)
class SyncWorkerCancellationTest {

    private class BlockingRepository : OfferRepository {
        val entered = CompletableDeferred<Unit>()
        override fun observeOffers(compatibleOnly: Boolean): Flow<List<Offer>> = emptyFlow()
        override fun observeOffer(remoteId: String): Flow<Offer?> = emptyFlow()
        override fun observeHistory(remoteId: String): Flow<List<ChangeEvent>> = emptyFlow()
        override fun observeHealth(): Flow<List<SourceHealth>> = emptyFlow()
        override fun observeLastRun(source: String): Flow<SyncRun?> = emptyFlow()
        override fun observeLatestRun(): Flow<SyncRun?> = emptyFlow()
        override suspend fun refresh(source: String): RefreshResult = RefreshResult.Ok
        override suspend fun refreshAll(force: Boolean): RefreshResult {
            entered.complete(Unit)
            awaitCancellation()
        }
        override suspend fun setFavorite(remoteId: String, favorite: Boolean) = Unit
        override suspend fun eventsSince(sinceId: Long, types: List<String>): List<ChangeEvent> =
            emptyList()
        override suspend fun latestEventId(): Long = 0L
    }

    private class NoopGate : SyncNotifier {
        override suspend fun beforeSync(): Long = 0L
        override suspend fun afterSync(watermark: Long) = Unit
    }

    private lateinit var repository: BlockingRepository

    @Before
    fun setup() {
        repository = BlockingRepository()
        stopKoin()
        startKoin {
            modules(
                module {
                    single<OfferRepository> { repository }
                    single<SyncNotifier> { NoopGate() }
                }
            )
        }
    }

    @After
    fun teardown() {
        stopKoin()
    }

    @Test
    fun cancelledWorkPropagatesInsteadOfRetrying(): Unit = runBlocking {
        val context: Context =
            InstrumentationRegistry.getInstrumentation().targetContext
        val worker = TestWorkerBuilder.from(context, SyncWorker::class.java).build()
        supervisorScope {
            // Outcome capsule: records what doWork itself did, independent
            // of join()/await() propagation subtleties.
            val outcome = CompletableDeferred<String>()
            val job = async(Dispatchers.Default) {
                try {
                    val result = worker.doWork()
                    outcome.complete("returned:$result")
                } catch (e: CancellationException) {
                    outcome.complete("threw-ce")
                    throw e
                }
            }
            withTimeout(10_000) { repository.entered.await() }
            job.cancel()
            val result = withTimeout(10_000) { outcome.await() }
            assertTrue(
                "cancelled doWork must propagate CancellationException, but $result",
                result == "threw-ce"
            )
        }
    }
}
