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

package androidx.room.paging.guava

import android.database.Cursor
import androidx.arch.core.executor.testing.CountingTaskExecutorRule
import androidx.kruth.assertThat
import androidx.kruth.assertWithMessage
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadResult
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.paging.util.ThreadSafeInvalidationObserver
import androidx.room.util.getColumnIndexOrThrow
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.testutils.TestExecutor
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures.addCallback
import com.google.common.util.concurrent.ListenableFuture
import java.util.LinkedList
import java.util.concurrent.CancellationException
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val tableName: String = "TestItem"

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@SmallTest
class LimitOffsetListenableFuturePagingSourceTest {

    @JvmField
    @Rule
    val countingTaskExecutorRule = CountingTaskExecutorRule()

    @Test
    fun initialLoad_registersInvalidationObserver() =
        setupAndRunWithTestExecutor { db, queryExecutor, _ ->
            val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(
                db = db,
                registerObserver = true
            )

            val listenableFuture = pagingSource.refresh()
            assertFalse(pagingSource.privateObserver().privateRegisteredState().get())

            // observer registration is queued up on queryExecutor by refresh() call
            queryExecutor.executeAll()

            assertTrue(pagingSource.privateObserver().privateRegisteredState().get())
            // note that listenableFuture is not done yet
            // The future has been transformed into a ListenableFuture<LoadResult> whose result
            // is still pending
            assertFalse(listenableFuture.isDone)
        }

    @Test
    fun initialEmptyLoad_futureIsDone() = setupAndRun { db ->
        val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(
            db = db,
            registerObserver = true
        )

        runTest {
            val listenableFuture = pagingSource.refresh()
            val page = listenableFuture.await() as LoadResult.Page

            assertThat(page.data).isEmpty()
            assertTrue(listenableFuture.isDone)
        }
    }

    @Test
    fun initialLoad_returnsFutureImmediately() =
        setupAndRunWithTestExecutor { db, queryExecutor, transactionExecutor ->
            val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(
                db = db,
                registerObserver = true
            )

            val listenableFuture = pagingSource.refresh()
            // ensure future is returned even as its result is still pending
            assertFalse(listenableFuture.isDone)
            assertThat(pagingSource.itemCount.get()).isEqualTo(-1)

            queryExecutor.executeAll() // run loadFuture
            transactionExecutor.executeAll() // start initialLoad callable + load data

            val page = listenableFuture.await() as LoadResult.Page
            assertThat(page.data).containsExactlyElementsIn(
                ITEMS_LIST.subList(0, 15)
            )
            assertTrue(listenableFuture.isDone)
        }

    @Test
    fun append_returnsFutureImmediately() =
        setupAndRunWithTestExecutor { db, queryExecutor, _ ->
            val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db)

            pagingSource.itemCount.set(100)

            val listenableFuture = pagingSource.append(key = 20)
            // ensure future is returned even as its result is still pending
            assertFalse(listenableFuture.isDone)

            // run transformAsync and async function
            queryExecutor.executeAll()

