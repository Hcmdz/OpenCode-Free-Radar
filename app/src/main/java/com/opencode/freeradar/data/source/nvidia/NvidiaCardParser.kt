/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.source.nvidia

data class NvidiaCard(
    val slug: String,
    val title: String,
    val displayName: String,
    val publisher: String?,
    val updated: String?,
    val description: String?,
    val canonical: String?,
    val contextLength: Int?,
    val license: String?,
    val supportsTools: Boolean?,
    val supportsVision: Boolean?,
    val supportsStructuredOutput: Boolean?,
    val chatCapable: Boolean,
)

private val frontmatterFence = Regex("""^---\s*$""")
private val frontmatterEntry = Regex("""^([A-Za-z_]+):\s*"?([^"]*)"?\s*$""")
private val tokenCount = Regex("""(?i)(\d[\d,]*)(?:\.\d+)?\s*([kKmM])?\s*tokens?""")
private val licenseRow = Regex("""^\|\s*\*\*License\*\*\s*\|\s*(.+?)\s*\|?\s*$""")
private val markdownLink = Regex("""\[([^\]]*)\]\([^)]*\)""")
private val negations = listOf("not supported", "no support", "unsupported", "not available")

private val toolsSignals = listOf(
    "tool calling", "tool-call", "function calling", "tool use", "tool_choice", "native tool",
)
private val visionSignals = listOf(
    "vision-language", "multimodal", "image reasoning", "text + image",
    "text+image", "vision adapter", "input modalities",
)
private val structuredSignals = listOf(
    "structured output", "structured-output", "json mode", "response_format", "json_schema",
)

/**
 * Null when the card has no usable frontmatter — the caller skips the card
 * and keeps the last valid offer (fail-closed, never guessed).
 */
fun parseNvidiaCard(slug: String, markdown: String): NvidiaCard? {
    val lines = markdown.lines()
    if (lines.firstOrNull()?.trim() != "---") return null
    val fence = lines.drop(1).indexOfFirst { frontmatterFence.matches(it) }
    if (fence < 0) return null
    val fields = lines.drop(1).take(fence).mapNotNull { line ->
        frontmatterEntry.matchEntire(line.trim())?.let { it.groupValues[1] to it.groupValues[2].trim() }
    }.toMap()
    val title = fields["title"]?.ifBlank { null } ?: return null
    val body = lines.drop(1 + fence + 1).joinToString("\n")
    val description = fields["description"]?.ifBlank { null }
    return NvidiaCard(
        slug = slug,
        title = title,
        displayName = firstHeading(body, slug),
        publisher = fields["publisher"]?.ifBlank { null },
        updated = fields["updated"]?.ifBlank { null },
        description = description,
        canonical = fields["canonical"]?.ifBlank { null },
        contextLength = parseContextLength(body),
        license = parseLicense(body),
        supportsTools = capabilityValue(body, toolsSignals),
        supportsVision = capabilityValue(body, visionSignals),
        supportsStructuredOutput = capabilityValue(body, structuredSignals),
        chatCapable = isChatCapable(title, description.orEmpty(), body),
    )
}

/**
 * Max token count on lines mentioning context, sanity-capped. Null when no
 * credible value — a missing context is UNKNOWN, never 0 or a default.
 */
internal fun parseContextLength(body: String): Int? {
    val candidates = body.lines()
        .filter { "context" in it.lowercase() }
        .flatMap { tokenCount.findAll(it).toList() }
        .mapNotNull { match ->
            val digits = match.groupValues[1].replace(",", "")
            val base = digits.toLongOrNull() ?: return@mapNotNull null
            val scaled = when (match.groupValues[2].lowercase()) {
                "m" -> base * 1_000_000
                "k" -> base * 1_000
                else -> base
            }
            scaled.takeIf { it in 1_000..50_000_000 }?.toInt()
        }
    return candidates.maxOrNull()
}

private fun parseLicense(body: String): String? {
    val row = body.lines().firstNotNullOfOrNull { licenseRow.matchEntire(it.trim()) } ?: return null
    return markdownLink.replace(row.groupValues[1]) { it.groupValues[1] }.trim().take(200).ifBlank { null }
}

private val heading = Regex("""^#\s+(.+?)\s*$""")

/** Template headings carry no model identity — fall back to the slug. */
private val genericHeadings = setOf("overview", "model overview", "introduction", "home")

private fun firstHeading(body: String, slug: String): String {
    val found = body.lines().firstNotNullOfOrNull { heading.matchEntire(it.trim())?.groupValues?.get(1) }
    if (found == null || found.lowercase() in genericHeadings) return slug
    return found
}

/**
 * Explicit "not supported" near a signal wins (false); a bare positive wins
 * (true); silence stays null (UNKNOWN). Card authors write both
 * ("Function Calling: Not supported" vs "tool calling" in prose), so order
 * matters: every occurrence is checked, one clean positive is enough.
 */
internal fun capabilityValue(body: String, signals: List<String>): Boolean? {
    val lower = body.lowercase()
    var seen = false
    for (signal in signals) {
        var from = 0
        while (true) {
            val at = lower.indexOf(signal, from)
            if (at < 0) break
            seen = true
            val window = lower.substring(at, minOf(at + 80, lower.length))
            if (negations.none { it in window }) return true
            from = at + 1
        }
    }
    return if (seen) false else null
}
