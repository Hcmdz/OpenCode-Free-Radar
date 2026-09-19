/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.source.remote

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.opencode.freeradar.domain.model.Confidence
import org.junit.jupiter.api.Test

class ZenMdxParserTest {

    private fun mdx() = """
        | Model | Model ID | Endpoint | AI SDK Package |
        | --- | --- | --- | --- |
        | Big Pickle | big-pickle | `https://x` | `@ai-sdk/openai-compatible` |
        | MiMo-V2.5 Free | mimo-v2.5-free | `https://x` | `@ai-sdk/openai-compatible` |
        | GPT 5.5 | gpt-5.5 | `https://x` | `@ai-sdk/openai` |
        | Claude Sonnet 4.5 | claude-sonnet-4-5 | `https://x` | `@ai-sdk/anthropic` |
        | Jev 1.13 | jev-1.13 | `https://x` | - |

        | Model | Input | Output | Cached Read | Cached Write |
        | --- | --- | --- | --- | --- |
        | Big Pickle | Free | Free | Free | - |
        | MiMo-V2.5 Free | Free | Free | Free | - |
        | GPT 5.5 | ${'$'}5.00 | ${'$'}30.00 | ${'$'}0.50 | - |
        | Claude Sonnet 4.5 (≤ 200K tokens) | ${'$'}3.00 | ${'$'}15.00 | ${'$'}0.30 | ${'$'}3.75 |
        | Jev 1.13 | ${'$'}0.042 | Free | - | - |
    """.trimIndent()

    private fun offer(providerId: String, modelId: String, confidence: Confidence? = null) =
        SourceOffer(
            providerId = providerId,
            modelId = modelId,
            name = modelId,
            inputPrice = 0.0,
            outputPrice = 0.0,
            contextLength = null,
            maxOutputTokens = null,
            supportsTools = null,
            supportsVision = null,
            supportsStructuredOutput = null,
            quota = null,
            conditions = null,
            officialUrl = null,
            sourceUrl = null,
            confidence = confidence
        )

    @Test
    fun `pricing free rows join endpoints ids, paid and hybrid excluded`() {
        assertThat(parseZenFreeIds(mdx())).isEqualTo(setOf("big-pickle", "mimo-v2.5-free"))
    }

    @Test
    fun `roster served plus suffix or mdx confirms, anything else silent`() {
        val offers = listOf(
            offer("opencode", "mimo-v2.5-free"),
            offer("opencode", "deepseek-v4-flash-free"),
            offer("opencode", "big-pickle"),
            offer("opencode", "ghost-free"),
            offer("opencode", "mdx-only"),
            offer("bothub", "b-free")
        )
        val merged = mergeZenFreeSignals(
            offers,
            roster = setOf("mimo-v2.5-free", "deepseek-v4-flash-free", "big-pickle"),
            mdxFree = setOf("mimo-v2.5-free", "big-pickle", "mdx-only")
        ).associateBy { it.modelId }
        assertThat(merged.getValue("mimo-v2.5-free").confidence).isNull()
        assertThat(merged.getValue("deepseek-v4-flash-free").confidence).isNull()
        assertThat(merged.getValue("big-pickle").confidence).isNull()
        assertThat(merged.getValue("ghost-free").confidence).isEqualTo(Confidence.TO_VERIFY)
        assertThat(merged.getValue("mdx-only").confidence).isEqualTo(Confidence.TO_VERIFY)
        assertThat(merged.getValue("b-free").confidence).isNull()
    }

    @Test
    fun `dead signals fail open`() {
        val offers = listOf(offer("opencode", "ghost-free"))
        assertThat(mergeZenFreeSignals(offers, null, null)).isEqualTo(offers)
    }

    @Test
    fun `missing roster free is synthesized silent without conditions`() {
        val merged = synthesizeZenFreeMissing(
            listOf(offer("opencode", "mimo-v2.5-free")),
            roster = setOf("mimo-v2.5-free", "deepseek-v4-flash-free"),
            mdxFree = setOf("mimo-v2.5-free")
        ).associateBy { it.modelId }
        val synth = merged.getValue("deepseek-v4-flash-free")
        assertThat(synth.confidence).isEqualTo(Confidence.TO_VERIFY)
        assertThat(synth.conditions).isNull()
        assertThat(synth.quota).isNull()
        assertThat(synth.sourceUrl).isEqualTo(ZEN_MDX_URL)
    }
}
