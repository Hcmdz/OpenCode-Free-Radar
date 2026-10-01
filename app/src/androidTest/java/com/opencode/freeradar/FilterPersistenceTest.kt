/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar

import android.Manifest
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Dashboard selections survive process death (DataStore round-trip). */
@RunWith(AndroidJUnit4::class)
class FilterPersistenceTest {

    // order 1 runs outermost: the dashboard asks for POST_NOTIFICATIONS on its
    // first composition, and that system dialog steals the compose hierarchy
    // from the test. Pre-granting means the app finds it already granted and
    // never shows the dialog.
    @get:Rule(order = 1)
    val notificationPermission =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule(order = 0)
    val rule = createAndroidComposeRule<MainActivity>()

    private fun openSheet() {
        rule.waitForIdle()
        rule.onNodeWithTag("dashboard_filter").performClick()
        rule.onNodeWithTag("filter_sheet").assertIsDisplayed()
    }

    @Test
    fun sourceSortAndLocalSwitchSurviveProcessDeath() {
        openSheet()
        rule.onNodeWithTag("source_option_opencode").performClick()
        rule.onNodeWithTag("sort_option_name").performClick()
        rule.onNodeWithTag("filter_show_local").performClick()
        rule.onNodeWithTag("filter_show_local").assertIsOn()
        // Flush the debounced DataStore write before killing the process.
        // The 300 ms debounce and the disk write both run on real time, so
        // mainClock.advanceTimeBy cannot reach them: it only advances compose
        // idling. A real sleep is the only wait that flushes the IO.
        Thread.sleep(1_000)
        rule.waitForIdle()
        rule.activityRule.scenario.close()
        ActivityScenario.launch(MainActivity::class.java)
        openSheet()
        // Async DataStore restore: poll the restored state instead of guessing
        // a duration, so a slow read cannot read as a persistence failure.
        rule.waitUntil(8_000) {
            rule.onAllNodesWithTag("source_option_opencode")
                .fetchSemanticsNodes()
                .any { it.config[SemanticsProperties.Selected] == true }
        }
        rule.onNodeWithTag("sort_option_name").assertIsSelected()
        rule.onNodeWithTag("filter_show_local").assertIsOn()
    }
}
