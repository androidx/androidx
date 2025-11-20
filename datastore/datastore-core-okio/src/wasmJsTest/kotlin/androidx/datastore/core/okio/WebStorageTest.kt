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
import androidx.datastore.core.StorageConnection
import androidx.datastore.core.readData
import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.browser.sessionStorage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.serializer

// TODO(b/441511612): Add testing once LocalStorage and OPFS are supported.
class WebStorageTest {
    private val testSessionStorageName = "test_session_storage"
    private lateinit var testingSerializer: WebSerializer<Byte>
    private lateinit var testStorage: Storage<Byte>
    private lateinit var testConnection: StorageConnection<Byte>
    private lateinit var testScope: TestScope

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        val default: Byte = 0
        testingSerializer = WebSerializer(Byte.serializer(), default)
        testStorage =
            WebStorage(
                name = testSessionStorageName,
                serializer = testingSerializer,
                storageType = WebStorageType.SESSION,
            )
        testConnection = testStorage.createConnection()
        testScope = TestScope(UnconfinedTestDispatcher())
    }

    private val testLocalStorageName = "test_local_storage"

    @AfterTest
    fun tearDown() {
        sessionStorage.removeItem(testSessionStorageName)
    }

    @Test
    fun readEmpty() =
        testScope.runTest {
            val data = testConnection.readData()

            assertThat(data).isEqualTo(0)
        }

    @Test
    fun readAfterDisposeFails() =
        testScope.runTest {
            testConnection.writeScope { writeData(1) }
            testConnection.close()

            assertThrows<IllegalStateException> { testConnection.readData() }
                .hasMessageThat()
                .isEqualTo("StorageConnection has already been disposed.")
        }

    @Test
    fun writeAfterDisposeFails() =
        testScope.runTest {
            testConnection.writeScope { writeData(1) }
            testConnection.close()

            assertThrows<IllegalStateException> { testConnection.writeScope { writeData(1) } }
                .hasMessageThat()
                .isEqualTo("StorageConnection has already been disposed.")
        }

    @Test
    fun blockWithNoWriteSucceeds() =
        testScope.runTest {
            val count = AtomicInt(0)

            testConnection.writeScope { count.incrementAndGet() }

            assertThat(count.get()).isEqualTo(1)
        }

    @Test
    fun testSessionStorage_writeThenRead() = runTest {
        val dataStore = DataStoreFactory.create(testStorage)
        val dataToWrite: Byte = 123
        dataStore.updateData { dataToWrite }

        val readData = dataStore.data.first()
        assertEquals(dataToWrite, readData)
    }

    // TODO: Add test CorruptionException case
}
