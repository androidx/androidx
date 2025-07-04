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
import androidx.room.Room
import androidx.room.integration.kotlintestapp.TestDatabase
import androidx.room.integration.kotlintestapp.vo.Book
import androidx.room.useWriterConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlin.concurrent.thread
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class InterruptionTest {

    @Test
    fun noInterruptionException() = runBlocking {
        val database =
            Room.inMemoryDatabaseBuilder<TestDatabase>(ApplicationProvider.getApplicationContext())
                .setQueryCoroutineContext(Dispatchers.IO)
                .setDriver(BundledSQLiteDriver())
                .build()

        val connectionAcquired = CompletableDeferred<Unit>()
        val connectionLock = CompletableDeferred<Unit>()

        // Launch a coroutine to grab the single connection
        val job =
            launch(Dispatchers.IO) {
                database.useWriterConnection {
                    connectionAcquired.complete(Unit)
                    connectionLock.await()
                }
            }
        connectionAcquired.await()

        // Start a thread that will try to do a query, but needs to wait for the connection being
        // hold by the coroutine to be free
        var result: List<Book>? = null
        val t = thread(start = true) { result = database.booksDao().getAllBooks() }

        // let the DAO query start and then interrupt the thread
        // no InterruptionException should be thrown
        delay(100)
        t.interrupt()

        // free the connection to allow blocking DAO query to continue
        connectionLock.complete(Unit)
        job.join()
        t.join()

        assertThat(result).isNotNull()
    }
}
