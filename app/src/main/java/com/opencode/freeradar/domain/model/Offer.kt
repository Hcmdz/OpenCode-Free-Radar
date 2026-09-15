/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.domain.model

data class Offer(
    val remoteId: String,
    val providerId: String,
    val modelId: String,
    val name: String,
    val inputPrice: Double?,
    val outputPrice: Double?,
    val freeStatus: FreeStatus,
    val quota: String?,
    val quotaPeriod: String?,
    val temporary: Boolean,
    val conditions: String?,
    val contextLength: Int?,
    val maxOutputTokens: Int?,
    val supportsTools: Boolean?,
    val supportsVision: Boolean?,
    val supportsStructuredOutput: Boolean?,
    val openCodeCompatible: Boolean,
    val officialUrl: String?,
    val source: String,
    val sourceUrl: String?,
    val retrievedAt: Long,
    val verifiedAt: Long,
    val confidence: Confidence,
    val favorite: Boolean,
    /** Consecutive successful-fetch absences (Story 2 gate, never user state). */
    val missedSyncs: Int = 0
)

data class ChangeEvent(
    val offerRemoteId: String,
    val type: ChangeType,
    val beforeJson: String?,
    val afterJson: String?,
    val createdAt: Long
)
