/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.domain.usecase

import com.opencode.freeradar.domain.model.Offer

/** Mirrors NvidiaMapper.NVIDIA_SOURCE_ID as a literal: domain never imports data. */
private const val NVIDIA_SOURCE = "nvidia-build"

/**
 * Pinned S1↔S2 model pair. Pins are static and reviewed — never fuzzy
 * auto-matching (a wrong match would corrupt confidence silently).
 */
data class OverlapPin(
    val s1RemoteId: String,
    val nvidiaSlug: String,
    val reason: String
)

val OVERLAP_PINS = listOf(
    OverlapPin(
        s1RemoteId = "bothub/nemotron-3-ultra-550b-a55b:free",
        nvidiaSlug = "nemotron-3-ultra-550b-a55b",
        reason = "S1 bothub Nemotron 3 Ultra tracks the NVIDIA-hosted Nemotron 3 Ultra",
    ),
)

data class CrossCheckResult(
    val confirmed: Set<String>,
    val conflicts: Set<String>,
)

/**
 * Pure comparison over pinned pairs: agreement confirms both rows,
 * freeStatus disagreement conflicts both (neither side trusted alone).
 * Missing sides and unpinned offers are ignored.
 */
fun crossCheck(offers: List<Offer>, pins: List<OverlapPin> = OVERLAP_PINS): CrossCheckResult {
    val byRemoteId = offers.associateBy { it.remoteId }
    val byNvidiaSlug = offers
        .filter { it.source == NVIDIA_SOURCE }
        .associateBy { it.modelId }
    val confirmed = mutableSetOf<String>()
    val conflicts = mutableSetOf<String>()
    for (pin in pins) {
        val s1 = byRemoteId[pin.s1RemoteId] ?: continue
        val s2 = byNvidiaSlug[pin.nvidiaSlug] ?: continue
        if (s1.freeStatus == s2.freeStatus) {
            confirmed += s1.remoteId
            confirmed += s2.remoteId
        } else {
            conflicts += s1.remoteId
            conflicts += s2.remoteId
        }
    }
    return CrossCheckResult(confirmed, conflicts)
}
