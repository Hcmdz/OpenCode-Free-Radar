/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.domain.error

sealed interface SourceError {
    data object Unreachable : SourceError
    data object Timeout : SourceError
    data object Server : SourceError
    data object RateLimited : SourceError
    data object ParseFailed : SourceError
    data class Unknown(val code: String) : SourceError
}

sealed interface RefreshResult {
    data object Ok : RefreshResult
    data class Partial(val failedSources: List<String>) : RefreshResult
    data class Failed(val error: SourceError) : RefreshResult
}

fun DataError.toSourceError(): SourceError = when (this) {
    DataError.Network.NO_INTERNET -> SourceError.Unreachable
    DataError.Network.REQUEST_TIMEOUT -> SourceError.Timeout
    DataError.Network.SERVER_ERROR -> SourceError.Server
    DataError.Network.TOO_MANY_REQUESTS -> SourceError.RateLimited
    DataError.Network.SERIALIZATION -> SourceError.ParseFailed
    is DataError.Local -> SourceError.Unknown("local")
    is DataError.Network -> SourceError.Unknown(name)
}
