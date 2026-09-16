/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.opencode.freeradar.R
import com.opencode.freeradar.ui.model.OfferUi
import com.opencode.freeradar.ui.model.sourceLabel

@Composable
fun OfferCard(offer: OfferUi, onClick: () -> Unit, onToggleFavorite: () -> Unit) {
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(text = offer.freeStatus.name, tone = freeStatusTone(offer.freeStatus))
                StatusPill(text = sourceLabel(offer.source), tone = StatusTone.NEUTRAL)
                if (offer.unverified) {
                    StatusPill(text = stringResource(R.string.status_unverified), tone = StatusTone.NEUTRAL)
                }
                offer.contextLength?.let {
                    AssistChip(onClick = {}, label = { Text("$it tokens") })
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    modifier = Modifier.testTag("offer_favorite"),
                    onClick = onToggleFavorite
                ) {
                    Icon(
                        imageVector = if (offer.favorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = stringResource(
                            if (offer.favorite) R.string.desc_unfavorite else R.string.desc_favorite
                        ),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
