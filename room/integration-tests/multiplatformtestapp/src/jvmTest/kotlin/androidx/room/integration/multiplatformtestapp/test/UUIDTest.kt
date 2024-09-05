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
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.integration.multiplatformtestapp.test.UUIDTest.SampleJvmDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.util.UUID
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest

class UUIDTest {
    @Test
    fun testUUIDQuery() = runTest {
        val db =
            Room.inMemoryDatabaseBuilder<SampleJvmDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        val dao = db.dao()
        val text = "88c6af75-8d2a-489c-85c9-92e5dd8a108c"
        val uuid = UUID.fromString(text)

        dao.insertWithQuery(uuid)
        assertThat(dao.getEntity(uuid)).isEqualTo(UUIDEntity(uuid))
    }

    @Database(entities = [UUIDEntity::class], version = 1, exportSchema = false)
    abstract class SampleJvmDatabase : RoomDatabase() {
        abstract fun dao(): ByteDao
    }

    @Dao
    interface ByteDao {
        @Insert suspend fun insert(byteEntity: UUIDEntity)

        @Query("INSERT INTO UUIDEntity (id_UUID) VALUES (:uuid)")
        suspend fun insertWithQuery(uuid: UUID): Long

        @Query("SELECT * FROM UUIDEntity WHERE id_UUID = :uuid")
        suspend fun getEntity(uuid: UUID): UUIDEntity
    }

    @Entity
    data class UUIDEntity(
        @PrimaryKey val id_UUID: UUID,
    )
}
