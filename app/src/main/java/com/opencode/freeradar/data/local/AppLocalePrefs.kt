/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private const val STORE_NAME = "locale"
private val Context.localeStore by preferencesDataStore(STORE_NAME)

class AppLocalePrefs(private val context: Context) {

    private object Keys {
        val TAG = stringPreferencesKey("locale_tag")
    }

    val tag: Flow<String> = context.localeStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs -> prefs[Keys.TAG].orEmpty() }

    suspend fun setTag(tag: String) {
        context.localeStore.edit { it[Keys.TAG] = tag }
    }
}
