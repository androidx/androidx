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

package androidx.room3.integration.kotlintestapp.test

import android.content.Context
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import androidx.kruth.assertWithMessage
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.integration.kotlintestapp.NewThreadDispatcher
import androidx.room3.integration.kotlintestapp.TestDatabase
import androidx.room3.integration.kotlintestapp.vo.Counter
import androidx.room3.support.getSupportWrapper
import androidx.room3.withWriteTransaction
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.intercepted
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.resume
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@LargeTest
@RunWith(Parameterized::class)
class SuspendingQueryTest(driver: UseDriver) : TestDatabaseTest(driver) {

    private companion object {
        @JvmStatic
        @Parameters(name = "useDriver={0}")
        fun parameters() = arrayOf(UseDriver.ANDROID, UseDriver.BUNDLED)
    }

    @After
    fun teardown() {
        // At the end of all tests, query executor should be idle (transaction thread released).
        countingTaskExecutorRule.drainTasks(500, TimeUnit.MILLISECONDS)
        assertThat(countingTaskExecutorRule.isIdle).isTrue()
    }

    @Test
    fun bookByIdSuspend() = runTest {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)

        assertThat(booksDao.getBookSuspend(TestUtil.BOOK_1.bookId)).isEqualTo(TestUtil.BOOK_1)
    }

    @Test
    fun upsertBookSuspend() = runTest {
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.upsertBookSuspend(TestUtil.BOOK_1)

        assertThat(booksDao.getBookSuspend(TestUtil.BOOK_1.bookId)).isEqualTo(TestUtil.BOOK_1)
    }

    @Test
    fun upsertSuspendLong() = runTest {
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.upsertBookSuspendReturnId(TestUtil.BOOK_1).let { result ->
            assertThat(booksDao.getBookSuspend(TestUtil.BOOK_1.bookId)).isEqualTo(TestUtil.BOOK_1)
            assertThat(result).isEqualTo(1)
        }
        booksDao.upsertBookSuspendReturnId(TestUtil.BOOK_1.copy(title = "changed title")).let {
            result ->
            assertThat(result).isEqualTo(-1)
        }
    }

    @Test
    fun upsertSuspendLongList() = runTest {
        booksDao.addPublishers(TestUtil.PUBLISHER)
        val bookList = buildList {
            add(TestUtil.BOOK_1)
            add(TestUtil.BOOK_2)
            add(TestUtil.BOOK_3)
        }
        booksDao.upsertBooksSuspendReturnIds(bookList).let { results ->
            assertThat(results.size).isEqualTo(3)
            assertThat(results).containsExactly(1L, 2L, 3L)
        }
    }

    @Test
    fun allBookSuspend() = runTest {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1, TestUtil.BOOK_2)

        val books = booksDao.getBooksSuspend()

        assertThat(books.size).isEqualTo((2))
        assertThat(books[0]).isEqualTo(TestUtil.BOOK_1)
        assertThat(books[1]).isEqualTo(TestUtil.BOOK_2)
    }

    @Test
    fun allBookSuspend_notOpened() {
        val context: Context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("closed.db")
        val db =
            Room.databaseBuilder<TestDatabase>(context = context, name = "closed.db")
                .setDriver(
                    when (useDriver) {
                        UseDriver.ANDROID -> AndroidSQLiteDriver()
                        UseDriver.BUNDLED -> BundledSQLiteDriver()
                    }
                )
                .build()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val currentPolicy = StrictMode.getThreadPolicy()
            StrictMode.setThreadPolicy(
                ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().penaltyDeath().build()
            )
            try {
                runBlocking {
                    // Opens DB, isOpen && inTransaction check should not cause violation
                    db.booksDao().getBooksSuspend()
                    // DB is open, isOpen && inTransaction check should not cause violation
                    db.booksDao().getBooksSuspend()
                }
            } finally {
                StrictMode.setThreadPolicy(currentPolicy)
            }
        }
        db.close()
    }

    @Test
    fun suspendingBlock_blockingDaoFunctions() = runTest {
        booksDao.insertPublisherSuspend(TestUtil.PUBLISHER.publisherId, TestUtil.PUBLISHER.name)

        booksDao.addBooks(TestUtil.BOOK_1)

        assertThat(booksDao.getBooksSuspend()).isEqualTo(listOf(TestUtil.BOOK_1))
    }

    @Test
    fun runSuspendingTransaction() = runTest {
        database.withWriteTransaction {
            booksDao.insertPublisherSuspend(TestUtil.PUBLISHER.publisherId, TestUtil.PUBLISHER.name)
            booksDao.insertBookSuspend(TestUtil.BOOK_1.copy(salesCnt = 0))
            booksDao.insertBookSuspend(TestUtil.BOOK_2)
            booksDao.deleteUnsoldBooks()
        }
        assertThat(booksDao.getBooksSuspend()).isEqualTo(listOf(TestUtil.BOOK_2))
    }

    @Test
    fun withWriteTransaction_newThreadDispatcher() {
        runBlocking(NewThreadDispatcher()) {
            database.withWriteTransaction {
                booksDao.insertPublisherSuspend(
                    TestUtil.PUBLISHER.publisherId,
                    TestUtil.PUBLISHER.name,
                )
                booksDao.insertBookSuspend(TestUtil.BOOK_1.copy(salesCnt = 0))
                booksDao.insertBookSuspend(TestUtil.BOOK_2)
                booksDao.deleteUnsoldBooks()
            }
        }
        assertThat(booksDao.getAllBooks()).isEqualTo(listOf(TestUtil.BOOK_2))
    }

    @Test
    fun withWriteTransaction_withContext_newThreadDispatcher() {
        runBlocking {
            withContext(NewThreadDispatcher()) {
                database.withWriteTransaction {
                    booksDao.insertPublisherSuspend(
                        TestUtil.PUBLISHER.publisherId,
                        TestUtil.PUBLISHER.name,
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
    fun withWriteTransaction_ioDispatcher() {
        runBlocking(Dispatchers.IO) {
            database.withWriteTransaction {
                booksDao.insertPublisherSuspend(
                    TestUtil.PUBLISHER.publisherId,
                    TestUtil.PUBLISHER.name,
                )
                booksDao.insertBookSuspend(TestUtil.BOOK_1.copy(salesCnt = 0))
                booksDao.insertBookSuspend(TestUtil.BOOK_2)
                booksDao.deleteUnsoldBooks()
            }
        }
        assertThat(booksDao.getAllBooks()).isEqualTo(listOf(TestUtil.BOOK_2))
    }

    @Test
    fun withWriteTransaction_contextSwitch() = runTest {
        database.withWriteTransaction {
            booksDao.insertPublisherSuspend(TestUtil.PUBLISHER.publisherId, TestUtil.PUBLISHER.name)
            withContext(Dispatchers.IO) {
                booksDao.insertBookSuspend(TestUtil.BOOK_1.copy(salesCnt = 0))
                booksDao.insertBookSuspend(TestUtil.BOOK_2)
            }
            booksDao.deleteUnsoldBooks()
        }
        assertThat(booksDao.getBooksSuspend()).isEqualTo(listOf(TestUtil.BOOK_2))
    }

    @Test
    fun withWriteTransaction_contextSwitch_exception() = runTest {
        assertThrows<IOException> {
                database.withWriteTransaction {
                    booksDao.insertPublisherSuspend(
                        TestUtil.PUBLISHER.publisherId,
                        TestUtil.PUBLISHER.name,
                    )
                    withContext(Dispatchers.IO) {
                        booksDao.insertBookSuspend(TestUtil.BOOK_1.copy(salesCnt = 0))
                        booksDao.insertBookSuspend(TestUtil.BOOK_2)
                    }
                    booksDao.deleteUnsoldBooks()
                    throw IOException("Boom!")
                }
            }
            .hasMessageThat()
            .isEqualTo("Boom!")

        assertThat(booksDao.getPublishersSuspend()).isEmpty()
        assertThat(booksDao.getBooksSuspend()).isEmpty()
    }

    @Test
    fun withWriteTransaction_exception() = runTest {
        database.withWriteTransaction {
            booksDao.insertPublisherSuspend(TestUtil.PUBLISHER.publisherId, TestUtil.PUBLISHER.name)
            booksDao.insertBookSuspend(TestUtil.BOOK_1)
        }

        assertThrows<IOException> {
                database.withWriteTransaction {
                    booksDao.insertBookSuspend(TestUtil.BOOK_2)
                    booksDao.insertBookSuspend(TestUtil.BOOK_3)
                    throw IOException("Boom!")
                }
            }
            .hasMessageThat()
            .isEqualTo("Boom!")

        assertThat(booksDao.getBooksSuspend()).isEqualTo(listOf(TestUtil.BOOK_1))
    }

    @Test
    fun withWriteTransaction_nested() = runTest {
        database.withWriteTransaction {
            booksDao.insertPublisherSuspend(TestUtil.PUBLISHER.publisherId, TestUtil.PUBLISHER.name)
            database.withWriteTransaction {
                booksDao.insertBookSuspend(TestUtil.BOOK_1.copy(salesCnt = 0))
                booksDao.insertBookSuspend(TestUtil.BOOK_2)
            }
            booksDao.deleteUnsoldBooks()
        }
        assertThat(booksDao.getBooksSuspend()).isEqualTo(listOf(TestUtil.BOOK_2))
    }

    @Test
    fun withWriteTransaction_nested_daoTransaction() = runTest {
        database.withWriteTransaction {
            booksDao.insertPublisherSuspend(TestUtil.PUBLISHER.publisherId, TestUtil.PUBLISHER.name)
            database.withWriteTransaction {
                booksDao.insertBookSuspend(TestUtil.BOOK_1.copy(salesCnt = 0))
                booksDao.insertBookSuspend(TestUtil.BOOK_2)
            }
            booksDao.deleteBooksWithZeroSales()
        }
        assertThat(booksDao.getBooksSuspend()).isEqualTo(listOf(TestUtil.BOOK_2))
    }

    @Test
    fun withWriteTransaction_nested_exception() = runTest {
        database.withWriteTransaction {
            booksDao.insertPublisherSuspend(TestUtil.PUBLISHER.publisherId, TestUtil.PUBLISHER.name)

            assertThrows<IOException> {
                    database.withWriteTransaction {
                        booksDao.insertBookSuspend(TestUtil.BOOK_1.copy(salesCnt = 0))
                        throw IOException("Boom!")
                    }
                }
                .hasMessageThat()
                .isEqualTo("Boom!")
        }

        assertThat(booksDao.getBooksSuspend()).isEmpty()
    }

    @Test
    fun withWriteTransaction_nested_finally() = runTest {
        var finallyBlocksExecuted = 0
        try {
            database.withWriteTransaction {
                try {
                    database.withWriteTransaction { throw IOException("Boom!") }
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

    @Test
    fun withWriteTransaction_nested_contextSwitch() = runTest {
        database.withWriteTransaction {
            booksDao.insertPublisherSuspend(TestUtil.PUBLISHER.publisherId, TestUtil.PUBLISHER.name)
            withContext(Dispatchers.IO) {
                database.withWriteTransaction {
                    booksDao.insertBookSuspend(TestUtil.BOOK_1.copy(salesCnt = 0))
                    booksDao.insertBookSuspend(TestUtil.BOOK_2)
                }
            }
            booksDao.deleteUnsoldBooks()
        }
        assertThat(booksDao.getBooksSuspend()).isEqualTo(listOf(TestUtil.BOOK_2))
    }

    @Test
    fun withWriteTransaction_childCoroutine_defaultDispatcher() = runTest {
        database.withWriteTransaction {
            booksDao.insertPublisherSuspend(TestUtil.PUBLISHER.publisherId, TestUtil.PUBLISHER.name)
            coroutineScope {
                launch {
                    booksDao.insertBookSuspend(TestUtil.BOOK_1)
                    booksDao.insertBookSuspend(TestUtil.BOOK_2)
                }
            }
        }
        assertThat(booksDao.getBooksSuspend()).isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_2))
    }

    @Test
    fun withWriteTransaction_childCoroutine_ioDispatcher() = runTest {
        database.withWriteTransaction {
            booksDao.insertPublisherSuspend(TestUtil.PUBLISHER.publisherId, TestUtil.PUBLISHER.name)
            coroutineScope {
                launch(Dispatchers.IO) {
                    booksDao.insertBookSuspend(TestUtil.BOOK_1)
                    booksDao.insertBookSuspend(TestUtil.BOOK_2)
                }
            }
        }
        assertThat(booksDao.getBooksSuspend()).isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_2))
    }

    @Test
    fun withWriteTransaction_cancelCoroutine() = runTest {
        booksDao.insertPublisherSuspend(TestUtil.PUBLISHER.publisherId, TestUtil.PUBLISHER.name)
        booksDao.insertBookSuspend(TestUtil.BOOK_1)

        val startedRunning = CountDownLatch(1)
        var insertAttempted = false
        val job =
            launch(Dispatchers.IO) {
                database.withWriteTransaction {
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
        assertThat(booksDao.getBooksSuspend()).isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_3))
    }

    @Test
    fun withWriteTransaction_busyExecutor_cancelCoroutine() = runTest {
        val executorService = Executors.newSingleThreadExecutor()
        val localDatabase =
            Room.inMemoryDatabaseBuilder<TestDatabase>(ApplicationProvider.getApplicationContext())
                .setDriver(
                    when (useDriver) {
                        UseDriver.ANDROID -> AndroidSQLiteDriver()
                        UseDriver.BUNDLED -> BundledSQLiteDriver()
                    }
                )
                .setQueryCoroutineContext(executorService.asCoroutineDispatcher())
                .build()

        // Simulate a busy executor, no thread to acquire for transaction.
        val busyLatch = CountDownLatch(1)
        executorService.execute { busyLatch.await() }
        val startedRunning = CountDownLatch(1)
        val job =
            launch(Dispatchers.IO) {
                startedRunning.countDown()
                delay(200) // yield and delay to queue the runnable in transaction executor
                localDatabase.withWriteTransaction {
                    fail("Transaction block should have never run!")
                }
            }

        assertThat(startedRunning.await(1, TimeUnit.SECONDS)).isTrue()
        job.cancelAndJoin()

        // free busy thread
        busyLatch.countDown()
        executorService.shutdown()
        assertThat(executorService.awaitTermination(1, TimeUnit.SECONDS)).isTrue()

        assertThat(localDatabase.booksDao().getPublishers()).isEmpty()

        localDatabase.close()
    }

    @Test
    fun withWriteTransaction_blockingDaoFunctions() = runTest {
        database.withWriteTransaction {
            booksDao.insertPublisherSuspend(TestUtil.PUBLISHER.publisherId, TestUtil.PUBLISHER.name)
            booksDao.addBooks(TestUtil.BOOK_1.copy(salesCnt = 0))
            booksDao.addBooks(TestUtil.BOOK_2)

            booksDao.deleteUnsoldBooks()
        }
        assertThat(booksDao.getBooksSuspend()).isEqualTo(listOf(TestUtil.BOOK_2))
    }

    @Test
    fun withWriteTransaction_variousLaunch() = runTest {
        database.withWriteTransaction {
            booksDao.insertPublisherSuspend(TestUtil.PUBLISHER.publisherId, TestUtil.PUBLISHER.name)

            coroutineScope {
                launch { booksDao.insertBookSuspend(TestUtil.BOOK_1) }
                launch(Dispatchers.Default) { booksDao.insertBookSuspend(TestUtil.BOOK_2) }
                launch(Dispatchers.IO) { booksDao.insertBookSuspend(TestUtil.BOOK_3) }
            }
        }

        // as Set since insertion order is undefined
        assertThat(booksDao.getBooksSuspend().toSet())
            .isEqualTo(setOf(TestUtil.BOOK_1, TestUtil.BOOK_2, TestUtil.BOOK_3))
    }

    @Test
    fun withWriteTransaction_variousLaunch_ioDispatcher() = runTest {
        database.withWriteTransaction {
            booksDao.insertPublisherSuspend(TestUtil.PUBLISHER.publisherId, TestUtil.PUBLISHER.name)

            withContext(Dispatchers.IO) {
                launch { booksDao.insertBookSuspend(TestUtil.BOOK_1) }
                launch { booksDao.insertBookSuspend(TestUtil.BOOK_2) }
                launch { booksDao.insertBookSuspend(TestUtil.BOOK_3) }
            }
        }

        // as Set since insertion order is undefined
        assertThat(booksDao.getBooksSuspend().toSet())
            .isEqualTo(setOf(TestUtil.BOOK_1, TestUtil.BOOK_2, TestUtil.BOOK_3))
    }

    @Test
    fun withWriteTransaction_multipleTransactions() = runTest {
        booksDao.insertPublisherSuspend(TestUtil.PUBLISHER.publisherId, TestUtil.PUBLISHER.name)

        coroutineScope {
            launch { database.withWriteTransaction { booksDao.insertBookSuspend(TestUtil.BOOK_1) } }
            launch { database.withWriteTransaction { booksDao.insertBookSuspend(TestUtil.BOOK_2) } }
        }

        // as Set since insertion order is undefined
        assertThat(booksDao.getAllBooks().toSet())
            .isEqualTo(setOf(TestUtil.BOOK_1, TestUtil.BOOK_2))
    }

    @Test
    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    fun withWriteTransaction_multipleTransactions_multipleThreads() = runTest {
        booksDao.insertPublisherSuspend(TestUtil.PUBLISHER.publisherId, TestUtil.PUBLISHER.name)

        coroutineScope {
            launch(newSingleThreadContext("asyncThread1")) {
                database.withWriteTransaction {
                    delay(100)
                    booksDao.insertBookSuspend(TestUtil.BOOK_1)
                }
            }
            launch(newSingleThreadContext("asyncThread2")) {
                database.withWriteTransaction {
                    delay(100)
                    booksDao.insertBookSuspend(TestUtil.BOOK_2)
                }
            }
        }

        // as Set since insertion order is undefined
        assertThat(booksDao.getAllBooks().toSet())
            .isEqualTo(setOf(TestUtil.BOOK_1, TestUtil.BOOK_2))
    }

    @Test
    fun withWriteTransaction_databaseOpenError() = runTest {
        val localDatabase =
            Room.inMemoryDatabaseBuilder<TestDatabase>(ApplicationProvider.getApplicationContext())
                .setDriver(
                    when (useDriver) {
                        UseDriver.ANDROID -> AndroidSQLiteDriver()
                        UseDriver.BUNDLED -> BundledSQLiteDriver()
                    }
                )
                .addCallback(
                    object : RoomDatabase.Callback() {
                        override suspend fun onOpen(connection: SQLiteConnection) {
                            // this causes all transaction functions to throw, this can happen IRL
                            throw RuntimeException("Error opening Database.")
                        }
                    }
                )
                .build()
        assertThrows<RuntimeException> {
                localDatabase.withWriteTransaction { fail("This transaction should never run.") }
            }
            .hasMessageThat()
            .contains("Error opening Database.")

        localDatabase.close()
    }

    @Test
    fun withWriteTransaction_setTransactionSuccessful_error() = runTest {
        assertThrows<IllegalStateException> {
                database.withWriteTransaction {
                    // ending transaction prematurely so that setTransactionSuccessful() invoked by
                    // withTransaction throws.
                    database.getSupportWrapper().endTransaction()
                }
            }
            .hasMessageThat()
            .contains("Cannot perform this operation because there is no current transaction")
    }

    @Test
    fun withWriteTransaction_endTransaction_error() = runTest {
        assertThrows<IllegalStateException> {
                database.withWriteTransaction {
                    // ending transaction prematurely and quickly throwing so that endTransaction()
                    // invoked by withTransaction throws.
                    database.getSupportWrapper().endTransaction()
                    // this exception will get swallowed by the exception thrown in endTransaction()
                    throw RuntimeException()
                }
            }
            .hasMessageThat()
            .contains("Cannot perform this operation because there is no current transaction")
    }

    @Test
    fun suspendTransactionFunctionWithSuspendFunctionalParamDoesNotCommitWhenError() = runTest {
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
    fun withTransaction_instantTaskExecutorRule() = runTest {
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
        database.withWriteTransaction {
            booksDao.insertPublisherSuspend(TestUtil.PUBLISHER.publisherId, TestUtil.PUBLISHER.name)
        }
        assertThat(booksDao.getPublishers().size).isEqualTo(1)
    }

    @Test
    fun withWriteTransaction_singleExecutorDispatcher() = runTest {
        val executor = Executors.newSingleThreadExecutor()
        val localDatabase =
            Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    TestDatabase::class.java,
                )
                .setDriver(
                    when (useDriver) {
                        UseDriver.ANDROID -> AndroidSQLiteDriver()
                        UseDriver.BUNDLED -> BundledSQLiteDriver()
                    }
                )
                .setQueryCoroutineContext(executor.asCoroutineDispatcher())
                .build()

        withContext(executor.asCoroutineDispatcher()) {
            localDatabase.withWriteTransaction {
                localDatabase
                    .booksDao()
                    .insertPublisherSuspend(TestUtil.PUBLISHER.publisherId, TestUtil.PUBLISHER.name)
            }
        }
        assertThat(localDatabase.booksDao().getPublishers().size).isEqualTo(1)

        executor.shutdown()
        assertThat(executor.awaitTermination(1, TimeUnit.SECONDS)).isTrue()

        localDatabase.close()
    }

    @Test
    fun withTransaction_reentrant_nested() = runTest {
        val executor = Executors.newSingleThreadExecutor()
        val localDatabase =
            Room.inMemoryDatabaseBuilder<TestDatabase>(ApplicationProvider.getApplicationContext())
                .setDriver(
                    when (useDriver) {
                        UseDriver.ANDROID -> AndroidSQLiteDriver()
                        UseDriver.BUNDLED -> BundledSQLiteDriver()
                    }
                )
                .setQueryCoroutineContext(executor.asCoroutineDispatcher())
                .build()

        withContext(executor.asCoroutineDispatcher()) {
            localDatabase.withWriteTransaction {
                localDatabase
                    .booksDao()
                    .insertPublisherSuspend(TestUtil.PUBLISHER.publisherId, TestUtil.PUBLISHER.name)
                localDatabase.withWriteTransaction {
                    localDatabase.booksDao().insertBookSuspend(TestUtil.BOOK_1)
                }
            }
        }
        assertThat(localDatabase.booksDao().getPublishers().size).isEqualTo(1)
        assertThat(localDatabase.booksDao().getAllBooks().size).isEqualTo(1)

        executor.shutdown()
        assertThat(executor.awaitTermination(1, TimeUnit.SECONDS)).isTrue()

        localDatabase.close()
    }

    @Test
    fun withWriteTransaction_reentrant_nested_exception() = runTest {
        val executor = Executors.newSingleThreadExecutor()
        val localDatabase =
            Room.inMemoryDatabaseBuilder<TestDatabase>(ApplicationProvider.getApplicationContext())
                .setDriver(
                    when (useDriver) {
                        UseDriver.ANDROID -> AndroidSQLiteDriver()
                        UseDriver.BUNDLED -> BundledSQLiteDriver()
                    }
                )
                .setQueryCoroutineContext(executor.asCoroutineDispatcher())
                .build()
        withContext(executor.asCoroutineDispatcher()) {
            localDatabase.withWriteTransaction {
                localDatabase
                    .booksDao()
                    .insertPublisherSuspend(TestUtil.PUBLISHER.publisherId, TestUtil.PUBLISHER.name)
                assertThrows<IOException> {
                        localDatabase.withWriteTransaction {
                            localDatabase.booksDao().insertBookSuspend(TestUtil.BOOK_1)
                            throw IOException("Boom!")
                        }
                    }
                    .hasMessageThat()
                    .contains("Boom!")
            }
        }
        assertThat(localDatabase.booksDao().getAllBooks()).isEmpty()

        executor.shutdown()
        assertThat(executor.awaitTermination(1, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun withWriteTransaction_reentrant_nested_contextSwitch() = runTest {
        val executor = Executors.newSingleThreadExecutor()
        val localDatabase =
            Room.inMemoryDatabaseBuilder<TestDatabase>(ApplicationProvider.getApplicationContext())
                .setDriver(
                    when (useDriver) {
                        UseDriver.ANDROID -> AndroidSQLiteDriver()
                        UseDriver.BUNDLED -> BundledSQLiteDriver()
                    }
                )
                .setQueryCoroutineContext(executor.asCoroutineDispatcher())
                .build()

        withContext(executor.asCoroutineDispatcher()) {
            localDatabase.withWriteTransaction {
                localDatabase
                    .booksDao()
                    .insertPublisherSuspend(TestUtil.PUBLISHER.publisherId, TestUtil.PUBLISHER.name)
                withContext(Dispatchers.IO) {
                    localDatabase.withWriteTransaction {
                        localDatabase.booksDao().insertBookSuspend(TestUtil.BOOK_1)
                    }
                }
            }
        }
        assertThat(localDatabase.booksDao().getPublishers().size).isEqualTo(1)
        assertThat(localDatabase.booksDao().getAllBooks().size).isEqualTo(1)

        executor.shutdown()
        assertThat(executor.awaitTermination(1, TimeUnit.SECONDS)).isTrue()

        localDatabase.close()
    }

    @Test
    fun withWriteTransaction_runBlocking() {
        runBlocking {
            database.withWriteTransaction {
                booksDao.insertPublisherSuspend(
                    TestUtil.PUBLISHER.publisherId,
                    TestUtil.PUBLISHER.name,
                )
                booksDao.insertBookSuspend(TestUtil.BOOK_1.copy(salesCnt = 0))
                booksDao.insertBookSuspend(TestUtil.BOOK_2)
                booksDao.deleteUnsoldBooks()
            }
            assertThat(booksDao.getBooksSuspend()).isEqualTo(listOf(TestUtil.BOOK_2))
        }
    }

    @Test
    fun withWriteTransaction_stress_testMutation() = runTest {
        val output = mutableListOf<String>()
        repeat(5000) { count ->
            database.withWriteTransaction {
                output.add("$count")
                suspendHere()
                output.add("$count")
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
    fun withWriteTransaction_stress_dbMutation() = runTest {
        val context: Context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("test_stress_dbMutation.db")
        val db = Room.databaseBuilder<TestDatabase>(context, "test_stress_dbMutation.db").build()
        db.counterDao().upsert(Counter(1, 0))
        coroutineScope {
            repeat(5000) {
                launch(Dispatchers.IO) {
                    db.withWriteTransaction {
                        val current = db.counterDao().getCounter(1)
                        suspendHere()
                        db.counterDao().upsert(current.copy(value = current.value + 1))
                    }
                }
            }
        }
        val count = db.counterDao().getCounter(1)
        assertThat(count.value).isEqualTo(5000)
        db.close()
    }

    // Utility function to _really_ suspend.
    private suspend fun suspendHere(): Unit = suspendCoroutineUninterceptedOrReturn {
        it.intercepted().resume(Unit)
        COROUTINE_SUSPENDED
    }
}
