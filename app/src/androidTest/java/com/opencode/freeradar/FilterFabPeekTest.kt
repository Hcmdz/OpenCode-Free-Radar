/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.opencode.freeradar.ui.model.OfferFilter
import com.opencode.freeradar.ui.model.SourceFilter
import com.opencode.freeradar.ui.screens.dashboard.DashboardScreen
import com.opencode.freeradar.ui.theme.AppThemePreview
import com.opencode.freeradar.ui.viewmodel.DashboardUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Filter FAB edge-peek: silence slides it toward the nearest edge (node stays
 * composed, unlike the old full-hide), any touch restores the docked spot.
 * Assertions compare pre/post bounds instead of absolute pixels, so they hold
 * at any density or FAB width. Sliver-tap restore is covered manually: the
 * node center sits off-screen once peeked, so no testTag click can reach it.
 */
@RunWith(AndroidJUnit4::class)
class FilterFabPeekTest {

    @get:Rule
    val rule = createComposeRule()

    private fun setScreen() {
        // Manual clock: auto-advance would burn the 5 s delay on first idle
        // and make every assertion racy.
        rule.mainClock.autoAdvance = false
        rule.setContent {
            AppThemePreview {
                DashboardScreen(
                    state = DashboardUiState(
                        isLoading = false,
                        // Empty catalog: mid-screen taps land on a neutral
                        // gap (tap-outside layer), never on a card.
                        offers = emptyList(),
                        filter = OfferFilter.FREE,
                        sourceFilter = SourceFilter.ALL_SOURCES,
                        query = "",
                        recentSearches = listOf("muse")
                    ),
                    onAction = {}
                )
            }
        }
    }

    private fun fabLeft(): Float {
        var left = 0f
        rule.runOnIdle {
            left = rule.onNodeWithTag("dashboard_filter")
                .fetchSemanticsNode().boundsInRoot.left
        }
        return left
    }

    private fun neutralTap() {
        val metrics = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics
        rule.onRoot().performTouchInput {
            click(Offset(metrics.widthPixels / 2f, metrics.heightPixels * 0.45f))
        }
    }

    @Test
    fun peeksToEdgeButStaysInTree() {
        setScreen()
        rule.onNodeWithTag("dashboard_filter").assertIsDisplayed()
        val dockedLeft = fabLeft()
        rule.mainClock.advanceTimeBy(6_000)
        rule.waitForIdle()
        // Still composed (fetch would throw otherwise) and slid toward the
        // right edge from its bottom-end anchor.
        val peekedLeft = fabLeft()
        assertTrue("peeked right: $peekedLeft was $dockedLeft", peekedLeft > dockedLeft)
    }

    @Test
    fun backgroundTapRestoresFullPosition() {
        setScreen()
        rule.mainClock.advanceTimeBy(6_000)
        rule.waitForIdle()
        val peekedLeft = fabLeft()
        neutralTap()
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        val restoredLeft = fabLeft()
        assertTrue("restored: $restoredLeft was $peekedLeft", restoredLeft < peekedLeft)
    }

    @Test
    fun releaseSnapsBackToNearestEdge() {
        setScreen()
        val dockedLeft = fabLeft()
        // Short leftward hop, released right-of-center: must glide back to
        // the anchor instead of stranding mid-screen. The clock advance
        // settles the 220 ms snap slide (and flushes the gesture's up event
        // so onDragEnd has run).
        rule.onNodeWithTag("dashboard_filter").performTouchInput { swipeLeft() }
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        val left = fabLeft()
        assertTrue("snapped back: $left was $dockedLeft", left >= dockedLeft - 1f)
    }

    @Test
    fun interactionRestartsTimer() {
        setScreen()
        val dockedLeft = fabLeft()
        // 4 s pass, a neutral tap pokes the timer, 4 more seconds pass:
        // only 4 s since the poke, so no rightward peek yet.
        rule.mainClock.advanceTimeBy(4_000)
        neutralTap()
        rule.mainClock.advanceTimeBy(4_000)
        rule.waitForIdle()
        val left = fabLeft()
        assertTrue("not peeked: $left was $dockedLeft", left <= dockedLeft + 0.5f)
    }
}
