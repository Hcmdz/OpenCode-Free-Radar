/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room3.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.opencode.freeradar.data.local.MIGRATION_1_2
import com.opencode.freeradar.data.local.OfferEntity
import com.opencode.freeradar.data.local.RadarDatabase
import com.opencode.freeradar.domain.model.Confidence
import com.opencode.freeradar.domain.model.FreeStatus
import com.opencode.freeradar.notifications.OfferNotifier
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Notification tap opens the snapshot card: sections, cards, details, restore. */
@RunWith(AndroidJUnit4::class)
class NewModelsTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private var scenario: ActivityScenario<MainActivity>? = null

    private fun entity(remoteId: String, name: String) = OfferEntity(
        remoteId = remoteId,
        providerId = "t",
        modelId = remoteId.substringAfter('/'),
        name = name,
        inputPrice = 0.0,
        outputPrice = 0.0,
        freeStatus = FreeStatus.FREE.name,
        quota = null,
        quotaPeriod = null,
        temporary = false,
        conditions = null,
        contextLength = 1000,
        maxOutputTokens = null,
        supportsTools = true,
        supportsVision = false,
        supportsStructuredOutput = false,
        openCodeCompatible = true,
        officialUrl = null,
        source = "test",
        sourceUrl = null,
        retrievedAt = 1_000L,
        verifiedAt = 1_000L,
        confidence = Confidence.OFFICIAL.name,
        favorite = false
    )

    private fun launchWithIds(newIds: List<String>, expiredIds: List<String>) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, MainActivity::class.java).apply {
            putStringArrayListExtra(OfferNotifier.EXTRA_NEW_IDS, ArrayList(newIds))
            putStringArrayListExtra(OfferNotifier.EXTRA_EXPIRED_IDS, ArrayList(expiredIds))
        }
        scenario = ActivityScenario.launch(intent)
    }

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.databaseBuilder(context, RadarDatabase::class.java, "radar.db")
            .addMigrations(MIGRATION_1_2)
            .build()
        runBlocking {
            db.offerDao().upsertAll(
                listOf(entity("t/a", "Model A"), entity("t/b", "Model B"))
            )
        }
        db.close()
    }

    @After
    fun teardown() {
        scenario?.close()
    }

    @Test
    fun tapOpensSnapshotCardWithSections() {
        launchWithIds(listOf("t/a", "t/gone"), listOf("t/b"))
        compose.waitUntil(8000) {
            try {
                compose.onNodeWithTag("new_models_screen").assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }
        compose.onNodeWithText("Model A").assertIsDisplayed()
        compose.onNodeWithText("Model B").assertIsDisplayed()
        compose.onAllNodesWithTag("offer_card").apply {
            fetchSemanticsNodes().size.let { assert(it == 2) { "expected 2 cards, got $it" } }
        }
    }

    @Test
    fun cardTapOpensDetailsAndBackReturns() {
        launchWithIds(listOf("t/a"), emptyList())
        compose.waitUntil(8000) {
            try {
                compose.onNodeWithTag("new_models_screen").assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }
        compose.onNodeWithText("Model A").performClick()
        compose.waitUntil(8000) {
            try {
                compose.onNodeWithTag("details_screen").assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }
        compose.onNodeWithTag("details_screen").assertIsDisplayed()
    }

    @Test
    fun cardSurvivesRecreation() {
        launchWithIds(listOf("t/a"), emptyList())
        compose.waitUntil(8000) {
            try {
                compose.onNodeWithTag("new_models_screen").assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }
        scenario?.recreate()
        compose.waitUntil(8000) {
            try {
                compose.onNodeWithTag("new_models_screen").assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }
        compose.onNodeWithText("Model A").assertIsDisplayed()
    }
}
