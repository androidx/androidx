/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.room3.integration.kotlintestapp.migration

import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import androidx.room3.AutoMigration
import androidx.room3.ColumnInfo
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.ProvidedAutoMigrationSpec
import androidx.room3.RoomDatabase
import androidx.room3.migration.AutoMigrationSpec
import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Test custom database migrations. */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ProvidedAutoMigrationSpecTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val providedSpec = ProvidedAutoMigrationDb.MyProvidedAutoMigration()
    val helperWithoutSpec =
        MigrationTestHelper(
            instrumentation = InstrumentationRegistry.getInstrumentation(),
            file = context.getDatabasePath(TEST_DB),
            driver = AndroidSQLiteDriver(),
            databaseClass = ProvidedAutoMigrationDb::class,
        )
    val helperWithSpec =
        MigrationTestHelper(
            instrumentation = InstrumentationRegistry.getInstrumentation(),
            file = context.getDatabasePath(TEST_DB),
            driver = AndroidSQLiteDriver(),
            databaseClass = ProvidedAutoMigrationDb::class,
            autoMigrationSpecs = listOf(providedSpec),
        )

    @Database(
        version = 2,
        entities = [ProvidedAutoMigrationDb.Entity1::class, ProvidedAutoMigrationDb.Entity2::class],
        autoMigrations =
            [
                AutoMigration(
                    from = 1,
                    to = 2,
                    spec = ProvidedAutoMigrationDb.MyProvidedAutoMigration::class,
                )
            ],
        exportSchema = true,
    )
    abstract class ProvidedAutoMigrationDb : RoomDatabase() {

        /** No change between versions. */
        @Entity
        internal class Entity1 {
            @PrimaryKey var id = 0
            var name: String? = null

            @ColumnInfo(defaultValue = "1") var addedInV1 = 0

            companion object {
                const val TABLE_NAME = "Entity1"
            }
        }

        /** A new table added. */
        @Entity
        internal class Entity2 {
            @PrimaryKey var id = 0
            var name: String? = null

            @ColumnInfo(defaultValue = "1") var addedInV1 = 0

            @ColumnInfo(defaultValue = "2") var addedInV2 = 0

            companion object {
                const val TABLE_NAME = "Entity2"
            }
        }

        @ProvidedAutoMigrationSpec
        internal class MyProvidedAutoMigration() : AutoMigrationSpec {
            var onPostMigrateCalled = false

            override fun onPostMigrate(connection: SQLiteConnection) {
                onPostMigrateCalled = true
            }
        }
    }

    // Run this to create the very 1st version of the db.
    private fun createFirstVersion() {
        helperWithoutSpec.createDatabase(1).use {
            it.execSQL("INSERT INTO Entity1 (id, name) VALUES (1, 'row1')")
        }
    }

    @Before
    fun setup() {
        context.deleteDatabase(TEST_DB)
    }

    @Test
    fun testOnPostMigrate() {
        createFirstVersion()
        helperWithSpec.runMigrationsAndValidate(version = 2, migrations = emptyList()).use {
            assertThat(providedSpec.onPostMigrateCalled).isTrue()
        }
    }

    @Test
    fun testNoSpecProvidedInConfig() {
        createFirstVersion()

        assertThrows<IllegalArgumentException> {
                helperWithoutSpec.runMigrationsAndValidate(version = 2, migrations = emptyList())
            }
            .hasMessageThat()
            .isEqualTo(
                "A required auto migration spec (" +
                    ProvidedAutoMigrationDb.MyProvidedAutoMigration::class.qualifiedName +
                    ") has not been provided."
            )
    }

    companion object {
        private const val TEST_DB = "auto-migration-test"
    }
}
