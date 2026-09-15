/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.source.remote

import com.opencode.freeradar.domain.error.Result
import com.opencode.freeradar.domain.error.SourceError
import com.opencode.freeradar.domain.error.safeCall
import com.opencode.freeradar.domain.error.toSourceError
import com.opencode.freeradar.domain.repository.OfferSource
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlin.coroutines.cancellation.CancellationException

class ModelsDevSource(private val client: HttpClient) : OfferSource {
    override val id: String = "opencode-data"

    override suspend fun fetch(): Result<List<SourceOffer>, SourceError> {
        return when (val response = safeCall { client.get(CATALOG_URL).bodyAsText() }) {
            is Result.Success -> try {
                Result.Success(parseCatalog(response.value))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.Error(SourceError.ParseFailed)
            }
            is Result.Error -> Result.Error(response.error.toSourceError())
        }
    }

    companion object {
        const val CATALOG_URL = "https://models.dev/api.json"
    }
}
