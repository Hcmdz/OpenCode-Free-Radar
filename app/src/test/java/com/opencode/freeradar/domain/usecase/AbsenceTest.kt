/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.domain.usecase

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class AbsenceTest {

    private fun row(id: String, missed: Int = 0, favorite: Boolean = false) =
        AbsenceRow(id, missed, favorite)

    @Test
    fun `first absence bumps without removing`() {
        val plan = planAbsence(listOf(row("a")), present = emptySet())
        assertThat(plan.bump).isEqualTo(listOf("a"))
        assertThat(plan.remove).isEqualTo(emptyList<String>())
    }

    @Test
    fun `second consecutive absence removes`() {
        val plan = planAbsence(listOf(row("a", missed = 1)), present = emptySet())
        assertThat(plan.bump).isEqualTo(listOf("a"))
        assertThat(plan.remove).isEqualTo(listOf("a"))
    }

    @Test
    fun `present rows are untouched`() {
        val plan = planAbsence(
            listOf(row("a", missed = 1), row("b")),
            present = setOf("a", "b")
        )
        assertThat(plan.bump).isEqualTo(emptyList<String>())
        assertThat(plan.remove).isEqualTo(emptyList<String>())
    }

    @Test
    fun `favorites are bumped but never removed`() {
        val plan = planAbsence(listOf(row("a", missed = 5, favorite = true)), present = emptySet())
        assertThat(plan.bump).isEqualTo(listOf("a"))
        assertThat(plan.remove).isEqualTo(emptyList<String>())
    }

    @Test
    fun `mixed rows split correctly`() {
        val plan = planAbsence(
            listOf(row("gone-twice", missed = 1), row("gone-once"), row("stays", favorite = true)),
            present = setOf("back")
        )
        assertThat(plan.bump).isEqualTo(listOf("gone-twice", "gone-once", "stays"))
        assertThat(plan.remove).isEqualTo(listOf("gone-twice"))
    }
}
