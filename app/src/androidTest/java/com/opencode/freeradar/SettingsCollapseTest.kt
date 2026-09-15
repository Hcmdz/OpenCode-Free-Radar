/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.opencode.freeradar.ui.screens.settings.SettingsScreen
import com.opencode.freeradar.ui.theme.AppThemePreview
import com.opencode.freeradar.ui.theme.ThemeState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Settings sections collapse and expand on header tap. */
@RunWith(AndroidJUnit4::class)
class SettingsCollapseTest {

    @get:Rule
    val rule = createComposeRule()

    private fun content(onOpenLink: (String) -> Unit = {}) {
        rule.setContent {
            AppThemePreview {
                SettingsScreen(
                    themeState = ThemeState(),
                    localeTag = "",
                    notifEnabled = false,
                    notifDenied = false,
                    onMode = {},
                    onBlack = {},
                    onLocale = {},
                    onNotifToggle = {},
                    onOpenNotifSettings = {},
                    onOpenLink = onOpenLink,
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
        val opened = mutableListOf<String>()
        content(onOpenLink = opened::add)
        rule.onNodeWithText("HcmDz.Dev@gmail.com").assertDoesNotExist()
        rule.onNodeWithText("About").performClick()
        rule.onNodeWithText("HcmDz.Dev@gmail.com").assertIsDisplayed()
        rule.onNodeWithText("HcmDz.Dev@gmail.com").performClick()
        rule.runOnIdle { assert(opened == listOf("mailto:HcmDz.Dev@gmail.com")) }
    }
}
