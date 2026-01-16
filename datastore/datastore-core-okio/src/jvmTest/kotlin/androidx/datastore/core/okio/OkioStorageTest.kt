/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.datastore.OkioTestIO
import androidx.datastore.TestingOkioSerializer
import androidx.datastore.TestingSerializerConfig
import androidx.datastore.core.ReadScope
import androidx.datastore.core.Storage
import androidx.datastore.core.StorageConnection
import androidx.datastore.core.WriteScope
import androidx.datastore.core.readData
import androidx.datastore.core.use
import androidx.datastore.core.writeData
import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import okio.FileSystem
import okio.IOException
import okio.Path

@OptIn(ExperimentalCoroutinesApi::class)
class OkioStorageTest {

    private lateinit var testPath: Path
    private lateinit var testingSerializerConfig: TestingSerializerConfig
    private lateinit var testingSerializer: TestingOkioSerializer
    private lateinit var testStorage: Storage<Byte>
    private lateinit var testConnection: StorageConnection<Byte>
    private lateinit var testScope: TestScope
    private lateinit var testIO: OkioTestIO
    private var fileSystem: FileSystem = FileSystem.SYSTEM

    @BeforeTest
    fun setUp() {
        testIO = OkioTestIO()
        testingSerializerConfig = TestingSerializerConfig()
        testingSerializer = TestingOkioSerializer(testingSerializerConfig)
        testPath = testIO.newTempFile().path
        testScope = TestScope(UnconfinedTestDispatcher())
        testStorage = OkioStorage(fileSystem, testingSerializer) { testPath }
        testConnection = testStorage.createConnection()
    }

    @Test
    fun readEmpty() =
        testScope.runTest {
            val data = testConnection.readData()

            assertThat(data).isEqualTo(0.toByte())
        }

    @Test
    fun readAfterDisposeFails() =
        testScope.runTest {
            testConnection.writeScope { writeData(1) }
            testConnection.close()

            assertThrows<IllegalStateException> { testConnection.readData() }
        }

    @Test
    fun writeAfterDisposeFails() =
        testScope.runTest {
            testConnection.writeScope { writeData(1) }
            testConnection.close()

            assertThrows<IllegalStateException> { testConnection.writeScope { writeData(1) } }
        }

    @Test
    fun multipleOpensFail() =
        testScope.runTest {
            val block1WriteDone = CompletableDeferred<Unit>()
            val block1BeforeClose = CompletableDeferred<Unit>()
            val block2BeforeCreate = CompletableDeferred<Unit>()

            val block1 = async {
                testConnection.writeData(1)
                block1WriteDone.complete(Unit)
                block1BeforeClose.await()
                testConnection.close()
            }

            val block2 = async {
                block2BeforeCreate.await()
                assertThrows<IllegalStateException> { testStorage.createConnection() }
                block1.await()
                assertThat(testStorage.createConnection().readData()).isEqualTo(1)
            }

            block1WriteDone.await()
            block2BeforeCreate.complete(Unit)
            block1BeforeClose.complete(Unit)

            block1.await()
            block2.await()
        }

    @Test
    fun blockWithNoWriteSucceeds() =
        testScope.runTest {
            val count = AtomicInt(0)

            testConnection.writeScope {
                // do no writes in here
                count.incrementAndGet()
            }

            assertThat(count.get()).isEqualTo(1)
        }

    @Test
    fun readWrite() =
        testScope.runTest {
            val expected: Byte = 1

            testConnection.writeData(1)
            val data = testConnection.readData()

            assertThat(data).isEqualTo(expected)
        }

    @Test
    fun getFilename() =
        testScope.runTest {
            testConnection.writeData(1)

            assertThat(fileSystem.exists(testPath)).isTrue()
        }

    @Test
    fun readWriteTwice() =
        testScope.runTest {
            val expected: Byte = 1
            val expected2: Byte = 2

            testConnection.writeData(expected)
            val data1 = testConnection.readData()
            testConnection.writeData(expected2)
            val data2 = testConnection.readData()

            assertThat(data1).isEqualTo(expected)
            assertThat(data2).isEqualTo(expected2)
        }

    @Test
    fun readAfterDelete() =
        testScope.runTest {
            testConnection.writeData(1)
            val beforeDelete = testConnection.readData()

            fileSystem.delete(testPath)

            val afterDelete = testConnection.readData()

            assertThat(beforeDelete).isEqualTo(1)
            assertThat(afterDelete).isEqualTo(0)
        }

    @Test
    fun readAfterTransientBadWrite() =
        testScope.runTest {
            coroutineScope {
                testConnection.writeData(1)
                testingSerializerConfig.failingWrite = true
                assertThrows<IOException> { testConnection.writeData(1) }
            }

            coroutineScope { assertThat(testConnection.readData()).isEqualTo(1) }
        }

