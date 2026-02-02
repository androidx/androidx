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

package androidx.room3.integration.kotlintestapp.test

import androidx.room3.Room
import androidx.room3.deferredTransaction
import androidx.room3.integration.kotlintestapp.TestDatabase
import androidx.room3.integration.kotlintestapp.test.TestDatabaseTest.UseDriver
import androidx.room3.useReaderConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class ReadOnlyTransactionTest(private val useDriver: UseDriver) {

    private companion object {
        @JvmStatic
        @Parameters(name = "useDriver={0}")
        fun parameters() = UseDriver.entries.toTypedArray()
    }

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private lateinit var database: TestDatabase

    @Before
    fun before() {
        instrumentation.targetContext.deleteDatabase("test.db")

        database =
            Room.databaseBuilder<TestDatabase>(
                    context = ApplicationProvider.getApplicationContext(),
                    name = "test.db",
                )
                .setDriver(
                    when (useDriver) {
                        UseDriver.ANDROID -> AndroidSQLiteDriver()
                        UseDriver.BUNDLED -> BundledSQLiteDriver()
                    }
                )
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
    }

    @After
    fun after() {
        database.close()
    }

    @Test
    fun readTransactionDoesNotBlockWrite() = runTest {
        val writeLatch = CompletableDeferred<Unit>()
        val readLatch = CompletableDeferred<Unit>()
        launch(Dispatchers.IO) {
            database.useReaderConnection { transactor ->
                transactor.deferredTransaction {
                    writeLatch.complete(Unit)
                    readLatch.await()
                }
            }
        }

        writeLatch.await()
        database.booksDao().insertPublisherSuspend("p1", "pub1")
        readLatch.complete(Unit)
    }
}