            val page = listenableFuture.await() as LoadResult.Page
            assertThat(page.data).containsExactlyElementsIn(
                ITEMS_LIST.subList(20, 25)
            )
            assertTrue(listenableFuture.isDone)
        }

    @Test
    fun prepend_returnsFutureImmediately() =
        setupAndRunWithTestExecutor { db, queryExecutor, _ ->
            val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db)
            pagingSource.itemCount.set(100) // bypass check for initial load

            val listenableFuture = pagingSource.prepend(key = 20)
            // ensure future is returned even as its result is still pending
            assertFalse(listenableFuture.isDone)

            // run transformAsync and async function
            queryExecutor.executeAll()

            val page = listenableFuture.await() as LoadResult.Page
            assertThat(page.data).containsExactlyElementsIn(
                ITEMS_LIST.subList(15, 20)
            )
            assertTrue(listenableFuture.isDone)
        }

    @Test
    fun append_returnsInvalid() =
        setupAndRunWithTestExecutor { db, queryExecutor, _ ->
            val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db)
            pagingSource.itemCount.set(100) // bypass check for initial load

            val listenableFuture = pagingSource.append(key = 50)

            pagingSource.invalidate() // imitate refreshVersionsAsync invalidating the PagingSource
            assertTrue(pagingSource.invalid)

            queryExecutor.executeAll() // run transformAsync and async function

            val result = listenableFuture.await()
            assertThat(result).isInstanceOf<LoadResult.Invalid<*, *>>()
            assertTrue(listenableFuture.isDone)
        }

    @Test
    fun prepend_returnsInvalid() =
        setupAndRunWithTestExecutor { db, queryExecutor, _ ->
            val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db)
            pagingSource.itemCount.set(100) // bypass check for initial load

            val listenableFuture = pagingSource.prepend(key = 50)

            pagingSource.invalidate() // imitate refreshVersionsAsync invalidating the PagingSource
            assertTrue(pagingSource.invalid)

            queryExecutor.executeAll() // run transformAsync and async function

            val result = listenableFuture.await()
            assertThat(result).isInstanceOf<LoadResult.Invalid<*, *>>()
            assertTrue(listenableFuture.isDone)
        }

    @Test
    fun refresh_consecutively() = setupAndRun { db ->
        db.dao.addAllItems(ITEMS_LIST)
        val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db, true)
        val pagingSource2 = LimitOffsetListenableFuturePagingSourceImpl(db, true)

        val listenableFuture1 = pagingSource.refresh(key = 10)
        val listenableFuture2 = pagingSource2.refresh(key = 15)

        // check that first Future completes first. If the first future didn't complete first,
        // this await() would not return.
        val page1 = listenableFuture1.await() as LoadResult.Page
        assertThat(page1.data).containsExactlyElementsIn(
            ITEMS_LIST.subList(10, 25)
        )

        val page2 = listenableFuture2.await() as LoadResult.Page
        assertThat(page2.data).containsExactlyElementsIn(
            ITEMS_LIST.subList(15, 30)
        )
    }

    @Test
    fun append_consecutively() =
        setupAndRunWithTestExecutor { db, queryExecutor, _ ->
            val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db)
            pagingSource.itemCount.set(100) // bypass check for initial load

            assertThat(queryExecutor.queuedSize()).isEqualTo(0)

            val listenableFuture1 = pagingSource.append(key = 10)
            val listenableFuture2 = pagingSource.append(key = 15)

            // both load futures are queued
            assertThat(queryExecutor.queuedSize()).isEqualTo(2)
            queryExecutor.executeNext() // first transformAsync
            queryExecutor.executeNext() // second transformAsync

            // both async functions are queued
            assertThat(queryExecutor.queuedSize()).isEqualTo(2)
            queryExecutor.executeNext() // first async function
            queryExecutor.executeNext() // second async function

            // both nonInitial loads are queued
            assertThat(queryExecutor.queuedSize()).isEqualTo(2)

            queryExecutor.executeNext() // first db load
            val page1 = listenableFuture1.await() as LoadResult.Page
            assertThat(page1.data).containsExactlyElementsIn(
                ITEMS_LIST.subList(10, 15)
            )

            queryExecutor.executeNext() // second db load
            val page2 = listenableFuture2.await() as LoadResult.Page
            assertThat(page2.data).containsExactlyElementsIn(
                ITEMS_LIST.subList(15, 20)
            )

            assertTrue(listenableFuture1.isDone)
            assertTrue(listenableFuture2.isDone)
        }

    @Test
    fun prepend_consecutively() =
        setupAndRunWithTestExecutor { db, queryExecutor, _ ->
            val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db)
            pagingSource.itemCount.set(100) // bypass check for initial load

            assertThat(queryExecutor.queuedSize()).isEqualTo(0)

            val listenableFuture1 = pagingSource.prepend(key = 25)
            val listenableFuture2 = pagingSource.prepend(key = 20)

            // both load futures are queued
            assertThat(queryExecutor.queuedSize()).isEqualTo(2)
            queryExecutor.executeNext() // first transformAsync
            queryExecutor.executeNext() // second transformAsync

            // both async functions are queued
            assertThat(queryExecutor.queuedSize()).isEqualTo(2)
            queryExecutor.executeNext() // first async function
            queryExecutor.executeNext() // second async function

            // both nonInitial loads are queued
            assertThat(queryExecutor.queuedSize()).isEqualTo(2)

            queryExecutor.executeNext() // first db load
            val page1 = listenableFuture1.await() as LoadResult.Page
            assertThat(page1.data).containsExactlyElementsIn(
                ITEMS_LIST.subList(20, 25)
            )

            queryExecutor.executeNext() // second db load
            val page2 = listenableFuture2.await() as LoadResult.Page
            assertThat(page2.data).containsExactlyElementsIn(
                ITEMS_LIST.subList(15, 20)
            )

            assertTrue(listenableFuture1.isDone)
            assertTrue(listenableFuture2.isDone)
        }
    @Test
    fun refresh_onSuccess() = setupAndRun { db ->
        db.dao.addAllItems(ITEMS_LIST)
        val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db, true)

        val listenableFuture = pagingSource.refresh(key = 30)

        var onSuccessReceived = false
        val callbackExecutor = TestExecutor()
        listenableFuture.onSuccess(callbackExecutor) { result ->
            val page = result as LoadResult.Page
            assertThat(page.data).containsExactlyElementsIn(
                ITEMS_LIST.subList(30, 45)
            )
            onSuccessReceived = true
        }

        // wait until Room db's refresh load is complete
        countingTaskExecutorRule.drainTasks(500, TimeUnit.MILLISECONDS)
        assertTrue(listenableFuture.isDone)

        callbackExecutor.executeAll()

        // make sure onSuccess callback was executed
        assertTrue(onSuccessReceived)
        assertTrue(listenableFuture.isDone)
    }

    @Test
    fun append_onSuccess() = setupAndRun { db ->
        db.dao.addAllItems(ITEMS_LIST)
        val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db)
        pagingSource.itemCount.set(100) // bypass check for initial load

        val listenableFuture = pagingSource.append(key = 20)
        // ensure future is returned even as its result is still pending
        assertFalse(listenableFuture.isDone)

        var onSuccessReceived = false
        val callbackExecutor = TestExecutor()
        listenableFuture.onSuccess(callbackExecutor) { result ->
            val page = result as LoadResult.Page
            assertThat(page.data).containsExactlyElementsIn(
                ITEMS_LIST.subList(20, 25)
            )
            onSuccessReceived = true
        }
        // let room db complete load
        countingTaskExecutorRule.drainTasks(500, TimeUnit.MILLISECONDS)
        callbackExecutor.executeAll()

        // make sure onSuccess callback was executed
        assertTrue(onSuccessReceived)
        assertTrue(listenableFuture.isDone)
        }

    @Test
    fun prepend_onSuccess() = setupAndRun { db ->
        db.dao.addAllItems(ITEMS_LIST)
        val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db)
        pagingSource.itemCount.set(100) // bypass check for initial load

        val listenableFuture = pagingSource.prepend(key = 40)
        // ensure future is returned even as its result is still pending
        assertFalse(listenableFuture.isDone)

        var onSuccessReceived = false
        val callbackExecutor = TestExecutor()
        listenableFuture.onSuccess(callbackExecutor) { result ->
            val page = result as LoadResult.Page
            assertThat(page.data).containsExactlyElementsIn(
                ITEMS_LIST.subList(35, 40)
            )
            onSuccessReceived = true
        }
        // let room db complete load
        countingTaskExecutorRule.drainTasks(500, TimeUnit.MILLISECONDS)
        callbackExecutor.executeAll()

        // make sure onSuccess callback was executed
        assertTrue(onSuccessReceived)
        assertTrue(listenableFuture.isDone)
    }

    @Test
    fun refresh_cancelBeforeObserverRegistered_CancellationException() =
        setupAndRunWithTestExecutor { db, queryExecutor, transactionExecutor ->
            val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db, true)

            val listenableFuture = pagingSource.refresh(key = 50)
            assertThat(queryExecutor.queuedSize()).isEqualTo(1) // transformAsync

            // cancel before observer has been registered. This queues up another task which is
            // the cancelled async function
            listenableFuture.cancel(true)

            // even though future is cancelled, transformAsync was already queued up which means
            // observer will still get registered
            assertThat(queryExecutor.queuedSize()).isEqualTo(2)
            // start async function but doesn't proceed further
            queryExecutor.executeAll()

            // ensure initial load is not queued up
            assertThat(transactionExecutor.queuedSize()).isEqualTo(0)

            // await() should throw after cancellation
            assertFailsWith<CancellationException> {
                listenableFuture.await()
            }

            // executors should be idle
            assertThat(queryExecutor.queuedSize()).isEqualTo(0)
            assertThat(transactionExecutor.queuedSize()).isEqualTo(0)
            assertTrue(listenableFuture.isDone)
            // even though initial refresh load is cancelled, the paging source itself
            // is NOT invalidated
            assertFalse(pagingSource.invalid)
        }

    @Test
    fun refresh_cancelAfterObserverRegistered_CancellationException() =
        setupAndRunWithTestExecutor { db, queryExecutor, transactionExecutor ->
            val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db, true)

            val listenableFuture = pagingSource.refresh(key = 50)

            // start transformAsync and register observer
            queryExecutor.executeNext()

            // cancel after observer registration
            listenableFuture.cancel(true)

            // start the async function but it has been cancelled so it doesn't queue up
            // initial load
            queryExecutor.executeNext()

            // initialLoad not queued
            assertThat(transactionExecutor.queuedSize()).isEqualTo(0)

            // await() should throw after cancellation
            assertFailsWith<CancellationException> {
                listenableFuture.await()
            }

            // executors should be idle
            assertThat(queryExecutor.queuedSize()).isEqualTo(0)
            assertThat(transactionExecutor.queuedSize()).isEqualTo(0)
            assertTrue(listenableFuture.isDone)
            // even though initial refresh load is cancelled, the paging source itself
            // is NOT invalidated
            assertFalse(pagingSource.invalid)
        }

    @Test
    fun refresh_cancelAfterLoadIsQueued_CancellationException() =
        setupAndRunWithTestExecutor { db, queryExecutor, transactionExecutor ->
            val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db, true)

            val listenableFuture = pagingSource.refresh(key = 50)

            queryExecutor.executeAll() // run loadFuture and queue up initial load

            listenableFuture.cancel(true)

            // initialLoad has been queued
            assertThat(transactionExecutor.queuedSize()).isEqualTo(1)
            assertThat(queryExecutor.queuedSize()).isEqualTo(0)

            transactionExecutor.executeAll() // room starts transaction but doesn't complete load
            queryExecutor.executeAll() // InvalidationTracker from end of transaction

            // await() should throw after cancellation
            assertFailsWith<CancellationException> {
                listenableFuture.await()
            }

            // executors should be idle
            assertThat(queryExecutor.queuedSize()).isEqualTo(0)
            assertThat(transactionExecutor.queuedSize()).isEqualTo(0)
            assertTrue(listenableFuture.isDone)
            // even though initial refresh load is cancelled, the paging source itself
            // is NOT invalidated
            assertFalse(pagingSource.invalid)
        }

    @Test
    fun append_awaitThrowsCancellationException() =
        setupAndRunWithTestExecutor { db, queryExecutor, _ ->
            val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db)
            pagingSource.itemCount.set(100) // bypass check for initial load

            // queue up the append first
            val listenableFuture = pagingSource.append(key = 20)
            assertThat(queryExecutor.queuedSize()).isEqualTo(1)

            listenableFuture.cancel(true)
            queryExecutor.executeAll()

            // await() should throw after cancellation
            assertFailsWith<CancellationException> {
                listenableFuture.await()
            }

            // although query was executed, it should not complete due to the cancellation signal.
            // If query was completed, paging source would call refreshVersionsAsync manually
            // and queuedSize() would be 1 instead of 0 with InvalidationTracker queued up
            assertThat(queryExecutor.queuedSize()).isEqualTo(0)
        }

    @Test
    fun prepend_awaitThrowsCancellationException() =
        setupAndRunWithTestExecutor { db, queryExecutor, _ ->
            val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db)
            pagingSource.itemCount.set(100) // bypass check for initial load

            // queue up the prepend first
            val listenableFuture = pagingSource.prepend(key = 30)
            assertThat(queryExecutor.queuedSize()).isEqualTo(1)

            listenableFuture.cancel(true)
            queryExecutor.executeAll()

            // await() should throw after cancellation
            assertFailsWith<CancellationException> {
                listenableFuture.await()
            }

            // although query was executed, it should not complete due to the cancellation signal.
            // If query was completed, paging source would call refreshVersionsAsync manually
            // and queuedSize() would be 1 instead of 0 with InvalidationTracker queued up
            assertThat(queryExecutor.queuedSize()).isEqualTo(0)
        }

    @Test
    fun refresh_canceledFutureRunsOnFailureCallback() =
        setupAndRunWithTestExecutor { db, queryExecutor, transactionExecutor ->
            val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db, true)

            val listenableFuture = pagingSource.refresh(key = 30)

            queryExecutor.executeAll() // start transformAsync & async function
            assertThat(transactionExecutor.queuedSize()).isEqualTo(1)

            val callbackExecutor = TestExecutor()
            var onFailureReceived = false
            listenableFuture.onFailure(callbackExecutor) { throwable ->
                assertThat(throwable).isInstanceOf<CancellationException>()
                onFailureReceived = true
            }

            // now cancel future and execute the refresh load. The refresh should not complete.
            listenableFuture.cancel(true)
            transactionExecutor.executeAll()
            assertThat(transactionExecutor.queuedSize()).isEqualTo(0)

            callbackExecutor.executeAll()

            // make sure onFailure callback was executed
            assertTrue(onFailureReceived)
            assertTrue(listenableFuture.isDone)
        }

    @Test
    fun append_canceledFutureRunsOnFailureCallback2() =
        setupAndRunWithTestExecutor { db, queryExecutor, _ ->
            val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db)
            pagingSource.itemCount.set(100) // bypass check for initial load

            val listenableFuture = pagingSource.append(key = 20)
            assertThat(queryExecutor.queuedSize()).isEqualTo(1)

            val callbackExecutor = TestExecutor()
            var onFailureReceived = false
            listenableFuture.onFailure(callbackExecutor) { throwable ->
                assertThat(throwable).isInstanceOf<CancellationException>()
                onFailureReceived = true
            }

            // now cancel future and execute the append load. The append should not complete.
            listenableFuture.cancel(true)

            queryExecutor.executeNext() // transformAsync
            queryExecutor.executeNext() // nonInitialLoad
            // if load was erroneously completed, InvalidationTracker would be queued
            assertThat(queryExecutor.queuedSize()).isEqualTo(0)

            callbackExecutor.executeAll()

            // make sure onFailure callback was executed
            assertTrue(onFailureReceived)
            assertTrue(listenableFuture.isDone)
        }

    @Test
    fun prepend_canceledFutureRunsOnFailureCallback() =
        setupAndRunWithTestExecutor { db, queryExecutor, _ ->
            val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db)
            pagingSource.itemCount.set(100) // bypass check for initial load

            // queue up the prepend first
            val listenableFuture = pagingSource.prepend(key = 30)
            assertThat(queryExecutor.queuedSize()).isEqualTo(1)

            val callbackExecutor = TestExecutor()
            var onFailureReceived = false
            listenableFuture.onFailure(callbackExecutor) { throwable ->
                assertThat(throwable).isInstanceOf<CancellationException>()
                onFailureReceived = true
            }

            // now cancel future and execute the prepend which should not complete.
            listenableFuture.cancel(true)
            queryExecutor.executeNext() // transformAsync
            queryExecutor.executeNext() // nonInitialLoad
            // if load was erroneously completed, InvalidationTracker would be queued
            assertThat(queryExecutor.queuedSize()).isEqualTo(0)

            callbackExecutor.executeAll()

            // make sure onFailure callback was executed
            assertTrue(onFailureReceived)
            assertTrue(listenableFuture.isDone)
        }

    @Test
    fun refresh_AfterCancellation() = setupAndRun { db ->
        db.dao.addAllItems(ITEMS_LIST)
        val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db, true)
        pagingSource.itemCount.set(100) // bypass check for initial load

        val listenableFuture = pagingSource.prepend(key = 50)

        listenableFuture.cancel(true)
        assertFailsWith<CancellationException> {
            listenableFuture.await()
        }

        // new gen after query from previous gen was cancelled
        val pagingSource2 = LimitOffsetListenableFuturePagingSourceImpl(db, true)
        val listenableFuture2 = pagingSource2.refresh()
        val result = listenableFuture2.await() as LoadResult.Page

        // the new generation should load as usual
        assertThat(result.data).containsExactlyElementsIn(
            ITEMS_LIST.subList(0, 15)
        )
    }

    @Test
    fun appendAgain_afterFutureCanceled() = setupAndRun { db ->
        db.dao.addAllItems(ITEMS_LIST)
        val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db)
        pagingSource.itemCount.set(100) // bypass check for initial load

        val listenableFuture = pagingSource.append(key = 30)

        listenableFuture.cancel(true)
        assertFailsWith<CancellationException> {
            listenableFuture.await()
        }
        assertTrue(listenableFuture.isDone)
        assertFalse(pagingSource.invalid)

        val listenableFuture2 = pagingSource.append(key = 30)

        val result = listenableFuture2.await() as LoadResult.Page
        assertThat(result.data).containsExactlyElementsIn(
            ITEMS_LIST.subList(30, 35)
        )
        assertTrue(listenableFuture2.isDone)
    }

    @Test
    fun prependAgain_afterFutureCanceled() = setupAndRun { db ->
        db.dao.addAllItems(ITEMS_LIST)
        val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db)
        pagingSource.itemCount.set(100) // bypass check for initial load

        val listenableFuture = pagingSource.prepend(key = 30)

        listenableFuture.cancel(true)
        assertFailsWith<CancellationException> {
            listenableFuture.await()
        }
        assertFalse(pagingSource.invalid)
        assertTrue(listenableFuture.isDone)

        val listenableFuture2 = pagingSource.prepend(key = 30)

        val result = listenableFuture2.await() as LoadResult.Page
            assertThat(result.data).containsExactlyElementsIn(
                ITEMS_LIST.subList(25, 30)
            )
        assertTrue(listenableFuture2.isDone)
    }

    @Test
    fun append_insertInvalidatesPagingSource() =
        setupAndRunWithTestExecutor { db, queryExecutor, _ ->
            val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(
                db = db,
                registerObserver = true
            )
            pagingSource.itemCount.set(100) // bypass check for initial load

            // queue up the append first
            val listenableFuture = pagingSource.append(key = 20)
            assertThat(queryExecutor.queuedSize()).isEqualTo(1)

            queryExecutor.executeNext() // start transformAsync
            queryExecutor.executeNext() // start async function
            assertThat(queryExecutor.queuedSize()).isEqualTo(1) // nonInitialLoad is queued up

            // run this async separately from queryExecutor
            run {
                db.dao.addItem(TestItem(101))
            }

            // tasks in queue [nonInitialLoad, InvalidationTracker(from additem)]
            assertThat(queryExecutor.queuedSize()).isEqualTo(2)

            // run nonInitialLoad first. The InvalidationTracker
            // is still queued up. This imitates delayed notification from Room.
            queryExecutor.executeNext()

            val result = listenableFuture.await()
            assertThat(result).isInstanceOf<LoadResult.Invalid<*, *>>()
            assertThat(pagingSource.invalid)
        }

    @Test
    fun prepend_insertInvalidatesPagingSource() =
        setupAndRunWithTestExecutor { db, queryExecutor, _ ->
            val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(
                db = db,
                registerObserver = true
            )
            pagingSource.itemCount.set(100) // bypass check for initial load

            // queue up the append first
            val listenableFuture = pagingSource.prepend(key = 20)
            assertThat(queryExecutor.queuedSize()).isEqualTo(1)

            queryExecutor.executeNext() // start transformAsync
            queryExecutor.executeNext() // start async function
            assertThat(queryExecutor.queuedSize()).isEqualTo(1) // nonInitialLoad is queued up

            // run this async separately from queryExecutor
            run {
                db.dao.addItem(TestItem(101))
            }

            // tasks in queue [nonInitialLoad, InvalidationTracker(from additem)]
            assertThat(queryExecutor.queuedSize()).isEqualTo(2)

            // run nonInitialLoad first. The InvalidationTracker
            // is still queued up. This imitates delayed notification from Room.
            queryExecutor.executeNext()

            val result = listenableFuture.await()
            assertThat(result).isInstanceOf<LoadResult.Invalid<*, *>>()
            assertThat(pagingSource.invalid)
        }

    @Test
    fun test_jumpSupport() = setupAndRun { db ->
        val pagingSource = LimitOffsetListenableFuturePagingSourceImpl(db)
        assertTrue(pagingSource.jumpingSupported)
    }

    @Test
    fun refresh_secondaryConstructor() = setupAndRun { db ->
        val pagingSource = object : LimitOffsetListenableFuturePagingSource<TestItem>(
            db = db,
            supportSQLiteQuery = SimpleSQLiteQuery(
                "SELECT * FROM $tableName ORDER BY id ASC"
            )
        ) {
            override fun convertRows(cursor: Cursor): List<TestItem> {
                return convertRowsHelper(cursor)
            }
        }

        db.dao.addAllItems(ITEMS_LIST)
        val listenableFuture = pagingSource.refresh()

        val page = listenableFuture.await() as LoadResult.Page
        assertThat(page.data).containsExactlyElementsIn(
            ITEMS_LIST.subList(0, 15)
        )
        assertTrue(listenableFuture.isDone)
    }

    @Test
    fun append_secondaryConstructor() = setupAndRun { db ->
        val pagingSource = object : LimitOffsetListenableFuturePagingSource<TestItem>(
            db = db,
            supportSQLiteQuery = SimpleSQLiteQuery(
                "SELECT * FROM $tableName ORDER BY id ASC"
            )
        ) {
            override fun convertRows(cursor: Cursor): List<TestItem> {
                return convertRowsHelper(cursor)
            }
        }

        db.dao.addAllItems(ITEMS_LIST)
        pagingSource.itemCount.set(100)
        val listenableFuture = pagingSource.append(key = 50)

        val page = listenableFuture.await() as LoadResult.Page
        assertThat(page.data).containsExactlyElementsIn(
            ITEMS_LIST.subList(50, 55)
        )
        assertTrue(listenableFuture.isDone)
    }

    @Test
    fun prepend_secondaryConstructor() = setupAndRun { db ->
        val pagingSource = object : LimitOffsetListenableFuturePagingSource<TestItem>(
            db = db,
            supportSQLiteQuery = SimpleSQLiteQuery(
                "SELECT * FROM $tableName ORDER BY id ASC"
            )
        ) {
            override fun convertRows(cursor: Cursor): List<TestItem> {
                return convertRowsHelper(cursor)
            }
        }

        db.dao.addAllItems(ITEMS_LIST)
        pagingSource.itemCount.set(100)
        val listenableFuture = pagingSource.prepend(key = 50)

        val page = listenableFuture.await() as LoadResult.Page
        assertThat(page.data).containsExactlyElementsIn(
            ITEMS_LIST.subList(45, 50)
        )
        assertTrue(listenableFuture.isDone)
    }

    private fun setupAndRun(
        test: suspend (LimitOffsetTestDb) -> Unit
    ) {
        val db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LimitOffsetTestDb::class.java
        ).build()

        runTest {
            test(db)
        }
        tearDown(db)
    }

    private fun setupAndRunWithTestExecutor(
        test: suspend (LimitOffsetTestDb, TestExecutor, TestExecutor) -> Unit
    ) {
        val queryExecutor = TestExecutor()
        val transactionExecutor = TestExecutor()
        val db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LimitOffsetTestDb::class.java
        )
            .setTransactionExecutor(transactionExecutor)
            .setQueryExecutor(queryExecutor)
            .build()

        runTest {
            db.dao.addAllItems(ITEMS_LIST)
            queryExecutor.executeAll() // InvalidationTracker from the addAllItems
          test(db, queryExecutor, transactionExecutor)
        }
        tearDown(db)
    }

    private fun tearDown(db: LimitOffsetTestDb) {
        if (db.isOpen) db.close()
        countingTaskExecutorRule.drainTasks(500, TimeUnit.MILLISECONDS)
        assertThat(countingTaskExecutorRule.isIdle).isTrue()
    }
}

