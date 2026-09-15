/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.source.nvidia

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

class NvidiaCatalogParserTest {

    private fun fixture(name: String): String =
        javaClass.classLoader.getResourceAsStream("fixtures/$name")!!.bufferedReader().readText()

    @Test
    fun `real index excerpt yields absolute deduped card links`() {
        val links = parseNvidiaCatalog(fixture("nvidia-catalog-excerpt.md"))
        assertThat(links.isEmpty()).isFalse()
        assertThat(links).hasSize(links.distinct().size)
        assertThat(links.all { it.startsWith("https://build.nvidia.com/") && it.endsWith(".md") }).isTrue()
    }

    @Test
    fun `synthetic index extracts slugs and dedupes`() {
        val index = """
            - [Nemotron 3 Ultra](/qc69jvmznzxy/nemotron-3-ultra-550b-a55b.md) — reasoner
            - [Nemotron 3 Ultra](/qc69jvmznzxy/nemotron-3-ultra-550b-a55b.md) — dupe
            - [Evo 2](/qc69jvmznzxy/evo2-40b.md) — bio
        """.trimIndent()
        val links = parseNvidiaCatalog(index)
        assertThat(links).hasSize(2)
        assertThat(links).contains("https://build.nvidia.com/qc69jvmznzxy/nemotron-3-ultra-550b-a55b.md")
    }

    @Test
    fun `empty index signals seed fallback`() {
        assertThat(parseNvidiaCatalog("")).hasSize(0)
        assertThat(catalogNeedsSeedFallback(emptyList())).isTrue()
        val healthy = (1..MIN_CATALOG_LINKS).map { "https://build.nvidia.com/o/m$it.md" }
        assertThat(catalogNeedsSeedFallback(healthy)).isFalse()
    }

    @Test
    fun `seed list covers chat-capable models`() {
        assertThat(NVIDIA_SEED_CARDS.size >= 10).isTrue()
        assertThat(NVIDIA_SEED_CARDS.all { it.startsWith("https://build.nvidia.com/") }).isTrue()
        assertThat(NVIDIA_SEED_CARDS).contains("https://build.nvidia.com/qc69jvmznzxy/nemotron-3-ultra-550b-a55b.md")
    }
}
