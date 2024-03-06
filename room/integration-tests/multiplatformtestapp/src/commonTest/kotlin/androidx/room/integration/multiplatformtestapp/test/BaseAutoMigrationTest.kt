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
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.execSQL
import androidx.sqlite.use
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

abstract class BaseAutoMigrationTest {
    abstract val driver: SQLiteDriver
    abstract fun getRoomDatabase(): AutoMigrationDatabase

    @Test
    fun migrateFromV1ToV2() = runTest {
        val connection = driver.open()
        // Create database in V1
        connection.execSQL("CREATE TABLE IF NOT EXISTS " +
            "`AutoMigrationEntity` (`pk` INTEGER NOT NULL, PRIMARY KEY(`pk`))"
        )
        connection.execSQL("CREATE TABLE IF NOT EXISTS " +
            "room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)"
        )
        connection.execSQL("INSERT OR REPLACE INTO " +
            "room_master_table (id,identity_hash) VALUES(42, 'a917f82d955ea88cc98a551d197529c3')"
        )
        connection.execSQL("PRAGMA user_version = 1")
        connection.prepare("INSERT INTO AutoMigrationEntity (pk) VALUES (?)").use {
            it.bindLong(1, 1)
            assertThat(it.step()).isFalse() // SQLITE_DONE
        }
        connection.prepare(
            "SELECT * FROM AutoMigrationEntity"
        ).use { stmt ->
            assertThat(stmt.step()).isTrue()
            // Make sure that there is only 1 column in V1
            assertThat(stmt.getColumnCount()).isEqualTo(1)
            assertThat(stmt.getColumnName(0)).isEqualTo("pk")
            assertThat(stmt.getLong(0)).isEqualTo(1)
            assertThat(stmt.step()).isFalse() // SQLITE_DONE
        }
        connection.close()

        // Auto migrate to V2
        val dbVersion2 = getRoomDatabase()
        assertThat(dbVersion2.dao().update(AutoMigrationEntity(1, 5))).isEqualTo(1)
        assertThat(dbVersion2.dao().getSingleItem().pk).isEqualTo(1)
        assertThat(dbVersion2.dao().getSingleItem().data).isEqualTo(5)
        dbVersion2.close()
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
