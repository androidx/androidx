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

import androidx.kruth.assertThat
import androidx.room3.Room
import androidx.room3.integration.kotlintestapp.TestDatabase
import androidx.room3.useReaderConnection
import androidx.room3.useWriterConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import kotlin.coroutines.ContinuationInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@SmallTest
@RunWith(Parameterized::class)
class DispatchersTest(driver: UseDriver) : TestDatabaseTest(driver) {

    private companion object {
        @JvmStatic
        @Parameters(name = "useDriver={0}")
        fun parameters() = arrayOf(UseDriver.ANDROID, UseDriver.BUNDLED)
    }

    /**
     * This test validates that the API useConnection (the restricted one) maintains the caller
     * dispatcher, i.e. that the connection pool does not do dispatchers changes which can be
     * expensive if they do thread hops.
     */
    @Test
    fun restrictedUseConnectionSameDispatcher() = runTest {
        val callerDispatcher = currentCoroutineContext()[ContinuationInterceptor]
        database.useConnection(isReadOnly = false) {
            val useConnectionDispatcher = currentCoroutineContext()[ContinuationInterceptor]
            assertThat(callerDispatcher).isSameInstanceAs(useConnectionDispatcher)
        }
    }

    /**
     * This test validates that useReaderConnection and useWriterConnection will move the coroutine
     * to the Room configured dispatcher. Enabling usages from the main dispatcher to avoid IO on
     * the main thread.
     */
    @Test
    fun publicUseConnectionConfiguredDispatcher() = runTest {
        val database =
            Room.inMemoryDatabaseBuilder<TestDatabase>(ApplicationProvider.getApplicationContext())
                .setDriver(
                    when (useDriver) {
                        UseDriver.ANDROID -> AndroidSQLiteDriver()
                        UseDriver.BUNDLED -> BundledSQLiteDriver()
                    }
                )
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        database.useReaderConnection {
            val useConnectionDispatcher = currentCoroutineContext()[ContinuationInterceptor]
            assertThat(useConnectionDispatcher).isSameInstanceAs(Dispatchers.IO)
        }
        database.useWriterConnection {
            val useConnectionDispatcher = currentCoroutineContext()[ContinuationInterceptor]
            assertThat(useConnectionDispatcher).isSameInstanceAs(Dispatchers.IO)
        }
    }
}
