/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.opencode.freeradar.R
import com.opencode.freeradar.domain.error.SourceError

sealed interface UiText {
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText
    data class Raw(val text: String) : UiText
}

@Composable
fun UiText.text(): String = when (this) {
    is UiText.Res -> stringResource(id, *args.toTypedArray())
    is UiText.Raw -> text
}

fun SourceError.toUiText(): UiText = UiText.Res(
    when (this) {
        SourceError.Unreachable -> R.string.error_no_internet
        SourceError.Timeout -> R.string.error_timeout
        SourceError.Server -> R.string.error_server
        SourceError.RateLimited -> R.string.error_rate_limited
        SourceError.ParseFailed -> R.string.error_serialization
        is SourceError.Unknown -> R.string.error_unknown
    }
)
