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

import androidx.kruth.assertThat
import androidx.room.Transactor
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

@LargeTest
class BundledSQLiteConnectionPoolTest : BaseConnectionPoolTest() {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val file = instrumentation.targetContext.getDatabasePath("test.db")

    override val fileName = file.path

    override fun getDriver(): SQLiteDriver {
        return BundledSQLiteDriver()
    }

    @BeforeTest
    fun before() {
        assertThat(file).isNotNull()
        file.parentFile?.mkdirs()
        deleteDatabaseFile()
    }

    @AfterTest
    fun after() {
        deleteDatabaseFile()
    }

    @Test
    fun reusingConnectionOnBlocking() = runTest {
        val driver = setupDriver()
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1
            )
        var count = 0
        withContext(NewThreadDispatcher()) {
            pool.useConnection(isReadOnly = true) { initialConnection ->
                pool.useConnectionBlocking(isReadOnly = true) { reusedConnection ->
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
    fun newThreadDispatcherDoesNotAffectThreadConfinement() = runTest {
        val driver = setupDriver()
        val pool =
            newConnectionPool(
                driver = driver,
                fileName = fileName,
                maxNumOfReaders = 1,
                maxNumOfWriters = 1
            )
        val job = launch(Dispatchers.IO) { pool.useReaderConnection { delay(500) } }
        withContext(NewThreadDispatcher()) {
            pool.useReaderConnection { connection ->
                connection.usePrepared("SELECT COUNT(*) FROM Pet") {
                    assertThat(it.step()).isTrue()
                    assertThat(it.getLong(0)).isEqualTo(20)
                }
                val beforeYieldThreadId = Thread.currentThread().id
                yield()
                // Assert dispatcher resumed on a new thread from the yield
                assertThat(beforeYieldThreadId).isNotEqualTo(Thread.currentThread().id)
                pool.useConnection(isReadOnly = true) { reusedConnection ->
                    reusedConnection.usePrepared("SELECT COUNT(*) FROM Pet") {
                        assertThat(it.step()).isTrue()
                        assertThat(it.getLong(0)).isEqualTo(20)
                    }
                }
            }
        }
        job.join()
        pool.useReaderConnection { connection ->
            connection.usePrepared("SELECT COUNT(*) FROM Pet") {
                assertThat(it.step()).isTrue()
                assertThat(it.getLong(0)).isEqualTo(20)
            }
        }
        pool.close()
    }

    /** A CoroutineDispatcher that dispatches every block into a new thread */
    private class NewThreadDispatcher : CoroutineDispatcher() {
        private val idCounter = atomic(0)

        @OptIn(InternalCoroutinesApi::class)
        override fun dispatchYield(context: CoroutineContext, block: Runnable) {
            super.dispatchYield(context, block)
        }

        override fun isDispatchNeeded(context: CoroutineContext) = true

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            thread(name = "NewThreadDispatcher-${idCounter.incrementAndGet()}") { block.run() }
        }
    }

    private fun deleteDatabaseFile() {
        instrumentation.targetContext.deleteDatabase(file.name)
    }

    private fun <R> ConnectionPool.useConnectionBlocking(
        isReadOnly: Boolean,
        block: suspend (Transactor) -> R
    ): R {
        return runBlocking(Dispatchers.Unconfined) { useConnection(isReadOnly, block) }
    }
}
