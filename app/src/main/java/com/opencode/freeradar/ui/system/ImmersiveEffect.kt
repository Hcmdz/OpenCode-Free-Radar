/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.system

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Sticky fullscreen: status and navigation bars stay hidden until an edge
 * swipe reveals them transiently. Restores the bars when leaving.
 * WindowCompat covers API 29 (sticky-immersive flags) up to 36
 * (transient-bars behavior) with one call.
 */
@Composable
fun ImmersiveEffect() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        if (window == null) return@DisposableEffect onDispose {}
        val controller = WindowCompat.getInsetsController(window, view)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
