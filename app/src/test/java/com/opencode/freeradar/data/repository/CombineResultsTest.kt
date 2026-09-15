/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.repository

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.opencode.freeradar.domain.error.RefreshResult
import com.opencode.freeradar.domain.error.SourceError
import org.junit.jupiter.api.Test

class CombineResultsTest {

    @Test
    fun `all ok aggregates to Ok`() {
        assertThat(
            OfflineFirstOfferRepository.combineResults(
                mapOf("a" to RefreshResult.Ok, "b" to RefreshResult.Ok)
            )
        ).isEqualTo(RefreshResult.Ok)
    }

    @Test
    fun `mixed results aggregate to Partial with failed ids`() {
        assertThat(
            OfflineFirstOfferRepository.combineResults(
                mapOf(
                    "opencode-data" to RefreshResult.Ok,
                    "nvidia-build" to RefreshResult.Failed(SourceError.Unreachable),
                )
            )
        ).isEqualTo(RefreshResult.Partial(listOf("nvidia-build")))
    }

    @Test
    fun `all failed aggregates to the first error`() {
        assertThat(
            OfflineFirstOfferRepository.combineResults(
                mapOf(
                    "a" to RefreshResult.Failed(SourceError.Timeout),
                    "b" to RefreshResult.Failed(SourceError.Unreachable),
                )
            )
        ).isEqualTo(RefreshResult.Failed(SourceError.Timeout))
    }
}
