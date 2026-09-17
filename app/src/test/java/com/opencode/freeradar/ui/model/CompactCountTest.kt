/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class CompactCountTest {

    @Test
    fun belowThousandIsRaw() {
        assertThat(0.compactCount()).isEqualTo("0")
        assertThat(999.compactCount()).isEqualTo("999")
    }

    @Test
    fun thousandsUseOneDecimalK() {
        assertThat(1_000.compactCount()).isEqualTo("1K")
        assertThat(1_024.compactCount()).isEqualTo("1K")
        assertThat(12_800.compactCount()).isEqualTo("12.8K")
        assertThat(32_000.compactCount()).isEqualTo("32K")
    }

    @Test
    fun roundingPromotesToMNever1000K() {
        assertThat(999_999.compactCount()).isEqualTo("1M")
        assertThat(1_000_000.compactCount()).isEqualTo("1M")
        assertThat(1_048_576.compactCount()).isEqualTo("1M")
        assertThat(1_280_000.compactCount()).isEqualTo("1.3M")
    }
}
