/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.source.remote

import com.opencode.freeradar.domain.model.Confidence
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

@Serializable
data class CatalogModelDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val family: String? = null,
    val tool_call: Boolean? = null,
    val structured_output: Boolean? = null,
    val modalities: CatalogModalitiesDto? = null,
    val limit: CatalogLimitDto? = null,
    val cost: CatalogCostDto? = null,
    val release_date: String? = null,
    val last_updated: String? = null,
    val open_weights: Boolean? = null
)

@Serializable
data class CatalogModalitiesDto(
    val input: List<String> = emptyList(),
    val output: List<String> = emptyList()
)

@Serializable
data class CatalogLimitDto(
    val context: Int? = null,
    val output: Int? = null
)

@Serializable
data class CatalogCostDto(
    val input: Double? = null,
    val output: Double? = null,
    val cache_read: Double? = null,
    val cache_write: Double? = null
)

@Serializable
data class CatalogProviderDto(
    val id: String = "",
    val name: String? = null,
    val doc: String? = null,
    val models: Map<String, CatalogModelDto> = emptyMap()
)

private val catalogJson = Json { ignoreUnknownKeys = true; explicitNulls = false }

data class SourceOffer(
    val providerId: String,
    val modelId: String,
    val name: String,
    val inputPrice: Double?,
    val outputPrice: Double?,
    val contextLength: Int?,
    val maxOutputTokens: Int?,
    val supportsTools: Boolean?,
    val supportsVision: Boolean?,
    val supportsStructuredOutput: Boolean?,
    val quota: String?,
    val conditions: String?,
    val officialUrl: String?,
    val sourceUrl: String?,
    /**
     * Sync-knowledge override (e.g. Zen-roster ghost marking). Null means
     * "no override" — the mapper falls back to OFFICIAL. Never serialized.
     */
    val confidence: Confidence? = null
)

fun parseCatalog(json: String): List<SourceOffer> {
    val root = catalogJson.parseToJsonElement(json).jsonObject
    val singleModel = root["model"]
    if (singleModel != null) {
        val providerId = root["provider"]?.toString()?.trim('"') ?: ""
        val dto = catalogJson.decodeFromJsonElement<CatalogModelDto>(singleModel)
        return listOf(dto.toSourceOffer(providerId, providerDoc = null))
    }
    return root.flatMap { (providerId, element) ->
        val provider = runCatching {
            catalogJson.decodeFromJsonElement<CatalogProviderDto>(element)
        }.getOrNull() ?: return@flatMap emptyList()
        val id = provider.id.ifEmpty { providerId }
        provider.models.values.map { it.toSourceOffer(id, provider.doc) }
    }
}

private fun CatalogModelDto.toSourceOffer(providerId: String, providerDoc: String?): SourceOffer {
    val imageInput = modalities?.input?.any { it.equals("image", ignoreCase = true) } == true
    return SourceOffer(
        providerId = providerId,
        modelId = id,
        name = name,
        inputPrice = cost?.input,
        outputPrice = cost?.output,
        contextLength = limit?.context,
        maxOutputTokens = limit?.output,
        supportsTools = tool_call,
        supportsVision = if (modalities == null) null else imageInput,
        supportsStructuredOutput = structured_output,
        quota = null,
        conditions = null,
        officialUrl = providerDoc,
        sourceUrl = providerDoc
    )
}
