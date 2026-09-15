/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
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
class SearchSuggestionsTest {

    private val offers = listOf(
        OfferUi("p/m", "Muse Spark", "p", FreeStatus.FREE, null, 1_000L, "opencode-data")
    )

    private fun stateOf(query: String, recents: List<String> = emptyList()) = DashboardUiState(
        isLoading = false,
        offers = offers,
        filter = OfferFilter.FREE,
        sourceFilter = SourceFilter.ALL_SOURCES,
        query = query,
        recentSearches = recents
    )

    @Test
    fun focusShowsProviderSuggestions() {
        val query = mutableStateOf("")
        rule.setContent {
            AppThemePreview {
                DashboardScreen(state = stateOf(query.value), onAction = {})
            }
        }
        rule.onNodeWithTag("search_suggestions").assertDoesNotExist()
        rule.onNodeWithTag("dashboard_search").performClick()
        rule.onNodeWithTag("search_suggestions").assertIsDisplayed()
        rule.onNodeWithText("OpenCode").assertIsDisplayed()
    }

    @Test
    fun tappingRecentAppliesItsQuery() {
        val query = mutableStateOf("")
        val actions = mutableListOf<DashboardAction>()
        rule.setContent {
            AppThemePreview {
                DashboardScreen(
                    state = stateOf(query.value, recents = listOf("muse")),
                    onAction = {
                        actions += it
                        if (it is DashboardAction.Search) query.value = it.query
                    }
                )
            }
        }
        rule.onNodeWithTag("dashboard_search").performClick()
        rule.onNodeWithText("muse", useUnmergedTree = true).performClick()
        rule.runOnIdle {
            assertEquals(DashboardAction.Search("muse"), actions.lastOrNull())
        }
    }

    @Test
    fun imeSearchSubmitsForRecents() {
        val query = mutableStateOf("")
        val actions = mutableListOf<DashboardAction>()
        rule.setContent {
            AppThemePreview {
                DashboardScreen(
                    state = stateOf(query.value),
                    onAction = {
                        actions += it
                        if (it is DashboardAction.Search) query.value = it.query
                    }
                )
            }
        }
        rule.onNodeWithTag("dashboard_search").performTextInput("muse!")
        rule.onNodeWithTag("dashboard_search").performImeAction()
        rule.runOnIdle {
            assertEquals(DashboardAction.SubmitSearch("muse!"), actions.lastOrNull())
        }
    }

    @get:Rule
    val rule = createComposeRule()
}
