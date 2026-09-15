/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChangeEventDao {
    @Query(
        "SELECT * FROM change_event WHERE offerRemoteId = :remoteId " +
            "ORDER BY createdAt DESC"
    )
    fun observeHistory(remoteId: String): Flow<List<ChangeEventEntity>>

    @Query(
        "SELECT * FROM change_event WHERE id > :sinceId AND type IN (:types) " +
            "ORDER BY id ASC"
    )
    suspend fun eventsAfter(sinceId: Long, types: List<String>): List<ChangeEventEntity>

    @Query("SELECT COALESCE(MAX(id), 0) FROM change_event")
    suspend fun maxEventId(): Long

    @Query("DELETE FROM change_event WHERE createdAt < :cutoff")
    suspend fun pruneOlderThan(cutoff: Long)
}

@Dao
interface SyncRunDao {
    @Insert
    suspend fun insert(run: SyncRunEntity): Long

    @Query(
        "UPDATE sync_run SET completedAt = :completedAt, result = :result, " +
            "error = :error WHERE id = :id"
    )
    suspend fun finishRun(id: Long, completedAt: Long, result: String, error: String?)

    @Query(
        "SELECT * FROM sync_run WHERE source = :source " +
            "ORDER BY startedAt DESC LIMIT :limit"
    )
    suspend fun recentRuns(source: String, limit: Int): List<SyncRunEntity>

    @Query(
        "SELECT * FROM sync_run WHERE source = :source " +
            "ORDER BY startedAt DESC LIMIT 1"
    )
    fun observeLastRun(source: String): Flow<SyncRunEntity?>

    @Query("SELECT * FROM sync_run ORDER BY startedAt DESC LIMIT 1")
    fun observeLatestRun(): Flow<SyncRunEntity?>

    @Query(
        "DELETE FROM sync_run WHERE source = :source AND id NOT IN " +
            "(SELECT id FROM sync_run WHERE source = :source " +
            "ORDER BY startedAt DESC LIMIT :keep)"
    )
    suspend fun pruneKeepLast(source: String, keep: Int)
}

@Dao
interface SourceHealthDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(health: SourceHealthEntity)

    @Query("SELECT * FROM source_health")
    fun observeAll(): Flow<List<SourceHealthEntity>>
}
