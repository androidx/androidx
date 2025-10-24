/*
 * Copyright 2018 The Android Open Source Project
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
import androidx.room3.immediateTransaction
import androidx.room3.useWriterConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.testutils.generateAllEnumerations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class InvalidationTrackerBenchmark(
    private val driver: UseDriver,
    private val sampleSize: Int,
    private val mode: Mode,
) {

    @get:Rule val benchmarkRule = BenchmarkRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val testScope = TestScope()

    @Before
    fun setup() {
        context.deleteDatabase(DB_NAME)
    }

    @Test
    fun largeTransaction() {
        val db =
            Room.databaseBuilder(context, TestDatabase::class.java, DB_NAME)
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                .setDriver(
                    when (driver) {
                        UseDriver.ANDROID -> AndroidSQLiteDriver()
                        UseDriver.BUNDLED -> BundledSQLiteDriver()
                    }
                )
                .build()

        val flowJob =
            testScope.launch(Dispatchers.IO) {
                db.invalidationTracker.createFlow("user").collect {}
            }

        val users = List(sampleSize) { User(it, "name$it") }

        benchmarkRule.measureRepeated {
            runWithTimingConditional(pauseTiming = mode == Mode.MEASURE_DELETE) {
                runBlocking {
                    // Insert the sample size
                    db.useWriterConnection { transactor ->
                        transactor.immediateTransaction {
                            for (user in users) {
                                db.getUserDao().insert(user)
                            }
                        }
                    }
                }
            }

            runWithTimingConditional(pauseTiming = mode == Mode.MEASURE_INSERT) {
                val result = runBlocking { db.getUserDao().deleteAll() }
                // Delete sample size (causing a large transaction)
                assertEquals(result, sampleSize)
            }
        }

        flowJob.cancel()
        db.close()
    }

    private inline fun runWithTimingConditional(pauseTiming: Boolean = false, block: () -> Unit) {
        if (pauseTiming) benchmarkRule.getState().pauseTiming()
        block()
        if (pauseTiming) benchmarkRule.getState().resumeTiming()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "driver={0}, sampleSize={1}, mode={2}")
        fun data(): List<Array<Any>> =
            generateAllEnumerations(
                UseDriver.entries,
                listOf(
                    100,
                    1000,
                    5000,
                    // Removed due to due to slow run times, see b/267544445 for details.
                    // 10000
                ),
                listOf(Mode.MEASURE_INSERT, Mode.MEASURE_DELETE, Mode.MEASURE_INSERT_AND_DELETE),
            )

        private const val DB_NAME = "invalidation-benchmark-test"
    }

    enum class UseDriver {
        ANDROID,
        BUNDLED,
    }

    enum class Mode {
        MEASURE_INSERT,
        MEASURE_DELETE,
        MEASURE_INSERT_AND_DELETE,
    }
}
