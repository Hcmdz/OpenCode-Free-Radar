/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.screens.details

import android.content.Intent
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opencode.freeradar.R
import com.opencode.freeradar.domain.model.ChangeEvent
import com.opencode.freeradar.domain.model.ChangeType
import com.opencode.freeradar.domain.model.Confidence
import com.opencode.freeradar.domain.model.FreeStatus
import com.opencode.freeradar.domain.model.Offer
import com.opencode.freeradar.ui.components.StatusPill
import com.opencode.freeradar.ui.components.StatusTone
import com.opencode.freeradar.ui.components.freeStatusTone
import com.opencode.freeradar.ui.model.compactCount
import com.opencode.freeradar.ui.model.sourceLabel
import com.opencode.freeradar.ui.theme.AppThemePreview
import com.opencode.freeradar.ui.viewmodel.DetailsUiState
import com.opencode.freeradar.ui.viewmodel.DetailsViewModel
import java.util.Locale
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun DetailsRoot(
    offerId: String,
    onBack: () -> Unit,
    viewModel: DetailsViewModel = koinViewModel { parametersOf(offerId) }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DetailsScreen(state = state, onBack = onBack, onToggleFavorite = viewModel::toggleFavorite)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DetailsScreen(
    state: DetailsUiState,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit = {}
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val offer = state.offer
    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .testTag("details_screen"),
        topBar = {
            MediumFlexibleTopAppBar(
                title = {
                    Text(
                        text = offer?.name ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("details_title")
                    )
                },
                subtitle = offer?.let {
                    {
                        Text(
                            text = it.providerId,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.desc_back)
                        )
                    }
                },
                actions = {
                    val favorite = offer?.favorite == true
                    IconButton(
                        modifier = Modifier.testTag("details_favorite"),
                        onClick = onToggleFavorite
                    ) {
                        Icon(
                            imageVector = if (favorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = stringResource(
                                if (favorite) R.string.desc_unfavorite else R.string.desc_favorite
                            )
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        if (state.isLoading || offer == null) {
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator()
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { HeroCard(offer = offer) }
            item {
                InfoCard(titleRes = R.string.section_prices) {
                    Text(priceLine(offer.inputPrice, offer.outputPrice, unknown()))
                    Text(
                        "${stringResource(R.string.label_context)}: ${offer.contextLength?.compactCount() ?: unknown()}  •  " +
                            "${stringResource(R.string.label_output)}: ${offer.maxOutputTokens?.compactCount() ?: unknown()}"
                    )
                }
            }
            item {
                InfoCard(titleRes = R.string.section_capabilities) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CapabilityChip(
                            label = stringResource(R.string.label_tools),
                            value = offer.supportsTools
                        )
                        CapabilityChip(
                            label = stringResource(R.string.label_vision),
                            value = offer.supportsVision
                        )
                        CapabilityChip(
                            label = stringResource(R.string.label_structured),
                            value = offer.supportsStructuredOutput
                        )
                    }
                }
            }
            if (offer.quota != null || offer.quotaPeriod != null) {
                item {
                    InfoCard(titleRes = R.string.section_quotas) {
                        Text(offer.quota ?: unknown())
                    }
                }
            }
            offer.conditions?.let { conditions ->
                item {
                    InfoCard(titleRes = R.string.section_conditions) { Text(conditions) }
                }
            }
            item {
                InfoCard(titleRes = R.string.section_compat) {
                    Text(if (offer.openCodeCompatible) yes() else no())
                }
            }
            item {
                InfoCard(titleRes = R.string.provenance_title) {
                    Text("${offer.source}${offer.sourceUrl?.let { " — $it" } ?: ""}")
                    Text(
                        DateUtils.getRelativeTimeSpanString(
                            offer.verifiedAt, System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS
                        ).toString()
                    )
                    if (offer.confidence == Confidence.TO_VERIFY) {
                        Text(
                            text = stringResource(R.string.conflict_warning),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            item {
                InfoCard(titleRes = R.string.section_history) {
                    if (state.history.isEmpty()) {
                        Text(stringResource(R.string.history_empty))
                    }
                }
            }
            items(state.history, key = { it.offerRemoteId + it.createdAt + it.type.name }) { event ->
                HistoryRow(event = event)
            }
        }
    }
}

@Composable
private fun HeroCard(offer: Offer) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(text = offer.freeStatus.name, tone = freeStatusTone(offer.freeStatus))
                StatusPill(text = sourceLabel(offer.source), tone = StatusTone.NEUTRAL)
                if (offer.confidence == Confidence.TO_VERIFY) {
                    StatusPill(
                        text = stringResource(R.string.status_unverified),
                        tone = StatusTone.NEUTRAL
                    )
                }
            }
            Text(
                text = priceLine(offer.inputPrice, offer.outputPrice, stringResource(R.string.value_unknown)),
                style = MaterialTheme.typography.headlineSmall
            )
            offer.officialUrl?.let { url ->
                FilledTonalButton(
                    modifier = Modifier.testTag("details_open"),
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                    }
                ) {
                    Text(stringResource(R.string.details_open))
                }
            }
        }
    }
}

@Composable
private fun InfoCard(titleRes: Int, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun CapabilityChip(label: String, value: Boolean?) {
    AssistChip(
        onClick = {},
        label = { Text("$label: ${yesNoUnknown(value)}") },
        leadingIcon = if (value == true) {
            {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null
                )
            }
        } else {
            {}
        }
    )
}

@Composable
private fun yesNoUnknown(value: Boolean?): String = when (value) {
    true -> stringResource(R.string.value_yes)
    false -> stringResource(R.string.value_no)
    null -> stringResource(R.string.value_unknown)
}

@Composable
private fun yes(): String = stringResource(R.string.value_yes)

@Composable
private fun no(): String = stringResource(R.string.value_no)

@Composable
private fun unknown(): String = stringResource(R.string.value_unknown)

private fun priceLine(input: Double?, output: Double?, unknown: String): String = when {
    input == null || output == null -> unknown
    else -> String.format(Locale.US, "$%.3f / $%.3f per 1M tokens", input, output)
}

@Composable
private fun HistoryRow(event: ChangeEvent) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = historyLabel(event.type), style = MaterialTheme.typography.bodyMedium)
            Text(
                text = DateUtils.getRelativeTimeSpanString(
                    event.createdAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
                ).toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun historyLabel(type: ChangeType): String = when (type) {
    ChangeType.NEW_MODEL -> stringResource(R.string.history_new_model)
    ChangeType.BECAME_FREE -> stringResource(R.string.history_became_free)
    ChangeType.FREE_EXPIRED -> stringResource(R.string.history_expired)
    else -> type.name
}

@Preview(showBackground = true)
@Composable
private fun DetailsPreview() {
    AppThemePreview {
        DetailsScreen(
            onBack = {},
            state = DetailsUiState(
                isLoading = false,
                offer = Offer(
                    remoteId = "p/m", providerId = "p", modelId = "m", name = "Model",
                    inputPrice = 0.0, outputPrice = 0.0, freeStatus = FreeStatus.FREE,
                    quota = "50/day", quotaPeriod = "day", temporary = false,
                    conditions = "Trial terms apply",
                    contextLength = 1_000_000, maxOutputTokens = 32_000,
                    supportsTools = true, supportsVision = false,
                    supportsStructuredOutput = true, openCodeCompatible = true,
                    officialUrl = "https://example.com", source = "opencode-data", sourceUrl = null,
                    retrievedAt = 1_000L, verifiedAt = 1_000L,
                    confidence = Confidence.OFFICIAL, favorite = false
                ),
                history = listOf(
                    ChangeEvent("p/m", ChangeType.BECAME_FREE, "PAID", "FREE", 2_000L)
                )
            )
        )
    }
}
