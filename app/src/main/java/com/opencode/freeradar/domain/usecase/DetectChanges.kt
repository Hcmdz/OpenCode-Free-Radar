/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.domain.usecase

import com.opencode.freeradar.domain.model.ChangeEvent
import com.opencode.freeradar.domain.model.ChangeType
import com.opencode.freeradar.domain.model.FreeStatus
import com.opencode.freeradar.domain.model.Offer
import com.opencode.freeradar.domain.model.isUsableFree

fun detectChanges(old: List<Offer>, new: List<Offer>, now: Long): List<ChangeEvent> {
    val oldById = old.associateBy { it.remoteId }
    val newById = new.associateBy { it.remoteId }
    val events = mutableListOf<ChangeEvent>()

    for ((id, current) in newById) {
        val previous = oldById[id]
        if (previous == null) {
            // Status stamp for notification counting: only usable-free
            // newcomers ring the free bell (legacy rows have null = counted).
            events += ChangeEvent(id, ChangeType.NEW_MODEL, null, current.freeStatus.name, now)
            continue
        }
        if (!previous.freeStatus.isUsableFree() && current.freeStatus.isUsableFree()) {
            // Becoming free-with-conditions alerts like becoming FREE (e.g. a
            // Muse Spark-style limited trial): that is the app's purpose.
            events += ChangeEvent(id, ChangeType.BECAME_FREE, previous.freeStatus.name, current.freeStatus.name, now)
        }
        if (previous.freeStatus == FreeStatus.FREE &&
            (current.freeStatus == FreeStatus.PAID || current.freeStatus == FreeStatus.EXPIRED)
        ) {
            // Missing cost maps to UNKNOWN, never expiry (fail-closed per spec FR-002).
            // Refinements to LIMITED/TRIAL/TEMPORARY stay usable at $0 with
            // conditions, so they never ring the expiry alarm either.
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
        if (previous.quota != current.quota) {
            events += ChangeEvent(id, ChangeType.LIMIT_CHANGED, previous.quota, current.quota, now)
        }
    }

    for (id in oldById.keys - newById.keys) {
        events += ChangeEvent(id, ChangeType.MODEL_REMOVED, null, null, now)
    }
    return events
}
