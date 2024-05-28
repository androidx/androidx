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

package androidx.room

import android.database.sqlite.SQLiteException
import android.os.CancellationSignal
import androidx.kruth.assertThat
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.fail
import org.junit.Test

@SmallTest
@OptIn(DelicateCoroutinesApi::class)
class CoroutineRoomCancellationTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val database = TestDatabase()

    private fun initWithDispatcher(dispatcher: CoroutineDispatcher) {
        database.init(
            DatabaseConfiguration(
                context = InstrumentationRegistry.getInstrumentation().targetContext,
                name = "test",
                sqliteOpenHelperFactory = FrameworkSQLiteOpenHelperFactory(),
                migrationContainer = RoomDatabase.MigrationContainer(),
                callbacks = null,
                allowMainThreadQueries = true,
                journalMode = RoomDatabase.JournalMode.TRUNCATE,
                queryExecutor = Executors.newSingleThreadExecutor(),
                transactionExecutor = Executors.newSingleThreadExecutor(),
                multiInstanceInvalidationServiceIntent = null,
                requireMigration = true,
                allowDestructiveMigrationOnDowngrade = false,
                migrationNotRequiredFrom = emptySet(),
                copyFromAssetPath = null,
                copyFromFile = null,
                copyFromInputStream = null,
                prepackagedDatabaseCallback = null,
                typeConverters = emptyList(),
                autoMigrationSpecs = emptyList(),
                allowDestructiveMigrationForAllTables = false,
                sqliteDriver = null,
                queryCoroutineContext = dispatcher
            )
        )
    }

    @Test
    fun testSuspend_cancellable_duringLongQuery() = runBlocking {
        initWithDispatcher(Dispatchers.IO)

        val inQueryLatch = CountDownLatch(1)
        val cancelledLatch = CountDownLatch(1)

        val cancellationSignal = CancellationSignal()
        cancellationSignal.setOnCancelListener {
            // query was cancelled so now we can finish our test
            cancelledLatch.countDown()
        }

        val job =
            GlobalScope.launch(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                CoroutinesRoom.execute(
                    db = database,
                    inTransaction = false,
                    cancellationSignal = cancellationSignal,
                    callable =
                        Callable {
                            // we're triggering our fake query
                            inQueryLatch.countDown()
                            // fake a long query so we can cancel
                            cancelledLatch.await()
                        }
                )
            }
        inQueryLatch.await()
        // we're in the query so we can cancel
        job.cancelAndJoin()

        assertThat(cancellationSignal.isCanceled).isTrue()
    }

    @Test
    fun testSuspend_cancellable_beforeQueryStarts() = runBlocking {
        initWithDispatcher(testDispatcher)

        val inCoroutineLatch = CountDownLatch(1)
        val cancelledLatch = CountDownLatch(1)

        val cancellationSignal = CancellationSignal()
        cancellationSignal.setOnCancelListener {
            // query was cancelled so now we can finish our test
            cancelledLatch.countDown()
        }

        val job =
            GlobalScope.launch(Dispatchers.IO) {
                // Coroutine started so now we can cancel it
                inCoroutineLatch.countDown()

                @Suppress("DEPRECATION")
                CoroutinesRoom.execute(
                    db = database,
                    inTransaction = false,
                    cancellationSignal = cancellationSignal,
                    callable =
                        Callable {
                            // this should never execute
                            fail("Blocking query triggered")
                        }
                )
            }
        inCoroutineLatch.await()
        job.cancelAndJoin()
        testDispatcher.scheduler.runCurrent()

        assertThat(cancellationSignal.isCanceled).isTrue()
    }

    @Test
    fun testSuspend_exception_in_query() = runBlocking {
        initWithDispatcher(Dispatchers.IO)
        val cancellationSignal = CancellationSignal()

        GlobalScope.launch(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                CoroutinesRoom.execute(
                    db = database,
                    inTransaction = false,
                    cancellationSignal = cancellationSignal,
                    callable = Callable { throw SQLiteException("stuff happened") }
                )
            } catch (exception: Throwable) {
                assertThat(exception).isInstanceOf<SQLiteException>()
            }
        }

        assertThat(cancellationSignal.isCanceled).isFalse()
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun testSuspend_notCancelled() = runBlocking {
        initWithDispatcher(testDispatcher)

        val cancellationSignal = CancellationSignal()

        val job =
            testScope.launch {
                @Suppress("DEPRECATION")
                CoroutinesRoom.execute(
                    db = database,
                    inTransaction = false,
                    cancellationSignal = cancellationSignal,
                    callable = Callable { /* nothing to do */ }
                )
            }
        testScope.runCurrent()
        // wait for the job to be finished
        job.join()

        assertThat(cancellationSignal.isCanceled).isFalse()
    }

    private class TestDatabase : RoomDatabase() {

        override fun createOpenDelegate(): RoomOpenDelegate {
            return object : RoomOpenDelegate(1, "", "") {
                override fun onCreate(connection: SQLiteConnection) {}

                override fun onPreMigrate(connection: SQLiteConnection) {}

                override fun onValidateSchema(connection: SQLiteConnection): ValidationResult {
                    return ValidationResult(true, null)
                }

                override fun onPostMigrate(connection: SQLiteConnection) {}

                override fun onOpen(connection: SQLiteConnection) {}

                override fun createAllTables(connection: SQLiteConnection) {}

                override fun dropAllTables(connection: SQLiteConnection) {}
            }
        }

        override fun createInvalidationTracker(): InvalidationTracker {
            return TestInvalidationTracker(this)
        }

        override fun clearAllTables() {
            throw UnsupportedOperationException("Shouldn't be called!")
        }
    }

    private class TestInvalidationTracker(db: RoomDatabase) :
        InvalidationTracker(db, emptyMap(), emptyMap(), "") {
        val observers = mutableListOf<Observer>()

        override fun addObserver(observer: Observer) {
            observers.add(observer)
        }

        override fun removeObserver(observer: Observer) {
            observers.remove(observer)
        }
    }
}
