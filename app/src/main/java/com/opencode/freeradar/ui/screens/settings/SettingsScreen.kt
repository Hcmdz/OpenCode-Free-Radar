/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opencode.freeradar.R
import com.opencode.freeradar.data.local.AppLocalePrefs
import com.opencode.freeradar.ui.components.OptionRow
import com.opencode.freeradar.ui.theme.AppThemePreview
import com.opencode.freeradar.ui.theme.ThemeMode
import com.opencode.freeradar.ui.theme.ThemePrefs
import com.opencode.freeradar.ui.theme.ThemeState
import kotlinx.coroutines.launch

@Composable
fun SettingsRoot(onBack: () -> Unit) {
    val context = LocalContext.current
    val themePrefs = remember { ThemePrefs(context.applicationContext) }
    val localePrefs = remember { AppLocalePrefs(context.applicationContext) }
    val themeState by themePrefs.state.collectAsStateWithLifecycle(initialValue = ThemeState())
    val scope = rememberCoroutineScope()
    val localeTag by localePrefs.tag.collectAsStateWithLifecycle(initialValue = "")
    SettingsScreen(
        themeState = themeState,
        localeTag = localeTag,
        onMode = { scope.launch { themePrefs.setMode(it) } },
        onBlack = { scope.launch { themePrefs.setBlackTheme(it) } },
        onLocale = { scope.launch { localePrefs.setTag(it) } },
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeState: ThemeState,
    localeTag: String,
    onMode: (ThemeMode) -> Unit,
    onBlack: (Boolean) -> Unit,
    onLocale: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        modifier = Modifier.testTag("settings_screen"),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_appearance),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            ThemeMode.entries.forEach { mode ->
                OptionRow(
                    icon = themeIcon(mode),
                    title = mode.name,
                    selected = themeState.mode == mode,
                    onClick = { onMode(mode) }
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.amoled_black)
                )
                Switch(checked = themeState.useBlackTheme, onCheckedChange = onBlack)
            }
            Text(
                text = stringResource(R.string.settings_language),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            LanguageOption(tag = "", labelRes = R.string.lang_system, selectedTag = localeTag, onLocale = onLocale)
            LanguageOption(tag = "en", labelRes = R.string.lang_english, selectedTag = localeTag, onLocale = onLocale)
            LanguageOption(tag = "fr", labelRes = R.string.lang_french, selectedTag = localeTag, onLocale = onLocale)
            LanguageOption(tag = "ar", labelRes = R.string.lang_arabic, selectedTag = localeTag, onLocale = onLocale)
        }
    }
}

@Composable
private fun themeIcon(mode: ThemeMode) = when (mode) {
    ThemeMode.SYSTEM -> Icons.Filled.BrightnessAuto
    ThemeMode.LIGHT -> Icons.Filled.LightMode
    ThemeMode.DARK -> Icons.Filled.DarkMode
}

@Composable
private fun LanguageOption(
    tag: String,
    labelRes: Int,
    selectedTag: String,
    onLocale: (String) -> Unit
) {
    OptionRow(
        icon = Icons.Filled.Translate,
        title = stringResource(labelRes),
        selected = tag == selectedTag,
        onClick = { onLocale(tag) }
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    AppThemePreview {
        SettingsScreen(
            themeState = ThemeState(),
            localeTag = "",
            onMode = {},
            onBlack = {},
            onLocale = {},
            onBack = {}
        )
    }
}
