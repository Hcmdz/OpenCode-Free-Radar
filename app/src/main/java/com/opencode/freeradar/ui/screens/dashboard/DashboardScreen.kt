/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.screens.dashboard

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateUtils
import android.view.accessibility.AccessibilityManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opencode.freeradar.R
import com.opencode.freeradar.domain.model.FreeStatus
import com.opencode.freeradar.ui.components.ErrorBanner
import com.opencode.freeradar.ui.components.FilterChipsRow
import com.opencode.freeradar.ui.components.FilterFab
import com.opencode.freeradar.ui.components.FilterSheet
import com.opencode.freeradar.ui.components.OfferCard
import com.opencode.freeradar.ui.components.PrimaryPillButton
import com.opencode.freeradar.ui.components.StatusPill
import com.opencode.freeradar.ui.components.StatusTone
import com.opencode.freeradar.ui.components.freeStatusTone
import com.opencode.freeradar.data.local.FilterFabPrefs
import com.opencode.freeradar.data.local.NotificationPrefs
import com.opencode.freeradar.ui.components.activeSummary
import com.opencode.freeradar.ui.model.OfferFilter
import com.opencode.freeradar.ui.model.OfferSort
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
    val appContext = LocalContext.current.applicationContext
    val fabPrefs = remember { FilterFabPrefs(appContext) }
    val persistScope = rememberCoroutineScope()
    val notificationPrefs = remember { NotificationPrefs(appContext) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) persistScope.launch { notificationPrefs.setEnabled(true) }
    }
    // Without the grant the notifier silently drops every alert, so the whole
    // background sync becomes invisible. Asked once: the OS ignores a second
    // system dialog, which would leave a permanently dead toggle.
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !notificationPrefs.permissionAsked.first()
        ) {
            notificationPrefs.setPermissionAsked()
            if (ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    val peekDelayMs by fabPrefs.peekDelayMillis.collectAsStateWithLifecycle(
        initialValue = FilterFabPrefs.DEFAULT_DELAY_MILLIS
    )
    val peekSliverDp by fabPrefs.peekSliverDp.collectAsStateWithLifecycle(
        initialValue = FilterFabPrefs.DEFAULT_SLIVER_DP
    )
    val persistedFabOffset by fabPrefs.fabOffset.collectAsStateWithLifecycle(
        initialValue = null
    )
    DashboardScreen(
        state = state,
        onAction = viewModel::onAction,
        onOpenSettings = onOpenSettings,
        peekDelayMs = peekDelayMs,
        peekSliverDp = peekSliverDp,
        persistedFabOffset = persistedFabOffset?.let { (x, y) -> IntOffset(x, y) },
        onPersistFabOffset = { offset ->
            persistScope.launch { fabPrefs.setFabOffset(offset.x, offset.y) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onAction: (DashboardAction) -> Unit,
    onOpenSettings: () -> Unit = {},
    filterAutoPeek: Boolean = true,
    peekDelayMs: Long = FilterFabPrefs.DEFAULT_DELAY_MILLIS,
    peekSliverDp: Int = FilterFabPrefs.DEFAULT_SLIVER_DP,
    /** Last persisted drag position, null when never dragged (anchor). */
    persistedFabOffset: IntOffset? = null,
    /** Called once per filter-FAB drop with the snapped position. */
    onPersistFabOffset: (IntOffset) -> Unit = {}
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    var sheetOpen by remember { mutableStateOf(false) }
    // Filter FAB edge-peek: every user interaction pokes the timer; after
    // [peekDelayMs] of screen-wide silence with the sheet closed, the FAB
    // slides to the nearest edge leaving a sliver visible — never fully
    // gone, always grabbable. Pure UI state (like sheetOpen); rotation
    // resets to unpeeked, which is sane.
    var peeked by remember { mutableStateOf(false) }
    var pokeTick by remember { mutableIntStateOf(0) }
    var fabDragging by remember { mutableStateOf(false) }
    fun poke() {
        peeked = false
        pokeTick++
    }
    // TalkBack explore-by-touch produces no taps: auto-hide would strand
    // those users without filters, so it stays off for them.
    val context = LocalContext.current
    val touchExploration = remember {
        (context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager)
            .isTouchExplorationEnabled
    }
    LaunchedEffect(pokeTick, fabDragging, sheetOpen, peekDelayMs) {
        // Never peek mid-drag, and never while the sheet covers the screen:
        // a peeked FAB behind the modal is unreachable until dismiss.
        // TalkBack explore-by-touch produces no taps: peeking would strand
        // those users with a sliver, so it stays off for them.
        if (filterAutoPeek && !fabDragging && !sheetOpen && !touchExploration) {
            delay(peekDelayMs)
            peeked = true
        }
    }
    // ponytail: single dismiss path for back-press, tap-outside and filter taps.
    val dismissSearch = {
        searchExpanded = false
        focusManager.clearFocus()
        poke()
    }
    BackHandler(enabled = searchExpanded) {
        dismissSearch()
    }
    // Scrolling the list folds the panel; the typed query lives in the
    // ViewModel and survives. Also covers the scroll-top FAB animation.
    // Settling re-pokes: a scroll that outlasts the peek delay brings the FAB back.
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) dismissSearch() else poke()
    }
    // Every emitted action is a user gesture: single poke point for all of them.
    fun userAction(action: DashboardAction) {
        poke()
        onAction(action)
    }
    // Visible past the first item only; Scaffold docks it bottom-end (right).
    val showScrollTop = listState.firstVisibleItemIndex > 0
    // The filter FAB shares the bottom-end corner: while the scroll-top
    // button is up, the anchor glides one button-height plus air above it
    // instead of stacking both pastilles on the same spot.
    val filterLift by animateDpAsState(
        targetValue = if (showScrollTop) ScrollTopClearance else 0.dp,
        label = "filterLift"
    )
    Scaffold(
        modifier = Modifier.testTag("dashboard_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.dashboard_title),
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
                }
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
        // value) in a plain container. BoxWithConstraints feeds the draggable
        // filter FAB its container size for clamping.
        BoxWithConstraints(modifier = Modifier.padding(padding).fillMaxSize()) {
            val density = LocalDensity.current
            val containerSize = with(density) {
                IntSize(maxWidth.roundToPx(), maxHeight.roundToPx())
            }
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
            DashboardCountLine(
                filter = state.filter,
                offerCount = state.offers.size,
                lastSyncAt = state.lastSyncAt
            )
            FilterChipsRow(
                filter = state.filter,
                statusCounts = state.statusCounts,
                onSelectFilter = {
                    dismissSearch()
                    userAction(DashboardAction.SelectFilter(it))
                }
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
                    onAction = { userAction(DashboardAction.Refresh) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { userAction(DashboardAction.Refresh) },
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
                                onClick = { userAction(DashboardAction.Refresh) }
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
                            PrimaryPillButton(
                                text = stringResource(R.string.reset_filters),
                                onClick = { userAction(DashboardAction.ClearSearchAndFilters) },
                                modifier = Modifier.testTag("dashboard_no_match_reset")
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
                            onClick = { userAction(DashboardAction.OpenOffer(offer.remoteId)) },
                            onToggleFavorite = {
                                userAction(DashboardAction.ToggleFavorite(offer.remoteId, !offer.favorite))
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
                        userAction(DashboardAction.AckPendingNew)
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
                            userAction(DashboardAction.Search(it))
                            searchExpanded = false
                            focusManager.clearFocus()
                        },
                        onProvider = {
                            userAction(DashboardAction.SelectSource(it))
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
                        onQueryChange = { userAction(DashboardAction.Search(it)) },
                        onFocusChange = { focused ->
                            if (focused) {
                                searchExpanded = true
                                poke()
                            }
                        },
                        onSubmit = {
                            userAction(DashboardAction.SubmitSearch(state.query))
                            searchExpanded = false
                            focusManager.clearFocus()
                        }
                    )
                }
            }
            // Draggable filter entry point, declared last so it stays on top of
            // the content column (tap-outside layer never sees its touches).
            // Anchored bottom-end above the search dock; drag offset is relative.
            FilterFab(
                summary = activeSummary(
                    source = state.sourceFilter,
                    sort = state.sort,
                    sourceLabels = SourceFilter.entries.associateWith { stringResource(it.labelRes) },
                    sortLabels = OfferSort.entries.associateWith { stringResource(it.labelRes) },
                    defaultTitle = stringResource(R.string.filter_title),
                    showLocal = state.showLocal,
                    localLabel = stringResource(R.string.filter_show_local)
                ),
                active = state.showResetFilters,
                containerSize = containerSize,
                onOpen = {
                    dismissSearch()
                    sheetOpen = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .testTag("dashboard_filter"),
                bottomPadding = OverlayDockReserve + 16.dp + filterLift,
                peeked = peeked,
                sliverDp = peekSliverDp,
                onUserInteraction = { poke() },
                onDraggingChange = { fabDragging = it },
                persistedOffset = persistedFabOffset,
                onPersistOffset = onPersistFabOffset
            )
            FilterSheet(
                visible = sheetOpen,
                filter = state.filter,
                sourceFilter = state.sourceFilter,
                statusCounts = state.statusCounts,
                sourceCounts = state.sourceCounts,
                showReset = state.showResetFilters,
                onSelectFilter = {
                    dismissSearch()
                    userAction(DashboardAction.SelectFilter(it))
                },
                onSelectSource = {
                    dismissSearch()
                    userAction(DashboardAction.SelectSource(it))
                },
                onReset = {
                    dismissSearch()
                    userAction(DashboardAction.ResetFilters)
                },
                onDismiss = { sheetOpen = false; poke() },
                sort = state.sort,
                onSelectSort = { userAction(DashboardAction.SelectSort(it)) },
                showLocal = state.showLocal,
                onToggleShowLocal = { userAction(DashboardAction.SetShowLocal(it)) }
            )
        }
    }
    if (state.meteredWarning) {
        AlertDialog(
            onDismissRequest = { userAction(DashboardAction.MeteredLater) },
            title = { Text(text = stringResource(R.string.metered_title)) },
            text = { Text(text = stringResource(R.string.metered_message)) },
            confirmButton = {
                TextButton(
                    onClick = { userAction(DashboardAction.MeteredSyncOnce) },
                    modifier = Modifier.testTag("metered_sync_once")
                ) {
                    Text(text = stringResource(R.string.metered_sync_once))
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { userAction(DashboardAction.MeteredLater) },
                        modifier = Modifier.testTag("metered_later")
                    ) {
                        Text(text = stringResource(R.string.update_later))
                    }
                    TextButton(
                        onClick = { userAction(DashboardAction.MeteredNeverWarn) },
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

/** Filter-FAB lift while the scroll-top button is up: its height + air. */
private val ScrollTopClearance = 48.dp

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

/** Wall clock refreshed every minute for relative-time labels. */
private fun minuteTicker(): Flow<Long> = flow {
    while (true) {
        emit(System.currentTimeMillis())
        delay(60_000)
    }
}

/** Former TopAppBar subtitle: count + sync age as a fixed line above the chips. */
@Composable
private fun DashboardCountLine(
    filter: OfferFilter,
    offerCount: Int,
    lastSyncAt: Long?,
    modifier: Modifier = Modifier
) {
    // Read inside this restartable scope: each tick recomposes this line
    // alone, never the list.
    val now by remember { minuteTicker() }
        .collectAsStateWithLifecycle(initialValue = System.currentTimeMillis())
    val countRes = when (filter) {
        OfferFilter.ALL, OfferFilter.COMPATIBLE -> R.plurals.models_count
        OfferFilter.FREE, OfferFilter.FREE_COMPATIBLE,
        OfferFilter.FAVORITE -> R.plurals.offers_count
    }
    val count = pluralStringResource(countRes, offerCount, offerCount)
    val age = lastSyncAt?.let {
        DateUtils.getRelativeTimeSpanString(
            it,
            now,
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
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    )
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
