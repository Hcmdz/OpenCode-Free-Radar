/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar

import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Metered-network warning dialog, driven by a really metered WiFi
 * (netpolicy shell commands; restored afterwards).
 */
@RunWith(AndroidJUnit4::class)
class MeteredDialogTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

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
    fun makeWifiMetered() = setMetered(true)

    @After
    fun restoreWifiMetered() = setMetered(null)

    @Test
    fun meteredPullShowsWarningDialog() {
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
