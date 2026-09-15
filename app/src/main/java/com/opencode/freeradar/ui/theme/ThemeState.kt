/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.theme

data class ThemeState(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val useBlackTheme: Boolean = false,
    val colorSource: ColorSource = ColorSource.MATERIAL_YOU
)
