/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

interface SyncSettings {
    suspend fun wifiOnly(): Boolean
    suspend fun setWifiOnly(enabled: Boolean)
    suspend fun warnOnMetered(): Boolean
    suspend fun setWarnOnMetered(enabled: Boolean)
    suspend fun firstSyncDone(): Boolean
    suspend fun setFirstSyncDone()
}

private const val STORE_NAME = "sync"

private val Context.syncStore by preferencesDataStore(STORE_NAME)

class SyncPrefs(private val context: Context) : SyncSettings {

    private object Keys {
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val WARN_ON_METERED = booleanPreferencesKey("warn_on_metered")
        val FIRST_SYNC_DONE = booleanPreferencesKey("first_sync_done")
    }

    val wifiOnlyFlow: Flow<Boolean> = context.syncStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs -> prefs[Keys.WIFI_ONLY] ?: true }

    override suspend fun wifiOnly(): Boolean = wifiOnlyFlow.first()

    override suspend fun setWifiOnly(enabled: Boolean) {
        context.syncStore.edit { it[Keys.WIFI_ONLY] = enabled }
    }

    override suspend fun warnOnMetered(): Boolean = context.syncStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs -> prefs[Keys.WARN_ON_METERED] ?: true }
        .first()

    override suspend fun setWarnOnMetered(enabled: Boolean) {
        context.syncStore.edit { it[Keys.WARN_ON_METERED] = enabled }
    }

    override suspend fun firstSyncDone(): Boolean = context.syncStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs -> prefs[Keys.FIRST_SYNC_DONE] ?: false }
        .first()

    override suspend fun setFirstSyncDone() {
        context.syncStore.edit { it[Keys.FIRST_SYNC_DONE] = true }
    }
}
