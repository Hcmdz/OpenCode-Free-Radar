/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.screens.details

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opencode.freeradar.R
import com.opencode.freeradar.domain.model.ChangeEvent
import com.opencode.freeradar.domain.model.ChangeType
import com.opencode.freeradar.domain.model.Confidence
import com.opencode.freeradar.domain.model.FreeStatus
import com.opencode.freeradar.domain.model.Offer
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
    DetailsScreen(state = state, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(state: DetailsUiState, onBack: () -> Unit) {
    Scaffold(
        modifier = Modifier.testTag("details_screen"),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.details_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.desc_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        val offer = state.offer ?: return@Scaffold
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(text = offer.name, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "${stringResource(R.string.label_provider)}: ${offer.providerId}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${stringResource(R.string.label_status)}: ${offer.freeStatus.name}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            item {
                Section(R.string.section_prices) {
                    Text(priceLine(offer.inputPrice, offer.outputPrice, unknown()))
                    Text("${stringResource(R.string.label_context)}: ${offer.contextLength ?: unknown()}  •  " +
                        "${stringResource(R.string.label_output)}: ${offer.maxOutputTokens ?: unknown()}")
                }
            }
            item {
                Section(R.string.section_quotas) {
                    Text(offer.quota ?: unknown())
                }
            }
            offer.conditions?.let { conditions ->
                item {
                    Section(R.string.section_conditions) { Text(conditions) }
                }
            }
            item {
                Section(R.string.section_capabilities) {
                    Text("${stringResource(R.string.label_tools)}: ${yesNoUnknown(offer.supportsTools)}")
                    Text("${stringResource(R.string.label_vision)}: ${yesNoUnknown(offer.supportsVision)}")
                }
            }
            item {
                Section(R.string.section_compat) {
                    Text(if (offer.openCodeCompatible) yes() else no())
                }
            }
            item {
                Section(R.string.provenance_title) {
                    Text("${offer.source}${offer.sourceUrl?.let { " — $it" } ?: ""}")
                    Text(
                        DateUtils.getRelativeTimeSpanString(
                            offer.verifiedAt, System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS
                        ).toString()
                    )
                }
            }
            item {
                Section(R.string.section_history) {
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
private fun Section(titleRes: Int, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        content()
        HorizontalDivider()
    }
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
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = event.type.name, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = DateUtils.getRelativeTimeSpanString(
                event.createdAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
            ).toString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
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
                    officialUrl = null, source = "opencode-data", sourceUrl = null,
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
