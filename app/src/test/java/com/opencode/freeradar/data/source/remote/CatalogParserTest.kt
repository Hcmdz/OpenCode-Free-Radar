/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.source.remote

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import org.junit.jupiter.api.Test

class CatalogParserTest {

    private fun fixture(name: String): String =
        javaClass.classLoader.getResourceAsStream("fixtures/$name")!!.bufferedReader().readText()

    @Test
    fun `free model entry parses with zero prices`() {
        val offers = parseCatalog(fixture("s1-free-model.json"))
        assertThat(offers).hasSize(1)
        val offer = offers.first()
        assertThat(offer.providerId).isEqualTo("bothub")
        assertThat(offer.inputPrice).isEqualTo(0.0)
        assertThat(offer.outputPrice).isEqualTo(0.0)
        assertThat(offer.contextLength).isNotNull()
    }

    @Test
    fun `paid model entry parses with prices and capabilities`() {
        val offers = parseCatalog(fixture("s1-paid-model.json"))
        assertThat(offers).hasSize(1)
        val offer = offers.first()
        assertThat(offer.providerId).isEqualTo("openrouter")
        assertThat(offer.supportsTools).isEqualTo(true)
        assertThat(offer.contextLength).isEqualTo(1_000_000)
    }

    @Test
    fun `missing optional fields map to null instead of failing`() {
        val offers = parseCatalog("""{"p": {"id": "p", "models": {"m": {"id": "m", "name": "M"}}}}""")
        assertThat(offers).hasSize(1)
        assertThat(offers.first().inputPrice).isNull()
        assertThat(offers.first().supportsTools).isNull()
        assertThat(offers.first().contextLength).isNull()
    }
}
