/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class SourceLabelTest {

    @Test
    fun `known sources map to friendly names`() {
        assertThat(sourceLabel("opencode-data")).isEqualTo("OpenCode")
        assertThat(sourceLabel("nvidia-build")).isEqualTo("NVIDIA")
        assertThat(sourceLabel("openrouter")).isEqualTo("OpenRouter")
    }

    @Test
    fun `unknown source falls back to raw id`() {
        assertThat(sourceLabel("future-source")).isEqualTo("future-source")
        assertThat(sourceLabel("")).isEqualTo("")
    }
}
