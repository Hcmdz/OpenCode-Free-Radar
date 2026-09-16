/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.repository

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.opencode.freeradar.data.repository.OfflineFirstOfferRepository.Companion.FRESHNESS_WINDOW_MILLIS
import com.opencode.freeradar.data.repository.OfflineFirstOfferRepository.Companion.shouldSkipFresh
import com.opencode.freeradar.domain.model.SyncResult
import org.junit.jupiter.api.Test

class FreshnessGateTest {

    private val now = 1_000_000_000L
    private val freshAt = now - FRESHNESS_WINDOW_MILLIS + 60_000
    private val staleAt = now - FRESHNESS_WINDOW_MILLIS - 60_000

    @Test
    fun `fresh ok run is skipped`() {
        assertThat(shouldSkipFresh(SyncResult.OK.name, freshAt, now)).isEqualTo(true)
    }

    @Test
    fun `stale ok run is fetched`() {
        assertThat(shouldSkipFresh(SyncResult.OK.name, staleAt, now)).isEqualTo(false)
    }

    @Test
    fun `failed run is always fetched`() {
        assertThat(shouldSkipFresh(SyncResult.FAILED.name, freshAt, now)).isEqualTo(false)
    }

    @Test
    fun `missing run is fetched`() {
        assertThat(shouldSkipFresh(null, null, now)).isEqualTo(false)
    }
}
