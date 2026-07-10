/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.room3.integration.kotlintestapp.test

import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.integration.kotlintestapp.test.TestDatabaseTest.UseDriver
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

// Verifies b/215583326
@LargeTest
@RunWith(Parameterized::class)
class SyncTriggersConcurrencyTest(val useDriver: UseDriver) {

    private companion object {

        private const val DB_NAME = "sample.db"
        private const val TABLE_NAME = "sample"

        private const val CONCURRENCY = 4
        private const val CHECK_ITERATIONS = 200

        @JvmStatic
        @Parameters(name = "useDriver={0}")
        fun parameters() = arrayOf(UseDriver.ANDROID, UseDriver.BUNDLED)
    }

    private val applicationContext = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var dispatcher: ExecutorCoroutineDispatcher
    private lateinit var database: SampleDatabase
    private lateinit var terminationSignal: AtomicBoolean

    @Before
    @OptIn(DelicateCoroutinesApi::class)
    fun setup() {
        applicationContext.deleteDatabase(DB_NAME)
        dispatcher = newFixedThreadPoolContext(Int.MAX_VALUE, "invalidation_tracker_test_thread_")
        database =
            Room.databaseBuilder<SampleDatabase>(applicationContext, DB_NAME)
                .setDriver(
                    when (useDriver) {
                        UseDriver.ANDROID -> AndroidSQLiteDriver()
                        UseDriver.BUNDLED -> BundledSQLiteDriver()
                    }
                )
                .setQueryCoroutineContext(dispatcher)
                .build()
        terminationSignal = AtomicBoolean(false)
    }

    @After
    fun tearDown() {
        terminationSignal.set(true)
        database.close()
        dispatcher.cancel()
        applicationContext.deleteDatabase(DB_NAME)
    }

    @Test
    fun test() = runTest {
        val invalidationTracker = database.invalidationTracker

        // Launch CONCURRENCY number of jobs which stress the InvalidationTracker by repeatedly
        // creating and canceling flows.
        repeat(CONCURRENCY) {
            launch(dispatcher) {
                while (!terminationSignal.get()) {
                    invalidationTracker.createFlow(TABLE_NAME).collect { cancel() }
                }
            }
        }

        // Repeatedly, CHECK_ITERATIONS number of times:
        // 1. Create a Flow (starts observation)
        // 2. Insert an entity
        // 4. Cancel the Flow (stops observation)
        // 5. No deadlock = assertion that invalidation was received
        val dao = database.getDao()
        repeat(CHECK_ITERATIONS) { iteration ->
            val collectingLatch = CompletableDeferred<Unit>()
            val collectedLatch = CompletableDeferred<Unit>()
            val collectJob =
                launch(dispatcher) {
                    invalidationTracker.createFlow(TABLE_NAME).collectIndexed { i, _ ->
                        when (i) {
                            0 -> collectingLatch.complete(Unit) // initial emission
                            1 -> collectedLatch.complete(Unit) // invalidation emission
                            else -> error("Received too many invalidations")
                        }
                    }
                }
            collectingLatch.await() // wait for collection to start
            dao.insert(SampleEntity(iteration.toString()))
            collectedLatch.await() // wait for invalidation
            collectJob.cancelAndJoin()
        }
    }

    @Database(entities = [SampleEntity::class], version = 1, exportSchema = false)
    abstract class SampleDatabase : RoomDatabase() {
        abstract fun getDao(): SampleDao
    }

    @Dao
    interface SampleDao {
        @Insert(onConflict = OnConflictStrategy.REPLACE) fun insert(count: SampleEntity)
    }

    @Entity(tableName = TABLE_NAME) class SampleEntity(@PrimaryKey val id: String)
}
