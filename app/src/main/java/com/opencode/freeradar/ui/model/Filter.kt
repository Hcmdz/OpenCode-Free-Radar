/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.model

import androidx.annotation.StringRes
import com.opencode.freeradar.R
import com.opencode.freeradar.domain.model.Offer
import com.opencode.freeradar.domain.model.isUsableFree

enum class OfferFilter(@StringRes val labelRes: Int) {
    ALL(R.string.filter_all),
    FREE(R.string.filter_free),
    COMPATIBLE(R.string.filter_compatible),
    FREE_COMPATIBLE(R.string.filter_free_compatible)
}

enum class SourceFilter(@StringRes val labelRes: Int, val sourceId: String?) {
    ALL_SOURCES(R.string.filter_all_sources, null),
    OPENCODE(R.string.filter_source_opencode, "opencode-data"),
    OPENROUTER(R.string.filter_source_openrouter, "openrouter")
}

fun OfferFilter.freeOnly(): Boolean = when (this) {
    OfferFilter.ALL, OfferFilter.COMPATIBLE -> false
    OfferFilter.FREE, OfferFilter.FREE_COMPATIBLE -> true
}

fun OfferFilter.compatibleOnly(): Boolean = when (this) {
    OfferFilter.ALL, OfferFilter.FREE -> false
    OfferFilter.COMPATIBLE, OfferFilter.FREE_COMPATIBLE -> true
}

data class FacetCounts(
    val status: Map<OfferFilter, Int>,
    val source: Map<SourceFilter, Int>
)

/**
 * Faceted counts over the FULL offer list: status counts honor the active
 * source filter, source counts honor the active status filter. Pure for
 * fast unit tests; the ViewModel feeds it repository rows.
 */
fun facetCounts(offers: List<Offer>, filter: OfferFilter, source: SourceFilter): FacetCounts {
    fun List<Offer>.matchingStatus(f: OfferFilter) = filter {
        (!f.freeOnly() || it.freeStatus.isUsableFree()) &&
            (!f.compatibleOnly() || it.openCodeCompatible)
    }
    val bySource = offers.filter { source.sourceId == null || it.source == source.sourceId }
    val statusCounts = OfferFilter.entries.associateWith { bySource.matchingStatus(it).size }
    val byStatus = offers.matchingStatus(filter)
    val sourceCounts = SourceFilter.entries.associateWith { s ->
        byStatus.count { s.sourceId == null || it.source == s.sourceId }
    }
    return FacetCounts(statusCounts, sourceCounts)
}
