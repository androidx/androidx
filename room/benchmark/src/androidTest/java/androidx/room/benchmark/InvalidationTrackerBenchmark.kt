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

package androidx.room.benchmark

import android.os.Build
import androidx.benchmark.BenchmarkRule
import androidx.benchmark.measureRepeated
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.InvalidationTracker
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN) // TODO Fix me for API 15 - b/120098504
class InvalidationTrackerBenchmark(private val sampleSize: Int, private val mode: Mode) {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    val context = ApplicationProvider.getApplicationContext() as android.content.Context

    @Before
    fun setup() {
        for (postfix in arrayOf("", "-wal", "-shm")) {
            val dbFile = context.getDatabasePath(DB_NAME + postfix)
            if (dbFile.exists()) {
                assertTrue(dbFile.delete())
            }
        }
    }

    @Test
    fun largeTransaction() {
        val db = Room.databaseBuilder(context, TestDatabase::class.java, DB_NAME)
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .build()

        val observer = object : InvalidationTracker.Observer("user") {
            override fun onInvalidated(tables: MutableSet<String>) {}
        }
        db.invalidationTracker.addObserver(observer)

        val users = List(sampleSize) { User(it, "name$it") }

        benchmarkRule.measureRepeated {
            runWithTimingConditional(pauseTiming = mode == Mode.MEASURE_DELETE) {
                // Insert the sample size
                db.runInTransaction {
                    for (user in users) {
                        db.getUserDao().insert(user)
                    }
                }
            }

            runWithTimingConditional(pauseTiming = mode == Mode.MEASURE_INSERT) {
                // Delete sample size (causing a large transaction)
                assertEquals(db.getUserDao().deleteAll(), sampleSize)
            }
        }

        db.close()
    }

    private inline fun runWithTimingConditional(
        pauseTiming: Boolean = false,
        block: () -> Unit
    ) {
        if (pauseTiming) benchmarkRule.getState().pauseTiming()
        block()
        if (pauseTiming) benchmarkRule.getState().resumeTiming()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "sampleSize={0}, mode={1}")
        fun data(): List<Array<Any>> {
            return mutableListOf<Array<Any>>().apply {
                arrayOf(
                    Mode.MEASURE_INSERT,
                    Mode.MEASURE_DELETE,
                    Mode.MEASURE_INSERT_AND_DELETE
                ).forEach { mode ->
                    arrayOf(100, 1000, 5000, 10000).forEach { sampleSize ->
                        add(arrayOf(sampleSize, mode))
                    }
                }
            }
        }

        private const val DB_NAME = "invalidation-benchmark-test"
    }

    @Database(entities = [User::class], version = 1, exportSchema = false)
    abstract class TestDatabase : RoomDatabase() {
        abstract fun getUserDao(): UserDao
    }

    @Entity
    data class User(@PrimaryKey val id: Int, val name: String)

    @Dao
    interface UserDao {
        @Insert
        fun insert(user: User)

        @Query("DELETE FROM User")
        fun deleteAll(): Int
    }

    enum class Mode {
        MEASURE_INSERT,
        MEASURE_DELETE,
        MEASURE_INSERT_AND_DELETE
    }
}
