/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.room.integration.multiplatformtestapp.test

import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.integration.multiplatformtestapp.test.BaseMigrationTest.MigrationDatabase
import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import androidx.sqlite.use
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

abstract class BaseMigrationTest {
    abstract fun getTestHelper(): MigrationTestHelper

    abstract fun getDatabaseBuilder(): RoomDatabase.Builder<MigrationDatabase>

    @Test
    fun migrateFromV1ToLatest() = runTest {
        val migrationTestHelper = getTestHelper()

        // Create database V1
        val connection = migrationTestHelper.createDatabase(1)
        // Insert some data, we'll validate it is present after migration
        connection.prepare("INSERT INTO MigrationEntity (pk) VALUES (?)").use {
            it.bindLong(1, 1)
            assertThat(it.step()).isFalse() // SQLITE_DONE
        }
        connection.close()

        // Migrate to latest
        val dbVersion2 =
            getDatabaseBuilder()
                .addMigrations(
                    object : Migration(1, 2) {
                        override fun migrate(connection: SQLiteConnection) {
                            connection.execSQL(
                                "ALTER TABLE MigrationEntity ADD COLUMN addedInV2 TEXT"
                            )
                        }
                    }
                )
                .build()
        val item = dbVersion2.dao().getSingleItem(1)
        assertNotNull(item)
        assertThat(item.pk).isEqualTo(1)
        assertThat(item.addedInV2).isNull()
        dbVersion2.close()
    }

    @Test
    fun migrateFromV1ToV2() = runTest {
        val migrationTestHelper = getTestHelper()

        // Create database V1
        val connectionV1 = migrationTestHelper.createDatabase(1)
        // Insert some data, we'll validate it is present after migration
        connectionV1.prepare("INSERT INTO MigrationEntity (pk) VALUES (?)").use {
            it.bindLong(1, 1)
            assertThat(it.step()).isFalse() // SQLITE_DONE
        }
        connectionV1.close()

        val migration =
            object : Migration(1, 2) {
                override fun migrate(connection: SQLiteConnection) {
                    connection.execSQL("ALTER TABLE MigrationEntity ADD COLUMN addedInV2 TEXT")
                }
            }
        // Migrate to V2 and validate data is still present
        val connectionV2 = migrationTestHelper.runMigrationsAndValidate(2, listOf(migration))
        connectionV2.prepare("SELECT count(*) FROM MigrationEntity").use {
            assertThat(it.step()).isTrue() // SQLITE_ROW
            assertThat(it.getInt(0)).isEqualTo(1)
        }
        connectionV2.close()
    }

    @Test
    fun missingMigration() = runTest {
        val migrationTestHelper = getTestHelper()
        // Create database V1
        val connection = migrationTestHelper.createDatabase(1)
        connection.close()

        // Create database missing migration
        val dbVersion2 = getDatabaseBuilder().build()
        // Fail to migrate
        assertThrows<IllegalStateException> { dbVersion2.dao().getSingleItem(1) }
            .hasMessageThat()
            .contains("A migration from 1 to 2 was required but not found.")
        dbVersion2.close()
    }

    @Test
    fun invalidMigration() = runTest {
        val migrationTestHelper = getTestHelper()
        // Create database V1
        val connection = migrationTestHelper.createDatabase(1)
        connection.close()

        // Create database with a migration that doesn't properly changes the schema
        val dbVersion2 =
            getDatabaseBuilder()
                .addMigrations(
                    object : Migration(1, 2) {
                        override fun migrate(connection: SQLiteConnection) {}
                    }
                )
                .build()
        // Fail to migrate
        assertThrows<IllegalStateException> { dbVersion2.dao().getSingleItem(1) }
            .hasMessageThat()
            .contains("Migration didn't properly handle: MigrationEntity")
        dbVersion2.close()
    }

    @Test
    fun destructiveMigration() = runTest {
        val migrationTestHelper = getTestHelper()
        // Create database V1
        val connection = migrationTestHelper.createDatabase(1)
        connection.close()

        // Create database with destructive migrations enabled
        val dbVersion2 =
            getDatabaseBuilder().fallbackToDestructiveMigration(dropAllTables = true).build()
        // Migrate via fallback destructive deletion
        val item = dbVersion2.dao().getSingleItem(1)
        assertNull(item)
        dbVersion2.close()
    }

    @Test
    fun destructiveMigrationOnDowngrade() = runTest {
        val migrationTestHelper = getTestHelper()
        // Create database from a future far away
        val connection = migrationTestHelper.createDatabase(99)
        connection.close()

        // Create database with destructive migrations on downgrade enabled
        val dbVersion2 =
            getDatabaseBuilder()
                .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                .build()
        // Migrate via fallback destructive deletion
        val item = dbVersion2.dao().getSingleItem(1)
        assertNull(item)
        dbVersion2.close()
    }

    @Test
    fun destructiveMigrationFrom() = runTest {
        val migrationTestHelper = getTestHelper()
        // Create database V1
        val connection = migrationTestHelper.createDatabase(1)
        connection.close()

        // Create database missing migration
        val dbVersion2 =
            getDatabaseBuilder().fallbackToDestructiveMigrationFrom(dropAllTables = true, 1).build()
        // Migrate via fallback destructive deletion
        val item = dbVersion2.dao().getSingleItem(1)
        assertNull(item)
        dbVersion2.close()
    }

    @Test
    fun invalidDestructiveMigrationFrom() {
        // Create database with an invalid combination of destructive migration from
        assertThrows<IllegalArgumentException> {
                getDatabaseBuilder()
                    .addMigrations(object : Migration(1, 2) {})
                    .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1)
                    .build()
            }
            .hasMessageThat()
            .contains("Inconsistency detected.")
    }

    @Test
    fun misuseTestHelperAlreadyCreatedDatabase() {
        val migrationTestHelper = getTestHelper()

        // Create database V1
        migrationTestHelper.createDatabase(1).close()

        // When trying to create at V1 again, fail due to database file being already created.
        assertThrows<IllegalStateException> { migrationTestHelper.createDatabase(1) }
            .hasMessageThat()
            .contains("Creation of tables didn't occur while creating a new database.")

        // If trying to create at V2, migration will try to run and fail.
        assertThrows<IllegalStateException> { migrationTestHelper.createDatabase(2) }
            .hasMessageThat()
            .contains("A migration from 1 to 2 was required but not found.")
    }

    @Test
    fun misuseTestHelperMissingDatabaseForValidateMigrations() {
        val migrationTestHelper = getTestHelper()

        // Try to validate migrations, but fail due to no previous database created.
        assertThrows<IllegalStateException> {
                migrationTestHelper.runMigrationsAndValidate(2, emptyList())
            }
            .hasMessageThat()
            .contains("Creation of tables should never occur while validating migrations.")
    }

    @Entity data class MigrationEntity(@PrimaryKey val pk: Long, val addedInV2: String?)

    @Dao
    interface MigrationDao {
        @Insert suspend fun insert(entity: MigrationEntity)

        @Query("SELECT * FROM MigrationEntity WHERE pk = :pk")
        suspend fun getSingleItem(pk: Long): MigrationEntity?
    }

    @Database(
        entities = [MigrationEntity::class],
        version = 2,
        exportSchema = true,
    )
    @ConstructedBy(BaseMigrationTest_MigrationDatabaseConstructor::class)
    abstract class MigrationDatabase : RoomDatabase() {
        abstract fun dao(): MigrationDao
    }
}

internal expect object BaseMigrationTest_MigrationDatabaseConstructor :
    RoomDatabaseConstructor<MigrationDatabase>
