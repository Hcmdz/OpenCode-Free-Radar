/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.domain.usecase

import com.opencode.freeradar.domain.model.FreeStatus
import com.opencode.freeradar.domain.model.Offer
import com.opencode.freeradar.domain.model.isUsableFree

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
 * Pure comparison over pinned pairs with three issues per pin: agreement
 * confirms both rows, usable-free disagreement conflicts both (neither side
 * trusted alone), and UNKNOWN on either side skips the pin (missing data is
 * not a disagreement). A pin with only one side present demotes a usable-free
 * survivor (delisted elsewhere stays unconfirmed); both sides missing and
 * unpinned offers are ignored.
 *
 * [excludedFromConfirm] holds rows that may never be promoted (e.g. Zen-roster
 * ghosts) — disagreements still demote them. [isFresh] gates each side on
 * fetch recency; a stale side skips the pin instead of confirming or
 * conflicting on mismatched time windows. Remote ids match exactly.
 */
fun crossCheck(
    offers: List<Offer>,
    pins: List<OverlapPin> = OVERLAP_PINS,
    excludedFromConfirm: Set<String> = emptySet(),
    isFresh: (Offer) -> Boolean = { true },
): CrossCheckResult {
    val byRemoteId = offers.associateBy { it.remoteId }
    val confirmed = mutableSetOf<String>()
    val conflicts = mutableSetOf<String>()
    for (pin in pins) {
        val first = byRemoteId[pin.firstRemoteId]
        val second = byRemoteId[pin.secondRemoteId]
        if (first == null && second == null) continue
        if (first == null || second == null) {
            val solo = first ?: second!!
            if (solo.freeStatus.isUsableFree() && isFresh(solo)) {
                conflicts += solo.remoteId
            }
            continue
        }
        if (!isFresh(first) || !isFresh(second)) continue
        if (first.freeStatus == FreeStatus.UNKNOWN || second.freeStatus == FreeStatus.UNKNOWN) continue
        val agree = first.freeStatus == second.freeStatus ||
            (first.freeStatus.isUsableFree() && second.freeStatus.isUsableFree())
        if (agree) {
            if (pin.firstRemoteId !in excludedFromConfirm && pin.secondRemoteId !in excludedFromConfirm) {
                confirmed += first.remoteId
                confirmed += second.remoteId
            }
        } else {
            conflicts += first.remoteId
            conflicts += second.remoteId
        }
    }
    return CrossCheckResult(confirmed, conflicts)
}
