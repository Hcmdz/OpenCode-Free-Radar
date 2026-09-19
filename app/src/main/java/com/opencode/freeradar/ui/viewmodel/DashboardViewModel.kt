/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencode.freeradar.data.local.SyncSettings
import com.opencode.freeradar.domain.error.RefreshResult
import com.opencode.freeradar.domain.model.ChangeType
import com.opencode.freeradar.domain.model.HealthState
import com.opencode.freeradar.domain.model.Offer
import com.opencode.freeradar.domain.repository.OfferRepository
import com.opencode.freeradar.notifications.SyncNotifier
import com.opencode.freeradar.ui.model.OfferFilter
import com.opencode.freeradar.ui.model.OfferSort
import com.opencode.freeradar.domain.model.isConfirmedFree
import com.opencode.freeradar.domain.model.isLocalProvider
import com.opencode.freeradar.ui.model.SourceFilter
import com.opencode.freeradar.ui.model.OfferUi
import com.opencode.freeradar.ui.model.UiText
import com.opencode.freeradar.ui.model.compatibleOnly
import com.opencode.freeradar.ui.model.facetCounts
import com.opencode.freeradar.ui.model.favoriteOnly
import com.opencode.freeradar.ui.model.freeOnly
import com.opencode.freeradar.ui.model.matches
import com.opencode.freeradar.ui.model.sortComparator
import com.opencode.freeradar.ui.model.toUi
import com.opencode.freeradar.ui.model.toUiText
import com.opencode.freeradar.util.NetworkMonitor
import java.util.Locale
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val offers: List<OfferUi> = emptyList(),
    val filter: OfferFilter = OfferFilter.FREE,
    val sourceFilter: SourceFilter = SourceFilter.ALL_SOURCES,
    val sort: OfferSort = OfferSort.RECENT,
    val showLocal: Boolean = false,
    val query: String = "",
    val recentSearches: List<String> = emptyList(),
    val statusCounts: Map<OfferFilter, Int> = emptyMap(),
    val sourceCounts: Map<SourceFilter, Int> = emptyMap(),
    val showResetFilters: Boolean = false,
    val offline: Boolean = false,
    val lastSyncAt: Long? = null,
    val error: UiText? = null,
    val isBaseEmpty: Boolean = true,
    val meteredWarning: Boolean = false,
    val pendingNew: Int = 0
)

sealed interface DashboardAction {
    data object Refresh : DashboardAction
    data class SelectFilter(val filter: OfferFilter) : DashboardAction
    data class SelectSource(val source: SourceFilter) : DashboardAction
    data class SelectSort(val sort: OfferSort) : DashboardAction
    data class SetShowLocal(val show: Boolean) : DashboardAction
    data object ResetFilters : DashboardAction
    // No-match escape hatch: ResetFilters keeps the typed query, so a
    // query-caused empty state needs all four selections cleared at once.
    data object ClearSearchAndFilters : DashboardAction
    data class OpenOffer(val remoteId: String) : DashboardAction
    data class SubmitSearch(val query: String) : DashboardAction
    data class ToggleFavorite(val remoteId: String, val favorite: Boolean) : DashboardAction
    data class Search(val query: String) : DashboardAction
    data object DismissError : DashboardAction
    data object MeteredSyncOnce : DashboardAction
    data object MeteredNeverWarn : DashboardAction
    data object MeteredLater : DashboardAction
    data object AckPendingNew : DashboardAction
}

sealed interface DashboardEvent {
    data class OpenDetails(val remoteId: String) : DashboardEvent
}

