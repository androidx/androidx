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
import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.ProvidedAutoMigrationSpec
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.integration.multiplatformtestapp.test.BaseAutoMigrationTest.AutoMigrationDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import androidx.sqlite.use
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

abstract class BaseAutoMigrationTest {
    abstract fun getTestHelper(): MigrationTestHelper

    abstract fun getDatabaseBuilder(): RoomDatabase.Builder<AutoMigrationDatabase>

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
        val dbVersion3 = getDatabaseBuilder().addAutoMigrationSpec(ProvidedSpecFrom2To3()).build()
        val dao = dbVersion3.dao()
        assertThat(dao.getSingleItem().pk).isEqualTo(1)
        assertThat(dao.getSingleItem().data).isEqualTo(0)
        assertThat(dao.getSingleItem().moreData).isEqualTo("5")
        dbVersion3.close()
    }

    @Test
    fun migrateFromV1ToV2() = runTest {
        val migrationTestHelper = getTestHelper()

        // Create database V1
        val connectionV1 = migrationTestHelper.createDatabase(1)
        // Insert some data, we'll validate it is present after migration
        connectionV1.prepare("INSERT INTO AutoMigrationEntity (pk) VALUES (?)").use {
            it.bindLong(1, 1)
            assertThat(it.step()).isFalse() // SQLITE_DONE
        }
        connectionV1.close()

        // Auto migrate to V2 and validate data is still present
        val connectionV2 = migrationTestHelper.runMigrationsAndValidate(2)
        connectionV2.prepare("SELECT count(*) FROM AutoMigrationEntity").use {
            assertThat(it.step()).isTrue() // SQLITE_ROW
            assertThat(it.getInt(0)).isEqualTo(1)
        }
        connectionV2.close()
    }

    @Test
    fun missingProvidedAutoMigrationSpec() {
        assertThrows<IllegalArgumentException> { getDatabaseBuilder().build() }
            .hasMessageThat()
            .contains(
                "A required auto migration spec (${ProvidedSpecFrom2To3::class.qualifiedName}) is " +
                    "missing in the database configuration."
            )
    }

    @Test
    fun extraProvidedAutoMigrationSpec() {
        assertThrows<IllegalArgumentException> {
                getDatabaseBuilder()
                    .addAutoMigrationSpec(ProvidedSpecFrom2To3())
                    .addAutoMigrationSpec(ExtraProvidedSpec())
                    .build()
            }
            .hasMessageThat()
            .contains("Unexpected auto migration specs found.")
    }

    @Test
    fun subclassedProvidedAutoMigrationSpec() {
        val db = getDatabaseBuilder().addAutoMigrationSpec(SubProvidedSpecFrom2To3()).build()
        db.close()
    }

    @Entity
    data class AutoMigrationEntity(
        @PrimaryKey val pk: Long,
        @ColumnInfo(defaultValue = "0") val data: Long,
        @ColumnInfo(defaultValue = "") val moreData: String
    )

    @Dao
    interface AutoMigrationDao {
        @Insert suspend fun insert(entity: AutoMigrationEntity)

        @Query("SELECT * FROM AutoMigrationEntity") suspend fun getSingleItem(): AutoMigrationEntity
    }

    @Database(
        entities = [AutoMigrationEntity::class],
        version = 3,
        exportSchema = true,
        autoMigrations =
            [
                AutoMigration(from = 1, to = 2),
                AutoMigration(from = 2, to = 3, spec = ProvidedSpecFrom2To3::class)
            ]
    )
    @ConstructedBy(BaseAutoMigrationTest_AutoMigrationDatabaseConstructor::class)
    abstract class AutoMigrationDatabase : RoomDatabase() {
        abstract fun dao(): AutoMigrationDao
    }

    @ProvidedAutoMigrationSpec
    open class ProvidedSpecFrom2To3 : AutoMigrationSpec {
        override fun onPostMigrate(connection: SQLiteConnection) {
            connection.execSQL("UPDATE AutoMigrationEntity SET moreData = '5'")
        }
    }

    class SubProvidedSpecFrom2To3 : ProvidedSpecFrom2To3()

    class ExtraProvidedSpec : AutoMigrationSpec
}

expect object BaseAutoMigrationTest_AutoMigrationDatabaseConstructor :
    RoomDatabaseConstructor<AutoMigrationDatabase> {
    override fun initialize(): AutoMigrationDatabase
}
