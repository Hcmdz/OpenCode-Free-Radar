/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.source.nvidia

import com.opencode.freeradar.data.source.remote.SourceOffer
import com.opencode.freeradar.data.source.remote.toOffer
import com.opencode.freeradar.domain.model.Confidence
import com.opencode.freeradar.domain.model.FreeStatus
import com.opencode.freeradar.domain.model.Offer

const val NVIDIA_SOURCE_ID = "nvidia-build"

/** Provenance for the free-tier rule (quota); card facts point at the card URL. */
const val NVIDIA_FAQ_URL = "https://build.nvidia.com/models"

/**
 * Null for non-chat cards (fail-closed). Prices stay null: NVIDIA publishes
 * no per-token billing, so UNKNOWN via [toOffer] — this source alone can
 * never yield FREE (Story 2).
 */
fun NvidiaCard.toSourceOffer(): SourceOffer? {
    if (!chatCapable) return null
    val licenseNote = license?.let { " License: $it." }.orEmpty()
    return SourceOffer(
        providerId = "nvidia",
        modelId = slug,
        name = displayName,
        inputPrice = null,
        outputPrice = null,
        contextLength = contextLength,
        maxOutputTokens = null,
        supportsTools = supportsTools,
        supportsVision = supportsVision,
        supportsStructuredOutput = supportsStructuredOutput,
        quota = "~40 RPM",
        conditions = "Free to prototype.$licenseNote See $NVIDIA_FAQ_URL.",
        officialUrl = canonical,
        sourceUrl = canonical,
    )
}

/**
 * Single confidence for the whole offer: the headline claim (LIMITED) is
 * derived from the FAQ, not the card — AUTOMATICALLY_DETECTED is the honest
 * level even though card facts are official.
 */
fun SourceOffer.toNvidiaOffer(now: Long): Offer =
    toOffer(now, NVIDIA_SOURCE_ID).copy(
        // Namespaced: S1 has its own "nvidia" provider (models.dev), and
        // remoteId is globally unique — "nvidia/<slug>" would collide.
        remoteId = "$NVIDIA_SOURCE_ID/${this.modelId}",
        freeStatus = FreeStatus.LIMITED,
        confidence = Confidence.AUTOMATICALLY_DETECTED,
    )
