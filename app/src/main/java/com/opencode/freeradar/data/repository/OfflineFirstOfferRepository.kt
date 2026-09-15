/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.repository

import com.opencode.freeradar.data.local.RadarDatabase
import com.opencode.freeradar.data.local.SourceHealthEntity
import com.opencode.freeradar.data.local.SyncRunEntity
import com.opencode.freeradar.data.source.remote.toOffer
import com.opencode.freeradar.domain.error.RefreshResult
import com.opencode.freeradar.domain.error.Result
import com.opencode.freeradar.domain.error.SourceError
import com.opencode.freeradar.domain.model.ChangeEvent
import com.opencode.freeradar.domain.model.HealthState
import com.opencode.freeradar.domain.model.Offer
import com.opencode.freeradar.domain.model.SourceHealth
import com.opencode.freeradar.domain.model.SyncResult
import com.opencode.freeradar.domain.model.SyncRun
import com.opencode.freeradar.domain.repository.OfferRepository
import com.opencode.freeradar.domain.repository.OfferSource
import com.opencode.freeradar.domain.usecase.detectChanges
import java.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineFirstOfferRepository(
    private val database: RadarDatabase,
    private val source: OfferSource,
    private val clock: Clock = Clock.systemUTC()
) : OfferRepository {

    private val offers = database.offerDao()
    private val events = database.changeEventDao()
    private val runs = database.syncRunDao()
    private val health = database.sourceHealthDao()

    override fun observeOffers(compatibleOnly: Boolean): Flow<List<Offer>> =
        offers.observeOffers(compatibleOnly).map { rows -> rows.map { it.toDomain() } }

    override fun observeOffer(remoteId: String): Flow<Offer?> =
        offers.observeOffer(remoteId).map { it?.toDomain() }

    override fun observeHistory(remoteId: String): Flow<List<ChangeEvent>> =
        events.observeHistory(remoteId).map { rows -> rows.map { it.toDomain() } }

    override fun observeHealth(): Flow<List<SourceHealth>> =
        health.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeLastRun(source: String): Flow<SyncRun?> =
        runs.observeLastRun(source).map { it?.toDomain() }

    override suspend fun refresh(source: String): RefreshResult {
        val runId = runs.insert(
            SyncRunEntity(
                source = source,
                startedAt = clock.millis(),
                completedAt = null,
                result = null,
                error = null
            )
        )
        return when (val fetched = this.source.fetch()) {
            is Result.Success -> {
                val now = clock.millis()
                val current = offers.snapshotBySource(source).map { it.toDomain() }
                val incoming = fetched.value.map { it.toOffer(now) }
                val detected = detectChanges(current, incoming, now)
                database.offerDao().replaceSourceWithEvents(
                    source,
                    incoming.map { it.toEntity() },
                    detected.map { it.toEntity() }
                )
                events.pruneOlderThan(now - HISTORY_RETENTION_MILLIS)
                runs.pruneKeepLast(source, MAX_SYNC_RUNS)
                runs.finishRun(runId, clock.millis(), SyncResult.OK.name, null)
                health.upsert(
                    SourceHealthEntity(
                        source = source,
                        state = HealthState.HEALTHY.name,
                        checkedAt = clock.millis()
                    )
                )
                RefreshResult.Ok
            }
            is Result.Error -> {
                runs.finishRun(runId, clock.millis(), SyncResult.FAILED.name, fetched.error.code())
                health.upsert(
                    SourceHealthEntity(
                        source = source,
                        state = HealthState.UNAVAILABLE.name,
                        checkedAt = clock.millis()
                    )
                )
                RefreshResult.Failed(fetched.error)
            }
        }
    }

    override suspend fun setFavorite(remoteId: String, favorite: Boolean) {
        offers.setFavorite(remoteId, favorite)
    }

    private fun SourceError.code(): String = when (this) {
        SourceError.Unreachable -> "unreachable"
        SourceError.Timeout -> "timeout"
        SourceError.Server -> "server"
        SourceError.RateLimited -> "rate-limited"
        SourceError.ParseFailed -> "parse-failed"
        is SourceError.Unknown -> "unknown:$code"
    }

    companion object {
        const val HISTORY_RETENTION_MILLIS = 90L * 24 * 60 * 60 * 1000
        const val MAX_SYNC_RUNS = 100
    }
}
