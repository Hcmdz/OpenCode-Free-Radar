/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.opencode.freeradar.robots.DashboardRobot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** WiFi-only toggle persists across process death (DataStore round-trip). */
@RunWith(AndroidJUnit4::class)
class SyncSettingsTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun openSyncSection() {
        rule.waitForIdle()
        DashboardRobot(rule).assertVisible().openSettings()
        rule.onNodeWithTag("settings_screen").assertIsDisplayed()
        rule.onNodeWithText("Sync").performClick()
    }

    @Test
    fun wifiOnlyToggleSurvivesProcessDeath() {
        openSyncSection()
        rule.onNodeWithTag("settings_sync_switch").assertIsDisplayed()
        val wasOn = try {
            rule.onNodeWithTag("settings_sync_switch").assertIsOn()
            true
        } catch (_: AssertionError) {
            false
        }
        rule.onNodeWithTag("settings_sync_switch").performClick()
        if (wasOn) {
            rule.onNodeWithTag("settings_sync_switch").assertIsOff()
        } else {
            rule.onNodeWithTag("settings_sync_switch").assertIsOn()
        }
        rule.activityRule.scenario.close()
        ActivityScenario.launch(MainActivity::class.java)
        openSyncSection()
        if (wasOn) {
            rule.onNodeWithTag("settings_sync_switch").assertIsOff()
        } else {
            rule.onNodeWithTag("settings_sync_switch").assertIsOn()
        }
    }
}
