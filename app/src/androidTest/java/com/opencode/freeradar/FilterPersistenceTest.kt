/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
        rule.activityRule.scenario.close()
        ActivityScenario.launch(MainActivity::class.java)
        openSheet()
        // Async DataStore restore: let it land before asserting.
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
        rule.onNodeWithTag("source_option_opencode").assertIsSelected()
        rule.onNodeWithTag("sort_option_name").assertIsSelected()
        rule.onNodeWithTag("filter_show_local").assertIsOn()
    }
}
