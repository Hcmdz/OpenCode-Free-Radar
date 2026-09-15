/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.source.openrouter

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.opencode.freeradar.data.source.remote.toOffer
import com.opencode.freeradar.domain.model.FreeStatus
import org.junit.jupiter.api.Test

/**
 * Pins the shared price rule for the OpenRouter source id: `:free` + 0/0
 * is the only FREE path; anything unparseable stays UNKNOWN, never FREE.
 */
class OpenRouterMapperTest {

    private fun fixture(name: String): String =
        javaClass.classLoader.getResourceAsStream("fixtures/$name")!!.bufferedReader().readText()

    @Test
    fun `zero-zero maps to FREE under openrouter source`() {
        val offer = parseOpenRouterModels(fixture("s3-free-model.json")).first()
            .toOffer(now = 1_000L, source = OPENROUTER_SOURCE_ID)
        assertThat(offer.freeStatus).isEqualTo(FreeStatus.FREE)
        assertThat(offer.source).isEqualTo(OPENROUTER_SOURCE_ID)
        assertThat(offer.remoteId).isEqualTo("inclusionai/ling-3.0-flash-vl:free")
    }

    @Test
    fun `nonzero maps to PAID`() {
        val offer = parseOpenRouterModels(fixture("s3-paid-model.json")).first()
            .toOffer(now = 1_000L, source = OPENROUTER_SOURCE_ID)
        assertThat(offer.freeStatus).isEqualTo(FreeStatus.PAID)
    }

    @Test
    fun `missing pricing maps to UNKNOWN never FREE`() {
        val offer = parseOpenRouterModels("""{"data": [{"id": "p/m:free", "name": "M"}]}""").first()
            .toOffer(now = 1_000L, source = OPENROUTER_SOURCE_ID)
        assertThat(offer.freeStatus).isEqualTo(FreeStatus.UNKNOWN)
    }
}
