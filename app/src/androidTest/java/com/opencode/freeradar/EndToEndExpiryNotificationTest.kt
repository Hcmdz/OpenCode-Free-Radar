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
import com.opencode.freeradar.data.local.RadarDatabase
import com.opencode.freeradar.data.repository.OfflineFirstOfferRepository
import com.opencode.freeradar.data.source.remote.SourceOffer
import com.opencode.freeradar.data.source.remote.toOffer
import com.opencode.freeradar.domain.error.Result
import com.opencode.freeradar.domain.error.SourceError
import com.opencode.freeradar.domain.model.ChangeType
import com.opencode.freeradar.domain.repository.FetchResult
import com.opencode.freeradar.domain.repository.OfferRepository
import com.opencode.freeradar.domain.repository.OfferSource
import com.opencode.freeradar.notifications.NotificationGate
import com.opencode.freeradar.notifications.OfferNotifier
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end: a free model that becomes paid must reach the user's notification
 * shade. Seeds the state through a real sync, flips the price, syncs again, and
 * checks the actual posted notification.
 */
@RunWith(AndroidJUnit4::class)
class EndToEndExpiryNotificationTest {

    @get:Rule
    val notificationPermission =
        GrantPermissionRule.grant(android.Manifest.permission.POST_NOTIFICATIONS)

    private val context: Context =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
            as Application

    private lateinit var database: RadarDatabase
    private lateinit var repository: OfferRepository
    private lateinit var notifier: OfferNotifier
    private lateinit var prefs: NotificationPrefs
    private lateinit var source: MutablePriceSource

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder<RadarDatabase>(
            InstrumentationRegistry.getInstrumentation().targetContext
        ).setDriver(AndroidSQLiteDriver()).build()
        source = MutablePriceSource(0.0)
        repository = OfflineFirstOfferRepository(
            database = database,
            sources = mapOf("s" to source),
            mappers = mapOf("s" to { dto: SourceOffer, now: Long -> dto.toOffer(now, "s") }),
            clock = Clock.fixed(Instant.ofEpochMilli(1_000_000L), ZoneOffset.UTC)
        )
        prefs = NotificationPrefs(context)
        notifier = OfferNotifier(context)
        clearNotifications()
    }

    @After
    fun tearDown() {
        clearNotifications()
        database.close()
    }

    private fun clearNotifications() {
        val manager = context.getSystemService(android.app.NotificationManager::class.java)
        manager.activeNotifications
            .filter { it.id == OfferNotifier.NOTIFICATION_ID }
            .forEach { manager.cancel(it.id) }
    }

    private fun posted(): StatusBarNotification? =
        context.getSystemService(android.app.NotificationManager::class.java)
            .activeNotifications
            .firstOrNull { it.id == OfferNotifier.NOTIFICATION_ID }

    private fun textOf(n: StatusBarNotification?): String =
        n?.notification?.extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

    @Test
    fun expiryReachesTheShade() = runBlocking {
        prefs.setEnabled(true)
        prefs.setExpiryEnabled(true)

        val gate = NotificationGate(repository, prefs, notifier)

        // 1. Seed: the model is free. Watermark is taken BEFORE the refresh so
        // the new events land above it (SyncWorker does the same).
        source.price = 0.0
        val w1 = gate.beforeSync()
        repository.refresh("s")
        gate.afterSync(w1)
        clearNotifications()

        // 2. It becomes paid.
        source.price = 2.0
        val w2 = gate.beforeSync()
        repository.refresh("s")
        gate.afterSync(w2)

        val n = posted()
        assertTrue("a paid-after-free model must post a notification", n != null)
        assertTrue(
            "the shade must announce the expiry, was: ${textOf(n)}",
            textOf(n).contains("1")
        )
    }

    private class MutablePriceSource(var price: Double) : OfferSource {
        override val id: String = "s"
        override suspend fun fetch(): Result<FetchResult, SourceError> =
            Result.Success(
                FetchResult(
                    listOf(
                        SourceOffer(
                            providerId = "s",
                            modelId = "m",
                            name = "M",
                            inputPrice = price,
                            outputPrice = price,
                            contextLength = 1000,
                            maxOutputTokens = null,
                            supportsTools = true,
                            supportsVision = false,
                            supportsStructuredOutput = false,
                            quota = null,
                            conditions = null,
                            officialUrl = null,
                            sourceUrl = null
                        )
                    ),
                    bodyHash = null
                )
            )
    }
}