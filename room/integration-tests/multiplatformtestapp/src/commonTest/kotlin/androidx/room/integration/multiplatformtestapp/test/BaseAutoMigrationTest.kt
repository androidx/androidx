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
//
package androidx.room.integration.multiplatformtestapp.test

import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import androidx.room.AutoMigration
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.use
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

abstract class BaseAutoMigrationTest {
    abstract fun getTestHelper(): MigrationTestHelper
    abstract fun getRoomDatabase(): AutoMigrationDatabase

    @Test
    fun migrateFromV1ToLatest() = runTest {
        val migrationTestHelper = getTestHelper()

        // Create database V1
        val connection = migrationTestHelper.createDatabase(1)
        // Insert some data, we'll validate it is present after migration
        connection.prepare("INSERT INTO AutoMigrationEntity (pk) VALUES (?)").use {
            it.bindLong(1, 1)
            assertThat(it.step()).isFalse() // SQLITE_DONE
        }
        connection.prepare("SELECT * FROM AutoMigrationEntity").use { stmt ->
            assertThat(stmt.step()).isTrue()
            // Make sure that there is only 1 column in V1
            assertThat(stmt.getColumnCount()).isEqualTo(1)
            assertThat(stmt.getColumnName(0)).isEqualTo("pk")
            assertThat(stmt.getLong(0)).isEqualTo(1)
            assertThat(stmt.step()).isFalse() // SQLITE_DONE
        }
        connection.close()

        // Auto migrate to latest
        val dbVersion2 = getRoomDatabase()
        assertThat(dbVersion2.dao().update(AutoMigrationEntity(1, 5))).isEqualTo(1)
        assertThat(dbVersion2.dao().getSingleItem().pk).isEqualTo(1)
        assertThat(dbVersion2.dao().getSingleItem().data).isEqualTo(5)
        dbVersion2.close()
    }

    @Test
    fun migrateFromV1ToV2() = runTest {
        val migrationTestHelper = getTestHelper()

        // Create database V1
        val connection = migrationTestHelper.createDatabase(1)
        // Insert some data, we'll validate it is present after migration
        connection.prepare("INSERT INTO AutoMigrationEntity (pk) VALUES (?)").use {
            it.bindLong(1, 1)
            assertThat(it.step()).isFalse() // SQLITE_DONE
        }
        connection.close()

        // Auto migrate to V2
        migrationTestHelper.runMigrationsAndValidate(2)
    }

    @Test
    fun misuseTestHelperAlreadyCreatedDatabase() {
        val migrationTestHelper = getTestHelper()

        // Create database V1
        migrationTestHelper.createDatabase(1).close()

        // When trying to create at V1 again, fail due to database file being already created.
        assertThrows<IllegalStateException> {
            migrationTestHelper.createDatabase(1)
        }.hasMessageThat()
            .contains("Creation of tables didn't occur while creating a new database.")

        // If trying to create at V2, migration will try to run and fail.
        assertThrows<IllegalStateException> {
            migrationTestHelper.createDatabase(2)
        }.hasMessageThat()
            .contains("A migration from 1 to 2 was required but not found.")
    }

    @Test
    fun misuseTestHelperMissingDatabaseForValidateMigrations() {
        val migrationTestHelper = getTestHelper()

        // Try to validate migrations, but fail due to no previous database created.
        assertThrows<IllegalStateException> {
            migrationTestHelper.runMigrationsAndValidate(2, emptyList())
        }.hasMessageThat()
            .contains("Creation of tables should never occur while validating migrations.")
    }

    @Entity
    data class AutoMigrationEntity(
        @PrimaryKey
        val pk: Long,
        @ColumnInfo(defaultValue = "0")
        val data: Long
    )

    @Dao
    interface AutoMigrationDao {
        @Insert
        suspend fun insert(entity: AutoMigrationEntity)

        @Update
        suspend fun update(entity: AutoMigrationEntity): Int

        @Query("SELECT * FROM AutoMigrationEntity")
        suspend fun getSingleItem(): AutoMigrationEntity
    }

    @Database(
        entities = [AutoMigrationEntity::class],
        version = 2,
        exportSchema = true,
        autoMigrations = [AutoMigration(from = 1, to = 2)]
    )
    abstract class AutoMigrationDatabase : RoomDatabase() {
        abstract fun dao(): AutoMigrationDao
    }
}
