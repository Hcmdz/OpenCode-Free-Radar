/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar

import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.opencode.freeradar.data.local.RadarDatabase
import com.opencode.freeradar.data.repository.OfflineFirstOfferRepository
import com.opencode.freeradar.data.source.remote.SourceOffer
import com.opencode.freeradar.domain.error.Result
import com.opencode.freeradar.domain.error.SourceError
import com.opencode.freeradar.domain.model.Confidence
import com.opencode.freeradar.domain.model.FreeStatus
import com.opencode.freeradar.domain.model.Offer
import com.opencode.freeradar.domain.repository.OfferRepository
import com.opencode.freeradar.domain.repository.OfferSource
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A cancelled sync must not leave an unfinished run row behind:
 * observeLatestRun would surface the orphan (completedAt null) as the
 * latest sync and the UI would date the last sync at the cancelled run.
 */
@RunWith(AndroidJUnit4::class)
class CancelledRefreshRunTest {

    private class BlockingSource(val entered: CompletableDeferred<Unit>) : OfferSource {
        override val id: String = "s"
        override suspend fun fetch(): Result<List<SourceOffer>, SourceError> {
            entered.complete(Unit)
            awaitCancellation()
        }
    }

    private lateinit var database: RadarDatabase
    private lateinit var repository: OfferRepository
    private lateinit var entered: CompletableDeferred<Unit>

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder<RadarDatabase>(
            InstrumentationRegistry.getInstrumentation().targetContext
        ).setDriver(AndroidSQLiteDriver()).build()
        entered = CompletableDeferred()
        repository = OfflineFirstOfferRepository(
            database = database,
            sources = mapOf("s" to BlockingSource(entered)),
            mappers = mapOf(
                "s" to { dto: SourceOffer, now: Long ->
                    Offer(
                        remoteId = "${dto.providerId}/${dto.modelId}",
                        providerId = dto.providerId,
                        modelId = dto.modelId,
                        name = dto.name,
                        inputPrice = 0.0,
                        outputPrice = 0.0,
                        freeStatus = FreeStatus.FREE,
                        quota = null,
                        quotaPeriod = null,
                        temporary = false,
                        conditions = null,
                        contextLength = 1000,
                        maxOutputTokens = null,
                        supportsTools = true,
                        supportsVision = false,
                        supportsStructuredOutput = false,
                        openCodeCompatible = true,
                        officialUrl = null,
                        source = "s",
                        sourceUrl = null,
                        retrievedAt = now,
                        verifiedAt = now,
                        confidence = Confidence.OFFICIAL,
                        favorite = false
                    )
                }
            ),
            clock = Clock.fixed(Instant.ofEpochMilli(1_000L), ZoneOffset.UTC)
        )
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun cancelledRefreshLeavesNoOrphanRun() = runBlocking {
        val job = launch { repository.refreshAll() }
        withTimeout(10_000) { entered.await() }
        job.cancelAndJoin()

        val latest = repository.observeLatestRun().first()
        assertTrue(
            "cancelled refresh must finish its run row, got $latest",
            latest == null || latest.completedAt != null
        )
    }
}
