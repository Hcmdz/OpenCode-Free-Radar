/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.viewmodel

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.opencode.freeradar.data.local.AutoSync
import com.opencode.freeradar.data.local.SyncSettings
import com.opencode.freeradar.util.NetworkMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private class FakeSyncSettings(
    var mode: AutoSync = AutoSync.ALWAYS,
    var warnOnMetered: Boolean = true,
    var firstSyncDone: Boolean = true,
) : SyncSettings {
    val wifiOnly: Boolean get() = mode.wifiOnly
    override suspend fun autoSync(): AutoSync = mode
    override suspend fun setAutoSync(mode: AutoSync) {
        this.mode = mode
    }
    override suspend fun autoSyncIntervalHours(): Int = AutoSync.DEFAULT_INTERVAL_HOURS
    override suspend fun setAutoSyncIntervalHours(hours: Int) = Unit
    override suspend fun warnOnMetered(): Boolean = warnOnMetered
    override suspend fun setWarnOnMetered(enabled: Boolean) {
        warnOnMetered = enabled
    }
    override suspend fun firstSyncDone(): Boolean = firstSyncDone
    override suspend fun setFirstSyncDone() {
        firstSyncDone = true
    }
}

private class FakeNetwork(var metered: Boolean = false) : NetworkMonitor {
    override fun isMetered(): Boolean = metered
}

@OptIn(ExperimentalCoroutinesApi::class)
class MeteredWarningTest {

    @BeforeEach
    fun setup() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterEach
    fun teardown() = Dispatchers.resetMain()

    private fun vm(
        repo: FakeOfferRepository = FakeOfferRepository(),
        prefs: FakeSyncSettings = FakeSyncSettings(),
        network: FakeNetwork = FakeNetwork(),
    ) = Triple(repo, prefs, DashboardViewModel(repo, NoopSyncNotifier(), prefs, network))

    @Test
    fun `metered pull raises the warning instead of fetching`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val (repo, _, vm) = vm(network = FakeNetwork(metered = true))
        vm.state.test {
            awaitItem()
            vm.onAction(DashboardAction.Refresh)
            assertThat(awaitItem().meteredWarning).isTrue()
            assertThat(repo.refreshCalls).isEqualTo(0)
        }
    }

    @Test
    fun `sync-once fetches and clears the warning`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val (repo, _, vm) = vm(network = FakeNetwork(metered = true))
        vm.state.test {
            awaitItem()
            vm.onAction(DashboardAction.Refresh)
            assertThat(awaitItem().meteredWarning).isTrue()
            vm.onAction(DashboardAction.MeteredSyncOnce)
            val done = awaitItem()
            assertThat(done.meteredWarning).isFalse()
            assertThat(repo.refreshCalls).isEqualTo(1)
        }
    }

    @Test
    fun `never-warn persists and fetches`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val (repo, prefs, vm) = vm(network = FakeNetwork(metered = true))
        vm.state.test {
            awaitItem()
            vm.onAction(DashboardAction.Refresh)
            awaitItem()
            vm.onAction(DashboardAction.MeteredNeverWarn)
            testScheduler.advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
            assertThat(repo.refreshCalls).isEqualTo(1)
        }
        assertThat(prefs.warnOnMetered).isFalse()
    }

    @Test
    fun `later dismisses without fetching`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val (repo, _, vm) = vm(network = FakeNetwork(metered = true))
        vm.state.test {
            awaitItem()
            vm.onAction(DashboardAction.Refresh)
            assertThat(awaitItem().meteredWarning).isTrue()
            vm.onAction(DashboardAction.MeteredLater)
            assertThat(awaitItem().meteredWarning).isFalse()
            assertThat(repo.refreshCalls).isEqualTo(0)
        }
    }

    @Test
    fun `unmetered pull fetches directly`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val (repo, _, vm) = vm(network = FakeNetwork(metered = false))
        vm.state.test {
            awaitItem()
            vm.onAction(DashboardAction.Refresh)
            testScheduler.advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
            assertThat(repo.refreshCalls).isEqualTo(1)
        }
    }

    @Test
    fun `first launch with empty base auto-fetches once`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val prefs = FakeSyncSettings(firstSyncDone = false)
        val repo = FakeOfferRepository()
        DashboardViewModel(repo, NoopSyncNotifier(), prefs, FakeNetwork())
        testScheduler.advanceUntilIdle()
        assertThat(repo.refreshCalls).isEqualTo(1)
        assertThat(prefs.firstSyncDone).isTrue()
    }

    @Test
    fun `first launch with cached base sets the flag without fetching`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val prefs = FakeSyncSettings(firstSyncDone = false)
        val repo = FakeOfferRepository()
        repo.offersFlow.value = listOf(sampleOffer())
        DashboardViewModel(repo, NoopSyncNotifier(), prefs, FakeNetwork())
        testScheduler.advanceUntilIdle()
        assertThat(repo.refreshCalls).isEqualTo(0)
        assertThat(prefs.firstSyncDone).isTrue()
    }
}
