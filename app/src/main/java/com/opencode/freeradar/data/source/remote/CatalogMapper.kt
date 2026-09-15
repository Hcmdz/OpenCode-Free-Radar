/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.source.remote

import com.opencode.freeradar.domain.model.Confidence
import com.opencode.freeradar.domain.model.FreeStatus
import com.opencode.freeradar.domain.model.Offer

/**
 * Reviewed 2026-09-15 against the live catalogs: a zero price alone does
 * not mean free. Dated trials (S3 `expiration_date`, the only `conditions`
 * producer today) are TRIAL; $0 rows served inside a purchasable plan or an
 * account-gated gateway are LIMITED (proven: gitlab needs Premium/Ultimate
 * + Duo/credits, opencode Zen needs account + billing). Uncertain providers
 * stay FREE — never guess from a name alone.
 */
private fun String.isPlanOrGated(): Boolean =
    "-plan" in this || this in setOf("gitlab", "opencode")

fun SourceOffer.toOffer(now: Long, source: String = "opencode-data"): Offer {
    val freeStatus = when {
        inputPrice == 0.0 && outputPrice == 0.0 && conditions != null -> FreeStatus.TRIAL
        inputPrice == 0.0 && outputPrice == 0.0 && providerId.isPlanOrGated() -> FreeStatus.LIMITED
        inputPrice == 0.0 && outputPrice == 0.0 -> FreeStatus.FREE
        inputPrice == null || outputPrice == null -> FreeStatus.UNKNOWN
        else -> FreeStatus.PAID
    }
    return Offer(
        remoteId = "$providerId/$modelId",
        providerId = providerId,
        modelId = modelId,
        name = name,
        inputPrice = inputPrice,
        outputPrice = outputPrice,
        freeStatus = freeStatus,
        quota = quota,
        quotaPeriod = null,
        temporary = false,
        conditions = conditions,
        contextLength = contextLength,
        maxOutputTokens = maxOutputTokens,
        supportsTools = supportsTools,
        supportsVision = supportsVision,
        supportsStructuredOutput = supportsStructuredOutput,
        openCodeCompatible = isCompatibleWithOpenCode(this),
        officialUrl = officialUrl,
        source = source,
        sourceUrl = sourceUrl,
        retrievedAt = now,
        verifiedAt = now,
        confidence = confidence ?: Confidence.OFFICIAL,
        favorite = false
    )
}

fun isCompatibleWithOpenCode(offer: SourceOffer): Boolean =
    offer.supportsTools == true
