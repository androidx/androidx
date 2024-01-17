/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.room.testing.kotlintestapp.migration

import android.database.sqlite.SQLiteException
import androidx.kruth.assertThat
import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test custom database migrations.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class AutoMigrationAndMigrationTest {
    @JvmField
    @Rule
    var helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SimpleAutoMigrationDb::class.java
    )

    // Run this to create the very 1st version of the db.
    fun createFirstVersion() {
        val db = helper.createDatabase(TEST_DB, 1)
        db.execSQL("INSERT INTO Entity1 (id, name) VALUES (1, 'row1')")
        db.execSQL("INSERT INTO Entity2 (id, name) VALUES (2, 'row2')")
        db.close()
    }

    /**
     * Verifies that the user defined migration is selected over using an autoMigration.
     */
    @Test
    fun testAutoMigrationsNotProcessedBeforeCustomMigrations() {
        createFirstVersion()
        try {
            helper.runMigrationsAndValidate(
                TEST_DB,
                2,
                true,
                MIGRATION_1_2
            )
        } catch (e: SQLiteException) {
            assertThat(e.message).contains("no such table: Entity0")
        }
    }

    @Test
    fun autoMigrationShouldBeAddedToMigrations_WhenManualDowngradeMigrationIsPresent() {
        createFirstVersion()
        helper.runMigrationsAndValidate(
            TEST_DB,
            2,
            true,
            MIGRATION_1_0
        )
        val config = helper.databaseConfiguration
        assertThat(config).isNotNull()
        assertThat(config.migrationContainer.findMigrationPath(1, 2)).isNotNull()
        assertThat(config.migrationContainer.findMigrationPath(1, 2)).isNotEmpty()
    }

    companion object {
        private const val TEST_DB = "auto-migration-test"
        private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `Entity0` ADD COLUMN `addedInV2` INTEGER NOT NULL " +
                        "DEFAULT 2"
                )
            }
        }
        private val MIGRATION_1_0: Migration = object : Migration(1, 0) {
            override fun migrate(db: SupportSQLiteDatabase) {}
        }
    }
}
