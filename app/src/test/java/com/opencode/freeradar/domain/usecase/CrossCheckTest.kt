/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.domain.usecase

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import com.opencode.freeradar.domain.model.Confidence
import com.opencode.freeradar.domain.model.FreeStatus
import com.opencode.freeradar.domain.model.Offer
import org.junit.jupiter.api.Test

class CrossCheckTest {

    private fun offer(
        remoteId: String,
        source: String,
        modelId: String = "m",
        freeStatus: FreeStatus = FreeStatus.FREE
    ) = Offer(
        remoteId = remoteId,
        providerId = "p",
        modelId = modelId,
        name = "M",
        inputPrice = null,
        outputPrice = null,
        freeStatus = freeStatus,
        quota = null,
        quotaPeriod = null,
        temporary = false,
        conditions = null,
        contextLength = null,
        maxOutputTokens = null,
        supportsTools = true,
        supportsVision = null,
        supportsStructuredOutput = null,
        openCodeCompatible = true,
        officialUrl = null,
        source = source,
        sourceUrl = null,
        retrievedAt = 1_000L,
        verifiedAt = 1_000L,
        confidence = Confidence.OFFICIAL,
        favorite = false
    )

    private val pin = OverlapPin(
        s1RemoteId = "bothub/nemotron:free",
        nvidiaSlug = "nemotron-3-ultra",
        reason = "test pin"
    )

    @Test
    fun `agreement on free status confirms both sides`() {
        val offers = listOf(
            offer("bothub/nemotron:free", "opencode-data", "nemotron:free"),
            offer("nvidia/nemotron-3-ultra", "nvidia-build", "nemotron-3-ultra")
        )
        val result = crossCheck(offers, listOf(pin))
        assertThat(result.confirmed).isEqualTo(
            setOf("bothub/nemotron:free", "nvidia/nemotron-3-ultra")
        )
        assertThat(result.conflicts).hasSize(0)
    }

    @Test
    fun `free status disagreement conflicts both sides`() {
        val offers = listOf(
            offer("bothub/nemotron:free", "opencode-data", "nemotron:free", FreeStatus.FREE),
            offer("nvidia/nemotron-3-ultra", "nvidia-build", "nemotron-3-ultra", FreeStatus.LIMITED)
        )
        val result = crossCheck(offers, listOf(pin))
        assertThat(result.confirmed).hasSize(0)
        assertThat(result.conflicts).isEqualTo(
            setOf("bothub/nemotron:free", "nvidia/nemotron-3-ultra")
        )
    }

    @Test
    fun `missing pin side yields nothing`() {
        val offers = listOf(offer("bothub/nemotron:free", "opencode-data"))
        val result = crossCheck(offers, listOf(pin))
        assertThat(result.confirmed).hasSize(0)
        assertThat(result.conflicts).hasSize(0)
    }

    @Test
    fun `unpinned offers are ignored`() {
        val offers = listOf(
            offer("other/a", "opencode-data", "a", FreeStatus.FREE),
            offer("nvidia/b", "nvidia-build", "b", FreeStatus.PAID)
        )
        val result = crossCheck(offers, listOf(pin))
        assertThat(result.confirmed).hasSize(0)
        assertThat(result.conflicts).hasSize(0)
        assertThat(result.conflicts.contains("nvidia/b")).isEqualTo(false)
    }
}
