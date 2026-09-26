/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.opencode.freeradar.R
import com.opencode.freeradar.data.local.AutoSync
import com.opencode.freeradar.ui.screens.settings.SettingsScreen
import com.opencode.freeradar.ui.theme.AppThemePreview
import com.opencode.freeradar.ui.theme.ThemeState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Settings sections collapse and expand on header tap. */
@RunWith(AndroidJUnit4::class)
class SettingsCollapseTest {

    @get:Rule
    val rule = createComposeRule()

    private fun content(onOpenLink: (String) -> Unit = {}, onCheckUpdate: () -> Unit = {}) {
        rule.setContent {
            AppThemePreview {
                SettingsScreen(
                    themeState = ThemeState(),
                    localeTag = "",
                    notifEnabled = false,
                    notifDenied = false,
                    autoSync = AutoSync.WIFI,
                    autoSyncIntervalHours = AutoSync.DEFAULT_INTERVAL_HOURS,
                    onMode = {},
                    onBlack = {},
                    onLocale = {},
                    onNotifToggle = {},
                    onAutoSyncSelect = {},
                    onIntervalSelect = {},
                    onOpenNotifSettings = {},
                    onOpenLink = onOpenLink,
                    updateRowText = "Check for updates",
                    onCheckUpdate = onCheckUpdate,
                    onBack = {}
                )
            }
        }
    }

    @Test
    fun collapsingLanguageHidesItsOptions() {
        content()
        rule.onNodeWithText("English").assertDoesNotExist()
        rule.onNodeWithText("Language").performClick()
        rule.onNodeWithText("English").assertIsDisplayed()
        rule.onNodeWithText("Language").performClick()
        rule.onNodeWithText("English").assertDoesNotExist()
    }

    @Test
    fun aboutSectionRevealsContactAndEmitsLink() {
        val contact = InstrumentationRegistry.getInstrumentation().targetContext
            .getString(R.string.about_contact)
        // Guards the wiring: an empty resource would render a blank contact
        // row and a mailto with no target, which must never be published.
        assertTrue("about_contact must be filled", contact.isNotBlank())
        val opened = mutableListOf<String>()
        content(onOpenLink = opened::add)
        rule.onNodeWithText(contact).assertDoesNotExist()
        rule.onNodeWithText("About").performClick()
        rule.onNodeWithText(contact).assertIsDisplayed()
        rule.onNodeWithText(contact).performClick()
        rule.runOnIdle { assert(opened == listOf("mailto:$contact")) }
    }

    @Test
    fun aboutSectionOpensPrivacyAndTermsLinks() {
        val opened = mutableListOf<String>()
        content(onOpenLink = opened::add)
        rule.onNodeWithText("About").performClick()
        rule.onNodeWithText("Privacy Policy").performClick()
        rule.onNodeWithText("Terms of Service").performClick()
        rule.runOnIdle {
            assert(opened == listOf(
                "https://hcmdz.github.io/OpenCode-Free-Radar/privacy/",
                "https://hcmdz.github.io/OpenCode-Free-Radar/terms/"
            ))
        }
    }

    @Test
    fun aboutSectionRevealsUpdateRowAndEmitsCheck() {
        var checks = 0
        content(onCheckUpdate = { checks++ })
        rule.onNodeWithText("Check for updates").assertDoesNotExist()
        rule.onNodeWithText("About").performClick()
        rule.onNodeWithText("Check for updates").assertIsDisplayed()
        rule.onNodeWithText("Check for updates").performClick()
        rule.runOnIdle { assert(checks == 1) }
    }

    @Test
    fun filterButtonSectionOffersDelayAndSliver() {
        content()
        rule.onNodeWithText("Filter button").performClick()
        rule.onNodeWithText("5 s").performClick()
        rule.onNodeWithText("24 dp").performClick()
        rule.onNodeWithText("Filter button").performClick()
        rule.onNodeWithText("5 s").assertDoesNotExist()
    }
}
