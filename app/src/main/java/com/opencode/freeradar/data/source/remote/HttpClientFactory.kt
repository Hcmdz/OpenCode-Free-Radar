/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.source.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.cache.storage.FileStorage
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import java.io.File
import kotlinx.serialization.json.Json

fun createHttpClient(
    engine: HttpClientEngine = OkHttp.create(),
    cacheDir: File? = null
): HttpClient =
    HttpClient(engine) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                }
            )
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
        }
        // Conditional GETs (ETag/Last-Modified) live here: a 304 serves the
        // stored body without re-parse. Update checks bypass this client
        // (AppModule) so release detection is never served stale.
        if (cacheDir != null) {
            install(HttpCache) {
                publicStorage(FileStorage(File(cacheDir, "http")))
            }
        }
        followRedirects = true
    }
