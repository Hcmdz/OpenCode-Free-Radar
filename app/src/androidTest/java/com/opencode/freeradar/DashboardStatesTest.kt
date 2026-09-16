/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.opencode.freeradar.ui.screens.dashboard.DashboardScreen
import com.opencode.freeradar.ui.viewmodel.DashboardAction
import com.opencode.freeradar.ui.viewmodel.DashboardUiState
import com.opencode.freeradar.ui.theme.AppThemePreview
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Dashboard empty/error/pill/dialog states render from state alone (no network). */
@RunWith(AndroidJUnit4::class)
class DashboardStatesTest {

    @get:Rule
    val rule = createComposeRule()

    private fun content(state: DashboardUiState, actions: MutableList<DashboardAction> = mutableListOf()) {
        rule.setContent {
            AppThemePreview {
                DashboardScreen(
                    state = state,
                    onAction = { actions += it },
                    onOpenSettings = {}
                )
            }
        }
    }

    @Test
    fun noMatchShowsHintWithoutRefreshButton() {
        content(DashboardUiState(isLoading = false, isBaseEmpty = false))
        rule.onNodeWithTag("dashboard_screen").assertIsDisplayed()
        rule.onNodeWithText("Refresh now").assertDoesNotExist()
    }

    @Test
    fun emptyBaseShowsRefreshButton() {
        content(DashboardUiState(isLoading = false, isBaseEmpty = true))
        rule.onNodeWithText("Refresh now").assertIsDisplayed()
    }

    @Test
    fun pendingNewShowsPillAndAcks() {
        val actions = mutableListOf<DashboardAction>()
        content(
            DashboardUiState(
                isLoading = false,
                isBaseEmpty = false,
                offers = listOf(
                    com.opencode.freeradar.ui.model.OfferUi(
                        remoteId = "s/m", name = "M", providerId = "s",
                        freeStatus = com.opencode.freeradar.domain.model.FreeStatus.FREE,
                        contextLength = null, verifiedAt = 1L, source = "s",
                        favorite = false
                    )
                ),
                pendingNew = 2
            ),
            actions
        )
        rule.onNodeWithTag("new_freebies_pill").assertIsDisplayed()
        rule.onNodeWithTag("new_freebies_pill").performClick()
        rule.runOnIdle { assert(actions.contains(DashboardAction.AckPendingNew)) }
    }

    @Test
    fun meteredWarningShowsDialogWithThreeActions() {
        val actions = mutableListOf<DashboardAction>()
        content(DashboardUiState(isLoading = false, meteredWarning = true), actions)
        rule.onNodeWithTag("metered_dialog").assertIsDisplayed()
        rule.onNodeWithTag("metered_sync_once").performClick()
        rule.runOnIdle { assert(actions.contains(DashboardAction.MeteredSyncOnce)) }
    }
}
