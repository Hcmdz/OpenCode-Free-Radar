/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.robots

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.opencode.freeradar.MainActivity

typealias RadarRule = AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>

class DashboardRobot(private val rule: RadarRule) {
    fun assertVisible(): DashboardRobot = apply {
        rule.onNodeWithTag("dashboard_screen").assertIsDisplayed()
    }

    fun assertOfferShown(name: String): DashboardRobot = apply {
        rule.onNodeWithText(name).assertIsDisplayed()
    }

    fun openFirstOffer(): DashboardRobot = apply {
        rule.onAllNodesWithTag("offer_card")[0].performClick()
    }

    fun openSecondOffer(): DashboardRobot = apply {
        rule.onAllNodesWithTag("offer_card")[1].performClick()
    }

    fun openSettings(): DashboardRobot = apply {
        rule.onNodeWithTag("dashboard_settings").performClick()
    }

    fun pullToRefresh(): DashboardRobot = apply {
        rule.onNodeWithTag("dashboard_list").performTouchInput { swipeDown() }
    }

    fun selectAll(): DashboardRobot = apply {
        rule.onNodeWithText("All").performClick()
    }
}

class DetailsRobot(private val rule: RadarRule) {
    fun assertVisible(): DetailsRobot = apply {
        rule.onNodeWithTag("details_screen").assertIsDisplayed()
    }

    fun assertTitle(name: String): DetailsRobot = apply {
        rule.onNodeWithText(name).assertIsDisplayed()
    }

    fun readTitle(): String {
        val node = rule.onNodeWithTag("details_title").fetchSemanticsNode()
        return node.config[androidx.compose.ui.semantics.SemanticsProperties.Text].first().text
    }

    fun assertPricesSection(): DetailsRobot = apply {
        rule.onNodeWithText("Prices").assertIsDisplayed()
    }
}

class SettingsRobot(private val rule: RadarRule) {
    fun assertVisible(): SettingsRobot = apply {
        rule.onNodeWithTag("settings_screen").assertIsDisplayed()
    }

    fun openSection(title: String): SettingsRobot = apply {
        rule.onNodeWithText(title).performClick()
    }

    fun tapDark(): SettingsRobot = apply {
        rule.onNodeWithText("DARK").performClick()
    }
}
