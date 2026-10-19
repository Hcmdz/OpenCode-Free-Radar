/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar

import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.opencode.freeradar.data.local.RadarDatabase
import com.opencode.freeradar.data.repository.OfflineFirstOfferRepository
import com.opencode.freeradar.data.source.remote.SourceOffer
import com.opencode.freeradar.data.source.remote.toOffer
import com.opencode.freeradar.domain.model.ChangeType
import com.opencode.freeradar.domain.repository.FetchResult
import com.opencode.freeradar.domain.repository.OfferRepository
import com.opencode.freeradar.domain.repository.OfferSource
import com.opencode.freeradar.domain.error.Result
import com.opencode.freeradar.domain.error.SourceError
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private const val MODEL = "nemotron-3-ultra-550b-a55b:free"

private class MutableClock(var millis: Long = 1_000_000L) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId?): Clock = this
    override fun instant(): Instant = Instant.ofEpochMilli(millis)
}

private class FakeSource(
    override val id: String,
    private val providerId: String
) : OfferSource {
    var price: Double = 0.0

    override suspend fun fetch(): Result<FetchResult, SourceError> {
        return Result.Success(
            FetchResult(
                listOf(
                    SourceOffer(
                        providerId = providerId,
                        modelId = MODEL,
                        name = "Nemotron 3 Ultra",
                        inputPrice = price,
                        outputPrice = price,
                        contextLength = 1000,
                        maxOutputTokens = null,
                        supportsTools = true,
                        supportsVision = false,
                        supportsStructuredOutput = false,
                        quota = null,
                        conditions = null,
                        officialUrl = null,
                        sourceUrl = null
                    )
                ),
                bodyHash = null
            )
        )
    }
}

/**
 * A free model withdrawn from a paid tier must ring FREE_EXPIRED even when
 * the two pinned catalogs disagreed on the way there. The disagreement demotes
 * both rows to TO_VERIFY, and gating the expiry test on confidence made the
 * demotion silence the very withdrawal it was caused by — the row that learned
 * first rang, the one demoted alongside it never could.
 */
@RunWith(AndroidJUnit4::class)
class CrossCheckDemotionTest {

    private lateinit var database: RadarDatabase
    private lateinit var repository: OfferRepository
    private lateinit var bothub: FakeSource
    private lateinit var nvidia: FakeSource
    private lateinit var clock: MutableClock

    private val remoteId = "bothub/$MODEL"

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder<RadarDatabase>(
            InstrumentationRegistry.getInstrumentation().targetContext
        ).setDriver(AndroidSQLiteDriver()).build()
        bothub = FakeSource("bothub", "bothub")
        nvidia = FakeSource("nvidia", "nvidia")
        clock = MutableClock()
        repository = OfflineFirstOfferRepository(
            database = database,
            sources = mapOf("bothub" to bothub, "nvidia" to nvidia),
            mappers = mapOf(
                "bothub" to { dto: SourceOffer, now: Long -> dto.toOffer(now, "bothub") },
                "nvidia" to { dto: SourceOffer, now: Long -> dto.toOffer(now, "nvidia") }
            ),
            clock = clock
        )
    }

    @After
    fun teardown() = database.close()

    private suspend fun confidence(): String =
        database.offerDao().observeOffer(remoteId).first()?.confidence.orEmpty()

    private suspend fun expiryCount(): Int = expiryCountFor(remoteId)

    private suspend fun expiryCountFor(id: String): Int =
        repository.observeHistory(id).first()
            .count { it.type == ChangeType.FREE_EXPIRED }

    @Test
    fun expiryRingsAfterCrossSourceDisagreement() = runTest {
        // 1. Both catalogs serve it free: the pin confirms both rows.
        bothub.price = 0.0
        nvidia.price = 0.0
        repository.refreshAll()
        assertEquals("CROSS_CHECKED", confidence())

        // 2. They disagree, which is what a withdrawn free tier looks like
        // while the two providers publish at different times.
        nvidia.price = 2.0
        clock.millis += 3_600_000
        repository.refreshAll()
        assertEquals("TO_VERIFY", confidence())

        // 3. Both now agree it is paid. The disagreement window is long over.
        bothub.price = 2.0
        clock.millis += 3_600_000
        repository.refreshAll()

        // The twin that happened to learn first already rang, back at step 2.
        assertEquals(
            "the row that learned first must have rung",
            1,
            expiryCountFor("nvidia/$MODEL")
        )
        assertEquals(
            "a row that was free, then paid, must ring FREE_EXPIRED even when a cross-source disagreement demoted it",
            1,
            expiryCount()
        )
    }
}