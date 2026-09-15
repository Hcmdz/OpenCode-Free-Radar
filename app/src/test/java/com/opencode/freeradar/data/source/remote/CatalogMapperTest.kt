/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.source.remote

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.opencode.freeradar.domain.model.Confidence
import com.opencode.freeradar.domain.model.FreeStatus
import org.junit.jupiter.api.Test

class CatalogMapperTest {

    private fun offer(
        inputPrice: Double? = 0.0,
        outputPrice: Double? = 0.0,
        supportsTools: Boolean? = true
    ) = SourceOffer(
        providerId = "bothub",
        modelId = "m",
        name = "M",
        inputPrice = inputPrice,
        outputPrice = outputPrice,
        contextLength = 262_144,
        maxOutputTokens = null,
        supportsTools = supportsTools,
        supportsVision = false,
        supportsStructuredOutput = true,
        quota = null,
        conditions = null,
        officialUrl = null,
        sourceUrl = null
    )

    @Test
    fun `zero prices map to FREE`() {
        val mapped = offer().toOffer(now = 1_000L)
        assertThat(mapped.freeStatus).isEqualTo(FreeStatus.FREE)
        assertThat(mapped.confidence).isEqualTo(Confidence.OFFICIAL)
        assertThat(mapped.remoteId).isEqualTo("bothub/m")
    }

    @Test
    fun `non-zero prices map to PAID`() {
        val mapped = offer(inputPrice = 1.475, outputPrice = 4.425).toOffer(now = 1_000L)
        assertThat(mapped.freeStatus).isEqualTo(FreeStatus.PAID)
    }

    @Test
    fun `missing cost maps to UNKNOWN, never FREE`() {
        val mapped = offer(inputPrice = null, outputPrice = null).toOffer(now = 1_000L)
        assertThat(mapped.freeStatus).isEqualTo(FreeStatus.UNKNOWN)
    }

    @Test
    fun `compat rule keeps UNKNOWN tools visible`() {
        assertThat(isCompatibleWithOpenCode(offer(supportsTools = true))).isTrue()
        assertThat(isCompatibleWithOpenCode(offer(supportsTools = false))).isFalse()
        assertThat(isCompatibleWithOpenCode(offer(supportsTools = null))).isFalse()
    }
}
