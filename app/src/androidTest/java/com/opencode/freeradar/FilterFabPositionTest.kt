/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.IntOffset
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.opencode.freeradar.domain.model.FreeStatus
import com.opencode.freeradar.ui.model.OfferFilter
import com.opencode.freeradar.ui.model.OfferUi
import com.opencode.freeradar.ui.model.SourceFilter
import com.opencode.freeradar.ui.screens.dashboard.DashboardScreen
import com.opencode.freeradar.ui.theme.AppThemePreview
import com.opencode.freeradar.ui.viewmodel.DashboardUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

/**
 * Filter FAB shares the bottom-end corner with the scroll-top button:
 * when the list is scrolled the filter anchor must glide clear above it,
 * and every drop must report its snapped position for cross-launch memory.
 */
@RunWith(AndroidJUnit4::class)
class FilterFabPositionTest {

    @get:Rule
    val rule = createComposeRule()

    private val many = (0 until 20).map {
        OfferUi("p/m$it", "Model $it", "p", FreeStatus.FREE, null, 1_000L, "opencode-data")
    }

    private fun setScreen(
        offers: List<OfferUi> = many,
        onPersistFabOffset: (IntOffset) -> Unit = {},
        persistedFabOffset: IntOffset? = null
    ) {
        rule.setContent {
            AppThemePreview {
                DashboardScreen(
                    state = DashboardUiState(
                        isLoading = false,
                        offers = offers,
                        filter = OfferFilter.FREE,
                        sourceFilter = SourceFilter.ALL_SOURCES,
                        query = "",
                        recentSearches = emptyList()
                    ),
                    // Peek slides the FAB horizontally mid-test; the
                    // clearance assertion is vertical-only, but a fixed
                    // anchor keeps the persist assertion exact.
                    filterAutoPeek = false,
                    onAction = {},
                    persistedFabOffset = persistedFabOffset,
                    onPersistFabOffset = onPersistFabOffset
                )
            }
        }
    }

    @Test
    fun scrolledListKeepsFilterAboveScrollTop() {
        setScreen()
        rule.onNodeWithTag("dashboard_list").performTouchInput { swipeUp() }
        rule.waitForIdle()
        // Guard: without a visible scroll-top button the clearance below
        // proves nothing.
        rule.onNodeWithTag("dashboard_scroll_top").assertIsDisplayed()
        var filterBottom = 0f
        var scrollTopTop = 0f
        rule.runOnIdle {
            filterBottom = rule.onNodeWithTag("dashboard_filter")
                .fetchSemanticsNode().boundsInRoot.bottom
            scrollTopTop = rule.onNodeWithTag("dashboard_scroll_top")
                .fetchSemanticsNode().boundsInRoot.top
        }
        assertTrue(
            "filter bottom $filterBottom overlaps scroll-top top $scrollTopTop",
            filterBottom <= scrollTopTop
        )
    }

    @Test
    fun dropReportsSnappedPositionForPersistence() {
        val persisted = mutableListOf<IntOffset>()
        setScreen(offers = many.take(1), onPersistFabOffset = persisted::add)
        // Short leftward hop, released right-of-center: snaps back to the
        // anchor, and the drop reports exactly that snapped position.
        rule.onNodeWithTag("dashboard_filter").performTouchInput { swipeLeft() }
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        rule.runOnIdle {
            assertTrue(
                "expected single snapped report, got $persisted",
                persisted == listOf(IntOffset.Zero)
            )
        }
    }

    @Test
    fun dropFarFromGrabLandsExactlyWithNoJumpBack() {
        // Far leftward drag from the anchor, released left-of-center: snaps
        // to the LEFT edge. Without an instant snap at drop the display
        // stays glued to the pre-drag animated value (anchor) while the
        // model says left — every far drop visibly reverts. The drag runs
        // on the root in screen coordinates (node-local swipes cannot
        // leave the node's own bounds).
        setScreen(offers = many.take(1))
        rule.waitForIdle()
        var start = Offset.Zero
        rule.runOnIdle {
            start = rule.onNodeWithTag("dashboard_filter")
                .fetchSemanticsNode().boundsInRoot.center
        }
        rule.onRoot().performTouchInput {
            swipe(start, Offset(200f, start.y), 500)
        }
        var atDrop = 0f
        rule.runOnIdle {
            atDrop = rule.onNodeWithTag("dashboard_filter")
                .fetchSemanticsNode().boundsInRoot.left
        }
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        var settled = 0f
        rule.runOnIdle {
            settled = rule.onNodeWithTag("dashboard_filter")
                .fetchSemanticsNode().boundsInRoot.left
        }
        assertTrue(
            "jump-back on drop: $atDrop settled to $settled",
            abs(atDrop - settled) <= 1f
        )
        assertTrue("FAB never left the anchor: settled at $settled", settled < 400f)
    }
}
