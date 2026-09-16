/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencode.freeradar.domain.error.RefreshResult
import com.opencode.freeradar.domain.model.HealthState
import com.opencode.freeradar.domain.model.Offer
import com.opencode.freeradar.domain.repository.OfferRepository
import com.opencode.freeradar.notifications.SyncNotifier
import com.opencode.freeradar.ui.model.OfferFilter
import com.opencode.freeradar.domain.model.isUsableFree
import com.opencode.freeradar.ui.model.SourceFilter
import com.opencode.freeradar.ui.model.OfferUi
import com.opencode.freeradar.ui.model.UiText
import com.opencode.freeradar.ui.model.compatibleOnly
import com.opencode.freeradar.ui.model.facetCounts
import com.opencode.freeradar.ui.model.freeOnly
import com.opencode.freeradar.ui.model.toUi
import com.opencode.freeradar.ui.model.toUiText
import java.util.Locale
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val offers: List<OfferUi> = emptyList(),
    val filter: OfferFilter = OfferFilter.FREE,
    val sourceFilter: SourceFilter = SourceFilter.ALL_SOURCES,
    val query: String = "",
    val recentSearches: List<String> = emptyList(),
    val statusCounts: Map<OfferFilter, Int> = emptyMap(),
    val sourceCounts: Map<SourceFilter, Int> = emptyMap(),
    val showResetFilters: Boolean = false,
    val offline: Boolean = false,
    val lastSyncAt: Long? = null,
    val error: UiText? = null
)

sealed interface DashboardAction {
    data object Refresh : DashboardAction
    data class SelectFilter(val filter: OfferFilter) : DashboardAction
    data class SelectSource(val source: SourceFilter) : DashboardAction
    data object ResetFilters : DashboardAction
    data class OpenOffer(val remoteId: String) : DashboardAction
    data class SubmitSearch(val query: String) : DashboardAction
    data class ToggleFavorite(val remoteId: String, val favorite: Boolean) : DashboardAction
    data class Search(val query: String) : DashboardAction
    data object DismissError : DashboardAction
}

sealed interface DashboardEvent {
    data class OpenDetails(val remoteId: String) : DashboardEvent
}

class DashboardViewModel(
    private val repository: OfferRepository,
    private val gate: SyncNotifier
) : ViewModel() {

    companion object {
        const val MAX_RECENTS = 3
    }

    private val filter = MutableStateFlow(OfferFilter.FREE)
    private val sourceFilter = MutableStateFlow(SourceFilter.ALL_SOURCES)
    private val refreshing = MutableStateFlow(false)
    private val manualError = MutableStateFlow<UiText?>(null)
    private val query = MutableStateFlow("")
    private val recents = MutableStateFlow<List<String>>(emptyList())
    private val events = Channel<DashboardEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    // Full list once: facet counts need every option's base, and 8k rows
    // filter in memory in under a millisecond — no per-option DAO roundtrips.
    private val offersFlow = repository.observeOffers(false)

    private data class Prefs(
        val filter: OfferFilter,
        val source: SourceFilter,
        val refreshing: Boolean,
        val error: UiText?,
        val query: String,
        val recentSearches: List<String> = emptyList()
    )

    private val prefsFlow = combine(filter, sourceFilter, refreshing, manualError, query, ::Prefs)
        .combine(recents) { prefs, recents -> prefs.copy(recentSearches = recents) }

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

    val state = combine(tripleFlow, prefsFlow, filterQuery) { triple, prefs, activeQuery ->
        val (offers, health, lastRun) = triple
            val counts = facetCounts(offers, prefs.filter, prefs.source)
            DashboardUiState(
                isLoading = false,
                isRefreshing = prefs.refreshing,
                offers = offers
                    .filter { !prefs.filter.freeOnly() || it.freeStatus.isUsableFree() }
                    .filter { !prefs.filter.compatibleOnly() || it.openCodeCompatible }
                    .filter { prefs.source.sourceId == null || it.source == prefs.source.sourceId }
                    .filter { matchesQuery(it, activeQuery) }
                    .map { it.toUi() },
                filter = prefs.filter,
                sourceFilter = prefs.source,
                query = prefs.query,
                recentSearches = prefs.recentSearches,
                statusCounts = counts.status,
                sourceCounts = counts.source,
                showResetFilters = prefs.filter != OfferFilter.FREE ||
                    prefs.source != SourceFilter.ALL_SOURCES,
                offline = health.any { it.state == HealthState.UNAVAILABLE },
                lastSyncAt = lastRun?.completedAt ?: lastRun?.startedAt,
                error = prefs.error
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun onAction(action: DashboardAction) {
        when (action) {
            DashboardAction.Refresh -> viewModelScope.launch {
                // A second pull while the spinner is up would start a second
                // full sync: double watermark, double afterSync, and the first
                // finisher drops the flag while work is still in flight.
                // Launches run sequentially on Main with no suspension between
                // the check and the set, so this coalesces to the running sync.
                if (refreshing.value) return@launch
                refreshing.value = true
                try {
                    val watermark = gate.beforeSync()
                    when (val result = repository.refreshAll(force = true)) {
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
            is DashboardAction.SelectFilter -> filter.value = action.filter
            is DashboardAction.SelectSource -> sourceFilter.value = action.source
            DashboardAction.ResetFilters -> {
                filter.value = OfferFilter.FREE
                sourceFilter.value = SourceFilter.ALL_SOURCES
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
        }
    }
}
