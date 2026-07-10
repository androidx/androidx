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
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.integration.kotlintestapp.TestDatabase
import androidx.room3.migration.Migration
import androidx.room3.testing.MigrationTestHelper
import androidx.room3.useReaderConnection
import androidx.room3.useWriterConnection
import androidx.room3.util.TableInfo
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import java.io.FileNotFoundException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(
            instrumentation = instrumentation,
            file = context.getDatabasePath(TEST_DB),
            driver = AndroidSQLiteDriver(),
            databaseClass = MigrationDb::class,
        )

    companion object {
        const val TEST_DB = "migration-test"
    }

    private fun getLatestDb(): MigrationDb {
        val db =
            Room.databaseBuilder<MigrationDb>(
                    context = InstrumentationRegistry.getInstrumentation().targetContext,
                    name = TEST_DB,
                )
                .setDriver(AndroidSQLiteDriver())
                .addMigrations(*ALL_MIGRATIONS)
                .build()
        // trigger opening database
        runBlocking { db.useWriterConnection {} }
        helper.closeWhenFinished(db)
        return db
    }

    @Before
    fun setup() {
        context.deleteDatabase(TEST_DB)
    }

    @Test
    fun giveBadResource() = runTest {
        val helper =
            MigrationTestHelper(
                instrumentation = instrumentation,
                file = context.getDatabasePath(TEST_DB),
                driver = AndroidSQLiteDriver(),
                databaseClass = TestDatabase::class,
            )
        assertThrows<FileNotFoundException> { helper.createDatabase(1) }
            .hasMessageThat()
            .contains("Cannot find")
    }

    @Test
    fun startInCurrentVersion() = runTest {
        helper.createDatabase(MigrationDb.LATEST_VERSION).use {
            val dao = MigrationDb.DaoV1(it)
            dao.insertIntoEntity1(2, "x")
        }

        val migrationDb = getLatestDb()
        val items = migrationDb.dao().loadAllEntity1s()
        assertThat(items.size).isEqualTo(1)
    }

    @Test
    fun addTable() = runTest {
        helper.createDatabase(1).use {
            val dao = MigrationDb.DaoV1(it)
            dao.insertIntoEntity1(2, "foo")
            dao.insertIntoEntity1(3, "bar")
        }

        helper.runMigrationsAndValidate(version = 2, migrations = listOf(MIGRATION_1_2)).use {
            MigrationDb.DaoV2(it).insertIntoEntity2(3, "blah")
        }

        val migrationDb = getLatestDb()
        val entity1s = migrationDb.dao().loadAllEntity1s()
        assertThat(entity1s.size).isEqualTo(2)

        val entity2 = MigrationDb.Entity2(2, null, "bar")
        migrationDb.dao().insert(entity2)
        val entity2s = migrationDb.dao().loadAllEntity2s()
        assertThat(entity2s.size).isEqualTo(2)
    }

    @Test fun addTableFailure() = runTest { testFailure(1, 2) }

    @Test
    fun addColumnFailure() = runTest {
        helper.createDatabase(2).close()

        assertThrows<IllegalStateException> {
            helper.runMigrationsAndValidate(version = 3, migrations = listOf(EmptyMigration(2, 3)))
        }
    }

    @Test
    fun addColumn() = runTest {
        helper.createDatabase(2).use {
            val v2Dao = MigrationDb.DaoV2(it)
            v2Dao.insertIntoEntity2(7, "blah")
        }

        helper.runMigrationsAndValidate(version = 3, migrations = listOf(MIGRATION_2_3)).close()

        val migrationDb = getLatestDb()
        val entity2s = migrationDb.dao().loadAllEntity2s()
        assertThat(entity2s.size).isEqualTo(1)
        assertThat(entity2s.single().name).isEqualTo("blah")
        assertThat(entity2s.single().addedInV3).isNull()
        val entity2Pojos = migrationDb.dao().loadAllEntity2sAsPojo()
        assertThat(entity2Pojos.size).isEqualTo(1)
        assertThat(entity2Pojos.single().name).isEqualTo("blah")
        assertThat(entity2Pojos.single().addedInV3).isNull()
    }

    @Test fun failedToRemoveColumn() = runTest { testFailure(4, 5) }

    @Test
    fun removeColumn() = runTest {
        helper.createDatabase(4).close()
        val connection =
            helper.runMigrationsAndValidate(version = 5, migrations = listOf(MIGRATION_4_5))
        val info = TableInfo.read(connection, MigrationDb.Entity3.TABLE_NAME)
        assertThat(info.columns.size).isEqualTo(2)
    }

    @Test
    fun dropTable() = runTest {
        helper.createDatabase(5).close()
        val connection =
            helper.runMigrationsAndValidate(version = 6, migrations = listOf(MIGRATION_5_6))
        val info = TableInfo.read(connection, MigrationDb.Entity3.TABLE_NAME)
        assertThat(info.columns.size).isEqualTo(0)
    }

    @Test
    @Ignore("b/445891912") // Add API to validate dropped tables.
    fun failedToDropTable() = runTest { testFailure(5, 6) }

    @Test
    fun failedToDropTableDontVerify() = runTest {
        helper.createDatabase(5).close()
        val connection = helper.runMigrationsAndValidate(6, listOf(EmptyMigration(5, 6)))
        val info = TableInfo.read(connection, MigrationDb.Entity3.TABLE_NAME)
        assertThat(info.columns.size).isEqualTo(2)
    }

    @Test
    fun failedForeignKey() = runTest {
        helper.createDatabase(6).close()

        assertThrows<IllegalStateException> {
                helper.runMigrationsAndValidate(
                    version = 7,
                    migrations =
                        listOf(
                            object : Migration(6, 7) {
                                override suspend fun migrate(connection: SQLiteConnection) {
                                    connection.execSQL(
                                        "CREATE TABLE Entity4 (`id` INTEGER, `name` TEXT," +
                                            " PRIMARY KEY(`id`))"
                                    )
                                }
                            }
                        ),
                )
            }
            .hasMessageThat()
            .contains("Migration didn't properly handle")
    }

    @Test
    fun newTableWithForeignKey() = runTest {
        helper.createDatabase(6).close()
        val connection =
            helper.runMigrationsAndValidate(version = 7, migrations = listOf(MIGRATION_6_7))
        val info = TableInfo.read(connection, MigrationDb.Entity4.TABLE_NAME)
        assertThat(info.foreignKeys.size).isEqualTo(1)
    }

    private suspend fun testFailure(startVersion: Int, endVersion: Int) {
        helper.createDatabase(startVersion).close()

        assertThrows<IllegalStateException> {
                helper.runMigrationsAndValidate(
                    version = endVersion,
                    migrations = listOf(EmptyMigration(startVersion, endVersion)),
                )
            }
            .hasMessageThat()
            .contains("Migration didn't properly handle")
    }

    @Test
    fun addDefaultValue() = runTest {
        helper.createDatabase(8).use { connection ->
            connection.execSQL("INSERT INTO " + MigrationDb.Entity5.TABLE_NAME + " (id) VALUES (1)")
        }

        helper.runMigrationsAndValidate(9, listOf(MIGRATION_8_9)).use { connection ->
            connection.prepare("SELECT * FROM " + MigrationDb.Entity5.TABLE_NAME).use {
                assertThat(it.step()).isTrue()
                assertThat(it.getInt(0)).isEqualTo(1)
                assertThat(it.getText(1)).isEqualTo("Unknown")
            }
        }
    }

    @Test
    fun addDefaultValueWithSurroundingParenthesis() = runTest {
        helper.createDatabase(9).use { connection ->
            connection.execSQL("INSERT INTO " + MigrationDb.Entity5.TABLE_NAME + " (id) VALUES (1)")
        }

        helper.runMigrationsAndValidate(10, listOf(MIGRATION_9_10)).use { connection ->
            connection.prepare("SELECT * FROM " + MigrationDb.Entity5.TABLE_NAME).use {
                assertThat(it.step()).isTrue()
                assertThat(it.getInt(0)).isEqualTo(1)
                assertThat(it.getText(1)).isEqualTo("Unknown")
                assertThat(it.getInt(2)).isEqualTo(0)
            }
        }
    }

    @Test
    fun invalidHash() = runTest {
        // Create database at version 1
        helper.createDatabase(1).use { connection ->
            // Set its version to 2
            connection.execSQL("PRAGMA user_version = 2")
        }

        // Open the database again at version 2 and it should fail identity, replicating the
        // scenario of:
        // 1. bumping version code without schema changes
        // 2. making schema changes and
        // 3. opening the DB again
        assertThrows<IllegalStateException> { helper.runMigrationsAndValidate(2, emptyList()) }
            .hasMessageThat()
            .apply {
                contains("Room cannot verify the data integrity.")
                contains(
                    "Expected identity hash: aee9a6eed720c059df0f2ee0d6e96d89, " +
                        "found: 2f3557e56d7f665363f3e20d14787a59"
                )
            }
    }

    @Test
    fun fallbackToDestructiveMigrationFrom_destructiveMigrationOccursForSuppliedVersion() =
        runTest {
            helper.createDatabase(6).use { connection ->
                val dao = MigrationDb.DaoV1(connection)
                dao.insertIntoEntity1(2, "foo")
                dao.insertIntoEntity1(3, "bar")
            }

            val db =
                Room.databaseBuilder<MigrationDb>(context, TEST_DB)
                    .setDriver(AndroidSQLiteDriver())
                    .fallbackToDestructiveMigrationFrom(true, 6)
                    .build()
            assertThat(db.dao().loadAllEntity1s()).hasSize(0)
            db.close()
        }

    @Test
    fun fallbackToDestructiveMigrationFrom_suppliedValueIsMigrationStartVersion_inconsistent() =
        runTest {
            helper.createDatabase(6).use { connection ->
                val dao = MigrationDb.DaoV1(connection)
                dao.insertIntoEntity1(2, "foo")
                dao.insertIntoEntity1(3, "bar")
            }

            assertThrows<IllegalArgumentException> {
                    Room.databaseBuilder<MigrationDb>(context, TEST_DB)
                        .setDriver(AndroidSQLiteDriver())
                        .addMigrations(MIGRATION_6_7)
                        .fallbackToDestructiveMigrationFrom(true, 6)
                        .build()
                }
                .hasMessageThat()
                .isEqualTo(
                    "Inconsistency detected. A Migration was supplied to addMigration() that " +
                        "has a start or end version equal to a start version supplied to " +
                        "fallbackToDestructiveMigrationFrom(). Start version is: 6"
                )
        }

    @Test
    fun fallbackToDestructiveMigrationFrom_suppliedValueIsMigrationEndVersion_inconsistent() =
        runTest {
            helper.createDatabase(5).use { connection ->
                val dao = MigrationDb.DaoV1(connection)
                dao.insertIntoEntity1(2, "foo")
                dao.insertIntoEntity1(3, "bar")
            }

            assertThrows<IllegalArgumentException> {
                    Room.databaseBuilder<MigrationDb>(context, TEST_DB)
                        .setDriver(AndroidSQLiteDriver())
                        .addMigrations(MIGRATION_5_6)
                        .fallbackToDestructiveMigrationFrom(true, 6)
                        .build()
                }
                .hasMessageThat()
                .isEqualTo(
                    "Inconsistency detected. A Migration was supplied to addMigration() that " +
                        "has a start or end version equal to a start version supplied to " +
                        "fallbackToDestructiveMigrationFrom(). Start version is: 6"
                )
        }

    @Test
    fun fallbackToDestructiveMigration_upgrade() = runTest {
        helper.createDatabase(1).use { connection ->
            val dao = MigrationDb.DaoV1(connection)
            dao.insertIntoEntity1(2, "foo")
            dao.insertIntoEntity1(3, "bar")
        }

        val db =
            Room.databaseBuilder<MigrationDb>(context, TEST_DB)
                .setDriver(AndroidSQLiteDriver())
                .fallbackToDestructiveMigration(false)
                .build()
        assertThat(db.dao().loadAllEntity1s()).hasSize(0)
        db.close()
    }

    @Test
    fun fallbackToDestructiveMigration_downgrade() = runTest {
        helper.createDatabase(1000).use { connection ->
            val dao = MigrationDb.DaoV1(connection)
            dao.insertIntoEntity1(2, "foo")
            dao.insertIntoEntity1(3, "bar")
        }

        val db =
            Room.databaseBuilder<MigrationDb>(context, TEST_DB)
                .setDriver(AndroidSQLiteDriver())
                .fallbackToDestructiveMigration(false)
                .build()
        assertThat(db.dao().loadAllEntity1s()).hasSize(0)
        db.close()
    }

    @Test
    fun fallbackToDestructiveMigrationOnDowngrade_upgrade() = runTest {
        helper.createDatabase(1).use { connection ->
            val dao = MigrationDb.DaoV1(connection)
            dao.insertIntoEntity1(2, "foo")
            dao.insertIntoEntity1(3, "bar")
        }

        assertThrows<IllegalStateException> {
                val db =
                    Room.databaseBuilder<MigrationDb>(context, TEST_DB)
                        .setDriver(AndroidSQLiteDriver())
                        .fallbackToDestructiveMigrationOnDowngrade(false)
                        .build()
                assertThat(db.dao().loadAllEntity1s()).hasSize(0)
                db.close()
            }
            .hasMessageThat()
            .startsWith(
                "A migration from 1 to ${MigrationDb.LATEST_VERSION} was required but not found."
            )
    }

    @Test
    fun fallbackToDestructiveMigrationOnDowngrade_downgrade() = runTest {
        helper.createDatabase(1000).use { connection ->
            val dao = MigrationDb.DaoV1(connection)
            dao.insertIntoEntity1(2, "foo")
            dao.insertIntoEntity1(3, "bar")
        }

        val db =
            Room.databaseBuilder<MigrationDb>(context, TEST_DB)
                .setDriver(AndroidSQLiteDriver())
                .fallbackToDestructiveMigrationOnDowngrade(false)
                .build()
        assertThat(db.dao().loadAllEntity1s()).hasSize(0)
        db.close()
    }

    @Test
    fun dropAllTablesDuringDestructiveMigrations() = runTest {
        helper.createDatabase(1000).use { connection ->
            connection.execSQL("CREATE TABLE ExtraTable (id INTEGER PRIMARY KEY)")
            connection.execSQL("CREATE VIEW ExtraView AS SELECT * FROM sqlite_master")
        }

        var onDestructiveMigrationInvoked = false
        val db =
            Room.databaseBuilder<MigrationDb>(context, TEST_DB)
                .setDriver(AndroidSQLiteDriver())
                .fallbackToDestructiveMigration(true)
                .addCallback(
                    object : RoomDatabase.Callback() {
                        override suspend fun onDestructiveMigration(connection: SQLiteConnection) {
                            super.onDestructiveMigration(connection)
                            onDestructiveMigrationInvoked = true
                        }
                    }
                )
                .build()
        val tableNames: Set<String> =
            db.useReaderConnection { connection ->
                connection.usePrepared("SELECT name FROM sqlite_master") { stmt ->
                    buildSet {
                        while (stmt.step()) {
                            add(stmt.getText(0))
                        }
                    }
                }
            }
        db.close()

        // Extra table is no longer present
        assertThat(tableNames).doesNotContain("ExtraTable")
        // Extra view is no longer present
        assertThat(tableNames).doesNotContain("ExtraView")
        // Android special table is present
        assertThat(tableNames).contains("android_metadata")

        assertThat(onDestructiveMigrationInvoked).isTrue()
    }

    @Test
    fun dropAllTablesDuringDestructiveMigrationsEscapeNames() = runTest {
        helper.createDatabase(1000).use { connection ->
            connection.execSQL("CREATE TABLE `order` (id INTEGER PRIMARY KEY)")
        }

        val db =
            Room.databaseBuilder<MigrationDb>(context, TEST_DB)
                .setDriver(AndroidSQLiteDriver())
                .fallbackToDestructiveMigration(true)
                .build()
        val tableNames: Set<String> =
            db.useReaderConnection { connection ->
                connection.usePrepared("SELECT name FROM sqlite_master") { stmt ->
                    buildSet {
                        while (stmt.step()) {
                            add(stmt.getText(0))
                        }
                    }
                }
            }
        db.close()

        // Extra table is no longer present
        assertThat(tableNames).doesNotContain("order")
    }

    private val MIGRATION_1_2: Migration =
        object : Migration(1, 2) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `Entity2` (`id` INTEGER NOT NULL," +
                        " `name` TEXT, PRIMARY KEY(`id`))"
                )
            }
        }

    private val MIGRATION_2_3: Migration =
        object : Migration(2, 3) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "ALTER TABLE " + MigrationDb.Entity2.TABLE_NAME + " ADD COLUMN addedInV3 TEXT"
                )
            }
        }

    private val MIGRATION_3_4: Migration =
        object : Migration(3, 4) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `Entity3` (`id` INTEGER NOT NULL," +
                        " `removedInV5` TEXT, `name` TEXT, PRIMARY KEY(`id`))"
                )
            }
        }

    private val MIGRATION_4_5: Migration =
        object : Migration(4, 5) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `Entity3_New` (`id` INTEGER NOT NULL," +
                        " `name` TEXT, PRIMARY KEY(`id`))"
                )
                connection.execSQL(
                    "INSERT INTO Entity3_New(`id`, `name`) " + "SELECT `id`, `name` FROM Entity3"
                )
                connection.execSQL("DROP TABLE Entity3")
                connection.execSQL("ALTER TABLE Entity3_New RENAME TO Entity3")
            }
        }

    private val MIGRATION_5_6: Migration =
        object : Migration(5, 6) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("DROP TABLE " + MigrationDb.Entity3.TABLE_NAME)
            }
        }

    private val MIGRATION_6_7: Migration =
        object : Migration(6, 7) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS " +
                        MigrationDb.Entity4.TABLE_NAME +
                        " (`id` INTEGER NOT NULL, `name` TEXT, PRIMARY KEY(`id`)," +
                        " FOREIGN KEY(`name`) REFERENCES `Entity1`(`name`)" +
                        " ON UPDATE NO ACTION ON DELETE NO ACTION DEFERRABLE INITIALLY DEFERRED)"
                )
                connection.execSQL(
                    "CREATE UNIQUE INDEX `index_entity1` ON " +
                        MigrationDb.Entity1.TABLE_NAME +
                        " (`name`)"
                )
            }
        }

    private val MIGRATION_8_9: Migration =
        object : Migration(8, 9) {
            override suspend fun migrate(connection: SQLiteConnection) {
                // Add column Entity4.addedInV9 with DEFAULT.
                connection.execSQL(
                    "ALTER TABLE " + MigrationDb.Entity5.TABLE_NAME + " RENAME TO save_Entity5"
                )
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS " +
                        MigrationDb.Entity5.TABLE_NAME +
                        " (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`addedInV9` TEXT DEFAULT 'Unknown')"
                )
                connection.execSQL(
                    "INSERT INTO " +
                        MigrationDb.Entity5.TABLE_NAME +
                        " (id) SELECT id FROM save_Entity5"
                )
                connection.execSQL("DROP TABLE save_Entity5")
            }
        }

    private val MIGRATION_9_10: Migration =
        object : Migration(9, 10) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "ALTER TABLE " +
                        MigrationDb.Entity5.TABLE_NAME +
                        " ADD COLUMN addedInV10 INTEGER NOT NULL DEFAULT (0)"
                )
            }
        }

    private val ALL_MIGRATIONS =
        arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_8_9,
            MIGRATION_9_10,
        )

    private class EmptyMigration(startVersion: Int, endVersion: Int) :
        Migration(startVersion, endVersion) {
        override suspend fun migrate(connection: SQLiteConnection) {
            // do nothing
        }
    }
}
