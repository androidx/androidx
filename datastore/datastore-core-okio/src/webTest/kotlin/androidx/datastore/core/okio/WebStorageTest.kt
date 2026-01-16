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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlinx.browser.localStorage
import kotlinx.browser.sessionStorage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.serializer
import okio.BufferedSink
import okio.BufferedSource

// TODO(b/441511612): Add testing once OPFS is supported.
class WebStorageTest {
    private val testSessionStorageName = "test_session_storage"
    private val testLocalStorageName = "test_local_storage"
    private val default: Byte = 0
    private val testingSerializer: WebSerializer<Byte> = WebSerializer(Byte.serializer(), default)
    private lateinit var testSessionStorage: Storage<Byte>
    private lateinit var testLocalStorage: Storage<Byte>
    @OptIn(ExperimentalCoroutinesApi::class)
    private val testScope: TestScope = TestScope(UnconfinedTestDispatcher())

    @BeforeTest
    fun setUp() {
        testSessionStorage =
            WebStorage(
                name = testSessionStorageName,
                serializer = testingSerializer,
                storageType = WebStorageType.SESSION,
            )

        testLocalStorage =
            WebStorage(
                name = testLocalStorageName,
                serializer = testingSerializer,
                storageType = WebStorageType.LOCAL,
            )
    }

    @AfterTest
    fun tearDown() {
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
        val dataStore = DataStoreFactory.create(testSessionStorage)
        val dataToWrite: Byte = 123
        dataStore.updateData { dataToWrite }

        val readData = dataStore.data.first()
        assertEquals(dataToWrite, readData)
    }

    @Test
    fun testLocalStorage_writeThenRead() = runTest {
        val dataStore = DataStoreFactory.create(testLocalStorage)
        val dataToWrite: Byte = 123
        dataStore.updateData { dataToWrite }

        val readData = dataStore.data.first()
        assertEquals(dataToWrite, readData)
    }

    @Test
    fun binaryData_isStoredAndRetrievedCorrectly() = runTest {
        val storeName = "test-binary-store"

        // Serializer that mimics Wire/Protobuf behavior
        @Suppress("MISSING_DEPENDENCY_SUPERCLASS_IN_TYPE_ARGUMENT")
        val rawByteSerializer =
            object : OkioSerializer<ByteArray> {
                override val defaultValue: ByteArray = byteArrayOf()

                override suspend fun readFrom(source: BufferedSource): ByteArray {
                    return source.readByteArray()
                }

                override suspend fun writeTo(t: ByteArray, sink: BufferedSink) {
                    sink.write(t)
                }
            }

        val storage: Storage<ByteArray> =
            WebStorage(
                serializer = rawByteSerializer,
                name = storeName,
                storageType = WebStorageType.SESSION,
            )

        // Binary data with invalid UTF-8 sequences
        val originalData =
            byteArrayOf(
                0x89.toByte(),
                0x50.toByte(),
                0x4e.toByte(),
                0x47.toByte(),
                0xff.toByte(),
                0xd8.toByte(),
                0xff.toByte(),
                0xe0.toByte(),
            )

        val connection = storage.createConnection()
        try {
            connection.writeScope { writeData(originalData) }

            val readData = connection.readScope { readData() }
            // Content should be equal, if not we have corrupted data
            assertContentEquals(originalData, readData)
        } finally {
            connection.close()
            sessionStorage.removeItem(storeName)
        }
    }
}
