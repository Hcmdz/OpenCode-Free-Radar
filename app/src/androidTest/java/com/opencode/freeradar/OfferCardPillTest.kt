/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.opencode.freeradar.domain.model.FreeStatus
import com.opencode.freeradar.ui.components.OfferCard
import com.opencode.freeradar.ui.model.OfferUi
import com.opencode.freeradar.ui.theme.AppThemePreview
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Local/self-hosted rows carry a Local pill; hosted rows never do. */
@RunWith(AndroidJUnit4::class)
class OfferCardPillTest {

    @get:Rule
    val rule = createComposeRule()

    private fun card(providerId: String, isLocal: Boolean) {
        rule.setContent {
            AppThemePreview {
                OfferCard(
                    offer = OfferUi(
                        remoteId = "$providerId/m",
                        name = "M",
                        providerId = providerId,
                        freeStatus = FreeStatus.FREE,
                        contextLength = null,
                        verifiedAt = 1_000L,
                        source = "litellm",
                        isLocal = isLocal
                    ),
                    onClick = {},
                    onToggleFavorite = {}
                )
            }
        }
    }

    @Test
    fun localOfferShowsLocalPill() {
        card(providerId = "ollama", isLocal = true)
        rule.onNodeWithText("Local").assertIsDisplayed()
    }

    @Test
    fun hostedOfferHidesLocalPill() {
        card(providerId = "gemini", isLocal = false)
        rule.onNodeWithText("Local").assertDoesNotExist()
    }
}
