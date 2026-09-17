/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.screens.dashboard

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
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
import com.opencode.freeradar.ui.components.OfferCard
import com.opencode.freeradar.ui.components.PrimaryPillButton
import com.opencode.freeradar.ui.components.StatusPill
import com.opencode.freeradar.ui.components.StatusTone
import com.opencode.freeradar.ui.components.freeStatusTone
import com.opencode.freeradar.ui.model.OfferFilter
import com.opencode.freeradar.ui.model.OfferUi
import com.opencode.freeradar.ui.model.SourceFilter
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
    val focusManager = LocalFocusManager.current
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    // ponytail: single dismiss path for back-press, tap-outside and filter taps.
    val dismissSearch = {
        searchExpanded = false
        focusManager.clearFocus()
    }
    BackHandler(enabled = searchExpanded) {
        dismissSearch()
    }
    // Scrolling the list folds the panel; the typed query lives in the
    // ViewModel and survives. Also covers the scroll-top FAB animation.
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) dismissSearch()
    }
    // Visible past the first item only; Scaffold docks it bottom-end (right).
    val showScrollTop = listState.firstVisibleItemIndex > 0
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier
            .testTag("dashboard_screen")
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumFlexibleTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.dashboard_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                subtitle = {
                    val countRes = when (state.filter) {
                        OfferFilter.ALL, OfferFilter.COMPATIBLE -> R.plurals.models_count
                        OfferFilter.FREE, OfferFilter.FREE_COMPATIBLE,
                        OfferFilter.FAVORITE -> R.plurals.offers_count
                    }
                    val count = pluralStringResource(
                        countRes,
                        state.offers.size,
                        state.offers.size
                    )
                    val age = state.lastSyncAt?.let {
                        DateUtils.getRelativeTimeSpanString(
                            it,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS
                        ).toString()
                    }
                    Text(
                        text = if (age != null) {
                            stringResource(R.string.dashboard_subtitle, count, age)
                        } else {
                            count
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            AnimatedVisibility(visible = showScrollTop) {
                SmallFloatingActionButton(
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    modifier = Modifier
                        .testTag("dashboard_scroll_top")
                        .padding(bottom = OverlayDockReserve)
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.desc_scroll_top)
                    )
                }
            }
        }
    ) { padding ->
        // Truly floating search: overlay in a Box over the list instead of a
        // docked bottomBar slot. The dock hosts our debugged input (raw display
        // value) in a plain container.
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Tap-outside dismiss: pointerInput adds no semantics, so TalkBack
            // never sees this layer. Scoped to the content column only — the
            // bottom search dock is a sibling and never collapses itself.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { dismissSearch() })
                    }
            ) {
            if (state.offline) {
                OfflineBanner(lastSyncAt = state.lastSyncAt)
            }
            FilterBar(
                filter = state.filter,
                sourceFilter = state.sourceFilter,
                statusCounts = state.statusCounts,
                sourceCounts = state.sourceCounts,
                showReset = state.showResetFilters,
                onSelectFilter = {
                    dismissSearch()
                    onAction(DashboardAction.SelectFilter(it))
                },
                onSelectSource = {
                    dismissSearch()
                    onAction(DashboardAction.SelectSource(it))
                },
                onReset = {
                    dismissSearch()
                    onAction(DashboardAction.ResetFilters)
                },
                onOpenSheet = dismissSearch,
                sort = state.sort,
                onSelectSort = { onAction(DashboardAction.SelectSort(it)) },
                modifier = Modifier
                    .testTag("dashboard_filter")
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            if (state.filter == OfferFilter.FREE &&
                state.sourceFilter == SourceFilter.ALL_SOURCES &&
                state.query.isBlank() &&
                !state.offline &&
                state.offers.isNotEmpty()
            ) {
                val activeSources = state.sourceCounts
                    .count { it.key.sourceId != null && it.value > 0 }
                Surface(
                    modifier = Modifier
                        .testTag("dashboard_hero")
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Text(
                        text = stringResource(
                            R.string.hero_stats,
                            state.statusCounts[OfferFilter.FREE] ?: 0,
                            state.statusCounts[OfferFilter.COMPATIBLE] ?: 0,
                            activeSources
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
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

                state.offers.isEmpty() -> when {
                    state.isRefreshing && state.lastSyncAt == null -> LoadingSkeleton(
                        modifier = Modifier.fillMaxSize()
                    )
                    state.isBaseEmpty -> Box(
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
                    else -> Box(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.no_match_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(R.string.no_match_hint),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                else -> LazyColumn(
                    modifier = Modifier.testTag("dashboard_list").fillMaxSize(),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = OverlayDockReserve)
                ) {
                    items(state.offers, key = { it.remoteId }) { offer ->
                        OfferCard(
                            offer = offer,
                            onClick = { onAction(DashboardAction.OpenOffer(offer.remoteId)) },
                            onToggleFavorite = {
                                onAction(DashboardAction.ToggleFavorite(offer.remoteId, !offer.favorite))
                            }
                        )
                    }
                }
                }
            }
        }
            // New freebies landed while browsing: a pill, never a jump.
            // Tapping acknowledges and scrolls to the top.
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                AnimatedVisibility(visible = state.pendingNew > 0 && state.offers.isNotEmpty()) {
                AssistChip(
                    onClick = {
                        onAction(DashboardAction.AckPendingNew)
                        scope.launch { listState.animateScrollToItem(0) }
                    },
                    label = {
                        Text(
                            pluralStringResource(
                                R.plurals.pill_new_free,
                                state.pendingNew,
                                state.pendingNew
                            )
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.ArrowUpward,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.testTag("new_freebies_pill")
                )
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp)
                    .imePadding()
            ) {
                AnimatedVisibility(visible = searchExpanded) {
                    SuggestionsPanel(
                        recents = state.recentSearches,
                        providers = state.sourceCounts,
                        onRecent = {
                            onAction(DashboardAction.Search(it))
                            searchExpanded = false
                            focusManager.clearFocus()
                        },
                        onProvider = {
                            onAction(DashboardAction.SelectSource(it))
                            searchExpanded = false
                            focusManager.clearFocus()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                }
                // Plain docked container instead of the Expressive SearchBar:
                // that component installs a soft-keyboard interceptor for any
                // input field not wired to its SearchBarState, so the IME
                // never opened. A framework TextField opens it on tap.
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = SearchBarDefaults.dockedShape,
                    color = SearchBarDefaults.colors().containerColor,
                    shadowElevation = 6.dp
                ) {
                    SearchInput(
                        query = state.query,
                        onQueryChange = { onAction(DashboardAction.Search(it)) },
                        onFocusChange = { focused -> if (focused) searchExpanded = true },
                        onSubmit = {
                            onAction(DashboardAction.SubmitSearch(state.query))
                            searchExpanded = false
                            focusManager.clearFocus()
                        }
                    )
                }
            }
        }
    }
    if (state.meteredWarning) {
        AlertDialog(
            onDismissRequest = { onAction(DashboardAction.MeteredLater) },
            title = { Text(text = stringResource(R.string.metered_title)) },
            text = { Text(text = stringResource(R.string.metered_message)) },
            confirmButton = {
                TextButton(
                    onClick = { onAction(DashboardAction.MeteredSyncOnce) },
                    modifier = Modifier.testTag("metered_sync_once")
                ) {
                    Text(text = stringResource(R.string.metered_sync_once))
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { onAction(DashboardAction.MeteredLater) },
                        modifier = Modifier.testTag("metered_later")
                    ) {
                        Text(text = stringResource(R.string.update_later))
                    }
                    TextButton(
                        onClick = { onAction(DashboardAction.MeteredNeverWarn) },
                        modifier = Modifier.testTag("metered_never_warn")
                    ) {
                        Text(text = stringResource(R.string.metered_never_warn))
                    }
                }
            },
            modifier = Modifier.testTag("metered_dialog")
        )
    }
}

/** Space reserved under the floating search dock (bar + margins). */
private val OverlayDockReserve = 96.dp

@Composable
private fun SuggestionsPanel(
    recents: List<String>,
    providers: Map<SourceFilter, Int>,
    onRecent: (String) -> Unit,
    onProvider: (SourceFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("search_suggestions"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            recents.forEach { recent ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRecent(recent) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(text = recent, style = MaterialTheme.typography.bodyLarge)
                }
            }
            providers.keys.filter { it.sourceId != null }.forEach { provider ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onProvider(provider) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Storefront,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = sourceLabel(provider.sourceId!!),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${providers[provider] ?: 0}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchInput(
    query: String,
    onQueryChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit = {},
    onSubmit: () -> Unit = {}
) {
    // Borderless: the docked container already provides the shape
    // and tonal background, so this field only draws text and icons.
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .testTag("dashboard_search")
            .fillMaxWidth()
            .onFocusChanged { onFocusChange(it.isFocused) },
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = stringResource(R.string.search_clear)
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
        singleLine = true,
        shape = SearchBarDefaults.dockedShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

/** First-sync placeholder: static card ghosts, never a spinner flash. */
@Composable
private fun LoadingSkeleton(modifier: Modifier = Modifier) {
    val ghost = MaterialTheme.colorScheme.surfaceContainerHighest
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(5) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_skeleton_card"),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(20.dp)
                            .background(ghost, MaterialTheme.shapes.small)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(14.dp)
                            .background(ghost, MaterialTheme.shapes.small)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.25f)
                                .height(28.dp)
                                .background(ghost, MaterialTheme.shapes.small)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.3f)
                                .height(28.dp)
                                .background(ghost, MaterialTheme.shapes.small)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineBanner(lastSyncAt: Long?) {    val age = lastSyncAt?.let {
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
