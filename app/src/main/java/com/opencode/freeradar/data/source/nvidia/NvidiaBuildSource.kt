/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.source.nvidia

import com.opencode.freeradar.data.source.remote.SourceOffer
import com.opencode.freeradar.domain.error.Result
import com.opencode.freeradar.domain.error.SourceError
import com.opencode.freeradar.domain.error.safeCall
import com.opencode.freeradar.domain.error.toSourceError
import com.opencode.freeradar.domain.repository.OfferSource
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlin.coroutines.cancellation.CancellationException

/** Cards are small (~25–95KB) but numerous; the index order is the priority. */
private const val MAX_NVIDIA_CARDS = 80

class NvidiaBuildSource(private val client: HttpClient) : OfferSource {
    override val id: String = NVIDIA_SOURCE_ID

    override suspend fun fetch(): Result<List<SourceOffer>, SourceError> {
        val index = when (val response = getText(NVIDIA_MODELS_INDEX)) {
            is Result.Success -> response.value
            is Result.Error -> return Result.Error(response.error)
        }
        val extracted = parseNvidiaCatalog(index)
        val cardUrls = (if (catalogNeedsSeedFallback(extracted)) NVIDIA_SEED_CARDS else extracted)
            .take(MAX_NVIDIA_CARDS)
        val offers = mutableListOf<SourceOffer>()
        var attempted = 0
        for (url in cardUrls) {
            // Per-card failure skips the card and keeps the last valid offer.
            // Zero yield from attempted cards means systemic failure (bot wall,
            // format change) — Error, never a silent empty success that would
            // read as a clean OK run.
            attempted++
            when (val response = getText(url)) {
                is Result.Success -> {
                    val slug = url.substringAfterLast("/").removeSuffix(".md")
                    try {
                        parseNvidiaCard(slug, response.value)?.toSourceOffer()?.let { offers += it }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        continue
                    }
                }
                is Result.Error -> continue
            }
        }
        if (offers.isEmpty() && attempted > 0) return Result.Error(SourceError.ParseFailed)
        return Result.Success(offers)
    }

    private suspend fun getText(url: String): Result<String, SourceError> {
        return when (
            val response = safeCall {
                // Browser UA + referer required: the host bot-challenges default
                // HTTP clients (proven 2026-09-15: bare clients got a $RC page).
                client.get(url) {
                    header("User-Agent", NVIDIA_USER_AGENT)
                    header("Referer", "https://build.nvidia.com/models")
                    header("Accept", "text/markdown, text/plain, */*")
                    header("Accept-Language", "en-US,en;q=0.9")
                }.bodyAsText()
            }
        ) {
            is Result.Success -> Result.Success(response.value)
            is Result.Error -> Result.Error(response.error.toSourceError())
        }
    }

    companion object {
        const val NVIDIA_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
    }
}
