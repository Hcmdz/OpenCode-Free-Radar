/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.source.remote

import com.opencode.freeradar.domain.error.Result
import com.opencode.freeradar.domain.error.SourceError
import com.opencode.freeradar.domain.error.safeCall
import com.opencode.freeradar.domain.error.toSourceError
import com.opencode.freeradar.domain.model.Confidence
import com.opencode.freeradar.domain.repository.FetchResult
import com.opencode.freeradar.domain.repository.OfferSource
import com.opencode.freeradar.util.sha256Hex
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class ZenIndex(val data: List<ZenModel> = emptyList())

@Serializable
private data class ZenModel(val id: String)

private val zenJson = Json { ignoreUnknownKeys = true }

class ModelsDevSource(private val client: HttpClient) : OfferSource {
    override val id: String = "opencode-data"

    override suspend fun fetch(): Result<FetchResult, SourceError> {
        val catalogBody = when (
            val response = safeCall { client.get(CATALOG_URL).bodyAsText() }
        ) {
            is Result.Success -> response.value
            is Result.Error -> return Result.Error(response.error.toSourceError())
        }
        return try {
            val roster = zenRoster()
            val mdx = zenMdxFree()
            // Composite hash: roster and MDX are extra inputs fetched every run.
            val hash = sha256Hex(catalogBody + "\n" + (roster.second ?: "") + "\n" + (mdx.second ?: ""))
            val merged = mergeZenFreeSignals(
                demoteAggregators(parseCatalog(catalogBody)), roster.first, mdx.first
            )
            Result.Success(FetchResult(synthesizeZenFreeMissing(merged, roster.first, mdx.first), hash))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(SourceError.ParseFailed)
        }
    }

    /**
     * The opencode provider lists $0 rows Zen no longer serves (legacy
     * entries — 24 of 31 on 2026-09-15). The live Zen roster is the truth:
     * ghosts stay visible but marked TO_VERIFY — silent on arrival, and a
     * roster confirmation later rings BECAME_FREE. A dead roster fails
     * open — a Zen outage changes nothing. Fusion with the MDX pricing
     * signal lives in mergeZenFreeSignals (ZenMdxParser.kt).
     */

    /**
     * Aggregator-only $0 rows with no corroboration are ghosts: models.dev
     * prices unknown costs at $0 (proven 2026-09-19: 23 unserved opencode
     * rows, kenari serving flagships at $0 with stale updates, a literal
     * "nan" provider). First-party pipelines confirm their own rows
     * (OpenRouter API, LiteLLM explicit zeros, Zen roster×MDX); nvidia
     * trial endpoints are safelisted (served $0 trial program). The rest
     * without conditions is TO_VERIFY — silent, UNKNOWN downstream.
     * `opencode` keeps its own fusion below; dated TRIAL rows are untouched.
     */
    private fun demoteAggregators(offers: List<SourceOffer>): List<SourceOffer> =
        offers.map { offer ->
            if (offer.providerId != ZEN_PROVIDER && offer.providerId !in AGGREGATOR_SAFE_PROVIDERS &&
                offer.inputPrice == 0.0 && offer.outputPrice == 0.0 && offer.conditions == null &&
                offer.confidence == null
            ) {
                offer.copy(confidence = Confidence.TO_VERIFY)
            } else {
                offer
            }
        }

    private suspend fun zenRoster(): Pair<Set<String>?, String?> {
        return when (val response = safeCall { client.get(ZEN_MODELS_URL).bodyAsText() }) {
            is Result.Success -> try {
                zenJson.decodeFromString<ZenIndex>(response.value).data.map { it.id }.toSet() to
                    response.value
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null to null
            }
            is Result.Error -> null to null
        }
    }

    /**
     * MIT-licensed docs source on a third-party host (raw.githubusercontent),
     * same pattern as the LiteLLM price map — no automated request ever hits
     * opencode.ai pages. Dead MDX fails open like a dead roster.
     */
    private suspend fun zenMdxFree(): Pair<Set<String>?, String?> {
        return when (val response = safeCall { client.get(ZEN_MDX_URL).bodyAsText() }) {
            is Result.Success -> try {
                val ids = parseZenFreeIds(response.value)
                // A restructured doc parses to empty and would silently
                // de-confirm non-suffixed free models: fail open instead.
                if (response.value.isNotBlank() && ids.isEmpty()) null to null
                else ids to response.value
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null to null
            }
            is Result.Error -> null to null
        }
    }

    companion object {
        const val CATALOG_URL = "https://models.dev/api.json"

        /** Documented public endpoint ("fetch the full list of available models"). */
        const val ZEN_MODELS_URL = "https://opencode.ai/zen/v1/models"
        const val ZEN_PROVIDER = "opencode"

        /** Aggregator $0 rows trusted without corroboration (served trial programs). */
        private val AGGREGATOR_SAFE_PROVIDERS = setOf("nvidia")
    }
}
