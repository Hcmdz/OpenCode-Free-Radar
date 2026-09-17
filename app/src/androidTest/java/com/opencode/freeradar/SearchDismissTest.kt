/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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

/** Tap-outside and related interactions must collapse the search suggestions. */
@RunWith(AndroidJUnit4::class)
class SearchDismissTest {

    @get:Rule
    val rule = createComposeRule()

    private val offers = listOf(
        OfferUi("p/m", "Muse Spark", "p", FreeStatus.FREE, null, 1_000L, "opencode-data")
    )

    private fun setScreen(
        query: androidx.compose.runtime.MutableState<String>,
        actions: MutableList<DashboardAction>,
        offers: List<OfferUi> = this.offers,
        recents: List<String> = listOf("muse")
    ) {
        rule.setContent {
            AppThemePreview {
                DashboardScreen(
                    state = DashboardUiState(
                        isLoading = false,
                        offers = offers,
                        filter = OfferFilter.FREE,
                        sourceFilter = SourceFilter.ALL_SOURCES,
                        query = query.value,
                        recentSearches = recents
                    ),
                    onAction = {
                        actions += it
                        if (it is DashboardAction.Search) query.value = it.query
                    }
                )
            }
        }
    }

    @Test
    fun openingFiltersDismissesSuggestions() {
        val query = mutableStateOf("")
        setScreen(query, mutableListOf())
        rule.onNodeWithTag("dashboard_search").performClick()
        rule.onNodeWithTag("search_suggestions").assertIsDisplayed()
        rule.onNodeWithTag("dashboard_filter").performClick()
        rule.onNodeWithTag("search_suggestions").assertDoesNotExist()
    }

    @Test
    fun selectingFilterOptionDismissesSuggestions() {
        val query = mutableStateOf("")
        val actions = mutableListOf<DashboardAction>()
        setScreen(query, actions)
        rule.onNodeWithTag("dashboard_search").performClick()
        rule.onNodeWithTag("search_suggestions").assertIsDisplayed()
        rule.onNodeWithTag("dashboard_filter").performClick()
        rule.onNodeWithTag("filter_option_all").performClick()
        rule.runOnIdle {
            assertEquals(DashboardAction.SelectFilter(OfferFilter.ALL), actions.lastOrNull())
        }
        rule.onNodeWithTag("search_suggestions").assertDoesNotExist()
    }

    @Test
    fun tappingEmptyAreaDismissesSuggestions() {
        val query = mutableStateOf("")
        setScreen(query, mutableListOf(), offers = emptyList())
        rule.onNodeWithTag("dashboard_search").performClick()
        rule.onNodeWithTag("search_suggestions").assertIsDisplayed()
        // Neutral gap between the filter button and the empty state: no
        // clickable child there, so only the tap-outside layer can react.
        val metrics = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics
        rule.onRoot().performTouchInput {
            click(Offset(metrics.widthPixels / 2f, metrics.heightPixels * 0.45f))
        }
        rule.onNodeWithTag("search_suggestions").assertDoesNotExist()
    }

    @Test
    fun clearingTextKeepsPanelOpen() {
        val query = mutableStateOf("")
        val actions = mutableListOf<DashboardAction>()
        setScreen(query, actions)
        rule.onNodeWithTag("dashboard_search").performTextInput("muse")
        rule.onNodeWithTag("search_suggestions").assertIsDisplayed()
        rule.onNodeWithContentDescription("Clear search").performClick()
        rule.runOnIdle {
            assertEquals(DashboardAction.Search(""), actions.lastOrNull())
        }
        // Clearing must not collapse the panel or drop field focus.
        rule.onNodeWithTag("search_suggestions").assertIsDisplayed()
        rule.onNodeWithTag("dashboard_search").assertIsFocused()
    }

    @Test
    fun scrollingDismissesSuggestionsButKeepsQuery() {
        val query = mutableStateOf("")
        val actions = mutableListOf<DashboardAction>()
        // ~20 tall cards: the list must actually scroll, otherwise
        // isScrollInProgress never rises and the test lies.
        val many = (0 until 20).map {
            OfferUi("p/m$it", "Model $it", "p", FreeStatus.FREE, null, 1_000L, "opencode-data")
        }
        setScreen(query, actions, offers = many)
        rule.onNodeWithTag("dashboard_search").performTextInput("muse")
        rule.onNodeWithTag("search_suggestions").assertIsDisplayed()
        rule.onNodeWithTag("dashboard_list").performTouchInput { swipeUp() }
        rule.onNodeWithTag("search_suggestions").assertDoesNotExist()
        // Only the panel folds; the typed query survives in the field.
        rule.onNodeWithTag("dashboard_search").assertTextContains("muse")
    }

    @Test
    fun draggingFilterFabKeepsItUsable() {
        val query = mutableStateOf("")
        setScreen(query, mutableListOf())
        rule.onNodeWithTag("dashboard_filter").performTouchInput { swipeUp() }
        // Drag repositions inside the container: still visible, still opening.
        rule.onNodeWithTag("dashboard_filter").assertIsDisplayed()
        rule.onNodeWithTag("dashboard_filter").performClick()
        rule.onNodeWithTag("filter_option_all").assertIsDisplayed()
    }
}
