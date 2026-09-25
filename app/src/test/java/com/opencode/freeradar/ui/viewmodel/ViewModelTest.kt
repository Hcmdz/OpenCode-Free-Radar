/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.viewmodel

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isEmpty
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
import com.opencode.freeradar.data.local.DashboardFilterPrefs
import com.opencode.freeradar.notifications.SyncNotifier
import com.opencode.freeradar.ui.model.OfferFilter
import com.opencode.freeradar.ui.model.OfferSort
import com.opencode.freeradar.ui.model.SourceFilter
import kotlinx.coroutines.CompletableDeferred
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
    var refreshGate: CompletableDeferred<Unit>? = null
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

    override fun observeLatestRun(): Flow<SyncRun?> = lastRunFlow

    override suspend fun refresh(source: String): RefreshResult {
        refreshCalls++
        return refreshResult
    }

    override suspend fun refreshAll(): RefreshResult {
        refreshCalls++
        refreshGate?.await()
        return refreshResult
    }

    var lastFavorite: Pair<String, Boolean>? = null

    override suspend fun setFavorite(remoteId: String, favorite: Boolean) {
        lastFavorite = remoteId to favorite
    }

    override suspend fun eventsSince(sinceId: Long, types: List<String>): List<ChangeEvent> =
        emptyList()

    override suspend fun latestEventId(): Long = 0L
}

class NoopSyncNotifier : SyncNotifier {
    var afterSyncCalls = 0

    override suspend fun beforeSync(): Long = 0L

    override suspend fun afterSync(watermark: Long) {
        afterSyncCalls++
    }
}

class FakeDashboardFilterPrefs(
    filterName: String? = null,
    sourceName: String? = null,
    sortName: String? = null,
    showLocal: Boolean = false,
    recents: List<String> = emptyList()
) : DashboardFilterPrefs {
    val filterFlow = MutableStateFlow(filterName)
    val sourceFlow = MutableStateFlow(sourceName)
    val sortFlow = MutableStateFlow(sortName)
    val localFlow = MutableStateFlow(showLocal)
    val recentsFlow = MutableStateFlow(recents)
    var saved: SavedFilters? = null

    data class SavedFilters(
        val filter: String,
        val source: String,
        val sort: String,
        val showLocal: Boolean,
        val recents: List<String>
    )

    override val filterName: Flow<String?> = filterFlow
    override val sourceName: Flow<String?> = sourceFlow
    override val sortName: Flow<String?> = sortFlow
    override val showLocal: Flow<Boolean> = localFlow
    override val recentQueries: Flow<List<String>> = recentsFlow

    override suspend fun save(
        filterName: String,
        sourceName: String,
        sortName: String,
        showLocal: Boolean,
        recentQueries: List<String>
    ) {
        saved = SavedFilters(filterName, sourceName, sortName, showLocal, recentQueries)
    }
}

