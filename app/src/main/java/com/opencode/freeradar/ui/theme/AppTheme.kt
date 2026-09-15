/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

private val FallbackSeed = Color(0xFF3B82F6)

// ponytail: wallpaper-change observer deferred to F6 polish — the scheme
// already rebuilds on config/locale change; add a WallpaperManager observer
// only if stale-after-wallpaper-change is reported.
@Composable
fun AppTheme(state: ThemeState, content: @Composable () -> Unit) {
    val dark = when (state.mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val scheme = when {
        state.colorSource == ColorSource.MATERIAL_YOU &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        dark -> rememberDynamicColorScheme(
            FallbackSeed,
            isDark = true,
            isAmoled = state.useBlackTheme,
            style = PaletteStyle.Expressive
        )

        else -> rememberDynamicColorScheme(
            FallbackSeed,
            isDark = false,
            isAmoled = false,
            style = PaletteStyle.Expressive
        )
    }
    val finalScheme = if (state.useBlackTheme && dark) scheme.toOled() else scheme
    MaterialExpressiveTheme(
        colorScheme = finalScheme,
        motionScheme = MotionScheme.expressive(),
        content = content
    )
}

private fun Color.towardBlack(factor: Float): Color = copy(
    red = red * (1 - factor),
    green = green * (1 - factor),
    blue = blue * (1 - factor)
)

private fun ColorScheme.toOled(): ColorScheme = copy(
    background = background.towardBlack(0.92f),
    surface = surface.towardBlack(0.90f),
    surfaceVariant = surfaceVariant.towardBlack(0.85f),
    surfaceContainerLowest = surfaceContainerLowest.towardBlack(0.95f),
    surfaceContainerLow = surfaceContainerLow.towardBlack(0.90f),
    surfaceContainer = surfaceContainer.towardBlack(0.85f),
    surfaceContainerHigh = surfaceContainerHigh.towardBlack(0.78f),
    surfaceContainerHighest = surfaceContainerHighest.towardBlack(0.70f)
)
