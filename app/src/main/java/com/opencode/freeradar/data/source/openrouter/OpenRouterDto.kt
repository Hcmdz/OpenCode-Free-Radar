/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.source.openrouter

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

const val OPENROUTER_SOURCE_ID = "openrouter"

@Serializable
data class OpenRouterIndex(
    val data: List<OpenRouterModelDto> = emptyList()
)

@Serializable
data class OpenRouterModelDto(
    val id: String = "",
    val name: String? = null,
    val created: Long? = null,
    val description: String? = null,
    val context_length: Int? = null,
    val architecture: OpenRouterArchitectureDto? = null,
    val pricing: OpenRouterPricingDto? = null,
    val top_provider: OpenRouterTopProviderDto? = null,
    val per_request_limits: JsonElement? = null,
    val supported_parameters: List<String>? = null,
    val hugging_face_id: String? = null,
    val expiration_date: String? = null
)

@Serializable
data class OpenRouterArchitectureDto(
    val modality: String? = null,
    val input_modalities: List<String> = emptyList(),
    val output_modalities: List<String> = emptyList(),
    val tokenizer: String? = null,
    val instruct_type: String? = null
)

/** Prices arrive as decimal strings ("0", "0.00000057948"); never assume numbers. */
@Serializable
data class OpenRouterPricingDto(
    val prompt: String? = null,
    val completion: String? = null
)

@Serializable
data class OpenRouterTopProviderDto(
    val context_length: Int? = null,
    val max_completion_tokens: Int? = null,
    val is_moderated: Boolean? = null
)