class DashboardViewModel(
    private val repository: OfferRepository,
    private val gate: SyncNotifier,
    private val syncSettings: SyncSettings? = null,
    private val network: NetworkMonitor? = null,
) : ViewModel() {

    companion object {
        const val MAX_RECENTS = 3
    }

    private val filter = MutableStateFlow(OfferFilter.FREE)
    private val sourceFilter = MutableStateFlow(SourceFilter.ALL_SOURCES)
    private val sort = MutableStateFlow(OfferSort.RECENT)
    private val showLocal = MutableStateFlow(false)
    private val refreshing = MutableStateFlow(false)
    private val manualError = MutableStateFlow<UiText?>(null)
    private val query = MutableStateFlow("")
    private val recents = MutableStateFlow<List<String>>(emptyList())
    private val meteredWarning = MutableStateFlow(false)
    private val seenEventBaseline = MutableStateFlow<Long?>(null)
    private val events = Channel<DashboardEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    // Declared before init: the init launches below read it, and an
    // undispatched Main.immediate start can reach the read before any
    // suspension point yields.
    // Full list once: facet counts need every option's base, and 8k rows
    // filter in memory in under a millisecond — no per-option DAO roundtrips.
    private val offersFlow = repository.observeOffers(false)

    init {
        // First install shows an empty catalog: fetch once automatically
        // instead of waiting for a tap. Once-only even on failure (the error
        // screen takes over); the daily worker owns later syncs.
        viewModelScope.launch {
            val prefs = syncSettings ?: return@launch
            if (prefs.firstSyncDone()) return@launch
            if (offersFlow.first().isNotEmpty()) {
                prefs.setFirstSyncDone()
                return@launch
            }
            prefs.setFirstSyncDone()
            if (autoSyncAllowed()) doRefresh(force = true)
        }
        viewModelScope.launch {
            seenEventBaseline.value = repository.latestEventId()
        }
    }

    private suspend fun autoSyncAllowed(): Boolean {
        if (syncSettings?.wifiOnly() != true) return true
        return network?.isMetered() != true
    }

    private data class Prefs(
        val filter: OfferFilter,
        val source: SourceFilter,
        val sort: OfferSort,
        val showLocal: Boolean,
        val refreshing: Boolean,
        val error: UiText?,
        val query: String,
        val recentSearches: List<String> = emptyList()
    )

    private val prefsFlow = combine(
        combine(filter, sourceFilter, sort, ::Triple),
        showLocal,
        combine(refreshing, manualError, query, ::Triple)
    ) { selections, local, flags ->
        Prefs(
            filter = selections.first,
            source = selections.second,
            sort = selections.third,
            showLocal = local,
            refreshing = flags.first,
            error = flags.second,
            query = flags.third
        )
    }.combine(recents) { prefs, recents -> prefs.copy(recentSearches = recents) }

    // Debounced for filtering only: the field itself always shows the raw
    // query, otherwise a stale display value reverts keystrokes mid-typing.
    private val filterQuery = query.debounce(300)

    /** Locale-fixed match: default lowercase breaks Turkish dotted-I. */
    private fun matchesQuery(offer: Offer, raw: String): Boolean {
        val q = raw.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) return true
        return offer.name.lowercase(Locale.ROOT).contains(q) ||
            offer.providerId.lowercase(Locale.ROOT).contains(q) ||
            offer.modelId.lowercase(Locale.ROOT).contains(q)
    }

    private val tripleFlow = combine(
        offersFlow,
        repository.observeHealth(),
        repository.observeLatestRun()
    ) { offers, health, lastRun -> Triple(offers, health, lastRun) }

    private val pendingNew: Flow<Int> = seenEventBaseline.flatMapLatest { baseline ->
        if (baseline == null) flowOf(0)
        else {
            repository.observeLatestRun().map {
                repository.eventsSince(baseline, listOf(ChangeType.BECAME_FREE.name)).size
            }
        }
    }

    val state = combine(tripleFlow, prefsFlow, filterQuery, meteredWarning, pendingNew) {
            triple, prefs, activeQuery, warning, pending ->
        val (offers, health, lastRun) = triple
            val counts = facetCounts(offers, prefs.filter, prefs.source, hideLocal = !prefs.showLocal)
            DashboardUiState(
                isLoading = false,
                isRefreshing = prefs.refreshing,
                offers = offers
                    .filter { prefs.showLocal || !it.providerId.isLocalProvider() }
                    .filter { !prefs.filter.freeOnly() || it.isConfirmedFree() }
                    .filter { !prefs.filter.compatibleOnly() || it.openCodeCompatible }
                    .filter { !prefs.filter.favoriteOnly() || it.favorite }
                    .filter { prefs.source.matches(it) }
                    .filter { matchesQuery(it, activeQuery) }
                    .sortedWith(sortComparator(prefs.sort))
                    .map { it.toUi() },
                filter = prefs.filter,
                sourceFilter = prefs.source,
                sort = prefs.sort,
                showLocal = prefs.showLocal,
                query = prefs.query,
                recentSearches = prefs.recentSearches,
                statusCounts = counts.status,
                sourceCounts = counts.source,
                showResetFilters = prefs.filter != OfferFilter.FREE ||
                    prefs.source != SourceFilter.ALL_SOURCES ||
                    prefs.showLocal,
                offline = health.any { it.state == HealthState.UNAVAILABLE },
                lastSyncAt = lastRun?.completedAt ?: lastRun?.startedAt,
                error = prefs.error,
                isBaseEmpty = offers.isEmpty(),
                meteredWarning = warning,
                pendingNew = pending
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    private suspend fun doRefresh(force: Boolean) {
        // A second pull while the spinner is up would start a second
        // full sync: double watermark, double afterSync, and the first
        // finisher drops the flag while work is still in flight.
        // Launches run sequentially on Main with no suspension between
        // the check and the set, so this coalesces to the running sync.
        if (refreshing.value) return
        refreshing.value = true
        try {
            val watermark = gate.beforeSync()
            when (val result = repository.refreshAll(force = force)) {
                RefreshResult.Ok, is RefreshResult.Partial -> {
                    manualError.value = null
                    gate.afterSync(watermark)
                }
                is RefreshResult.Failed -> manualError.value = result.error.toUiText()
            }
        } finally {
            refreshing.value = false
        }
    }

    fun onAction(action: DashboardAction) {
        when (action) {
            DashboardAction.Refresh -> viewModelScope.launch {
                if (refreshing.value) return@launch
                if (network?.isMetered() == true && syncSettings?.warnOnMetered() != false) {
                    meteredWarning.value = true
                    return@launch
                }
                doRefresh(force = true)
            }
            is DashboardAction.SelectFilter -> filter.value = action.filter
            is DashboardAction.SelectSource -> sourceFilter.value = action.source
            is DashboardAction.SelectSort -> sort.value = action.sort
            is DashboardAction.SetShowLocal -> showLocal.value = action.show
            DashboardAction.ResetFilters -> {
                filter.value = OfferFilter.FREE
                sourceFilter.value = SourceFilter.ALL_SOURCES
                sort.value = OfferSort.RECENT
                showLocal.value = false
            }
            DashboardAction.ClearSearchAndFilters -> {
                filter.value = OfferFilter.FREE
                sourceFilter.value = SourceFilter.ALL_SOURCES
                sort.value = OfferSort.RECENT
                showLocal.value = false
                query.value = ""
            }
            is DashboardAction.OpenOffer -> events.trySend(DashboardEvent.OpenDetails(action.remoteId))
            is DashboardAction.Search -> query.value = action.query
            is DashboardAction.SubmitSearch -> {
                val submitted = action.query.trim()
                if (submitted.isNotEmpty()) {
                    recents.value = (listOf(submitted) + recents.value.filter { it != submitted })
                        .take(MAX_RECENTS)
                }
            }
    is DashboardAction.ToggleFavorite -> viewModelScope.launch {
                repository.setFavorite(action.remoteId, action.favorite)
            }
    DashboardAction.DismissError -> manualError.value = null
    DashboardAction.MeteredSyncOnce -> viewModelScope.launch {
        meteredWarning.value = false
        doRefresh(force = true)
    }
    DashboardAction.MeteredNeverWarn -> viewModelScope.launch {
        syncSettings?.setWarnOnMetered(false)
        meteredWarning.value = false
        doRefresh(force = true)
    }
    DashboardAction.MeteredLater -> {
        meteredWarning.value = false
    }
    DashboardAction.AckPendingNew -> viewModelScope.launch {
        seenEventBaseline.value = repository.latestEventId()
    }
        }
    }
}
