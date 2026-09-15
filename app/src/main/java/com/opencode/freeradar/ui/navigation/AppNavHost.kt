/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.opencode.freeradar.ui.screens.dashboard.DashboardRoot
import com.opencode.freeradar.ui.screens.details.DetailsRoot
import com.opencode.freeradar.ui.screens.settings.SettingsRoot

@Composable
fun AppNavHost() {
    val backStack = rememberNavBackStack(Dashboard)
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider<NavKey> {
            entry<Dashboard> {
                DashboardRoot(
                    onOpenDetails = { backStack.add(Details(it)) },
                    onOpenSettings = { backStack.add(Settings) }
                )
            }
            entry<Details> { key ->
                // Nav3 1.1.x ships no ViewModelStore decorator (official one
                // lands in 1.2): without a per-entry store, koinViewModel()
                // resolves to the activity and every Details screen reuses
                // the first offerId. Upgrade path: drop this for
                // rememberViewModelStoreNavEntryDecorator on Nav3 1.2 stable.
                val storeOwner = remember {
                    object : ViewModelStoreOwner {
                        override val viewModelStore: ViewModelStore = ViewModelStore()
                    }
                }
                DisposableEffect(Unit) {
                    onDispose { storeOwner.viewModelStore.clear() }
                }
                CompositionLocalProvider(LocalViewModelStoreOwner provides storeOwner) {
                    DetailsRoot(offerId = key.offerId, onBack = { backStack.removeLastOrNull() })
                }
            }
            entry<Settings> {
                SettingsRoot(onBack = { backStack.removeLastOrNull() })
            }
        }
    )
}
