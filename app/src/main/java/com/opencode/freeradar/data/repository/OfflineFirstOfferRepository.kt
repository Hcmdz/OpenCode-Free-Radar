/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.repository

import com.opencode.freeradar.data.local.RadarDatabase
import com.opencode.freeradar.data.local.SourceHealthEntity
import com.opencode.freeradar.data.local.SyncRunEntity
import com.opencode.freeradar.data.source.remote.SourceOffer
import com.opencode.freeradar.data.source.remote.toOffer
import com.opencode.freeradar.domain.error.RefreshResult
import com.opencode.freeradar.domain.error.Result
import com.opencode.freeradar.domain.error.SourceError
import com.opencode.freeradar.domain.model.ChangeEvent
import com.opencode.freeradar.domain.model.Confidence
import com.opencode.freeradar.domain.model.HealthState
import com.opencode.freeradar.domain.model.Offer
import com.opencode.freeradar.domain.model.SourceHealth
import com.opencode.freeradar.domain.model.SyncResult
import com.opencode.freeradar.domain.model.SyncRun
import com.opencode.freeradar.domain.repository.OfferRepository
import com.opencode.freeradar.domain.repository.OfferSource
import com.opencode.freeradar.domain.usecase.detectChanges
import com.opencode.freeradar.domain.usecase.crossCheck
import java.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class OfflineFirstOfferRepository(
    private val database: RadarDatabase,
    private val sources: Map<String, OfferSource>,
    private val mappers: Map<String, (SourceOffer, Long) -> Offer> = emptyMap(),
    private val clock: Clock = Clock.systemUTC()
) : OfferRepository {

    private val offers = database.offerDao()
    private val events = database.changeEventDao()
    private val runs = database.syncRunDao()
    private val health = database.sourceHealthDao()
    // Concurrent full syncs race snapshot→replace across sources (lost events,
    // false MODEL_REMOVED). refreshAll is the only multi-source entry point.
    private val refreshMutex = Mutex()

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

    override fun observeLatestRun(): Flow<SyncRun?> =
        runs.observeLatestRun().map { it?.toDomain() }

    override suspend fun refresh(source: String): RefreshResult {
        val offerSource = sources[source]
            ?: return RefreshResult.Failed(SourceError.Unknown("unknown-source"))
        val runId = runs.insert(
            SyncRunEntity(
                source = source,
                startedAt = clock.millis(),
                completedAt = null,
                result = null,
                error = null
            )
        )
        return when (val fetched = offerSource.fetch()) {
            is Result.Success -> {
                val now = clock.millis()
                val current = offers.snapshotBySource(source).map { it.toDomain() }
                // Mapping is source-specific (S1 prices vs S2 LIMITED rule):
                // a shared mapper here once laundered NVIDIA rows into
                // opencode-data rows. The registry is wired in AppModule.
                val mapOffer = mappers[source] ?: { dto: SourceOffer, at: Long -> dto.toOffer(at, source) }
                val incoming = fetched.value.map { mapOffer(it, now) }
                if (incoming.isEmpty() && current.isNotEmpty()) {
                    // Empty catalog with cached offers means truncated fetch, never a wipe.
                    runs.finishRun(runId, clock.millis(), SyncResult.FAILED.name, "empty-catalog")
                    health.upsert(
                        SourceHealthEntity(
                            source = source,
                            state = HealthState.DEGRADED.name,
                            checkedAt = clock.millis()
                        )
                    )
                    return RefreshResult.Failed(SourceError.ParseFailed)
                }
                // ponytail: empty-only guard, ratio guard (<50% of cache) if partial truncations appear
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

    override suspend fun refreshAll(): RefreshResult = refreshMutex.withLock {
        // Deterministic order: map iteration follows DI registration.
        val results = sources.keys.associateWith { refresh(it) }
        applyCrossCheck()
        combineResults(results)
    }

    override suspend fun setFavorite(remoteId: String, favorite: Boolean) {
        offers.setFavorite(remoteId, favorite)
    }

    override suspend fun eventsSince(sinceId: Long, types: List<String>): List<ChangeEvent> =
        events.eventsAfter(sinceId, types).map { it.toDomain() }

    /**
     * Pinned S1↔S2 comparison after every run: agreement raises both rows to
     * CROSS_CHECKED, disagreement drops both to TO_VERIFY (never overwrites
     * user state — confidence is sync-owned, favorites are not).
     */
    private suspend fun applyCrossCheck() {
        val result = crossCheck(offers.snapshotAll().map { it.toDomain() })
        if (result.confirmed.isNotEmpty()) {
            offers.updateConfidence(result.confirmed.toList(), Confidence.CROSS_CHECKED.name)
        }
        if (result.conflicts.isNotEmpty()) {
            offers.updateConfidence(result.conflicts.toList(), Confidence.TO_VERIFY.name)
        }
    }

    override suspend fun latestEventId(): Long = events.maxEventId()

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

        /**
         * Pure aggregation over per-source results: all ok → Ok, mixed →
         * Partial (failed ids), all failed → first error. Unit-tested; the
         * row-level isolation lives in per-source [refresh] (untouched).
         */
        fun combineResults(results: Map<String, RefreshResult>): RefreshResult {
            val failed = results.filterValues { it is RefreshResult.Failed }
            if (failed.isEmpty()) return RefreshResult.Ok
            if (failed.size < results.size) return RefreshResult.Partial(failed.keys.toList())
            return failed.values.first() as RefreshResult.Failed
        }
    }
}
