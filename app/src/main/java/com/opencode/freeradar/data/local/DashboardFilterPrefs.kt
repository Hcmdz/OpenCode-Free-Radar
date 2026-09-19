/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private const val STORE_NAME = "dashboard-filters"

/** Unit separator: search queries are single-line, so this never collides. */
private const val RECENTS_SEPARATOR = "\u001F"

private val Context.dashboardFilterStore by preferencesDataStore(STORE_NAME)

/**
 * Last dashboard state (filters, sort, local switch, recent searches).
 * Primitives only — the UI layer maps enum names, so an unknown name
 * (renamed enum) falls back to the current defaults instead of crashing.
 */
interface DashboardFilterPrefs {
    val filterName: Flow<String?>
    val sourceName: Flow<String?>
    val sortName: Flow<String?>
    val showLocal: Flow<Boolean>
    val recentQueries: Flow<List<String>>
    suspend fun save(
        filterName: String,
        sourceName: String,
        sortName: String,
        showLocal: Boolean,
        recentQueries: List<String>
    )
}

class DataStoreDashboardFilterPrefs(private val context: Context) : DashboardFilterPrefs {

    private object Keys {
        val FILTER = stringPreferencesKey("filter")
        val SOURCE = stringPreferencesKey("source")
        val SORT = stringPreferencesKey("sort")
        val SHOW_LOCAL = booleanPreferencesKey("show_local")
        val RECENTS = stringPreferencesKey("recents")
    }

    private val data = context.dashboardFilterStore.data
        .catch { emit(emptyPreferences()) }

    override val filterName: Flow<String?> = data.map { it[Keys.FILTER] }
    override val sourceName: Flow<String?> = data.map { it[Keys.SOURCE] }
    override val sortName: Flow<String?> = data.map { it[Keys.SORT] }
    override val showLocal: Flow<Boolean> = data.map { it[Keys.SHOW_LOCAL] ?: false }
    override val recentQueries: Flow<List<String>> = data.map {
        val raw = it[Keys.RECENTS].orEmpty()
        if (raw.isEmpty()) emptyList() else raw.split(RECENTS_SEPARATOR).filter { q -> q.isNotEmpty() }
    }

    override suspend fun save(
        filterName: String,
        sourceName: String,
        sortName: String,
        showLocal: Boolean,
        recentQueries: List<String>
    ) {
        context.dashboardFilterStore.edit {
            it[Keys.FILTER] = filterName
            it[Keys.SOURCE] = sourceName
            it[Keys.SORT] = sortName
            it[Keys.SHOW_LOCAL] = showLocal
            it[Keys.RECENTS] = recentQueries.joinToString(RECENTS_SEPARATOR)
        }
    }
}
