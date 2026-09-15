/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.local

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection

/**
 * v1 → v2: absence counter for the Story 2 removal gate. Additive,
 * backfilled 0 (every cached row counts as freshly seen).
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.prepare(
            "ALTER TABLE offer ADD COLUMN missedSyncs INTEGER NOT NULL DEFAULT 0"
        ).use { it.step() }
    }
}
