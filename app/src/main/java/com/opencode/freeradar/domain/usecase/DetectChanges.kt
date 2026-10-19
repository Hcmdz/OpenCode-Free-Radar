/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.domain.usecase

import com.opencode.freeradar.domain.model.ChangeEvent
import com.opencode.freeradar.domain.model.ChangeType
import com.opencode.freeradar.domain.model.Confidence
import com.opencode.freeradar.domain.model.FreeStatus
import com.opencode.freeradar.domain.model.Offer
import com.opencode.freeradar.domain.model.isConfirmedFree
import com.opencode.freeradar.domain.model.isGatedProvider
import com.opencode.freeradar.domain.model.isUsableFree

fun detectChanges(old: List<Offer>, new: List<Offer>, now: Long): List<ChangeEvent> {
    val oldById = old.associateBy { it.remoteId }
    val newById = new.associateBy { it.remoteId }
    val events = mutableListOf<ChangeEvent>()

    for ((id, current) in newById) {
        val previous = oldById[id]
        if (previous == null) {
            // Unverified newcomers (e.g. Zen-roster ghosts) appear silently:
            // no bell until a source confirms them. The status stamp drives
            // notification counting (legacy rows have null = counted).
            if (current.confidence != Confidence.TO_VERIFY) {
                events += ChangeEvent(id, ChangeType.NEW_MODEL, null, current.freeStatus.name, now)
            }
            continue
        }
        if (!previous.isConfirmedFree() && current.isConfirmedFree()) {
            // Becoming confirmed usable-free alerts like becoming FREE: a
            // price drop to $0 and a roster confirmation (e.g. a Muse
            // Spark-style limited trial) are both new deals.
            events += ChangeEvent(id, ChangeType.BECAME_FREE, previous.freeStatus.name, current.freeStatus.name, now)
        }
        // Confidence is deliberately not part of this test. The two catalogs
        // never publish a withdrawal at the same time: they disagree, the
        // cross-check demotes both rows to TO_VERIFY, and from then on the
        // withdrawal it was caused by could never be announced again. What the
        // user was shown is the price, not the confidence: a row that *was*
        // usable-free has to ring when it stops being so. Gating still holds —
        // a paywalled row was never a free one to lose.
        if (previous.freeStatus.isUsableFree() && !previous.providerId.isGatedProvider() &&
            (current.freeStatus == FreeStatus.PAID || current.freeStatus == FreeStatus.EXPIRED ||
             current.providerId.isGatedProvider())
        ) {
            // Missing cost maps to UNKNOWN, never expiry (fail-closed per spec FR-002).
            // Refinements between usable-free states (FREE→TEMPORARY and the
            // like) stay usable at $0 with conditions, so they never ring
            // the expiry alarm either — only a free row leaving
            // usable-free does. Gated rows never rang, never will.
            events += ChangeEvent(id, ChangeType.FREE_EXPIRED, previous.freeStatus.name, current.freeStatus.name, now)
        }
        if (previous.inputPrice != current.inputPrice || previous.outputPrice != current.outputPrice) {
            events += ChangeEvent(
                id, ChangeType.PRICE_CHANGED,
                "${previous.inputPrice}/${previous.outputPrice}",
                "${current.inputPrice}/${current.outputPrice}",
                now
            )
        }
        if (previous.contextLength != current.contextLength ||
            previous.maxOutputTokens != current.maxOutputTokens
        ) {
            events += ChangeEvent(
                id, ChangeType.CONTEXT_CHANGED,
                "${previous.contextLength}/${previous.maxOutputTokens}",
                "${current.contextLength}/${current.maxOutputTokens}",
                now
            )
        }
        if (previous.supportsTools != current.supportsTools) {
            events += ChangeEvent(
                id, ChangeType.TOOL_SUPPORT_CHANGED,
                previous.supportsTools.toString(), current.supportsTools.toString(), now
            )
        }
        if (previous.supportsVision != current.supportsVision) {
            events += ChangeEvent(
                id, ChangeType.VISION_CHANGED,
                previous.supportsVision.toString(), current.supportsVision.toString(), now
            )
        }
        if (previous.conditions != current.conditions || previous.temporary != current.temporary) {
            events += ChangeEvent(
                id, ChangeType.CONDITIONS_CHANGED,
                "${previous.conditions}/${previous.temporary}",
                "${current.conditions}/${current.temporary}",
                now
            )
        }
        if (previous.quota != current.quota) {
            events += ChangeEvent(id, ChangeType.LIMIT_CHANGED, previous.quota, current.quota, now)
        }
    }

    for (id in oldById.keys - newById.keys) {
        events += ChangeEvent(id, ChangeType.MODEL_REMOVED, null, null, now)
    }
    return events
}
