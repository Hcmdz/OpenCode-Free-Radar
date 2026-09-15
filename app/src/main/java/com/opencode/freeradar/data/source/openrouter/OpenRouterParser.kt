/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.source.openrouter

import com.opencode.freeradar.data.source.remote.SourceOffer
import kotlinx.serialization.json.Json

private val openRouterJson = Json { ignoreUnknownKeys = true; explicitNulls = false }

private fun String?.toPriceOrNull(): Double? = this?.toDoubleOrNull()

/**
 * Validated entries only: blank ids skipped, dupes collapsed by id.
 * Unparseable bodies fail closed to empty (caller treats empty per the
 * empty-catalog guard). Missing fields stay null → UNKNOWN downstream.
 */
fun parseOpenRouterModels(body: String): List<SourceOffer> {
    val index = try {
        openRouterJson.decodeFromString<OpenRouterIndex>(body)
    } catch (e: Exception) {
        return emptyList()
    }
    return index.data
        .filter { it.id.isNotBlank() }
        .distinctBy { it.id }
        .map { it.toSourceOffer() }
}

private fun OpenRouterModelDto.toSourceOffer(): SourceOffer {
    val slash = id.indexOf('/')
    val providerId = if (slash > 0) id.substring(0, slash) else "openrouter"
    val modelId = if (slash > 0) id.substring(slash + 1) else id
    val modalities = architecture?.input_modalities
    val expiration = expiration_date?.ifBlank { null }
    return SourceOffer(
        providerId = providerId,
        modelId = modelId,
        name = name?.ifBlank { null } ?: id,
        inputPrice = pricing?.prompt.toPriceOrNull(),
        outputPrice = pricing?.completion.toPriceOrNull(),
        contextLength = context_length,
        maxOutputTokens = top_provider?.max_completion_tokens,
        supportsTools = supported_parameters?.let { "tools" in it },
        supportsVision = if (modalities == null) null else "image" in modalities,
        supportsStructuredOutput = supported_parameters?.let {
            "structured_output" in it || "response_format" in it
        },
        // per_request_limits is uniformly null today; quota stays UNKNOWN
        // (documented 20 RPM + daily caps live in docs/sources/openrouter.md).
        quota = null,
        conditions = expiration?.let { "Free trial ends $it." },
        // OpenCode compat is derived later by the shared rule in toOffer().
        officialUrl = hugging_face_id?.ifBlank { null }?.let { "https://huggingface.co/$it" },
        sourceUrl = "https://openrouter.ai/api/v1/models"
    )
}
