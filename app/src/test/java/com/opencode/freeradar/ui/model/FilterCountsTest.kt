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
        favorite: Boolean = false,
        providerId: String = "p",
        confidence: Confidence = Confidence.OFFICIAL
    ) = Offer(
        remoteId = remoteId,
        providerId = providerId,
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
        confidence = confidence,
        favorite = favorite
    )

    private val offers = listOf(
        offer("op/open", "opencode-data", FreeStatus.FREE, providerId = "opencode"),
        offer("op/paid", "opencode-data", FreeStatus.PAID, providerId = "opencode"),
        offer("op/nc", "opencode-data", FreeStatus.FREE, compatible = false, providerId = "opencode"),
        offer("or/ltd", "opencode-data", FreeStatus.LIMITED, providerId = "openrouter"),
        offer("o/ltd", "other-source", FreeStatus.LIMITED)
    )

    @Test
    fun `opencode filter matches the zen provider, not the pipeline`() {
        // source "opencode-data" carries the whole models.dev catalog:
        // only providerId "opencode" rows count, whatever their source.
        val rows = listOf(
            offer("op/m", "opencode-data", FreeStatus.FREE, providerId = "opencode"),
            offer("or/m", "opencode-data", FreeStatus.FREE, providerId = "openrouter"),
            offer("op/x", "openrouter", FreeStatus.FREE, providerId = "opencode")
        )
        val counts = facetCounts(rows, OfferFilter.FREE, SourceFilter.ALL_SOURCES)
        assertThat(counts.source[SourceFilter.OPENCODE]).isEqualTo(2)
    }

    @Test
    fun `status counts honor the active source filter`() {
        val counts = facetCounts(offers, OfferFilter.FREE, SourceFilter.OPENCODE)
        assertThat(counts.status[OfferFilter.FREE]).isEqualTo(2)
        assertThat(counts.status[OfferFilter.ALL]).isEqualTo(3)
        assertThat(counts.status[OfferFilter.COMPATIBLE]).isEqualTo(2)
    }

    @Test
    fun `source counts honor the active status filter`() {
        // o/ltd and or/ltd are LIMITED: usable-free, so counted under the FREE view.
        val counts = facetCounts(offers, OfferFilter.FREE, SourceFilter.ALL_SOURCES)
        assertThat(counts.source[SourceFilter.ALL_SOURCES]).isEqualTo(4)
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

    @Test
    fun `free view counts confirmed rows only`() {
        // Unverified $0 (TO_VERIFY) and gated access (gitlab Premium,
        // *-plan) are usable-free but never real free models.
        val rows = listOf(
            offer("ok/m", "opencode-data", FreeStatus.FREE, providerId = "kilo"),
            offer("ghost/m", "opencode-data", FreeStatus.FREE, providerId = "kenari",
                confidence = Confidence.TO_VERIFY),
            offer("gl/m", "opencode-data", FreeStatus.LIMITED, providerId = "gitlab"),
            offer("plan/m", "opencode-data", FreeStatus.LIMITED, providerId = "x-token-plan")
        )
        val counts = facetCounts(rows, OfferFilter.FREE, SourceFilter.ALL_SOURCES)
        assertThat(counts.status[OfferFilter.FREE]).isEqualTo(1)
        assertThat(counts.status[OfferFilter.ALL]).isEqualTo(4)
    }

    @Test
    fun `hideLocal drops local providers from every count`() {
        val rows = listOf(
            offer("ollama/m", "litellm", FreeStatus.FREE, providerId = "ollama"),
            offer("g/m", "litellm", FreeStatus.FREE, providerId = "gemini")
        )
        val hidden = facetCounts(rows, OfferFilter.FREE, SourceFilter.ALL_SOURCES, hideLocal = true)
        assertThat(hidden.status[OfferFilter.FREE]).isEqualTo(1)
        assertThat(hidden.source[SourceFilter.LITELLM]).isEqualTo(1)
        val shown = facetCounts(rows, OfferFilter.FREE, SourceFilter.ALL_SOURCES)
        assertThat(shown.status[OfferFilter.FREE]).isEqualTo(2)
        assertThat(shown.source[SourceFilter.LITELLM]).isEqualTo(2)
    }
}
