/*
 * Copyright 2025 The Android Open Source Project
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
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class FtsMigrationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @get:Rule
    val helper =
        MigrationTestHelper(
            instrumentation = instrumentation,
            file = context.getDatabasePath(TEST_DB),
            driver = AndroidSQLiteDriver(),
            databaseClass = FtsMigrationDb::class,
        )

    @Before
    fun setup() {
        context.deleteDatabase(TEST_DB)
    }

    @Test
    fun validMigration() {
        helper.createDatabase(1).use { connection ->
            connection.execSQL(
                "INSERT INTO BOOK VALUES('Ready Player One', 'Ernest Cline', 402, " +
                    "'Everyone my age remembers where they were and what they were doing...')"
            )
        }

        helper.runMigrationsAndValidate(2, listOf(MIGRATION_1_2)).close()

        val latestDb = getLatestDb()
        val book = latestDb.bookDao().getBook("Ready Player")
        assertThat(book.title).isEqualTo("Ready Player One")
        assertThat(book.author).isEqualTo("Ernest Cline")
        assertThat(book.numOfPages).isEqualTo(402)
        latestDb.close()
    }

    @Test
    fun invalidMigration_missingFtsOption() {
        helper.createDatabase(1).close()

        assertThrows<IllegalStateException> {
                Room.databaseBuilder<FtsMigrationDb>(context = context, name = TEST_DB)
                    .setDriver(AndroidSQLiteDriver())
                    .addMigrations(
                        BAD_MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                    )
                    .build()
                    .bookDao()
                    .getAllBooks()
            }
            .hasMessageThat()
            .contains("Migration didn't properly handle")
    }

    @Test
    fun validFtsContentMigration() {
        helper.createDatabase(3).use { connection ->
            connection.execSQL(
                "INSERT INTO Person VALUES(1, 'Ernest', 'Cline', 'Ruth Ave', '', 'TX', 78757)"
            )
        }

        helper.runMigrationsAndValidate(4, listOf(MIGRATION_3_4)).close()

        val latestDb = getLatestDb()
        val addresses = latestDb.userDao().searchAddress("Ruth")
        assertThat(addresses.size).isEqualTo(1)
        assertThat(addresses[0].line1).isEqualTo("Ruth Ave")
        latestDb.close()
    }

    @Test
    fun validFtsWithNamedRowIdMigration() {
        helper.createDatabase(4).close()

        helper.runMigrationsAndValidate(5, listOf(MIGRATION_4_5)).close()
    }

    @Test
    @Throws(Exception::class)
    fun validFtsWithLanguageIdMigration() {
        helper.createDatabase(5).close()

        helper.runMigrationsAndValidate(6, listOf(MIGRATION_5_6)).close()
    }

    private fun getLatestDb(): FtsMigrationDb {
        return Room.databaseBuilder<FtsMigrationDb>(context = context, name = TEST_DB)
            .setDriver(AndroidSQLiteDriver())
            .addMigrations(*ALL_MIGRATIONS)
            .build()
    }

    private val MIGRATION_1_2: Migration =
        object : Migration(1, 2) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE Book RENAME TO Book_old")
                connection.execSQL(
                    "CREATE VIRTUAL TABLE IF NOT EXISTS `Book` USING FTS4(" +
                        "`title`, `author`, `numOfPages`, `text`, matchinfo=fts3)"
                )
                connection.execSQL("INSERT INTO Book SELECT * FROM Book_old")
                connection.execSQL("DROP TABLE Book_old")
            }
        }

    private val MIGRATION_2_3: Migration =
        object : Migration(2, 3) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    ("CREATE TABLE IF NOT EXISTS `Person` (`id` INTEGER NOT NULL, " +
                        "`firstName` TEXT, `lastName` TEXT, `line1` TEXT, `line2` TEXT, " +
                        "`state` TEXT, `zipcode` INTEGER, PRIMARY KEY(`id`))")
                )
                connection.execSQL(
                    "CREATE VIRTUAL TABLE IF NOT EXISTS `AddressFts` USING FTS4(`line1` TEXT, " +
                        "`line2` TEXT, `state` TEXT, `zipcode` INTEGER, content=`Person`)"
                )
            }
        }

    private val MIGRATION_3_4: Migration =
        object : Migration(3, 4) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE `Person` RENAME TO `User`")
                connection.execSQL("DROP TABLE `AddressFts`")
                connection.execSQL(
                    "CREATE VIRTUAL TABLE IF NOT EXISTS `AddressFts` USING FTS4(`line1` TEXT, " +
                        "`line2` TEXT, `state` TEXT, `zipcode` INTEGER, content=`User`)"
                )
                connection.execSQL(
                    "INSERT INTO `AddressFts` (`docid`, `line1`, `line2`, `state`, `zipcode`) " +
                        "SELECT `rowid`, `line1`, `line2`, `state`, `zipcode` FROM `User`"
                )
            }
        }

    private val MIGRATION_4_5: Migration =
        object : Migration(4, 5) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "CREATE VIRTUAL TABLE IF NOT EXISTS `Mail` USING FTS4(" +
                        "`content` TEXT NOT NULL)"
                )
            }
        }

    private val MIGRATION_5_6: Migration =
        object : Migration(5, 6) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("DROP TABLE `Mail`")
                connection.execSQL(
                    "CREATE VIRTUAL TABLE IF NOT EXISTS `Mail` USING FTS4(" +
                        "`content` TEXT NOT NULL, languageid=`lid`)"
                )
            }
        }

    private val BAD_MIGRATION_1_2: Migration =
        object : Migration(1, 2) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("DROP TABLE Book")
                connection.execSQL(
                    "CREATE VIRTUAL TABLE `Book` USING FTS4(" +
                        "`title`, `author`, `numOfPages`, `text`)"
                )
            }
        }

    private val ALL_MIGRATIONS =
        arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)

    companion object {
        private const val TEST_DB = "migration-test"
    }
}
