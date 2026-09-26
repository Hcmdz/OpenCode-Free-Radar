/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.opencode.freeradar.domain.model.Confidence
import com.opencode.freeradar.domain.model.FreeStatus
import com.opencode.freeradar.domain.model.Offer
import org.junit.jupiter.api.Test

class SortTest {

    private fun offer(
        remoteId: String,
        name: String = "M",
        verifiedAt: Long = 1_000L,
        contextLength: Int? = null,
        favorite: Boolean = false
    ) = Offer(
        remoteId = remoteId,
        providerId = "p",
        modelId = "m",
        name = name,
        inputPrice = 0.0,
        outputPrice = 0.0,
        freeStatus = FreeStatus.FREE,
        quota = null,
        quotaPeriod = null,
        temporary = false,
        conditions = null,
        contextLength = contextLength,
        maxOutputTokens = null,
        supportsTools = true,
        supportsVision = null,
        supportsStructuredOutput = null,
        openCodeCompatible = true,
        officialUrl = null,
        source = "opencode-data",
        sourceUrl = null,
        retrievedAt = 1_000L,
        verifiedAt = verifiedAt,
        confidence = Confidence.OFFICIAL,
        favorite = favorite
    )

    @Test
    fun `recent orders verifiedAt descending`() {
        val rows = listOf(
            offer("old", verifiedAt = 100L),
            offer("new", verifiedAt = 300L),
            offer("mid", verifiedAt = 200L)
        )
        assertThat(rows.sortedWith(sortComparator(OfferSort.RECENT)).map { it.remoteId })
            .isEqualTo(listOf("new", "mid", "old"))
    }

    @Test
    fun `recent is stable on ties`() {
        val rows = listOf(offer("a"), offer("b"), offer("c"))
        assertThat(rows.sortedWith(sortComparator(OfferSort.RECENT)).map { it.remoteId })
            .isEqualTo(listOf("a", "b", "c"))
    }

    @Test
    fun `name orders case-insensitively`() {
        val rows = listOf(
            offer("1", name = "Zulu"),
            offer("2", name = "alpha"),
            offer("3", name = "Mike")
        )
        assertThat(rows.sortedWith(sortComparator(OfferSort.NAME)).map { it.remoteId })
            .isEqualTo(listOf("2", "3", "1"))
    }

    @Test
    fun `context orders descending with nulls last`() {
        val rows = listOf(
            offer("null", contextLength = null),
            offer("small", contextLength = 8_000),
            offer("big", contextLength = 1_000_000)
        )
        assertThat(rows.sortedWith(sortComparator(OfferSort.CONTEXT)).map { it.remoteId })
            .isEqualTo(listOf("big", "small", "null"))
    }

    @Test
    fun `favorite outranks a fresher row on recent`() {
        val rows = listOf(
            offer("fresh", verifiedAt = 900L),
            offer("pinned", verifiedAt = 100L, favorite = true)
        )
        assertThat(rows.sortedWith(sortComparator(OfferSort.RECENT)).map { it.remoteId })
            .isEqualTo(listOf("pinned", "fresh"))
    }

    @Test
    fun `favorite outranks the name order too`() {
        val rows = listOf(
            offer("aaa", name = "Aaa"),
            offer("pinned", name = "Zzz", favorite = true)
        )
        assertThat(rows.sortedWith(sortComparator(OfferSort.NAME)).map { it.remoteId })
            .isEqualTo(listOf("pinned", "aaa"))
    }

    @Test
    fun `order is total regardless of input order`() {
        val rows = listOf(
            offer("c", verifiedAt = 100L),
            offer("a", verifiedAt = 100L),
            offer("b", verifiedAt = 100L)
        )
        val expected = listOf("a", "b", "c")
        assertThat(rows.sortedWith(sortComparator(OfferSort.RECENT)).map { it.remoteId })
            .isEqualTo(expected)
        assertThat(rows.reversed().sortedWith(sortComparator(OfferSort.RECENT)).map { it.remoteId })
            .isEqualTo(expected)
    }

    @Test
    fun `sort still applies inside the favorite group`() {
        val rows = listOf(
            offer("old-pinned", verifiedAt = 100L, favorite = true),
            offer("new-pinned", verifiedAt = 900L, favorite = true),
            offer("plain", verifiedAt = 950L)
        )
        assertThat(rows.sortedWith(sortComparator(OfferSort.RECENT)).map { it.remoteId })
            .isEqualTo(listOf("new-pinned", "old-pinned", "plain"))
    }
}
