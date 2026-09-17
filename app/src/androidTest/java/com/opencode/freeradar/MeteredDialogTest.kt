/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar

import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.opencode.freeradar.data.local.SyncPrefs
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Metered-network warning dialog, driven by a really metered WiFi
 * (netpolicy shell commands; restored afterwards).
 *
 * The activity launches inside the test body, after the metered
 * precondition: dashboard auto first-sync reads the metered state at
 * launch and would otherwise start (skeleton, no Refresh button) in a
 * race with the click below.
 */
@RunWith(AndroidJUnit4::class)
class MeteredDialogTest {

    @get:Rule
    val rule = createEmptyComposeRule()

    private fun shell(cmd: String): String {
        val fd: ParcelFileDescriptor = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand(cmd)
        return ParcelFileDescriptor.AutoCloseInputStream(fd).bufferedReader().use { it.readText() }
    }

    private fun setMetered(metered: Boolean?) {
        val value = metered?.toString() ?: "undefined"
        shell("cmd netpolicy set metered-network AndroidWifi $value")
        val expected = "AndroidWifi;${metered?.toString() ?: "none"}"
        val deadline = System.currentTimeMillis() + 15_000
        while (System.currentTimeMillis() < deadline) {
            if (shell("cmd netpolicy list wifi-networks").contains(expected)) break
            Thread.sleep(500)
        }
        // Toggling metered disconnects/reconnects WiFi: wait for a usable
        // route back, otherwise the tap races a null active network.
        val pingDeadline = System.currentTimeMillis() + 30_000
        while (System.currentTimeMillis() < pingDeadline) {
            if (shell("ping -c 1 -W 2 8.8.8.8").contains("1 received")) return
            Thread.sleep(1000)
        }
        throw AssertionError("no route back after metered=$value")
    }

    @Before
    fun makeWifiMetered() {
        setMetered(true)
        // Freeze the first-install state before launch: the dashboard
        // auto first-sync would otherwise race the Refresh click below
        // (skeleton instead of the button, or fail-open read mid WiFi
        // reconnect). This test only covers the manual refresh path.
        runBlocking {
            SyncPrefs(InstrumentationRegistry.getInstrumentation().targetContext)
                .setFirstSyncDone()
        }
    }

    @After
    fun restoreWifiMetered() = setMetered(null)

    @Test
    fun meteredPullShowsWarningDialog() {
        ActivityScenario.launch(MainActivity::class.java).use {
            rule.waitForIdle()
            rule.onNodeWithText("Refresh now").performClick()
            rule.waitUntil(10_000) {
                rule.onAllNodesWithTag("metered_dialog").fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithTag("metered_dialog").assertIsDisplayed()
            rule.onNodeWithTag("metered_later").performClick()
            rule.onNodeWithTag("metered_dialog").assertDoesNotExist()
        }
    }
}
