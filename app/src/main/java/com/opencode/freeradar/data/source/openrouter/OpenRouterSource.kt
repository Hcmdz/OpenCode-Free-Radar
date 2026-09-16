/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.source.openrouter

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
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlin.coroutines.cancellation.CancellationException

class OpenRouterSource(private val client: HttpClient) : OfferSource {
    override val id: String = OPENROUTER_SOURCE_ID

    override suspend fun fetch(): Result<FetchResult, SourceError> {
        // Documented public Models API (no key); collections pages, the
        // :free Router and the playground are never sources (§59H).
        val body = when (
            val response = safeCall {
                client.get(MODELS_URL) { header("User-Agent", OPENROUTER_USER_AGENT) }.bodyAsText()
            }
        ) {
            is Result.Success -> response.value
            is Result.Error -> return Result.Error(response.error.toSourceError())
        }
        return try {
            Result.Success(FetchResult(parseOpenRouterModels(body), sha256Hex(body)))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(SourceError.ParseFailed)
        }
    }

    companion object {
        const val MODELS_URL = "https://openrouter.ai/api/v1/models"
        const val OPENROUTER_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
    }
}
