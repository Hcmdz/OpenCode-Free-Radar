/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.domain.model

enum class FreeStatus {
    FREE, LIMITED, TRIAL, TEMPORARY, PAID, EXPIRED, UNKNOWN
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
