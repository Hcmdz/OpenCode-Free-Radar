/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar

import androidx.room3.Room
import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.opencode.freeradar.data.local.MIGRATION_1_2
import com.opencode.freeradar.data.local.RadarDatabase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RadarMigrationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = instrumentation,
        databaseClass = RadarDatabase::class,
        driver = AndroidSQLiteDriver(),
        file = instrumentation.targetContext.getDatabasePath(TEST_DB)
    )

    @Test
    fun schemaV1CreatesAndValidates() = runTest {
        helper.createDatabase(1).close()
        helper.runMigrationsAndValidate(1, emptyList()).close()
        Room.databaseBuilder<RadarDatabase>(
            instrumentation.targetContext, TEST_DB
        ).setDriver(AndroidSQLiteDriver()).build().close()
    }

    @Test
    fun migrate1To2KeepsRowsWithMissedSyncsZero() = runTest {
        helper.createDatabase(1).use { db ->
            db.prepare(
                "INSERT INTO offer (remoteId, providerId, modelId, name, " +
                    "freeStatus, temporary, openCodeCompatible, source, " +
                    "retrievedAt, verifiedAt, confidence, favorite) VALUES (" +
                    "'s1/m', 's1', 'm', 'M', 'FREE', 0, 1, 'opencode-data', " +
                    "1000, 1000, 'OFFICIAL', 0)"
            ).use { it.step() }
        }
        helper.runMigrationsAndValidate(2, listOf(MIGRATION_1_2)).close()
        val migrated = Room.databaseBuilder<RadarDatabase>(
            instrumentation.targetContext, TEST_DB
        ).setDriver(AndroidSQLiteDriver()).addMigrations(MIGRATION_1_2).build()
        try {
            val rows = migrated.offerDao().snapshotBySource("opencode-data")
            assertEquals(1, rows.size)
            assertEquals("s1/m", rows.first().remoteId)
            assertEquals(0, rows.first().missedSyncs)
        } finally {
            migrated.close()
        }
    }

    companion object {
        const val TEST_DB = "migration-test"
    }
}
