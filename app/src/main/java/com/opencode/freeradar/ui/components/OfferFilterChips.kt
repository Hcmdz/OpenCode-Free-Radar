/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.opencode.freeradar.ui.model.OfferFilter
import com.opencode.freeradar.ui.theme.AppThemePreview

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OfferFilterChips(
    selected: OfferFilter,
    onSelect: (OfferFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        OfferFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelect(filter) },
                label = { Text(text = stringResource(filter.labelRes)) }
            )
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OfferFilterChipsPreview() {
    AppThemePreview {
        OfferFilterChips(selected = OfferFilter.FREE, onSelect = {})
    }
}
