/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.local

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "offer",
    indices = [
        Index(value = ["remoteId"], unique = true),
        Index(value = ["source", "freeStatus"]),
        Index(value = ["openCodeCompatible", "freeStatus"])
    ]
)
data class OfferEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: String,
    val providerId: String,
    val modelId: String,
    val name: String,
    val inputPrice: Double?,
    val outputPrice: Double?,
    val freeStatus: String,
    val quota: String?,
    val quotaPeriod: String?,
    val temporary: Boolean = false,
    val conditions: String?,
    val contextLength: Int?,
    val maxOutputTokens: Int?,
    val supportsTools: Boolean?,
    val supportsVision: Boolean?,
    val supportsStructuredOutput: Boolean?,
    val openCodeCompatible: Boolean = false,
    val officialUrl: String?,
    val source: String,
    val sourceUrl: String?,
    val retrievedAt: Long,
    val verifiedAt: Long,
    val confidence: String,
    val favorite: Boolean = false
)
