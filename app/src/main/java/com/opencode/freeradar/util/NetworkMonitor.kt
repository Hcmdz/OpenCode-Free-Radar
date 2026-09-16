/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

interface NetworkMonitor {
    /** True only when positively metered; unknown means proceed (fail open). */
    fun isMetered(): Boolean
}

class AndroidNetworkMonitor(context: Context) : NetworkMonitor {
    private val connectivity =
        context.applicationContext.getSystemService(ConnectivityManager::class.java)

    override fun isMetered(): Boolean {
        val network = connectivity.activeNetwork ?: return false
        val caps = connectivity.getNetworkCapabilities(network) ?: return false
        return !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }
}
