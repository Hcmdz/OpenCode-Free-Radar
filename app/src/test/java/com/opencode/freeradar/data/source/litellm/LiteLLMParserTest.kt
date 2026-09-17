/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.source.litellm

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.junit.jupiter.api.Test

class LiteLLMParserTest {

    private fun entry(
        key: String,
        mode: String = "chat",
        input: Double? = 0.0,
        output: Double? = 0.0,
        provider: String? = "gemini"
    ): String {
        fun num(v: Double?) = v?.toString() ?: "null"
        fun str(v: String?) = v?.let { "\"$it\"" } ?: "null"
        return "\"$key\": {\"mode\": \"$mode\", " +
            "\"input_cost_per_token\": ${num(input)}, " +
            "\"output_cost_per_token\": ${num(output)}, " +
            "\"litellm_provider\": ${str(provider)}, " +
            "\"max_input_tokens\": 128000, " +
            "\"supports_function_calling\": true, " +
            "\"supports_vision\": false}"
    }

    @Test
    fun `zero-cost chat entry maps fields`() {
        val offers = parseLiteLLMModels("{${entry("gemini-flash-lite")}}")
        assertThat(offers).hasSize(1)
        val offer = offers.first()
        assertThat(offer.providerId).isEqualTo("gemini")
        assertThat(offer.modelId).isEqualTo("gemini-flash-lite")
        assertThat(offer.name).isEqualTo("gemini-flash-lite")
        assertThat(offer.inputPrice).isEqualTo(0.0)
        assertThat(offer.outputPrice).isEqualTo(0.0)
        assertThat(offer.contextLength).isEqualTo(128000)
        assertThat(offer.supportsTools).isEqualTo(true)
    }

    @Test
    fun `paid non-chat and providerless entries dropped`() {
        val body = "{${entry("paid", input = 1e-6)}," +
            "${entry("painter", mode = "image_generation")}," +
            "${entry("ghost", provider = null)}}"
        assertThat(parseLiteLLMModels(body)).hasSize(0)
    }

    @Test
    fun `openrouter proxy rows dropped`() {
        val body = "{${entry("openrouter/deepseek/deepseek-chat", provider = "openrouter")}," +
            "${entry("deepseek-chat", provider = "deepseek")}}"
        val offers = parseLiteLLMModels(body)
        assertThat(offers).hasSize(1)
        assertThat(offers.first().providerId).isEqualTo("deepseek")
    }

    @Test
    fun `garbage body and bad entries fail closed`() {
        assertThat(parseLiteLLMModels("not json")).hasSize(0)
        val body = "{\"ok\": {\"mode\": \"chat\", \"input_cost_per_token\": 0, " +
            "\"output_cost_per_token\": 0, \"litellm_provider\": \"mistral\"}," +
            "\"bad\": [1, 2]}"
        val offers = parseLiteLLMModels(body)
        assertThat(offers).hasSize(1)
        assertThat(offers.first().modelId).isEqualTo("ok")
        assertThat(offers.first().conditions).isNull()
        assertThat(offers.first().officialUrl).isNull()
    }
}
