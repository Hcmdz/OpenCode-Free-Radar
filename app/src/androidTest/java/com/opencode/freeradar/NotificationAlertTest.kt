/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar

import android.app.Application
import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.opencode.freeradar.data.local.NotificationPrefs
import com.opencode.freeradar.domain.error.RefreshResult
import com.opencode.freeradar.domain.model.ChangeEvent
import com.opencode.freeradar.domain.model.ChangeType
import com.opencode.freeradar.domain.model.Confidence
import com.opencode.freeradar.domain.model.FreeStatus
import com.opencode.freeradar.domain.model.Offer
import com.opencode.freeradar.domain.model.SourceHealth
import com.opencode.freeradar.domain.model.SyncRun
import com.opencode.freeradar.domain.repository.OfferRepository
import com.opencode.freeradar.notifications.NotificationGate
import com.opencode.freeradar.notifications.OfferNotifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The product outcome: a worker run that discovers a free offer must reach the
 * user's notification shade. The pure summarizers are unit-tested; this covers
 * the glue the background path depends on, against the real notifier.
 */
@RunWith(AndroidJUnit4::class)
class NotificationAlertTest {

    @get:Rule
    val notificationPermission =
        GrantPermissionRule.grant(android.Manifest.permission.POST_NOTIFICATIONS)

    private val context: Context =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
            as Application

    private lateinit var notifier: OfferNotifier
    private lateinit var prefs: NotificationPrefs
    private val repository = StubRepository()

    @Before
    fun setUp() {
        notifier = OfferNotifier(context)
        prefs = NotificationPrefs(context)
        // Clear the shade first: a leftover from another class would pass the
        // negative assertion below for the wrong reason.
        clearShade()
    }

    @After
    fun tearDown() = clearShade()

    private fun clearShade() {
        val manager = context.getSystemService(android.app.NotificationManager::class.java)
        manager.activeNotifications
            .filter { it.id == OfferNotifier.NOTIFICATION_ID }
            .forEach { manager.cancel(it.id) }
    }

    private fun posted(): StatusBarNotification? =
        context.getSystemService(android.app.NotificationManager::class.java)
            .activeNotifications
            .firstOrNull { it.id == OfferNotifier.NOTIFICATION_ID }

    private fun textOf(notification: StatusBarNotification?): String =
        notification?.notification?.extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: ""

    private fun offer(id: String, name: String) = Offer(
        remoteId = id,
        providerId = "opencode-data",
        modelId = id,
        name = name,
        inputPrice = 0.0,
        outputPrice = 0.0,
        freeStatus = FreeStatus.FREE,
        quota = null,
        quotaPeriod = null,
        temporary = false,
        conditions = null,
        contextLength = 8192,
        maxOutputTokens = 4096,
        supportsTools = null,
        supportsVision = null,
        supportsStructuredOutput = null,
        openCodeCompatible = true,
        officialUrl = null,
        source = "opencode-data",
        sourceUrl = null,
        retrievedAt = 0L,
        verifiedAt = 0L,
        confidence = Confidence.OFFICIAL,
        favorite = false,
    )

    private fun gate() = NotificationGate(repository, prefs, notifier)

    @Test
    fun newFreeOfferReachesTheShade() = runBlocking {
        prefs.setEnabled(true)
        repository.offers = listOf(offer("opencode-data/muse-flash", "Muse Flash"))
        repository.events = listOf(
            ChangeEvent(
                offerRemoteId = "opencode-data/muse-flash",
                type = ChangeType.NEW_MODEL,
                beforeJson = null,
                afterJson = FreeStatus.FREE.name,
                createdAt = 0L,
            )
        )

        val watermark = gate().beforeSync()
        gate().afterSync(watermark)

        val notification = posted()
        assertTrue("a new free offer must post a notification", notification != null)
        assertTrue(
            "the summary must count the new free offer, was: ${textOf(notification)}",
            textOf(notification).contains("1")
        )
    }

    @Test
    fun disabledAlertsPostNothing() = runBlocking {
        prefs.setEnabled(false)
        repository.offers = listOf(offer("opencode-data/muse-flash", "Muse Flash"))
        repository.events = listOf(
            ChangeEvent(
                offerRemoteId = "opencode-data/muse-flash",
                type = ChangeType.BECAME_FREE,
                beforeJson = FreeStatus.PAID.name,
                afterJson = FreeStatus.FREE.name,
                createdAt = 0L,
            )
        )

        val watermark = gate().beforeSync()
        gate().afterSync(watermark)

        assertNull("alerts off must post nothing", posted())
    }

    @Test
    fun noQualifyingEventPostsNothing() = runBlocking {
        prefs.setEnabled(true)
        repository.offers = listOf(offer("opencode-data/muse-flash", "Muse Flash"))
        // A price edit the alert deliberately ignores: no new free, no expiry.
        repository.events = listOf(
            ChangeEvent(
                offerRemoteId = "opencode-data/muse-flash",
                type = ChangeType.PRICE_CHANGED,
                beforeJson = null,
                afterJson = FreeStatus.FREE.name,
                createdAt = 0L,
            )
        )

        val watermark = gate().beforeSync()
        gate().afterSync(watermark)

        assertNull("an ignored event type must not alert", posted())
    }

    @Test
    fun expiryAlertsOnItsOwn() = runBlocking {
        prefs.setEnabled(true)
        repository.offers = listOf(offer("opencode-data/muse-flash", "Muse Flash"))
        repository.events = listOf(
            ChangeEvent(
                offerRemoteId = "opencode-data/muse-flash",
                type = ChangeType.FREE_EXPIRED,
                beforeJson = FreeStatus.FREE.name,
                afterJson = FreeStatus.PAID.name,
                createdAt = 0L,
            )
        )

        val watermark = gate().beforeSync()
        gate().afterSync(watermark)

        assertTrue("an expired free offer must alert", posted() != null)
    }

    @Test
    fun watermarkIsTakenBeforeTheEvents() = runBlocking {
        prefs.setEnabled(true)
        repository.latestId = 42L
        assertEquals(42L, gate().beforeSync())
    }

    private class StubRepository : OfferRepository {
        var offers: List<Offer> = emptyList()
        var events: List<ChangeEvent> = emptyList()
        var latestId: Long = 0L

        override fun observeOffers(compatibleOnly: Boolean): Flow<List<Offer>> = flowOf(offers)
        override fun observeOffer(remoteId: String): Flow<Offer?> = emptyFlow()
        override fun observeHistory(remoteId: String): Flow<List<ChangeEvent>> = emptyFlow()
        override fun observeHealth(): Flow<List<SourceHealth>> = emptyFlow()
        override fun observeLastRun(source: String): Flow<SyncRun?> = emptyFlow()
        override fun observeLatestRun(): Flow<SyncRun?> = emptyFlow()
        override suspend fun refresh(source: String): RefreshResult = RefreshResult.Ok
        override suspend fun refreshAll(): RefreshResult = RefreshResult.Ok
        override suspend fun setFavorite(remoteId: String, favorite: Boolean) = Unit
        override suspend fun eventsSince(sinceId: Long, types: List<String>): List<ChangeEvent> = events
        override suspend fun latestEventId(): Long = latestId
    }
}
