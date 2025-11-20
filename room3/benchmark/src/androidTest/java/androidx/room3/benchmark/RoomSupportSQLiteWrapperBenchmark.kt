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

package androidx.room3.benchmark

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.support.getSupportWrapper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.use
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
class RoomSupportSQLiteWrapperBenchmark(private val useDriver: UseDriver) {
    @get:Rule val benchmarkRule = BenchmarkRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var supportDatabase: SupportSQLiteDatabase

    @Before
    fun setup() {
        context.deleteDatabase("test.db")
        supportDatabase = Room.databaseBuilder<TestDatabase>(context, "test.db").buildForTest()

        supportDatabase.execSQL("PRAGMA journal_mode = WAL")
        supportDatabase.execSQL("PRAGMA synchronous = NORMAL")
        val columns =
            listOf("id INTEGER PRIMARY KEY AUTOINCREMENT") +
                List(COLUMN_COUNT) { "Column$it TEXT NOT NULL" }
        supportDatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS $TABLE_NAME (${columns.joinToString()})"
        )
    }

    @After
    fun cleanup() {
        supportDatabase.close()
    }

    @Test
    fun write_small() {
        benchmarkRule.measureRepeated {
            repeat(SMALL_AMOUNT) { insert() }
            supportDatabase.execSQL("DELETE FROM $TABLE_NAME")
        }
    }

    @Test
    fun write_large() {
        benchmarkRule.measureRepeated {
            repeat(LARGE_AMOUNT) { insert() }
            supportDatabase.execSQL("DELETE FROM $TABLE_NAME")
        }
    }

    @Test
    fun read_small() {
        repeat(SMALL_AMOUNT) { insert() }
        benchmarkRule.measureRepeated {
            supportDatabase.query("SELECT * FROM $TABLE_NAME").use {
                assertEquals(SMALL_AMOUNT, it.count)
            }
        }
    }

    @Test
    fun read_large() {
        repeat(SMALL_AMOUNT) { insert() }
        benchmarkRule.measureRepeated {
            supportDatabase.query("SELECT * FROM $TABLE_NAME").use {
                assertEquals(SMALL_AMOUNT, it.count)
            }
        }
    }

    @Test
    fun small_transaction() {
        benchmarkRule.measureRepeated {
            supportDatabase.beginTransactionNonExclusive()
            try {
                repeat(SMALL_AMOUNT) { insert() }
                supportDatabase.setTransactionSuccessful()
            } finally {
                supportDatabase.endTransaction()
            }
        }
        supportDatabase.execSQL("DELETE FROM $TABLE_NAME")
    }

    @Test
    fun large_transaction() {
        benchmarkRule.measureRepeated {
            supportDatabase.beginTransactionNonExclusive()
            try {
                repeat(LARGE_AMOUNT) { insert() }
                supportDatabase.setTransactionSuccessful()
            } finally {
                supportDatabase.endTransaction()
            }
        }
        supportDatabase.execSQL("DELETE FROM $TABLE_NAME")
    }

    private fun insert() {
        // Inserts one row into the table
        val values = ContentValues()
        for (i in 0..<COLUMN_COUNT) {
            values.put("Column$i", "value$i")
        }
        supportDatabase.insert(TABLE_NAME, SQLiteDatabase.CONFLICT_FAIL, values)
    }

    private fun RoomDatabase.Builder<TestDatabase>.buildForTest(): SupportSQLiteDatabase {
        when (useDriver) {
            UseDriver.ANDROID -> setDriver(AndroidSQLiteDriver())
            UseDriver.BUNDLED -> setDriver(BundledSQLiteDriver())
            UseDriver.NONE -> {
                /* no driver */
            }
        }
        val db = build()
        return db.getSupportWrapper()
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
        // Test when table has many columns
        const val COLUMN_COUNT = 100
        const val TABLE_NAME = "WrapperTable"
    }
}
