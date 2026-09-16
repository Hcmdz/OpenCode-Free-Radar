/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.domain.usecase

import com.opencode.freeradar.domain.model.ChangeEvent
import com.opencode.freeradar.domain.model.ChangeType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NotifiedIdsTest {

    private fun event(id: String, type: ChangeType, afterJson: String? = null) =
        ChangeEvent(id, type, null, afterJson, 0L)

    @Test
    fun `new and expired ids land in their section`() {
        val ids = notifiedIds(
            listOf(
                event("p/a", ChangeType.NEW_MODEL),
                event("p/b", ChangeType.BECAME_FREE),
                event("p/c", ChangeType.FREE_EXPIRED)
            )
        )
        assertEquals(NotifiedIds(listOf("p/a", "p/b"), listOf("p/c")), ids)
    }

    @Test
    fun `paid newcomer is excluded`() {
        val ids = notifiedIds(listOf(event("p/a", ChangeType.NEW_MODEL, "PAID")))
        assertEquals(NotifiedIds(emptyList(), emptyList()), ids)
    }

    @Test
    fun `duplicate events collapse to one id`() {
        val ids = notifiedIds(
            listOf(
                event("p/a", ChangeType.NEW_MODEL),
                event("p/a", ChangeType.BECAME_FREE)
            )
        )
        assertEquals(NotifiedIds(listOf("p/a"), emptyList()), ids)
    }

    @Test
    fun `sections are capped`() {
        val events = (1..(MAX_NOTIFIED_IDS + 10)).map {
            event("p/$it", ChangeType.BECAME_FREE)
        }
        val ids = notifiedIds(events)
        assertEquals(MAX_NOTIFIED_IDS, ids.newIds.size)
    }
}
