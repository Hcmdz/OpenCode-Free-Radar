/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencode.freeradar.domain.model.ChangeEvent
import com.opencode.freeradar.domain.model.Offer
import com.opencode.freeradar.domain.repository.OfferRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailsUiState(
    val isLoading: Boolean = true,
    val offer: Offer? = null,
    val history: List<ChangeEvent> = emptyList()
)

class DetailsViewModel(
    private val offerId: String,
    private val repository: OfferRepository
) : ViewModel() {

    val state = combine(
        repository.observeOffer(offerId),
        repository.observeHistory(offerId)
    ) { offer, history ->
        DetailsUiState(isLoading = false, offer = offer, history = history)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailsUiState())

    fun toggleFavorite() {
        viewModelScope.launch {
            val current = state.value.offer ?: return@launch
            repository.setFavorite(current.remoteId, !current.favorite)
        }
    }
}
