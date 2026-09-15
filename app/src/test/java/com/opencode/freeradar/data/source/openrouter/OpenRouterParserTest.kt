/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.source.openrouter

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

class OpenRouterParserTest {

    private fun fixture(name: String): String =
        javaClass.classLoader.getResourceAsStream("fixtures/$name")!!.bufferedReader().readText()

    @Test
    fun `free entry splits provider and model`() {
        val offers = parseOpenRouterModels(fixture("s3-free-model.json"))
        assertThat(offers).hasSize(1)
        val offer = offers.first()
        assertThat(offer.providerId).isEqualTo("inclusionai")
        assertThat(offer.modelId).isEqualTo("ling-3.0-flash-vl:free")
        assertThat(offer.inputPrice).isEqualTo(0.0)
        assertThat(offer.outputPrice).isEqualTo(0.0)
        assertThat(offer.contextLength).isEqualTo(262_144)
        assertThat(offer.supportsTools).isEqualTo(true)
        assertThat(offer.supportsVision).isEqualTo(true)
        assertThat(offer.officialUrl).isEqualTo("https://huggingface.co/inclusionAI/Ling-3.0-flash-VL")
    }

    @Test
    fun `paid entry keeps nonzero prices`() {
        val offers = parseOpenRouterModels(fixture("s3-paid-model.json"))
        assertThat(offers).hasSize(1)
        val offer = offers.first()
        assertThat(offer.inputPrice!! > 0.0).isTrue()
        assertThat(offer.outputPrice!! > 0.0).isTrue()
    }

    @Test
    fun `expiration date lands in conditions`() {
        val offers = parseOpenRouterModels(fixture("s3-expiring-model.json"))
        assertThat(offers).hasSize(1)
        assertThat(offers.first().conditions?.contains("2026-09-30")).isEqualTo(true)
    }

    @Test
    fun `blank ids skipped and dupes collapsed`() {
        val json = """{"data": [
            {"id": "", "name": "X"},
            {"id": "p/m:free", "name": "M", "pricing": {"prompt": "0", "completion": "0"}},
            {"id": "p/m:free", "name": "M", "pricing": {"prompt": "0", "completion": "0"}}
        ]}"""
        val offers = parseOpenRouterModels(json)
        assertThat(offers).hasSize(1)
    }

    @Test
    fun `missing pricing maps to null never zero`() {
        val offers = parseOpenRouterModels("""{"data": [{"id": "p/m:free", "name": "M"}]}""")
        assertThat(offers).hasSize(1)
        assertThat(offers.first().inputPrice).isNull()
        assertThat(offers.first().outputPrice).isNull()
        assertThat(offers.first().supportsTools).isNull()
        assertThat(offers.first().contextLength).isNull()
    }

    @Test
    fun `unparseable body fails closed to empty`() {
        assertThat(parseOpenRouterModels("not json").isEmpty()).isTrue()
        assertThat(parseOpenRouterModels("{}").isEmpty()).isTrue()
    }
}
