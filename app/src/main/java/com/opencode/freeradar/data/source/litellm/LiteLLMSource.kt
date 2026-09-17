/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.source.litellm

import com.opencode.freeradar.data.source.remote.SourceOffer
import com.opencode.freeradar.domain.error.Result
import com.opencode.freeradar.domain.error.SourceError
import com.opencode.freeradar.domain.error.safeCall
import com.opencode.freeradar.domain.error.toSourceError
import com.opencode.freeradar.domain.repository.FetchResult
import com.opencode.freeradar.domain.repository.OfferSource
import com.opencode.freeradar.util.sha256Hex
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlin.coroutines.cancellation.CancellationException

class LiteLLMSource(private val client: HttpClient) : OfferSource {
    override val id: String = LITELLM_SOURCE_ID

    override suspend fun fetch(): Result<FetchResult, SourceError> {
        // Public price map in the LiteLLM repo (MIT); synced live every run,
        // never bundled. Ktor HttpCache serves 304s from the file ETag.
        val body = when (
            val response = safeCall { client.get(MODELS_URL).bodyAsText() }
        ) {
            is Result.Success -> response.value
            is Result.Error -> return Result.Error(response.error.toSourceError())
        }
        return try {
            Result.Success(FetchResult(parseLiteLLMModels(body), sha256Hex(body)))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(SourceError.ParseFailed)
        }
    }

    companion object {
        const val MODELS_URL =
            "https://raw.githubusercontent.com/BerriAI/litellm/main/model_prices_and_context_window.json"
    }
}
