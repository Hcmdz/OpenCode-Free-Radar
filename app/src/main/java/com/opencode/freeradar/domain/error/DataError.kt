/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.domain.error

sealed interface DataError {
    enum class Network : DataError {
        NO_INTERNET,
        UNAUTHORIZED,
        REQUEST_TIMEOUT,
        CONFLICT,
        PAYLOAD_TOO_LARGE,
        TOO_MANY_REQUESTS,
        SERVER_ERROR,
        SERIALIZATION
    }

    enum class Local : DataError {
        DISK_FULL,
        IO_ERROR,
        NOT_FOUND
    }
}
