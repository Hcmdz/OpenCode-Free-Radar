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
        supportsTools: Boolean? = true,
        providerId: String = "bothub",
        conditions: String? = null,
        confidence: Confidence? = null,
        sourceUrl: String? = null
    ) = SourceOffer(
        providerId = providerId,
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
        conditions = conditions,
        officialUrl = null,
        sourceUrl = sourceUrl,
        confidence = confidence
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
    fun `dated expiry maps to TRIAL, never FREE`() {
        // Live proof: dots-3-note-preview:free expires 2026-09-30 but
        // prices 0/0 — a time-bombed trial, not a free offer.
        val mapped = offer(conditions = "Free trial ends 2026-09-30.").toOffer(now = 1_000L)
        assertThat(mapped.freeStatus).isEqualTo(FreeStatus.TRIAL)
    }

    @Test
    fun `plan-gated provider maps to LIMITED, never FREE`() {
        // Live proof: *-token-plan/*-coding-plan $0 rows are free tiers
        // inside a purchasable plan (alibaba, xiaomi, tencent, ...).
        val mapped = offer(providerId = "xiaomi-token-plan-cn").toOffer(now = 1_000L)
        assertThat(mapped.freeStatus).isEqualTo(FreeStatus.LIMITED)
    }

    @Test
    fun `account-gated gateway maps to LIMITED, never FREE`() {
        // Live proof (official docs 2026-09-15): gitlab needs Premium/
        // Ultimate + Duo/credits.
        assertThat(offer(providerId = "gitlab").toOffer(now = 1_000L).freeStatus)
            .isEqualTo(FreeStatus.LIMITED)
    }

    @Test
    fun `opencode zero rows map to TEMPORARY, never FREE`() {
        // Zen docs 2026-09-17: every free model is "available for a
        // limited time" (https://opencode.ai/docs/zen/).
        assertThat(offer(providerId = "opencode").toOffer(now = 1_000L).freeStatus)
            .isEqualTo(FreeStatus.TEMPORARY)
    }

    @Test
    fun `unconfirmed opencode ghost maps to UNKNOWN, never free`() {
        // Live proof 2026-09-19: 23 models.dev $0 rows Zen does not serve
        // (absent from the 74-id roster, flagged deprecated by models.dev).
        val mapped = offer(
            providerId = "opencode",
            confidence = Confidence.TO_VERIFY,
            sourceUrl = "https://models.dev/api.json"
        ).toOffer(now = 1_000L)
        assertThat(mapped.freeStatus).isEqualTo(FreeStatus.UNKNOWN)
    }

    @Test
    fun `mdx-synth rows keep TEMPORARY despite TO_VERIFY`() {
        // Synth rows are confirmed by construction (served + free
        // signal); only their confidence is TO_VERIFY (silent arrival).
        val mapped = offer(
            providerId = "opencode",
            confidence = Confidence.TO_VERIFY,
            sourceUrl = ZEN_MDX_URL
        ).toOffer(now = 1_000L)
        assertThat(mapped.freeStatus).isEqualTo(FreeStatus.TEMPORARY)
    }

    @Test
    fun `plan and trial rules never touch PAID or UNKNOWN`() {
        assertThat(
            offer(inputPrice = 1.0, outputPrice = 1.0, providerId = "gitlab").toOffer(now = 1_000L).freeStatus
        ).isEqualTo(FreeStatus.PAID)
        assertThat(
            offer(inputPrice = null, outputPrice = null, conditions = "Free trial ends 2026-09-30.")
                .toOffer(now = 1_000L).freeStatus
        ).isEqualTo(FreeStatus.UNKNOWN)
    }

    @Test
    fun `compat rule keeps UNKNOWN tools visible`() {
        assertThat(isCompatibleWithOpenCode(offer(supportsTools = true))).isTrue()
        assertThat(isCompatibleWithOpenCode(offer(supportsTools = false))).isFalse()
        assertThat(isCompatibleWithOpenCode(offer(supportsTools = null))).isFalse()
    }
}
