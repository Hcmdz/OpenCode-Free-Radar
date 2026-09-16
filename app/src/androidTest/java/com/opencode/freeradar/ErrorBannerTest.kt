/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.opencode.freeradar.ui.components.ErrorBanner
import com.opencode.freeradar.ui.theme.AppThemePreview
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Error banner: exposes stable tags and fires its retry action. */
@RunWith(AndroidJUnit4::class)
class ErrorBannerTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun bannerShowsMessageAndFiresAction() {
        var clicks = 0
        rule.setContent {
            AppThemePreview {
                ErrorBanner(
                    message = "The source changed format.",
                    actionLabel = "Retry",
                    onAction = { clicks++ }
                )
            }
        }
        rule.onNodeWithTag("error_banner").assertIsDisplayed()
        rule.onNodeWithTag("error_banner_action").assertIsDisplayed()
        rule.onNodeWithText("Retry").performClick()
        rule.runOnIdle { assert(clicks == 1) }
    }
}
