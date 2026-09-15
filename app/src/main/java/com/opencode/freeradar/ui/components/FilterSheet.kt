/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.opencode.freeradar.R
import com.opencode.freeradar.ui.model.OfferFilter
import com.opencode.freeradar.ui.model.SourceFilter
import com.opencode.freeradar.ui.theme.AppThemePreview

/**
 * Single entry point to both facets. [summary] is "Filters" at defaults,
 * otherwise the active selections joined ("Free • NVIDIA", Baymard overview).
 */
@Composable
fun FilterBarButton(
    summary: String,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onOpen,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Filled.Tune,
            contentDescription = null
        )
        Text(text = summary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun activeSummary(
    filter: OfferFilter,
    source: SourceFilter,
    filterLabels: Map<OfferFilter, String>,
    sourceLabels: Map<SourceFilter, String>,
    defaultTitle: String
): String {
    val parts = buildList<String> {
        if (filter != OfferFilter.FREE) add(filterLabels.getValue(filter))
        if (source != SourceFilter.ALL_SOURCES) add(sourceLabels.getValue(source))
    }
    return parts.ifEmpty { listOf(defaultTitle) }.joinToString(" • ")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    visible: Boolean,
    filter: OfferFilter,
    sourceFilter: SourceFilter,
    statusCounts: Map<OfferFilter, Int>,
    sourceCounts: Map<SourceFilter, Int>,
    showReset: Boolean,
    onSelectFilter: (OfferFilter) -> Unit,
    onSelectSource: (SourceFilter) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.section_status),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Column(Modifier.selectableGroup()) {
                OfferFilter.entries.forEach { entry ->
                    val selected = entry == filter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selected,
                                role = Role.RadioButton,
                                onClick = { onSelectFilter(entry) }
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(selected = selected, onClick = null)
                        Text(
                            text = "${stringResource(entry.labelRes)} (${statusCounts[entry] ?: 0})"
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.section_source),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
            Column(Modifier.selectableGroup()) {
                SourceFilter.entries.forEach { entry ->
                    val selected = entry == sourceFilter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selected,
                                role = Role.RadioButton,
                                onClick = { onSelectSource(entry) }
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(selected = selected, onClick = null)
                        Text(
                            text = "${stringResource(entry.labelRes)} (${sourceCounts[entry] ?: 0})"
                        )
                    }
                }
            }
            if (showReset) {
                TextButton(
                    onClick = onReset,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(text = stringResource(R.string.reset_filters))
                }
            }
        }
    }
}

/**
 * Dashboard wiring: button + sheet sharing one local visibility state.
 * Pure UI state stays in remember; filter values come from the ViewModel.
 */
@Composable
fun FilterBar(
    filter: OfferFilter,
    sourceFilter: SourceFilter,
    statusCounts: Map<OfferFilter, Int>,
    sourceCounts: Map<SourceFilter, Int>,
    showReset: Boolean,
    onSelectFilter: (OfferFilter) -> Unit,
    onSelectSource: (SourceFilter) -> Unit,
    onReset: () -> Unit,
    onOpenSheet: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    val title = stringResource(R.string.filter_title)
    FilterBarButton(
        summary = activeSummary(
            filter = filter,
            source = sourceFilter,
            filterLabels = OfferFilter.entries.associateWith { stringResource(it.labelRes) },
            sourceLabels = SourceFilter.entries.associateWith { stringResource(it.labelRes) },
            defaultTitle = title
        ),
        onOpen = {
            onOpenSheet()
            open = true
        },
        modifier = modifier
    )
    FilterSheet(
        visible = open,
        filter = filter,
        sourceFilter = sourceFilter,
        statusCounts = statusCounts,
        sourceCounts = sourceCounts,
        showReset = showReset,
        onSelectFilter = onSelectFilter,
        onSelectSource = onSelectSource,
        onReset = onReset,
        onDismiss = { open = false }
    )
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FilterBarPreview() {
    AppThemePreview {
        FilterBar(
            filter = OfferFilter.FREE,
            sourceFilter = SourceFilter.OPENCODE,
            statusCounts = OfferFilter.entries.associateWith { 12 },
            sourceCounts = SourceFilter.entries.associateWith { 7 },
            showReset = true,
            onSelectFilter = {},
            onSelectSource = {},
            onReset = {}
        )
    }
}
