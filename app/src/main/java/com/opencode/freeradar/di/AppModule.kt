/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.di

import androidx.room3.Room
import com.opencode.freeradar.data.local.DashboardFilterPrefs
import com.opencode.freeradar.data.local.DataStoreDashboardFilterPrefs
import com.opencode.freeradar.data.local.MIGRATION_1_2
import com.opencode.freeradar.data.local.NotificationPrefs
import com.opencode.freeradar.data.local.RadarDatabase
import com.opencode.freeradar.data.local.SyncPrefs
import com.opencode.freeradar.data.local.SyncSettings
import com.opencode.freeradar.data.local.SyncStatePrefs
import com.opencode.freeradar.data.local.SyncStateStore
import com.opencode.freeradar.data.local.UpdatePrefs
import com.opencode.freeradar.data.repository.OfflineFirstOfferRepository
import com.opencode.freeradar.data.source.litellm.LITELLM_SOURCE_ID
import com.opencode.freeradar.data.source.litellm.LiteLLMSource
import com.opencode.freeradar.data.source.openrouter.OPENROUTER_SOURCE_ID
import com.opencode.freeradar.data.source.openrouter.OpenRouterSource
import com.opencode.freeradar.data.source.remote.ModelsDevSource
import com.opencode.freeradar.data.source.remote.SourceOffer
import com.opencode.freeradar.data.source.remote.toOffer
import com.opencode.freeradar.data.source.remote.createHttpClient
import com.opencode.freeradar.domain.model.Offer
import com.opencode.freeradar.domain.repository.OfferRepository
import com.opencode.freeradar.util.AndroidNetworkMonitor
import com.opencode.freeradar.util.NetworkMonitor
import com.opencode.freeradar.util.UpdateCheckStore
import com.opencode.freeradar.util.UpdateManager
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
        ).addMigrations(MIGRATION_1_2).build()
    }
    single { get<RadarDatabase>().offerDao() }
    single { get<RadarDatabase>().changeEventDao() }
    single { get<RadarDatabase>().syncRunDao() }
    single { get<RadarDatabase>().sourceHealthDao() }
    single { createHttpClient(cacheDir = androidContext().cacheDir) }
    singleOf(::ModelsDevSource)
    singleOf(::OpenRouterSource)
    singleOf(::LiteLLMSource)
    single<Set<OfferSource>> { linkedSetOf(get<ModelsDevSource>(), get<OpenRouterSource>(), get<LiteLLMSource>()) }
    single<Map<String, (SourceOffer, Long) -> Offer>> {
        mapOf(
            "opencode-data" to { dto: SourceOffer, now: Long -> dto.toOffer(now) },
            OPENROUTER_SOURCE_ID to { dto: SourceOffer, now: Long -> dto.toOffer(now, OPENROUTER_SOURCE_ID) },
            LITELLM_SOURCE_ID to { dto: SourceOffer, now: Long -> dto.toOffer(now, LITELLM_SOURCE_ID) },
        )
    }
    single<OfferRepository> {
        OfflineFirstOfferRepository(
            get(),
            get<Set<OfferSource>>().associateBy { it.id },
            get(),
            syncState = get(),
            syncPrefs = get(),
            network = get(),
        )
    }
    singleOf(::SyncStatePrefs) bind SyncStateStore::class
    singleOf(::SyncPrefs) bind SyncSettings::class
    single<NetworkMonitor> { AndroidNetworkMonitor(androidContext()) }
    single { NotificationPrefs(androidContext()) }
    single<UpdateCheckStore> { UpdatePrefs(androidContext()) }
    single<DashboardFilterPrefs> { DataStoreDashboardFilterPrefs(androidContext()) }
    single { UpdateManager(createHttpClient(), get()) }
    single { OfferNotifier(androidContext()) }
    singleOf(::NotificationGate) bind SyncNotifier::class
}
