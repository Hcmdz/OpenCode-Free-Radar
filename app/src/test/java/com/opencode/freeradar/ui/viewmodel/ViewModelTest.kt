/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.viewmodel

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.opencode.freeradar.domain.error.RefreshResult
import com.opencode.freeradar.domain.error.SourceError
import com.opencode.freeradar.domain.model.ChangeEvent
import com.opencode.freeradar.domain.model.Confidence
import com.opencode.freeradar.domain.model.FreeStatus
import com.opencode.freeradar.domain.model.HealthState
import com.opencode.freeradar.domain.model.Offer
import com.opencode.freeradar.domain.model.SourceHealth
import com.opencode.freeradar.domain.model.SyncRun
import com.opencode.freeradar.domain.repository.OfferRepository
import com.opencode.freeradar.ui.model.OfferFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FakeOfferRepository : OfferRepository {
    val offersFlow = MutableStateFlow<List<Offer>>(emptyList())
    val healthFlow = MutableStateFlow<List<SourceHealth>>(emptyList())
    val lastRunFlow = MutableStateFlow<SyncRun?>(null)
    val history = MutableStateFlow<List<ChangeEvent>>(emptyList())
    var refreshResult: RefreshResult = RefreshResult.Ok
    var refreshCalls = 0
    var lastCompatibleOnly: Boolean? = null

    override fun observeOffers(compatibleOnly: Boolean): Flow<List<Offer>> {
        lastCompatibleOnly = compatibleOnly
        return offersFlow.map { list ->
            if (compatibleOnly) list.filter { it.openCodeCompatible } else list
        }
    }

    override fun observeOffer(remoteId: String): Flow<Offer?> =
        offersFlow.map { list -> list.firstOrNull { it.remoteId == remoteId } }

    override fun observeHistory(remoteId: String): Flow<List<ChangeEvent>> = history

    override fun observeHealth(): Flow<List<SourceHealth>> = healthFlow

    override fun observeLastRun(source: String): Flow<SyncRun?> = lastRunFlow

    override suspend fun refresh(source: String): RefreshResult {
        refreshCalls++
        return refreshResult
    }

    override suspend fun setFavorite(remoteId: String, favorite: Boolean) = Unit
}

