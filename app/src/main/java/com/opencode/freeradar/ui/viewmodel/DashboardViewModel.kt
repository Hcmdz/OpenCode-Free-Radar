/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencode.freeradar.domain.error.RefreshResult
import com.opencode.freeradar.domain.model.FreeStatus
import com.opencode.freeradar.domain.model.HealthState
import com.opencode.freeradar.domain.repository.OfferRepository
import com.opencode.freeradar.notifications.SyncNotifier
import com.opencode.freeradar.ui.model.OfferFilter
import com.opencode.freeradar.ui.model.SourceFilter
import com.opencode.freeradar.ui.model.OfferUi
import com.opencode.freeradar.ui.model.UiText
import com.opencode.freeradar.ui.model.compatibleOnly
import com.opencode.freeradar.ui.model.facetCounts
import com.opencode.freeradar.ui.model.freeOnly
import com.opencode.freeradar.ui.model.toUi
import com.opencode.freeradar.ui.model.toUiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val offers: List<OfferUi> = emptyList(),
    val filter: OfferFilter = OfferFilter.FREE,
    val sourceFilter: SourceFilter = SourceFilter.ALL_SOURCES,
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
    data object DismissError : DashboardAction
}

sealed interface DashboardEvent {
    data class OpenDetails(val remoteId: String) : DashboardEvent
}

class DashboardViewModel(
    private val repository: OfferRepository,
    private val gate: SyncNotifier
) : ViewModel() {

    private val filter = MutableStateFlow(OfferFilter.FREE)
    private val sourceFilter = MutableStateFlow(SourceFilter.ALL_SOURCES)
    private val manualError = MutableStateFlow<UiText?>(null)
    private val events = Channel<DashboardEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    // Full list once: facet counts need every option's base, and 8k rows
    // filter in memory in under a millisecond — no per-option DAO roundtrips.
    private val offersFlow = repository.observeOffers(false)

    private data class Prefs(val filter: OfferFilter, val source: SourceFilter, val error: UiText?)

    private val prefsFlow = combine(filter, sourceFilter, manualError, ::Prefs)

    val state = combine(
        offersFlow,
        repository.observeHealth(),
        repository.observeLatestRun()
    ) { offers, health, lastRun -> Triple(offers, health, lastRun) }
        .combine(prefsFlow) { (offers, health, lastRun), prefs ->
            val counts = facetCounts(offers, prefs.filter, prefs.source)
            DashboardUiState(
                isLoading = false,
                offers = offers
                    .filter { !prefs.filter.freeOnly() || it.freeStatus == FreeStatus.FREE }
                    .filter { !prefs.filter.compatibleOnly() || it.openCodeCompatible }
                    .filter { prefs.source.sourceId == null || it.source == prefs.source.sourceId }
                    .map { it.toUi() },
                filter = prefs.filter,
                sourceFilter = prefs.source,
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
                val watermark = gate.beforeSync()
                when (val result = repository.refreshAll()) {
                    RefreshResult.Ok, is RefreshResult.Partial -> {
                        manualError.value = null
                        gate.afterSync(watermark)
                    }
                    is RefreshResult.Failed -> manualError.value = result.error.toUiText()
                }
            }
            is DashboardAction.SelectFilter -> filter.value = action.filter
            is DashboardAction.SelectSource -> sourceFilter.value = action.source
            DashboardAction.ResetFilters -> {
                filter.value = OfferFilter.FREE
                sourceFilter.value = SourceFilter.ALL_SOURCES
            }
            is DashboardAction.OpenOffer -> events.trySend(DashboardEvent.OpenDetails(action.remoteId))
            DashboardAction.DismissError -> manualError.value = null
        }
    }
}
