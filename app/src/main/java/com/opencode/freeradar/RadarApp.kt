/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar

import android.app.Application
import com.opencode.freeradar.data.local.SyncPrefs
import com.opencode.freeradar.di.appModule
import com.opencode.freeradar.di.presentationModule
import com.opencode.freeradar.worker.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class RadarApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@RadarApp)
            modules(appModule, presentationModule)
        }
        // DataStore reads are suspend, so the schedule cannot be built inside
        // onCreate the way a synchronous preference read would allow.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val prefs = SyncPrefs(this@RadarApp)
            SyncScheduler.schedule(
                this@RadarApp,
                prefs.autoSync(),
                prefs.autoSyncIntervalHours(),
                force = false
            )
        }
    }
}
