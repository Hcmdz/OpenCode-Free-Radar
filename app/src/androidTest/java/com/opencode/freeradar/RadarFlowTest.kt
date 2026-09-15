/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import com.opencode.freeradar.robots.DashboardRobot
import com.opencode.freeradar.robots.DetailsRobot
import com.opencode.freeradar.robots.SettingsRobot
import org.junit.Rule
import org.junit.Test

class RadarFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun dashboardToDetailsToSettingsFlow() {
        rule.waitForIdle()
        // Fresh database shows the empty state: populate through its button,
        // then exercise pull-to-refresh on the resulting list.
        rule.onNodeWithText("Refresh now").performClick()
        rule.waitUntil(60_000) {
            rule.onAllNodesWithTag("offer_card").fetchSemanticsNodes().isNotEmpty()
        }
        DashboardRobot(rule).pullToRefresh()
        rule.waitForIdle()
        val dashboard = DashboardRobot(rule).assertVisible()
        dashboard.openFirstOffer()

        DetailsRobot(rule)
            .assertVisible()
            .assertPricesSection()

        Espresso.pressBack()
        DashboardRobot(rule).assertVisible().openSettings()

        SettingsRobot(rule)
            .assertVisible()
            .tapDark()

        Espresso.pressBack()
        DashboardRobot(rule).assertVisible()
    }
}