    @Test
    fun leakedReadTransactionDoesntWork() =
        testScope.runTest {
            var scope: ReadScope<Byte>? = null
            testConnection.readScope {
                readData()
                scope = this
            }
            assertThrows<IllegalStateException> { scope!!.readData() }
        }

    @Test
    fun leakedWriteTransactionDoesntWork() =
        testScope.runTest {
            var scope: WriteScope<Byte>? = null
            testConnection.writeScope {
                writeData(1)
                scope = this
            }
            assertThrows<IllegalStateException> { scope!!.writeData(1) }
        }

    @Test
    fun onlyOneWriter() =
        testScope.runTest {
            val hook1 = CompletableDeferred<Unit>()
            val count = AtomicInt(0)

            val async1 = async {
                hook1.await()
                testConnection.writeScope { assertThat(count.incrementAndGet()).isEqualTo(3) }
            }
            val async2 = async {
                testConnection.writeScope {
                    hook1.complete(Unit)
                    assertThat(count.incrementAndGet()).isEqualTo(1)
                    yield()
                    assertThat(count.incrementAndGet()).isEqualTo(2)
                }
            }

            async1.await()
            async2.await()
        }

    // TODO:(b/234153817) Add remaining readLock tests.  Refactor is making this hard currently

    @Test
    fun noWriteDuringRead() =
        testScope.runTest {
            val hook1 = CompletableDeferred<Unit>()
            val count = AtomicInt(0)

            val async1 = async {
                hook1.await()
                testConnection.writeScope { assertThat(count.incrementAndGet()).isEqualTo(3) }
            }
            val async2 = async {
                testConnection.readScope {
                    hook1.complete(Unit)
                    assertThat(count.incrementAndGet()).isEqualTo(1)
                    yield()
                    assertThat(count.incrementAndGet()).isEqualTo(2)
                }
            }

            async1.await()
            async2.await()
        }

    @Test
    fun readFromTwoStorage() =
        testScope.runTest {
            testConnection.writeData(1)

            coroutineScope {
                assertThat(testConnection.readData()).isEqualTo(1)
                testConnection.close()
            }

            coroutineScope {
                val storage = OkioStorage(fileSystem, testingSerializer) { testPath }
                assertThat(storage.createConnection().readData()).isEqualTo(1)
            }
        }

    @Test
    fun testWriteToNonExistentDir() =
        testScope.runTest {
            val fileInNonExistentDir =
                FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "this/does/not/exist/foo.tst"
            coroutineScope {
                val storage = OkioStorage(fileSystem, testingSerializer) { fileInNonExistentDir }
                storage.createConnection().use { connection ->
                    connection.writeData(1)
                    assertThat(connection.readData()).isEqualTo(1)
                }
            }

            coroutineScope {
                val storage = OkioStorage(fileSystem, testingSerializer) { fileInNonExistentDir }
                storage.createConnection().use { connection ->
                    assertThat(connection.readData()).isEqualTo(1)
                }
            }
        }

    @Test
    fun writeToDirFails() =
        testScope.runTest {
            val directoryFile = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "this/is/a/directory"
            fileSystem.createDirectories(directoryFile)

            val storage = OkioStorage(fileSystem, testingSerializer) { directoryFile }
            val connection = storage.createConnection()
            assertThrows<IOException> { connection.readData() }
        }

    @Test
    fun exceptionWhenCreatingFilePropagates() =
        testScope.runTest {
            var failFileProducer = true

            val fileProducer = {
                if (failFileProducer) {
                    throw IOException("Exception when producing file")
                }
                FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "new-temp-file.tmp"
            }
            val storage = OkioStorage(fileSystem, testingSerializer, producePath = fileProducer)

            assertThrows<IOException> { storage.createConnection().use { it.readData() } }
                .hasMessageThat()
                .isEqualTo("Exception when producing file")

            failFileProducer = false

            assertThat(storage.createConnection().readData()).isEqualTo(0)
        }

    @Test
    fun writeAfterTransientBadRead() =
        testScope.runTest {
            testingSerializerConfig.failingRead = true

            testConnection.writeData(1)

            assertThrows<IOException> { testConnection.readData() }

            testingSerializerConfig.failingRead = false

            testConnection.writeData(1)
            assertThat(testConnection.readData()).isEqualTo(1)
        }

    @Test
    fun handleConcurrentNewDatastores() =
        testScope.runTest {
            val fakeFileSystem = OkioFakeFileSystem(fileSystem)
            testIO = OkioTestIO(fakeFileSystem)
            val fakeTestPath = testIO.newTempFile().path
            val fakeTestStorage = OkioStorage(fakeFileSystem, testingSerializer) { fakeTestPath }
            val fakeTestConnection = fakeTestStorage.createConnection()
            fakeTestConnection.writeData(5.toByte())

            // Succeeds without throwing "FileNotFoundException", confirm we read the value written.
            assertThat(fakeTestConnection.readData()).isEqualTo(5.toByte())
        }
}
