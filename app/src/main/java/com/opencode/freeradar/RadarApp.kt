/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar

import android.app.Application
import com.opencode.freeradar.di.appModule
import com.opencode.freeradar.di.presentationModule
import com.opencode.freeradar.worker.SyncScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class RadarApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@RadarApp)
            modules(appModule, presentationModule)
        }
        SyncScheduler.scheduleDaily(this)
    }
}