private class LimitOffsetListenableFuturePagingSourceImpl(
    db: RoomDatabase,
    registerObserver: Boolean = false,
    queryString: String = "SELECT * FROM $tableName ORDER BY id ASC",
) : LimitOffsetListenableFuturePagingSource<TestItem>(
    sourceQuery = RoomSQLiteQuery.acquire(
        queryString,
        0
    ),
    db = db,
    tables = arrayOf(tableName)
) {

   init {
       // bypass register check and avoid registering observer
       if (!registerObserver) {
           privateObserver().privateRegisteredState().set(true)
       }
   }

    override fun convertRows(cursor: Cursor): List<TestItem> {
        return convertRowsHelper(cursor)
    }
}

private fun convertRowsHelper(cursor: Cursor): List<TestItem> {
    val cursorIndexOfId = getColumnIndexOrThrow(cursor, "id")
    val data = mutableListOf<TestItem>()
    while (cursor.moveToNext()) {
        val tmpId = cursor.getInt(cursorIndexOfId)
        data.add(TestItem(tmpId))
    }
    return data
}

@Suppress("UNCHECKED_CAST")
private fun TestExecutor.executeNext() {
    val tasks = javaClass.getDeclaredField("mTasks").let {
        it.isAccessible = true
        it.get(this)
    } as LinkedList<Runnable>

    if (!tasks.isEmpty()) {
        val task = tasks.poll()
        task?.run()
    }
}

