/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.room.coroutines

import androidx.kruth.ThrowableSubject
import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import androidx.room.PooledConnection
import androidx.room.Transactor
import androidx.room.concurrent.AtomicBoolean
import androidx.room.concurrent.AtomicInt
import androidx.room.deferredTransaction
import androidx.room.exclusiveTransaction
import androidx.room.execSQL
import androidx.room.immediateTransaction
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.SQLiteException
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.execSQL
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.fail
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
abstract class BaseConnectionPoolTest {

    abstract fun getDriver(): SQLiteDriver

    abstract val fileName: String

    @Test
    fun readerIsReadOnlyConnection() = runTest {
        val driver = setupDriver()
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1,
            )
        fun ThrowableSubject<SQLiteException>.assertMsg() {
            hasMessageThat()
                .isEqualTo("Error code: 8, message: attempt to write a readonly database")
        }
        pool.useReaderConnection { connection ->
            assertThrows<SQLiteException> {
                    connection.execSQL("INSERT INTO Pet (id, name) VALUES (100, 'Pelusa')")
                }
                .assertMsg()
            assertThrows<SQLiteException> { connection.execSQL("CREATE TABLE Foo (id)") }
                .assertMsg()
            // Yup, temp tables even though local to a connection are considered write operations.
            assertThrows<SQLiteException> { connection.execSQL("CREATE TEMP TABLE TempFoo (id)") }
                .assertMsg()
            assertThrows<SQLiteException> { connection.execSQL("PRAGMA user_version = 100") }
                .assertMsg()
            assertThrows<SQLiteException> { connection.exclusiveTransaction {} }.assertMsg()
            assertThrows<SQLiteException> { connection.immediateTransaction {} }.assertMsg()
        }
        pool.close()
    }

    @Test
    fun reusingConnection() = runTest {
        val driver = setupDriver()
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1,
            )
        var count = 0
        pool.useReaderConnection { initialConnection ->
            pool.useReaderConnection { reusedConnection ->
                reusedConnection.usePrepared("SELECT * FROM Pet") {
                    while (it.step()) {
                        count++
                    }
                }
                assertThat(reusedConnection).isEqualTo(initialConnection)
            }
        }
        assertThat(count).isEqualTo(20)
        pool.close()
    }

    @Test
    fun reusingConnectionOnLaunch() = runTest {
        val driver = setupDriver()
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1,
            )
        var count = 0
        pool.useReaderConnection { initialConnection ->
            coroutineScope {
                launch {
                        pool.useReaderConnection { reusedConnection ->
                            reusedConnection.usePrepared("SELECT * FROM Pet") {
                                while (it.step()) {
                                    count++
                                }
                            }
                            assertThat(reusedConnection).isEqualTo(initialConnection)
                        }
                    }
                    .join()
            }
        }
        assertThat(count).isEqualTo(20)
        pool.close()
    }

    @Test
    fun reusingConnectionOnAsync() = runTest {
        val driver = setupDriver()
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1,
            )
        var count = 0
        pool.useReaderConnection { initialConnection ->
            coroutineScope {
                async {
                        pool.useReaderConnection { reusedConnection ->
                            reusedConnection.usePrepared("SELECT * FROM Pet") {
                                while (it.step()) {
                                    count++
                                }
                            }
                            assertThat(reusedConnection).isEqualTo(initialConnection)
                        }
                    }
                    .await()
            }
        }
        assertThat(count).isEqualTo(20)
        pool.close()
    }

    @Test
    fun reusingConnectionWithContext() = runTest {
        val driver = setupDriver()
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1,
            )
        var count = 0
        pool.useReaderConnection { initialConnection ->
            withContext(Dispatchers.IO) {
                pool.useReaderConnection { reusedConnection ->
                    reusedConnection.usePrepared("SELECT * FROM Pet") {
                        while (it.step()) {
                            count++
                        }
                    }
                    assertThat(reusedConnection).isEqualTo(initialConnection)
                }
            }
        }
        assertThat(count).isEqualTo(20)
        pool.close()
    }

    @Test
    fun failureToUpgradeConnection() = runTest {
        val driver = setupDriver()
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1,
            )
        pool.useReaderConnection {
            assertThrows<SQLiteException> { pool.useWriterConnection {} }
                .hasMessageThat()
                .isEqualTo(
                    "Error code: 1, message: Cannot upgrade connection from reader to writer"
                )
        }
        pool.close()
    }

    @Test
    fun failureToOpenConnection() = runTest {
        val actualDriver = setupDriver()
        val driver =
            object : SQLiteDriver {
                private val throwIntermediateError = AtomicBoolean(true)

                override fun open(fileName: String): SQLiteConnection {
                    if (throwIntermediateError.compareAndSet(true, false)) {
                        throw SQLiteException("Intermediate Error")
                    }
                    return actualDriver.open(fileName)
                }
            }
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1,
            )
        assertThrows<SQLiteException> { pool.useWriterConnection {} }
            .hasMessageThat()
            .isEqualTo("Intermediate Error")

        pool.useWriterConnection { it.execSQL("PRAGMA user_version = 5") }
        val result =
            pool.useReaderConnection {
                it.usePrepared("PRAGMA user_version") {
                    it.step()
                    it.getLong(0)
                }
            }
        assertThat(result).isEqualTo(5)
        pool.close()
    }

    @Test
    fun cannotUseAlreadyRecycledConnection() = runTest {
        val driver = setupDriver()
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1,
            )
        var leakedConnection: PooledConnection? = null
        pool.useReaderConnection { leakedConnection = it }
        assertThrows<SQLiteException> { leakedConnection!!.usePrepared("SELECT * FROM Pet") {} }
            .hasMessageThat()
            .isEqualTo("Error code: 21, message: Connection is recycled")
        pool.close()
    }

    @Test
    fun cannotUseAlreadyRecycledStatement() = runTest {
        val driver = setupDriver()
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1,
            )
        var leakedRawStatement: SQLiteStatement? = null
        pool.useReaderConnection { connection ->
            connection.usePrepared("SELECT * FROM Pet") { leakedRawStatement = it }
        }
        assertThrows<SQLiteException> { leakedRawStatement!!.step() }
            .hasMessageThat()
            .isEqualTo("Error code: 21, message: Statement is recycled")
        pool.close()
    }

    @Test
    fun cannotUsedAlreadyClosedPool() = runTest {
        val driver = setupDriver()
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1,
            )
        pool.close()
        assertThrows<SQLiteException> { pool.useWriterConnection {} }
            .hasMessageThat()
            .isEqualTo("Error code: 21, message: Connection pool is closed")
    }

    @Test
    fun idempotentPoolClosing() {
        val driver = setupDriver()
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1,
            )
        pool.close()
        pool.close()
    }

    @Test
    fun connectionUsedOnWrongCoroutine() = runTest {
        val singleThreadContext = newFixedThreadPoolContext(1, "Test-Threads")
        val driver = setupDriver()
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1,
            )
        pool.useReaderConnection { connection ->
            launch(singleThreadContext) {
                    assertThrows<SQLiteException> { connection.usePrepared("SELECT * FROM Pet") {} }
                        .hasMessageThat()
                        .isEqualTo(
                            "Error code: 21, message: Attempted to use connection on a different coroutine"
                        )
                }
                .join()
        }
        pool.close()
        singleThreadContext.close()
    }

    @Test
    fun connectionUsedOnWrongCoroutineWithLeakedContext() = runTest {
        val singleThreadContext = newSingleThreadContext("Test-Thread")
        val driver = setupDriver()
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1,
            )
        var leakedContext: CoroutineContext? = null
        var leakedConnection: PooledConnection? = null
        val job =
            launch(singleThreadContext) {
                while (leakedContext == null && leakedConnection == null) {
                    delay(20)
                }
                withContext(leakedContext!!) {
                    assertThrows<SQLiteException> {
                            leakedConnection!!.usePrepared("SELECT * FROM Pet") {}
                        }
                        .hasMessageThat()
                        .isEqualTo(
                            "Error code: 21, message: Attempted to use connection on a different coroutine"
                        )
                }
            }
        pool.useReaderConnection {
            leakedContext = coroutineContext
            leakedConnection = it
            job.join()
        }
        pool.close()
        singleThreadContext.close()
    }

    @Test
    fun statementUsedOnWrongThread() = runTest {
        val singleThreadContext = newFixedThreadPoolContext(1, "Test-Threads")
        val driver = setupDriver()
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1,
            )
        pool.useReaderConnection { connection ->
            connection.usePrepared("SELECT * FROM Pet") { statement ->
                val expectedErrorMsg =
                    "Error code: 21, message: Attempted to use statement on a different thread"
                runBlocking(singleThreadContext) {
                    assertThrows<SQLiteException> { statement.step() }
                        .hasMessageThat()
                        .isEqualTo(expectedErrorMsg)
                    assertThrows<SQLiteException> { statement.getColumnCount() }
                        .hasMessageThat()
                        .isEqualTo(expectedErrorMsg)
                    assertThrows<SQLiteException> { statement.getColumnName(0) }
                        .hasMessageThat()
                        .isEqualTo(expectedErrorMsg)
                    assertThrows<SQLiteException> { statement.isNull(0) }
                        .hasMessageThat()
                        .isEqualTo(expectedErrorMsg)
                    assertThrows<SQLiteException> { statement.getLong(0) }
                        .hasMessageThat()
                        .isEqualTo(expectedErrorMsg)
                    assertThrows<SQLiteException> { statement.getText(0) }
                        .hasMessageThat()
                        .isEqualTo(expectedErrorMsg)
                    assertThrows<SQLiteException> { statement.bindText(0, "") }
                        .hasMessageThat()
                        .isEqualTo(expectedErrorMsg)
                    assertThrows<SQLiteException> { statement.bindLong(0, 0L) }
                        .hasMessageThat()
                        .isEqualTo(expectedErrorMsg)
                    assertThrows<SQLiteException> { statement.close() }
                        .hasMessageThat()
                        .isEqualTo(expectedErrorMsg)
                }
            }
        }
        pool.close()
        singleThreadContext.close()
    }

    @Test
    fun useStatementLocksConnection() = runTest {
        val multiThreadContext = newFixedThreadPoolContext(2, "Test-Threads")
        val driver = setupDriver()
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1,
            )
        var count = 0
        pool.useReaderConnection { connection ->
            coroutineScope {
                val mutex = Mutex(locked = true)
                launch(multiThreadContext) {
                    connection.usePrepared("SELECT * FROM Pet") {
                        runBlocking { mutex.withLock {} }
                        while (it.step()) {
                            count++
                        }
                    }
                }
                launch(multiThreadContext) {
                    assertThrows<TimeoutCancellationException> {
                        withTimeout(200) {
                            delay(50) // to let statement above be used first
                            connection.usePrepared("SELECT * FROM Pet") {
                                fail("Statement should never prepare")
                            }
                        }
                    }
                    mutex.unlock()
                }
            }
        }
        pool.close()
        multiThreadContext.close()
        assertThat(count).isEqualTo(20)
    }

    @Test
    fun singleConnectionPool() = runTest {
        val multiThreadContext = newFixedThreadPoolContext(2, "Test-Threads")
        val connectionsOpened = AtomicInt(0)
        val actualDriver = setupDriver()
        val driver =
            object : SQLiteDriver by actualDriver {
                override fun open(fileName: String) =
                    actualDriver.open(fileName).also { connectionsOpened.incrementAndGet() }
            }
        val pool = newSingleConnectionPool(driver, ":memory:")
        val jobs = mutableListOf<Job>()
        repeat(5) {
            val job1 =
                launch(multiThreadContext) {
                    pool.useReaderConnection { pool.useWriterConnection {} }
                }
            jobs.add(job1)
            val job2 =
                launch(multiThreadContext) {
                    pool.useWriterConnection { pool.useReaderConnection {} }
                }
            jobs.add(job2)
        }
        jobs.joinAll()
        pool.close()
        multiThreadContext.close()
        assertThat(connectionsOpened.get()).isEqualTo(1)
    }

    @Test
    fun openOneConnectionWhenUsedSerially() = runTest {
        val connectionsOpened = AtomicInt(0)
        val actualDriver = setupDriver()
        val driver =
            object : SQLiteDriver by actualDriver {
                override fun open(fileName: String) =
                    actualDriver.open(fileName).also { connectionsOpened.incrementAndGet() }
            }
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 4,
                maxNumOfWriters = 1,
            )
        repeat(5) {
            pool.useReaderConnection { connection ->
                var count = 0
                connection.usePrepared("SELECT * FROM Pet") {
                    while (it.step()) {
                        count++
                    }
                }
                assertThat(count).isEqualTo(20)
            }
        }
        pool.close()
        assertThat(connectionsOpened.get()).isEqualTo(1)
    }

    @Test
    fun cancelCoroutineWaitingForConnection() = runTest {
        val multiThreadContext = newFixedThreadPoolContext(2, "Test-Threads")
        val driver = setupDriver()
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1,
            )
        val coroutineStartedMutex = Mutex(locked = true)
        var acquiredSecondConnection = false
        pool.useWriterConnection {
            val job =
                launch(multiThreadContext) {
                    coroutineStartedMutex.unlock()
                    pool.useWriterConnection { acquiredSecondConnection = true }
                }
            coroutineStartedMutex.withLock {
                delay(300)
                job.cancelAndJoin()
            }
        }
        pool.close()
        multiThreadContext.close()
        assertThat(acquiredSecondConnection).isFalse()
    }

    @Test
    fun stressCancelCoroutineAcquiringConnection() = runTest {
        val multiThreadContext = newFixedThreadPoolContext(3, "Test-Threads")
        val driver = setupDriver()
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1,
            )
        // This stress test is very non-deterministic, on purpose. It launches three coroutines, two
        // of them attempt to use the connection, but one of the coroutines is canceled shortly
        // after by the third coroutines. The goal of this test is to validate the Pool's semaphore
        // behaviour does not leave a lingering connection.
        val jobsToWaitFor = mutableListOf<Job>()
        repeat(1000) {
            val jobToCancel =
                launch(multiThreadContext + CoroutineName("TheOneWhichIsCancelled")) {
                    pool.useWriterConnection { delay(Random.nextLong(5)) }
                }
            jobsToWaitFor.add(
                launch(multiThreadContext + CoroutineName("TheExtraOne")) {
                    pool.useWriterConnection { delay(Random.nextLong(5)) }
                }
            )
            jobsToWaitFor.add(
                launch(multiThreadContext + CoroutineName("TheOneWhoCancels")) {
                    delay(Random.nextLong(5))
                    jobToCancel.cancel()
                }
            )
        }
        jobsToWaitFor.joinAll()

        pool.close()
        multiThreadContext.close()
    }

    @Test
    fun timeoutExceptionWaitingForConnection() = runTest {
        val multiThreadContext = newFixedThreadPoolContext(2, "Test-Threads")
        val driver = setupDriver()
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1,
            )
        check(pool is ConnectionPoolImpl)
        pool.throwOnTimeout = true
        val coroutineStartedMutex = Mutex(locked = true)
        var acquiredSecondConnection = false
        val testContext = coroutineContext
        withContext(multiThreadContext) {
            pool.useReaderConnection {
                val job =
                    launch(testContext) {
                        coroutineStartedMutex.unlock()
                        assertThrows<SQLiteException> {
                                pool.useReaderConnection { acquiredSecondConnection = true }
                            }
                            .hasMessageThat()
                            .contains(
                                "Error code: 5, message: Timed out attempting to acquire a " +
                                    "reader connection"
                            )
                    }
                coroutineStartedMutex.withLock {
                    delay(300)
                    job.join()
                }
            }
        }
        pool.close()
        multiThreadContext.close()
        assertThat(acquiredSecondConnection).isFalse()
    }

    @Test
    fun timeoutExceptionAndRetryUsingConnection() = runTest {
        val multiThreadContext = newFixedThreadPoolContext(2, "Test-Threads")
        val driver = setupDriver()
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1,
            )
        check(pool is ConnectionPoolImpl)
        pool.throwOnTimeout = true
        pool.timeout = 100.milliseconds

        val firstBarrier = CompletableDeferred<Unit>()
        val secondBarrier = CompletableDeferred<Unit>()
        val busyJob =
            launch(multiThreadContext) {
                pool.useReaderConnection {
                    firstBarrier.complete(Unit)
                    secondBarrier.await()
                }
            }

        val timeoutJob =
            launch(multiThreadContext) {
                assertThrows<SQLiteException> {
                        firstBarrier.await()
                        pool.useReaderConnection {}
                    }
                    .hasMessageThat()
                    .contains(
                        "Error code: 5, message: Timed out attempting to acquire a reader connection"
                    )
            }

        timeoutJob.join()
        secondBarrier.complete(Unit)
        busyJob.join()

        pool.useReaderConnection {
            // Can use connection after a timeout
        }

        pool.close()
        multiThreadContext.close()
    }

    @Test
    fun timeoutWithoutException() = runTest {
        val multiThreadContext = newFixedThreadPoolContext(2, "Test-Threads")
        val driver = setupDriver()
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1,
            )
        check(pool is ConnectionPoolImpl)
        pool.throwOnTimeout = false
        pool.timeout = 100.milliseconds

        val items = mutableListOf<String>()
        coroutineScope {
            val busyBarrier = CompletableDeferred<Unit>()
            launch(multiThreadContext) {
                pool.useReaderConnection {
                    busyBarrier.complete(Unit)
                    delay(200)
                    items.add("BusyJob")
                }
            }
            launch(multiThreadContext) {
                busyBarrier.await()
                pool.useReaderConnection { items.add("TimeoutJob") }
            }
        }
        assertThat(items).containsExactly("BusyJob", "TimeoutJob").inOrder()
        pool.close()
        multiThreadContext.close()
    }

    @Test
    fun timeoutCoroutineWithResource() = runBlocking {
        val openedConnections = AtomicInt(0)
        val driver =
            object : SQLiteDriver {
                override fun open(fileName: String): SQLiteConnection {
                    openedConnections.incrementAndGet()
                    return object : SQLiteConnection {

                        override fun inTransaction() = false

                        override fun prepare(sql: String): SQLiteStatement {
                            return FakeSQLiteStatement()
                        }

                        override fun close() {
                            openedConnections.decrementAndGet()
                        }
                    }
                }
            }
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 100,
                maxNumOfWriters = 1,
            )

        // prime the pool with connections
        val barriers = List(100) { CompletableDeferred<Unit>() }
        val latch = CompletableDeferred<Unit>()
        List(100) { i ->
                launch(Dispatchers.IO) {
                    pool.useReaderConnection {
                        barriers[i].complete(Unit)
                        latch.await()
                    }
                }
            }
            .run {
                barriers.awaitAll()
                latch.complete(Unit)
                joinAll() // wait for all coroutines to prime the pool
            }

        assertThat(openedConnections.get()).isEqualTo(100)

        // create a lot of coroutines, some timeout, some don't, validating we are using withTimeout
        // with resources correctly as recommended in
        // https://kotlinlang.org/docs/cancellation-and-timeouts.html#asynchronous-timeout-and-resources
        check(pool is ConnectionPoolImpl)
        pool.throwOnTimeout = true
        pool.timeout = 20.milliseconds
        coroutineScope {
            repeat(10_000) {
                launch(Dispatchers.IO) {
                    try {
                        pool.useReaderConnection { delay(10) }
                    } catch (_: SQLiteException) {
                        // Timeout
                    }
                }
            }
        }

        // at the end of various acquire with timeouts all 100 connections should be back in the
        // pool along with the permits
        pool.timeout = 0.milliseconds
        assertThrows<SQLiteException> { pool.useReaderConnection {} }
            .hasMessageThat()
            .apply {
                contains(
                    "Error code: 5, message: Timed out attempting to acquire a reader connection"
                )
                contains("capacity=100, permits=100, queue=(size=100)[")
            }

        pool.close()
    }

    @Test
    fun withTimeoutUsingConnection() = runTest {
        val driver = setupDriver()
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1,
            )
        assertThrows<TimeoutCancellationException> {
            pool.useWriterConnection {
                withTimeout(0) { fail("withTimeout body should never run") }
            }
        }
        pool.useWriterConnection { connection ->
            connection.usePrepared("SELECT COUNT(*) FROM Pet") {
                assertThat(it.step()).isTrue()
                assertThat(it.getLong(0)).isEqualTo(20)
            }
        }
        pool.close()
    }

    @Test
    fun errorUsingConnection() = runTest {
        val driver = setupDriver()
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1,
            )
        assertThrows<IllegalStateException> { pool.useWriterConnection { error("BOOM") } }
            .hasMessageThat()
            .isEqualTo("BOOM")
        pool.useWriterConnection { connection ->
            connection.usePrepared("SELECT COUNT(*) FROM Pet") {
                assertThat(it.step()).isTrue()
                assertThat(it.getLong(0)).isEqualTo(20)
            }
        }
        pool.close()
    }

    @Test
    fun closeUnusedConnections() = runTest {
        class CloseAwareConnection(val actual: SQLiteConnection) : SQLiteConnection by actual {
            var isClosed = false

            override fun close() {
                isClosed = true
                actual.close()
            }
        }
        val connectionArrCount = AtomicInt(0)
        val connectionsArr = arrayOfNulls<CloseAwareConnection>(4)
        val actualDriver = setupDriver()
        val driver =
            object : SQLiteDriver by actualDriver {
                override fun open(fileName: String) =
                    CloseAwareConnection(actualDriver.open(fileName)).also {
                        connectionsArr[connectionArrCount.getAndIncrement()] = it
                    }
            }
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 4,
                maxNumOfWriters = 1,
            )
        val multiThreadContext = newFixedThreadPoolContext(4, "Test-Threads")
        val useLatches = List(4) { CompletableDeferred<Unit>() }
        val barrier = CompletableDeferred<Unit>()
        val jobs =
            List(4) { i ->
                launch(multiThreadContext) {
                    pool.useReaderConnection {
                        useLatches[i].complete(Unit)
                        barrier.await()
                    }
                }
            }
        useLatches.awaitAll()
        barrier.complete(Unit)
        jobs.joinAll()
        pool.close()
        multiThreadContext.close()
        // 4 readers are expected to have been opened
        assertThat(connectionArrCount.get()).isEqualTo(4)
        assertThat(connectionsArr.filterNotNull().size).isEqualTo(4)
        connectionsArr.forEach { assertThat(checkNotNull(it).isClosed).isTrue() }
    }

    @Test
    fun rollbackTransaction() = runTest {
        val driver = setupDriver()
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1,
            )
        pool.useWriterConnection { connection ->
            connection.exclusiveTransaction {
                execSQL("INSERT INTO Pet (id, name) VALUES (100, 'Pelusa')")
                usePrepared("SELECT * FROM Pet WHERE id = 100") {
                    assertThat(it.step()).isTrue()
                    assertThat(it.getText(1)).isEqualTo("Pelusa")
                }
                rollback(Unit)
            }
            connection.usePrepared("SELECT * FROM Pet WHERE id = 100") {
                assertThat(it.step()).isFalse()
            }
        }
        pool.close()
    }

    @Test
    fun rollbackTransactionWithResult() = runTest {
        val driver = setupDriver()
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1,
            )
        pool.useWriterConnection { connection ->
            connection.execSQL("CREATE TEMP TABLE Cat (name)")
            val name = "Pelusa"
            val result =
                connection.exclusiveTransaction {
                    val newName =
                        usePrepared("INSERT INTO Cat (name) VALUES ('$name') RETURNING *") { stmt ->
                            assertThat(stmt.step()).isTrue()
                            stmt.getText(0).also { assertThat(it).isEqualTo(name) }
                        }
                    rollback(newName)
                }
            assertThat(result).isEqualTo(name)
        }
        pool.close()
    }

    @Test
    fun rollbackTransactionDueToException() = runTest {
        val driver = setupDriver()
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1,
            )
        pool.useWriterConnection { connection ->
            assertThrows<TestingRollbackException> {
                connection.exclusiveTransaction {
                    execSQL("INSERT INTO Pet (id, name) VALUES (100, 'Pelusa')")
                    usePrepared("SELECT * FROM Pet WHERE id = 100") {
                        assertThat(it.step()).isTrue()
                        assertThat(it.getText(1)).isEqualTo("Pelusa")
                    }
                    throw TestingRollbackException()
                }
            }
            connection.usePrepared("SELECT * FROM Pet WHERE id = 100") {
                assertThat(it.step()).isFalse()
            }
        }
        pool.close()
    }

    @Test
    fun rollbackNestedTransaction() = runTest {
        val driver = setupDriver()
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1,
            )
        pool.useWriterConnection { connection ->
            connection.exclusiveTransaction {
                execSQL("INSERT INTO Pet (id, name) VALUES (100, 'Pelusa')")
                withNestedTransaction<Unit> {
                    execSQL("INSERT INTO Pet (id, name) VALUES (101, 'Tom')")
                    rollback(Unit)
                }
            }
            connection.usePrepared("SELECT * FROM Pet WHERE id = 100") {
                assertThat(it.step()).isTrue()
                assertThat(it.getText(1)).isEqualTo("Pelusa")
            }
            connection.usePrepared("SELECT * FROM Pet WHERE id = 101") {
                assertThat(it.step()).isFalse()
            }
        }
        pool.close()
    }

    @Test
    fun rollbackParentTransaction() = runTest {
        val driver = setupDriver()
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1,
            )
        pool.useWriterConnection { connection ->
            connection.exclusiveTransaction {
                execSQL("INSERT INTO Pet (id, name) VALUES (100, 'Pelusa')")
                withNestedTransaction { execSQL("INSERT INTO Pet (id, name) VALUES (101, 'Tom')") }
                rollback(Unit)
            }
            // If the top-most parent transaction is rollback so does all its children
            // even if they were successful
            connection.usePrepared("SELECT * FROM Pet WHERE id = 100") {
                assertThat(it.step()).isFalse()
            }
            connection.usePrepared("SELECT * FROM Pet WHERE id = 101") {
                assertThat(it.step()).isFalse()
            }
        }
        pool.close()
    }

    @Test
    fun rollbackDeeplyNestedTransaction() = runTest {
        val driver = setupDriver()
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1,
            )
        pool.useWriterConnection { connection ->
            connection.exclusiveTransaction {
                execSQL("INSERT INTO Pet (id, name) VALUES (100, 'Pelusa')")
                withNestedTransaction<Unit> {
                    execSQL("INSERT INTO Pet (id, name) VALUES (101, 'Tom')")
                    withNestedTransaction {
                        execSQL("INSERT INTO Pet (id, name) VALUES (102, 'Simba')")
                    }
                    rollback(Unit)
                }
            }
            connection.usePrepared("SELECT * FROM Pet WHERE id = 100") {
                assertThat(it.step()).isTrue()
                assertThat(it.getText(1)).isEqualTo("Pelusa")
            }
            connection.usePrepared("SELECT * FROM Pet WHERE id = 101") {
                assertThat(it.step()).isFalse()
            }
            connection.usePrepared("SELECT * FROM Pet WHERE id = 102") {
                assertThat(it.step()).isFalse()
            }
        }
        pool.close()
    }

    @Test
    fun rollbackNestedTransactionOnReusedConnection() = runTest {
        val driver = setupDriver()
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1,
            )
        pool.useWriterConnection { connection ->
            connection.exclusiveTransaction {
                execSQL("INSERT INTO Pet (id, name) VALUES (100, 'Pelusa')")
                pool.useWriterConnection { reusedConnection ->
                    reusedConnection.exclusiveTransaction<Unit> {
                        execSQL("INSERT INTO Pet (id, name) VALUES (101, 'Tom')")
                        rollback(Unit)
                    }
                }
            }
            connection.usePrepared("SELECT * FROM Pet WHERE id = 100") {
                assertThat(it.step()).isTrue()
                assertThat(it.getText(1)).isEqualTo("Pelusa")
            }
            connection.usePrepared("SELECT * FROM Pet WHERE id = 101") {
                assertThat(it.step()).isFalse()
            }
        }
        pool.close()
    }

    @Test
    fun rollbackNestedTransactionDueToExceptionOnReusedConnection() = runTest {
        val driver = setupDriver()
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1,
            )
        pool.useWriterConnection { connection ->
            connection.exclusiveTransaction {
                execSQL("INSERT INTO Pet (id, name) VALUES (100, 'Pelusa')")
                assertThrows<TestingRollbackException> {
                    pool.useWriterConnection { reusedConnection ->
                        reusedConnection.exclusiveTransaction {
                            execSQL("INSERT INTO Pet (id, name) VALUES (101, 'Tom')")
                            throw TestingRollbackException()
                        }
                    }
                }
            }
            connection.usePrepared("SELECT * FROM Pet WHERE id = 100") {
                assertThat(it.step()).isTrue()
                assertThat(it.getText(1)).isEqualTo("Pelusa")
            }
            connection.usePrepared("SELECT * FROM Pet WHERE id = 101") {
                assertThat(it.step()).isFalse()
            }
        }
        pool.close()
    }

    @Test
    fun rollbackEvenWhenCatchingRollbackException() = runTest {
        val driver = setupDriver()
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1,
            )
        pool.useWriterConnection { connection ->
            connection.exclusiveTransaction {
                execSQL("INSERT INTO Pet (id, name) VALUES (100, 'Pelusa')")
                try {
                    rollback(Unit)
                } catch (_: Throwable) {}
            }
            connection.usePrepared("SELECT * FROM Pet WHERE id = 100") {
                assertThat(it.step()).isFalse()
            }
        }
        pool.close()
    }

    @Test
    fun nestedWriteTransactionDoesNotUpgradeConnection() = runTest {
        val driver = setupDriver()
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1,
            )
        var nestedTransactionBlockExecuted = false
        pool.useReaderConnection { connection ->
            connection.deferredTransaction<Unit> {
                pool.useReaderConnection { reusedConnection ->
                    // The parent transaction is deferred and a nested transaction on a
                    // reused connection does not change the transaction type and should cause
                    // no failure of trying to upgrade the connection
                    reusedConnection.exclusiveTransaction { nestedTransactionBlockExecuted = true }
                }
            }
        }
        pool.close()
        assertThat(nestedTransactionBlockExecuted).isTrue()
    }

    @Test
    fun endNestedTransaction() = runTest {
        val driver = setupDriver()
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1,
            )
        pool.useWriterConnection { connection ->
            connection.exclusiveTransaction<Unit> {
                execSQL("INSERT INTO Pet (id, name) VALUES (100, 'Pelusa')")
                withNestedTransaction { execSQL("INSERT INTO Pet (id, name) VALUES (101, 'Tom')") }
            }
            connection.usePrepared("SELECT * FROM Pet WHERE id = 100") {
                assertThat(it.step()).isTrue()
                assertThat(it.getText(1)).isEqualTo("Pelusa")
            }
            connection.usePrepared("SELECT * FROM Pet WHERE id = 101") {
                assertThat(it.step()).isTrue()
                assertThat(it.getText(1)).isEqualTo("Tom")
            }
        }
        pool.close()
    }

    @Test
    fun endNestedTransactionOnReusedConnection() = runTest {
        val driver = setupDriver()
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1,
            )
        pool.useWriterConnection { connection ->
            connection.exclusiveTransaction<Unit> {
                execSQL("INSERT INTO Pet (id, name) VALUES (100, 'Pelusa')")
                pool.useWriterConnection { reusedConnection ->
                    reusedConnection.exclusiveTransaction {
                        execSQL("INSERT INTO Pet (id, name) VALUES (101, 'Tom')")
                    }
                }
            }
            connection.usePrepared("SELECT * FROM Pet WHERE id = 100") {
                assertThat(it.step()).isTrue()
                assertThat(it.getText(1)).isEqualTo("Pelusa")
            }
            connection.usePrepared("SELECT * FROM Pet WHERE id = 101") {
                assertThat(it.step()).isTrue()
                assertThat(it.getText(1)).isEqualTo("Tom")
            }
        }
        pool.close()
    }

    @Test
    fun explicitRollbackTransaction() = runTest {
        val driver = setupDriver()
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1,
            )
        pool.useWriterConnection { connection ->
            assertThrows<SQLiteException> {
                    connection.exclusiveTransaction {
                        execSQL("INSERT INTO Pet (id, name) VALUES (100, 'Pelusa')")
                        execSQL("ROLLBACK TRANSACTION")
                        rollback(Unit)
                    }
                }
                .hasMessageThat()
                .isEqualTo("Error code: 1, message: cannot rollback - no transaction is active")
            connection.usePrepared("SELECT * FROM Pet WHERE id = 100") {
                assertThat(it.step()).isFalse()
            }
        }
        pool.close()
    }

    @Test
    fun explicitEndTransaction() = runTest {
        val driver = setupDriver()
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1,
            )
        pool.useWriterConnection { connection ->
            assertThrows<SQLiteException> {
                    connection.exclusiveTransaction {
                        execSQL("INSERT INTO Pet (id, name) VALUES (100, 'Pelusa')")
                        execSQL("END TRANSACTION")
                    }
                }
                .hasMessageThat()
                .isEqualTo("Error code: 1, message: cannot commit - no transaction is active")
            connection.usePrepared("SELECT * FROM Pet WHERE id = 100") {
                assertThat(it.step()).isTrue()
                assertThat(it.getText(1)).isEqualTo("Pelusa")
            }
        }
        pool.close()
    }

    @Test
    fun unfinishedExplicitTransaction() = runTest {
        val driver = setupDriver()
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1,
            )
        pool.useWriterConnection { connection -> connection.execSQL("BEGIN EXCLUSIVE TRANSACTION") }
        pool.useWriterConnection { connection ->
            connection.exclusiveTransaction {
                execSQL("INSERT INTO Pet (id, name) VALUES (100, 'Pelusa')")
            }
            connection.usePrepared("SELECT * FROM Pet WHERE id = 100") {
                assertThat(it.step()).isTrue()
                assertThat(it.getText(1)).isEqualTo("Pelusa")
            }
        }
        pool.close()
    }

    @Test
    fun parallelConnectionUsage() = runTest {
        val multiThreadContext = newFixedThreadPoolContext(4, "Test-Threads")
        val driver = setupDriver()
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1,
            )
        pool.useReaderConnection { connection ->
            coroutineScope {
                repeat(10) {
                    launch(multiThreadContext) {
                        var count = 0
                        connection.usePrepared("SELECT * FROM Pet") {
                            while (it.step()) {
                                count++
                            }
                        }
                        assertThat(count).isEqualTo(20)
                    }
                }
            }
        }
        pool.close()
        multiThreadContext.close()
    }

    protected fun setupDriver(): SQLiteDriver {
        return getDriver().apply { setupTestDatabase(this) }
    }

    private fun setupTestDatabase(driver: SQLiteDriver) {
        val connection = driver.open(fileName)
        val compileOptions = buildList {
            connection.prepare("PRAGMA compile_options").use {
                while (it.step()) {
                    add(it.getText(0))
                }
            }
        }
        // Connection pool tests are only relevant if on multi-thread mode
        assertThat(compileOptions).contains("THREADSAFE=2")
        // Add a bit of data to run queries during tests.
        connection.execSQL("CREATE TABLE Pet (id INTEGER, name TEXT, PRIMARY KEY(id))")
        for (i in 1..20) {
            connection.execSQL("INSERT INTO Pet (id, name) VALUES ($i, 'Tom_$i')")
        }
        connection.close()
    }

    private class TestingRollbackException : Throwable()
}

internal suspend fun <R> ConnectionPool.useReaderConnection(block: suspend (Transactor) -> R): R =
    this.useConnection(isReadOnly = true, block)

internal suspend fun <R> ConnectionPool.useWriterConnection(block: suspend (Transactor) -> R): R =
    this.useConnection(isReadOnly = false, block)

private class FakeSQLiteStatement : SQLiteStatement {
    override fun bindBlob(index: Int, value: ByteArray) {}

    override fun bindDouble(index: Int, value: Double) {}

    override fun bindLong(index: Int, value: Long) {}

    override fun bindText(index: Int, value: String) {}

    override fun bindNull(index: Int) {}

    override fun getBlob(index: Int): ByteArray = byteArrayOf()

    override fun getDouble(index: Int): Double = 0.0

    override fun getLong(index: Int): Long = 0L

    override fun getText(index: Int): String = ""

    override fun isNull(index: Int): Boolean = false

    override fun getColumnCount(): Int = 0

    override fun getColumnName(index: Int): String = ""

    override fun getColumnType(index: Int): Int = 0

    override fun step(): Boolean = false

    override fun reset() {}

    override fun clearBindings() {}

    override fun close() {}
}
