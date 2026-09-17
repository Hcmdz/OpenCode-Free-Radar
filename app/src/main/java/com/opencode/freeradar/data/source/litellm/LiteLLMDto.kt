/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.source.litellm

import kotlinx.serialization.Serializable

const val LITELLM_SOURCE_ID = "litellm"

@Serializable
data class LiteLLMModelDto(
    val input_cost_per_token: Double? = null,
    val output_cost_per_token: Double? = null,
    val litellm_provider: String? = null,
    val max_input_tokens: Int? = null,
    val max_output_tokens: Int? = null,
    val mode: String? = null,
    val supports_function_calling: Boolean? = null,
    val supports_vision: Boolean? = null,
    val supports_response_schema: Boolean? = null
)
