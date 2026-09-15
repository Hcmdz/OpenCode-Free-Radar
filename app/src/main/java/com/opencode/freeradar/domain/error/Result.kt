/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.domain.error

sealed interface Result<out T, out E> {
    data class Success<T>(val value: T) : Result<T, Nothing>
    data class Error<E>(val error: E) : Result<Nothing, E>
}

typealias EmptyResult<E> = Result<Unit, E>

inline fun <T, E, R> Result<T, E>.map(transform: (T) -> R): Result<R, E> =
    when (this) {
        is Result.Success -> Result.Success(transform(value))
        is Result.Error -> this
    }

inline fun <T, E> Result<T, E>.onSuccess(action: (T) -> Unit): Result<T, E> {
    if (this is Result.Success) action(value)
    return this
}

inline fun <T, E> Result<T, E>.onFailure(action: (E) -> Unit): Result<T, E> {
    if (this is Result.Error) action(error)
    return this
}

fun <T, E> Result<T, E>.asEmptyResult(): EmptyResult<E> = map { }
