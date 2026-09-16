/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class UpdateManagerTest {

    private class FakeStore(var ts: Long = 0L) : UpdateCheckStore {
        override suspend fun lastCheck(): Long = ts
        override suspend fun record(timestamp: Long) {
            ts = timestamp
        }
    }

    private fun manager(body: String, status: HttpStatusCode = HttpStatusCode.OK): UpdateManager {
        val engine = MockEngine {
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        return UpdateManager(client, FakeStore())
    }

    private fun releaseBody(tag: String, apkName: String = "OFR-release.v0.1.0.apk") = """
        {"tag_name": "$tag", "body": "## Integrity\n\nSHA-256: 4989a8dab4d4fee9470bbb072e11f14a4fb11439b1b66cf64b9c8334f2a7c43a",
         "assets": [{"name": "$apkName", "browser_download_url": "https://example.com/a.apk", "size": 4}]}
    """.trimIndent()

    @Test
    fun `newer remote patch is detected`() = runTest {
        val m = manager(releaseBody("v0.1.1"))
        assertThat(m.isVersionNewer("0.1.0", "0.1.1")).isTrue()
        assertThat(m.isVersionNewer("0.1.0", "0.1.0")).isEqualTo(false)
        assertThat(m.isVersionNewer("0.2.0", "0.1.9")).isEqualTo(false)
    }

    @Test
    fun `found release exposes apk asset and parsed sha`() = runTest {
        val m = manager(releaseBody("v0.2.0"))
        val result = m.checkForUpdate("0.1.0")
        assertThat(result is UpdateManager.UpdateResult.Found).isTrue()
        val info = (result as UpdateManager.UpdateResult.Found).info
        assertThat(info.versionTag).isEqualTo("v0.2.0")
        assertThat(info.versionName).isEqualTo("0.2.0")
        assertThat(info.fileName).isEqualTo("OFR-release.v0.1.0.apk")
        assertThat(info.sha256).isEqualTo("4989a8dab4d4fee9470bbb072e11f14a4fb11439b1b66cf64b9c8334f2a7c43a")
    }

    @Test
    fun `same version is up to date`() = runTest {
        val m = manager(releaseBody("v0.1.0"))
        val result = m.checkForUpdate("0.1.0")
        assertThat(result is UpdateManager.UpdateResult.UpToDate).isTrue()
    }

    @Test
    fun `release without apk asset is up to date`() = runTest {
        val m = manager(releaseBody("v9.9.9", "notes.txt"))
        val result = m.checkForUpdate("0.1.0")
        assertThat(result is UpdateManager.UpdateResult.UpToDate).isTrue()
    }

    @Test
    fun `server error maps to Error`() = runTest {
        val m = manager("oops", HttpStatusCode.InternalServerError)
        val result = m.checkForUpdate("0.1.0")
        assertThat(result is UpdateManager.UpdateResult.Error).isTrue()
    }

    @Test
    fun `missing sha stays null`() = runTest {
        val m = manager("""{"tag_name": "v0.3.0", "body": "no hash here",
            "assets": [{"name": "a.apk", "browser_download_url": "https://example.com/a.apk", "size": 1}]}""")
        val result = m.checkForUpdate("0.1.0")
        val info = (result as UpdateManager.UpdateResult.Found).info
        assertThat(info.sha256).isNull()
    }
}
