/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.source.remote

import com.opencode.freeradar.domain.model.Confidence
import com.opencode.freeradar.domain.model.FreeStatus
import com.opencode.freeradar.domain.model.Offer

fun SourceOffer.toOffer(now: Long, source: String = "opencode-data"): Offer {
    val freeStatus = when {
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
        confidence = Confidence.OFFICIAL,
        favorite = false
    )
}

fun isCompatibleWithOpenCode(offer: SourceOffer): Boolean =
    offer.supportsTools == true