fun sampleOffer(
    remoteId: String = "p/m",
    compatible: Boolean = true,
    status: FreeStatus = FreeStatus.FREE,
    source: String = "opencode-data",
    name: String = "M",
    providerId: String = "p",
    confidence: Confidence = Confidence.OFFICIAL
) = Offer(
    remoteId = remoteId, providerId = providerId, modelId = "m", name = name,
    inputPrice = 0.0, outputPrice = 0.0, freeStatus = status,
    quota = null, quotaPeriod = null, temporary = false, conditions = null,
    contextLength = 1000, maxOutputTokens = null, supportsTools = true,
    supportsVision = false, supportsStructuredOutput = false,
    openCodeCompatible = compatible, officialUrl = null, source = source,
    sourceUrl = null, retrievedAt = 1_000L, verifiedAt = 1_000L,
    confidence = confidence, favorite = false
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
        val vm = DashboardViewModel(repo, NoopSyncNotifier())
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
        val vm = DashboardViewModel(repo, NoopSyncNotifier())
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
        val vm = DashboardViewModel(repo, NoopSyncNotifier())
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
        val vm = DashboardViewModel(repo, NoopSyncNotifier())
        vm.state.test {
            awaitItem()
            testScheduler.advanceUntilIdle()
            awaitItem()
            vm.onAction(DashboardAction.SelectFilter(OfferFilter.COMPATIBLE))
            testScheduler.advanceUntilIdle()
            var filtered = awaitItem()
            while (filtered.filter != OfferFilter.COMPATIBLE) filtered = awaitItem()
            // Compatible filtering now happens in-memory over the full list.
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
        val vm = DashboardViewModel(repo, NoopSyncNotifier())
        vm.state.test {
            awaitItem()
            testScheduler.advanceUntilIdle()
            awaitItem()
            vm.onAction(DashboardAction.SelectFilter(OfferFilter.FREE_COMPATIBLE))
            testScheduler.advanceUntilIdle()
            var filtered = awaitItem()
            while (filtered.filter != OfferFilter.FREE_COMPATIBLE) filtered = awaitItem()
            assertThat(filtered.offers.map { it.remoteId }).isEqualTo(listOf("p/m"))
        }
    }

    @Test
    fun `clear search and filters resets all four selections`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeOfferRepository()
        val vm = DashboardViewModel(repo, NoopSyncNotifier())
        vm.state.test {
            awaitItem()
            vm.onAction(DashboardAction.SelectFilter(OfferFilter.ALL))
            vm.onAction(DashboardAction.SelectSource(SourceFilter.OPENROUTER))
            vm.onAction(DashboardAction.SelectSort(OfferSort.NAME))
            vm.onAction(DashboardAction.Search("zzz"))
            testScheduler.advanceUntilIdle()
            var dirty = awaitItem()
            while (dirty.query != "zzz") dirty = awaitItem()
            vm.onAction(DashboardAction.ClearSearchAndFilters)
            testScheduler.advanceUntilIdle()
            var cleared = awaitItem()
            while (cleared.query != "" || cleared.filter != OfferFilter.FREE) cleared = awaitItem()
            assertThat(cleared.filter).isEqualTo(OfferFilter.FREE)
            assertThat(cleared.sourceFilter).isEqualTo(SourceFilter.ALL_SOURCES)
            assertThat(cleared.sort).isEqualTo(OfferSort.RECENT)
            assertThat(cleared.query).isEqualTo("")
            assertThat(cleared.recentSearches).isEmpty()
        }
    }

    @Test
    fun `selecting OPENROUTER source shows openrouter rows`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeOfferRepository()
        repo.offersFlow.value = listOf(
            sampleOffer(),
            sampleOffer("or/m:free", source = "openrouter")
        )
        val vm = DashboardViewModel(repo, NoopSyncNotifier())
        vm.state.test {
            awaitItem()
            testScheduler.advanceUntilIdle()
            awaitItem()
            vm.onAction(DashboardAction.SelectSource(SourceFilter.OPENROUTER))
            testScheduler.advanceUntilIdle()
            var filtered = awaitItem()
            while (filtered.sourceFilter != SourceFilter.OPENROUTER) filtered = awaitItem()
            assertThat(filtered.offers.map { it.remoteId }).isEqualTo(listOf("or/m:free"))
        }
    }

    @Test
    fun `selecting OPENCODE source shows provider rows, not pipeline rows`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeOfferRepository()
        repo.offersFlow.value = listOf(
            sampleOffer("opencode/m", providerId = "opencode"),
            sampleOffer("or/m", source = "opencode-data")
        )
        val vm = DashboardViewModel(repo, NoopSyncNotifier())
        vm.state.test {
            awaitItem()
            testScheduler.advanceUntilIdle()
            awaitItem()
            vm.onAction(DashboardAction.SelectSource(SourceFilter.OPENCODE))
            testScheduler.advanceUntilIdle()
            var filtered = awaitItem()
            while (filtered.sourceFilter != SourceFilter.OPENCODE) filtered = awaitItem()
            assertThat(filtered.offers.map { it.remoteId }).isEqualTo(listOf("opencode/m"))
        }
    }

    @Test
    fun `selecting OPENCODE source hides other sources`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeOfferRepository()
        repo.offersFlow.value = listOf(
            sampleOffer("opencode/m", providerId = "opencode"),
            sampleOffer("other/m", source = "other-source")
        )
        val vm = DashboardViewModel(repo, NoopSyncNotifier())
        vm.state.test {
            awaitItem()
            testScheduler.advanceUntilIdle()
            awaitItem()
            vm.onAction(DashboardAction.SelectSource(SourceFilter.OPENCODE))
            testScheduler.advanceUntilIdle()
            var filtered = awaitItem()
            while (filtered.sourceFilter != SourceFilter.OPENCODE) filtered = awaitItem()
            assertThat(filtered.offers.map { it.remoteId }).isEqualTo(listOf("opencode/m"))
        }
    }

    @Test
    fun `source and status filters intersect`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeOfferRepository()
        repo.offersFlow.value = listOf(
            sampleOffer("opencode/m", providerId = "opencode"),
            sampleOffer("opencode/paid", status = FreeStatus.PAID, providerId = "opencode"),
            sampleOffer("other/m", source = "other-source")
        )
        val vm = DashboardViewModel(repo, NoopSyncNotifier())
        vm.state.test {
            awaitItem()
            testScheduler.advanceUntilIdle()
            awaitItem()
            vm.onAction(DashboardAction.SelectFilter(OfferFilter.ALL))
            vm.onAction(DashboardAction.SelectSource(SourceFilter.OPENCODE))
            testScheduler.advanceUntilIdle()
            var filtered = awaitItem()
            while (filtered.sourceFilter != SourceFilter.OPENCODE) filtered = awaitItem()
            assertThat(filtered.offers.map { it.remoteId }).isEqualTo(listOf("opencode/m", "opencode/paid"))
        }
    }

    @Test
    fun `reset restores defaults and full list`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeOfferRepository()
        repo.offersFlow.value = listOf(
            sampleOffer("opencode/m", providerId = "opencode"),
            sampleOffer("opencode/paid", status = FreeStatus.PAID, providerId = "opencode"),
            sampleOffer("other/m", source = "other-source", status = FreeStatus.LIMITED)
        )
        val vm = DashboardViewModel(repo, NoopSyncNotifier())
        vm.state.test {
            awaitItem()
            testScheduler.advanceUntilIdle()
            awaitItem()
            vm.onAction(DashboardAction.SelectFilter(OfferFilter.ALL))
            vm.onAction(DashboardAction.SelectSource(SourceFilter.OPENCODE))
            testScheduler.advanceUntilIdle()
            var filtered = awaitItem()
            while (filtered.sourceFilter != SourceFilter.OPENCODE) filtered = awaitItem()
            assertThat(filtered.showResetFilters).isTrue()
            assertThat(filtered.offers.map { it.remoteId }).isEqualTo(listOf("opencode/m", "opencode/paid"))
            vm.onAction(DashboardAction.ResetFilters)
            testScheduler.advanceUntilIdle()
            var reset = awaitItem()
            while (reset.showResetFilters) reset = awaitItem()
            assertThat(reset.filter).isEqualTo(OfferFilter.FREE)
            assertThat(reset.sourceFilter).isEqualTo(SourceFilter.ALL_SOURCES)
            // other/m is LIMITED: usable-free, so visible in the default view.
            assertThat(reset.offers.map { it.remoteId }).isEqualTo(listOf("opencode/m", "other/m"))
        }
    }

    @Test
    fun `default free view hides unverified and gated rows`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeOfferRepository()
        repo.offersFlow.value = listOf(
            sampleOffer("kilo/m", providerId = "kilo"),
            sampleOffer("kenari/m", providerId = "kenari", confidence = Confidence.TO_VERIFY),
            sampleOffer("gitlab/m", providerId = "gitlab", status = FreeStatus.LIMITED)
        )
        val vm = DashboardViewModel(repo, NoopSyncNotifier())
        vm.state.test {
            awaitItem()
            testScheduler.advanceUntilIdle()
            val shown = awaitItem()
            assertThat(shown.offers.map { it.remoteId }).isEqualTo(listOf("kilo/m"))
        }
    }

    @Test
    fun `stored filters restore on launch`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeOfferRepository()
        repo.offersFlow.value = listOf(sampleOffer())
        val prefs = FakeDashboardFilterPrefs(
            filterName = OfferFilter.FAVORITE.name,
            sourceName = SourceFilter.OPENROUTER.name,
            sortName = OfferSort.NAME.name,
            showLocal = true,
            recents = listOf("q1", "q2")
        )
        val vm = DashboardViewModel(repo, NoopSyncNotifier(), filterPrefs = prefs)
        vm.state.test {
            awaitItem()
            testScheduler.advanceUntilIdle()
            var restored = awaitItem()
            while (restored.filter != OfferFilter.FAVORITE) restored = awaitItem()
            assertThat(restored.sourceFilter).isEqualTo(SourceFilter.OPENROUTER)
            assertThat(restored.sort).isEqualTo(OfferSort.NAME)
            assertThat(restored.showLocal).isTrue()
            assertThat(restored.recentSearches).isEqualTo(listOf("q1", "q2"))
        }
    }

    @Test
    fun `selection changes persist debounced`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeOfferRepository()
        repo.offersFlow.value = listOf(sampleOffer())
        val prefs = FakeDashboardFilterPrefs()
        val vm = DashboardViewModel(repo, NoopSyncNotifier(), filterPrefs = prefs)
        vm.state.test {
            awaitItem()
            testScheduler.advanceUntilIdle()
            vm.onAction(DashboardAction.SelectFilter(OfferFilter.COMPATIBLE))
            vm.onAction(DashboardAction.SelectSource(SourceFilter.LITELLM))
            testScheduler.advanceTimeBy(400)
            testScheduler.advanceUntilIdle()
            var shown = awaitItem()
            while (shown.filter != OfferFilter.COMPATIBLE ||
                shown.sourceFilter != SourceFilter.LITELLM
            ) {
                shown = awaitItem()
            }
            assertThat(prefs.saved?.filter).isEqualTo("COMPATIBLE")
            assertThat(prefs.saved?.source).isEqualTo("LITELLM")
            assertThat(prefs.saved?.sort).isEqualTo("RECENT")
            assertThat(prefs.saved?.showLocal).isEqualTo(false)
        }
    }

    @Test
    fun `unknown stored names fall back to defaults`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeOfferRepository()
        repo.offersFlow.value = listOf(sampleOffer())
        // recents prove the restore ran; names prove the fallback.
        val prefs = FakeDashboardFilterPrefs(
            filterName = "NOPE", sourceName = "NOPE", sortName = "NOPE",
            recents = listOf("x")
        )
        val vm = DashboardViewModel(repo, NoopSyncNotifier(), filterPrefs = prefs)
        vm.state.test {
            awaitItem()
            testScheduler.advanceUntilIdle()
            var restored = awaitItem()
            while (restored.recentSearches != listOf("x")) restored = awaitItem()
            assertThat(restored.filter).isEqualTo(OfferFilter.FREE)
            assertThat(restored.sourceFilter).isEqualTo(SourceFilter.ALL_SOURCES)
            assertThat(restored.sort).isEqualTo(OfferSort.RECENT)
        }
    }

    @Test
    fun `restored recents keep order and cap`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeOfferRepository()
        repo.offersFlow.value = listOf(sampleOffer())
        val prefs = FakeDashboardFilterPrefs(recents = listOf("a", "b", "c", "d"))
        val vm = DashboardViewModel(repo, NoopSyncNotifier(), filterPrefs = prefs)
        vm.state.test {
            awaitItem()
            testScheduler.advanceUntilIdle()
            var restored = awaitItem()
            while (restored.recentSearches != listOf("a", "b", "c")) restored = awaitItem()
            assertThat(restored.recentSearches).isEqualTo(listOf("a", "b", "c"))
        }
    }

    @Test
    fun `refresh raises and clears the refreshing flag`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeOfferRepository()
        val gate = CompletableDeferred<Unit>()
        repo.refreshGate = gate
        val vm = DashboardViewModel(repo, NoopSyncNotifier())
        vm.state.test {
            awaitItem()
            testScheduler.advanceUntilIdle()
            awaitItem()
            vm.onAction(DashboardAction.Refresh)
            testScheduler.runCurrent()
            assertThat(awaitItem().isRefreshing).isTrue()
            gate.complete(Unit)
            testScheduler.advanceUntilIdle()
            assertThat(awaitItem().isRefreshing).isFalse()
        }
    }

    @Test
    fun `failed refresh surfaces error and dismiss clears`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeOfferRepository()
        repo.refreshResult = RefreshResult.Failed(SourceError.Timeout)
        val vm = DashboardViewModel(repo, NoopSyncNotifier())
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
    fun `manual refresh evaluates the notification gate on success`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeOfferRepository()
        val gate = NoopSyncNotifier()
        val vm = DashboardViewModel(repo, gate)
        vm.state.test {
            awaitItem()
            testScheduler.advanceUntilIdle()
            awaitItem()
            vm.onAction(DashboardAction.Refresh)
            testScheduler.advanceUntilIdle()
            assertThat(repo.refreshCalls).isEqualTo(1)
            assertThat(gate.afterSyncCalls).isEqualTo(1)
        }
    }

    @Test
    fun `manual refresh skips the gate on failure`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeOfferRepository()
        repo.refreshResult = RefreshResult.Failed(SourceError.Timeout)
        val gate = NoopSyncNotifier()
        val vm = DashboardViewModel(repo, gate)
        vm.state.test {
            awaitItem()
            testScheduler.advanceUntilIdle()
            awaitItem()
            vm.onAction(DashboardAction.Refresh)
            testScheduler.advanceUntilIdle()
            assertThat(awaitItem().error).isNotNull()
            assertThat(repo.refreshCalls).isEqualTo(1)
            assertThat(gate.afterSyncCalls).isEqualTo(0)
        }
    }

    @Test
    fun `search filters by name`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeOfferRepository()
        repo.offersFlow.value = listOf(
            sampleOffer("p/alpha", name = "Alpha Coder"),
            sampleOffer("p/beta", name = "Beta Chat")
        )
        val vm = DashboardViewModel(repo, NoopSyncNotifier())
        vm.state.test {
            awaitItem()
            testScheduler.advanceUntilIdle()
            awaitItem()
            vm.onAction(DashboardAction.Search("alp"))
            testScheduler.advanceUntilIdle()
            var filtered = awaitItem()
            while (filtered.offers.map { it.remoteId } != listOf("p/alpha")) {
                filtered = awaitItem()
            }
            assertThat(filtered.offers.map { it.remoteId }).isEqualTo(listOf("p/alpha"))
        }
    }

    @Test
    fun `search combines with status filter`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeOfferRepository()
        repo.offersFlow.value = listOf(
            sampleOffer("p/alpha", name = "Alpha Coder"),
            sampleOffer("p/beta", name = "Alpha Paid", status = FreeStatus.PAID)
        )
        val vm = DashboardViewModel(repo, NoopSyncNotifier())
        vm.state.test {
            awaitItem()
            testScheduler.advanceUntilIdle()
            awaitItem()
            vm.onAction(DashboardAction.Search("alpha"))
            testScheduler.advanceUntilIdle()
            var filtered = awaitItem()
            while (filtered.query != "alpha" || filtered.offers.map { it.remoteId } != listOf("p/alpha")) {
                filtered = awaitItem()
            }
            assertThat(filtered.offers.map { it.remoteId }).isEqualTo(listOf("p/alpha"))
        }
    }

    @Test
    fun `submitted searches are remembered distinct and capped`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeOfferRepository()
        val vm = DashboardViewModel(repo, NoopSyncNotifier())
        vm.state.test {
            awaitItem()
            testScheduler.advanceUntilIdle()
            awaitItem()
            vm.onAction(DashboardAction.SubmitSearch("muse"))
            vm.onAction(DashboardAction.SubmitSearch("  "))
            vm.onAction(DashboardAction.SubmitSearch("qwq"))
            vm.onAction(DashboardAction.SubmitSearch("muse"))
            vm.onAction(DashboardAction.SubmitSearch("zzz"))
            vm.onAction(DashboardAction.SubmitSearch("yyy"))
            testScheduler.advanceUntilIdle()
            var state = awaitItem()
            while (state.recentSearches != listOf("yyy", "zzz", "muse")) {
                state = awaitItem()
            }
            assertThat(state.recentSearches).isEqualTo(listOf("yyy", "zzz", "muse"))
        }
    }

    @Test
    fun `toggle favorite records setFavorite`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeOfferRepository()
        val vm = DashboardViewModel(repo, NoopSyncNotifier())
        vm.onAction(DashboardAction.ToggleFavorite("p/m", true))
        testScheduler.advanceUntilIdle()
        assertThat(repo.lastFavorite).isEqualTo("p/m" to true)
    }

    @Test
    fun `offline reflects unavailable source health`() = runTest {        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeOfferRepository()
        val vm = DashboardViewModel(repo, NoopSyncNotifier())
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

    @Test
    fun `toggle favorite flips the current flag`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeOfferRepository()
        repo.offersFlow.value = listOf(sampleOffer())
        val vm = DetailsViewModel("p/m", repo)
        vm.state.test {
            awaitItem()
            testScheduler.advanceUntilIdle()
            awaitItem()
            vm.toggleFavorite()
            testScheduler.advanceUntilIdle()
            assertThat(repo.lastFavorite).isEqualTo("p/m" to true)
        }
    }
}