@Suppress("UNCHECKED_CAST")
private fun TestExecutor.queuedSize(): Int {
    val tasks = javaClass.getDeclaredField("mTasks").let {
        it.isAccessible = true
        it.get(this)
    } as LinkedList<Runnable>

    return tasks.size
}

@Suppress("UNCHECKED_CAST")
private fun ThreadSafeInvalidationObserver.privateRegisteredState(): AtomicBoolean {
    return ThreadSafeInvalidationObserver::class.java
        .getDeclaredField("registered")
        .let {
            it.isAccessible = true
            it.get(this)
        } as AtomicBoolean
}

@Suppress("UNCHECKED_CAST")
private fun LimitOffsetListenableFuturePagingSource<TestItem>.privateObserver():
    ThreadSafeInvalidationObserver {
    return LimitOffsetListenableFuturePagingSource::class.java
        .getDeclaredField("observer")
        .let {
            it.isAccessible = true
            it.get(this)
        } as ThreadSafeInvalidationObserver
}

private fun LimitOffsetListenableFuturePagingSource<TestItem>.refresh(
    key: Int? = null,
): ListenableFuture<LoadResult<Int, TestItem>> {
    return loadFuture(
        createLoadParam(
            loadType = LoadType.REFRESH,
            key = key,
        )
    )
}

