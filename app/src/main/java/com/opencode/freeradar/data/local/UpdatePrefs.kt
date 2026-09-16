/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.opencode.freeradar.util.UpdateCheckStore

private const val STORE_NAME = "updates"
private val Context.updateStore by preferencesDataStore(STORE_NAME)

class UpdatePrefs(private val context: Context) : UpdateCheckStore {

    private object Keys {
        val LAST_CHECK = longPreferencesKey("last_check_timestamp")
    }

    override suspend fun lastCheck(): Long = lastCheckFlow.first()

    override suspend fun record(timestamp: Long) {
        setLastCheck(timestamp)
    }

    val lastCheckFlow: Flow<Long> = context.updateStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs -> prefs[Keys.LAST_CHECK] ?: 0L }

    suspend fun setLastCheck(timestamp: Long) {
        context.updateStore.edit { it[Keys.LAST_CHECK] = timestamp }
    }
}
