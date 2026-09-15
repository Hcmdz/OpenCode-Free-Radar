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
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(offers: List<OfferEntity>)

    @Query("DELETE FROM offer WHERE source = :source AND favorite = 0")
    suspend fun deleteBySourceKeepingFavorites(source: String)

    @Transaction
    suspend fun replaceSource(source: String, offers: List<OfferEntity>) {
        deleteBySourceKeepingFavorites(source)
        upsertAll(offers)
    }

    @Insert
    suspend fun insertEvents(events: List<ChangeEventEntity>)

    @Transaction
    suspend fun replaceSourceWithEvents(
        source: String,
        offers: List<OfferEntity>,
        events: List<ChangeEventEntity>
    ) {
        deleteBySourceKeepingFavorites(source)
        upsertAll(offers)
        insertEvents(events)
    }

    @Query("UPDATE offer SET favorite = :favorite WHERE remoteId = :remoteId")
    suspend fun setFavorite(remoteId: String, favorite: Boolean)
}
