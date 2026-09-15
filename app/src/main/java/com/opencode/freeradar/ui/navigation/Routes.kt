/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object Dashboard : NavKey

@Serializable
data class Details(val offerId: String) : NavKey

@Serializable
data object Settings : NavKey
