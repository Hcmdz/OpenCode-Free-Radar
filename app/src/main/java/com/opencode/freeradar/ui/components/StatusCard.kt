/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.opencode.freeradar.ui.theme.AppThemePreview
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle

@Composable
fun StatusCard(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    pill: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    body: (@Composable ColumnScope.() -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        onClick = onClick ?: {}
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium
                )
                pill?.invoke()
            }
            body?.invoke(this)
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "RTL", showBackground = true, locale = "ar")
@Composable
private fun StatusCardPreview() {
    AppThemePreview {
        StatusCard(
            icon = Icons.Filled.CheckCircle,
            title = "Source health",
            pill = { StatusPill(text = "HEALTHY", tone = StatusTone.GOOD) }
        ) {
            Text(
                text = "All sources verified recently.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
