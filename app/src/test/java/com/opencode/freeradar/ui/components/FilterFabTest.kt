/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.components

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class FilterFabTest {

    private val container = IntSize(1000, 1600)
    private val fab = IntSize(160, 96)
    private val padEnd = 32
    private val padBottom = 224

    @Test
    fun `zero offset stays at anchor`() {
        assertThat(coerceFabOffset(IntOffset.Zero, container, fab, padEnd, padBottom))
            .isEqualTo(IntOffset.Zero)
    }

    @Test
    fun `up-left travel is clamped inside the container`() {
        assertThat(coerceFabOffset(IntOffset(-5000, -5000), container, fab, padEnd, padBottom))
            .isEqualTo(IntOffset(-(1000 - 160 - 32), -(1600 - 96 - 224)))
    }

    @Test
    fun `positive offset past the anchor is rejected`() {
        assertThat(coerceFabOffset(IntOffset(50, 40), container, fab, padEnd, padBottom))
            .isEqualTo(IntOffset.Zero)
    }

    @Test
    fun `shrunk container re-coerces a restored offset`() {
        val tiny = IntSize(300, 400)
        assertThat(coerceFabOffset(IntOffset(-700, -1200), tiny, fab, padEnd, padBottom))
            .isEqualTo(IntOffset(-(300 - 160 - 32), -(400 - 96 - 224)))
    }

    @Test
    fun `unmeasured container resets to anchor`() {
        assertThat(coerceFabOffset(IntOffset(-100, -100), IntSize.Zero, fab, padEnd, padBottom))
            .isEqualTo(IntOffset.Zero)
    }

    @Test
    fun `unmeasured fab keeps its offset until sizes land`() {
        val offset = IntOffset(-100, -100)
        assertThat(coerceFabOffset(offset, container, IntSize.Zero, padEnd, padBottom))
            .isEqualTo(offset)
    }

    @Test
    fun `saver round-trips the offset`() {
        val saver: Saver<IntOffset, Any> = IntOffsetSaver
        val scope = object : SaverScope {
            override fun canBeSaved(value: Any) = true
        }
        val saved: Any? = with(saver) { scope.save(IntOffset(-120, -300)) }
        assertThat(saved).isEqualTo(listOf(-120, -300))
        assertThat(saver.restore(listOf(-120, -300))).isEqualTo(IntOffset(-120, -300))
    }

    @Test
    fun `saver restore falls back to zero on truncated data`() {
        val saver: Saver<IntOffset, Any> = IntOffsetSaver
        assertThat(saver.restore(emptyList<Int>())).isEqualTo(IntOffset.Zero)
        assertThat(saver.restore(listOf(5))).isEqualTo(IntOffset(5, 0))
    }
}
