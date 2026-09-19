/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.source.remote

import com.opencode.freeradar.domain.model.Confidence

/** MIT-licensed docs source, same pattern as the LiteLLM price map. */
const val ZEN_MDX_URL =
    "https://raw.githubusercontent.com/anomalyco/opencode/dev/packages/web/src/content/docs/zen.mdx"

/** Roster ids carrying the free signal on their own (stealth model has no suffix). */
private val ZEN_FREE_ALLOWLIST = setOf("big-pickle")

private fun String.isZenFreeRosterId(): Boolean =
    endsWith("-free") || this in ZEN_FREE_ALLOWLIST

/**
 * Pricing table ids from the MDX: Pricing rows carry display names only, so
 * they are joined with the Endpoints table (Model → Model ID) on the Model
 * column, after stripping `(…)` qualifiers (`GPT 5.5 (≤ 272K tokens)`).
 * Free ⟺ Input and Output are both `Free`. Names missing from Endpoints are
 * skipped — never guess an id from a name alone.
 */
fun parseZenFreeIds(body: String): Set<String> {
    val idsByName = mutableMapOf<String, String>()
    var inEndpoints = false
    var inPricing = false
    val free = mutableSetOf<String>()
    for (raw in body.lineSequence()) {
        val line = raw.trim()
        if (!line.startsWith("|")) {
            inEndpoints = false
            inPricing = false
            continue
        }
        val cells = line.split("|").drop(1).dropLast(1).map { it.trim() }
        // Separator rows are `---` runs: a lone `-` is a real "no value" cell.
        if (cells.any { it.length >= 3 && it.all { c -> c == '-' || c == ':' } }) continue
        when {
            "Model ID" in cells -> {
                inEndpoints = true
                inPricing = false
            }
            cells.getOrNull(1) == "Input" -> {
                inEndpoints = false
                inPricing = true
            }
            inEndpoints && cells.size >= 2 -> {
                idsByName[cells[0]] = cells[1]
            }
            inPricing && cells.size >= 3 &&
                cells[1] == "Free" && cells[2] == "Free" -> {
                val name = cells[0].substringBeforeLast(" (")
                idsByName[name]?.let { free += it }
            }
        }
    }
    return free
}

/**
 * Roster (served?) × MDX (free-priced?) fusion over `opencode` `$0` rows.
 * Confirmed ⟺ served AND (free-suffixed/allowlisted OR MDX-priced): the
 * roster leads the docs by days, so a served `-free` row needs no doc to
 * ring. Anything else usable-free stays TO_VERIFY and silent. Null signals
 * fail open — a dead input changes nothing.
 */
fun mergeZenFreeSignals(
    offers: List<SourceOffer>,
    roster: Set<String>?,
    mdxFree: Set<String>?
): List<SourceOffer> {
    if (roster == null && mdxFree == null) return offers
    return offers.map { offer ->
        if (offer.providerId == ModelsDevSource.ZEN_PROVIDER && offer.isFree() &&
            !isConfirmedZenFree(offer.modelId, roster, mdxFree)
        ) {
            offer.copy(confidence = Confidence.TO_VERIFY)
        } else {
            offer
        }
    }
}

private fun SourceOffer.isFree(): Boolean =
    inputPrice == 0.0 && outputPrice == 0.0

private fun isConfirmedZenFree(id: String, roster: Set<String>?, mdxFree: Set<String>?): Boolean {
    if (roster == null || id !in roster) return false
    return id.isZenFreeRosterId() || (mdxFree != null && id in mdxFree)
}

/**
 * Roster/MDX free ids missing from the catalog (models.dev lags behind Zen):
 * silent TO_VERIFY rows with no conditions/quota (both would mis-map or
 * ring downstream), provenance in sourceUrl.
 */
fun synthesizeZenFreeMissing(
    offers: List<SourceOffer>,
    roster: Set<String>?,
    mdxFree: Set<String>?
): List<SourceOffer> {
    val present = offers
        .filter { it.providerId == ModelsDevSource.ZEN_PROVIDER }
        .map { it.modelId }.toSet()
    val candidates = mutableSetOf<String>()
    roster?.filter { it.isZenFreeRosterId() }?.let { candidates += it }
    mdxFree?.let { candidates += it.filter { id -> roster == null || id in roster } }
    return offers + (candidates - present).map { id ->
        SourceOffer(
            providerId = ModelsDevSource.ZEN_PROVIDER,
            modelId = id,
            name = id,
            inputPrice = 0.0,
            outputPrice = 0.0,
            contextLength = null,
            maxOutputTokens = null,
            supportsTools = null,
            supportsVision = null,
            supportsStructuredOutput = null,
            quota = null,
            conditions = null,
            officialUrl = "https://opencode.ai/docs/zen/",
            sourceUrl = ZEN_MDX_URL,
            confidence = Confidence.TO_VERIFY
        )
    }
}
