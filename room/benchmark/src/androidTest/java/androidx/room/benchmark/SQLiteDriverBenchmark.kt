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

package androidx.room.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.room.benchmark.RoomDriverBenchmark.Companion.UseDriver
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 23)
class SQLiteDriverBenchmark(private val useDriver: UseDriver) {

    @get:Rule val benchmarkRule = BenchmarkRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var connection: SQLiteConnection

    @Before
    fun setup() {
        context.deleteDatabase("test.db")
        val databaseFilePath = context.getDatabasePath("test.db").path
        val driver =
            when (useDriver) {
                UseDriver.ANDROID -> AndroidSQLiteDriver()
                UseDriver.BUNDLED -> BundledSQLiteDriver()
            }
        connection = driver.open(databaseFilePath)
        // Use WAL mode in these benchmark as that is the most common and recommended journal
        // mode configuration.
        connection.execSQL("PRAGMA journal_mode = WAL")
        connection.execSQL("PRAGMA synchronous = NORMAL")
        connection.execSQL("CREATE TABLE IF NOT EXISTS TestEntity (id INTEGER PRIMARY KEY)")
    }

    @After
    fun cleanup() {
        connection.close()
    }

    @Test
    fun write_small() {
        benchmarkRule.measureRepeated {
            repeat(SMALL_AMOUNT) { connection.execSQL("INSERT INTO TestEntity (id) VALUES (NULL)") }
            connection.execSQL("DELETE FROM TestEntity")
        }
    }

    @Test
    fun write_large() {
        benchmarkRule.measureRepeated {
            repeat(LARGE_AMOUNT) { connection.execSQL("INSERT INTO TestEntity (id) VALUES (NULL)") }
            connection.execSQL("DELETE FROM TestEntity")
        }
    }

    @Test
    fun read_small() {
        repeat(SMALL_AMOUNT) { connection.execSQL("INSERT INTO TestEntity (id) VALUES (NULL)") }
        benchmarkRule.measureRepeated {
            val ids =
                connection.prepare("SELECT * FROM TestEntity").use {
                    buildList {
                        while (it.step()) {
                            add(it.getLong(0))
                        }
                    }
                }
            assertEquals(SMALL_AMOUNT, ids.size)
        }
    }

    @Test
    fun read_large() {
        repeat(LARGE_AMOUNT) { connection.execSQL("INSERT INTO TestEntity (id) VALUES (NULL)") }
        benchmarkRule.measureRepeated {
            val ids =
                connection.prepare("SELECT * FROM TestEntity").use {
                    buildList {
                        while (it.step()) {
                            add(it.getLong(0))
                        }
                    }
                }
            assertEquals(LARGE_AMOUNT, ids.size)
        }
    }

    companion object {
        @JvmStatic @Parameters(name = "driver = {0}") fun drivers() = UseDriver.entries

        enum class UseDriver {
            ANDROID,
            BUNDLED,
        }

        const val SMALL_AMOUNT = 25
        const val LARGE_AMOUNT = 1000
    }
}
