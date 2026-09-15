/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.opencode.freeradar.ui.components.FilterBar
import com.opencode.freeradar.ui.model.OfferFilter
import com.opencode.freeradar.ui.model.SourceFilter
import com.opencode.freeradar.ui.theme.AppThemePreview
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Filter bottom sheet: opens from the bar button and emits selections. */
@RunWith(AndroidJUnit4::class)
class FilterSheetTest {

    @get:Rule
    val rule = createComposeRule()

    private fun content(
        onSelectSource: (SourceFilter) -> Unit = {},
        onSelectFilter: (OfferFilter) -> Unit = {}
    ) {
        rule.setContent {
            AppThemePreview {
                FilterBar(
                    filter = OfferFilter.FREE,
                    sourceFilter = SourceFilter.ALL_SOURCES,
                    statusCounts = OfferFilter.entries.associateWith { 1 },
                    sourceCounts = SourceFilter.entries.associateWith { 2 },
                    showReset = true,
                    onSelectFilter = onSelectFilter,
                    onSelectSource = onSelectSource,
                    onReset = {}
                )
            }
        }
    }

    @Test
    fun sheetOpensAndSelectsSource() {
        val selected = mutableListOf<SourceFilter>()
        content(onSelectSource = selected::add)
        rule.onNodeWithText("Filters").performClick()
        rule.onNodeWithText("OpenCode (2)").assertIsDisplayed()
        rule.onNodeWithText("OpenCode (2)").performClick()
        rule.runOnIdle { assert(selected == listOf(SourceFilter.OPENCODE)) }
    }

    @Test
    fun sheetShowsBothSectionsWithCounts() {
        content()
        rule.onNodeWithText("Filters").performClick()
        rule.onNodeWithText("Free (1)").assertIsDisplayed()
        rule.onNodeWithText("OpenCode (2)").assertIsDisplayed()
    }
}
