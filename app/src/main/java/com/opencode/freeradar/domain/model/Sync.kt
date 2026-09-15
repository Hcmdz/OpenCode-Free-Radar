/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.domain.model

data class SyncRun(
    val source: String,
    val startedAt: Long,
    val completedAt: Long?,
    val result: SyncResult?,
    val error: String?
)

data class SourceHealth(
    val source: String,
    val state: HealthState,
    val checkedAt: Long
)
