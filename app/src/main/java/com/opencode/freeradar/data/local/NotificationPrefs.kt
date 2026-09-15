/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private const val STORE_NAME = "notifications"
private val Context.notificationStore by preferencesDataStore(STORE_NAME)

class NotificationPrefs(private val context: Context) {

    private object Keys {
        val ENABLED = booleanPreferencesKey("enabled")
    }

    val enabled: Flow<Boolean> = context.notificationStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs -> prefs[Keys.ENABLED] ?: false }

    suspend fun setEnabled(enabled: Boolean) {
        context.notificationStore.edit { it[Keys.ENABLED] = enabled }
    }
}
