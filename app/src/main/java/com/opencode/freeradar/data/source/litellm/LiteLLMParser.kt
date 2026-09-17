/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.source.litellm

import com.opencode.freeradar.data.source.remote.SourceOffer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

private val liteLLMJson = Json { ignoreUnknownKeys = true; explicitNulls = false }

/**
 * Zero-cost chat entries only. `openrouter`-provider rows are a proxy
 * mirror of the catalog OpenRouterSource already fetches directly (fresher
 * and richer: descriptions, endpoints), so they are dropped here instead
 * of doubling every row. Unparseable bodies fail closed to empty (caller
 * treats empty per the empty-catalog guard); one bad entry never kills
 * the other 4k. Missing fields stay null → UNKNOWN downstream.
 */
fun parseLiteLLMModels(body: String): List<SourceOffer> {
    val root = try {
        liteLLMJson.parseToJsonElement(body).jsonObject
    } catch (e: Exception) {
        return emptyList()
    }
    return root.entries
        .filter { it.key.isNotBlank() }
        .mapNotNull { (key, element) ->
            runCatching { key to liteLLMJson.decodeFromJsonElement<LiteLLMModelDto>(element) }
                .getOrNull()
        }
        .filter { (_, dto) -> dto.mode == "chat" || dto.mode == "completion" }
        .filter { (_, dto) ->
            dto.input_cost_per_token == 0.0 && dto.output_cost_per_token == 0.0
        }
        .filter { (_, dto) ->
            val provider = dto.litellm_provider?.ifBlank { null } ?: return@filter false
            provider != "openrouter"
        }
        .distinctBy { it.first }
        .map { (key, dto) -> dto.toSourceOffer(key) }
}

private fun LiteLLMModelDto.toSourceOffer(key: String): SourceOffer {
    val providerId = litellm_provider ?: ""
    return SourceOffer(
        providerId = providerId,
        modelId = key,
        // The price map carries no display name: the key is the name.
        name = key,
        inputPrice = input_cost_per_token,
        outputPrice = output_cost_per_token,
        contextLength = max_input_tokens,
        maxOutputTokens = max_output_tokens,
        supportsTools = supports_function_calling,
        supportsVision = supports_vision,
        supportsStructuredOutput = supports_response_schema,
        // No quota/conditions/homepage in the price map; compat and
        // free status are derived later by the shared rule in toOffer().
        quota = null,
        conditions = null,
        officialUrl = null,
        sourceUrl = "https://github.com/BerriAI/litellm/blob/main/model_prices_and_context_window.json"
    )
}
