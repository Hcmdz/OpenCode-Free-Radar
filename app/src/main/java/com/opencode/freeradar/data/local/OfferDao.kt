/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface OfferDao {
    @Query(
        "SELECT * FROM offer " +
            "WHERE (:compatibleOnly = 0 OR openCodeCompatible = 1) " +
            "ORDER BY verifiedAt DESC"
    )
    fun observeOffers(compatibleOnly: Boolean): Flow<List<OfferEntity>>

    @Query("SELECT * FROM offer WHERE remoteId = :remoteId")
    fun observeOffer(remoteId: String): Flow<OfferEntity?>

    @Query("SELECT * FROM offer WHERE source = :source")
    suspend fun snapshotBySource(source: String): List<OfferEntity>

    @Query("SELECT * FROM offer")
    suspend fun snapshotAll(): List<OfferEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(offers: List<OfferEntity>)

    @Query("UPDATE offer SET missedSyncs = missedSyncs + 1 WHERE remoteId IN (:remoteIds)")
    suspend fun bumpMissed(remoteIds: List<String>)

    @Query("DELETE FROM offer WHERE remoteId IN (:remoteIds) AND favorite = 0")
    suspend fun deleteByIds(remoteIds: List<String>)

    @Insert
    suspend fun insertEvents(events: List<ChangeEventEntity>)

    @Transaction
    suspend fun replaceSource(
        offers: List<OfferEntity>,
        events: List<ChangeEventEntity>,
        removeIds: List<String>
    ) {
        if (removeIds.isNotEmpty()) deleteByIds(removeIds)
        upsertAll(offers)
        if (events.isNotEmpty()) insertEvents(events)
    }

    @Query("UPDATE offer SET favorite = :favorite WHERE remoteId = :remoteId")
    suspend fun setFavorite(remoteId: String, favorite: Boolean)

    @Query("UPDATE offer SET confidence = :confidence WHERE remoteId IN (:remoteIds)")
    suspend fun updateConfidence(remoteIds: List<String>, confidence: String)
}
