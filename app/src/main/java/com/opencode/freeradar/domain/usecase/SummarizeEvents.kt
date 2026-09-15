/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.domain.usecase

import com.opencode.freeradar.domain.model.ChangeEvent
import com.opencode.freeradar.domain.model.ChangeType

data class EventSummary(val newFree: Int, val expired: Int)

val V1_NOTIFY_TYPES = setOf(
    ChangeType.NEW_MODEL,
    ChangeType.BECAME_FREE,
    ChangeType.FREE_EXPIRED
)

fun summarizeEvents(events: List<ChangeEvent>): EventSummary? {
    var newFree = 0
    var expired = 0
    for (event in events) {
        when (event.type) {
            ChangeType.NEW_MODEL, ChangeType.BECAME_FREE -> newFree++
            ChangeType.FREE_EXPIRED -> expired++
            else -> Unit
        }
    }
    return if (newFree == 0 && expired == 0) null else EventSummary(newFree, expired)
}
