/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.domain.usecase

import com.opencode.freeradar.domain.model.Offer

/**
 * Pinned same-model pair across two sources. Pins are static and reviewed —
 * never fuzzy auto-matching (a wrong match would corrupt confidence silently).
 */
data class OverlapPin(
    val firstRemoteId: String,
    val secondRemoteId: String,
    val reason: String
)

val OVERLAP_PINS = listOf(
    OverlapPin(
        firstRemoteId = "bothub/nemotron-3-ultra-550b-a55b:free",
        secondRemoteId = "nvidia/nemotron-3-ultra-550b-a55b:free",
        reason = "S1 bothub Nemotron 3 Ultra :free tracks the OpenRouter :free twin",
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
    val confirmed = mutableSetOf<String>()
    val conflicts = mutableSetOf<String>()
    for (pin in pins) {
        val first = byRemoteId[pin.firstRemoteId] ?: continue
        val second = byRemoteId[pin.secondRemoteId] ?: continue
        if (first.freeStatus == second.freeStatus) {
            confirmed += first.remoteId
            confirmed += second.remoteId
        } else {
            conflicts += first.remoteId
            conflicts += second.remoteId
        }
    }
    return CrossCheckResult(confirmed, conflicts)
}
