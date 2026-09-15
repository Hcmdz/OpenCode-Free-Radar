/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.domain.error

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import java.net.UnknownHostException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SafeCallTest {

    private fun client(status: HttpStatusCode) = HttpClient(
        MockEngine { respondError(status) }
    ) {
        expectSuccess = true
    }

    @Test
    fun `success wraps value`() = runTest {
        val result = safeCall { 42 }
        assertThat(result).isEqualTo(Result.Success(42))
    }

    @Test
    fun `401 maps to UNAUTHORIZED`() = runTest {
        val result = safeCall { client(HttpStatusCode.Unauthorized).get("https://x.test") }
        assertThat(result).isEqualTo(Result.Error(DataError.Network.UNAUTHORIZED))
    }

    @Test
    fun `408 maps to REQUEST_TIMEOUT`() = runTest {
        val result = safeCall { client(HttpStatusCode.RequestTimeout).get("https://x.test") }
        assertThat(result).isEqualTo(Result.Error(DataError.Network.REQUEST_TIMEOUT))
    }

    @Test
    fun `409 maps to CONFLICT`() = runTest {
        val result = safeCall { client(HttpStatusCode.Conflict).get("https://x.test") }
        assertThat(result).isEqualTo(Result.Error(DataError.Network.CONFLICT))
    }

    @Test
    fun `413 maps to PAYLOAD_TOO_LARGE`() = runTest {
        val result = safeCall { client(HttpStatusCode.PayloadTooLarge).get("https://x.test") }
        assertThat(result).isEqualTo(Result.Error(DataError.Network.PAYLOAD_TOO_LARGE))
    }

    @Test
    fun `429 maps to TOO_MANY_REQUESTS`() = runTest {
        val result = safeCall { client(HttpStatusCode.TooManyRequests).get("https://x.test") }
        assertThat(result).isEqualTo(Result.Error(DataError.Network.TOO_MANY_REQUESTS))
    }

    @Test
    fun `500 maps to SERVER_ERROR`() = runTest {
        val result = safeCall { client(HttpStatusCode.InternalServerError).get("https://x.test") }
        assertThat(result).isEqualTo(Result.Error(DataError.Network.SERVER_ERROR))
    }

    @Test
    fun `unknown host maps to NO_INTERNET`() = runTest {
        val result: Result<Unit, DataError> = safeCall { throw UnknownHostException() }
        assertThat(result).isEqualTo(Result.Error(DataError.Network.NO_INTERNET))
    }

    @Test
    fun `serialization failure maps to SERIALIZATION`() = runTest {
        val result: Result<Unit, DataError> = safeCall { throw SerializationException("bad json") }
        assertThat(result).isEqualTo(Result.Error(DataError.Network.SERIALIZATION))
    }

    @Test
    fun `cancellation is never swallowed`() = runTest {
        val job = launch {
            safeCall {
                delay(10_000)
            }
        }
        delay(10)
        job.cancelAndJoin()
        assertThat(job.isCancelled).isTrue()
    }

    @Test
    fun `cancellation rethrows instead of mapping`() = runTest {
        val deferred = async {
            safeCall<Unit> { throw kotlinx.coroutines.CancellationException() }
        }
        assertThrows<kotlinx.coroutines.CancellationException> { deferred.await() }
    }

    @Test
    fun `result chaining composes`() {
        val mapped = Result.Success(2).map { it * 3 }
        assertThat(mapped).isEqualTo(Result.Success(6))
        var seen = 0
        Result.Success(1).onSuccess { seen = it }.onFailure { seen = -1 }
        assertThat(seen).isEqualTo(1)
        val empty: EmptyResult<DataError> = Result.Success("x").asEmptyResult()
        assertThat(empty).isInstanceOf(Result.Success::class)
    }
}
