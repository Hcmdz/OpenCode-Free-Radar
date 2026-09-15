/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.local

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "sync_run",
    indices = [Index(value = ["source"])]
)
data class SyncRunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val source: String,
    val startedAt: Long,
    val completedAt: Long?,
    val result: String?,
    val error: String?
)

@Entity(tableName = "source_health")
data class SourceHealthEntity(
    @PrimaryKey val source: String,
    val state: String,
    val checkedAt: Long
)
