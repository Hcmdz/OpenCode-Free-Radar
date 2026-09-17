/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.repository

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.opencode.freeradar.data.local.SyncRunEntity
import com.opencode.freeradar.data.repository.OfflineFirstOfferRepository.Companion.FRESHNESS_WINDOW_MILLIS
import com.opencode.freeradar.data.repository.OfflineFirstOfferRepository.Companion.isSourceFresh
import com.opencode.freeradar.domain.model.SyncResult
import org.junit.jupiter.api.Test

class SourceFreshnessTest {

    private val now = 1_000_000_000L
    private val freshAt = now - FRESHNESS_WINDOW_MILLIS + 60_000
    private val staleAt = now - FRESHNESS_WINDOW_MILLIS - 60_000

    private fun run(
        result: String?,
        completedAt: Long?,
        error: String? = null,
        startedAt: Long = completedAt ?: now,
    ) = SyncRunEntity(
        source = "s",
        startedAt = startedAt,
        completedAt = completedAt,
        result = result,
        error = error
    )

    @Test
    fun `recent real fetch is fresh`() {
        assertThat(
            isSourceFresh(listOf(run(SyncResult.OK.name, freshAt)), now)
        ).isEqualTo(true)
    }

    @Test
    fun `old real fetch is stale`() {
        assertThat(
            isSourceFresh(listOf(run(SyncResult.OK.name, staleAt)), now)
        ).isEqualTo(false)
    }

    @Test
    fun `hash skip attests freshness`() {
        assertThat(
            isSourceFresh(
                listOf(run(SyncResult.OK.name, freshAt, "skipped-hash")),
                now
            )
        ).isEqualTo(true)
    }

    @Test
    fun `metered skip proves nothing`() {
        assertThat(
            isSourceFresh(
                listOf(run(SyncResult.OK.name, freshAt, "skipped-metered")),
                now
            )
        ).isEqualTo(false)
    }

    @Test
    fun `metered skip falls through to older real fetch`() {
        assertThat(
            isSourceFresh(
                listOf(
                    run(SyncResult.OK.name, now, "skipped-metered", startedAt = now),
                    run(SyncResult.OK.name, freshAt, null, startedAt = freshAt)
                ),
                now
            )
        ).isEqualTo(true)
    }

    @Test
    fun `failed run is skipped over`() {
        assertThat(
            isSourceFresh(
                listOf(
                    run(SyncResult.FAILED.name, now, "server", startedAt = now),
                    run(SyncResult.OK.name, freshAt, null, startedAt = freshAt)
                ),
                now
            )
        ).isEqualTo(true)
    }

    @Test
    fun `no runs means stale`() {
        assertThat(isSourceFresh(emptyList(), now)).isEqualTo(false)
    }

    @Test
    fun `unfinished run is skipped over`() {
        assertThat(
            isSourceFresh(listOf(run(null, null)), now)
        ).isEqualTo(false)
    }
}
