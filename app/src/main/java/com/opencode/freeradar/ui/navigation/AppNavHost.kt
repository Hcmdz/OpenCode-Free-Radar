/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
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
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider<NavKey> {
            entry<Dashboard> {
                DashboardRoot(
                    onOpenDetails = { backStack.add(Details(it)) },
                    onOpenSettings = { backStack.add(Settings) }
                )
            }
            entry<Details> { key ->
                DetailsRoot(offerId = key.offerId, onBack = { backStack.removeLastOrNull() })
            }
            entry<Settings> {
                SettingsRoot(onBack = { backStack.removeLastOrNull() })
            }
        }
    )
}