private fun LimitOffsetListenableFuturePagingSource<TestItem>.append(
    key: Int? = -1,
): ListenableFuture<LoadResult<Int, TestItem>> {
    return loadFuture(
        createLoadParam(
            loadType = LoadType.APPEND,
            key = key,
        )
    )
}

private fun LimitOffsetListenableFuturePagingSource<TestItem>.prepend(
    key: Int? = -1,
): ListenableFuture<LoadResult<Int, TestItem>> {
    return loadFuture(
        createLoadParam(
            loadType = LoadType.PREPEND,
            key = key,
        )
    )
}

private val CONFIG = PagingConfig(
    pageSize = 5,
    enablePlaceholders = true,
    initialLoadSize = 15
)

private val ITEMS_LIST = createItemsForDb(0, 100)

private fun createItemsForDb(startId: Int, count: Int): List<TestItem> {
    return List(count) {
        TestItem(
            id = it + startId,
        )
    }
}

private fun createLoadParam(
    loadType: LoadType,
    key: Int? = null,
    initialLoadSize: Int = CONFIG.initialLoadSize,
    pageSize: Int = CONFIG.pageSize,
    placeholdersEnabled: Boolean = CONFIG.enablePlaceholders
): PagingSource.LoadParams<Int> {
    return when (loadType) {
        LoadType.REFRESH -> {
            PagingSource.LoadParams.Refresh(
                key = key,
                loadSize = initialLoadSize,
                placeholdersEnabled = placeholdersEnabled
            )
        }
        LoadType.APPEND -> {
            PagingSource.LoadParams.Append(
                key = key ?: -1,
                loadSize = pageSize,
                placeholdersEnabled = placeholdersEnabled
            )
        }
        LoadType.PREPEND -> {
            PagingSource.LoadParams.Prepend(
                key = key ?: -1,
                loadSize = pageSize,
                placeholdersEnabled = placeholdersEnabled
            )
        }
    }
}

