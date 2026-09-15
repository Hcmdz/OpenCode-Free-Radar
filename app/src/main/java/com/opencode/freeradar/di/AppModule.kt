/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.di

import androidx.room3.Room
import com.opencode.freeradar.data.local.NotificationPrefs
import com.opencode.freeradar.data.local.RadarDatabase
import com.opencode.freeradar.data.repository.OfflineFirstOfferRepository
import com.opencode.freeradar.data.source.remote.ModelsDevSource
import com.opencode.freeradar.data.source.remote.SourceOffer
import com.opencode.freeradar.data.source.remote.toOffer
import com.opencode.freeradar.data.source.remote.createHttpClient
import com.opencode.freeradar.domain.model.Offer
import com.opencode.freeradar.domain.repository.OfferRepository
import com.opencode.freeradar.domain.repository.OfferSource
import com.opencode.freeradar.notifications.NotificationGate
import com.opencode.freeradar.notifications.OfferNotifier
import com.opencode.freeradar.notifications.SyncNotifier
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule = module {
    single<RadarDatabase> {
        Room.databaseBuilder(
            androidContext(),
            RadarDatabase::class.java,
            "radar.db"
        ).build()
    }
    single { get<RadarDatabase>().offerDao() }
    single { get<RadarDatabase>().changeEventDao() }
    single { get<RadarDatabase>().syncRunDao() }
    single { get<RadarDatabase>().sourceHealthDao() }
    single { createHttpClient() }
    singleOf(::ModelsDevSource)
    single<Set<OfferSource>> { linkedSetOf(get<ModelsDevSource>()) }
    single<Map<String, (SourceOffer, Long) -> Offer>> {
        mapOf(
            "opencode-data" to { dto: SourceOffer, now: Long -> dto.toOffer(now) },
        )
    }
    single<OfferRepository> {
        OfflineFirstOfferRepository(
            get(),
            get<Set<OfferSource>>().associateBy { it.id },
            get(),
        )
    }
    single { NotificationPrefs(androidContext()) }
    single { OfferNotifier(androidContext()) }
    singleOf(::NotificationGate) bind SyncNotifier::class
}
