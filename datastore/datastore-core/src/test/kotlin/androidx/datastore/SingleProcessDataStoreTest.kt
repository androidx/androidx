/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.datastore

import androidx.datastore.handlers.NoOpCorruptionHandler
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File
import java.io.IOException
import java.lang.IllegalStateException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@kotlinx.coroutines.ExperimentalCoroutinesApi
@kotlinx.coroutines.InternalCoroutinesApi
@kotlinx.coroutines.ObsoleteCoroutinesApi
@kotlinx.coroutines.FlowPreview
@RunWith(JUnit4::class)
class SingleProcessDataStoreTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var store: DataStore<Byte>
    private lateinit var serializer: TestingSerializer
    private lateinit var testFile: File
    private lateinit var dataStoreScope: TestCoroutineScope

    @Before
    fun setUp() {
        serializer = TestingSerializer()
        testFile = tempFolder.newFile("test_file." + serializer.fileExtension)
        dataStoreScope = TestCoroutineScope(TestCoroutineDispatcher() + Job())
        store =
            SingleProcessDataStore<Byte>({ testFile }, serializer, scope = dataStoreScope)
    }

    @After
    fun cleanUp() {
        dataStoreScope.cleanupTestCoroutines()
    }

    @Test
    fun testReadNewMessage() = runBlockingTest {
        assertThat(store.data.first()).isEqualTo(0)
    }

    @Test
    fun testReadWithNewInstance() = runBlockingTest {
        store.updateData { 1 }
        val newStore = newDataStore(testFile)
        assertThat(newStore.data.first()).isEqualTo(1)
    }

    @Test
    fun testReadUnreadableFile() = runBlockingTest {
        testFile.setReadable(false)
        val result = runCatching {
            store.data.first()
        }

        assertThat(result.exceptionOrNull()).isInstanceOf(IOException::class.java)
        assertThat(result.exceptionOrNull()).hasMessageThat().contains("Permission denied")
    }

    @Test
    fun testReadAfterTransientBadRead() = runBlockingTest {
        testFile.setReadable(false)

        assertThrows<IOException> { store.data.first() }.hasMessageThat()
            .contains("Permission denied")

        testFile.setReadable(true)
        assertThat(store.data.first()).isEqualTo(0)
    }

    @Test
    fun testScopeCancelledWithActiveFlow() = runBlockingTest {
        val collection = async {
            store.data.take(2).collect {
                // Do nothing, this will wait on another element which will never arrive
            }
        }

        dataStoreScope.cancel()

        assertThat(collection.isCompleted).isTrue()
        assertThat(collection.isActive).isFalse()
    }

    @Test
    fun testWriteAndRead() = runBlockingTest {
        store.updateData { 1 }
        assertThat(store.data.first()).isEqualTo(1)
    }

    @Test
    fun testWritesDontBlockReadsInSameProcess() = runBlockingTest {
        val transformStarted = CompletableDeferred<Unit>()
        val continueTransform = CompletableDeferred<Unit>()

        val slowUpdate = async {
            store.updateData {
                transformStarted.complete(Unit)
                continueTransform.await()
                it.inc()
            }
        }

        // Wait for the transform to begin.
        transformStarted.await()

        // Read is not blocked.
        assertThat(store.data.first()).isEqualTo(0)

        continueTransform.complete(Unit)
        slowUpdate.await()

        // After update completes, update runs, and read shows new data.
        assertThat(store.data.first()).isEqualTo(1)
    }

    @Test
    fun testWriteMultiple() = runBlockingTest {
        store.updateData { 2 }
        store.updateData { it.dec() }

        assertThat(store.data.first()).isEqualTo(1)
    }

    @Test
    fun testReadAfterTransientBadWrite() = runBlockingTest {
        store.updateData { 1 }
        serializer.failingWrite = true

        assertThrows<IOException> { store.updateData { 2 } }

        val newStore = newDataStore(testFile)
        assertThat(newStore.data.first()).isEqualTo(1)
    }

    @Test
    fun testWriteToNonExistentDir() = runBlockingTest {
        val fileInNonExistentDir =
            File(tempFolder.newFolder(), "/this/does/not/exist/foo." + serializer.fileExtension)
        var newStore = newDataStore(fileInNonExistentDir)

        newStore.updateData { 1 }

        assertThat(newStore.data.first()).isEqualTo(1)

        newStore = newDataStore(fileInNonExistentDir)
        assertThat(newStore.data.first()).isEqualTo(1)
    }

    @Test
    fun testWriteToDirFails() = runBlockingTest {
        val directoryFile =
            File(tempFolder.newFolder(), "/this/is/a/directory." + serializer.fileExtension)
        directoryFile.mkdirs()
        assertThat(directoryFile.isDirectory)

        val newStore = newDataStore(directoryFile)
        assertThrows<IOException> { newStore.data.first() }
    }

    @Test
    fun testIncorrectFileExtension() = runBlockingTest {
        val badExtensionFile =
            File(tempFolder.newFile().absolutePath + "/file/with.incorrect_extension")

        val newStore = newDataStore(badExtensionFile)
        assertThrows<IllegalStateException> { newStore.data.first() }.hasMessageThat()
            .contains("does not match required extension for serializer")
        assertThrows<IllegalStateException> { newStore.updateData { 1 } }.hasMessageThat()
            .contains("does not match required extension for serializer")
    }

    @Test
    fun testExceptionWhenCreatingFilePropagates() = runBlockingTest {
        var failFileProducer = true

        val fileProducer = {
            if (failFileProducer) {
                throw IOException("Exception when producing file")
            }
            testFile
        }

        val newStore = SingleProcessDataStore(
            fileProducer,
            serializer = serializer,
            scope = dataStoreScope,
            initTasksList = listOf()
        )

        assertThrows<IOException> { newStore.data.first() }.hasMessageThat().isEqualTo(
            "Exception when producing file"
        )

        failFileProducer = false

        assertThat(newStore.data.first()).isEqualTo(0)
    }

    @Test
    fun testWriteTransformCancellation() = runBlockingTest {
        val transform = CompletableDeferred<Byte>()

        val write = async { store.updateData { transform.await() } }

        assertThat(write.isCompleted).isFalse()

        transform.cancel()

        assertThrows<CancellationException> { write.await() }
    }

    @Test
    fun testWriteAfterTransientBadRead() = runBlockingTest {
        serializer.failingRead = true

        assertThrows<IOException> { store.data.first() }

        serializer.failingRead = false

        store.updateData { 1 }
        assertThat(store.data.first()).isEqualTo(1)
    }

    @Test
    fun testWriteWithBadReadFails() = runBlockingTest {
        serializer.failingRead = true

        assertThrows<IOException> { store.updateData { 1 } }
    }

    @Test
    fun testCancellingScopePropagatesToWrites() = runBlockingTest {
        val latch = CompletableDeferred<Unit>()

        val slowUpdate = async {
            store.updateData {
                latch.await()
                it.inc()
            }
        }

        val notStartedUpdate = async {
            store.updateData {
                it.inc()
            }
        }

        dataStoreScope.cancel()

        assertThrows<CancellationException> { slowUpdate.await() }
        assertThrows<CancellationException> { notStartedUpdate.await() }
    }

    @Test
    fun testCanWriteFromInitTask() = runBlockingTest {
        store = newDataStore(initTasksList = listOf { api -> api.updateData { 1 } })

        assertThat(store.data.first()).isEqualTo(1)
    }

    @Test
    fun testInitTaskFailsFirstTimeDueToReadFail() = runBlockingTest {
        store = newDataStore(initTasksList = listOf { api -> api.updateData { 1 } })

        serializer.failingRead = true
        assertThrows<IOException> { store.updateData { 2 } }

        serializer.failingRead = false
        store.updateData { it.inc().inc() }

        assertThat(store.data.first()).isEqualTo(3)
    }

    @Test
    fun testInitTaskFailsFirstTimeDueToException() = runBlockingTest {
        val failInit = AtomicBoolean(true)
        store = newDataStore(
            initTasksList = listOf { _ ->
                if (failInit.get()) {
                    throw IOException("I was asked to fail init")
                }
            }
        )
        assertThrows<IOException> { store.updateData { 5 } }

        failInit.set(false)

        store.updateData { it.inc() }
        assertThat(store.data.first()).isEqualTo(1)
    }

    @Test
    fun testInitTaskOnlyRunsOnce() = runBlockingTest {
        store.updateData { 1 }

        val count = AtomicInteger()
        val newStore = newDataStore(testFile,
            initTasksList = listOf { _ ->
                count.incrementAndGet()
            }
        )

        repeat(10) {
            newStore.updateData { it.inc() }
            newStore.data.first()
        }

        assertThat(count.get()).isEqualTo(1)
    }

    @Test
    fun testWriteDuringInit() = runBlockingTest {
        val continueInit = CompletableDeferred<Unit>()

        store = newDataStore(
            initTasksList = listOf { api ->
                continueInit.await()
                api.updateData { 1 }
            })

        val update = async {
            store.updateData { b ->
                assertThat(b).isEqualTo(1)
                b
            }
        }

        continueInit.complete(Unit)
        update.await()

        assertThat(store.data.first()).isEqualTo(1)
    }

    @Test
    fun testCancelDuringInit() = runBlockingTest {
        val continueInit = CompletableDeferred<Unit>()

        store = newDataStore(initTasksList =
        listOf { api ->
            continueInit.await()
            api.updateData { 1 }
        })

        val update = async {
            store.updateData { it }
        }

        val read = async {
            store.data.first()
        }

        update.cancel()
        read.cancel()
        continueInit.complete(Unit)

        assertThrows<CancellationException> { update.await() }
        assertThrows<CancellationException> { read.await() }

        store.updateData { it.inc().inc() }

        assertThat(store.data.first()).isEqualTo(3)
    }

    @Test
    fun testConcurrentUpdatesInit() = runBlockingTest {
        val continueUpdate = CompletableDeferred<Unit>()

        val concurrentUpdateInitializer: suspend (InitializerApi<Byte>) -> Unit = { api ->
            val update1 = async {
                api.updateData {
                    continueUpdate.await()
                    it.inc().inc()
                }
            }
            api.updateData {
                it.inc()
            }
            update1.await()
        }

        store = newDataStore(initTasksList = listOf(concurrentUpdateInitializer))
        val getData = async { store.data.first() }
        continueUpdate.complete(Unit)

        assertThat(getData.await()).isEqualTo(3)
    }

    @Test
    fun testUpdateSuccessfullyCommittedInit() = runBlockingTest {
        var otherStorage: Byte = 123

        val initializer: suspend (InitializerApi<Byte>) -> Unit = { api ->
            api.updateData {
                otherStorage
            }
            // Similar to cleanUp():
            otherStorage = 0
        }

        val store = newDataStore(initTasksList = listOf(initializer))

        serializer.failingWrite = true
        assertThrows<IOException> { store.data.first() }

        serializer.failingWrite = false
        assertThat(store.data.first()).isEqualTo(123)
    }

    @Test
    fun testInitApiUpdateThrowsAfterInitTasksComplete() = runBlockingTest {
        var savedApi: InitializerApi<Byte>? = null

        val initializer: suspend (InitializerApi<Byte>) -> Unit = { api ->
            savedApi = api
        }

        val store = newDataStore(initTasksList = listOf(initializer))

        assertThat(store.data.first()).isEqualTo(0)

        assertThrows<IllegalStateException> { savedApi?.updateData { 123 } }
    }

    @Test
    fun testFlowReceivesUpdates() = runBlockingTest {
        val collectedBytes = mutableListOf<Byte>()

        val flowCollectionJob = async {
            store.data.take(8).toList(collectedBytes)
        }

        repeat(7) {
            store.updateData { it.inc() }
        }

        flowCollectionJob.join()

        assertThat(collectedBytes).isEqualTo(mutableListOf<Byte>(0, 1, 2, 3, 4, 5, 6, 7))
    }

    @Test
    fun testMultipleFlowsReceiveData() = runBlockingTest {
        val flowOf8 = store.data.take(8)

        val bytesFromFirstCollect = mutableListOf<Byte>()
        val bytesFromSecondCollect = mutableListOf<Byte>()

        val flowCollection1 = async {
            flowOf8.toList(bytesFromFirstCollect)
        }

        val flowCollection2 = async {
            flowOf8.toList(bytesFromSecondCollect)
        }

        repeat(7) {
            store.updateData { it.inc() }
        }

        flowCollection1.join()
        flowCollection2.join()

        assertThat(bytesFromFirstCollect).isEqualTo(mutableListOf<Byte>(0, 1, 2, 3, 4, 5, 6, 7))
        assertThat(bytesFromSecondCollect).isEqualTo(mutableListOf<Byte>(0, 1, 2, 3, 4, 5, 6, 7))
    }

    @Test
    fun testExceptionInFlowDoesNotBreakUpstream() = runBlockingTest {
        val flowOf8 = store.data.take(8)

        val collectedBytes = mutableListOf<Byte>()

        val failedFlowCollection = async {
            flowOf8.collect {
                throw Exception("Failure while collecting")
            }
        }

        val successfulFlowCollection = async {
            flowOf8.take(8).toList(collectedBytes)
        }

        repeat(7) {
            store.updateData { it.inc() }
        }

        successfulFlowCollection.join()
        assertThrows<Exception> { failedFlowCollection.await() }.hasMessageThat()
            .contains("Failure while collecting")

        assertThat(collectedBytes).isEqualTo(mutableListOf<Byte>(0, 1, 2, 3, 4, 5, 6, 7))
    }

    @Test
    fun testSlowConsumerDoesntBlockOtherConsumers() = runBlockingTest {
        val flowOf8 = store.data.take(8)

        val collectedBytes = mutableListOf<Byte>()

        val flowCollection2 = async {
            flowOf8.toList(collectedBytes)
        }

        val blockedCollection = async {
            flowOf8.collect {
                flowCollection2.await()
            }
        }

        repeat(15) {
            store.updateData { it.inc() }
        }

        flowCollection2.await()
        assertThat(collectedBytes).isEqualTo(mutableListOf<Byte>(0, 1, 2, 3, 4, 5, 6, 7))

        blockedCollection.await()
    }

    @Test
    fun testHandlerNotCalledGoodData() = runBlockingTest {
        store.updateData { 1 } // Pre-seed the data so the file exists.

        val testingHandler: TestingCorruptionHandler = TestingCorruptionHandler()
        store = newDataStore(corruptionHandler = testingHandler, file = testFile)

        store.updateData { 2 }
        store.data.first()

        assertThat(testingHandler.numCalls).isEqualTo(0)
    }

    @Test
    fun handlerNotCalledNonCorruption() = runBlockingTest {
        store.updateData { 1 } // Pre-seed the data so the file exists.

        val testingHandler: TestingCorruptionHandler = TestingCorruptionHandler()
        serializer.failingRead = true
        store = newDataStore(corruptionHandler = testingHandler, file = testFile)

        assertThrows<IOException> { store.updateData { 2 } }
        assertThrows<IOException> { store.data.first() }

        assertThat(testingHandler.numCalls).isEqualTo(0)
    }

    @Test
    fun testHandlerCalledCorruptDataRead() = runBlockingTest {
        store.updateData { 1 } // Pre-seed the data so the file exists.

        val testingHandler: TestingCorruptionHandler = TestingCorruptionHandler()
        serializer.failReadWithCorruptionException = true
        store = newDataStore(corruptionHandler = testingHandler, file = testFile)

        assertThrows<IOException> { store.data.first() }.hasMessageThat().contains(
            "Handler thrown exception."
        )

        assertThat(testingHandler.numCalls).isEqualTo(1)
    }

    @Test
    fun testHandlerCalledCorruptDataWrite() = runBlockingTest {
        store.updateData { 1 } // Pre-seed the data so the file exists.

        val testingHandler: TestingCorruptionHandler = TestingCorruptionHandler()
        serializer.failReadWithCorruptionException = true
        store = newDataStore(corruptionHandler = testingHandler, file = testFile)

        assertThrows<IOException> { store.updateData { 1 } }.hasMessageThat().contains(
            "Handler thrown exception."
        )

        assertThat(testingHandler.numCalls).isEqualTo(1)
    }

    @Test
    fun testHandlerReplaceData() = runBlockingTest {
        store.updateData { 1 } // Pre-seed the data so the file exists.

        val testingHandler: TestingCorruptionHandler = TestingCorruptionHandler(replaceWith = 10)
        serializer.failReadWithCorruptionException = true
        store = newDataStore(corruptionHandler = testingHandler, file = testFile)

        assertThat(store.data.first()).isEqualTo(10)
    }

    private class TestingCorruptionHandler(
        private val replaceWith: Byte? = null
    ) : CorruptionHandler<Byte> {

        @Volatile
        var numCalls = 0

        override suspend fun handleCorruption(ex: CorruptionException): Byte {
            numCalls++

            replaceWith?.let {
                return it
            }

            throw IOException("Handler thrown exception.")
        }
    }

    private fun newDataStore(
        file: File = testFile,
        scope: CoroutineScope = dataStoreScope,
        initTasksList: List<suspend (api: InitializerApi<Byte>) -> Unit> = listOf(),
        corruptionHandler: CorruptionHandler<Byte> = NoOpCorruptionHandler<Byte>()
    ): DataStore<Byte> {
        return SingleProcessDataStore(
            { file },
            serializer = serializer,
            scope = scope,
            initTasksList = initTasksList,
            corruptionHandler = corruptionHandler
        )
    }
}
