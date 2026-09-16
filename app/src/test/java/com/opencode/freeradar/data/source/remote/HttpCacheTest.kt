/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.source.remote

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class HttpCacheTest {

    @Test
    fun `etag revalidation serves cached body without re-download`() = runTest {
        val sentValidators = mutableListOf<String?>()
        var calls = 0
        val engine = MockEngine { request ->
            calls++
            sentValidators += request.headers[HttpHeaders.IfNoneMatch]
            if (calls == 1) {
                respond(
                    "hello",
                    HttpStatusCode.OK,
                    headersOf(
                        HttpHeaders.ETag to listOf("\"v1\""),
                        HttpHeaders.ContentType to listOf("text/plain")
                    )
                )
            } else {
                respond(
                    "",
                    HttpStatusCode.NotModified,
                    headersOf(HttpHeaders.ETag to listOf("\"v1\""))
                )
            }
        }
        val client = HttpClient(engine) { install(HttpCache) }
        val first: String = client.get("https://example.com/r").body()
        val second: String = client.get("https://example.com/r").body()
        assertThat(first).isEqualTo("hello")
        assertThat(second).isEqualTo("hello")
        assertThat(sentValidators[1]).isEqualTo("\"v1\"")
        assertThat(calls).isEqualTo(2)
    }
}
