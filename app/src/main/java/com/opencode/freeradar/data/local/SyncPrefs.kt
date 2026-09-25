/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * When the periodic worker may hit the network. The mode carries both axes so
 * the WorkManager constraint and the runtime metered guard derive from one
 * value instead of two settings that can disagree.
 */
enum class AutoSync {
    /** Unmetered only: the usual choice, catalog fetches are small. */
    WIFI,
    WIFI_BATTERY,
    /** Metered allowed, device plugged in. */
    BATTERY,
    /** Metered allowed, any power state. */
    ALWAYS;

    /** True when a metered connection must not trigger a fetch. */
    val wifiOnly: Boolean get() = this == WIFI || this == WIFI_BATTERY

    val requiresCharging: Boolean get() = this == WIFI_BATTERY || this == BATTERY

    companion object {
        /** All above the repository's 6 h freshness window, so no option here
         * can be swallowed by the fresh-source gate. */
        val INTERVAL_OPTIONS_HOURS = listOf(8, 12, 24, 48, 72)
        const val DEFAULT_INTERVAL_HOURS = 8

        fun fromName(raw: String?): AutoSync =
            entries.firstOrNull { it.name == raw } ?: WIFI
    }
}

interface SyncSettings {
    suspend fun autoSync(): AutoSync
    suspend fun setAutoSync(mode: AutoSync)
    suspend fun autoSyncIntervalHours(): Int
    suspend fun setAutoSyncIntervalHours(hours: Int)
    suspend fun warnOnMetered(): Boolean
    suspend fun setWarnOnMetered(enabled: Boolean)
    suspend fun firstSyncDone(): Boolean
    suspend fun setFirstSyncDone()
}

private const val STORE_NAME = "sync"

private val Context.syncStore by preferencesDataStore(STORE_NAME)

class SyncPrefs(private val context: Context) : SyncSettings {

    private object Keys {
        val AUTO_SYNC = stringPreferencesKey("auto_sync")
        val AUTO_SYNC_INTERVAL_HOURS = intPreferencesKey("auto_sync_interval_hours")
        val WARN_ON_METERED = booleanPreferencesKey("warn_on_metered")
        val FIRST_SYNC_DONE = booleanPreferencesKey("first_sync_done")
    }

    private val store = context.syncStore.data
        .catch { emit(emptyPreferences()) }

    val autoSyncFlow: Flow<AutoSync> = store.map { prefs ->
        AutoSync.fromName(prefs[Keys.AUTO_SYNC])
    }

    val autoSyncIntervalHoursFlow: Flow<Int> = store.map { prefs ->
        prefs[Keys.AUTO_SYNC_INTERVAL_HOURS] ?: AutoSync.DEFAULT_INTERVAL_HOURS
    }

    override suspend fun autoSync(): AutoSync = autoSyncFlow.first()

    override suspend fun setAutoSync(mode: AutoSync) {
        context.syncStore.edit { it[Keys.AUTO_SYNC] = mode.name }
    }

    override suspend fun autoSyncIntervalHours(): Int = autoSyncIntervalHoursFlow.first()

    override suspend fun setAutoSyncIntervalHours(hours: Int) {
        context.syncStore.edit { it[Keys.AUTO_SYNC_INTERVAL_HOURS] = hours }
    }

    override suspend fun warnOnMetered(): Boolean = store
        .map { prefs -> prefs[Keys.WARN_ON_METERED] ?: true }
        .first()

    override suspend fun setWarnOnMetered(enabled: Boolean) {
        context.syncStore.edit { it[Keys.WARN_ON_METERED] = enabled }
    }

    override suspend fun firstSyncDone(): Boolean = store
        .map { prefs -> prefs[Keys.FIRST_SYNC_DONE] ?: false }
        .first()

    override suspend fun setFirstSyncDone() {
        context.syncStore.edit { it[Keys.FIRST_SYNC_DONE] = true }
    }
}
