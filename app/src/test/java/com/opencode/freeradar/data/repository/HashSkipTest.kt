/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.repository

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.opencode.freeradar.data.repository.OfflineFirstOfferRepository.Companion.shouldSkipHash
import org.junit.jupiter.api.Test

class HashSkipTest {

    @Test
    fun `identical hash is skipped`() {
        assertThat(shouldSkipHash("abc", "abc")).isEqualTo(true)
    }

    @Test
    fun `changed hash is processed`() {
        assertThat(shouldSkipHash("abc", "def")).isEqualTo(false)
    }

    @Test
    fun `unknown incoming hash is always processed`() {
        assertThat(shouldSkipHash("abc", null)).isEqualTo(false)
    }

    @Test
    fun `missing stored hash is always processed`() {
        assertThat(shouldSkipHash(null, "abc")).isEqualTo(false)
    }
}
