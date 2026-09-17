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
 * Stable comparator: RECENT matches the DAO's verifiedAt DESC order, so the
 * default view never reshuffles what the database already ordered.
 */
fun sortComparator(sort: OfferSort): Comparator<Offer> = when (sort) {
    OfferSort.RECENT -> compareByDescending { it.verifiedAt }
    OfferSort.NAME -> compareBy { it.name.lowercase(Locale.ROOT) }
    OfferSort.CONTEXT -> compareByDescending { it.contextLength ?: -1 }
}
