/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.domain.model

enum class FreeStatus {
    FREE, LIMITED, TRIAL, TEMPORARY, PAID, EXPIRED, UNKNOWN
}

/**
 * The app exists to catch usable $0 offers (e.g. Muse Spark 1.3 Free):
 * LIMITED / TRIAL / TEMPORARY rows are free-with-conditions, not paid.
 * Lives in domain so detectors and notifiers share the UI's definition.
 */
fun FreeStatus.isUsableFree(): Boolean = when (this) {
    FreeStatus.FREE, FreeStatus.LIMITED, FreeStatus.TRIAL, FreeStatus.TEMPORARY -> true
    FreeStatus.PAID, FreeStatus.EXPIRED, FreeStatus.UNKNOWN -> false
}

enum class Confidence {
    OFFICIAL, API_VERIFIED, CROSS_CHECKED, AUTOMATICALLY_DETECTED, TO_VERIFY
}

enum class ChangeType {
    NEW_MODEL,
    BECAME_FREE,
    FREE_EXPIRED,
    PRICE_CHANGED,
    LIMIT_CHANGED,
    CONTEXT_CHANGED,
    TOOL_SUPPORT_CHANGED,
    VISION_CHANGED,
    MODEL_REMOVED,
    PROVIDER_ADDED,
    PROVIDER_REMOVED,
    SOURCE_UNAVAILABLE
}

enum class SyncResult { OK, PARTIAL, FAILED }

enum class HealthState { HEALTHY, DEGRADED, UNAVAILABLE, UNKNOWN }
