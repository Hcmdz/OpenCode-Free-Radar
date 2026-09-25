/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar

import android.Manifest
import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.opencode.freeradar.data.local.AutoSync
import com.opencode.freeradar.data.local.SyncPrefs
import com.opencode.freeradar.robots.DashboardRobot
import com.opencode.freeradar.robots.SettingsRobot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Auto-sync mode and interval persist across process death (DataStore round-trip). */
@RunWith(AndroidJUnit4::class)
class SyncSettingsTest {

    // order 1 runs outermost: the dashboard asks for POST_NOTIFICATIONS on its
    // first composition, and that system dialog steals the compose hierarchy
    // from the test. Pre-granting means the app finds it already granted and
    // never shows the dialog.
    @get:Rule(order = 1)
    val notificationPermission =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule(order = 0)
    val rule = createAndroidComposeRule<MainActivity>()

    private fun prefs() = SyncPrefs(
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
            as Application
    )

    private fun openSyncSection() {
        rule.waitForIdle()
        DashboardRobot(rule).assertVisible().openSettings()
        SettingsRobot(rule).assertVisible().openSection("Sync")
    }

    private fun storedMode() = runBlocking { prefs().autoSync() }

    private fun storedInterval() = runBlocking { prefs().autoSyncIntervalHours() }

    @Test
    fun modeSelectionPersistsAcrossProcessDeath() {
        // Compare against whatever the run started with so the target is
        // guaranteed to be a real change, not a no-op tap.
        val target = if (storedMode() == AutoSync.WIFI) AutoSync.ALWAYS else AutoSync.WIFI
        val tag = "sync_mode_${target.name.lowercase()}"

        openSyncSection()
        rule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag(tag).performClick()
        rule.waitForIdle()
        assertEquals(target, storedMode())

        rule.activityRule.scenario.close()
        ActivityScenario.launch(MainActivity::class.java)
        openSyncSection()
        assertEquals(target, storedMode())
        rule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun intervalSelectionPersistsAcrossProcessDeath() {
        val target = AutoSync.INTERVAL_OPTIONS_HOURS.first { it != storedInterval() }
        val tag = "sync_interval_$target"

        openSyncSection()
        rule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag(tag).performClick()
        rule.waitForIdle()
        assertEquals(target, storedInterval())

        rule.activityRule.scenario.close()
        ActivityScenario.launch(MainActivity::class.java)
        openSyncSection()
        assertEquals(target, storedInterval())
    }

    @Test
    fun everyOfferedModeAndIntervalIsReachable() {
        openSyncSection()
        for (mode in AutoSync.entries) {
            val tag = "sync_mode_${mode.name.lowercase()}"
            rule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
        }
        for (hours in AutoSync.INTERVAL_OPTIONS_HOURS) {
            val tag = "sync_interval_$hours"
            rule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
        }
    }
}
