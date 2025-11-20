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

package androidx.room3.integration.multiplatformtestapp.test

import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import androidx.room3.immediateTransaction
import androidx.room3.useReaderConnection
import androidx.room3.useWriterConnection
import androidx.sqlite.SQLiteException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield

abstract class BaseCoroutineTest {

    private lateinit var db: SampleDatabase

    abstract fun getRoomDatabase(): SampleDatabase

    @BeforeTest
    open fun before() {
        db = getRoomDatabase()
    }

    @AfterTest
    open fun after() {
        db.close()
    }

    @Test
    fun queriesAreIsolated() = runTest {
        db.dao().insertItem(22)

        // Validates that Room's coroutine scope provides isolation, if one query fails
        // it doesn't affect others.
        val failureQueryScope = CoroutineScope(Job())
        val successQueryScope = CoroutineScope(Job())
        val failureDeferred =
            failureQueryScope.async {
                db.useReaderConnection { connection ->
                    connection.usePrepared("SELECT * FROM WrongTableName") {
                        assertThat(it.step()).isFalse()
                    }
                }
            }
        val successDeferred =
            successQueryScope.async {
                db.useReaderConnection { connection ->
                    connection.usePrepared("SELECT * FROM SampleEntity") {
                        assertThat(it.step()).isTrue()
                        it.getLong(0)
                    }
                }
            }
        assertThrows<SQLiteException> { failureDeferred.await() }
            .hasMessageThat()
            .contains("no such table: WrongTableName")
        assertThat(successDeferred.await()).isEqualTo(22)
    }

    @Test
    fun queriesAreIsolatedWhenCancelled() = runTest {
        // Validates that Room's coroutine scope provides isolation, if scope doing a query is
        // cancelled it doesn't affect others.
        val toBeCancelledScope = CoroutineScope(Job())
        val notCancelledScope = CoroutineScope(Job())
        val latch = Mutex(locked = true)
        val cancelledDeferred =
            toBeCancelledScope.async {
                db.useReaderConnection { latch.withLock {} }
                1
            }
        val notCancelledDeferred =
            notCancelledScope.async {
                db.useReaderConnection { latch.withLock {} }
                1
            }

        yield()
        toBeCancelledScope.cancel()
        latch.unlock()

        assertThrows<CancellationException> { cancelledDeferred.await() }
        assertThat(notCancelledDeferred.await()).isEqualTo(1)
    }

    @Test
    fun concurrentDaoUsageInTransaction() = runTest {
        coroutineScope {
            db.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    // Validates that multiple concurrent DAO operation re-using the same connection
                    // (due to the transaction) operate mutex-ed such that SQLite's multi-thread
                    // restrictions are respected. b/447171815
                    repeat(10) { launch(Dispatchers.IO) { db.dao().insertItem(it.toLong()) } }
                }
            }
        }
        assertThat(db.dao().getItemList()).hasSize(10)
    }
}
