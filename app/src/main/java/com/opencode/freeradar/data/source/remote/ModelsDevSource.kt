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
            val offers = dropZenGhosts(parseCatalog(catalogBody), roster.first)
            // Composite hash: the roster is a second input fetched every run.
            val hash = sha256Hex(catalogBody + "\n" + (roster.second ?: ""))
            Result.Success(FetchResult(offers, hash))
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
     * open — a Zen outage changes nothing.
     */
    private suspend fun dropZenGhosts(
        offers: List<SourceOffer>,
        roster: Set<String>?
    ): List<SourceOffer> {
        if (roster == null) return offers
        return offers.map { offer ->
            if (offer.providerId == ZEN_PROVIDER && offer.isFree() && offer.modelId !in roster) {
                offer.copy(confidence = Confidence.TO_VERIFY)
            } else {
                offer
            }
        }
    }

    private fun SourceOffer.isFree(): Boolean =
        inputPrice == 0.0 && outputPrice == 0.0

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

    companion object {
        const val CATALOG_URL = "https://models.dev/api.json"

        /** Documented public endpoint ("fetch the full list of available models"). */
        const val ZEN_MODELS_URL = "https://opencode.ai/zen/v1/models"
        const val ZEN_PROVIDER = "opencode"
    }
}
