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

@file:OptIn(ExperimentalUuidApi::class)

package androidx.room3.integration.multiplatformtestapp.test

import androidx.kruth.assertThat
import androidx.room3.ConstructedBy
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest

abstract class BaseUuidTest {

    private lateinit var db: SampleUuidDatabase

    abstract fun getRoomDatabase(): SampleUuidDatabase

    @BeforeTest
    fun before() {
        db = getRoomDatabase()
    }

    @AfterTest
    fun after() {
        db.close()
    }

    @Test
    fun testUuidQuery() = runTest {
        val dao = db.dao()
        val uuid1 = Uuid.parse("88c6af75-8d2a-489c-85c9-92e5dd8a108c")
        val uuid2 = Uuid.parse("12345678-1234-1234-1234-123456789abc")

        val entity = UuidEntity(id = uuid1, nullableId = uuid2)
        dao.insert(entity)

        val loaded = dao.getEntity(uuid1)
        assertThat(loaded).isEqualTo(entity)

        val nullEntity = UuidEntity(id = uuid2, nullableId = null)
        dao.insert(nullEntity)

        val loadedNull = dao.getEntity(uuid2)
        assertThat(loadedNull).isEqualTo(nullEntity)
    }
}

@Database(entities = [UuidEntity::class], version = 1, exportSchema = false)
@ConstructedBy(SampleUuidDatabaseConstructor::class)
abstract class SampleUuidDatabase : RoomDatabase() {
    abstract fun dao(): UuidDao
}

expect object SampleUuidDatabaseConstructor : RoomDatabaseConstructor<SampleUuidDatabase>

@Entity data class UuidEntity(@PrimaryKey val id: Uuid, val nullableId: Uuid?)

@Dao
interface UuidDao {
    @Insert suspend fun insert(entity: UuidEntity)

    @Query("SELECT * FROM UuidEntity WHERE id = :id") suspend fun getEntity(id: Uuid): UuidEntity?
}
