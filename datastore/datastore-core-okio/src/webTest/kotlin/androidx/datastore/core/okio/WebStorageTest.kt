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

package androidx.datastore.core.okio

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Storage
import androidx.datastore.core.readData
import androidx.datastore.core.use
import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.atomicfu.atomic
import kotlinx.browser.localStorage
import kotlinx.browser.sessionStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.serializer

private val testFileCounter = atomic(0)

class WebStorageTest {
    private val suiteRunId = Random.nextInt(100000)
    private lateinit var testSessionStorageName: String
    private lateinit var testLocalStorageName: String
    private lateinit var dataStoreScope: CoroutineScope
    private val default: Byte = 0
    private val testingSerializer: WebSerializer<Byte> = WebSerializer(Byte.serializer(), default)
    private lateinit var testSessionStorage: Storage<Byte>
    private lateinit var testLocalStorage: Storage<Byte>
    private lateinit var testScope: TestScope

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        testScope = TestScope(UnconfinedTestDispatcher())
        dataStoreScope = CoroutineScope(Dispatchers.Unconfined + Job())
        val runId = "${suiteRunId}_${testFileCounter.incrementAndGet()}"
        testSessionStorageName = "test_session_storage_$runId"
        testLocalStorageName = "test_local_storage_$runId"

        testSessionStorage =
            WebSessionStorage(name = testSessionStorageName, serializer = testingSerializer)

        testLocalStorage =
            WebLocalStorage(name = testLocalStorageName, serializer = testingSerializer)
    }

    @AfterTest
    fun tearDown() = runTest {
        dataStoreScope.cancel()
        sessionStorage.removeItem(testSessionStorageName)
        localStorage.removeItem(testLocalStorageName)
    }

    @Test
    fun readEmptySessionStorage() =
        testScope.runTest {
            val data = testSessionStorage.createConnection().use { it.readData() }
            assertThat(data).isEqualTo(0)
        }

    @Test
    fun readEmptyLocalStorage() =
        testScope.runTest {
            val data = testLocalStorage.createConnection().use { it.readData() }
            assertThat(data).isEqualTo(0)
        }

    @Test
    fun readAfterDisposeFailsSessionStorage() =
        testScope.runTest {
            testSessionStorage.createConnection().use {
                it.writeScope { writeData(1) }
                it.close()
                assertThrows<IllegalStateException> { it.readData() }
                    .hasMessageThat()
                    .isEqualTo("StorageConnection has already been disposed.")
            }
        }

    @Test
    fun readAfterDisposeFailsLocalStorage() =
        testScope.runTest {
            testLocalStorage.createConnection().use {
                it.writeScope { writeData(1) }
                it.close()
                assertThrows<IllegalStateException> { it.readData() }
                    .hasMessageThat()
                    .isEqualTo("StorageConnection has already been disposed.")
            }
        }

    @Test
    fun writeAfterDisposeFailsSessionStorage() =
        testScope.runTest {
            testSessionStorage.createConnection().use {
                it.writeScope { writeData(1) }
                it.close()
                assertThrows<IllegalStateException> { it.writeScope { writeData(1) } }
                    .hasMessageThat()
                    .isEqualTo("StorageConnection has already been disposed.")
            }
        }

    @Test
    fun writeAfterDisposeFailsLocalStorage() =
        testScope.runTest {
            testLocalStorage.createConnection().use {
                it.writeScope { writeData(1) }
                it.close()
                assertThrows<IllegalStateException> { it.writeScope { writeData(1) } }
                    .hasMessageThat()
                    .isEqualTo("StorageConnection has already been disposed.")
            }
        }

    @Test
    fun blockWithNoWriteSucceedsSessionStorage() =
        testScope.runTest {
            testSessionStorage.createConnection().use {
                val count = AtomicInt(0)
                it.writeScope { count.incrementAndGet() }

                assertThat(count.get()).isEqualTo(1)
            }
        }

    @Test
    fun blockWithNoWriteSucceedsLocalStorage() =
        testScope.runTest {
            testLocalStorage.createConnection().use {
                val count = AtomicInt(0)
                it.writeScope { count.incrementAndGet() }

                assertThat(count.get()).isEqualTo(1)
            }
        }

    @Test
    fun testSessionStorage_writeThenRead() = runTest {
        val dataStore = DataStoreFactory.create(testSessionStorage, scope = dataStoreScope)
        val dataToWrite: Byte = 123
        dataStore.updateData { dataToWrite }

        val readData = dataStore.data.first()
        assertEquals(dataToWrite, readData)
    }

    @Test
    fun testLocalStorage_writeThenRead() = runTest {
        val dataStore = DataStoreFactory.create(testLocalStorage, scope = dataStoreScope)
        val dataToWrite: Byte = 123
        dataStore.updateData { dataToWrite }

        val readData = dataStore.data.first()
        assertEquals(dataToWrite, readData)
    }

    @Test
    fun binaryData_isStoredAndRetrievedCorrectlySessionStorage() = runTest {
        val storeName = "test-binary-store-session"
        runBinaryDataTest(WebSessionStorage(serializer = rawByteSerializer, name = storeName)) {
            sessionStorage.removeItem(storeName)
        }
    }
}
