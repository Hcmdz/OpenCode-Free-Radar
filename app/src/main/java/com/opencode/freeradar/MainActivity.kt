/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar

import android.app.LocaleManager
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import com.opencode.freeradar.ui.theme.ThemeMode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.opencode.freeradar.data.local.AppLocalePrefs
import com.opencode.freeradar.ui.components.UpdateDialog
import com.opencode.freeradar.ui.navigation.AppNavHost
import com.opencode.freeradar.ui.theme.AppTheme
import com.opencode.freeradar.ui.theme.ThemePrefs
import com.opencode.freeradar.ui.theme.ThemeState
import com.opencode.freeradar.util.UpdateManager
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val localePrefs by lazy { AppLocalePrefs(applicationContext) }
    private val updateManager: UpdateManager by inject()

    private var pendingUpdate by mutableStateOf<UpdateManager.UpdateInfo?>(null)
    private var updateDownloadProgress by mutableFloatStateOf(-1f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false
        lifecycleScope.launch {
            localePrefs.tag.collect { tag ->
                if (tag.isNotEmpty()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        getSystemService(LocaleManager::class.java)
                            ?.setApplicationLocales(LocaleList.forLanguageTags(tag))
                    } else {
                        AppCompatDelegate.setApplicationLocales(
                            LocaleListCompat.forLanguageTags(tag)
                        )
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        getSystemService(LocaleManager::class.java)
                            ?.setApplicationLocales(LocaleList())
                    } else {
                        AppCompatDelegate.setApplicationLocales(
                            LocaleListCompat.create()
                        )
                    }
                }
            }
        }
        lifecycleScope.launch {
            if (updateManager.shouldAutoCheck()) {
                when (val result = updateManager.checkForUpdate(BuildConfig.VERSION_NAME)) {
                    is UpdateManager.UpdateResult.Found -> {
                        pendingUpdate = result.info
                        updateManager.recordCheck()
                    }
                    is UpdateManager.UpdateResult.UpToDate -> {
                        updateManager.recordCheck()
                    }
                    is UpdateManager.UpdateResult.Error -> {
                        /* silent — no record */
                    }
                }
            }
        }
        setContent {
            val context = LocalContext.current
            val themePrefs = remember { ThemePrefs(context.applicationContext) }
            val themeState by themePrefs.state.collectAsStateWithLifecycle(
                initialValue = ThemeState()
            )
            val dark = when (themeState.mode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).let {
                    it.isAppearanceLightStatusBars = !dark
                    it.isAppearanceLightNavigationBars = !dark
                }
            }
            AppTheme(state = themeState) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost()
                }
                val update = pendingUpdate
                if (update != null) {
                    UpdateDialog(
                        info = update,
                        downloadProgress = updateDownloadProgress,
                        onDownload = {
                            updateDownloadProgress = 0f
                            val job = lifecycleScope.launch {
                                val file = updateManager.downloadApk(
                                    applicationContext,
                                    update.downloadUrl,
                                    update.fileName,
                                    update.sha256
                                ) { progress ->
                                    updateDownloadProgress = progress
                                }
                                if (file != null) {
                                    updateManager.installApk(applicationContext, file)
                                }
                                pendingUpdate = null
                                updateDownloadProgress = -1f
                                updateManager.downloadJob = null
                            }
                            updateManager.downloadJob = job
                        },
                        onDismiss = {
                            updateManager.cancelDownload()
                            pendingUpdate = null
                            updateDownloadProgress = -1f
                        }
                    )
                }
            }
        }
    }
}
