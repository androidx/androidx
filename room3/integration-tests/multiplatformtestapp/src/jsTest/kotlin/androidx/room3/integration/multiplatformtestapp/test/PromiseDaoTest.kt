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

package androidx.room3.integration.multiplatformtestapp.test

import androidx.driver.web.worker.createDefaultWebWorkerDriver
import androidx.kruth.assertThat
import androidx.room3.Dao
import androidx.room3.DaoReturnTypeConverter
import androidx.room3.DaoReturnTypeConverters
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.ExperimentalRoomApi
import androidx.room3.Insert
import androidx.room3.OperationType
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.RoomWarnings
import kotlin.js.Promise
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.promise
import kotlinx.coroutines.test.runTest

class PromiseDaoTest {

    @Test
    fun validateDaoReturnTypeConverter() = runTest {
        val db =
            Room.inMemoryDatabaseBuilder<TestDatabase>(
                    factory = { PromiseDaoTest_TestDatabase_Impl() }
                )
                .setDriver(createDefaultWebWorkerDriver())
                .build()

        val latch = CompletableDeferred<Unit>()
        db.getDao().insert(TestEntity(1)).then {
            db.getDao().getAll().then { items ->
                assertThat(items).containsExactly(TestEntity(1))
                latch.complete(Unit)
            }
        }
        latch.await()
        db.close()
    }

    @OptIn(ExperimentalRoomApi::class)
    @Suppress(RoomWarnings.NO_DATABASE_CONSTRUCTOR)
    @Database(entities = [TestEntity::class], version = 1, exportSchema = false)
    @DaoReturnTypeConverters(PromiseDaoReturnTypeConverter::class)
    abstract class TestDatabase : RoomDatabase() {
        abstract fun getDao(): TestDao
    }

    @Dao
    interface TestDao {
        @Insert fun insert(item: TestEntity): Promise<Unit>

        @Query("SELECT * FROM TestEntity") fun getAll(): Promise<List<TestEntity>>
    }

    @Entity data class TestEntity(@PrimaryKey val id: Int)

    object PromiseDaoReturnTypeConverter {
        @DaoReturnTypeConverter([OperationType.READ, OperationType.WRITE])
        fun <T> convert(db: RoomDatabase, executeAndConvert: suspend () -> T): Promise<T> {
            return db.getCoroutineScope().promise { executeAndConvert() }
        }
    }
}
