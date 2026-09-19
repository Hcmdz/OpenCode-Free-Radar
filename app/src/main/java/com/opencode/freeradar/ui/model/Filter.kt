/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.model

import androidx.annotation.StringRes
import com.opencode.freeradar.R
import com.opencode.freeradar.domain.model.Offer
import com.opencode.freeradar.domain.model.isLocalProvider
import com.opencode.freeradar.domain.model.isUsableFree

enum class OfferFilter(@StringRes val labelRes: Int) {
    ALL(R.string.filter_all),
    FREE(R.string.filter_free),
    COMPATIBLE(R.string.filter_compatible),
    FREE_COMPATIBLE(R.string.filter_free_compatible),
    FAVORITE(R.string.filter_favorite)
}

enum class SourceFilter(@StringRes val labelRes: Int, val sourceId: String?) {
    ALL_SOURCES(R.string.filter_all_sources, null),
    OPENCODE(R.string.filter_source_opencode, "opencode-data"),
    OPENROUTER(R.string.filter_source_openrouter, "openrouter"),
    LITELLM(R.string.filter_source_litellm, "litellm")
}

/** Zen provider id: OPENCODE matches served models, not the ingestion pipeline. */
private const val ZEN_PROVIDER_ID = "opencode"

/**
 * OPENCODE is the Zen provider: source "opencode-data" carries the whole
 * models.dev catalog (222 providers), so matching it shows 600+ rows that
 * are not Zen models. Every other entry keeps its source matching.
 */
fun SourceFilter.matches(offer: Offer): Boolean =
    if (this == SourceFilter.OPENCODE) offer.providerId == ZEN_PROVIDER_ID
    else sourceId == null || offer.source == sourceId

fun OfferFilter.freeOnly(): Boolean = when (this) {
    OfferFilter.ALL, OfferFilter.COMPATIBLE, OfferFilter.FAVORITE -> false
    OfferFilter.FREE, OfferFilter.FREE_COMPATIBLE -> true
}

fun OfferFilter.compatibleOnly(): Boolean = when (this) {
    OfferFilter.ALL, OfferFilter.FREE, OfferFilter.FAVORITE -> false
    OfferFilter.COMPATIBLE, OfferFilter.FREE_COMPATIBLE -> true
}

fun OfferFilter.favoriteOnly(): Boolean = this == OfferFilter.FAVORITE

data class FacetCounts(
    val status: Map<OfferFilter, Int>,
    val source: Map<SourceFilter, Int>
)

/**
 * Faceted counts over the FULL offer list: status counts honor the active
 * source filter, source counts honor the active status filter. Pure for
 * fast unit tests; the ViewModel feeds it repository rows. hideLocal
 * mirrors the sheet switch: local/self-hosted rows leave every count.
 */
fun facetCounts(
    offers: List<Offer>,
    filter: OfferFilter,
    source: SourceFilter,
    hideLocal: Boolean = false
): FacetCounts {
    fun List<Offer>.matchingStatus(f: OfferFilter) = filter {
        (!hideLocal || !it.providerId.isLocalProvider()) &&
        (!f.freeOnly() || it.freeStatus.isUsableFree()) &&
            (!f.compatibleOnly() || it.openCodeCompatible) &&
            (!f.favoriteOnly() || it.favorite)
    }
    val bySource = offers.filter { source.matches(it) }
    val statusCounts = OfferFilter.entries.associateWith { bySource.matchingStatus(it).size }
    val byStatus = offers.matchingStatus(filter)
    val sourceCounts = SourceFilter.entries.associateWith { s ->
        byStatus.count { s.matches(it) }
    }
    return FacetCounts(statusCounts, sourceCounts)
}
