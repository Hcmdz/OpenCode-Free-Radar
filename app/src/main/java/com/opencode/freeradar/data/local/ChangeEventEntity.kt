/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.local

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

// Intentionally FK-free: the event log outlives offers. A cascade would wipe
// history on every source replacement (proven on device 2026-09-15).
// Bounded by pruneOlderThan (90 days, see ChangeEventDao).
@Entity(
    tableName = "change_event",
    indices = [Index(value = ["offerRemoteId", "createdAt"])]
)
data class ChangeEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val offerRemoteId: String,
    val type: String,
    val beforeJson: String?,
    val afterJson: String?,
    val createdAt: Long
)
