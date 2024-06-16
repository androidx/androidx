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

package androidx.room.integration.kotlintestapp.test

import android.content.Context
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import androidx.kruth.assertThat
import androidx.kruth.assertWithMessage
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.integration.kotlintestapp.NewThreadDispatcher
import androidx.room.integration.kotlintestapp.TestDatabase
import androidx.room.integration.kotlintestapp.vo.Book
import androidx.room.integration.kotlintestapp.vo.Counter
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.intercepted
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.resume
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.fail
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class SuspendingQueryTest : TestDatabaseTest() {

    @After
    fun teardown() {
        // At the end of all tests, query executor should be idle (transaction thread released).
        countingTaskExecutorRule.drainTasks(500, TimeUnit.MILLISECONDS)
        assertThat(countingTaskExecutorRule.isIdle).isTrue()
    }

    @Test
    fun bookByIdSuspend() {
        runBlocking {
            booksDao.addAuthors(TestUtil.AUTHOR_1)
            booksDao.addPublishers(TestUtil.PUBLISHER)
            booksDao.addBooks(TestUtil.BOOK_1)

            assertThat(booksDao.getBookSuspend(TestUtil.BOOK_1.bookId)).isEqualTo(TestUtil.BOOK_1)
        }
    }

    // Need to add other return type tests
    @Test
    fun upsertBookSuspend() {
        runBlocking {
            booksDao.addPublishers(TestUtil.PUBLISHER)
            booksDao.upsertBookSuspend(TestUtil.BOOK_1)

            assertThat(booksDao.getBookSuspend(TestUtil.BOOK_1.bookId)).isEqualTo(TestUtil.BOOK_1)
        }
    }

    @Test
    fun upsertSuspendLong() {
        runBlocking {
            booksDao.addPublishers(TestUtil.PUBLISHER)
            booksDao.upsertBookSuspendReturnId(TestUtil.BOOK_1).let { result ->
                assertThat(booksDao.getBookSuspend(TestUtil.BOOK_1.bookId))
                    .isEqualTo(TestUtil.BOOK_1)
                assertThat(result).isEqualTo(1)
            }
            booksDao.upsertBookSuspendReturnId(TestUtil.BOOK_1.copy(title = "changed title")).let {
                result ->
                assertThat(result).isEqualTo(-1)
            }
        }
    }

    @Test
    fun upsertSuspendLongList() {
        runBlocking {
            booksDao.addPublishers(TestUtil.PUBLISHER)
            val bookList =
                buildList<Book> {
                    add(TestUtil.BOOK_1)
                    add(TestUtil.BOOK_2)
                    add(TestUtil.BOOK_3)
                }
            booksDao.upsertBooksSuspendReturnIds(bookList).let { results ->
                assertThat(results.size).isEqualTo(3)
                assertThat(results).containsExactly(1L, 2L, 3L)
            }
        }
    }

    @Test
    fun allBookSuspend() {
        runBlocking {
            booksDao.addAuthors(TestUtil.AUTHOR_1)
            booksDao.addPublishers(TestUtil.PUBLISHER)
            booksDao.addBooks(TestUtil.BOOK_1, TestUtil.BOOK_2)

            val books = booksDao.getBooksSuspend()

            assertThat(books.size).isEqualTo((2))
            assertThat(books[0]).isEqualTo(TestUtil.BOOK_1)
            assertThat(books[1]).isEqualTo(TestUtil.BOOK_2)
        }
    }

    @OptIn(androidx.room.ExperimentalRoomApi::class)
    @Test
    fun allBookSuspend_autoClose() {
        val context: Context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("autoClose.db")
        val db =
            Room.databaseBuilder(
                    context = context,
                    klass = TestDatabase::class.java,
                    name = "test.db"
                )
                .setAutoCloseTimeout(10, TimeUnit.MILLISECONDS)
                .build()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            StrictMode.setThreadPolicy(
                ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().penaltyDeath().build()
            )
            runBlocking {
                db.booksDao().getBooksSuspend()
                delay(100) // let db auto-close
                db.booksDao().getBooksSuspend()
            }
        }
    }

    @Test
    fun allBookSuspend_closed() {
        val context: Context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("autoClose.db")
        val db =
            Room.databaseBuilder(
                    context = context,
                    klass = TestDatabase::class.java,
                    name = "test.db"
                )
                .build()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            StrictMode.setThreadPolicy(
                ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().penaltyDeath().build()
            )
            runBlocking {
                // Opens DB, isOpen && inTransaction check should not cause violation
                db.booksDao().getBooksSuspend()
                // DB is open, isOpen && inTransaction check should not cause violation
                db.booksDao().getBooksSuspend()
            }
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun suspendingBlock_beginEndTransaction() {
        runBlocking {
            try {
                database.beginTransaction()
                booksDao.insertPublisherSuspend(
                    TestUtil.PUBLISHER.publisherId,
                    TestUtil.PUBLISHER.name
                )
                booksDao.insertBookSuspend(TestUtil.BOOK_1.copy(salesCnt = 0))
                booksDao.insertBookSuspend(TestUtil.BOOK_2)

                booksDao.deleteUnsoldBooks()
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
        }
        assertThat(booksDao.getAllBooks()).isEqualTo(listOf(TestUtil.BOOK_2))
    }

    @Test
    @Suppress("DEPRECATION")
    fun suspendingBlock_beginEndTransaction_blockingDaoMethods() {
        runBlocking {
            try {
                database.beginTransaction()
                booksDao.insertPublisherSuspend(
                    TestUtil.PUBLISHER.publisherId,
                    TestUtil.PUBLISHER.name
                )

                booksDao.addBooks(TestUtil.BOOK_1.copy(salesCnt = 0), TestUtil.BOOK_2)

                booksDao.deleteUnsoldBooks()
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
        }
        assertThat(booksDao.getAllBooks()).isEqualTo(listOf(TestUtil.BOOK_2))
    }

    @Test
    @Suppress("DEPRECATION")
    fun suspendingBlock_beginEndTransaction_newThreadDispatcher() {
        runBlocking(NewThreadDispatcher()) {
            try {
                database.beginTransaction()
                booksDao.insertPublisherSuspend(
                    TestUtil.PUBLISHER.publisherId,
                    TestUtil.PUBLISHER.name
                )
                booksDao.insertBookSuspend(TestUtil.BOOK_1.copy(salesCnt = 0))
                booksDao.insertBookSuspend(TestUtil.BOOK_2)

                booksDao.deleteUnsoldBooks()
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
        }
        assertThat(booksDao.getAllBooks()).isEqualTo(listOf(TestUtil.BOOK_2))
    }

    @Test
    fun suspendingBlock_blockingDaoMethods() {
        runBlocking {
            booksDao.insertPublisherSuspend(TestUtil.PUBLISHER.publisherId, TestUtil.PUBLISHER.name)

            booksDao.addBooks(TestUtil.BOOK_1)

            assertThat(booksDao.getBooksSuspend()).isEqualTo(listOf(TestUtil.BOOK_1))
        }
    }

    @Test
    fun runSuspendingTransaction() {
        runBlocking {
            database.withTransaction {
                booksDao.insertPublisherSuspend(
                    TestUtil.PUBLISHER.publisherId,
                    TestUtil.PUBLISHER.name
                )
                booksDao.insertBookSuspend(TestUtil.BOOK_1.copy(salesCnt = 0))
                booksDao.insertBookSuspend(TestUtil.BOOK_2)
                booksDao.deleteUnsoldBooks()
            }
            assertThat(booksDao.getBooksSuspend()).isEqualTo(listOf(TestUtil.BOOK_2))
        }
    }

    @Test
    fun withTransaction_newThreadDispatcher() {
        runBlocking(NewThreadDispatcher()) {
            database.withTransaction {
                booksDao.insertPublisherSuspend(
                    TestUtil.PUBLISHER.publisherId,
                    TestUtil.PUBLISHER.name
                )
                booksDao.insertBookSuspend(TestUtil.BOOK_1.copy(salesCnt = 0))
                booksDao.insertBookSuspend(TestUtil.BOOK_2)
                booksDao.deleteUnsoldBooks()
            }
        }
        assertThat(booksDao.getAllBooks()).isEqualTo(listOf(TestUtil.BOOK_2))
    }

    @Test
    fun withTransaction_withContext_newThreadDispatcher() {
        runBlocking {
            withContext(NewThreadDispatcher()) {
                database.withTransaction {
                    booksDao.insertPublisherSuspend(
                        TestUtil.PUBLISHER.publisherId,
                        TestUtil.PUBLISHER.name
                    )
                    booksDao.insertBookSuspend(TestUtil.BOOK_1.copy(salesCnt = 0))
                    booksDao.insertBookSuspend(TestUtil.BOOK_2)
                    booksDao.deleteUnsoldBooks()
                }
            }
        }
        assertThat(booksDao.getAllBooks()).isEqualTo(listOf(TestUtil.BOOK_2))
    }

    @Test
    fun withTransaction_ioDispatcher() {
        runBlocking(Dispatchers.IO) {
            database.withTransaction {
                booksDao.insertPublisherSuspend(
                    TestUtil.PUBLISHER.publisherId,
                    TestUtil.PUBLISHER.name
                )
                booksDao.insertBookSuspend(TestUtil.BOOK_1.copy(salesCnt = 0))
                booksDao.insertBookSuspend(TestUtil.BOOK_2)
                booksDao.deleteUnsoldBooks()
            }
        }
        assertThat(booksDao.getAllBooks()).isEqualTo(listOf(TestUtil.BOOK_2))
    }

    @Test
    fun withTransaction_contextSwitch() {
        runBlocking {
            database.withTransaction {
                booksDao.insertPublisherSuspend(
                    TestUtil.PUBLISHER.publisherId,
                    TestUtil.PUBLISHER.name
                )
                withContext(Dispatchers.IO) {
                    booksDao.insertBookSuspend(TestUtil.BOOK_1.copy(salesCnt = 0))
                    booksDao.insertBookSuspend(TestUtil.BOOK_2)
                }
                booksDao.deleteUnsoldBooks()
            }
            assertThat(booksDao.getBooksSuspend()).isEqualTo(listOf(TestUtil.BOOK_2))
        }
    }

    @Test
    fun withTransaction_contextSwitch_exception() {
        runBlocking {
            try {
                database.withTransaction {
                    booksDao.insertPublisherSuspend(
                        TestUtil.PUBLISHER.publisherId,
                        TestUtil.PUBLISHER.name
                    )
                    withContext(Dispatchers.IO) {
                        booksDao.insertBookSuspend(TestUtil.BOOK_1.copy(salesCnt = 0))
                        booksDao.insertBookSuspend(TestUtil.BOOK_2)
                    }
                    booksDao.deleteUnsoldBooks()
                    throw IOException("Boom!")
                }
            } catch (ex: IOException) {
                assertThat(ex).hasMessageThat().contains("Boom")
            }
            assertThat(booksDao.getPublishersSuspend()).isEmpty()
            assertThat(booksDao.getBooksSuspend()).isEmpty()
        }
    }

    @Test
    fun withTransaction_exception() {
        runBlocking {
            database.withTransaction {
                booksDao.insertPublisherSuspend(
                    TestUtil.PUBLISHER.publisherId,
                    TestUtil.PUBLISHER.name
                )
                booksDao.insertBookSuspend(TestUtil.BOOK_1)
            }

            try {
                database.withTransaction {
                    booksDao.insertBookSuspend(TestUtil.BOOK_2)
                    booksDao.insertBookSuspend(TestUtil.BOOK_3)
                    throw IOException("Boom!")
                }
                @Suppress("UNREACHABLE_CODE") fail("An exception should have been thrown.")
            } catch (ex: IOException) {
                assertThat(ex).hasMessageThat().contains("Boom")
            }

            assertThat(booksDao.getBooksSuspend()).isEqualTo(listOf(TestUtil.BOOK_1))
        }
    }

    @Test
    fun withTransaction_nested() {
        runBlocking {
            database.withTransaction {
                booksDao.insertPublisherSuspend(
                    TestUtil.PUBLISHER.publisherId,
                    TestUtil.PUBLISHER.name
                )
                database.withTransaction {
                    booksDao.insertBookSuspend(TestUtil.BOOK_1.copy(salesCnt = 0))
                    booksDao.insertBookSuspend(TestUtil.BOOK_2)
                }
                booksDao.deleteUnsoldBooks()
            }
            assertThat(booksDao.getBooksSuspend()).isEqualTo(listOf(TestUtil.BOOK_2))
        }
    }

    @Test
    fun withTransaction_nested_daoTransaction() {
        runBlocking {
            database.withTransaction {
                booksDao.insertPublisherSuspend(
                    TestUtil.PUBLISHER.publisherId,
                    TestUtil.PUBLISHER.name
                )
                database.withTransaction {
                    booksDao.insertBookSuspend(TestUtil.BOOK_1.copy(salesCnt = 0))
                    booksDao.insertBookSuspend(TestUtil.BOOK_2)
                }
                booksDao.deleteBooksWithZeroSales()
            }
            assertThat(booksDao.getBooksSuspend()).isEqualTo(listOf(TestUtil.BOOK_2))
        }
    }

    @Test
    fun withTransaction_nested_exception() {
        runBlocking {
            database.withTransaction {
                booksDao.insertPublisherSuspend(
                    TestUtil.PUBLISHER.publisherId,
                    TestUtil.PUBLISHER.name
                )

                try {
                    database.withTransaction {
                        booksDao.insertBookSuspend(TestUtil.BOOK_1.copy(salesCnt = 0))
                        throw IOException("Boom!")
                    }
                    @Suppress("UNREACHABLE_CODE") fail("An exception should have been thrown.")
                } catch (ex: IOException) {
                    assertThat(ex).hasMessageThat().contains("Boom")
                }
            }

            assertThat(booksDao.getPublishersSuspend()).isEmpty()
            assertThat(booksDao.getBooksSuspend()).isEmpty()
        }
    }

    @Test
    fun withTransaction_nested_finally() {
        runBlocking {
            var finallyBlocksExecuted = 0
            try {
                database.withTransaction {
                    try {
                        database.withTransaction { throw IOException("Boom!") }
                        @Suppress("UNREACHABLE_CODE") fail("An exception should have been thrown.")
                    } catch (ex: IOException) {
                        assertThat(ex).hasMessageThat().contains("Boom")
                    } finally {
                        finallyBlocksExecuted++
                    }
                }
            } finally {
                finallyBlocksExecuted++
            }

            assertThat(finallyBlocksExecuted).isEqualTo(2)
        }
    }

    @Test
    fun withTransaction_nested_contextSwitch() {
        runBlocking {
            database.withTransaction {
                booksDao.insertPublisherSuspend(
                    TestUtil.PUBLISHER.publisherId,
                    TestUtil.PUBLISHER.name
                )
                withContext(Dispatchers.IO) {
                    database.withTransaction {
                        booksDao.insertBookSuspend(TestUtil.BOOK_1.copy(salesCnt = 0))
                        booksDao.insertBookSuspend(TestUtil.BOOK_2)
                    }
                }
                booksDao.deleteUnsoldBooks()
            }
            assertThat(booksDao.getBooksSuspend()).isEqualTo(listOf(TestUtil.BOOK_2))
        }
    }

    @Test
    fun withTransaction_childCoroutine_defaultDispatcher() {
        runBlocking {
            database.withTransaction {
                booksDao.insertPublisherSuspend(
                    TestUtil.PUBLISHER.publisherId,
                    TestUtil.PUBLISHER.name
                )
                coroutineScope {
                    launch {
                        booksDao.insertBookSuspend(TestUtil.BOOK_1)
                        booksDao.insertBookSuspend(TestUtil.BOOK_2)
                    }
                }
            }
            assertThat(booksDao.getBooksSuspend())
                .isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_2))
        }
    }

    @Test
    fun withTransaction_childCoroutine_ioDispatcher() {
        runBlocking {
            database.withTransaction {
                booksDao.insertPublisherSuspend(
                    TestUtil.PUBLISHER.publisherId,
                    TestUtil.PUBLISHER.name
                )
                coroutineScope {
                    launch(Dispatchers.IO) {
                        booksDao.insertBookSuspend(TestUtil.BOOK_1)
                        booksDao.insertBookSuspend(TestUtil.BOOK_2)
                    }
                }
            }
            assertThat(booksDao.getBooksSuspend())
                .isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_2))
        }
    }

    @Test
    fun withTransaction_cancelCoroutine() {

        runBlocking {
            booksDao.insertPublisherSuspend(TestUtil.PUBLISHER.publisherId, TestUtil.PUBLISHER.name)
            booksDao.insertBookSuspend(TestUtil.BOOK_1)

            val startedRunning = CountDownLatch(1)
            var insertAttempted = false
            val job =
                launch(Dispatchers.IO) {
                    database.withTransaction {
                        // insert before delaying, to then assert transaction is not committed
                        booksDao.insertBookSuspend(TestUtil.BOOK_2)
                        insertAttempted = true
                        startedRunning.countDown()
                        // delay so we can cancel
                        delay(Long.MAX_VALUE)
                    }
                }

            assertThat(startedRunning.await(1, TimeUnit.SECONDS)).isTrue()
            job.cancelAndJoin()

            booksDao.insertBookSuspend(TestUtil.BOOK_3)

            assertThat(insertAttempted).isTrue() // make sure we attempted to insert
            assertThat(booksDao.getBooksSuspend())
                .isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_3))
        }
    }

    @Test
    fun withTransaction_busyExecutor_cancelCoroutine() {
        val executorService = Executors.newSingleThreadExecutor()
        val localDatabase =
            Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    TestDatabase::class.java
                )
                .setTransactionExecutor(executorService)
                .build()

        // Simulate a busy executor, no thread to acquire for transaction.
        val busyLatch = CountDownLatch(1)
        executorService.execute { busyLatch.await() }
        runBlocking {
            val startedRunning = CountDownLatch(1)
            val job =
                launch(Dispatchers.IO) {
                    startedRunning.countDown()
                    delay(200) // yield and delay to queue the runnable in transaction executor
                    localDatabase.withTransaction {
                        fail("Transaction block should have never run!")
                    }
                }

            assertThat(startedRunning.await(1, TimeUnit.SECONDS)).isTrue()
            job.cancelAndJoin()
        }

        // free busy thread
        busyLatch.countDown()
        executorService.shutdown()
        assertThat(executorService.awaitTermination(1, TimeUnit.SECONDS)).isTrue()

        assertThat(localDatabase.booksDao().getPublishers()).isEmpty()
    }

    @Test
    fun withTransaction_blockingDaoMethods() {
        runBlocking {
            database.withTransaction {
                booksDao.insertPublisherSuspend(
                    TestUtil.PUBLISHER.publisherId,
                    TestUtil.PUBLISHER.name
                )
                booksDao.addBooks(TestUtil.BOOK_1.copy(salesCnt = 0))
                booksDao.addBooks(TestUtil.BOOK_2)

                booksDao.deleteUnsoldBooks()
            }
            assertThat(booksDao.getBooksSuspend()).isEqualTo(listOf(TestUtil.BOOK_2))
        }
    }

    @Test
    fun withTransaction_blockingDaoMethods_contextSwitch() {
        runBlocking {
            database.withTransaction {
                // normal query
                try {
                    withContext(Dispatchers.IO) { booksDao.getBook("b1") }
                    fail("An exception should have been thrown")
                } catch (ex: IllegalStateException) {
                    assertThat(ex)
                        .hasMessageThat()
                        .contains("Cannot access database on a different coroutine context")
                }

                // delete or update shortcut
                try {
                    withContext(Dispatchers.IO) { booksDao.deleteUnsoldBooks() }
                    fail("An exception should have been thrown")
                } catch (ex: IllegalStateException) {
                    assertThat(ex)
                        .hasMessageThat()
                        .contains("Cannot access database on a different coroutine context")
                }

                // insert shortcut
                try {
                    withContext(Dispatchers.IO) { booksDao.insertPublisherVoid("p1", "publisher1") }
                    fail("An exception should have been thrown")
                } catch (ex: IllegalStateException) {
                    assertThat(ex)
                        .hasMessageThat()
                        .contains("Cannot access database on a different coroutine context")
                }

                // shared prepared query
                try {
                    withContext(Dispatchers.IO) { booksDao.addPublishers(TestUtil.PUBLISHER) }
                    fail("An exception should have been thrown")
                } catch (ex: IllegalStateException) {
                    assertThat(ex)
                        .hasMessageThat()
                        .contains("Cannot access database on a different coroutine context")
                }

                // prepared query
                try {
                    withContext(Dispatchers.IO) { booksDao.deleteBookWithIds("b1", "b2") }
                    fail("An exception should have been thrown")
                } catch (ex: IllegalStateException) {
                    assertThat(ex)
                        .hasMessageThat()
                        .contains("Cannot access database on a different coroutine context")
                }
            }
        }
    }

    @Test
    @Suppress("DeferredResultUnused")
    fun withTransaction_async() {
        runBlocking {
            database.withTransaction {
                booksDao.insertPublisherSuspend(
                    TestUtil.PUBLISHER.publisherId,
                    TestUtil.PUBLISHER.name
                )

                coroutineScope {
                    async { booksDao.insertBookSuspend(TestUtil.BOOK_1) }
                    async(Dispatchers.Default) { booksDao.insertBookSuspend(TestUtil.BOOK_2) }
                    async(Dispatchers.IO) { booksDao.insertBookSuspend(TestUtil.BOOK_3) }
                }
            }

            // as Set since insertion order is undefined
            assertThat(booksDao.getBooksSuspend().toSet())
                .isEqualTo(setOf(TestUtil.BOOK_1, TestUtil.BOOK_2, TestUtil.BOOK_3))
        }
    }

    @Test
    @Suppress("DeferredResultUnused")
    fun withTransaction_async_ioDispatcher() {
        runBlocking {
            database.withTransaction {
                booksDao.insertPublisherSuspend(
                    TestUtil.PUBLISHER.publisherId,
                    TestUtil.PUBLISHER.name
                )

                withContext(Dispatchers.IO) {
                    async { booksDao.insertBookSuspend(TestUtil.BOOK_1) }
                    async { booksDao.insertBookSuspend(TestUtil.BOOK_2) }
                    async { booksDao.insertBookSuspend(TestUtil.BOOK_3) }
                }
            }

            // as Set since insertion order is undefined
            assertThat(booksDao.getBooksSuspend().toSet())
                .isEqualTo(setOf(TestUtil.BOOK_1, TestUtil.BOOK_2, TestUtil.BOOK_3))
        }
    }

    @Test
    @Suppress("DeferredResultUnused")
    fun withTransaction_multipleTransactions() {
        runBlocking {
            booksDao.insertPublisherSuspend(TestUtil.PUBLISHER.publisherId, TestUtil.PUBLISHER.name)

            async { database.withTransaction { booksDao.insertBookSuspend(TestUtil.BOOK_1) } }

            async { database.withTransaction { booksDao.insertBookSuspend(TestUtil.BOOK_2) } }
        }

        // as Set since insertion order is undefined
        assertThat(booksDao.getAllBooks().toSet())
            .isEqualTo(setOf(TestUtil.BOOK_1, TestUtil.BOOK_2))
    }

    @Test
    @Suppress("DeferredResultUnused")
    fun withTransaction_multipleTransactions_multipleThreads() {
        runBlocking {
            booksDao.insertPublisherSuspend(TestUtil.PUBLISHER.publisherId, TestUtil.PUBLISHER.name)

            @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
            async(newSingleThreadContext("asyncThread1")) {
                database.withTransaction {
                    delay(100)
                    booksDao.insertBookSuspend(TestUtil.BOOK_1)
                }
            }

            @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
            async(newSingleThreadContext("asyncThread2")) {
                database.withTransaction {
                    delay(100)
                    booksDao.insertBookSuspend(TestUtil.BOOK_2)
                }
            }
        }

        // as Set since insertion order is undefined
        assertThat(booksDao.getAllBooks().toSet())
            .isEqualTo(setOf(TestUtil.BOOK_1, TestUtil.BOOK_2))
    }

    @Ignore // b/263502892
    @Test
    @Suppress("DeferredResultUnused")
    fun withTransaction_multipleTransactions_verifyThreadUsage() {
        val busyThreadsCount = AtomicInteger()
        val wrappedExecutor = BusyCountingService(busyThreadsCount, Executors.newCachedThreadPool())
        val localDatabase =
            Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    TestDatabase::class.java
                )
                .setQueryExecutor(ArchTaskExecutor.getIOThreadExecutor())
                .setTransactionExecutor(wrappedExecutor)
                .build()

        // Run two parallel transactions but verify that only 1 thread is busy when the transactions
        // execute, indicating that threads are not busy waiting on sql connections but are instead
        // suspended.
        runBlocking(Dispatchers.IO) {
            async {
                localDatabase.withTransaction {
                    delay(200) // delay a bit to let the other transaction proceed
                    assertThat(busyThreadsCount.get()).isEqualTo(1)
                }
            }

            async {
                localDatabase.withTransaction {
                    delay(200) // delay a bit to let the other transaction proceed
                    assertThat(busyThreadsCount.get()).isEqualTo(1)
                }
            }
        }

        assertThat(busyThreadsCount.get()).isEqualTo(0)
        wrappedExecutor.shutdown()
        assertThat(wrappedExecutor.awaitTermination(1, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun withTransaction_busyExecutor() {
        val executorService = Executors.newSingleThreadExecutor()
        val localDatabase =
            Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    TestDatabase::class.java
                )
                .setTransactionExecutor(executorService)
                .build()

        // Simulate a busy executor, no thread to acquire for transaction.
        val busyLatch = CountDownLatch(1)
        executorService.execute { busyLatch.await() }
        runBlocking {
            var asyncExecuted = false
            val job =
                async(Dispatchers.IO) {
                    asyncExecuted = true
                    localDatabase.withTransaction {
                        booksDao.insertPublisherSuspend(
                            TestUtil.PUBLISHER.publisherId,
                            TestUtil.PUBLISHER.name
                        )
                    }
                }

            try {
                withTimeout(1000) { job.join() }
                fail("A timeout should have occurred!")
            } catch (_: TimeoutCancellationException) {}
            job.cancelAndJoin()

            assertThat(asyncExecuted).isTrue()
        }
        // free busy thread
        busyLatch.countDown()
        executorService.shutdown()
        assertThat(executorService.awaitTermination(1, TimeUnit.SECONDS)).isTrue()

        assertThat(booksDao.getPublishers()).isEmpty()
    }

    @Test
    fun withTransaction_shutdownExecutor() {
        val executorService = Executors.newCachedThreadPool()
        val localDatabase =
            Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    TestDatabase::class.java
                )
                .setTransactionExecutor(executorService)
                .build()

        executorService.shutdownNow()

        runBlocking {
            try {
                localDatabase.withTransaction { fail("This coroutine should never run.") }
                fail("An exception should have been thrown by withTransaction")
            } catch (ex: IllegalStateException) {
                assertThat(ex)
                    .hasMessageThat()
                    .contains("Unable to acquire a thread to perform the database transaction")
            }
        }

        executorService.shutdown()
        assertThat(executorService.awaitTermination(1, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun withTransaction_databaseOpenError() {
        val localDatabase =
            Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    TestDatabase::class.java
                )
                .addCallback(
                    object : RoomDatabase.Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            // this causes all transaction methods to throw, this can happen IRL
                            throw RuntimeException("Error opening Database.")
                        }
                    }
                )
                .build()
        runBlocking {
            try {
                localDatabase.withTransaction { fail("This coroutine should never run.") }
            } catch (ex: RuntimeException) {
                assertThat(ex).hasMessageThat().contains("Error opening Database.")
            }
        }
    }

    @Test
    fun withTransaction_beginTransaction_error() {
        // delegate and delegate just so that we can throw in beginTransaction()
        val localDatabase =
            Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    TestDatabase::class.java
                )
                .openHelperFactory(
                    object : SupportSQLiteOpenHelper.Factory {
                        val factoryDelegate = FrameworkSQLiteOpenHelperFactory()

                        override fun create(
                            configuration: SupportSQLiteOpenHelper.Configuration
                        ): SupportSQLiteOpenHelper {
                            val helperDelegate = factoryDelegate.create(configuration)
                            return object : SupportSQLiteOpenHelper by helperDelegate {
                                override val writableDatabase: SupportSQLiteDatabase
                                    get() {
                                        val databaseDelegate = helperDelegate.writableDatabase
                                        return object : SupportSQLiteDatabase by databaseDelegate {
                                            override fun beginTransaction() {
                                                throw RuntimeException(
                                                    "Error beginning transaction."
                                                )
                                            }

                                            override fun beginTransactionNonExclusive() {
                                                throw RuntimeException(
                                                    "Error beginning transaction."
                                                )
                                            }
                                        }
                                    }
                            }
                        }
                    }
                )
                .build()
        runBlocking {
            try {
                localDatabase.withTransaction { fail("This coroutine should never run.") }
            } catch (ex: RuntimeException) {
                assertThat(ex).hasMessageThat().contains("Error beginning transaction")
            }
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun withTransaction_setTransactionSuccessful_error() {
        runBlocking {
            try {
                database.withTransaction {
                    // ending transaction prematurely so that setTransactionSuccessful() invoked by
                    // withTransaction throws.
                    database.endTransaction()
                }
            } catch (ex: IllegalStateException) {
                assertThat(ex)
                    .hasMessageThat()
                    .contains(
                        "Cannot perform this operation because there is no current " + "transaction"
                    )
            }
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun withTransaction_endTransaction_error() {
        runBlocking {
            try {
                database.withTransaction {
                    // ending transaction prematurely and quickly throwing so that endTransaction()
                    // invoked by withTransaction throws.
                    database.endTransaction()
                    // this exception will get swallowed by the exception thrown in endTransaction()
                    throw RuntimeException()
                }
            } catch (ex: IllegalStateException) {
                assertThat(ex)
                    .hasMessageThat()
                    .contains(
                        "Cannot perform this operation because there is no current " + "transaction"
                    )
            }
        }
    }

    @Test
    fun transactionFunctionWithSuspendFunctionalParamCommits() = runBlocking {
        // GIVEN a database with a book
        val bookPublisher = TestUtil.PUBLISHER
        val addedBook = TestUtil.BOOK_1.copy(bookPublisherId = bookPublisher.publisherId)
        booksDao.addPublishers(bookPublisher)
        booksDao.addBooks(addedBook)

        // WHEN a transaction is run
        val output =
            kotlin.runCatching {
                booksDao.functionWithSuspendFunctionalParam(addedBook) { book ->
                    booksDao.deleteBookSuspend(book)
                    return@functionWithSuspendFunctionalParam book
                }
            }

        // THEN the change has been committed
        assertWithMessage("The higher-order fun ran successfully")
            .that(output.isSuccess)
            .isEqualTo(true)
        assertThat(booksDao.getBooksSuspend()).doesNotContain(addedBook)
    }

    @Test
    fun transactionFunctionWithSuspendFunctionalParamDoesntCommitWhenError() = runBlocking {
        // GIVEN a database with a book
        val bookPublisher = TestUtil.PUBLISHER
        val addedBook = TestUtil.BOOK_1.copy(bookPublisherId = bookPublisher.publisherId)
        booksDao.addPublishers(bookPublisher)
        booksDao.addBooks(addedBook)

        // WHEN a transaction is started and then fails before completing
        val output =
            kotlin.runCatching {
                booksDao.functionWithSuspendFunctionalParam(addedBook) { book ->
                    booksDao.deleteBookSuspend(book)
                    error("Fake error in transaction")
                }
            }

        // THEN the change hasn't been committed
        assertWithMessage("RunCatching caught the thrown error")
            .that(output.isFailure)
            .isEqualTo(true)
        assertThat(booksDao.getBooksSuspend()).contains(addedBook)
    }

    @Test
    fun suspendTransactionFunctionWithSuspendFunctionalParamCommits() = runBlocking {
        // GIVEN a database with a book
        val bookPublisher = TestUtil.PUBLISHER
        val addedBook = TestUtil.BOOK_1.copy(bookPublisherId = bookPublisher.publisherId)
        booksDao.addPublishers(bookPublisher)
        booksDao.addBooks(addedBook)

        // WHEN a transaction is run
        val output =
            kotlin.runCatching {
                booksDao.functionWithSuspendFunctionalParam(addedBook) { book ->
                    booksDao.deleteBookSuspend(book)
                    return@functionWithSuspendFunctionalParam book
                }
            }

        // THEN the change has been committed
        assertWithMessage("The higher-order fun ran successfully")
            .that(output.isSuccess)
            .isEqualTo(true)
        assertThat(booksDao.getBooksSuspend()).doesNotContain(addedBook)
    }

    @Test
    fun suspendTransactionFunctionWithSuspendFunctionalParamDoesntCommitWhenError() = runBlocking {
        // GIVEN a database with a book
        val bookPublisher = TestUtil.PUBLISHER
        val addedBook = TestUtil.BOOK_1.copy(bookPublisherId = bookPublisher.publisherId)
        booksDao.addPublishers(bookPublisher)
        booksDao.addBooks(addedBook)

        // WHEN a transaction is started and then fails before completing
        val output = runCatching {
            booksDao.suspendFunctionWithSuspendFunctionalParam(addedBook) { book ->
                booksDao.deleteBookSuspend(book)
                error("Fake error in transaction")
            }
        }

        // THEN the change hasn't been committed
        assertWithMessage("RunCatching caught the thrown error")
            .that(output.isFailure)
            .isEqualTo(true)
        assertThat(booksDao.getBooksSuspend()).contains(addedBook)
    }

    @Test
    fun withTransaction_instantTaskExecutorRule() = runBlocking {
        // Not the actual InstantTaskExecutorRule since this test class already uses
        // CountingTaskExecutorRule but same behaviour.
        ArchTaskExecutor.getInstance()
            .setDelegate(
                object : TaskExecutor() {
                    override fun executeOnDiskIO(runnable: Runnable) {
                        runnable.run()
                    }

                    override fun postToMainThread(runnable: Runnable) {
                        runnable.run()
                    }

                    override fun isMainThread(): Boolean {
                        return false
                    }
                }
            )
        database.withTransaction {
            booksDao.insertPublisherSuspend(TestUtil.PUBLISHER.publisherId, TestUtil.PUBLISHER.name)
        }
        assertThat(booksDao.getPublishers().size).isEqualTo(1)
    }

    @Test
    fun withTransaction_singleExecutorDispatcher() {
        val executor = Executors.newSingleThreadExecutor()
        val localDatabase =
            Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    TestDatabase::class.java
                )
                .setTransactionExecutor(executor)
                .build()
        runBlocking {
            withContext(executor.asCoroutineDispatcher()) {
                localDatabase.withTransaction {
                    localDatabase
                        .booksDao()
                        .insertPublisherSuspend(
                            TestUtil.PUBLISHER.publisherId,
                            TestUtil.PUBLISHER.name
                        )
                }
            }
        }
        assertThat(localDatabase.booksDao().getPublishers().size).isEqualTo(1)

        executor.shutdown()
        assertThat(executor.awaitTermination(1, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun withTransaction_reentrant_nested() {
        val executor = Executors.newSingleThreadExecutor()
        val localDatabase =
            Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    TestDatabase::class.java
                )
                .setTransactionExecutor(executor)
                .build()
        runBlocking {
            withContext(executor.asCoroutineDispatcher()) {
                localDatabase.withTransaction {
                    localDatabase
                        .booksDao()
                        .insertPublisherSuspend(
                            TestUtil.PUBLISHER.publisherId,
                            TestUtil.PUBLISHER.name
                        )
                    localDatabase.withTransaction {
                        localDatabase.booksDao().insertBookSuspend(TestUtil.BOOK_1)
                    }
                }
            }
        }
        assertThat(localDatabase.booksDao().getPublishers().size).isEqualTo(1)
        assertThat(localDatabase.booksDao().getAllBooks().size).isEqualTo(1)

        executor.shutdown()
        assertThat(executor.awaitTermination(1, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun withTransaction_reentrant_nested_exception() {
        val executor = Executors.newSingleThreadExecutor()
        val localDatabase =
            Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    TestDatabase::class.java
                )
                .setTransactionExecutor(executor)
                .build()
        runBlocking {
            withContext(executor.asCoroutineDispatcher()) {
                localDatabase.withTransaction {
                    localDatabase
                        .booksDao()
                        .insertPublisherSuspend(
                            TestUtil.PUBLISHER.publisherId,
                            TestUtil.PUBLISHER.name
                        )
                    try {
                        localDatabase.withTransaction {
                            localDatabase.booksDao().insertBookSuspend(TestUtil.BOOK_1)
                            throw IOException("Boom!")
                        }
                        @Suppress("UNREACHABLE_CODE") fail("An exception should have been thrown.")
                    } catch (ex: IOException) {
                        assertThat(ex).hasMessageThat().contains("Boom")
                    }
                }
            }
        }
        assertThat(localDatabase.booksDao().getPublishers()).isEmpty()
        assertThat(localDatabase.booksDao().getAllBooks()).isEmpty()

        executor.shutdown()
        assertThat(executor.awaitTermination(1, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun withTransaction_reentrant_nested_contextSwitch() {
        val executor = Executors.newSingleThreadExecutor()
        val localDatabase =
            Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    TestDatabase::class.java
                )
                .setTransactionExecutor(executor)
                .build()

        runBlocking {
            withContext(executor.asCoroutineDispatcher()) {
                localDatabase.withTransaction {
                    localDatabase
                        .booksDao()
                        .insertPublisherSuspend(
                            TestUtil.PUBLISHER.publisherId,
                            TestUtil.PUBLISHER.name
                        )
                    withContext(Dispatchers.IO) {
                        localDatabase.withTransaction {
                            localDatabase.booksDao().insertBookSuspend(TestUtil.BOOK_1)
                        }
                    }
                }
            }
        }
        assertThat(localDatabase.booksDao().getPublishers().size).isEqualTo(1)
        assertThat(localDatabase.booksDao().getAllBooks().size).isEqualTo(1)

        executor.shutdown()
        assertThat(executor.awaitTermination(1, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun withTransaction_reentrant_busyExecutor() {
        val busyThreadsCount = AtomicInteger()
        val executor = BusyCountingService(busyThreadsCount, Executors.newFixedThreadPool(2))
        val localDatabase =
            Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    TestDatabase::class.java
                )
                .setTransactionExecutor(executor)
                .build()

        // Grab one of the thread and simulate busy work
        val busyLatch = CountDownLatch(1)
        executor.execute { busyLatch.await() }

        runBlocking {
            // Using the other thread in the pool this will cause a reentrant situation
            withContext(executor.asCoroutineDispatcher()) {
                localDatabase.withTransaction {
                    val transactionThread = Thread.currentThread()
                    // Suspend transaction thread while freeing the busy thread from the pool
                    withContext(Dispatchers.IO) {
                        busyLatch.countDown()
                        delay(200)
                        // Only one thread is busy, the transaction thread
                        assertThat(busyThreadsCount.get()).isEqualTo(1)
                    }
                    // Resume in the transaction thread, the recently free thread in the pool that
                    // is not in a transaction should not be used.
                    assertThat(Thread.currentThread()).isEqualTo(transactionThread)
                    localDatabase
                        .booksDao()
                        .insertPublisherSuspend(
                            TestUtil.PUBLISHER.publisherId,
                            TestUtil.PUBLISHER.name
                        )
                }
            }
        }

        assertThat(localDatabase.booksDao().getPublishers().size).isEqualTo(1)

        executor.shutdown()
        assertThat(executor.awaitTermination(1, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun withTransaction_runTest() {
        runTest {
            database.withTransaction {
                booksDao.insertPublisherSuspend(
                    TestUtil.PUBLISHER.publisherId,
                    TestUtil.PUBLISHER.name
                )
                booksDao.insertBookSuspend(TestUtil.BOOK_1.copy(salesCnt = 0))
                booksDao.insertBookSuspend(TestUtil.BOOK_2)
                booksDao.deleteUnsoldBooks()
            }
            assertThat(booksDao.getBooksSuspend()).isEqualTo(listOf(TestUtil.BOOK_2))
        }
    }

    @Test
    fun withTransaction_stress_testMutation() {
        val output = mutableListOf<String>()
        runBlocking {
            repeat(5000) { count ->
                database.withTransaction {
                    output.add("$count")
                    suspendHere()
                    output.add("$count")
                }
            }
        }

        val expectedOutput = buildList {
            repeat(5000) { count ->
                add("$count")
                add("$count")
            }
        }
        assertThat(output).isEqualTo(expectedOutput)
    }

    @Test
    fun withTransaction_stress_dbMutation() {
        val context: Context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("test_stress_dbMutation.db")
        val db = Room.databaseBuilder(context, TestDatabase::class.java, "test.db").build()
        runBlocking {
            db.counterDao().upsert(Counter(1, 0))
            repeat(5000) {
                launch(Dispatchers.IO) {
                    db.withTransaction {
                        val current = db.counterDao().getCounter(1)
                        suspendHere()
                        db.counterDao().upsert(current.copy(value = current.value + 1))
                    }
                }
            }
        }
        runBlocking {
            val count = db.counterDao().getCounter(1)
            assertThat(count.value).isEqualTo(5000)
        }
        db.close()
    }

    // Utility function to _really_ suspend.
    private suspend fun suspendHere(): Unit = suspendCoroutineUninterceptedOrReturn {
        it.intercepted().resume(Unit)
        COROUTINE_SUSPENDED
    }

    // Executor wrapper that counts threads that are busy executing commands.
    class BusyCountingService(val count: AtomicInteger, val delegate: ExecutorService) :
        ExecutorService by delegate {
        override fun execute(command: Runnable) {
            delegate.execute {
                count.incrementAndGet()
                try {
                    command.run()
                } finally {
                    count.decrementAndGet()
                }
            }
        }
    }
}
