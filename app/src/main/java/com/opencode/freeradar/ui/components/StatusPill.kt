/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.opencode.freeradar.domain.model.FreeStatus
import com.opencode.freeradar.domain.model.HealthState
import com.opencode.freeradar.ui.theme.AppThemePreview

enum class StatusTone { GOOD, WARNING, BAD, NEUTRAL }

@Composable
private fun StatusTone.container() = when (this) {
    StatusTone.GOOD -> MaterialTheme.colorScheme.primaryContainer
    StatusTone.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
    StatusTone.BAD -> MaterialTheme.colorScheme.errorContainer
    StatusTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant
}

@Composable
private fun StatusTone.content() = when (this) {
    StatusTone.GOOD -> MaterialTheme.colorScheme.onPrimaryContainer
    StatusTone.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
    StatusTone.BAD -> MaterialTheme.colorScheme.onErrorContainer
    StatusTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
}

fun freeStatusTone(status: FreeStatus): StatusTone = when (status) {
    FreeStatus.FREE -> StatusTone.GOOD
    FreeStatus.LIMITED, FreeStatus.TRIAL, FreeStatus.TEMPORARY -> StatusTone.WARNING
    FreeStatus.PAID, FreeStatus.EXPIRED -> StatusTone.BAD
    FreeStatus.UNKNOWN -> StatusTone.NEUTRAL
}

fun healthTone(state: HealthState): StatusTone = when (state) {
    HealthState.HEALTHY -> StatusTone.GOOD
    HealthState.DEGRADED -> StatusTone.WARNING
    HealthState.UNAVAILABLE -> StatusTone.BAD
    HealthState.UNKNOWN -> StatusTone.NEUTRAL
}

@Composable
fun StatusPill(text: String, tone: StatusTone) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(text = text, style = MaterialTheme.typography.labelMedium) },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = tone.container(),
            disabledLabelColor = tone.content()
        ),
        border = null
    )
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StatusPillPreview() {
    AppThemePreview {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill(text = "FREE", tone = StatusTone.GOOD)
            StatusPill(text = "LIMITED", tone = StatusTone.WARNING)
            StatusPill(text = "EXPIRED", tone = StatusTone.BAD)
            StatusPill(text = "UNKNOWN", tone = StatusTone.NEUTRAL)
        }
    }
}
