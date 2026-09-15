/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.domain.usecase

import com.opencode.freeradar.domain.model.ChangeEvent
import com.opencode.freeradar.domain.model.ChangeType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SummarizeEventsTest {

    private fun event(type: ChangeType, afterJson: String? = null) =
        ChangeEvent("p/m", type, null, afterJson, 0L)

    @Test
    fun `empty list yields no summary`() {
        assertNull(summarizeEvents(emptyList()))
    }

    @Test
    fun `non-V1 types are ignored`() {
        val events = listOf(
            event(ChangeType.PRICE_CHANGED),
            event(ChangeType.MODEL_REMOVED),
            event(ChangeType.SOURCE_UNAVAILABLE)
        )
        assertNull(summarizeEvents(events))
    }

    @Test
    fun `new models and became-free count as new`() {
        val summary = summarizeEvents(
            listOf(
                event(ChangeType.NEW_MODEL),
                event(ChangeType.NEW_MODEL),
                event(ChangeType.BECAME_FREE)
            )
        )
        assertEquals(EventSummary(newFree = 3, expired = 0), summary)
    }

    @Test
    fun `new paid model does not count as new free`() {
        // A newcomer with a price tag must never ring the free bell.
        assertNull(summarizeEvents(listOf(event(ChangeType.NEW_MODEL, "PAID"))))
    }

    @Test
    fun `new limited model counts as new free`() {
        val summary = summarizeEvents(listOf(event(ChangeType.NEW_MODEL, "LIMITED")))
        assertEquals(EventSummary(newFree = 1, expired = 0), summary)
    }

    @Test
    fun `legacy new model without status still counts`() {
        // Fail open: rows recorded before the status stamp stay counted.
        val summary = summarizeEvents(listOf(event(ChangeType.NEW_MODEL)))
        assertEquals(EventSummary(newFree = 1, expired = 0), summary)
    }

    @Test
    fun `expired counts separately from new`() {
        val summary = summarizeEvents(
            listOf(event(ChangeType.BECAME_FREE), event(ChangeType.FREE_EXPIRED))
        )
        assertEquals(EventSummary(newFree = 1, expired = 1), summary)
    }
}
