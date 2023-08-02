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

package androidx.room.integration.kotlintestapp.test

import androidx.arch.core.executor.testing.CountingTaskExecutorRule
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.InvalidationTracker
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Verifies b/215583326
@LargeTest
@RunWith(AndroidJUnit4::class)
class SyncTriggersConcurrencyTest {

    @Rule
    @JvmField
    val countingTaskExecutorRule = CountingTaskExecutorRule()

    private lateinit var executor: ExecutorService
    private lateinit var database: SampleDatabase
    private lateinit var terminationSignal: AtomicBoolean

    @Before
    fun setup() {
        val applicationContext = InstrumentationRegistry.getInstrumentation().targetContext
        val threadId = AtomicInteger()
        executor = Executors.newCachedThreadPool { runnable ->
            Thread(runnable).apply {
                name = "invalidation_tracker_test_worker_${threadId.getAndIncrement()}"
            }
        }
        database = Room
            .databaseBuilder(applicationContext, SampleDatabase::class.java, DB_NAME)
            .build()
        terminationSignal = AtomicBoolean()
    }

    @After
    fun tearDown() {
        terminationSignal.set(true)
        executor.shutdown()
        val terminated = executor.awaitTermination(1L, TimeUnit.SECONDS)
        countingTaskExecutorRule.drainTasks(500, TimeUnit.MILLISECONDS)
        database.close()
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(DB_NAME)
        check(terminated)
        check(countingTaskExecutorRule.isIdle)
    }

    @Test
    fun test() {
        val invalidationTracker = database.invalidationTracker

        // Launch CONCURRENCY number of tasks which stress the InvalidationTracker by repeatedly
        // registering and unregistering observers.
        repeat(CONCURRENCY) {
            executor.execute(StressRunnable(invalidationTracker, terminationSignal))
        }

        // Repeatedly, CHECK_ITERATIONS number of times:
        // 1. Add an observer
        // 2. Insert an entity
        // 4. Remove the observer
        // 5. Assert that the observer received an invalidation call.
        val dao = database.getDao()
        repeat(CHECK_ITERATIONS) { iteration ->
            val checkObserver = TestObserver(
                expectedInvalidationCount = 1
            )
            invalidationTracker.addObserver(checkObserver)
            try {
                val entity = SampleEntity(UUID.randomUUID().toString())
                dao.insert(entity)
                val countedDown = checkObserver.latch.await(10L, TimeUnit.SECONDS)
                assertTrue(countedDown, "iteration $iteration timed out")
            } finally {
                invalidationTracker.removeObserver(checkObserver)
            }
        }
    }

    /**
     * Stresses the invalidation tracker by repeatedly adding and removing an observer.
     * @property invalidationTracker the invalidation tracker
     * @property terminationSignal when set to true, signals the loop to terminate
     */
    private class StressRunnable(
        private val invalidationTracker: InvalidationTracker,
        private val terminationSignal: AtomicBoolean,
    ) : Runnable {

        val observer = TestObserver()

        override fun run() {
            while (!terminationSignal.get()) {
                invalidationTracker.addObserver(observer)
                invalidationTracker.removeObserver(observer)
            }
        }
    }

    private class TestObserver(
        expectedInvalidationCount: Int = 0
    ) : InvalidationTracker.Observer(SampleEntity::class.java.simpleName) {

        val latch = CountDownLatch(expectedInvalidationCount)

        override fun onInvalidated(tables: Set<String>) {
            latch.countDown()
        }
    }

    @Database(entities = [SampleEntity::class], version = 1, exportSchema = false)
    abstract class SampleDatabase : RoomDatabase() {
        abstract fun getDao(): SampleDao
    }

    @Dao
    interface SampleDao {

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        fun insert(count: SampleEntity)

        @Delete
        fun delete(count: SampleEntity)
    }

    @Entity
    class SampleEntity(
        @PrimaryKey val id: String,
    )

    companion object {

        private const val DB_NAME = "sample.db"

        private const val CONCURRENCY = 4
        private const val CHECK_ITERATIONS = 200
    }
}
