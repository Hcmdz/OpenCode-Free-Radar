/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.source.nvidia

private val keepTokens = setOf(
    // "chat", "assistant" and "code" are deliberately absent: every card's
    // Prototype snippet contains /v1/chat/completions, assistant roles and
    // shell code, so those tokens fire on image generators too (proven on
    // the FLUX.1-dev card 2026-09-15). "coding" stays — prose-only.
    "llm", "llms", "coding", "agent", "agents", "agentic",
    "reasoning", "instruction", "multimodal", "rag", "conversation",
    "conversational", "chatbot",
)
private val keepPhrases = listOf(
    "function calling", "tool calling", "tool use", "vision-language",
    "language model", "language models", "text generation",
    "language generation", "image reasoning",
)
private val excludeTokens = setOf(
    // Each entry collided with zero keep-bearing fixture cards (2026-09-15).
    // Broad words (climate/weather/driving/fluid) are deliberately absent:
    // cards without any keep signal stay out through the no-keep rule, and
    // "weather"/"fluid" both fired on the Nemotron-Ultra fixture itself.
    "protein", "proteins", "genomic", "genome", "molecule", "molecular",
    "dna", "rna", "amino", "cfd", "speech",
    "transcription", "dubbing", "lip",
)

/**
 * Generator-class phrases: substring match (hyphens included). Narrow by
 * construction — "image reasoning"/captioning cards (VLM) contain none of these.
 */
private val excludePhrases = listOf(
    "image editing",
    "image generation",
    "text-to-image",
)

/**
 * True when the card plausibly describes a chat/text model usable with
 * OpenCode. Exclusion wins over keep signals; unknown content stays out
 * (fail-closed: a missed model is better than a wrong offer).
 */
fun isChatCapable(title: String, description: String, body: String): Boolean {
    val text = "$title\n$description\n$body".lowercase()
    if (excludePhrases.any { it in text }) return false
    val tokens = text.split(Regex("[^a-z0-9]+")).toSet()
    if (excludeTokens.any { it in tokens }) return false
    if (keepPhrases.any { it in text }) return true
    return keepTokens.any { it in tokens }
}
