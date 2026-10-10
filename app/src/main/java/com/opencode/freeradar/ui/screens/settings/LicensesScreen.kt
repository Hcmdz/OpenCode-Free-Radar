/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import android.util.Log
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.opencode.freeradar.R
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One row per resolved runtime artifact, pointing at a shared licence body. */
@Serializable
data class ThirdPartyEntry(
    val group: String = "",
    val artifact: String = "",
    val version: String = "",
    val label: String = "",
    val license: String = ""
)

private const val APACHE = "apache_2_0.txt"
private const val MIT = "mit.txt"
private const val BSD = "bsd_3_clause.txt"

/**
 * Third-party notices, grouped by licence then by coordinate. The index is
 * generated from the resolved dependency graph, so no row can name a dependency
 * the app no longer ships, and every licence points at a body we ship.
 */
@Composable
fun LicensesRoot(onBack: () -> Unit) {
    val context = LocalContext.current
    val entries = remember(context) {
        runCatching {
            val text = context.resources.openRawResource(R.raw.third_party)
                .bufferedReader().use { it.readText() }
            Json.decodeFromString<List<ThirdPartyEntry>>(text)
        }.onFailure { Log.e("Licenses", "cannot read the notices index", it) }
            .getOrDefault(emptyList())
    }
    LicensesScreen(entries = entries, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(entries: List<ThirdPartyEntry>, onBack: () -> Unit) {
    var openLicense by rememberSaveable { mutableStateOf<String?>(null) }
    val current = openLicense
    if (current != null) {
        LicenseBodyScreen(license = current, onBack = { openLicense = null })
        return
    }
    LicensesList(
        entries = entries,
        onBack = onBack,
        onOpenLicense = { openLicense = it }
    )
}

/**
 * Artefact labels and Maven coordinates are Latin technical identifiers, so the rows are forced LTR
 * like the licence body; only the translated top bar title keeps the locale direction.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LicensesList(
    entries: List<ThirdPartyEntry>,
    onBack: () -> Unit,
    onOpenLicense: (String) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .testTag("licenses_screen"),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_open_source_notices)) },
                navigationIcon = { BackButton(onBack) },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        val grouped = entries
            .sortedWith(compareBy({ it.license }, { it.group }, { it.artifact }))
            .groupBy { it.license }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .testTag("licenses_list"),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                grouped.forEach { (license, items) ->
                    item(key = "header-$license") {
                        Text(
                            text = licenseLabel(license),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp)
                                .testTag("licenses_group_${license.substringBefore('.')}")
                        )
                    }
                    items(items, key = { "${it.group}:${it.artifact}" }) { entry ->
                        LicenseRow(entry = entry, onOpen = { onOpenLicense(license) })
                    }
                }
            }
        }
    }
}

@Composable
private fun LicenseRow(entry: ThirdPartyEntry, onOpen: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Tag on the clickable node, not the inner Text: clickable merges
            // descendants' semantics away, so a tag below it is invisible to tests
            // and to assistive tech. Group-qualified because two artifacts can share
            // a name across groups.
            .clickable(onClick = onOpen)
            .testTag("license_row_${entry.group}_${entry.artifact}")
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(
                text = entry.label.ifBlank { entry.artifact },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )
            Text(
                text = "${entry.group}:${entry.artifact}:${entry.version}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
        HorizontalDivider()
    }
}

/**
 * The licence body. Kept in English: it is legal text, not UI copy, so the paragraph is forced LTR
 * rather than inheriting the locale direction and reflowing flush right in an RTL locale.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LicenseBodyScreen(license: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val body = remember(license) {
        runCatching {
            context.resources.openRawResource(
                when (license) {
                    APACHE -> R.raw.apache_2_0
                    BSD -> R.raw.bsd_3_clause
                    else -> R.raw.mit
                }
            ).bufferedReader().use { it.readText() }
        }.getOrDefault("")
    }
    Scaffold(
        modifier = Modifier.testTag("license_body_screen"),
        topBar = {
            TopAppBar(
                title = { Text(licenseLabel(license)) },
                navigationIcon = { BackButton(onBack) }
            )
        }
    ) { padding ->
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Text(
                text = remember(body) { reflowParagraphs(body) },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
                    .testTag("license_body_text")
            )
        }
    }
}

/**
 * Licence bodies arrive pre-wrapped near 70 columns, wider than a phone screen in a proportional
 * font, so the layout re-wraps every source line and leaves an orphan fragment behind it. Collapse
 * each paragraph to a single line and let the layout wrap it once, cleanly. Whitespace only: no
 * word of the licence changes and the raw resources stay byte-identical.
 */
private fun reflowParagraphs(body: String): String =
    body.split(Regex("\n[ \t]*\n")).joinToString("\n\n") { it.replace(Regex("\\s+"), " ").trim() }

@Composable
private fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.desc_back)
        )
    }
}

internal fun licenseLabel(file: String): String = when (file) {
    APACHE -> "Apache License 2.0"
    MIT -> "MIT License"
    BSD -> "BSD 3-Clause License"
    else -> file
}
