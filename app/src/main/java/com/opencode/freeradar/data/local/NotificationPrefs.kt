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
        val PERMISSION_ASKED = booleanPreferencesKey("permission_asked")
    }

    val enabled: Flow<Boolean> = context.notificationStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs -> prefs[Keys.ENABLED] ?: true }

    /**
     * Asked once per install: the OS only shows the system dialog the first
     * time, so re-asking later is a silent no-op and a nagging prompt instead.
     */
    val permissionAsked: Flow<Boolean> = context.notificationStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs -> prefs[Keys.PERMISSION_ASKED] ?: false }

    suspend fun setPermissionAsked() {
        context.notificationStore.edit { it[Keys.PERMISSION_ASKED] = true }
    }

    suspend fun setEnabled(enabled: Boolean) {
        context.notificationStore.edit { it[Keys.ENABLED] = enabled }
    }
}
