/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.opencode.freeradar.domain.model.Confidence
import com.opencode.freeradar.domain.model.FreeStatus
import com.opencode.freeradar.domain.model.Offer
import org.junit.jupiter.api.Test

class FilterCountsTest {

    private fun offer(
        remoteId: String,
        source: String,
        status: FreeStatus = FreeStatus.FREE,
        compatible: Boolean = true
    ) = Offer(
        remoteId = remoteId,
        providerId = "p",
        modelId = "m",
        name = "M",
        inputPrice = 0.0,
        outputPrice = 0.0,
        freeStatus = status,
        quota = null,
        quotaPeriod = null,
        temporary = false,
        conditions = null,
        contextLength = null,
        maxOutputTokens = null,
        supportsTools = true,
        supportsVision = null,
        supportsStructuredOutput = null,
        openCodeCompatible = compatible,
        officialUrl = null,
        source = source,
        sourceUrl = null,
        retrievedAt = 1_000L,
        verifiedAt = 1_000L,
        confidence = Confidence.OFFICIAL,
        favorite = false
    )

    private val offers = listOf(
        offer("s1/free", "opencode-data", FreeStatus.FREE),
        offer("s1/paid", "opencode-data", FreeStatus.PAID),
        offer("s1/nc", "opencode-data", FreeStatus.FREE, compatible = false),
        offer("nv/ltd", "nvidia-build", FreeStatus.LIMITED)
    )

    @Test
    fun `status counts honor the active source filter`() {
        val counts = facetCounts(offers, OfferFilter.FREE, SourceFilter.OPENCODE)
        assertThat(counts.status[OfferFilter.FREE]).isEqualTo(2)
        assertThat(counts.status[OfferFilter.ALL]).isEqualTo(3)
        assertThat(counts.status[OfferFilter.COMPATIBLE]).isEqualTo(2)
    }

    @Test
    fun `source counts honor the active status filter`() {
        val counts = facetCounts(offers, OfferFilter.FREE, SourceFilter.ALL_SOURCES)
        assertThat(counts.source[SourceFilter.ALL_SOURCES]).isEqualTo(2)
        assertThat(counts.source[SourceFilter.OPENCODE]).isEqualTo(2)
        assertThat(counts.source[SourceFilter.NVIDIA]).isEqualTo(0)
    }
}
