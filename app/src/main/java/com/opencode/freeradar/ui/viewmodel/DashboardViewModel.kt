/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencode.freeradar.domain.error.RefreshResult
import com.opencode.freeradar.domain.model.FreeStatus
import com.opencode.freeradar.domain.model.HealthState
import com.opencode.freeradar.domain.repository.OfferRepository
import com.opencode.freeradar.ui.model.OfferFilter
import com.opencode.freeradar.ui.model.OfferUi
import com.opencode.freeradar.ui.model.UiText
import com.opencode.freeradar.ui.model.toUi
import com.opencode.freeradar.ui.model.toUiText
import com.opencode.freeradar.worker.SyncWorker
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val offers: List<OfferUi> = emptyList(),
    val filter: OfferFilter = OfferFilter.FREE,
    val offline: Boolean = false,
    val lastSyncAt: Long? = null,
    val error: UiText? = null
)

sealed interface DashboardAction {
    data object Refresh : DashboardAction
    data class SelectFilter(val filter: OfferFilter) : DashboardAction
    data class OpenOffer(val remoteId: String) : DashboardAction
    data object DismissError : DashboardAction
}

sealed interface DashboardEvent {
    data class OpenDetails(val remoteId: String) : DashboardEvent
}

class DashboardViewModel(private val repository: OfferRepository) : ViewModel() {

    private val filter = MutableStateFlow(OfferFilter.FREE)
    private val manualError = MutableStateFlow<UiText?>(null)
    private val events = Channel<DashboardEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    private fun OfferFilter.compatibleOnly(): Boolean = when (this) {
        OfferFilter.ALL, OfferFilter.FREE -> false
        OfferFilter.COMPATIBLE, OfferFilter.FREE_COMPATIBLE -> true
    }

    private fun OfferFilter.freeOnly(): Boolean = when (this) {
        OfferFilter.ALL, OfferFilter.COMPATIBLE -> false
        OfferFilter.FREE, OfferFilter.FREE_COMPATIBLE -> true
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val offersFlow = filter.flatMapLatest { repository.observeOffers(it.compatibleOnly()) }

    private data class Prefs(val filter: OfferFilter, val error: UiText?)

    private val prefsFlow = combine(filter, manualError, ::Prefs)

    val state = combine(
        offersFlow,
        repository.observeHealth(),
        repository.observeLastRun(SyncWorker.SOURCE_ID)
    ) { offers, health, lastRun -> Triple(offers, health, lastRun) }
        .combine(prefsFlow) { (offers, health, lastRun), prefs ->
            DashboardUiState(
                isLoading = false,
                offers = offers
                    .filter { !prefs.filter.freeOnly() || it.freeStatus == FreeStatus.FREE }
                    .filter { !prefs.filter.compatibleOnly() || it.openCodeCompatible }
                    .map { it.toUi() },
                filter = prefs.filter,
                offline = health.any { it.state == HealthState.UNAVAILABLE },
                lastSyncAt = lastRun?.completedAt ?: lastRun?.startedAt,
                error = prefs.error
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun onAction(action: DashboardAction) {
        when (action) {
            DashboardAction.Refresh -> viewModelScope.launch {
                when (val result = repository.refresh(SyncWorker.SOURCE_ID)) {
                    is RefreshResult.Ok -> manualError.value = null
                    is RefreshResult.Failed -> manualError.value = result.error.toUiText()
                    is RefreshResult.Partial -> manualError.value = null
                }
            }
            is DashboardAction.SelectFilter -> filter.value = action.filter
            is DashboardAction.OpenOffer -> events.trySend(DashboardEvent.OpenDetails(action.remoteId))
            DashboardAction.DismissError -> manualError.value = null
        }
    }
}
