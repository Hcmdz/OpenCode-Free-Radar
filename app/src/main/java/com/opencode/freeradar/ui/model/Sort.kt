/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.model

import androidx.annotation.StringRes
import com.opencode.freeradar.R
import com.opencode.freeradar.domain.model.Offer
import java.util.Locale

enum class OfferSort(@StringRes val labelRes: Int) {
    RECENT(R.string.sort_recent),
    NAME(R.string.sort_name),
    CONTEXT(R.string.sort_context)
}

/**
 * Favorites first, whatever sort the user picked: a starred model must not
 * sink under a fresher row. Inside each group the chosen sort applies, and
 * remoteId breaks the remaining ties so the order is a rule rather than a
 * side effect of SQLite's physical row order — a sync upserts with REPLACE,
 * which would otherwise reshuffle equal rows and move a star.
 *
 * sortedWith + thenBy, never two sortedBy: a second sortedBy replaces the
 * first ordering instead of adding to it.
 */
fun sortComparator(sort: OfferSort): Comparator<Offer> {
    val bySort: Comparator<Offer> = when (sort) {
        OfferSort.RECENT -> compareByDescending { it.verifiedAt }
        OfferSort.NAME -> compareBy { it.name.lowercase(Locale.ROOT) }
        OfferSort.CONTEXT -> compareByDescending { it.contextLength ?: -1 }
    }
    return compareByDescending<Offer> { it.favorite }
        .then(bySort)
        .thenBy { it.remoteId }
}
