/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.source.nvidia

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

class NvidiaCardParserTest {

    private fun fixture(name: String): String =
        javaClass.classLoader.getResourceAsStream("fixtures/$name")!!.bufferedReader().readText()

    @Test
    fun `ultra card parses frontmatter and tool strengths`() {
        val card = parseNvidiaCard("nemotron-3-ultra-550b-a55b", fixture("nvidia-ultra-card.md"))!!
        assertThat(card.title).isEqualTo("nemotron-3-ultra-550b-a55b")
        assertThat(card.publisher).isEqualTo("nvidia")
        assertThat(card.canonical).isEqualTo("https://build.nvidia.com/nvidia/nemotron-3-ultra-550b-a55b")
        assertThat(card.contextLength).isEqualTo(1_048_576) // card states 1M and precise 1048576; max wins
        assertThat(card.supportsTools).isEqualTo(true)
        assertThat(card.supportsVision).isNull()
        assertThat(card.chatCapable).isTrue()
        assertThat(card.license).isNotNull()
        assertThat(card.displayName).isEqualTo("NVIDIA-Nemotron-3-Ultra-550B-A55B-NVFP4")
    }

    @Test
    fun `vision card honors explicit not-supported capabilities`() {
        val card = parseNvidiaCard("llama-3.2-11b-vision-instruct", fixture("nvidia-vlm-card.md"))!!
        assertThat(card.contextLength).isEqualTo(131_072)
        assertThat(card.supportsTools).isEqualTo(false)
        assertThat(card.supportsVision).isEqualTo(true)
        assertThat(card.supportsStructuredOutput).isEqualTo(false)
        assertThat(card.chatCapable).isTrue()
        assertThat(card.displayName).isEqualTo("llama-3.2-11b-vision-instruct")
    }

    @Test
    fun `missing frontmatter skips the card`() {
        assertThat(parseNvidiaCard("x", "## No frontmatter here")).isNull()
        assertThat(parseNvidiaCard("x", "")).isNull()
    }

    @Test
    fun `card without token counts yields null context`() {
        val bare = parseNvidiaCard(
            "m",
            "---\ntitle: \"m\"\ncanonical: \"https://build.nvidia.com/o/m\"\n---\n" +
                "Reasoning model for chat workloads.\n"
        )!!
        assertThat(bare.contextLength).isNull()
        assertThat(bare.chatCapable).isTrue()
    }

    @Test
    fun `bare chat word alone is not chat capable`() {
        // Prototype snippets contain /v1/chat/completions on every card, so a
        // lone "chat" must not qualify (proven on the FLUX.1-dev card).
        val bare = parseNvidiaCard(
            "m",
            "---\ntitle: \"m\"\ncanonical: \"https://build.nvidia.com/o/m\"\n---\nChat model.\n"
        )!!
        assertThat(bare.chatCapable).isFalse()
    }

    // Synthetic signal cases (no real bio card: host bot-challenged repeated
    // fetches during fixture collection; the predicate itself is fully tested).
    @Test
    fun `biological model description is not chat capable`() {
        assertThat(
            isChatCapable(
                "evo2-40b",
                "Evo 2 is a biological foundation model over genomic sequences.",
                "Predicts protein structures from genomic sequences and protein backbones."
            )
        ).isFalse()
    }

    @Test
    fun `transcription model description is not chat capable`() {
        assertThat(
            isChatCapable(
                "parakeet-ctc-0.6b-asr",
                "Accurate English transcriptions with punctuation.",
                "Automatic speech recognition model, transcribes speech."
            )
        ).isFalse()
    }

    @Test
    fun `image generator with multimodal prose is excluded by phrase`() {
        assertThat(
            isChatCapable(
                "flux_1-kontext-dev",
                "FLUX.1 Kontext is a multimodal model for in-context image generation and editing.",
                "Multimodal image generation and editing with subject consistency."
            )
        ).isFalse()
    }

    @Test
    fun `generic template heading falls back to slug`() {
        val card = parseNvidiaCard(
            "flux_1-kontext-dev",
            "---\ntitle: \"flux_1-kontext-dev\"\ncanonical: \"https://build.nvidia.com/b/flux_1-kontext-dev\"\n---\n" +
                "# Overview\nSome reasoning model text for agents.\n"
        )!!
        assertThat(card.displayName).isEqualTo("flux_1-kontext-dev")
    }
}