private fun ListenableFuture<LoadResult<Int, TestItem>>.onSuccess(
    executor: Executor,
    onSuccessCallback: (LoadResult<Int, TestItem>?) -> Unit,
) {
    addCallback(
        this,
        object : FutureCallback<LoadResult<Int, TestItem>> {
            override fun onSuccess(result: LoadResult<Int, TestItem>?) {
                onSuccessCallback(result)
            }

            override fun onFailure(t: Throwable) {
                assertWithMessage("Expected onSuccess callback instead of onFailure, " +
                    "received ${t.localizedMessage}").fail()
            }
        },
        executor
    )
}

private fun ListenableFuture<LoadResult<Int, TestItem>>.onFailure(
    executor: Executor,
    onFailureCallback: (Throwable) -> Unit,
) {
    addCallback(
        this,
        object : FutureCallback<LoadResult<Int, TestItem>> {
            override fun onSuccess(result: LoadResult<Int, TestItem>?) {
                assertWithMessage("Expected onFailure callback instead of onSuccess, " +
                    "received result $result").fail()
            }

            override fun onFailure(t: Throwable) {
                onFailureCallback(t)
            }
        },
        executor
    )
}

@Database(entities = [TestItem::class], version = 1, exportSchema = false)
abstract class LimitOffsetTestDb : RoomDatabase() {
    abstract val dao: TestItemDao
}

@Entity(tableName = "TestItem")
data class TestItem(
    @PrimaryKey val id: Int,
    val value: String = "item $id"
)

@Dao
interface TestItemDao {
    @Insert
    fun addAllItems(testItems: List<TestItem>)

    @Insert
    fun addItem(testItem: TestItem)
}
