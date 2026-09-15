/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.source.nvidia

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.opencode.freeradar.domain.model.Confidence
import com.opencode.freeradar.domain.model.FreeStatus
import org.junit.jupiter.api.Test

class NvidiaMapperTest {

    private fun fixture(name: String): String =
        javaClass.classLoader.getResourceAsStream("fixtures/$name")!!.bufferedReader().readText()

    @Test
    fun `ultra card maps to LIMITED with quota and provenance`() {
        val card = parseNvidiaCard("nemotron-3-ultra-550b-a55b", fixture("nvidia-ultra-card.md"))!!
        val offer = card.toSourceOffer()!!.toNvidiaOffer(now = 1_000L)
        assertThat(offer.freeStatus).isEqualTo(FreeStatus.LIMITED)
        assertThat(offer.inputPrice).isNull()
        assertThat(offer.outputPrice).isNull()
        assertThat(offer.quota).isEqualTo("~40 RPM")
        assertThat(offer.source).isEqualTo(NVIDIA_SOURCE_ID)
        assertThat(offer.remoteId).isEqualTo("nvidia-build/nemotron-3-ultra-550b-a55b")
        assertThat(offer.sourceUrl).isEqualTo("https://build.nvidia.com/nvidia/nemotron-3-ultra-550b-a55b")
        assertThat(offer.confidence).isEqualTo(Confidence.AUTOMATICALLY_DETECTED)
        assertThat(offer.openCodeCompatible).isEqualTo(true)
    }

    @Test
    fun `vision card maps capabilities and stays compatible-filtered`() {
        val card = parseNvidiaCard("llama-3.2-11b-vision-instruct", fixture("nvidia-vlm-card.md"))!!
        val offer = card.toSourceOffer()!!.toNvidiaOffer(now = 1_000L)
        assertThat(offer.freeStatus).isEqualTo(FreeStatus.LIMITED)
        assertThat(offer.supportsVision).isEqualTo(true)
        assertThat(offer.supportsTools).isEqualTo(false)
        assertThat(offer.openCodeCompatible).isEqualTo(false)
    }

    @Test
    fun `non-chat card maps to null, never an offer`() {
        val card = parseNvidiaCard(
            "evo2-40b",
            "---\ntitle: \"evo2-40b\"\ncanonical: \"https://build.nvidia.com/n/evo2-40b\"\n---\n" +
                "Biological foundation model over genomic sequences and protein structures.\n"
        )!!
        assertThat(card.toSourceOffer()).isNull()
    }
}
