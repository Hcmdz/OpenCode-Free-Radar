/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.notifications

interface SyncNotifier {
    suspend fun beforeSync(): Long
    suspend fun afterSync(watermark: Long)
}
