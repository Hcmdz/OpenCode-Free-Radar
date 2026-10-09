/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.opencode.freeradar.R

/**
 * Third-party licence notices. The body is legal text, so it stays English in
 * `res/raw` and only the surrounding labels are translated.
 */
@Composable
fun OpenSourceNoticesDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val notices = remember(context) {
        runCatching {
            context.resources.openRawResource(R.raw.open_source_notices)
                .bufferedReader()
                .use { it.readText() }
        }.getOrDefault("")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.about_open_source_notices),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = notices,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .testTag("open_source_notices_body")
            )
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("open_source_notices_close")
            ) {
                Text(stringResource(R.string.action_close))
            }
        }
    )
}