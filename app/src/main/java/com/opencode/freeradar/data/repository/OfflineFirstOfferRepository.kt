/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.repository

import com.opencode.freeradar.data.local.RadarDatabase
import com.opencode.freeradar.data.local.SourceHealthEntity
import com.opencode.freeradar.data.local.SyncRunEntity
import com.opencode.freeradar.data.source.remote.ModelsDevSource
import com.opencode.freeradar.domain.usecase.OVERLAP_PINS
import com.opencode.freeradar.data.local.SyncStateStore
import com.opencode.freeradar.data.local.SyncSettings
import com.opencode.freeradar.util.NetworkMonitor
import com.opencode.freeradar.data.source.remote.SourceOffer
import com.opencode.freeradar.data.source.remote.toOffer
import com.opencode.freeradar.domain.error.RefreshResult
import com.opencode.freeradar.domain.error.Result
import com.opencode.freeradar.domain.error.SourceError
import com.opencode.freeradar.domain.model.ChangeEvent
import com.opencode.freeradar.domain.model.ChangeType
import com.opencode.freeradar.domain.model.Confidence
import com.opencode.freeradar.domain.model.HealthState
import com.opencode.freeradar.domain.model.Offer
import com.opencode.freeradar.domain.model.SourceHealth
import com.opencode.freeradar.domain.model.SyncResult
import com.opencode.freeradar.domain.model.SyncRun
import com.opencode.freeradar.domain.repository.OfferRepository
import com.opencode.freeradar.domain.repository.OfferSource
import com.opencode.freeradar.domain.usecase.AbsenceRow
import com.opencode.freeradar.domain.usecase.crossCheck
import com.opencode.freeradar.domain.usecase.detectChanges
import com.opencode.freeradar.domain.usecase.planAbsence
import java.time.Clock
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class OfflineFirstOfferRepository(
    private val database: RadarDatabase,
    private val sources: Map<String, OfferSource>,
    private val mappers: Map<String, (SourceOffer, Long) -> Offer> = emptyMap(),
    private val clock: Clock = Clock.systemUTC(),
    private val syncState: SyncStateStore? = null,
    private val syncPrefs: SyncSettings? = null,
    private val network: NetworkMonitor? = null,
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
        return try {
            when (val fetched = offerSource.fetch()) {
            is Result.Success -> {
                val body = fetched.value
                if (isUnchanged(source, body.bodyHash)) {
                    runs.finishRun(runId, clock.millis(), SyncResult.OK.name, "skipped-hash")
                    health.upsert(
                        SourceHealthEntity(
                            source = source,
                            state = HealthState.HEALTHY.name,
                            checkedAt = clock.millis()
                        )
                    )
                    return RefreshResult.Ok
                }
                body.bodyHash?.let { hash -> syncState?.recordHash(source, hash) }
                val dtos = body.offers
                val now = clock.millis()
                val current = offers.snapshotBySource(source).map { it.toDomain() }
                // Mapping is source-specific (S1 prices vs S2 LIMITED rule):
                // a shared mapper here once laundered NVIDIA rows into
                // opencode-data rows. The registry is wired in AppModule.
                val mapOffer = mappers[source] ?: { dto: SourceOffer, at: Long -> dto.toOffer(at, source) }
                // Mappers are sync-owned (favorite=false): carry the user's
                // flags over, or every refresh wipes them via REPLACE.
                val favorites = current.filter { it.favorite }.map { it.remoteId }.toSet()
                val incoming = dtos.map { dto ->
                    mapOffer(dto, now).let { offer ->
                        if (offer.remoteId in favorites) offer.copy(favorite = true) else offer
                    }
                }
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
                // ponytail: empty-only guard; >50% shrink below is the partial-truncation guard
                val present = incoming.map { it.remoteId }.toSet()
                if (current.isNotEmpty() && incoming.size * 2 < current.size) {
                    // Successful fetch that lost over half the catalog means a
                    // truncated payload, never a mass delisting: fail closed.
                    runs.finishRun(runId, clock.millis(), SyncResult.FAILED.name, "shrunk-catalog")
                    health.upsert(
                        SourceHealthEntity(
                            source = source,
                            state = HealthState.DEGRADED.name,
                            checkedAt = clock.millis()
                        )
                    )
                    return RefreshResult.Failed(SourceError.ParseFailed)
                }
                val plan = planAbsence(
                    current.map { AbsenceRow(it.remoteId, it.missedSyncs, it.favorite) },
                    present
                )
                if (plan.bump.isNotEmpty()) offers.bumpMissed(plan.bump)
                val detected = detectChanges(current, incoming, now)
                    .filter { it.type != ChangeType.MODEL_REMOVED }
                    .filterNot { event ->
                        // Anti-flap: a $0 swing rings once per retention window.
                        // The first BECAME_FREE stays in history as proof; later
                        // swings re-alert only after it ages out with the prune
                        // below, so the window is self-cleaning. One extra read
                        // per BECAME_FREE — rare by construction.
                        event.type == ChangeType.BECAME_FREE &&
                            events.countTypeSince(
                                event.offerRemoteId,
                                ChangeType.BECAME_FREE.name,
                                now - HISTORY_RETENTION_MILLIS
                            ) > 0
                    }
                    .filterNot { event ->
                        // Symmetric expiry flap guard: a flickering $0
                        // (TEMPORARY→PAID→TEMPORARY across syncs) must not
                        // re-ring FREE_EXPIRED every cycle.
                        event.type == ChangeType.FREE_EXPIRED &&
                            events.countTypeSince(
                                event.offerRemoteId,
                                ChangeType.FREE_EXPIRED.name,
                                now - HISTORY_RETENTION_MILLIS
                            ) > 0
                    }
                val removals = plan.remove.map { ChangeEvent(it, ChangeType.MODEL_REMOVED, null, null, now) }
                offers.replaceSource(
                    incoming.map { it.toEntity() },
                    (detected + removals).map { it.toEntity() },
                    plan.remove
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
        } catch (e: CancellationException) {
            // A cancelled sync must not leave an unfinished run row behind:
            // observeLatestRun would surface the orphan as the latest sync.
            // Suspend cleanup is skipped under cancellation, so record the
            // failure in NonCancellable, then rethrow so the worker still
            // dies instead of scheduling a zombie retry.
            withContext(NonCancellable) {
                runs.finishRun(runId, clock.millis(), SyncResult.FAILED.name, "cancelled")
            }
            throw e
        }
    }

    override suspend fun refreshAll(force: Boolean): RefreshResult = refreshMutex.withLock {
        // Deterministic order: map iteration follows DI registration.
        // A fresh-enough source is skipped (recorded, not fetched): pull and
        // worker share this gate, manual pulls bypass it with force.
        val now = clock.millis()
        val results = sources.keys.associateWith { source ->
            if (!force && isFresh(source, now)) {
                recordSkip(source, now, "skipped-fresh")
                RefreshResult.Ok
            } else if (!force && isWifiBlocked()) {
                recordSkip(source, now, "skipped-metered")
                RefreshResult.Ok
            } else {
                refresh(source)
            }
        }
        applyCrossCheck()
        return combineResults(results)
    }

    private suspend fun isUnchanged(source: String, incomingHash: String?): Boolean {
        if (incomingHash == null) return false
        val store = syncState ?: return false
        return shouldSkipHash(store.bodyHash(source), incomingHash)
    }

    private suspend fun isFresh(source: String, now: Long): Boolean {
        val last = runs.recentRuns(source, 1).firstOrNull() ?: return false
        return shouldSkipFresh(last.result, last.completedAt, now)
    }

    private suspend fun isWifiBlocked(): Boolean {
        if (syncPrefs?.wifiOnly() != true) return false
        return network?.isMetered() == true
    }

    private suspend fun recordSkip(source: String, now: Long, reason: String) {
        val runId = runs.insert(
            SyncRunEntity(
                source = source,
                startedAt = now,
                completedAt = null,
                result = null,
                error = null
            )
        )
        runs.finishRun(runId, now, SyncResult.OK.name, reason)
        runs.pruneKeepLast(source, MAX_SYNC_RUNS)
    }

    /**
     * Pinned cross-source comparison after every run, gated per pin on fetch
     * recency (a stale side skips its pin instead of confirming or conflicting
     * across mismatched time windows). Agreement raises both rows to
     * CROSS_CHECKED, disagreement drops both to TO_VERIFY, a lone usable-free
     * survivor of a broken pin is demoted too. Zen-roster ghosts are never
     * promoted by a pin. Confidence is sync-owned (favorites are not), and the
     * update intentionally leaves verifiedAt untouched: confidence is not
     * freshness (see docs/sources/openrouter.md).
     */
    private suspend fun applyCrossCheck() {
        val now = clock.millis()
        val all = offers.snapshotAll().map { it.toDomain() }
        val freshSources = sources.keys.filter { source ->
            isSourceFresh(runs.recentRuns(source, FRESH_RUN_LOOKBACK), now)
        }.toSet()
        val ghosts = all.filter {
            it.source == MODELS_DEV_SOURCE_ID &&
                it.providerId == ModelsDevSource.ZEN_PROVIDER &&
                it.confidence == Confidence.TO_VERIFY
        }.map { it.remoteId }.toSet()
        val result = crossCheck(all, OVERLAP_PINS, ghosts) { offer -> offer.source in freshSources }
        if (result.confirmed.isNotEmpty()) {
            offers.updateConfidence(result.confirmed.toList(), Confidence.CROSS_CHECKED.name)
        }
        if (result.conflicts.isNotEmpty()) {
            offers.updateConfidence(result.conflicts.toList(), Confidence.TO_VERIFY.name)
        }
    }

    override suspend fun setFavorite(remoteId: String, favorite: Boolean) {
        offers.setFavorite(remoteId, favorite)
    }

    override suspend fun eventsSince(sinceId: Long, types: List<String>): List<ChangeEvent> =
        events.eventsAfter(sinceId, types).map { it.toDomain() }

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
        const val FRESHNESS_WINDOW_MILLIS = 6L * 60 * 60 * 1000
        private const val FRESH_RUN_LOOKBACK = 20
        private const val MODELS_DEV_SOURCE_ID = "opencode-data"

        /**
         * Newest-first run scan for fetch recency: failed and unfinished runs
         * carry no signal and are skipped over, `skipped-metered` runs prove
         * nothing (no fetch happened), every other OK run (real fetch,
         * `skipped-hash`, `skipped-fresh`) attests freshness at its completion
         * time. No attesting run inside the window means stale.
         */
        internal fun isSourceFresh(runs: List<SyncRunEntity>, now: Long): Boolean {
            for (run in runs) {
                if (run.result == SyncResult.FAILED.name) continue
                if (run.result != SyncResult.OK.name) continue
                if (run.error == "skipped-metered") continue
                val at = run.completedAt ?: continue
                return now - at < FRESHNESS_WINDOW_MILLIS
            }
            return false
        }

        internal fun shouldSkipHash(storedHash: String?, incomingHash: String?): Boolean =
            incomingHash != null && storedHash == incomingHash

        internal fun shouldSkipFresh(result: String?, completedAt: Long?, now: Long): Boolean {
            if (result != SyncResult.OK.name) return false
            if (completedAt == null) return false
            return now - completedAt < FRESHNESS_WINDOW_MILLIS
        }

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
