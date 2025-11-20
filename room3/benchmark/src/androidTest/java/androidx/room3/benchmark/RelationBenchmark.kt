/*
 * Copyright 2019 The Android Open Source Project
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

import android.content.Context
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.testutils.generateAllEnumerations
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class RelationBenchmark(
    private val driver: UseDriver,
    private val parentSampleSize: Int,
    private val childSampleSize: Int,
) {

    @get:Rule val benchmarkRule = BenchmarkRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setup() {
        context.deleteDatabase(DB_NAME)
    }

    @Test
    fun largeRelationQuery() {
        val db =
            Room.databaseBuilder(context, TestDatabase::class.java, DB_NAME)
                .setDriver(
                    when (driver) {
                        UseDriver.ANDROID -> AndroidSQLiteDriver()
                        UseDriver.BUNDLED -> BundledSQLiteDriver()
                    }
                )
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                .build()
        val dao = db.getUserDao()

        val users = List(parentSampleSize) { i -> User(i, "name$i") }
        val items = List(parentSampleSize * childSampleSize) { i -> Item(i, i / childSampleSize) }
        runBlocking {
            dao.insertUsers(users)
            dao.insertItems(items)
        }

        benchmarkRule.measureRepeated {
            val result = runBlocking { dao.getUserWithItems() }
            assertEquals(result.size, parentSampleSize)
            assertEquals(result.first().items.size, childSampleSize)
            assertEquals(result.last().items.size, childSampleSize)
        }

        db.close()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "driver={0}, parentSampleSize={1}, childSampleSize={2}")
        fun data() = generateAllEnumerations(UseDriver.entries, listOf(100, 500, 1000), listOf(10))

        private const val DB_NAME = "relation-benchmark-test"
    }

    enum class UseDriver {
        ANDROID,
        BUNDLED,
    }
}
