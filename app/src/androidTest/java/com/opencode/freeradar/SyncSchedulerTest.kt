/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.opencode.freeradar.data.local.AutoSync
import com.opencode.freeradar.worker.SyncScheduler
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The periodic request must carry the network and power axes in its WorkManager
 * constraint, not only in a runtime check: an unsatisfied constraint costs no
 * wakeup, whereas a runtime check wakes the device and skips every source.
 */
@RunWith(AndroidJUnit4::class)
class SyncSchedulerTest {

    private lateinit var context: Context
    private lateinit var work: WorkManager

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            WorkManagerTestInitHelper.ExecutorsMode.LEGACY_OVERRIDE_WITH_SYNCHRONOUS_EXECUTORS
        )
        work = WorkManager.getInstance(context)
    }

    private fun periodic(): WorkInfo? =
        work.getWorkInfosForUniqueWork(SyncScheduler.DAILY_WORK).get().firstOrNull()

    @Test
    fun wifiOnlyModesConstrainToUnmetered() {
        for (mode in listOf(AutoSync.WIFI, AutoSync.WIFI_BATTERY)) {
            SyncScheduler.schedule(context, mode, 8, force = true)
            assertEquals(
                "mode $mode must require an unmetered network",
                NetworkType.UNMETERED,
                periodic()?.constraints?.requiredNetworkType
            )
        }
    }

    @Test
    fun meteredModesConstrainToConnected() {
        for (mode in listOf(AutoSync.BATTERY, AutoSync.ALWAYS)) {
            SyncScheduler.schedule(context, mode, 8, force = true)
            assertEquals(
                "mode $mode must allow metered networks",
                NetworkType.CONNECTED,
                periodic()?.constraints?.requiredNetworkType
            )
        }
    }

    @Test
    fun chargingModesConstrainToCharging() {
        for (mode in listOf(AutoSync.WIFI_BATTERY, AutoSync.BATTERY)) {
            SyncScheduler.schedule(context, mode, 8, force = true)
            assertTrue(
                "mode $mode must require charging",
                periodic()?.constraints?.requiresCharging() == true
            )
        }
        for (mode in listOf(AutoSync.WIFI, AutoSync.ALWAYS)) {
            SyncScheduler.schedule(context, mode, 8, force = true)
            assertEquals(
                "mode $mode must not require charging",
                false,
                periodic()?.constraints?.requiresCharging()
            )
        }
    }

    @Test
    fun changingModeReplacesTheStoredConstraint() {
        SyncScheduler.schedule(context, AutoSync.WIFI, 8, force = true)
        assertEquals(NetworkType.UNMETERED, periodic()?.constraints?.requiredNetworkType)

        SyncScheduler.schedule(context, AutoSync.ALWAYS, 8, force = true)
        assertEquals(
            "a settings change must rewrite the stored constraint",
            NetworkType.CONNECTED,
            periodic()?.constraints?.requiredNetworkType
        )
    }

    @Test
    fun intervalIsAppliedAsThePeriod() {
        SyncScheduler.schedule(context, AutoSync.WIFI, 12, force = true)
        val periodMillis = periodic()?.periodicityInfo?.repeatIntervalMillis
        assertEquals(12, Duration.ofMillis(periodMillis!!).toHours().toInt())
    }

    @Test
    fun unforcedScheduleIsIdempotentAcrossRestarts() {
        SyncScheduler.schedule(context, AutoSync.WIFI, 8, force = true)
        val first = periodic()?.id

        // Two app starts in a row: an unconditional re-enqueue would push the
        // period forward every launch, and a user opening the app daily would
        // never get a sync at all.
        SyncScheduler.schedule(context, AutoSync.WIFI, 8, force = false)
        SyncScheduler.schedule(context, AutoSync.WIFI, 8, force = false)

        assertEquals("an unforced schedule must not replace existing work", first, periodic()?.id)
    }

    @Test
    fun unforcedScheduleStillEnqueuesWhenAbsent() {
        assertTrue(periodic() == null)
        SyncScheduler.schedule(context, AutoSync.WIFI, 8, force = false)
        assertTrue("a cold start with no scheduled work must enqueue", periodic() != null)
    }
}
