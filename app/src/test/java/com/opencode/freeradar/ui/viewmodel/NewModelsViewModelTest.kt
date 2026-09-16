/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.viewmodel

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.opencode.freeradar.ui.navigation.NewModels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NewModelsViewModelTest {

    @BeforeEach
    fun setup() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterEach
    fun teardown() = Dispatchers.resetMain()

    @Test
    fun `snapshot ids resolve to rows, missing ids are dropped`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeOfferRepository()
        repo.offersFlow.value = listOf(sampleOffer("p/a"), sampleOffer("p/b"))
        val vm = NewModelsViewModel(NewModels(listOf("p/a", "p/gone"), listOf("p/b")), repo)
        vm.state.test {
            assertThat(awaitItem().isLoading).isTrue()
            testScheduler.advanceUntilIdle()
            val loaded = awaitItem()
            assertThat(loaded.isLoading).isFalse()
            assertThat(loaded.newModels.map { it.remoteId }).isEqualTo(listOf("p/a"))
            assertThat(loaded.expired.map { it.remoteId }).isEqualTo(listOf("p/b"))
        }
    }

    @Test
    fun `toggle favorite records setFavorite`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeOfferRepository()
        val vm = NewModelsViewModel(NewModels(listOf("p/a"), emptyList()), repo)
        vm.toggleFavorite("p/a", true)
        testScheduler.advanceUntilIdle()
        assertThat(repo.lastFavorite).isEqualTo("p/a" to true)
    }
}
