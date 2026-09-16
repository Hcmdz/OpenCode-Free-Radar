/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.viewmodel

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.opencode.freeradar.domain.error.RefreshResult
import com.opencode.freeradar.domain.model.ChangeEvent
import com.opencode.freeradar.domain.model.Offer
import com.opencode.freeradar.domain.model.SourceHealth
import com.opencode.freeradar.domain.model.SyncRun
import com.opencode.freeradar.domain.repository.OfferRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** One gate per refreshAll call, so overlapping refreshes can be steered. */
private class GatedRefreshRepository : OfferRepository {
    val gates = mutableListOf<CompletableDeferred<Unit>>()
    var refreshCalls = 0
    private val empty = MutableStateFlow<List<Offer>>(emptyList())
    private val emptyHealth = MutableStateFlow<List<SourceHealth>>(emptyList())
    private val noRun = MutableStateFlow<SyncRun?>(null)
    private val noHistory = MutableStateFlow<List<ChangeEvent>>(emptyList())

    override fun observeOffers(compatibleOnly: Boolean): Flow<List<Offer>> = empty
    override fun observeOffer(remoteId: String): Flow<Offer?> =
        MutableStateFlow<Offer?>(null)

    override fun observeHistory(remoteId: String): Flow<List<ChangeEvent>> = noHistory
    override fun observeHealth(): Flow<List<SourceHealth>> = emptyHealth
    override fun observeLastRun(source: String): Flow<SyncRun?> = noRun
    override fun observeLatestRun(): Flow<SyncRun?> = noRun
    override suspend fun refresh(source: String): RefreshResult = RefreshResult.Ok

    override suspend fun refreshAll(force: Boolean): RefreshResult {
        val gate = CompletableDeferred<Unit>()
        synchronized(gates) { gates += gate }
        refreshCalls++
        gate.await()
        return RefreshResult.Ok
    }

    override suspend fun setFavorite(remoteId: String, favorite: Boolean) = Unit
    override suspend fun eventsSince(sinceId: Long, types: List<String>): List<ChangeEvent> =
        emptyList()

    override suspend fun latestEventId(): Long = 0L
}

@OptIn(ExperimentalCoroutinesApi::class)
class RefreshCoalescingTest {

    @BeforeEach
    fun setup() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterEach
    fun teardown() = Dispatchers.resetMain()

    @Test
    fun `second refresh while in flight is dropped`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = GatedRefreshRepository()
        val gate = NoopSyncNotifier()
        val vm = DashboardViewModel(repo, gate)
        vm.state.test {
            awaitItem()
            testScheduler.advanceUntilIdle()
            awaitItem()
            vm.onAction(DashboardAction.Refresh)
            testScheduler.runCurrent()
            assertThat(awaitItem().isRefreshing).isTrue()
            // Second pull while the spinner is already up: must not start
            // a second sync (double watermark, double afterSync, early flag).
            vm.onAction(DashboardAction.Refresh)
            testScheduler.runCurrent()
            repo.gates[0].complete(Unit)
            testScheduler.advanceUntilIdle()
            assertThat(awaitItem().isRefreshing).isFalse()
            assertThat(repo.refreshCalls).isEqualTo(1)
            assertThat(gate.afterSyncCalls).isEqualTo(1)
        }
    }
}
