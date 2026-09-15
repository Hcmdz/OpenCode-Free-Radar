/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.domain.usecase

/** Removal fires only after this many consecutive absences (Story 2). */
const val ABSENCE_THRESHOLD = 2

data class AbsenceRow(
    val remoteId: String,
    val missed: Int,
    val favorite: Boolean
)

data class AbsencePlan(
    /** Missing rows whose counter advances (kept in DB). */
    val bump: List<String>,
    /** Missing rows at threshold, non-favorites only (deleted + event). */
    val remove: List<String>
)

/**
 * Pure absence planning over one source's cached rows vs the fresh id set.
 * Favorites are never removed (§63) — they linger with a growing counter
 * until the model returns or the user acts.
 */
fun planAbsence(rows: List<AbsenceRow>, present: Set<String>): AbsencePlan {
    val missing = rows.filter { it.remoteId !in present }
    return AbsencePlan(
        bump = missing.map { it.remoteId },
        remove = missing.filter { it.missed + 1 >= ABSENCE_THRESHOLD && !it.favorite }
            .map { it.remoteId }
    )
}
