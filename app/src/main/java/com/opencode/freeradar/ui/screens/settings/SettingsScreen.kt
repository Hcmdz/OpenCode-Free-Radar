/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings as SystemSettings
import com.opencode.freeradar.BuildConfig
import com.opencode.freeradar.R
import com.opencode.freeradar.data.local.AppLocalePrefs
import com.opencode.freeradar.data.local.FilterFabPrefs
import com.opencode.freeradar.data.local.NotificationPrefs
import com.opencode.freeradar.data.local.SyncPrefs
import com.opencode.freeradar.ui.components.OptionRow
import com.opencode.freeradar.ui.components.UpdateDialog
import com.opencode.freeradar.util.UpdateManager
import org.koin.compose.koinInject
import com.opencode.freeradar.ui.theme.AppThemePreview
import com.opencode.freeradar.ui.theme.ThemeMode
import com.opencode.freeradar.ui.theme.ThemePrefs
import com.opencode.freeradar.ui.theme.ThemeState
import kotlinx.coroutines.launch

@Composable
fun SettingsRoot(onBack: () -> Unit) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val themePrefs = remember { ThemePrefs(appContext) }
    val localePrefs = remember { AppLocalePrefs(appContext) }
    val notifPrefs = remember { NotificationPrefs(appContext) }
    val syncPrefs = remember { SyncPrefs(appContext) }
    val fabPrefs = remember { FilterFabPrefs(appContext) }
    val themeState by themePrefs.state.collectAsStateWithLifecycle(initialValue = ThemeState())
    val scope = rememberCoroutineScope()
    val localeTag by localePrefs.tag.collectAsStateWithLifecycle(initialValue = "")
    val notifEnabled by notifPrefs.enabled.collectAsStateWithLifecycle(initialValue = false)
    val wifiOnly by syncPrefs.wifiOnlyFlow.collectAsStateWithLifecycle(initialValue = true)
    val peekDelayMs by fabPrefs.peekDelayMillis.collectAsStateWithLifecycle(
        initialValue = FilterFabPrefs.DEFAULT_DELAY_MILLIS
    )
    val peekSliverDp by fabPrefs.peekSliverDp.collectAsStateWithLifecycle(
        initialValue = FilterFabPrefs.DEFAULT_SLIVER_DP
    )
    var notifDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifDenied = !granted
        scope.launch { notifPrefs.setEnabled(granted) }
    }
    val updateManager: UpdateManager = koinInject()
    var updateRowRes by remember { mutableIntStateOf(R.string.update_check) }
    var updateInfo by remember { mutableStateOf<UpdateManager.UpdateInfo?>(null) }
    var showUpdateDialog by rememberSaveable { mutableStateOf(false) }
    var updateProgress by remember { mutableFloatStateOf(-1f) }
    fun launchUpdateCheck() {
        scope.launch {
            updateRowRes = R.string.update_checking
            when (val result = updateManager.checkForUpdate(BuildConfig.VERSION_NAME)) {
                is UpdateManager.UpdateResult.Found -> {
                    updateInfo = result.info
                    showUpdateDialog = true
                    updateManager.recordCheck()
                    updateRowRes = R.string.update_check
                }
                is UpdateManager.UpdateResult.UpToDate -> {
                    updateManager.recordCheck()
                    updateRowRes = R.string.update_up_to_date
                }
                is UpdateManager.UpdateResult.Error -> {
                    updateRowRes = R.string.update_check_failed
                }
            }
        }
    }
    SettingsScreen(
        themeState = themeState,
        localeTag = localeTag,
        notifEnabled = notifEnabled,
        notifDenied = notifDenied,
        wifiOnly = wifiOnly,
        peekDelayMs = peekDelayMs,
        peekSliverDp = peekSliverDp,
        onMode = { scope.launch { themePrefs.setMode(it) } },
        onBlack = { scope.launch { themePrefs.setBlackTheme(it) } },
        onLocale = { scope.launch { localePrefs.setTag(it) } },
        onNotifToggle = { enabled ->
            if (!enabled) {
                notifDenied = false
                scope.launch { notifPrefs.setEnabled(false) }
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    appContext, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                scope.launch { notifPrefs.setEnabled(true) }
            } else {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onWifiOnlyToggle = { scope.launch { syncPrefs.setWifiOnly(it) } },
        onPeekDelay = { scope.launch { fabPrefs.setPeekDelayMillis(it) } },
        onPeekSliver = { scope.launch { fabPrefs.setPeekSliverDp(it) } },
        onOpenNotifSettings = {
            context.startActivity(
                Intent(SystemSettings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(SystemSettings.EXTRA_APP_PACKAGE, context.packageName)
                }
            )
        },
        onOpenLink = { url ->
            context.startActivity(
                Intent(
                    if (url.startsWith("mailto:")) Intent.ACTION_SENDTO else Intent.ACTION_VIEW,
                    Uri.parse(url)
                )
            )
        },
        updateRowText = stringResource(updateRowRes),
        onCheckUpdate = { launchUpdateCheck() },
        onBack = onBack
    )
    val info = updateInfo
    if (showUpdateDialog && info != null) {
        UpdateDialog(
            info = info,
            downloadProgress = updateProgress,
            onDownload = {
                updateProgress = 0f
                val job = scope.launch {
                    val file = updateManager.downloadApk(
                        appContext,
                        info.downloadUrl,
                        info.fileName,
                        info.sha256
                    ) { progress ->
                        updateProgress = progress
                    }
                    if (file != null) {
                        updateManager.installApk(appContext, file)
                        updateRowRes = R.string.update_up_to_date
                    } else {
                        updateRowRes = R.string.update_check_failed
                    }
                    showUpdateDialog = false
                    updateProgress = -1f
                    updateManager.downloadJob = null
                }
                updateManager.downloadJob = job
            },
            onDismiss = {
                updateManager.cancelDownload()
                showUpdateDialog = false
                updateProgress = -1f
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeState: ThemeState,
    localeTag: String,
    notifEnabled: Boolean,
    notifDenied: Boolean,
    wifiOnly: Boolean,
    peekDelayMs: Long = FilterFabPrefs.DEFAULT_DELAY_MILLIS,
    peekSliverDp: Int = FilterFabPrefs.DEFAULT_SLIVER_DP,
    onMode: (ThemeMode) -> Unit,
    onBlack: (Boolean) -> Unit,
    onLocale: (String) -> Unit,
    onNotifToggle: (Boolean) -> Unit,
    onWifiOnlyToggle: (Boolean) -> Unit,
    onPeekDelay: (Long) -> Unit = {},
    onPeekSliver: (Int) -> Unit = {},
    onOpenNotifSettings: () -> Unit,
    onOpenLink: (String) -> Unit,
    updateRowText: String,
    onCheckUpdate: () -> Unit,
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
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CollapsibleSection(titleRes = R.string.settings_appearance) {
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
            }
            CollapsibleSection(titleRes = R.string.settings_language) {
                LanguageOption(tag = "", labelRes = R.string.lang_system, selectedTag = localeTag, onLocale = onLocale)
                LanguageOption(tag = "en", labelRes = R.string.lang_english, selectedTag = localeTag, onLocale = onLocale)
                LanguageOption(tag = "fr", labelRes = R.string.lang_french, selectedTag = localeTag, onLocale = onLocale)
                LanguageOption(tag = "ar", labelRes = R.string.lang_arabic, selectedTag = localeTag, onLocale = onLocale)
            }
            CollapsibleSection(titleRes = R.string.settings_notifications) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = null
                    )
                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.notif_enable)
                    )
                    Switch(
                        modifier = Modifier.testTag("settings_notifications_switch"),
                        checked = notifEnabled,
                        onCheckedChange = onNotifToggle
                    )
                }
                Text(
                    text = stringResource(R.string.notif_benefit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (notifDenied) {
                    Text(
                        text = stringResource(R.string.notif_denied_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    TextButton(onClick = onOpenNotifSettings) {
                        Text(text = stringResource(R.string.notif_open_settings))
                    }
                }
            }
            CollapsibleSection(titleRes = R.string.settings_sync) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Sync,
                        contentDescription = null
                    )
                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.sync_wifi_only)
                    )
                    Switch(
                        modifier = Modifier.testTag("settings_sync_switch"),
                        checked = wifiOnly,
                        onCheckedChange = onWifiOnlyToggle
                    )
                }
                Text(
                    text = stringResource(R.string.sync_wifi_benefit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            CollapsibleSection(titleRes = R.string.settings_filter_button) {
                Text(
                    text = stringResource(R.string.filter_peek_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.filter_hide_delay),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                PeekDelayOption(
                    delayMs = 3_000L,
                    labelRes = R.string.filter_delay_3s,
                    selectedDelayMs = peekDelayMs,
                    onSelect = onPeekDelay
                )
                PeekDelayOption(
                    delayMs = 5_000L,
                    labelRes = R.string.filter_delay_5s,
                    selectedDelayMs = peekDelayMs,
                    onSelect = onPeekDelay
                )
                PeekDelayOption(
                    delayMs = 8_000L,
                    labelRes = R.string.filter_delay_8s,
                    selectedDelayMs = peekDelayMs,
                    onSelect = onPeekDelay
                )
                Text(
                    text = stringResource(R.string.filter_edge_size),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                PeekSliverOption(
                    sliverDp = 16,
                    labelRes = R.string.filter_sliver_16dp,
                    selectedSliverDp = peekSliverDp,
                    onSelect = onPeekSliver
                )
                PeekSliverOption(
                    sliverDp = 24,
                    labelRes = R.string.filter_sliver_24dp,
                    selectedSliverDp = peekSliverDp,
                    onSelect = onPeekSliver
                )
                PeekSliverOption(
                    sliverDp = 32,
                    labelRes = R.string.filter_sliver_32dp,
                    selectedSliverDp = peekSliverDp,
                    onSelect = onPeekSliver
                )
            }
            CollapsibleSection(titleRes = R.string.settings_about) {
                AboutRow(
                    icon = Icons.Filled.Person,
                    text = "${stringResource(R.string.about_developer)}: HcmDZ",
                    onClick = null
                )
                AboutRow(
                    icon = Icons.Filled.Email,
                    text = "HcmDz.Dev@gmail.com",
                    onClick = { onOpenLink("mailto:HcmDz.Dev@gmail.com") }
                )
                AboutRow(
                    icon = Icons.Filled.Link,
                    text = "github.com/Hcmdz/OpenCode-Free-Radar",
                    onClick = { onOpenLink("https://github.com/Hcmdz/OpenCode-Free-Radar") }
                )
                AboutRow(
                    icon = Icons.Filled.PrivacyTip,
                    text = stringResource(R.string.about_privacy),
                    onClick = { onOpenLink("https://hcmdz.github.io/OpenCode-Free-Radar/privacy/") }
                )
                AboutRow(
                    icon = Icons.Filled.Description,
                    text = stringResource(R.string.about_terms),
                    onClick = { onOpenLink("https://hcmdz.github.io/OpenCode-Free-Radar/terms/") }
                )
                AboutRow(
                    icon = Icons.Filled.Info,
                    text = "${stringResource(R.string.about_version)}: ${BuildConfig.VERSION_NAME}",
                    onClick = null
                )
                AboutRow(
                    icon = Icons.Filled.SystemUpdate,
                    text = updateRowText,
                    onClick = onCheckUpdate
                )
            }
        }
    }
}

/**
 * One collapsible settings group. Expanded by default (first paint matches
 * the previous always-open layout); collapse state is local UI state that
 * survives rotation. Header meets the 48dp touch target and exposes heading
 * + expanded state to screen readers (never color/icon alone).
 */
@Composable
private fun CollapsibleSection(
    @StringRes titleRes: Int,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val stateLabel = stringResource(
        if (expanded) R.string.state_expanded else R.string.state_collapsed
    )
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = { expanded = !expanded })
                .semantics(mergeDescendants = true) {
                    heading()
                    stateDescription = stateLabel
                }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun AboutRow(
    icon: ImageVector,
    text: String,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
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

@Composable
private fun PeekDelayOption(
    delayMs: Long,
    labelRes: Int,
    selectedDelayMs: Long,
    onSelect: (Long) -> Unit
) {
    OptionRow(
        icon = Icons.Filled.Timer,
        title = stringResource(labelRes),
        selected = delayMs == selectedDelayMs,
        onClick = { onSelect(delayMs) }
    )
}

@Composable
private fun PeekSliverOption(
    sliverDp: Int,
    labelRes: Int,
    selectedSliverDp: Int,
    onSelect: (Int) -> Unit
) {
    OptionRow(
        icon = Icons.Filled.Visibility,
        title = stringResource(labelRes),
        selected = sliverDp == selectedSliverDp,
        onClick = { onSelect(sliverDp) }
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    AppThemePreview {
        SettingsScreen(
            themeState = ThemeState(),
            localeTag = "",
            notifEnabled = false,
            notifDenied = false,
            wifiOnly = true,
            onMode = {},
            onBlack = {},
            onLocale = {},
            onNotifToggle = {},
            onWifiOnlyToggle = {},
            onOpenNotifSettings = {},
            onOpenLink = {},
            updateRowText = "Check for updates",
            onCheckUpdate = {},
            onBack = {}
        )
    }
}
