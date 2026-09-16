/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar

import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.opencode.freeradar.data.local.RadarDatabase
import com.opencode.freeradar.data.repository.OfflineFirstOfferRepository
import com.opencode.freeradar.data.source.remote.SourceOffer
import com.opencode.freeradar.domain.error.RefreshResult
import com.opencode.freeradar.domain.error.Result
import com.opencode.freeradar.domain.error.SourceError
import com.opencode.freeradar.domain.model.Confidence
import com.opencode.freeradar.domain.model.FreeStatus
import com.opencode.freeradar.domain.model.Offer
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
 * User state must survive sync: a favorited offer keeps its flag across
 * refreshes (replaceSource upserts whole rows, so the repository has to
 * carry favorites over — mappers always emit favorite=false).
 */
@RunWith(AndroidJUnit4::class)
class FavoritePersistenceTest {

    private class FakeSource(var dtos: List<SourceOffer>) : OfferSource {
        override val id: String = "s"
        override suspend fun fetch(): Result<FetchResult, SourceError> =
            Result.Success(FetchResult(dtos, null))
    }

    private fun dto(modelId: String) = SourceOffer(
        providerId = "s",
        modelId = modelId,
        name = "M $modelId",
        inputPrice = 0.0,
        outputPrice = 0.0,
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

    private lateinit var database: RadarDatabase
    private lateinit var repository: OfferRepository
    private lateinit var source: FakeSource

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder<RadarDatabase>(
            InstrumentationRegistry.getInstrumentation().targetContext
        ).setDriver(AndroidSQLiteDriver()).build()
        source = FakeSource(listOf(dto("a"), dto("b")))
        repository = OfflineFirstOfferRepository(
            database = database,
            sources = mapOf("s" to source),
            mappers = mapOf(
                "s" to { dto: SourceOffer, now: Long ->
                    Offer(
                        remoteId = "${dto.providerId}/${dto.modelId}",
                        providerId = dto.providerId,
                        modelId = dto.modelId,
                        name = dto.name,
                        inputPrice = dto.inputPrice,
                        outputPrice = dto.outputPrice,
                        freeStatus = FreeStatus.FREE,
                        quota = null,
                        quotaPeriod = null,
                        temporary = false,
                        conditions = null,
                        contextLength = dto.contextLength,
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
    fun favoriteSurvivesRefresh() = runTest {
        assertTrue(repository.refresh("s") is RefreshResult.Ok)
        repository.setFavorite("s/a", true)

        assertTrue(repository.refresh("s") is RefreshResult.Ok)

        val rows = repository.observeOffers(false).first()
        assertEquals(2, rows.size)
        assertTrue(rows.first { it.remoteId == "s/a" }.favorite)
        assertTrue(rows.none { it.remoteId == "s/b" && it.favorite })
    }
}
