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

package androidx.room3.support

import android.content.Context
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import androidx.arch.core.executor.testing.CountingTaskExecutorRule
import androidx.kruth.assertThat
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.ExperimentalRoomApi
import androidx.room3.Insert
import androidx.room3.InvalidationTracker
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@MediumTest
@OptIn(ExperimentalRoomApi::class)
class AutoClosingDatabaseTest {
    @get:Rule val executorRule = CountingTaskExecutorRule()

    private lateinit var db: TestDatabase
    private lateinit var userDao: TestUserDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("testDb")
        db =
            Room.databaseBuilder(context, TestDatabase::class.java, "testDb")
                .setAutoCloseTimeout(10, TimeUnit.MILLISECONDS)
                .build()
        userDao = db.getUserDao()
    }

    @After
    fun cleanUp() {
        executorRule.drainTasks(1, TimeUnit.SECONDS)
        assertThat(executorRule.isIdle).isTrue()
        db.close()
    }

    @Test
    fun invalidationObserver_notifiedByTableName() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("test.db")

        val db: TestDatabase =
            Room.databaseBuilder(context, TestDatabase::class.java, "test.db")
                .setAutoCloseTimeout(0, TimeUnit.MILLISECONDS)
                .build()

        val invalidationCount = AtomicInteger(0)

        db.invalidationTracker.addObserver(
            object : InvalidationTracker.Observer("user") {
                override fun onInvalidated(tables: Set<String>) {
                    invalidationCount.getAndIncrement()
                }
            }
        )

        db.getUserDao().insert(TestUser(1, "bob"))

        executorRule.drainTasks(1, TimeUnit.SECONDS)
        assertThat(invalidationCount.get()).isEqualTo(1)

        delay(100) // Let db auto close

        db.invalidationTracker.notifyObserversByTableNames(setOf("user"))

        executorRule.drainTasks(1, TimeUnit.SECONDS)
        assertThat(invalidationCount.get()).isEqualTo(2)

        db.close()
    }

    /**
     * Validate that re-opening the database after auto-closed through a suspend DAO function will
     * not cause a IO on the main thread.
     */
    @Test
    fun suspendQueryMainThreadAutoClosed() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val currentPolicy = StrictMode.getThreadPolicy()
            StrictMode.setThreadPolicy(
                ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().penaltyDeath().build()
            )
            try {
                runBlocking {
                    db.getUserDao().getAllSuspend()
                    delay(20) // let db auto-close
                    db.getUserDao().getAllSuspend()
                }
            } finally {
                StrictMode.setThreadPolicy(currentPolicy)
            }
        }
        db.close()
    }

    @Test
    fun twoThreadsConcurrentlyStressTest() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("testDb")

        // One nanosecond is basically 'zero' but we use it to bypass the check in setAutoClose().
        // We use such value because it has higher probability of revealing concurrency issues,
        // making this test more useful.
        val db =
            Room.databaseBuilder<TestDatabase>(context, "testDb")
                .setAutoCloseTimeout(1, TimeUnit.NANOSECONDS)
                .build()

        List(2) { coroutineId ->
                when (coroutineId) {
                    0 ->
                        launch(Dispatchers.IO) {
                            repeat(1000) { db.getUserDao().insert(TestUser(it.toLong(), "$it")) }
                        }
                    1 -> launch(Dispatchers.IO) { repeat(1000) { db.getUserDao().getAll() } }
                    else -> error("Too many repeat")
                }
            }
            .joinAll()

        db.close()
    }

    /**
     * A stress test to validate that an auto-closed database does not deadlock when re-opening the
     * database and re-configuring the connection with the InvalidationTracker at the same time a
     * reactive query (Flow) is also syncing triggers and grabbing InvalidationTracker locks.
     * b/446643789
     */
    @SdkSuppress(minSdkVersion = 24) // b/454114873
    @Test
    fun flowReadAndInsertConcurrentlyStressTest() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()

        repeat(1000) {
            context.deleteDatabase("testDb")
            // Ideally we would use one nanosecond which is basically 'zero' but that has the side
            // effect of closing the database in-between our two concurrent operations, not
            // reproducing the issue. Therefore we still use a small timeout (1ms) and await
            // for 2ms to guarantee the database is indeed closed and both read and writers are
            // executed concurrently.
            val db =
                Room.databaseBuilder<TestDatabase>(context, "testDb")
                    .setAutoCloseTimeout(1, TimeUnit.MILLISECONDS)
                    .build()
            db.getUserDao().insert(TestUser(1L, "1"))
            withContext(Dispatchers.IO) { delay(2) }

            val readJob =
                launch(Dispatchers.IO) { db.getUserDao().getFlow(2L).first { it != null } }
            val writeJob = launch(Dispatchers.IO) { db.getUserDao().insert(TestUser(2L, "2")) }
            listOf(readJob, writeJob).joinAll()
            db.close()
        }
    }

    @Database(entities = [TestUser::class], version = 1, exportSchema = false)
    abstract class TestDatabase : RoomDatabase() {
        abstract fun getUserDao(): TestUserDao
    }

    @Dao
    interface TestUserDao {
        @Insert fun insert(user: TestUser)

        @Insert suspend fun insertSuspend(user: TestUser)

        @Query("SELECT * FROM user") fun getAll(): List<TestUser>

        @Query("SELECT * FROM user") suspend fun getAllSuspend(): List<TestUser>

        @Query("SELECT * FROM user WHERE id = :id") fun get(id: Long): TestUser

        @Query("SELECT * FROM user WHERE id = :id") fun getFlow(id: Long): Flow<TestUser?>
    }

    @Entity(tableName = "user") data class TestUser(@PrimaryKey val id: Long, val data: String)
}
