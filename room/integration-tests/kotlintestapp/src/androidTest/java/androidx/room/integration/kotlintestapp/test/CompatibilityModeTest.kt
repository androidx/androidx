/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import androidx.room.deferredTransaction
import androidx.room.immediateTransaction
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import androidx.room.withTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.coroutineContext
import kotlin.test.Ignore
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.junit.runner.RunWith

/*
 * Tests related to Room driver API usages while in compatibility mode (no driver configured)
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
class CompatibilityModeTest : TestDatabaseTest() {

    @Test
    fun transaction_useConnection() = runTest {
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                usePrepared("INSERT INTO publisher (publisherId, name) VALUES (?, ?)") { stmt ->
                    stmt.bindText(1, "p1")
                    stmt.bindText(2, "pub1")
                    stmt.step()
                }
            }
        }
        assertThat(database.booksDao().getPublishersSuspend()).hasSize(1)
    }

    @Test
    fun transaction_useConnection_yield() = runTest {
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                yield() // to likely resume on another thread
                usePrepared("INSERT INTO publisher (publisherId, name) VALUES (?, ?)") { stmt ->
                    stmt.bindText(1, "p1")
                    stmt.bindText(2, "pub1")
                    stmt.step()
                }
            }
        }
        assertThat(database.booksDao().getPublishersSuspend()).hasSize(1)
    }

    @Test
    fun transaction_useConnection_withContext() = runTest {
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                val ctx = newSingleThreadContext("TestThread")
                withContext(ctx) {
                    usePrepared("INSERT INTO publisher (publisherId, name) VALUES (?, ?)") { stmt ->
                        stmt.bindText(1, "p1")
                        stmt.bindText(2, "pub1")
                        stmt.step()
                    }
                }
                ctx.close()
            }
        }
        assertThat(database.booksDao().getPublishersSuspend()).hasSize(1)
    }

    @Test
    fun transaction_reUseConnection() = runTest {
        database.useWriterConnection { transactor ->
            assertThat(transactor.inTransaction()).isFalse()

            transactor.immediateTransaction {
                assertThat(transactor.inTransaction()).isTrue()

                database.useWriterConnection { reusedConnection ->
                    assertThat(reusedConnection.inTransaction()).isTrue()

                    reusedConnection.usePrepared(
                        "INSERT INTO publisher (publisherId, name) VALUES (?, ?)"
                    ) { stmt ->
                        stmt.bindText(1, "p1")
                        stmt.bindText(2, "pub1")
                        stmt.step()
                    }
                }
            }
        }
        assertThat(database.booksDao().getPublishersSuspend()).hasSize(1)
    }

    @Test
    fun transaction_reUseConnection_withContext() = runTest {
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                val ctx = newSingleThreadContext("TestThread")
                withContext(ctx) {
                    database.useWriterConnection {
                        usePrepared("INSERT INTO publisher (publisherId, name) VALUES (?, ?)") {
                            stmt ->
                            stmt.bindText(1, "p1")
                            stmt.bindText(2, "pub1")
                            stmt.step()
                        }
                    }
                }
                ctx.close()
            }
        }
        assertThat(database.booksDao().getPublishersSuspend()).hasSize(1)
    }

    @Test
    fun transaction_daoFunction() = runTest {
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                database.booksDao().insertPublisherSuspend("p1", "pub1")
            }
        }
        assertThat(database.booksDao().getPublishersSuspend()).hasSize(1)
    }

    @Test
    fun transaction_daoFunction_readOnly() = runTest {
        database.booksDao().insertPublisherSuspend("p1", "pub1")
        database.useWriterConnection { transactor ->
            transactor.deferredTransaction {
                assertThat(database.booksDao().getPublishersSuspend()).hasSize(1)
            }
        }
    }

    @Test
    fun transaction_daoFunction_yield() = runTest {
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                yield() // to likely resume on another thread
                database.booksDao().insertPublisherSuspend("p1", "pub1")
            }
        }
        assertThat(database.booksDao().getPublishersSuspend()).hasSize(1)
    }

    @Test
    fun transaction_daoFunction_withContext() = runTest {
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                val ctx = newSingleThreadContext("TestThread")
                withContext(ctx) { database.booksDao().insertPublisherSuspend("p1", "pub1") }
                ctx.close()
            }
        }
        assertThat(database.booksDao().getPublishersSuspend()).hasSize(1)
    }

    @Test
    fun transaction_daoFunction_blocking() = runTest {
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                // Ok, because we are in transaction context
                database.booksDao().insertPublisher("p1", "pub1")
            }
        }
    }

    @Test
    fun transaction_daoFunction_yield_blocking() = runTest {
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                yield() // to likely resume on another thread
                // Ok, because we are in transaction context
                database.booksDao().insertPublisher("p1", "pub1")
            }
        }
        assertThat(database.booksDao().getPublishersSuspend()).hasSize(1)
    }

    @Test
    fun transaction_daoFunction_withContext_blocking() = runTest {
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                val ctx = newSingleThreadContext("TestThread")
                withContext(ctx) {
                    // Disallow blocking DAO functions on a non transaction context
                    assertThrows<IllegalStateException> {
                            database.booksDao().insertPublisher("p1", "pub1")
                        }
                        .hasMessageThat()
                        .contains("Cannot access database on a different coroutine context")
                }
                ctx.close()
            }
        }
    }

    @Test
    fun transaction_daoFunction_nestedTransaction() = runTest {
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                database.booksDao().executeTransactionSuspending {
                    database.booksDao().insertPublisherSuspend("p1", "pub1")
                }
            }
        }
        assertThat(database.booksDao().getPublishersSuspend()).hasSize(1)
    }

    @Test
    fun transaction_daoFunction_nestedTransaction_blocking() = runTest {
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                database.booksDao().executeTransaction {
                    database.booksDao().insertPublisher("p1", "pub1")
                }
            }
        }
        assertThat(database.booksDao().getPublishersSuspend()).hasSize(1)
    }

    @Test
    fun transaction_extensionFunction_nestedTransaction() = runTest {
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                database.withTransaction { database.booksDao().insertPublisher("p1", "pub1") }
            }
        }
        assertThat(database.booksDao().getPublishersSuspend()).hasSize(1)
    }

    @Test
    fun transaction_extensionFunction_nestedTransaction_withContext() = runTest {
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                val ctx = newSingleThreadContext("TestThread")
                withContext(ctx) {
                    database.withTransaction { database.booksDao().insertPublisher("p1", "pub1") }
                }
                ctx.close()
            }
        }
        assertThat(database.booksDao().getPublishersSuspend()).hasSize(1)
    }

    @Test
    fun transaction_extensionFunction_nestedTransaction_reverse() = runTest {
        database.withTransaction {
            database.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    database.booksDao().insertPublisher("p1", "pub1")
                }
            }
        }
        assertThat(database.booksDao().getPublishersSuspend()).hasSize(1)
    }

    @Test
    fun transaction_extensionFunction_nestedTransaction_reverse_withContext() = runTest {
        database.withTransaction {
            val ctx = newSingleThreadContext("TestThread")
            withContext(ctx) {
                database.useWriterConnection { transactor ->
                    transactor.immediateTransaction {
                        database.booksDao().insertPublisher("p1", "pub1")
                    }
                }
            }
        }
        assertThat(database.booksDao().getPublishersSuspend()).hasSize(1)
    }

    @Test
    fun transaction_concurrent() = runTest {
        val jobOne =
            launch(Dispatchers.IO) {
                database.useWriterConnection { transactor ->
                    transactor.immediateTransaction {
                        database.booksDao().insertPublisher("p1", "pub1")
                    }
                }
            }
        val jobTwo =
            launch(Dispatchers.IO) {
                database.useWriterConnection { transactor ->
                    transactor.immediateTransaction {
                        database.booksDao().insertPublisher("p2", "pub2")
                    }
                }
            }
        listOf(jobOne, jobTwo).joinAll()
        assertThat(database.booksDao().getPublishersSuspend()).hasSize(2)
    }

    @Test
    @Ignore("b/288918056") // Support read-only transaction in Room with SupportSQLite
    fun transaction_readNotBlockedByWrite() = runTest {
        database.booksDao().insertPublisher("p1", "pub1")

        val readerLatch = CompletableDeferred<Unit>()
        val writerLatch = CompletableDeferred<Unit>()

        val jobOne =
            launch(Dispatchers.IO) {
                database.useWriterConnection { transactor ->
                    transactor.immediateTransaction {
                        readerLatch.complete(Unit)
                        writerLatch.await()
                        database.booksDao().insertPublisher("p2", "pub2")
                    }
                }
            }
        val jobTwo =
            launch(Dispatchers.IO) {
                readerLatch.await()
                database.useReaderConnection { transactor ->
                    transactor.deferredTransaction {
                        assertThat(database.booksDao().getPublishersSuspend()).hasSize(1)
                        writerLatch.complete(Unit)
                    }
                }
            }
        listOf(jobOne, jobTwo).joinAll()
        assertThat(database.booksDao().getPublishersSuspend()).hasSize(2)
    }

    @Test
    fun transaction_dispatcherOptimization() = runTest {
        // Validates that we avoid a dispatcher switch when requesting for a connection being
        // already in a transaction.
        database.useWriterConnection {
            it.immediateTransaction {
                val transactionDispatcher = extractInterceptor()
                database.useWriterConnection {
                    assertThat(transactionDispatcher).isSameInstanceAs(extractInterceptor())
                }
                database.useReaderConnection {
                    assertThat(transactionDispatcher).isSameInstanceAs(extractInterceptor())
                }
            }
        }
    }

    private suspend fun extractInterceptor(): ContinuationInterceptor? {
        return coroutineContext[ContinuationInterceptor]
    }
}
