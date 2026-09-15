/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.source.nvidia

/** Discovery index: server-rendered model list, no JS needed. */
const val NVIDIA_MODELS_INDEX = "https://build.nvidia.com/models.md"

/** Below this many extracted links the index is considered broken → seed fallback. */
const val MIN_CATALOG_LINKS = 5

private val cardLink = Regex("""\[[^\]]*\]\((/[^)]+\.md)\)""")

/** Absolute, deduped card URLs in index order. Empty when the index is unusable. */
fun parseNvidiaCatalog(index: String): List<String> =
    cardLink.findAll(index)
        .map { "https://build.nvidia.com" + it.groupValues[1] }
        .distinct()
        .toList()

fun catalogNeedsSeedFallback(links: List<String>): Boolean = links.size < MIN_CATALOG_LINKS

/**
 * Pinned chat-capable cards used when index extraction fails. Slugs verified
 * against the live index 2026-09-15; card fetches may still 404 over time and
 * are skipped individually (never fatal).
 */
val NVIDIA_SEED_CARDS: List<String> = listOf(
    "nemotron-3-ultra-550b-a55b",
    "nemotron-3-super-120b-a12b",
    "nemotron-3.5-lightning-30b-a3b",
    "mistral-nemotron",
    "kimi-k3",
    "llama-3.2-11b-vision-instruct",
    "llama-3.2-90b-vision-instruct",
    "deepseek-v4-flash-0731",
    "gpt-oss-20b",
    "gemma-4-31b-it",
    "muse-glimmer-30b",
    "nemotron-parse",
).map { "https://build.nvidia.com/qc69jvmznzxy/$it.md" }
