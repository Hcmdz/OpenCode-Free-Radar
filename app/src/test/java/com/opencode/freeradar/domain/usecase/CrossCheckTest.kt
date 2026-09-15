/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.domain.usecase

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import com.opencode.freeradar.domain.model.Confidence
import com.opencode.freeradar.domain.model.FreeStatus
import com.opencode.freeradar.domain.model.Offer
import org.junit.jupiter.api.Test

class CrossCheckTest {

    private fun offer(
        remoteId: String,
        freeStatus: FreeStatus = FreeStatus.FREE
    ) = Offer(
        remoteId = remoteId,
        providerId = "p",
        modelId = "m",
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
        source = "s",
        sourceUrl = null,
        retrievedAt = 1_000L,
        verifiedAt = 1_000L,
        confidence = Confidence.OFFICIAL,
        favorite = false
    )

    private val pin = OverlapPin("s1/m:free", "s3/m:free", "test pin")

    @Test
    fun `agreement confirms both sides`() {
        val result = crossCheck(
            listOf(offer("s1/m:free"), offer("s3/m:free")),
            listOf(pin)
        )
        assertThat(result.confirmed).isEqualTo(setOf("s1/m:free", "s3/m:free"))
        assertThat(result.conflicts).hasSize(0)
    }

    @Test
    fun `disagreement conflicts both sides`() {
        val result = crossCheck(
            listOf(
                offer("s1/m:free", FreeStatus.FREE),
                offer("s3/m:free", FreeStatus.PAID)
            ),
            listOf(pin)
        )
        assertThat(result.confirmed).hasSize(0)
        assertThat(result.conflicts).isEqualTo(setOf("s1/m:free", "s3/m:free"))
    }

    @Test
    fun `missing pin side yields nothing`() {
        val result = crossCheck(listOf(offer("s1/m:free")), listOf(pin))
        assertThat(result.confirmed).hasSize(0)
        assertThat(result.conflicts).hasSize(0)
    }

    @Test
    fun `unpinned offers are ignored`() {
        val result = crossCheck(
            listOf(offer("other/a", FreeStatus.FREE), offer("other/b", FreeStatus.PAID)),
            listOf(pin)
        )
        assertThat(result.confirmed).hasSize(0)
        assertThat(result.conflicts).hasSize(0)
    }
}
