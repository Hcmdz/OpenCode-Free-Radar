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

/**
 * Gated access, not a free offer: free tiers inside a purchasable plan
 * (`*-plan`) or an account-gated gateway (gitlab needs Premium/Ultimate).
 * Kept as LIMITED in the base, excluded from the free views and the bell.
 */
fun String.isGatedProvider(): Boolean =
    "-plan" in this || this == "gitlab"

/**
 * Free AND confirmed AND ungated: the single definition of "a real free
 * model" shared by detectors, notifiers and the free views. Unverified
 * rows (TO_VERIFY) are visible elsewhere but never ring or count as free.
 */
fun Offer.isConfirmedFree(): Boolean =
    freeStatus.isUsableFree() && confidence != Confidence.TO_VERIFY && !providerId.isGatedProvider()

enum class Confidence {
    OFFICIAL, API_VERIFIED, CROSS_CHECKED, AUTOMATICALLY_DETECTED, TO_VERIFY
}

/**
 * Providers serving local runtimes or self-deployed clouds instead of
 * hosted APIs: zero cost means "no meter", not "free offer". Reviewed
 * 2026-09-17 against the LiteLLM price map (ollama/lemonade are local
 * runtimes; sagemaker rows need a self-deployed AWS endpoint).
 */
val LOCAL_PROVIDERS = setOf("ollama", "lemonade", "sagemaker")

fun String.isLocalProvider(): Boolean = this in LOCAL_PROVIDERS

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
