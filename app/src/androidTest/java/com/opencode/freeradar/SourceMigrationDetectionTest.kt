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
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private data class Row(val modelId: String, val price: Double)

private class ListSource(
    override val id: String,
    private val providerId: String,
    private val rows: () -> List<Row>
) : OfferSource {
    override suspend fun fetch(): Result<FetchResult, SourceError> =
        Result.Success(
            FetchResult(
                rows().map { r ->
                    SourceOffer(
                        providerId = providerId,
                        modelId = r.modelId,
                        name = r.modelId,
                        inputPrice = r.price,
                        outputPrice = r.price,
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
                },
                bodyHash = null
            )
        )
}

/**
 * The source-migration path: a model that moves between catalogs is compared
 * against its own prior state, and the user state it carried survives the move.
 */
@RunWith(AndroidJUnit4::class)
class SourceMigrationDetectionTest {

    private lateinit var database: RadarDatabase
    private lateinit var repository: OfferRepository
    private lateinit var aRows: MutableList<Row>
    private lateinit var bRows: MutableList<Row>
    private lateinit var sourceA: ListSource
    private lateinit var sourceB: ListSource

    private val remoteId = "p/m1"

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder<RadarDatabase>(
            InstrumentationRegistry.getInstrumentation().targetContext
        ).setDriver(AndroidSQLiteDriver()).build()
        aRows = mutableListOf(Row("m1", 0.0), Row("m2", 0.0))
        bRows = mutableListOf(Row("m3", 0.0))
        sourceA = ListSource("a", "p") { aRows.toList() }
        sourceB = ListSource("b", "p") { bRows.toList() }
        repository = OfflineFirstOfferRepository(
            database = database,
            sources = mapOf("a" to sourceA, "b" to sourceB),
            mappers = mapOf(
                "a" to { dto: SourceOffer, now: Long -> dto.toOffer(now, "a") },
                "b" to { dto: SourceOffer, now: Long -> dto.toOffer(now, "b") }
            ),
            clock = Clock.fixed(java.time.Instant.ofEpochMilli(1_000_000L), ZoneOffset.UTC)
        )
    }

    @After
    fun teardown() = database.close()

    private suspend fun expiryCount(): Int =
        repository.observeHistory(remoteId).first()
            .count { it.type == ChangeType.FREE_EXPIRED }

    @Test
    fun expiryRingsWhenModelMigratesBetweenSources() = runTest {
        // 1. m1 is free and lives under source a.
        repository.refreshAll()
        assertTrue(expiryCount() == 0)

        // 2. m1 leaves a and reappears under b, now paid.
        aRows.removeIf { it.modelId == "m1" }
        bRows.add(Row("m1", 2.0))

        repository.refreshAll()

        assertEquals(
            "a model that was free then paid must ring even across a source migration",
            1,
            expiryCount()
        )
    }

    @Test
    fun favoriteSurvivesSourceMigration() = runTest {
        // 1. m1 is free and lives under source a.
        repository.refreshAll()
        repository.setFavorite(remoteId, true)

        // 2. m1 leaves a and reappears under b. The absence sweep never
        // deletes a favorite, so its row lingers under a until b upserts it.
        aRows.removeIf { it.modelId == "m1" }
        bRows.add(Row("m1", 0.0))

        repository.refreshAll()

        val migrated = repository.observeOffers(false).first().first { it.remoteId == remoteId }
        assertTrue(
            "a favorite must keep its flag when its model moves between catalogs",
            migrated.favorite
        )
    }
}