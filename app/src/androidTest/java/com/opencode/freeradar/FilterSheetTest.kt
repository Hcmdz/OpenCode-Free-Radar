/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.opencode.freeradar.ui.components.FilterChipsRow
import com.opencode.freeradar.ui.components.FilterFab
import com.opencode.freeradar.ui.components.FilterSheet
import com.opencode.freeradar.ui.model.OfferFilter
import com.opencode.freeradar.ui.model.SourceFilter
import com.opencode.freeradar.ui.theme.AppThemePreview
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Filter bottom sheet: opens from the FAB and emits selections. */
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
                var open by remember { mutableStateOf(false) }
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val density = LocalDensity.current
                    val container = with(density) {
                        IntSize(maxWidth.roundToPx(), maxHeight.roundToPx())
                    }
                    FilterFab(
                        summary = "Filters",
                        active = true,
                        containerSize = container,
                        onOpen = { open = true },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .testTag("dashboard_filter")
                    )
                    FilterSheet(
                        visible = open,
                        filter = OfferFilter.FREE,
                        sourceFilter = SourceFilter.ALL_SOURCES,
                        statusCounts = OfferFilter.entries.associateWith { 1 },
                        sourceCounts = SourceFilter.entries.associateWith { 2 },
                        showReset = true,
                        onSelectFilter = onSelectFilter,
                        onSelectSource = onSelectSource,
                        onReset = {},
                        onDismiss = { open = false }
                    )
                }
            }
        }
    }

    @Test
    fun sheetOpensAndSelectsSource() {
        val selected = mutableListOf<SourceFilter>()
        content(onSelectSource = selected::add)
        rule.onNodeWithTag("dashboard_filter").performClick()
        rule.onNodeWithTag("filter_sheet").assertIsDisplayed()
        rule.onNodeWithText("OpenCode (2)").assertIsDisplayed()
        rule.onNodeWithText("OpenCode (2)").performClick()
        rule.runOnIdle { assert(selected == listOf(SourceFilter.OPENCODE)) }
    }

    @Test
    fun sheetShowsBothSectionsWithCounts() {
        content()
        rule.onNodeWithTag("dashboard_filter").performClick()
        rule.onNodeWithText("Free (1)").assertIsDisplayed()
        rule.onNodeWithText("OpenCode (2)").assertIsDisplayed()
    }

    @Test
    fun chipsSelectStatusWithoutSheet() {
        val selected = mutableListOf<OfferFilter>()
        rule.setContent {
            AppThemePreview {
                FilterChipsRow(
                    filter = OfferFilter.FREE,
                    statusCounts = OfferFilter.entries.associateWith { 1 },
                    onSelectFilter = selected::add
                )
            }
        }
        rule.onNodeWithTag("dashboard_chip_free").assertIsDisplayed()
        rule.onNodeWithTag("dashboard_chip_all").performClick()
        rule.runOnIdle { assert(selected == listOf(OfferFilter.ALL)) }
    }
}
