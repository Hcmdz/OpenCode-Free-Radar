/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.opencode.freeradar.MainActivity
import com.opencode.freeradar.R
import com.opencode.freeradar.domain.usecase.EventSummary
import com.opencode.freeradar.domain.usecase.NotifiedIds

class OfferNotifier(private val context: Context) {

    fun ensureChannel() {
        // No SDK guard: NotificationChannel needs API 26, minSdk is 29.
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_events),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        context.getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(channel)
    }

    fun canPost(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    // Guarded by canPost() above (lint cannot see through the helper).
    @SuppressLint("MissingPermission")
    fun post(summary: EventSummary, ids: NotifiedIds, names: Map<String, String>) {
        if (!canPost()) return
        ensureChannel()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putStringArrayListExtra(EXTRA_NEW_IDS, ArrayList(ids.newIds))
            putStringArrayListExtra(EXTRA_EXPIRED_IDS, ArrayList(ids.expiredIds))
        }
        val pending = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.notif_title))
            .setContentText(summaryText(summary))
            .setStyle(inboxStyle(ids, names))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun inboxStyle(ids: NotifiedIds, names: Map<String, String>): NotificationCompat.InboxStyle {
        val style = NotificationCompat.InboxStyle()
        val lines = ids.newIds.take(MAX_INBOX_LINES)
        lines.forEach { style.addLine(displayName(it, names)) }
        val overflow = ids.newIds.size - lines.size
        if (overflow > 0) style.setSummaryText("+$overflow")
        return style
    }

    private fun displayName(remoteId: String, names: Map<String, String>): String =
        names[remoteId] ?: remoteId.substringAfter('/', remoteId)

    internal fun summaryText(summary: EventSummary): String {
        val parts = mutableListOf<String>()
        if (summary.newFree > 0) {
            parts += context.resources.getQuantityString(
                R.plurals.notif_new_free, summary.newFree, summary.newFree
            )
        }
        if (summary.expired > 0) {
            parts += context.resources.getQuantityString(
                R.plurals.notif_expired, summary.expired, summary.expired
            )
        }
        return parts.joinToString(" · ")
    }

    companion object {
        const val CHANNEL_ID = "offer_events"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_NEW_IDS = "com.opencode.freeradar.extra.NEW_IDS"
        const val EXTRA_EXPIRED_IDS = "com.opencode.freeradar.extra.EXPIRED_IDS"
        const val MAX_INBOX_LINES = 5
    }
}
