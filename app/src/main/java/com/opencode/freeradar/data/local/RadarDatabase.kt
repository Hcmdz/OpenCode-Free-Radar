/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.local

import androidx.room3.Database
import androidx.room3.RoomDatabase

@Database(
    entities = [
        OfferEntity::class,
        ChangeEventEntity::class,
        SyncRunEntity::class,
        SourceHealthEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class RadarDatabase : RoomDatabase() {
    abstract fun offerDao(): OfferDao
    abstract fun changeEventDao(): ChangeEventDao
    abstract fun syncRunDao(): SyncRunDao
    abstract fun sourceHealthDao(): SourceHealthDao
}
