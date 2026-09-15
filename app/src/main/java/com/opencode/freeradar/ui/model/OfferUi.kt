/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.model

import com.opencode.freeradar.domain.model.FreeStatus
import com.opencode.freeradar.domain.model.Offer

data class OfferUi(
    val remoteId: String,
    val name: String,
    val providerId: String,
    val freeStatus: FreeStatus,
    val contextLength: Int?,
    val verifiedAt: Long,
    val sourceLabel: String
)

/**
 * Display label for the catalog source. Proper nouns need no translation;
 * unknown ids fall back to the raw id so future sources never render blank.
 */
fun sourceLabel(source: String): String = when (source) {
    "opencode-data" -> "OpenCode"
    "nvidia-build" -> "NVIDIA"
    else -> source
}

fun Offer.toUi(): OfferUi = OfferUi(
    remoteId = remoteId,
    name = name,
    providerId = providerId,
    freeStatus = freeStatus,
    contextLength = contextLength,
    verifiedAt = verifiedAt,
    sourceLabel = sourceLabel(source)
)
