/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.model

import java.util.Locale
import kotlin.math.roundToInt

/**
 * Compact US count for pills and info lines: 999, 1K, 12.8K, 1M, 1.3M.
 * Hand-rolled over android.icu: the ICU class is a framework stub under
 * testDebugUnitTest, so only pure code stays covered by the fast suite.
 */
fun Int.compactCount(): String {
    if (this < 1_000) return toString()
    val inK = this / 1_000.0
    if (inK < 1_000.0) {
        val rounded = (inK * 10).roundToInt() / 10.0
        // 999_999 rounds to 1000.0K: promote to M instead of printing "1000K".
        if (rounded < 1_000.0) return trimmed(rounded) + "K"
    }
    val inM = (this / 1_000_000.0 * 10).roundToInt() / 10.0
    return trimmed(inM) + "M"
}

private fun trimmed(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString()
    else String.format(Locale.US, "%.1f", value)
