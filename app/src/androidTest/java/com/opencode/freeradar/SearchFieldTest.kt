/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.opencode.freeradar.domain.model.FreeStatus
import com.opencode.freeradar.ui.model.OfferFilter
import com.opencode.freeradar.ui.model.OfferUi
import com.opencode.freeradar.ui.model.SourceFilter
import com.opencode.freeradar.ui.screens.dashboard.DashboardScreen
import com.opencode.freeradar.ui.theme.AppThemePreview
import com.opencode.freeradar.ui.viewmodel.DashboardAction
import com.opencode.freeradar.ui.viewmodel.DashboardUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchFieldTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun typingEmitsSearchActionsAndShowsText() {
        val actions = mutableListOf<DashboardAction>()
        val query = androidx.compose.runtime.mutableStateOf("")
        rule.setContent {
            AppThemePreview {
                DashboardScreen(
                    state = DashboardUiState(
                        isLoading = false,
                        offers = listOf(
                            OfferUi("p/m", "Muse Spark", "p", FreeStatus.FREE, null, 1_000L, "opencode-data")
                        ),
                        filter = OfferFilter.FREE,
                        sourceFilter = SourceFilter.ALL_SOURCES,
                        query = query.value
                    ),
                    onAction = {
                        actions += it
                        if (it is DashboardAction.Search) query.value = it.query
                    }
                )
            }
        }
        rule.onNodeWithTag("dashboard_search").assertIsDisplayed()
        rule.onNodeWithTag("dashboard_search").performTextInput("muse")
        rule.runOnIdle {
            assertEquals(DashboardAction.Search("muse"), actions.lastOrNull())
        }
        // The typed text must stick: no stale display value reverting it.
        rule.onNodeWithTag("dashboard_search").assertTextContains("muse")
    }

    @Test
    fun bareFieldKeepsInput() {
        val query = androidx.compose.runtime.mutableStateOf("")
        rule.setContent {
            AppThemePreview {
                androidx.compose.material3.OutlinedTextField(
                    value = query.value,
                    onValueChange = { query.value = it },
                    modifier = androidx.compose.ui.Modifier
                        .testTag("bare_search")
                )
            }
        }
        rule.onNodeWithTag("bare_search").performTextInput("muse")
        rule.onNodeWithTag("bare_search").assertTextContains("muse")
    }
}
