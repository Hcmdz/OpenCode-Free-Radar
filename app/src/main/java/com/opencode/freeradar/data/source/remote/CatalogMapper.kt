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
 * + Duo/credits). Uncertain providers stay FREE — never guess from a name
 * alone. The `opencode` provider has its own TEMPORARY branch below.
 */
private fun String.isPlanOrGated(): Boolean =
    "-plan" in this || this == "gitlab"

fun SourceOffer.toOffer(now: Long, source: String = "opencode-data"): Offer {
    val freeStatus = when {
        inputPrice == 0.0 && outputPrice == 0.0 && conditions != null -> FreeStatus.TRIAL
        // Zen refinement 2026-09-17: every free model on the opencode
        // provider is time-boxed per their docs (https://opencode.ai/docs/zen/
        // pricing section: each of the 7 free rows is "available for a
        // limited time"), so `opencode` $0 rows are TEMPORARY (~32 rows on
        // 2026-09-17; roster ghosts keep TO_VERIFY via ModelsDevSource and
        // stay silent).
        inputPrice == 0.0 && outputPrice == 0.0 && providerId == "opencode" -> FreeStatus.TEMPORARY
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
