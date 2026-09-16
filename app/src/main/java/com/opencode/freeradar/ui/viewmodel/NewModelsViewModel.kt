/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencode.freeradar.domain.repository.OfferRepository
import com.opencode.freeradar.ui.model.OfferUi
import com.opencode.freeradar.ui.model.toUi
import com.opencode.freeradar.ui.navigation.NewModels
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NewModelsUiState(
    val isLoading: Boolean = true,
    val newModels: List<OfferUi> = emptyList(),
    val expired: List<OfferUi> = emptyList()
)

class NewModelsViewModel(
    private val key: NewModels,
    private val repository: OfferRepository
) : ViewModel() {

    val state = repository.observeOffers(false)
        .map { offers ->
            val byId = offers.associateBy { it.remoteId }
            NewModelsUiState(
                isLoading = false,
                newModels = key.newIds.mapNotNull { byId[it]?.toUi() },
                expired = key.expiredIds.mapNotNull { byId[it]?.toUi() }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NewModelsUiState())

    fun toggleFavorite(remoteId: String, favorite: Boolean) {
        viewModelScope.launch { repository.setFavorite(remoteId, favorite) }
    }
}
