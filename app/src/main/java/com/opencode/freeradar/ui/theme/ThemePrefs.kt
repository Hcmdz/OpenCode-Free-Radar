/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private const val STORE_NAME = "theme"
private val Context.themeStore by preferencesDataStore(STORE_NAME)

class ThemePrefs(private val context: Context) {

    private object Keys {
        val MODE = stringPreferencesKey("mode")
        val BLACK = booleanPreferencesKey("black")
        val SOURCE = stringPreferencesKey("source")
    }

    val state: Flow<ThemeState> = context.themeStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs ->
            ThemeState(
                mode = runCatching {
                    ThemeMode.valueOf(prefs[Keys.MODE] ?: ThemeMode.SYSTEM.name)
                }.getOrDefault(ThemeMode.SYSTEM),
                useBlackTheme = prefs[Keys.BLACK] ?: false,
                colorSource = runCatching {
                    ColorSource.valueOf(prefs[Keys.SOURCE] ?: ColorSource.MATERIAL_YOU.name)
                }.getOrDefault(ColorSource.MATERIAL_YOU)
            )
        }

    suspend fun setMode(mode: ThemeMode) {
        context.themeStore.edit { it[Keys.MODE] = mode.name }
    }

    suspend fun setBlackTheme(enabled: Boolean) {
        context.themeStore.edit { it[Keys.BLACK] = enabled }
    }

    suspend fun setColorSource(source: ColorSource) {
        context.themeStore.edit { it[Keys.SOURCE] = source.name }
    }
}
