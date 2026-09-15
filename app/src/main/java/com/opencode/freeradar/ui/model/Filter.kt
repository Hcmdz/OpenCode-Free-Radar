/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.model

import androidx.annotation.StringRes
import com.opencode.freeradar.R

enum class OfferFilter(@StringRes val labelRes: Int) {
    ALL(R.string.filter_all),
    FREE(R.string.filter_free),
    COMPATIBLE(R.string.filter_compatible),
    FREE_COMPATIBLE(R.string.filter_free_compatible)
}

enum class SourceFilter(@StringRes val labelRes: Int, val sourceId: String?) {
    ALL_SOURCES(R.string.filter_all_sources, null),
    OPENCODE(R.string.filter_source_opencode, "opencode-data"),
    NVIDIA(R.string.filter_source_nvidia, "nvidia-build")
}
