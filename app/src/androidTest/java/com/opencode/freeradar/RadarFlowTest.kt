/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar

import android.Manifest
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.rule.GrantPermissionRule
import com.opencode.freeradar.robots.DashboardRobot
import com.opencode.freeradar.robots.DetailsRobot
import com.opencode.freeradar.robots.SettingsRobot
import org.junit.Rule
import org.junit.Test

class RadarFlowTest {

    // order 1 runs outermost: the dashboard asks for POST_NOTIFICATIONS on its
    // first composition, and that system dialog steals the compose hierarchy
    // from the test. Pre-granting means the app finds it already granted and
    // never shows the dialog.
    @get:Rule(order = 1)
    val notificationPermission =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule(order = 0)
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

        val details = DetailsRobot(rule)
            .assertVisible()
            .assertPricesSection()
        val firstTitle = details.readTitle()

        // Regression: the second Details must not reuse the first offerId
        // (shared ViewModelStore across Nav3 entries).
        Espresso.pressBack()
        DashboardRobot(rule).assertVisible().openSecondOffer()

        val secondTitle = DetailsRobot(rule).assertVisible().readTitle()
        assert(secondTitle != firstTitle) {
            "Second details showed the first offer: $secondTitle"
        }

        Espresso.pressBack()
        DashboardRobot(rule).assertVisible().openSettings()

        SettingsRobot(rule)
            .assertVisible()
            .openSection("Appearance")
            .tapDark()

        Espresso.pressBack()
        DashboardRobot(rule).assertVisible()
    }
}
