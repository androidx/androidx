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

package androidx.room.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 23)
class RoomDriverBenchmark(private val useDriver: UseDriver) {

    @get:Rule val benchmarkRule = BenchmarkRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private var database: TestDatabase? = null

    private fun createMemoryDatabase(): TestDatabase {
        return Room.inMemoryDatabaseBuilder<TestDatabase>(context).buildForTest()
    }

    private fun createFileDatabase(): TestDatabase {
        return Room.databaseBuilder<TestDatabase>(context, "test.db").buildForTest()
    }

    private fun RoomDatabase.Builder<TestDatabase>.buildForTest(): TestDatabase {
        when (useDriver) {
            UseDriver.ANDROID -> setDriver(AndroidSQLiteDriver())
            UseDriver.BUNDLED -> setDriver(BundledSQLiteDriver())
            UseDriver.NONE -> {
                /* no driver */
            }
        }
        val db = build()
        database = db
        return db
    }

    @Before
    fun setup() {
        context.deleteDatabase("test.db")
    }

    @After
    fun cleanup() {
        database?.close()
    }

    @Test
    fun write_small() {
        val dao = createMemoryDatabase().getDao()
        benchmarkRule.measureRepeated {
            runBlocking {
                repeat(SMALL_AMOUNT) { dao.add(TestEntity()) }
                dao.delete()
            }
        }
    }

    @Test
    fun write_large() {
        val dao = createMemoryDatabase().getDao()
        benchmarkRule.measureRepeated {
            runBlocking {
                repeat(LARGE_AMOUNT) { dao.add(TestEntity()) }
                dao.delete()
            }
        }
    }

    @Test
    fun read_small() {
        val dao = createMemoryDatabase().getDao()
        runBlocking { repeat(SMALL_AMOUNT) { dao.add(TestEntity()) } }
        benchmarkRule.measureRepeated { runBlocking { dao.get() } }
    }

    @Test
    fun read_large() {
        val dao = createMemoryDatabase().getDao()
        runBlocking { repeat(LARGE_AMOUNT) { dao.add(TestEntity()) } }
        benchmarkRule.measureRepeated { runBlocking { dao.get() } }
    }

    @Test
    fun read_small_concurrently() {
        val dao = createFileDatabase().getDao()
        runBlocking { repeat(SMALL_AMOUNT) { dao.add(TestEntity()) } }
        benchmarkRule.measureRepeated {
            runBlocking {
                coroutineScope {
                    repeat(CONCURRENT_READERS) { launch(Dispatchers.IO) { dao.get() } }
                }
            }
        }
    }

    @Test
    fun read_large_concurrently() {
        val dao = createFileDatabase().getDao()
        runBlocking { repeat(LARGE_AMOUNT) { dao.add(TestEntity()) } }
        benchmarkRule.measureRepeated {
            runBlocking {
                coroutineScope {
                    repeat(CONCURRENT_READERS) { launch(Dispatchers.IO) { dao.get() } }
                }
            }
        }
    }

    @Test
    fun read_write_concurrently() {
        val dao = createFileDatabase().getDao()
        benchmarkRule.measureRepeated {
            runBlocking {
                coroutineScope {
                    repeat(CONCURRENT_WRITERS) { launch(Dispatchers.IO) { dao.add(TestEntity()) } }
                    repeat(CONCURRENT_READERS) { launch(Dispatchers.IO) { dao.get() } }
                }
                dao.delete()
            }
        }
    }

    @Database(entities = [TestEntity::class], version = 1, exportSchema = false)
    abstract class TestDatabase : RoomDatabase() {
        abstract fun getDao(): TestDao
    }

    @Entity data class TestEntity(@PrimaryKey(autoGenerate = true) val id: Int = 0)

    @Dao
    interface TestDao {
        @Insert suspend fun add(item: TestEntity)

        @Query("SELECT * FROM TestEntity") suspend fun get(): List<TestEntity>

        @Query("DELETE FROM TestEntity") suspend fun delete()
    }

    companion object {
        @JvmStatic @Parameters(name = "driver = {0}") fun drivers() = UseDriver.entries

        enum class UseDriver {
            ANDROID,
            BUNDLED,
            NONE,
        }

        const val SMALL_AMOUNT = 25
        const val LARGE_AMOUNT = 1000
        const val CONCURRENT_WRITERS = 2
        const val CONCURRENT_READERS = 6
    }
}
