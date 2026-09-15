/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.repository

import com.opencode.freeradar.data.local.ChangeEventEntity
import com.opencode.freeradar.data.local.OfferEntity
import com.opencode.freeradar.data.local.SourceHealthEntity
import com.opencode.freeradar.data.local.SyncRunEntity
import com.opencode.freeradar.data.source.remote.SourceOffer
import com.opencode.freeradar.domain.model.ChangeEvent
import com.opencode.freeradar.domain.model.ChangeType
import com.opencode.freeradar.domain.model.Confidence
import com.opencode.freeradar.domain.model.FreeStatus
import com.opencode.freeradar.domain.model.HealthState
import com.opencode.freeradar.domain.model.Offer
import com.opencode.freeradar.domain.model.SourceHealth
import com.opencode.freeradar.domain.model.SyncResult
import com.opencode.freeradar.domain.model.SyncRun

fun Offer.toEntity(): OfferEntity = OfferEntity(
    remoteId = remoteId,
    providerId = providerId,
    modelId = modelId,
    name = name,
    inputPrice = inputPrice,
    outputPrice = outputPrice,
    freeStatus = freeStatus.name,
    quota = quota,
    quotaPeriod = quotaPeriod,
    temporary = temporary,
    conditions = conditions,
    contextLength = contextLength,
    maxOutputTokens = maxOutputTokens,
    supportsTools = supportsTools,
    supportsVision = supportsVision,
    supportsStructuredOutput = supportsStructuredOutput,
    openCodeCompatible = openCodeCompatible,
    officialUrl = officialUrl,
    source = source,
    sourceUrl = sourceUrl,
    retrievedAt = retrievedAt,
    verifiedAt = verifiedAt,
    confidence = confidence.name,
    favorite = favorite,
    missedSyncs = 0
)

fun OfferEntity.toDomain(): Offer = Offer(
    remoteId = remoteId,
    providerId = providerId,
    modelId = modelId,
    name = name,
    inputPrice = inputPrice,
    outputPrice = outputPrice,
    freeStatus = runCatching { FreeStatus.valueOf(freeStatus) }.getOrDefault(FreeStatus.UNKNOWN),
    quota = quota,
    quotaPeriod = quotaPeriod,
    temporary = temporary,
    conditions = conditions,
    contextLength = contextLength,
    maxOutputTokens = maxOutputTokens,
    supportsTools = supportsTools,
    supportsVision = supportsVision,
    supportsStructuredOutput = supportsStructuredOutput,
    openCodeCompatible = openCodeCompatible,
    officialUrl = officialUrl,
    source = source,
    sourceUrl = sourceUrl,
    retrievedAt = retrievedAt,
    verifiedAt = verifiedAt,
    confidence = runCatching { Confidence.valueOf(confidence) }
        .getOrDefault(Confidence.TO_VERIFY),
    favorite = favorite,
    missedSyncs = missedSyncs
)

fun ChangeEvent.toEntity(): ChangeEventEntity = ChangeEventEntity(
    offerRemoteId = offerRemoteId,
    type = type.name,
    beforeJson = beforeJson,
    afterJson = afterJson,
    createdAt = createdAt
)

fun ChangeEventEntity.toDomain(): ChangeEvent = ChangeEvent(
    offerRemoteId = offerRemoteId,
    // Forward-compat: event kinds added by newer versions degrade to a generic entry.
    type = runCatching { ChangeType.valueOf(type) }.getOrDefault(ChangeType.SOURCE_UNAVAILABLE),
    beforeJson = beforeJson,
    afterJson = afterJson,
    createdAt = createdAt
)

fun SyncRunEntity.toDomain(): SyncRun = SyncRun(
    source = source,
    startedAt = startedAt,
    completedAt = completedAt,
    result = result?.let { runCatching { SyncResult.valueOf(it) }.getOrNull() },
    error = error
)

fun SourceHealthEntity.toDomain(): SourceHealth = SourceHealth(
    source = source,
    state = runCatching { HealthState.valueOf(state) }.getOrDefault(HealthState.UNKNOWN),
    checkedAt = checkedAt
)

fun SourceOffer.remoteId(): String = "$providerId/$modelId"
