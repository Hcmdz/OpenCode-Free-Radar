/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.domain.repository

import com.opencode.freeradar.domain.error.RefreshResult
import com.opencode.freeradar.domain.model.ChangeEvent
import com.opencode.freeradar.domain.model.Offer
import com.opencode.freeradar.domain.model.SourceHealth
import com.opencode.freeradar.domain.model.SyncRun
import kotlinx.coroutines.flow.Flow

interface OfferRepository {
    fun observeOffers(compatibleOnly: Boolean): Flow<List<Offer>>
    fun observeOffer(remoteId: String): Flow<Offer?>
    fun observeHistory(remoteId: String): Flow<List<ChangeEvent>>
    fun observeHealth(): Flow<List<SourceHealth>>
    fun observeLastRun(source: String): Flow<SyncRun?>
    suspend fun refresh(source: String): RefreshResult
    suspend fun setFavorite(remoteId: String, favorite: Boolean)
}
