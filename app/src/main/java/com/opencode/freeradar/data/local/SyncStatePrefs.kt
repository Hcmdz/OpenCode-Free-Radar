/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

interface SyncStateStore {
    suspend fun bodyHash(source: String): String?
    suspend fun recordHash(source: String, hash: String)
}

private const val STORE_NAME = "sync_state"

private val Context.syncStateStore by preferencesDataStore(STORE_NAME)

class SyncStatePrefs(private val context: Context) : SyncStateStore {

    override suspend fun bodyHash(source: String): String? =
        context.syncStateStore.data
            .map { prefs -> prefs[stringPreferencesKey("body_hash_$source")] }
            .first()

    override suspend fun recordHash(source: String, hash: String) {
        context.syncStateStore.edit { it[stringPreferencesKey("body_hash_$source")] = hash }
    }
}
