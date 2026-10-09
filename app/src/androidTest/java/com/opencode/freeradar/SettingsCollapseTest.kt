/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar

import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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

    /** Asserts the translated label, never an English literal: the app ships fr+ar. */
    private fun str(@StringRes id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun content(
        onOpenLink: (String) -> Unit = {},
        onCheckUpdate: () -> Unit = {},
        onNotifExpiryToggle: (Boolean) -> Unit = {}
    ) {
        rule.setContent {
            AppThemePreview {
                SettingsScreen(
                    themeState = ThemeState(),
                    localeTag = "",
                    notifEnabled = false,
                    notifDenied = false,
                    notifExpiryEnabled = true,
                    autoSync = AutoSync.WIFI,
                    autoSyncIntervalHours = AutoSync.DEFAULT_INTERVAL_HOURS,
                    onMode = {},
                    onBlack = {},
                    onLocale = {},
                    onNotifToggle = {},
                    onNotifExpiryToggle = onNotifExpiryToggle,
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
    fun aboutSectionOpensAndClosesTheOpenSourceNotices() {
        content()
        rule.onNodeWithTag("about_open_source_notices_row").assertDoesNotExist()
        rule.onNodeWithText(str(R.string.settings_about)).performClick()
        rule.onNodeWithTag("about_open_source_notices_row").performClick()
        rule.onNodeWithTag("open_source_notices_body").assertIsDisplayed()
        rule.onNodeWithTag("open_source_notices_close").performClick()
        rule.onNodeWithTag("open_source_notices_body").assertDoesNotExist()
    }

    /**
     * The notice body is only meaningful if the raw resource actually loaded and
     * every bundled MIT copyright survives into it, and if the body scrolls —
     * otherwise the disclaimer at the end is unreachable.
     */
    @Test
    fun openSourceNoticesCarryEveryBundledMitCopyrightAndScroll() {
        content()
        rule.onNodeWithText(str(R.string.settings_about)).performClick()
        rule.onNodeWithTag("about_open_source_notices_row").performClick()

        val body = rule.onNodeWithTag("open_source_notices_body").fetchSemanticsNode()
        assertTrue(
            "notice body must be scrollable, else its closing disclaimer is unreachable",
            body.config.contains(SemanticsProperties.VerticalScrollAxisRange)
        )
        listOf("Jordon de Hoog", "AJ Alt", "QOS.ch").forEach { copyright ->
            rule.onNodeWithText(copyright, substring = true).assertIsDisplayed()
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

    /** The expiry switch rides the existing notifications section. */
    @Test
    fun expirySwitchLivesInTheNotificationsSection() {
        val emitted = mutableListOf<Boolean>()
        content(onNotifExpiryToggle = emitted::add)
        val section = targetContext().getString(R.string.settings_notifications)
        val label = targetContext().getString(R.string.notif_expiry)

        rule.onNodeWithText(section).performClick()
        rule.onNodeWithTag("settings_notifications_expiry_switch").assertIsDisplayed()
        rule.onNodeWithText(label).assertIsDisplayed()
        rule.onNodeWithTag("settings_notifications_expiry_switch").performClick()
        rule.runOnIdle { assert(emitted == listOf(false)) }
    }

    private fun targetContext() =
        InstrumentationRegistry.getInstrumentation().targetContext
}
