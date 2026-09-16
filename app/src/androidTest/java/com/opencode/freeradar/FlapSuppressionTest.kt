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
import com.opencode.freeradar.domain.error.RefreshResult
import com.opencode.freeradar.domain.error.Result
import com.opencode.freeradar.domain.error.SourceError
import com.opencode.freeradar.domain.model.ChangeType
import com.opencode.freeradar.domain.repository.OfferRepository
import com.opencode.freeradar.domain.repository.FetchResult
import com.opencode.freeradar.domain.repository.OfferSource
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A $0 flip-flop must ring the bell once per 90-day window, not on every
 * swing: the second PAID -> usable-free swing stays recorded in history
 * but emits no new BECAME_FREE while the first is still in retention.
 */
@RunWith(AndroidJUnit4::class)
class FlapSuppressionTest {

    private class FakeSource(var price: Double) : OfferSource {
        override val id: String = "s"
        override suspend fun fetch(): Result<FetchResult, SourceError> =
            Result.Success(
                FetchResult(
                    listOf(
                        SourceOffer(
                        providerId = "s",
                        modelId = "m",
                        name = "M",
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
                bodyHash = null,
            )
        )
    }

    private lateinit var database: RadarDatabase
    private lateinit var repository: OfferRepository
    private lateinit var source: FakeSource

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder<RadarDatabase>(
            InstrumentationRegistry.getInstrumentation().targetContext
        ).setDriver(AndroidSQLiteDriver()).build()
        source = FakeSource(1.0)
        repository = OfflineFirstOfferRepository(
            database = database,
            sources = mapOf("s" to source),
            mappers = mapOf("s" to { dto: SourceOffer, now: Long -> dto.toOffer(now, "s") }),
            clock = Clock.fixed(Instant.ofEpochMilli(1_000L), ZoneOffset.UTC)
        )
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun repeatedBecameFreeRingsOnce() = runTest {
        source.price = 1.0
        assertTrue(repository.refresh("s") is RefreshResult.Ok)
        source.price = 0.0
        assertTrue(repository.refresh("s") is RefreshResult.Ok)
        source.price = 1.0
        assertTrue(repository.refresh("s") is RefreshResult.Ok)
        source.price = 0.0
        assertTrue(repository.refresh("s") is RefreshResult.Ok)

        val becameFree = repository.observeHistory("s/m").first()
            .count { it.type == ChangeType.BECAME_FREE }
        assertEquals(1, becameFree)
    }
}
