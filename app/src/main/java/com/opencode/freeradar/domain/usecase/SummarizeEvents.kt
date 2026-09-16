/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.domain.usecase

import com.opencode.freeradar.domain.model.ChangeEvent
import com.opencode.freeradar.domain.model.ChangeType
import com.opencode.freeradar.domain.model.FreeStatus
import com.opencode.freeradar.domain.model.isUsableFree

data class EventSummary(val newFree: Int, val expired: Int)

val V1_NOTIFY_TYPES = setOf(
    ChangeType.NEW_MODEL,
    ChangeType.BECAME_FREE,
    ChangeType.FREE_EXPIRED
)

/**
 * A NEW_MODEL counts only when stamped usable-free. Legacy rows (null
 * stamp, recorded before stamping) and unreadable stamps fail open and
 * still count — never silently drop a possible deal.
 */
/** Snapshot of the offer ids behind a notification: ids frozen, content live. */
data class NotifiedIds(val newIds: List<String>, val expiredIds: List<String>)

/** Binder ceiling: extras never carry more than this per section. */
const val MAX_NOTIFIED_IDS = 50

internal fun ChangeEvent.countsAsNewFree(): Boolean = when (type) {
    ChangeType.BECAME_FREE -> true
    ChangeType.NEW_MODEL -> {
        val stamp = afterJson
        stamp == null ||
            runCatching { FreeStatus.valueOf(stamp) }.getOrDefault(FreeStatus.FREE).isUsableFree()
    }
    else -> false
}

fun summarizeEvents(events: List<ChangeEvent>): EventSummary? {
    var newFree = 0
    var expired = 0
    for (event in events) {
        when (event.type) {
            ChangeType.NEW_MODEL, ChangeType.BECAME_FREE ->
                if (event.countsAsNewFree()) newFree++
            ChangeType.FREE_EXPIRED -> expired++
            else -> Unit
        }
    }
    return if (newFree == 0 && expired == 0) null else EventSummary(newFree, expired)
}

fun notifiedIds(events: List<ChangeEvent>): NotifiedIds {
    val newIds = events
        .filter {
            (it.type == ChangeType.NEW_MODEL || it.type == ChangeType.BECAME_FREE) &&
                it.countsAsNewFree()
        }
        .map { it.offerRemoteId }
        .distinct()
        .take(MAX_NOTIFIED_IDS)
    val expiredIds = events
        .filter { it.type == ChangeType.FREE_EXPIRED }
        .map { it.offerRemoteId }
        .distinct()
        .take(MAX_NOTIFIED_IDS)
    return NotifiedIds(newIds, expiredIds)
}
