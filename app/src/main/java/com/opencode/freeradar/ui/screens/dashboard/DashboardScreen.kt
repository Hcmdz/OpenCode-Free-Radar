/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.screens.dashboard

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opencode.freeradar.R
import com.opencode.freeradar.domain.model.FreeStatus
import com.opencode.freeradar.ui.components.ErrorBanner
import com.opencode.freeradar.ui.components.FilterBar
import com.opencode.freeradar.ui.components.PrimaryPillButton
import com.opencode.freeradar.ui.components.StatusPill
import com.opencode.freeradar.ui.components.StatusTone
import com.opencode.freeradar.ui.components.freeStatusTone
import com.opencode.freeradar.ui.model.OfferFilter
import com.opencode.freeradar.ui.model.OfferUi
import com.opencode.freeradar.ui.model.sourceLabel
import com.opencode.freeradar.ui.model.text
import com.opencode.freeradar.ui.theme.AppThemePreview
import com.opencode.freeradar.ui.viewmodel.DashboardAction
import com.opencode.freeradar.ui.viewmodel.DashboardEvent
import com.opencode.freeradar.ui.viewmodel.DashboardUiState
import com.opencode.freeradar.ui.viewmodel.DashboardViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun DashboardRoot(
    viewModel: DashboardViewModel = koinViewModel(),
    onOpenDetails: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is DashboardEvent.OpenDetails -> onOpenDetails(event.remoteId)
            }
        }
    }
    DashboardScreen(state = state, onAction = viewModel::onAction, onOpenSettings = onOpenSettings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onAction: (DashboardAction) -> Unit,
    onOpenSettings: () -> Unit = {}
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // Visible past the first item only; Scaffold docks it bottom-end (right).
    val showScrollTop = listState.firstVisibleItemIndex > 0
    Scaffold(
        modifier = Modifier.testTag("dashboard_screen"),
        topBar = {
            TopAppBar(
                title = {
                    val countRes = when (state.filter) {
                        OfferFilter.ALL, OfferFilter.COMPATIBLE -> R.plurals.models_count
                        OfferFilter.FREE, OfferFilter.FREE_COMPATIBLE -> R.plurals.offers_count
                    }
                    Text(
                        pluralStringResource(
                            countRes,
                            state.offers.size,
                            state.offers.size
                        )
                    )
                },
                actions = {
                    IconButton(
                        modifier = Modifier.testTag("dashboard_settings"),
                        onClick = onOpenSettings
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.desc_settings)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(visible = showScrollTop) {
                SmallFloatingActionButton(
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    modifier = Modifier.testTag("dashboard_scroll_top")
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.desc_scroll_top)
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.offline) {
                OfflineBanner(lastSyncAt = state.lastSyncAt)
            }
            FilterBar(
                filter = state.filter,
                sourceFilter = state.sourceFilter,
                statusCounts = state.statusCounts,
                sourceCounts = state.sourceCounts,
                showReset = state.showResetFilters,
                onSelectFilter = { onAction(DashboardAction.SelectFilter(it)) },
                onSelectSource = { onAction(DashboardAction.SelectSource(it)) },
                onReset = { onAction(DashboardAction.ResetFilters) },
                modifier = Modifier
                    .testTag("dashboard_filter")
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            state.error?.let { error ->
                ErrorBanner(
                    message = error.text(),
                    actionLabel = stringResource(R.string.retry),
                    onAction = { onAction(DashboardAction.Refresh) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onAction(DashboardAction.Refresh) },
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    state.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }

                state.offers.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.empty_offers_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.empty_offers_hint),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        PrimaryPillButton(
                            text = stringResource(R.string.refresh_now),
                            onClick = { onAction(DashboardAction.Refresh) }
                        )
                    }
                }

                else -> LazyColumn(
                    modifier = Modifier.testTag("dashboard_list").fillMaxSize(),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.offers, key = { it.remoteId }) { offer ->
                        OfferCard(
                            offer = offer,
                            onClick = { onAction(DashboardAction.OpenOffer(offer.remoteId)) }
                        )
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun OfflineBanner(lastSyncAt: Long?) {
    val age = lastSyncAt?.let {
        DateUtils.getRelativeTimeSpanString(it, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
            .toString()
    } ?: stringResource(R.string.value_unknown)
    Text(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        text = stringResource(R.string.offline_banner, age),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun OfferCard(offer: OfferUi, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("offer_card"),
        shape = MaterialTheme.shapes.extraLarge,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = offer.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = offer.providerId,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(text = offer.freeStatus.name, tone = freeStatusTone(offer.freeStatus))
                StatusPill(text = sourceLabel(offer.source), tone = StatusTone.NEUTRAL)
                offer.contextLength?.let {
                    AssistChip(onClick = {}, label = { Text("$it tokens") })
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardListPreview() {
    AppThemePreview {
        DashboardScreen(
            state = DashboardUiState(
                isLoading = false,
                filter = OfferFilter.FREE_COMPATIBLE,
                offers = listOf(
                    OfferUi("p/m", "Model", "p", FreeStatus.FREE, 1_000_000, 1_000L, "opencode-data")
                )
            ),
            onAction = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardLoadingPreview() {
    AppThemePreview {
        DashboardScreen(state = DashboardUiState(), onAction = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardEmptyPreview() {
    AppThemePreview {
        DashboardScreen(state = DashboardUiState(isLoading = false), onAction = {})
    }
}
