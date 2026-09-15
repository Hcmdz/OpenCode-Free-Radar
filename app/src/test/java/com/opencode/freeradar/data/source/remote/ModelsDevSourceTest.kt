/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.source.remote

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.opencode.freeradar.domain.error.Result
import com.opencode.freeradar.domain.model.Confidence
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/**
 * The opencode provider lists $0 rows Zen no longer serves (legacy
 * entries). The live Zen roster is the truth: $0 ghosts are dropped so
 * the absence pipeline retires them; a dead roster fails open.
 */
class ModelsDevSourceTest {

    private fun catalog() = """
        {
          "opencode": {"id": "opencode", "models": {
            "a-free": {"id": "a-free", "name": "A", "cost": {"input": 0, "output": 0}},
            "ghost-free": {"id": "ghost-free", "name": "G", "cost": {"input": 0, "output": 0}},
            "paid": {"id": "paid", "name": "P", "cost": {"input": 1.0, "output": 2.0}}
          }},
          "bothub": {"id": "bothub", "models": {
            "b-free": {"id": "b-free", "name": "B", "cost": {"input": 0, "output": 0}}
          }}
        }
    """.trimIndent()

    private fun roster() = """
        {"object": "list", "data": [
          {"id": "a-free", "object": "model", "created": 1, "owned_by": "opencode"}
        ]}
    """.trimIndent()

    private fun client(rosterStatus: HttpStatusCode = HttpStatusCode.OK) = HttpClient(
        MockEngine { request ->
            when {
                request.url.toString() == ModelsDevSource.CATALOG_URL ->
                    respond(catalog(), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                request.url.toString() == ModelsDevSource.ZEN_MODELS_URL ->
                    // A real outage serves an error page, never the roster.
                    respond(
                        if (rosterStatus == HttpStatusCode.OK) roster() else "boom",
                        rosterStatus,
                        headersOf(HttpHeaders.ContentType, "application/json")
                    )
                else -> respond("nope", HttpStatusCode.NotFound)
            }
        }
    )

    @Test
    fun `zero-price ghost absent from zen roster is marked TO_VERIFY, not dropped`() = runTest {
        val result = ModelsDevSource(client()).fetch()
        assertThat(result is Result.Success).isEqualTo(true)
        val rows = (result as Result.Success).value
        assertThat(rows.map { "${it.providerId}/${it.modelId}" }.toSet()).isEqualTo(
            setOf("opencode/a-free", "opencode/ghost-free", "opencode/paid", "bothub/b-free")
        )
        assertThat(rows.first { it.modelId == "ghost-free" }.confidence)
            .isEqualTo(Confidence.TO_VERIFY)
        assertThat(rows.first { it.modelId == "a-free" }.confidence).isNull()
    }

    @Test
    fun `dead roster fails open and keeps every row`() = runTest {
        val result = ModelsDevSource(client(HttpStatusCode.InternalServerError)).fetch()
        assertThat(result is Result.Success).isEqualTo(true)
        val ids = (result as Result.Success).value.map { "${it.providerId}/${it.modelId}" }.toSet()
        assertThat(ids).isEqualTo(
            setOf("opencode/a-free", "opencode/ghost-free", "opencode/paid", "bothub/b-free")
        )
    }
}