fun sampleOffer(
    remoteId: String = "p/m",
    compatible: Boolean = true,
    status: FreeStatus = FreeStatus.FREE
) = Offer(
    remoteId = remoteId, providerId = "p", modelId = "m", name = "M",
    inputPrice = 0.0, outputPrice = 0.0, freeStatus = status,
    quota = null, quotaPeriod = null, temporary = false, conditions = null,
    contextLength = 1000, maxOutputTokens = null, supportsTools = true,
    supportsVision = false, supportsStructuredOutput = false,
    openCodeCompatible = compatible, officialUrl = null, source = "opencode-data",
    sourceUrl = null, retrievedAt = 1_000L, verifiedAt = 1_000L,
    confidence = Confidence.OFFICIAL, favorite = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @BeforeEach
    fun setup() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterEach
    fun teardown() = Dispatchers.resetMain()

    @Test
    fun `loading then list`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeOfferRepository()
        val vm = DashboardViewModel(repo)
        vm.state.test {
            assertThat(awaitItem().isLoading).isTrue()
            repo.offersFlow.value = listOf(sampleOffer())
            testScheduler.advanceUntilIdle()
            val loaded = awaitItem()
            assertThat(loaded.isLoading).isFalse()
            assertThat(loaded.offers.map { it.remoteId }).isEqualTo(listOf("p/m"))
        }
    }

    @Test
    fun `default filter shows free only`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeOfferRepository()
        repo.offersFlow.value = listOf(
            sampleOffer(),
            sampleOffer("p/paid", status = FreeStatus.PAID)
        )
        val vm = DashboardViewModel(repo)
        vm.state.test {
            awaitItem()
            testScheduler.advanceUntilIdle()
            val free = awaitItem()
            assertThat(free.filter).isEqualTo(OfferFilter.FREE)
            assertThat(free.offers.map { it.remoteId }).isEqualTo(listOf("p/m"))
        }
    }

    @Test
    fun `selecting ALL shows everything`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeOfferRepository()
        repo.offersFlow.value = listOf(
            sampleOffer(),
            sampleOffer("p/paid", status = FreeStatus.PAID)
        )
        val vm = DashboardViewModel(repo)
        vm.state.test {
            awaitItem()
            testScheduler.advanceUntilIdle()
            awaitItem()
            vm.onAction(DashboardAction.SelectFilter(OfferFilter.ALL))
            testScheduler.advanceUntilIdle()
            var all = awaitItem()
            while (all.filter != OfferFilter.ALL) all = awaitItem()
            assertThat(all.offers.map { it.remoteId }).isEqualTo(listOf("p/m", "p/paid"))
        }
    }

    @Test
    fun `selecting COMPATIBLE queries compatible only`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeOfferRepository()
        repo.offersFlow.value = listOf(sampleOffer(), sampleOffer("p/x", compatible = false))
        val vm = DashboardViewModel(repo)
        vm.state.test {
            awaitItem()
            testScheduler.advanceUntilIdle()
            awaitItem()
            vm.onAction(DashboardAction.SelectFilter(OfferFilter.COMPATIBLE))
            testScheduler.advanceUntilIdle()
            var filtered = awaitItem()
            while (filtered.filter != OfferFilter.COMPATIBLE) filtered = awaitItem()
            assertThat(repo.lastCompatibleOnly).isEqualTo(true)
            assertThat(filtered.offers.map { it.remoteId }).isEqualTo(listOf("p/m"))
            assertThat(filtered.filter).isEqualTo(OfferFilter.COMPATIBLE)
        }
    }

    @Test
    fun `selecting FREE_COMPATIBLE intersects both`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeOfferRepository()
        repo.offersFlow.value = listOf(
            sampleOffer(),
            sampleOffer("p/paid", status = FreeStatus.PAID),
            sampleOffer("p/nc", compatible = false)
        )
        val vm = DashboardViewModel(repo)
        vm.state.test {
            awaitItem()
            testScheduler.advanceUntilIdle()
            awaitItem()
            vm.onAction(DashboardAction.SelectFilter(OfferFilter.FREE_COMPATIBLE))
            testScheduler.advanceUntilIdle()
            var filtered = awaitItem()
            while (filtered.filter != OfferFilter.FREE_COMPATIBLE) filtered = awaitItem()
            assertThat(repo.lastCompatibleOnly).isEqualTo(true)
            assertThat(filtered.offers.map { it.remoteId }).isEqualTo(listOf("p/m"))
        }
    }

    @Test
    fun `failed refresh surfaces error and dismiss clears`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeOfferRepository()
        repo.refreshResult = RefreshResult.Failed(SourceError.Timeout)
        val vm = DashboardViewModel(repo)
        vm.state.test {
            awaitItem()
            testScheduler.advanceUntilIdle()
            awaitItem()
            vm.onAction(DashboardAction.Refresh)
            testScheduler.advanceUntilIdle()
            val errored = awaitItem()
            assertThat(errored.error).isNotNull()
            assertThat(repo.refreshCalls).isEqualTo(1)
            vm.onAction(DashboardAction.DismissError)
            testScheduler.advanceUntilIdle()
            assertThat(awaitItem().error).isNull()
        }
    }

    @Test
    fun `offline reflects unavailable source health`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeOfferRepository()
        val vm = DashboardViewModel(repo)
        vm.state.test {
            awaitItem()
            repo.healthFlow.value = listOf(
                SourceHealth("opencode-data", HealthState.UNAVAILABLE, 2_000L)
            )
            testScheduler.advanceUntilIdle()
            assertThat(awaitItem().offline).isTrue()
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DetailsViewModelTest {

    @BeforeEach
    fun setup() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterEach
    fun teardown() = Dispatchers.resetMain()

    @Test
    fun `offer plus history combined`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeOfferRepository()
        repo.offersFlow.value = listOf(sampleOffer())
        val vm = DetailsViewModel("p/m", repo)
        vm.state.test {
            assertThat(awaitItem().isLoading).isTrue()
            testScheduler.advanceUntilIdle()
            val loaded = awaitItem()
            assertThat(loaded.offer?.remoteId).isEqualTo("p/m")
            assertThat(loaded.history).isEqualTo(emptyList())
            assertThat(loaded.isLoading).isFalse()
        }
    }
}
