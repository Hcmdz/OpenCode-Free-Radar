/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.domain.repository

import com.opencode.freeradar.data.source.remote.SourceOffer
import com.opencode.freeradar.domain.error.Result
import com.opencode.freeradar.domain.error.SourceError

interface OfferSource {
    val id: String
    suspend fun fetch(): Result<FetchResult, SourceError>
}

data class FetchResult(
    val offers: List<SourceOffer>,
    /** Hex hash of the raw bodies backing this fetch, or null when unknown. */
    val bodyHash: String?,
)
