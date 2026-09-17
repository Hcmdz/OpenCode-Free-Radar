/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val STORE_NAME = "filter_fab"
private val Context.filterFabStore by preferencesDataStore(STORE_NAME)

/**
 * Filter FAB peek behavior: idle delay before edge-peek and visible sliver
 * size, both user-configurable. Bounds mirror the ramblr pattern the peek
 * math follows (delay AND sliver clamped, never trusted raw).
 */
class FilterFabPrefs(private val context: Context) {

    companion object {
        const val DEFAULT_DELAY_MILLIS = 5_000L
        val DELAY_OPTIONS_MILLIS = listOf(3_000L, 5_000L, 8_000L)
        const val DEFAULT_SLIVER_DP = 24
        val SLIVER_OPTIONS_DP = listOf(16, 24, 32)
        const val MIN_SLIVER_DP = 8
        const val MAX_SLIVER_DP = 40
    }

    private object Keys {
        val PEEK_DELAY = longPreferencesKey("peek_delay_millis")
        val PEEK_SLIVER = intPreferencesKey("peek_sliver_dp")
    }

    val peekDelayMillis: Flow<Long> = context.filterFabStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs ->
            (prefs[Keys.PEEK_DELAY] ?: DEFAULT_DELAY_MILLIS)
                .coerceIn(DELAY_OPTIONS_MILLIS.min(), DELAY_OPTIONS_MILLIS.max())
        }

    val peekSliverDp: Flow<Int> = context.filterFabStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs ->
            (prefs[Keys.PEEK_SLIVER] ?: DEFAULT_SLIVER_DP).coerceIn(MIN_SLIVER_DP, MAX_SLIVER_DP)
        }

    suspend fun peekDelayMillis(): Long = peekDelayMillis.first()

    suspend fun peekSliverDp(): Int = peekSliverDp.first()

    suspend fun setPeekDelayMillis(delayMillis: Long) {
        context.filterFabStore.edit {
            it[Keys.PEEK_DELAY] =
                delayMillis.coerceIn(DELAY_OPTIONS_MILLIS.min(), DELAY_OPTIONS_MILLIS.max())
        }
    }

    suspend fun setPeekSliverDp(sliverDp: Int) {
        context.filterFabStore.edit {
            it[Keys.PEEK_SLIVER] = sliverDp.coerceIn(MIN_SLIVER_DP, MAX_SLIVER_DP)
        }
    }
}
