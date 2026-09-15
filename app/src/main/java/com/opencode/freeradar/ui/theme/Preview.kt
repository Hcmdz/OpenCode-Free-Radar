/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.theme

import androidx.compose.runtime.Composable
import com.opencode.freeradar.ui.theme.AppTheme
import com.opencode.freeradar.ui.theme.ThemeState

@Composable
fun AppThemePreview(content: @Composable () -> Unit) {
    AppTheme(state = ThemeState()) {
        content()
    }
}
