/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.notifications

import com.opencode.freeradar.data.local.NotificationPrefs
import com.opencode.freeradar.domain.repository.OfferRepository
import com.opencode.freeradar.domain.usecase.V1_NOTIFY_TYPES
import com.opencode.freeradar.domain.usecase.notifiedIds
import com.opencode.freeradar.domain.usecase.summarizeEvents
import kotlinx.coroutines.flow.first

class NotificationGate(
    private val repository: OfferRepository,
    private val prefs: NotificationPrefs,
    private val notifier: OfferNotifier
) : SyncNotifier {

    override suspend fun beforeSync(): Long = repository.latestEventId()

    override suspend fun afterSync(watermark: Long) {
        if (prefs.enabled.first()) {
            val events = repository.eventsSince(watermark, V1_NOTIFY_TYPES.map { it.name })
            val summary = summarizeEvents(events) ?: return
            val ids = notifiedIds(events)
            // Names are best-effort: a model deleted since the sync has no row.
            val names = repository.observeOffers(false).first()
                .associate { it.remoteId to it.name }
            notifier.post(summary, ids, names)
        }
    }
}
