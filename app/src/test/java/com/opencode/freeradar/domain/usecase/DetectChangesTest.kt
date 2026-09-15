/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.domain.usecase

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import com.opencode.freeradar.domain.model.ChangeEvent
import com.opencode.freeradar.domain.model.ChangeType
import com.opencode.freeradar.domain.model.Confidence
import com.opencode.freeradar.domain.model.FreeStatus
import com.opencode.freeradar.domain.model.Offer
import org.junit.jupiter.api.Test

class DetectChangesTest {

    private fun offer(
        remoteId: String = "p/m",
        freeStatus: FreeStatus = FreeStatus.FREE,
        confidence: Confidence = Confidence.OFFICIAL,
        inputPrice: Double? = 0.0,
        outputPrice: Double? = 0.0,
        contextLength: Int? = 1000,
        supportsTools: Boolean? = true,
        quota: String? = null
    ) = Offer(
        remoteId = remoteId,
        providerId = "p",
        modelId = "m",
        name = "M",
        inputPrice = inputPrice,
        outputPrice = outputPrice,
        freeStatus = freeStatus,
        quota = quota,
        quotaPeriod = null,
        temporary = false,
        conditions = null,
        confidence = confidence,
        contextLength = contextLength,
        maxOutputTokens = null,
        supportsTools = supportsTools,
        supportsVision = false,
        supportsStructuredOutput = false,
        openCodeCompatible = true,
        officialUrl = null,
        source = "opencode-data",
        sourceUrl = null,
        retrievedAt = 1_000L,
        verifiedAt = 1_000L,
        favorite = false
    )

    private fun types(events: List<ChangeEvent>) = events.map { it.type }.toSet()

    @Test
    fun `new model emits NEW_MODEL`() {
        val events = detectChanges(old = emptyList(), new = listOf(offer()), now = 2_000L)
        assertThat(events).hasSize(1)
        assertThat(events.first().type).isEqualTo(ChangeType.NEW_MODEL)
        assertThat(events.first().offerRemoteId).isEqualTo("p/m")
    }

    @Test
    fun `removed model emits MODEL_REMOVED`() {
        val events = detectChanges(old = listOf(offer()), new = emptyList(), now = 2_000L)
        assertThat(events).hasSize(1)
        assertThat(events.first().type).isEqualTo(ChangeType.MODEL_REMOVED)
    }

    @Test
    fun `paid to free emits BECAME_FREE`() {
        val events = detectChanges(
            old = listOf(offer(freeStatus = FreeStatus.PAID, inputPrice = 1.0)),
            new = listOf(offer(freeStatus = FreeStatus.FREE, inputPrice = 0.0)),
            now = 2_000L
        )
        assertThat(types(events)).isEqualTo(setOf(ChangeType.BECAME_FREE, ChangeType.PRICE_CHANGED))
    }

    @Test
    fun `paid to limited emits BECAME_FREE`() {
        // The Zen pattern: becoming free-with-conditions must alert like
        // becoming FREE (e.g. a Muse Spark-style limited trial appears).
        val events = detectChanges(
            old = listOf(offer(freeStatus = FreeStatus.PAID, inputPrice = 1.0)),
            new = listOf(offer(freeStatus = FreeStatus.LIMITED, inputPrice = 0.0)),
            now = 2_000L
        )
        assertThat(types(events).contains(ChangeType.BECAME_FREE)).isEqualTo(true)
    }

    @Test
    fun `free to paid emits FREE_EXPIRED`() {
        val events = detectChanges(
            old = listOf(offer()),
            new = listOf(offer(freeStatus = FreeStatus.PAID, inputPrice = 2.0)),
            now = 2_000L
        )
        assertThat(types(events)).isEqualTo(setOf(ChangeType.FREE_EXPIRED, ChangeType.PRICE_CHANGED))
    }

    @Test
    fun `context change emits CONTEXT_CHANGED`() {
        val events = detectChanges(
            old = listOf(offer()),
            new = listOf(offer(contextLength = 2000)),
            now = 2_000L
        )
        assertThat(types(events)).isEqualTo(setOf(ChangeType.CONTEXT_CHANGED))
    }

    @Test
    fun `tool support change emits TOOL_SUPPORT_CHANGED`() {
        val events = detectChanges(
            old = listOf(offer()),
            new = listOf(offer(supportsTools = false)),
            now = 2_000L
        )
        assertThat(types(events)).isEqualTo(setOf(ChangeType.TOOL_SUPPORT_CHANGED))
    }

    @Test
    fun `quota change emits LIMIT_CHANGED`() {
        val events = detectChanges(
            old = listOf(offer()),
            new = listOf(offer(quota = "50/day")),
            now = 2_000L
        )
        assertThat(types(events)).isEqualTo(setOf(ChangeType.LIMIT_CHANGED))
    }

    @Test
    fun `identical offers emit nothing`() {
        val events = detectChanges(old = listOf(offer()), new = listOf(offer()), now = 2_000L)
        assertThat(events).hasSize(0)
    }

    @Test
    fun `free to unknown never emits FREE_EXPIRED`() {
        val events = detectChanges(
            old = listOf(offer()),
            new = listOf(offer(freeStatus = FreeStatus.UNKNOWN, inputPrice = null, outputPrice = null)),
            now = 2_000L
        )
        assertThat(types(events).contains(ChangeType.FREE_EXPIRED)).isEqualTo(false)
    }

    @Test
    fun `free to limited trial or temporary never emits FREE_EXPIRED`() {
        // Refinement rollout: still usable at $0 with conditions, so no
        // false "expired" alarm (same rationale as the UNKNOWN guard).
        for (status in listOf(FreeStatus.LIMITED, FreeStatus.TRIAL, FreeStatus.TEMPORARY)) {
            val events = detectChanges(
                old = listOf(offer()),
                new = listOf(offer(freeStatus = status)),
                now = 2_000L
            )
            assertThat(types(events).contains(ChangeType.FREE_EXPIRED)).isEqualTo(false)
        }
    }

    @Test
    fun `unverified newcomer emits nothing`() {
        // A ghost (absent from the Zen roster) appears in the list but
        // never rings the bell until the roster confirms it.
        val events = detectChanges(
            old = emptyList(),
            new = listOf(
                offer(freeStatus = FreeStatus.LIMITED, confidence = Confidence.TO_VERIFY)
            ),
            now = 2_000L
        )
        assertThat(events).hasSize(0)
    }

    @Test
    fun `roster confirmation emits BECAME_FREE`() {
        // TO_VERIFY + usable-free flipping to confirmed usable-free is a
        // new deal (e.g. a Muse Spark-style trial confirmed on Zen).
        val events = detectChanges(
            old = listOf(offer(freeStatus = FreeStatus.LIMITED, confidence = Confidence.TO_VERIFY)),
            new = listOf(offer(freeStatus = FreeStatus.LIMITED)),
            now = 2_000L
        )
        assertThat(types(events)).isEqualTo(setOf(ChangeType.BECAME_FREE))
    }

    @Test
    fun `unknown to free emits BECAME_FREE`() {
        val events = detectChanges(
            old = listOf(offer(freeStatus = FreeStatus.UNKNOWN, inputPrice = null, outputPrice = null)),
            new = listOf(offer()),
            now = 2_000L
        )
        assertThat(types(events).contains(ChangeType.BECAME_FREE)).isEqualTo(true)
    }
}
