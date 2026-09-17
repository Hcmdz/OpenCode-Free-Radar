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
        compatible: Boolean = true,
        favorite: Boolean = false
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
        favorite = favorite
    )

    private val offers = listOf(
        offer("s1/free", "opencode-data", FreeStatus.FREE),
        offer("s1/paid", "opencode-data", FreeStatus.PAID),
        offer("s1/nc", "opencode-data", FreeStatus.FREE, compatible = false),
        offer("o/ltd", "other-source", FreeStatus.LIMITED)
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
        // o/ltd is LIMITED: usable-free, so counted under the FREE view.
        val counts = facetCounts(offers, OfferFilter.FREE, SourceFilter.ALL_SOURCES)
        assertThat(counts.source[SourceFilter.ALL_SOURCES]).isEqualTo(3)
        assertThat(counts.source[SourceFilter.OPENCODE]).isEqualTo(2)
    }

    @Test
    fun `limited trial and temporary count as free, paid expired unknown do not`() {
        val rows = listOf(
            offer("s/ltd", "opencode-data", FreeStatus.LIMITED),
            offer("s/trl", "opencode-data", FreeStatus.TRIAL),
            offer("s/tmp", "opencode-data", FreeStatus.TEMPORARY),
            offer("s/exp", "opencode-data", FreeStatus.EXPIRED),
            offer("s/unk", "opencode-data", FreeStatus.UNKNOWN),
            offer("s/paid", "opencode-data", FreeStatus.PAID)
        )
        val counts = facetCounts(rows, OfferFilter.FREE, SourceFilter.ALL_SOURCES)
        assertThat(counts.status[OfferFilter.FREE]).isEqualTo(3)
    }

    @Test
    fun `favorite counts ignore status and compatibility`() {
        val rows = listOf(
            offer("s/fav-free", "opencode-data", FreeStatus.FREE, favorite = true),
            offer("s/fav-paid", "opencode-data", FreeStatus.PAID, favorite = true),
            offer("s/plain", "opencode-data", FreeStatus.FREE, favorite = false)
        )
        val counts = facetCounts(rows, OfferFilter.FAVORITE, SourceFilter.ALL_SOURCES)
        assertThat(counts.status[OfferFilter.FAVORITE]).isEqualTo(2)
    }
}
