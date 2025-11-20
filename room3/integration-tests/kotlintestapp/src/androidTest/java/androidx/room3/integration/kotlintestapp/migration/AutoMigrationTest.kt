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
import androidx.room3.testing.MigrationTestHelper
import androidx.room3.util.TableInfo.Companion.read
import androidx.sqlite.SQLiteException
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class AutoMigrationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(
            instrumentation = instrumentation,
            file = context.getDatabasePath(TEST_DB),
            driver = AndroidSQLiteDriver(),
            databaseClass = AutoMigrationDb::class,
        )

    @Before
    fun setup() {
        context.deleteDatabase(TEST_DB)
    }

    // Run this to create the very 1st version of the db.
    private fun createFirstVersion() {
        val connection = helper.createDatabase(1)
        connection.execSQL("INSERT INTO Entity9 (id, name) VALUES (1, 'row1')")
        connection.execSQL("INSERT INTO Entity9 (id, name) VALUES (2, 'row2')")
        connection.execSQL("INSERT INTO Entity27 (id27) VALUES (3)")
        connection.execSQL("INSERT INTO Entity27 (id27) VALUES (5)")
        connection.close()
    }

    @Test
    fun goFromV1ToV2() {
        createFirstVersion()
        val connection = helper.runMigrationsAndValidate(version = 2, migrations = emptyList())
        val info = read(connection, AutoMigrationDb.Entity1.TABLE_NAME)
        assertThat(info.columns.size).isEqualTo(3)
        connection.close()
    }

    @Test
    fun goFromV1ToV3() {
        createFirstVersion()
        val connection = helper.runMigrationsAndValidate(version = 3, migrations = emptyList())
        val info = read(connection, AutoMigrationDb.Entity1.TABLE_NAME)
        assertThat(info.columns.size).isEqualTo(3)
        connection.close()
    }

    @Test
    fun goFromV1ToV4() {
        createFirstVersion()
        val connection = helper.runMigrationsAndValidate(version = 4, migrations = emptyList())
        val info = read(connection, AutoMigrationDb.Entity1.TABLE_NAME)
        assertThat(info.columns.size).isEqualTo(3)
        connection.close()
    }

    @Test
    fun goFromV1ToV5() {
        createFirstVersion()
        try {
            helper.runMigrationsAndValidate(version = 5, migrations = emptyList())
        } catch (e: SQLiteException) {
            assertThat(e.message).contains("""Foreign key violation(s) detected in 'Entity9'""")
        }
    }

    @Test
    fun testAutoMigrationWithNewEmbeddedField() {
        val embeddedHelper =
            MigrationTestHelper(
                instrumentation = instrumentation,
                file = context.getDatabasePath("embedded-auto-migration-test"),
                driver = AndroidSQLiteDriver(),
                databaseClass = EmbeddedAutoMigrationDb::class,
            )
        context.deleteDatabase("embedded-auto-migration-test")
        embeddedHelper.createDatabase(1).use {
            it.execSQL("INSERT INTO Entity1 (id, name) VALUES (1, 'row1')")
        }

        val connection =
            embeddedHelper.runMigrationsAndValidate(version = 2, migrations = emptyList())
        val info = read(connection, EmbeddedAutoMigrationDb.EmbeddedEntity1.TABLE_NAME)
        assertThat(info.columns.size).isEqualTo(3)
        connection.close()
    }

    companion object {
        private const val TEST_DB = "auto-migration-test"
    }
}
