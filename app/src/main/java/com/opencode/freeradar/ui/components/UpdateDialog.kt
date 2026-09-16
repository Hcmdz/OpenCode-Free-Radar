/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opencode.freeradar.R
import com.opencode.freeradar.util.UpdateManager

@Composable
fun UpdateDialog(
    info: UpdateManager.UpdateInfo,
    downloadProgress: Float,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (downloadProgress < 0f) onDismiss() },
        title = {
            Text(
                text = stringResource(R.string.update_available_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.update_available_body, info.versionName, info.releaseNotes),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (downloadProgress >= 0f) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.update_downloading, (downloadProgress * 100).toInt()),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            if (downloadProgress < 0f) {
                TextButton(onClick = onDownload) {
                    Text(stringResource(R.string.update_download))
                }
            }
        },
        dismissButton = {
            if (downloadProgress < 0f) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.update_later))
                }
            }
        }
    )
}
