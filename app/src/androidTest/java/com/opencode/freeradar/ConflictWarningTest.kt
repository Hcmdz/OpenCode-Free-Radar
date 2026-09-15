/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.opencode.freeradar.domain.model.Confidence
import com.opencode.freeradar.domain.model.FreeStatus
import com.opencode.freeradar.domain.model.Offer
import com.opencode.freeradar.ui.screens.details.DetailsScreen
import com.opencode.freeradar.ui.theme.AppThemePreview
import com.opencode.freeradar.ui.viewmodel.DetailsUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Story 2 acceptance: a TO_VERIFY offer shows the conflict warning. */
@RunWith(AndroidJUnit4::class)
class ConflictWarningTest {

    @get:Rule
    val rule = createComposeRule()

    private fun offer(confidence: Confidence) = Offer(
        remoteId = "nvidia-build/nemotron-3-ultra-550b-a55b",
        providerId = "nvidia",
        modelId = "nemotron-3-ultra-550b-a55b",
        name = "NVIDIA-Nemotron-3-Ultra-550B-A55B-NVFP4",
        inputPrice = null,
        outputPrice = null,
        freeStatus = FreeStatus.LIMITED,
        quota = "~40 RPM",
        quotaPeriod = null,
        temporary = false,
        conditions = null,
        contextLength = 1_048_576,
        maxOutputTokens = null,
        supportsTools = true,
        supportsVision = null,
        supportsStructuredOutput = null,
        openCodeCompatible = true,
        officialUrl = null,
        source = "nvidia-build",
        sourceUrl = "https://build.nvidia.com/nvidia/nemotron-3-ultra-550b-a55b",
        retrievedAt = 1_000L,
        verifiedAt = 1_000L,
        confidence = confidence,
        favorite = false
    )

    @Test
    fun toVerifyOfferShowsConflictWarning() {
        rule.setContent {
            AppThemePreview {
                DetailsScreen(
                    state = DetailsUiState(isLoading = false, offer = offer(Confidence.TO_VERIFY)),
                    onBack = {}
                )
            }
        }
        rule.onNodeWithText("Conflicting information between sources — verify before use.")
            .assertIsDisplayed()
    }

    @Test
    fun officialOfferShowsNoConflictWarning() {
        rule.setContent {
            AppThemePreview {
                DetailsScreen(
                    state = DetailsUiState(isLoading = false, offer = offer(Confidence.OFFICIAL)),
                    onBack = {}
                )
            }
        }
        rule.onNodeWithText("Conflicting information between sources — verify before use.")
            .assertDoesNotExist()
    }
}
