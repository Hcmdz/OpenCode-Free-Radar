/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.opencode.freeradar.ui.components.FilterFab
import com.opencode.freeradar.ui.theme.AppThemePreview
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

/**
 * Disk restore must be one-shot: a late DataStore emission arriving after
 * the user dragged elsewhere must never yank the button back, otherwise
 * every quick re-grab visibly trembles.
 */
@RunWith(AndroidJUnit4::class)
class FilterFabRestoreTest {

    @get:Rule
    val rule = createComposeRule()

    private fun fabLeft(): Float {
        var left = 0f
        rule.runOnIdle {
            left = rule.onNodeWithTag("restore_filter")
                .fetchSemanticsNode().boundsInRoot.left
        }
        return left
    }

    @Test
    fun latePersistedEmissionNeverMovesDraggedFab() {
        val persisted: MutableState<IntOffset?> = mutableStateOf(IntOffset(-300, -200))
        rule.setContent {
            AppThemePreview {
                Box(modifier = Modifier.fillMaxSize()) {
                    FilterFab(
                        summary = "Filters",
                        active = false,
                        containerSize = IntSize(1080, 2000),
                        onOpen = {},
                        modifier = Modifier.testTag("restore_filter"),
                        persistedOffset = persisted.value
                    )
                }
            }
        }
        rule.waitForIdle()
        val settledLeft = fabLeft()
        // A late emission (e.g. the previous drop flushing through
        // DataStore after the user already dragged elsewhere) arrives:
        rule.runOnIdle { persisted.value = IntOffset(-600, -200) }
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        val afterLeft = fabLeft()
        assertTrue(
            "restored emission yanked FAB from $settledLeft to $afterLeft",
            abs(afterLeft - settledLeft) <= 1f
        )
    }
}
