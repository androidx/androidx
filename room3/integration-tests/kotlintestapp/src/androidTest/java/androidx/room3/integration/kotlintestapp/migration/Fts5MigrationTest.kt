/*
 * Copyright 2026 The Android Open Source Project
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
import androidx.room3.Room
import androidx.room3.migration.Migration
import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class Fts5MigrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @get:Rule
    val helper =
        MigrationTestHelper(
            instrumentation = instrumentation,
            file = context.getDatabasePath(TEST_DB),
            driver = BundledSQLiteDriver(),
            databaseClass = Fts5MigrationDb::class,
        )

    @Before
    fun setup() {
        context.deleteDatabase(TEST_DB)
    }

    @Test
    fun validMigration_updateFtsVersion() = runTest {
        helper.createDatabase(1).use { connection ->
            connection.execSQL(
                "INSERT INTO Mail (title, content) VALUES('Cafe?', 'Would you like to join me " +
                    "for coffee? Found a nice place in downtown called 787.')"
            )
        }

        // using AutoMigration(from = 1, to = 2)
        helper.runMigrationsAndValidate(2).close()

        val latestDb = getLatestDb()
        assertThat(latestDb.getMailDao().getMailCount()).isEqualTo(1)
        latestDb.close()
    }

    @Test
    fun invalidMigration_badContentRowId() = runTest {
        helper.createDatabase(2).close()

        assertThrows<IllegalStateException> {
                Room.databaseBuilder<Fts5MigrationDb>(context = context, name = TEST_DB)
                    .setDriver(BundledSQLiteDriver())
                    .addMigrations(BAD_MIGRATION_2_3)
                    .build()
                    .getMailDao()
                    .getMailCount()
            }
            .hasMessageThat()
            .contains("Migration didn't properly handle")
    }

    @Test
    fun validMigration_addContentTable() = runTest {
        helper.createDatabase(2).close()

        // using AutoMigration(from = 2, to = 3)
        helper.runMigrationsAndValidate(2).close()
    }

    @Test
    fun validMigration_addContentRowId() = runTest {
        helper.createDatabase(3).use { connection ->
            connection.execSQL("INSERT INTO Song (songId, title) VALUES (16, 'Amar De Nuevo')")
        }

        // using AutoMigration(from = 3, to = 4)
        helper.runMigrationsAndValidate(4).use { connection ->
            val (id, title) =
                connection.prepare("SELECT rowid, * FROM SongFts WHERE title MATCH ?").use { stmt ->
                    stmt.bindText(1, "amar")
                    stmt.step()
                    stmt.getText(0) to stmt.getText(1)
                }
            assertThat(id).isEqualTo("16")
            assertThat(title).isEqualTo("Amar De Nuevo")
        }
    }

    private fun getLatestDb(): Fts5MigrationDb {
        return Room.databaseBuilder<Fts5MigrationDb>(context = context, name = TEST_DB)
            .setDriver(BundledSQLiteDriver())
            .build()
    }

    private val BAD_MIGRATION_2_3: Migration =
        object : Migration(2, 3) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("DROP TABLE `Mail`")
                connection.execSQL(
                    "CREATE VIRTUAL TABLE IF NOT EXISTS `SongFts` USING FTS5(`title`, " +
                        "tokenize=`unicode61`, content=`Song`, content_rowid=`sId`)"
                )
            }
        }

    companion object {
        private const val TEST_DB = "migration-test"
    }
}
