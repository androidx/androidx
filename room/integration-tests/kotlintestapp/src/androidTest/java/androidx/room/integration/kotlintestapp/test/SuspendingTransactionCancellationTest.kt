/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.room.integration.kotlintestapp.vo.Book
import androidx.room.withTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

// see b/148181325
@RunWith(AndroidJUnit4::class)
class SuspendingTransactionCancellationTest : TestDatabaseTest() {
    @Before
    fun prepareDb() = runBlocking {
        booksDao = database.booksDao()
        booksDao.addAuthorPublisherBooks(
            TestUtil.AUTHOR_1,
            TestUtil.PUBLISHER,
            TestUtil.BOOK_1
        )
    }

    @After
    fun teardown() {
        // At the end of all tests, query executor should be idle (transaction thread released).
        countingTaskExecutorRule.drainTasks(500, TimeUnit.MILLISECONDS)
        assertThat(countingTaskExecutorRule.isIdle).isTrue()
    }

    @Test
    @SmallTest
    fun canceledTransaction() = runBlocking {
        val toBeCancelled = async {
            getBook {
                throw CancellationException("custom-cancel")
            }
        }
        val cancelled = runCatching { toBeCancelled.await() }
        assertThat(cancelled.exceptionOrNull()?.message).isEqualTo("custom-cancel")
        // now should be able to read again
        assertThat(getBook()).isEqualTo(TestUtil.BOOK_1)
    }

    @Test
    @SmallTest
    fun canceledTransaction_viaJob_afterTransactionStarts() = runBlocking {
        val transactionStarted = CompletableDeferred<Unit>()
        val toBeCancelled = launch {
            getBook {
                suspendCancellableCoroutine<Unit> {
                    transactionStarted.complete(Unit)
                    // never ending, will be cancelled
                }
            }
        }
        transactionStarted.await()
        toBeCancelled.cancelAndJoin()
        // now should be able to read again
        assertThat(getBook()).isEqualTo(TestUtil.BOOK_1)
    }

    /**
     * This is a race condition test hence we execute it multiple times :(
     * Unfortunately, there is no way to hook into the exact moment of race.
     */
    @Test
    @MediumTest
    fun canceledTransaction_viaJob_beforeTransactionStarts() = repeat(10) {
        runBlocking(Dispatchers.Main.immediate) {
            val toBeCancelled = launch {
                getBook {
                    suspendCancellableCoroutine<Unit> {
                        // never ending, will be cancelled
                    }
                }
            }

            toBeCancelled.cancel()
            // now should be able to read again
            withTimeout(10_000) {
                assertThat(getBook()).isEqualTo(TestUtil.BOOK_1)
            }
        }
    }

    @Test
    @LargeTest
    fun canceledTransaction_withDelay_large() = repeat(100) {
        canceledTransaction_viaJob_beforeTransactionStarts()
    }

    /**
     * This is a race condition test hence we execute it multiple times :(
     * Unfortunately, there is no way to hook into the exact moment of race.
     */
    @Test
    @MediumTest
    fun canceledTransaction_immediatelyOnTheSameThread() = repeat(10) {
        runBlocking {
            // see b/148181325
            // cancelling a coroutine from the same thread (hence immediately) was deadlocking
            // room ktx transactions.
            val immediateMainScope = CoroutineScope(
                SupervisorJob() + Dispatchers.Main.immediate
            )
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val toBeCancelled = immediateMainScope.launch {
                    getBook {
                        suspendCancellableCoroutine<Unit> {
                            // infinite
                        }
                    }
                }
                toBeCancelled.cancel()
            }
            immediateMainScope.cancel()
            // now should be able to read again
            withTimeout(10_000) {
                assertThat(getBook()).isEqualTo(TestUtil.BOOK_1)
            }
        }
    }

    @Test
    @LargeTest
    fun canceledTransaction_immediatelyOnTheSameThread_large() = repeat(100) {
        canceledTransaction_immediatelyOnTheSameThread()
    }

    /**
     * Reads data from the database but also runs the given code inside the transaction.
     */
    private suspend fun getBook(
        // executed after method starts
        inTransaction: (suspend () -> Unit)? = null
    ): Book {
        return database.withTransaction {
            inTransaction?.invoke()
            booksDao.getBookSuspend(TestUtil.BOOK_1.bookId)
        }
    }
}