/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar

import androidx.room3.Room
import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.opencode.freeradar.data.local.RadarDatabase
import kotlinx.coroutines.test.runTest
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

    companion object {
        const val TEST_DB = "migration-test"
    }
}
