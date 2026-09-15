/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.domain.error

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.JsonConvertException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.serialization.SerializationException
import kotlin.coroutines.cancellation.CancellationException

suspend fun <T> safeCall(block: suspend () -> T): Result<T, DataError> {
    return try {
        Result.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: ClientRequestException) {
        Result.Error(
            when (e.response.status) {
                HttpStatusCode.Unauthorized -> DataError.Network.UNAUTHORIZED
                HttpStatusCode.RequestTimeout -> DataError.Network.REQUEST_TIMEOUT
                HttpStatusCode.Conflict -> DataError.Network.CONFLICT
                HttpStatusCode.PayloadTooLarge -> DataError.Network.PAYLOAD_TOO_LARGE
                HttpStatusCode.TooManyRequests -> DataError.Network.TOO_MANY_REQUESTS
                else -> DataError.Network.SERVER_ERROR
            }
        )
    } catch (e: ServerResponseException) {
        Result.Error(DataError.Network.SERVER_ERROR)
    } catch (e: HttpRequestTimeoutException) {
        Result.Error(DataError.Network.REQUEST_TIMEOUT)
    } catch (e: SocketTimeoutException) {
        Result.Error(DataError.Network.REQUEST_TIMEOUT)
    } catch (e: UnknownHostException) {
        Result.Error(DataError.Network.NO_INTERNET)
    } catch (e: ConnectException) {
        Result.Error(DataError.Network.NO_INTERNET)
    } catch (e: JsonConvertException) {
        Result.Error(DataError.Network.SERIALIZATION)
    } catch (e: SerializationException) {
        Result.Error(DataError.Network.SERIALIZATION)
    } catch (e: Exception) {
        Result.Error(DataError.Network.NO_INTERNET)
    }
}
