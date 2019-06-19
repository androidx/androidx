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

import android.os.Build
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.integration.kotlintestapp.NewThreadDispatcher
import androidx.room.integration.kotlintestapp.TestDatabase
import androidx.room.integration.kotlintestapp.vo.Book
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

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

            assertThat(booksDao.getBookSuspend(TestUtil.BOOK_1.bookId))
                .isEqualTo(TestUtil.BOOK_1)
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
        runBlocking {
            assertThat(booksDao.getBooksSuspend()).isEqualTo(listOf(TestUtil.BOOK_2))
        }
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
        runBlocking {
            assertThat(booksDao.getBooksSuspend()).isEqualTo(listOf(TestUtil.BOOK_2))
        }
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
        runBlocking(NewThreadDispatcher()) {
            assertThat(booksDao.getBooksSuspend()).isEqualTo(listOf(TestUtil.BOOK_2))
        }
    }

    @Test
    fun suspendingBlock_blockingDaoMethods() {
        runBlocking {
            booksDao.insertPublisherSuspend(
                TestUtil.PUBLISHER.publisherId,
                TestUtil.PUBLISHER.name
            )

            booksDao.addBooks(TestUtil.BOOK_1)

            assertThat(booksDao.getBooksSuspend())
                .isEqualTo(listOf(TestUtil.BOOK_1))
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
            assertThat(booksDao.getBooksSuspend())
                .isEqualTo(listOf(TestUtil.BOOK_2))
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
        runBlocking(NewThreadDispatcher()) {
            assertThat(booksDao.getBooksSuspend())
                .isEqualTo(listOf(TestUtil.BOOK_2))
        }
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
        runBlocking(NewThreadDispatcher()) {
            assertThat(booksDao.getBooksSuspend())
                .isEqualTo(listOf(TestUtil.BOOK_2))
        }
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
            assertThat(booksDao.getBooksSuspend())
                .isEqualTo(listOf(TestUtil.BOOK_2))
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
                @Suppress("IMPLICIT_NOTHING_AS_TYPE_PARAMETER")
                database.withTransaction {
                    booksDao.insertBookSuspend(TestUtil.BOOK_2)
                    throw IOException("Boom!")
                }
                @Suppress("UNREACHABLE_CODE")
                fail("An exception should have been thrown.")
            } catch (ex: IOException) {
                assertThat(ex).hasMessageThat()
                    .contains("Boom")
            }

            assertThat(booksDao.getBooksSuspend())
                .isEqualTo(listOf(TestUtil.BOOK_1))
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
            assertThat(booksDao.getBooksSuspend())
                .isEqualTo(listOf(TestUtil.BOOK_2))
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
                    @Suppress("IMPLICIT_NOTHING_AS_TYPE_PARAMETER")
                    database.withTransaction {
                        booksDao.insertBookSuspend(TestUtil.BOOK_1.copy(salesCnt = 0))
                        throw IOException("Boom!")
                    }
                    @Suppress("UNREACHABLE_CODE")
                    fail("An exception should have been thrown.")
                } catch (ex: IOException) {
                    assertThat(ex).hasMessageThat()
                        .contains("Boom")
                }
            }

            assertThat(booksDao.getBooksSuspend())
                .isEqualTo(emptyList<Book>())
        }
    }

    @Test
    fun withTransaction_nested_finally() {
        runBlocking {
            var finallyBlocksExecuted = 0
            try {
                database.withTransaction {
                    try {
                        @Suppress("IMPLICIT_NOTHING_AS_TYPE_PARAMETER")
                        database.withTransaction {
                            throw IOException("Boom!")
                        }
                        @Suppress("UNREACHABLE_CODE")
                        fail("An exception should have been thrown.")
                    } catch (ex: IOException) {
                        assertThat(ex).hasMessageThat()
                            .contains("Boom")
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
            assertThat(booksDao.getBooksSuspend())
                .isEqualTo(listOf(TestUtil.BOOK_2))
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
            booksDao.insertPublisherSuspend(
                TestUtil.PUBLISHER.publisherId,
                TestUtil.PUBLISHER.name
            )
            booksDao.insertBookSuspend(TestUtil.BOOK_1)

            val startedRunning = CountDownLatch(1)
            var insertAttempted = false
            val job = launch(Dispatchers.IO) {
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
            assertThat(booksDao.getBooksSuspend())
                .isEqualTo(listOf(TestUtil.BOOK_2))
        }
    }

    @Test
    fun withTransaction_blockingDaoMethods_contextSwitch() {
        runBlocking {
            database.withTransaction {
                // normal query
                try {
                    withContext(Dispatchers.IO) {
                        booksDao.getBook("b1")
                    }
                    fail("An exception should have been thrown")
                } catch (ex: IllegalStateException) {
                    assertThat(ex).hasMessageThat()
                        .contains("Cannot access database on a different coroutine context")
                }

                // delete or update shortcut
                try {
                    withContext(Dispatchers.IO) {
                        booksDao.deleteUnsoldBooks()
                    }
                    fail("An exception should have been thrown")
                } catch (ex: IllegalStateException) {
                    assertThat(ex).hasMessageThat()
                        .contains("Cannot access database on a different coroutine context")
                }

                // insert shortcut
                try {
                    withContext(Dispatchers.IO) {
                        booksDao.insertPublisherVoid("p1", "publisher1")
                    }
                    fail("An exception should have been thrown")
                } catch (ex: IllegalStateException) {
                    assertThat(ex).hasMessageThat()
                        .contains("Cannot access database on a different coroutine context")
                }

                // shared prepared query
                try {
                    withContext(Dispatchers.IO) {
                        booksDao.addPublishers(TestUtil.PUBLISHER)
                    }
                    fail("An exception should have been thrown")
                } catch (ex: IllegalStateException) {
                    assertThat(ex).hasMessageThat()
                        .contains("Cannot access database on a different coroutine context")
                }

                // prepared query
                try {
                    withContext(Dispatchers.IO) {
                        booksDao.deleteBookWithIds("b1", "b2")
                    }
                    fail("An exception should have been thrown")
                } catch (ex: IllegalStateException) {
                    assertThat(ex).hasMessageThat()
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
                    async {
                        booksDao.insertBookSuspend(TestUtil.BOOK_1)
                    }
                    async(Dispatchers.Default) {
                        booksDao.insertBookSuspend(TestUtil.BOOK_2)
                    }
                    async(Dispatchers.IO) {
                        booksDao.insertBookSuspend(TestUtil.BOOK_3)
                    }
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
                    async {
                        booksDao.insertBookSuspend(TestUtil.BOOK_1)
                    }
                    async {
                        booksDao.insertBookSuspend(TestUtil.BOOK_2)
                    }
                    async {
                        booksDao.insertBookSuspend(TestUtil.BOOK_3)
                    }
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
            booksDao.insertPublisherSuspend(
                TestUtil.PUBLISHER.publisherId,
                TestUtil.PUBLISHER.name
            )

            async {
                database.withTransaction {
                    booksDao.insertBookSuspend(TestUtil.BOOK_1)
                }
            }

            async {
                database.withTransaction {
                    booksDao.insertBookSuspend(TestUtil.BOOK_2)
                }
            }
        }

        runBlocking {
            // as Set since insertion order is undefined
            assertThat(booksDao.getBooksSuspend().toSet())
                .isEqualTo(setOf(TestUtil.BOOK_1, TestUtil.BOOK_2))
        }
    }

    @Test
    @ObsoleteCoroutinesApi
    @Suppress("DeferredResultUnused")
    fun withTransaction_multipleTransactions_multipleThreads() {
        runBlocking {
            booksDao.insertPublisherSuspend(
                TestUtil.PUBLISHER.publisherId,
                TestUtil.PUBLISHER.name
            )

            async(newSingleThreadContext("asyncThread1")) {
                database.withTransaction {
                    delay(100)
                    booksDao.insertBookSuspend(TestUtil.BOOK_1)
                }
            }

            async(newSingleThreadContext("asyncThread2")) {
                database.withTransaction {
                    delay(100)
                    booksDao.insertBookSuspend(TestUtil.BOOK_2)
                }
            }
        }

        runBlocking {
            // as Set since insertion order is undefined
            assertThat(booksDao.getBooksSuspend().toSet())
                .isEqualTo(setOf(TestUtil.BOOK_1, TestUtil.BOOK_2))
        }
    }

    @Test
    @Suppress("DeferredResultUnused")
    fun withTransaction_multipleTransactions_verifyThreadUsage() {
        val busyThreadsCount = AtomicInteger()
        // Executor wrapper that counts threads that are busy executing commands.
        class WrappedService(val delegate: ExecutorService) : ExecutorService by delegate {
            override fun execute(command: Runnable) {
                delegate.execute {
                    busyThreadsCount.incrementAndGet()
                    try {
                        command.run()
                    } finally {
                        busyThreadsCount.decrementAndGet()
                    }
                }
            }
        }
        val wrappedExecutor = WrappedService(Executors.newCachedThreadPool())
        val localDatabase = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), TestDatabase::class.java)
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

        wrappedExecutor.awaitTermination(1, TimeUnit.SECONDS)
    }

    @Test
    fun withTransaction_busyExecutor() {
        runBlocking {
            val executorService = Executors.newSingleThreadExecutor()
            val localDatabase = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(), TestDatabase::class.java)
                .setTransactionExecutor(executorService)
                .build()

            // Simulate a busy executor, no thread to acquire for transaction.
            val busyLatch = CountDownLatch(1)
            executorService.execute {
                busyLatch.await()
            }

            var asyncExecuted = false
            val transactionLatch = CountDownLatch(1)
            val job = async(Dispatchers.IO) {
                asyncExecuted = true
                localDatabase.withTransaction {
                    transactionLatch.countDown()
                }
            }

            assertThat(transactionLatch.await(1000, TimeUnit.MILLISECONDS)).isFalse()
            job.cancelAndJoin()

            assertThat(asyncExecuted).isTrue()

            // free busy thread
            busyLatch.countDown()
            executorService.awaitTermination(1, TimeUnit.SECONDS)
        }
    }

    @Test
    fun withTransaction_shutdownExecutor() {
        runBlocking {
            val executorService = Executors.newCachedThreadPool()
            val localDatabase = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(), TestDatabase::class.java)
                .setTransactionExecutor(executorService)
                .build()

            executorService.shutdownNow()

            try {
                localDatabase.withTransaction {
                    fail("This coroutine should never run.")
                }
                fail("An exception should have been thrown by withTransaction")
            } catch (ex: IllegalStateException) {
                assertThat(ex).hasMessageThat()
                    .contains("Unable to acquire a thread to perform the database transaction")
            }
        }
    }

    @Test
    fun withTransaction_databaseOpenError() {
        runBlocking {
            val localDatabase = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(), TestDatabase::class.java)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        // this causes all transaction methods to throw, this can happen IRL
                        throw RuntimeException("Error opening Database.")
                    }
                })
                .build()

            try {
                localDatabase.withTransaction {
                    fail("This coroutine should never run.")
                }
            } catch (ex: RuntimeException) {
                assertThat(ex).hasMessageThat()
                    .contains("Error opening Database.")
            }
        }
    }

    @Test
    fun withTransaction_beginTransaction_error() {
        runBlocking {
            // delegate and delegate just so that we can throw in beginTransaction()
            val localDatabase = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(), TestDatabase::class.java)
                .openHelperFactory(
                    object : SupportSQLiteOpenHelper.Factory {
                        val factoryDelegate = FrameworkSQLiteOpenHelperFactory()
                        override fun create(
                            configuration: SupportSQLiteOpenHelper.Configuration?
                        ): SupportSQLiteOpenHelper {
                            val helperDelegate = factoryDelegate.create(configuration)
                            return object : SupportSQLiteOpenHelper by helperDelegate {
                                override fun getWritableDatabase(): SupportSQLiteDatabase {
                                    val databaseDelegate = helperDelegate.writableDatabase
                                    return object : SupportSQLiteDatabase by databaseDelegate {
                                        override fun beginTransaction() {
                                            throw RuntimeException("Error beginning transaction.")
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
                .build()

            try {
                localDatabase.withTransaction {
                    fail("This coroutine should never run.")
                }
            } catch (ex: RuntimeException) {
                assertThat(ex).hasMessageThat()
                    .contains("Error beginning transaction")
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
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    assertThat(ex).hasMessageThat()
                        .contains(
                            "Cannot perform this operation because there is no current " +
                                    "transaction"
                        )
                } else {
                    assertThat(ex).hasMessageThat()
                        .contains("Don't have database lock")
                }
            }
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun withTransaction_endTransaction_error() {
        @Suppress("IMPLICIT_NOTHING_AS_TYPE_PARAMETER")
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
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    assertThat(ex).hasMessageThat()
                        .contains(
                            "Cannot perform this operation because there is no current " +
                                    "transaction"
                        )
                } else {
                    assertThat(ex).hasMessageThat()
                        .contains("Don't have database lock")
                }
            }
        }
    }
}