/*
 * Copyright 2026 The Android Open Source Project
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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.serializer

private val testFileCounter = atomic(0)

class WebOpfsStorageTest {
    private val suiteRunId = kotlin.random.Random.nextInt(100000)
    private lateinit var testOpfsStorageName: String
    private lateinit var dataStoreScope: kotlinx.coroutines.CoroutineScope
    private val default: Byte = 0
    private val testingSerializer: WebSerializer<Byte> = WebSerializer(Byte.serializer(), default)
    private lateinit var testOpfsStorage: Storage<Byte>
    @OptIn(ExperimentalCoroutinesApi::class) private lateinit var testScope: TestScope

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        testScope = TestScope(UnconfinedTestDispatcher())
        dataStoreScope =
            kotlinx.coroutines.CoroutineScope(
                kotlinx.coroutines.Dispatchers.Unconfined + kotlinx.coroutines.Job()
            )
        val runId = "${suiteRunId}_${testFileCounter.incrementAndGet()}"
        testOpfsStorageName = "test_opfs_storage_$runId"

        testOpfsStorage = WebOpfsStorage(name = testOpfsStorageName, serializer = testingSerializer)
    }

    @AfterTest
    fun tearDown() = runTest {
        dataStoreScope.cancel()
        removeOpfsEntry(testOpfsStorageName)
    }

    @Test
    fun readEmptyOpfsStorage() =
        testScope.runTest {
            val data = testOpfsStorage.createConnection().use { it.readData() }
            assertThat(data).isEqualTo(0)
        }

    @Test
    fun readAfterDisposeFailsOpfsStorage() =
        testScope.runTest {
            testOpfsStorage.createConnection().use {
                it.writeScope { writeData(1) }
                it.close()
                assertThrows<IllegalStateException> { it.readData() }
                    .hasMessageThat()
                    .isEqualTo("StorageConnection has already been disposed.")
            }
        }

    @Test
    fun writeAfterDisposeFailsOpfsStorage() =
        testScope.runTest {
            testOpfsStorage.createConnection().use {
                it.writeScope { writeData(1) }
                it.close()
                assertThrows<IllegalStateException> { it.writeScope { writeData(1) } }
                    .hasMessageThat()
                    .isEqualTo("StorageConnection has already been disposed.")
            }
        }

    @Test
    fun blockWithNoWriteSucceedsOpfsStorage() =
        testScope.runTest {
            testOpfsStorage.createConnection().use {
                val count = atomic(0)
                it.writeScope { count.incrementAndGet() }

                assertThat(count.value).isEqualTo(1)
            }
        }

    @Test
    fun testOpfsStorage_writeThenRead() = runTest {
        val dataStore = DataStoreFactory.create(testOpfsStorage, scope = dataStoreScope)
        val dataToWrite: Byte = 123
        dataStore.updateData { dataToWrite }

        val readData = dataStore.data.first()
        assertEquals(dataToWrite, readData)
    }

    @Test
    fun binaryData_isStoredAndRetrievedCorrectlyOpfs() = runTest {
        val storeName = "test-binary-store-opfs_${suiteRunId}_${testFileCounter.incrementAndGet()}"
        runBinaryDataTest(WebOpfsStorage(serializer = rawByteSerializer, name = storeName)) {
            removeOpfsEntry(storeName)
        }
    }
}

expect suspend fun removeOpfsEntry(name: String)
