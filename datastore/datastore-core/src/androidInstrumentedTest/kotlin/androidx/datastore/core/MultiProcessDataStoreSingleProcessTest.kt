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

package androidx.datastore.core

import android.os.StrictMode
import androidx.datastore.TestFile
import androidx.datastore.TestIO
import androidx.datastore.TestingSerializerConfig
import androidx.datastore.core.handlers.NoOpCorruptionHandler
import androidx.test.filters.FlakyTest
import androidx.test.filters.LargeTest
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * A testing class based on duplicate from "SingleProcessDataStoreTest" that only tests the features
 * in a single process use case. More tests are added for StrictMode.
 */
@OptIn(DelicateCoroutinesApi::class)
@ExperimentalCoroutinesApi
@LargeTest
@RunWith(JUnit4::class)
abstract class MultiProcessDataStoreSingleProcessTest<F : TestFile<F>>(
    protected val testIO: TestIO<F, *>
) {
    protected lateinit var store: DataStore<Byte>
    private lateinit var serializerConfig: TestingSerializerConfig
    protected lateinit var testFile: F
    protected lateinit var tempFolder: F
    protected lateinit var dataStoreScope: TestScope

    abstract fun getJavaFile(file: F): File

    private fun newDataStore(
        file: F = testFile,
        scope: CoroutineScope = dataStoreScope,
        initTasksList: List<suspend (api: InitializerApi<Byte>) -> Unit> = listOf(),
        corruptionHandler: CorruptionHandler<Byte> = NoOpCorruptionHandler<Byte>()
    ): DataStore<Byte> {
        return DataStoreImpl(
            storage = testIO.getStorage(
                serializerConfig,
                {
                    MultiProcessCoordinator(
                        dataStoreScope.coroutineContext,
                        getJavaFile(testFile)
                    )
                }) { file },
            scope = scope,
            initTasksList = initTasksList,
            corruptionHandler = corruptionHandler
        )
    }

    @Before
    fun setUp() {
        serializerConfig = TestingSerializerConfig()
        tempFolder = testIO.newTempFile().also { it.mkdirs() }
        testFile = testIO.newTempFile(parentFile = tempFolder)
        dataStoreScope = TestScope(UnconfinedTestDispatcher() + Job())
        store = testIO.getStore(
            serializerConfig,
            dataStoreScope,
            { MultiProcessCoordinator(dataStoreScope.coroutineContext, getJavaFile(testFile)) }
        ) { testFile }
    }

    @Test
    fun testReadNewMessage() = runTest {
        assertThat(store.data.first()).isEqualTo(0)
    }

    @Test
    fun testReadWithNewInstance() = runBlocking {
        runTest {
            val newStore = newDataStore(testFile, scope = backgroundScope)
            newStore.updateData { 1 }
        }
        runTest {
            val newStore = newDataStore(testFile, scope = backgroundScope)
            assertThat(newStore.data.first()).isEqualTo(1)
        }
    }

    @Test
    fun testScopeCancelledWithActiveFlow() = runTest {
        val storeScope = CoroutineScope(Job())
        val dataStore = newDataStore(scope = storeScope)
        val collection = async {
            dataStore.data.take(2).collect {
                // Do nothing, this will wait on another element which will never arrive
            }
        }

        storeScope.cancel()
        collection.join()

        assertThat(collection.isCompleted).isTrue()
        assertThat(collection.isActive).isFalse()
    }

    @Test
    fun testWriteAndRead() = runTest {
        store.updateData { 1 }
        assertThat(store.data.first()).isEqualTo(1)
    }

    @Test
    fun testWritesDontBlockReadsInSameProcess() = runTest {
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
    fun testWriteMultiple() = runTest {
        store.updateData { 2 }

        assertThat(store.data.first()).isEqualTo(2)

        store.updateData { it.dec() }

        assertThat(store.data.first()).isEqualTo(1)
    }

    @Test
    fun testReadAfterTransientBadWrite() = runBlocking {
        val file = testIO.newTempFile(tempFolder)
        runTest {
            val store = newDataStore(file, scope = backgroundScope)
            store.updateData { 1 }
            serializerConfig.failingWrite = true
            assertThrows<IOException> { store.updateData { 2 } }
        }

        runTest {
            val newStore = newDataStore(file, scope = backgroundScope)
            assertThat(newStore.data.first()).isEqualTo(1)
        }
    }

    @Test
    fun testWriteToNonExistentDir() = runBlocking {
        val fileInNonExistentDir = testIO.newTempFile(
            relativePath = "/this/does/not/exist/ds.txt"
        )
        assertThat(fileInNonExistentDir.exists()).isFalse()
        assertThat(fileInNonExistentDir.parentFile()!!.exists()).isFalse()
        runTest {
            val newStore = newDataStore(fileInNonExistentDir, scope = backgroundScope)

            newStore.updateData { 1 }

            assertThat(newStore.data.first()).isEqualTo(1)
        }

        runTest {
            val newStore = newDataStore(fileInNonExistentDir, scope = backgroundScope)
            assertThat(newStore.data.first()).isEqualTo(1)
        }
    }

    @Test
    fun testReadFromNonExistentFile() = runTest {
        val newStore = newDataStore(testFile)
        assertThat(newStore.data.first()).isEqualTo(0)
    }

    @Test
    fun testWriteToDirFails() = runTest {
        val directoryFile = testIO.newTempFile(relativePath = "this/is/a/directory").also {
            it.mkdirs()
        }

        assertThat(directoryFile.isDirectory()).isTrue()

        val newStore = newDataStore(directoryFile)
        assertThrows<IOException> { newStore.data.first() }
    }

    @Test
    fun testExceptionWhenCreatingFilePropagates() = runTest {
        var failFileProducer = true

        val fileProducer = {
            if (failFileProducer) {
                throw IOException("Exception when producing file")
            }
            testFile
        }

        val newStore = testIO.getStore(
            serializerConfig,
            dataStoreScope,
            {
                MultiProcessCoordinator(
                    dataStoreScope.coroutineContext,
                    getJavaFile(fileProducer())
                )
            },
            fileProducer
        )

        assertThrows<IOException> { newStore.data.first() }.hasMessageThat().isEqualTo(
            "Exception when producing file"
        )

        failFileProducer = false

        assertThat(newStore.data.first()).isEqualTo(0)
    }

    @Test
    fun testWriteTransformCancellation() = runTest {
        val transform = CompletableDeferred<Byte>()

        val write = async { store.updateData { transform.await() } }

        assertThat(write.isCompleted).isFalse()

        transform.cancel()

        assertThrows<CancellationException> { write.await() }

        // Check that the datastore's scope is still active:

        assertThat(store.updateData { it.inc().inc() }).isEqualTo(2)
    }

    @Test
    fun testWriteAfterTransientBadRead() = runTest {
        testFile.write("")
        assertThat(testFile.exists()).isTrue()

        serializerConfig.failingRead = true

        assertThrows<IOException> { store.data.first() }

        serializerConfig.failingRead = false

        store.updateData { 1 }
        assertThat(store.data.first()).isEqualTo(1)
    }

    @Test
    fun testWriteWithBadReadFails() = runTest {
        testFile.write("")
        assertThat(testFile.exists()).isTrue()

        serializerConfig.failingRead = true

        assertThrows<IOException> { store.updateData { 1 } }
    }

    @Test
    fun testCancellingDataStoreScopePropagatesToWrites() = runBlocking<Unit> {
        val scope = CoroutineScope(Job())

        val dataStore = newDataStore(scope = scope)

        val latch = CompletableDeferred<Unit>()

        val slowUpdate = async {
            dataStore.updateData {
                latch.await()
                it.inc()
            }
        }

        val notStartedUpdate = async {
            dataStore.updateData {
                it.inc()
            }
        }

        scope.cancel()

        assertThrows<CancellationException> { slowUpdate.await() }

        assertThrows<CancellationException> { notStartedUpdate.await() }

        assertThrows<CancellationException> { dataStore.updateData { 123 } }
    }

    @Test
    fun testCancellingCallerScopePropagatesToWrites() = runBlocking<Unit> {
        val dsScope = CoroutineScope(Job())
        val callerScope = CoroutineScope(Job())

        val dataStore = newDataStore(scope = dsScope)

        val latch = CompletableDeferred<Unit>()

        // The ordering of the following are not guaranteed but I think they won't be flaky with
        // Dispatchers.Unconfined
        val awaitingCancellation = callerScope.async(Dispatchers.Unconfined) {
            dataStore.updateData { awaitCancellation() }
        }

        val started = dsScope.async(Dispatchers.Unconfined) {
            dataStore.updateData {
                latch.await()
                it.inc()
            }
        }

        val notStarted = callerScope.async(Dispatchers.Unconfined) {
            dataStore.updateData { it.inc() }
        }

        callerScope.coroutineContext.job.cancelAndJoin()

        assertThat(awaitingCancellation.isCancelled).isTrue()
        assertThat(notStarted.isCancelled).isTrue()

        // wait for coroutine to complete to prevent it from outliving the test, which is flaky
        latch.complete(Unit)
        started.await()
        assertThat(dataStore.data.first()).isEqualTo(1)
    }

    @Test
    fun testCanWriteFromInitTask() = runTest {
        store = newDataStore(initTasksList = listOf { api -> api.updateData { 1 } })

        assertThat(store.data.first()).isEqualTo(1)
    }

    @FlakyTest(bugId = 242765370)
    @Test
    fun testInitTaskFailsFirstTimeDueToReadFail() = runTest {
        store = newDataStore(initTasksList = listOf { api -> api.updateData { 1 } })

        serializerConfig.failingRead = true
        assertThrows<IOException> { store.updateData { 2 } }

        serializerConfig.failingRead = false
        store.updateData { it.inc().inc() }

        assertThat(store.data.first()).isEqualTo(3)
    }

    @Test
    fun testInitTaskFailsFirstTimeDueToException() = runTest {
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
    fun testInitTaskOnlyRunsOnce() = runTest {
        val count = AtomicInteger()
        val newStore = newDataStore(
            testFile,
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
    fun testWriteDuringInit() = runTest {
        val continueInit = CompletableDeferred<Unit>()

        store = newDataStore(
            initTasksList = listOf { api ->
                continueInit.await()
                api.updateData { 1 }
            }
        )

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
    fun testCancelDuringInit() = runTest {
        val continueInit = CompletableDeferred<Unit>()

        store = newDataStore(
            initTasksList = listOf { api ->
                continueInit.await()
                api.updateData { 1 }
            }
        )

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
    fun testConcurrentUpdatesInit() = runTest {
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
    fun testInitUpdateBlockRead() = runTest {
        val continueInit = CompletableDeferred<Unit>()
        val continueUpdate = CompletableDeferred<Unit>()

        val updateInitializer: suspend (InitializerApi<Byte>) -> Unit = { api ->
            api.updateData {
                continueInit.await()
                it.inc()
            }
        }

        store = newDataStore(initTasksList = listOf(updateInitializer))
        val getData = async { store.data.first() }
        val updateData = async {
            store.updateData {
                continueUpdate.await()
                it.inc()
            }
        }

        assertThat(getData.isCompleted).isFalse()
        assertThat(getData.isActive).isTrue()

        continueInit.complete(Unit)
        assertThat(getData.await()).isEqualTo(1)

        assertThat(updateData.isCompleted).isFalse()
        assertThat(updateData.isActive).isTrue()

        continueUpdate.complete(Unit)
        assertThat(updateData.await()).isEqualTo(2)
        assertThat(store.data.first()).isEqualTo(2)
    }

    @Test
    fun testUpdateSuccessfullyCommittedInit() = runTest {
        var otherStorage: Byte = 123

        val initializer: suspend (InitializerApi<Byte>) -> Unit = { api ->
            api.updateData {
                otherStorage
            }
            // Similar to cleanUp():
            otherStorage = 0
        }

        val store = newDataStore(initTasksList = listOf(initializer))

        serializerConfig.failingWrite = true
        assertThrows<IOException> { store.data.first() }

        serializerConfig.failingWrite = false
        assertThat(store.data.first()).isEqualTo(123)
    }

    @Test
    fun testInitApiUpdateThrowsAfterInitTasksComplete() = runTest {
        var savedApi: InitializerApi<Byte>? = null

        val initializer: suspend (InitializerApi<Byte>) -> Unit = { api ->
            savedApi = api
        }

        val store = newDataStore(initTasksList = listOf(initializer))

        assertThat(store.data.first()).isEqualTo(0)

        assertThrows<IllegalStateException> { savedApi?.updateData { 123 } }
    }

    @Test
    fun testFlowReceivesUpdates() = runTest {
        val collectedBytes = mutableListOf<Byte>()

        val flowCollectionJob = async {
            store.data.take(8).toList(collectedBytes)
        }

        runCurrent()
        repeat(7) {
            store.updateData { it.inc() }
        }

        flowCollectionJob.join()

        assertThat(collectedBytes).isEqualTo(mutableListOf<Byte>(0, 1, 2, 3, 4, 5, 6, 7))
    }

    @Test
    fun testMultipleFlowsReceiveData() = runTest {
        val flowOf8 = store.data.take(8)

        val bytesFromFirstCollect = mutableListOf<Byte>()
        val bytesFromSecondCollect = mutableListOf<Byte>()

        val flowCollection1 = async {
            flowOf8.toList(bytesFromFirstCollect)
        }

        val flowCollection2 = async {
            flowOf8.toList(bytesFromSecondCollect)
        }

        runCurrent()
        repeat(7) {
            store.updateData { it.inc() }
        }

        flowCollection1.join()
        flowCollection2.join()

        // This test only works because runTest ensures consistent behavior
        // Otherwise, we cannot really expect the collector to read every single value
        // (we provide eventual consistency, so it would also be OK if it missed some intermediate
        // values as long as it received 7 at the end).
        assertThat(bytesFromFirstCollect).isEqualTo(mutableListOf<Byte>(0, 1, 2, 3, 4, 5, 6, 7))
        assertThat(bytesFromSecondCollect).isEqualTo(mutableListOf<Byte>(0, 1, 2, 3, 4, 5, 6, 7))
    }

    @Test
    fun testExceptionInFlowDoesNotBreakUpstream() = runTest {
        val flowOf8 = store.data.take(8)

        val collectedBytes = mutableListOf<Byte>()

        val failedFlowCollection = async {
            assertThrows<Exception> {
                flowOf8.collect {
                    throw Exception("Failure while collecting")
                }
            }.hasMessageThat().contains("Failure while collecting")
        }

        val successfulFlowCollection = async {
            flowOf8.take(8).toList(collectedBytes)
        }

        runCurrent()
        repeat(7) {
            store.updateData { it.inc() }
        }

        successfulFlowCollection.join()
        failedFlowCollection.await()

        assertThat(collectedBytes).isEqualTo(mutableListOf<Byte>(0, 1, 2, 3, 4, 5, 6, 7))
    }

    @Test
    fun testSlowConsumerDoesntBlockOtherConsumers() = runTest {
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

        runCurrent()
        repeat(15) {
            store.updateData { it.inc() }
        }

        flowCollection2.await()
        assertThat(collectedBytes).isEqualTo(mutableListOf<Byte>(0, 1, 2, 3, 4, 5, 6, 7))

        blockedCollection.await()
    }

    @Test
    fun testHandlerNotCalledGoodData() = runBlocking {
        runTest {
            newDataStore(testFile, scope = backgroundScope).updateData { 1 }
        }

        runTest {
            val testingHandler: TestingCorruptionHandler = TestingCorruptionHandler()
            val newStore = newDataStore(corruptionHandler = testingHandler, file = testFile)

            newStore.updateData { 2 }
            newStore.data.first()

            assertThat(testingHandler.numCalls).isEqualTo(0)
        }
    }

    @Test
    fun handlerNotCalledNonCorruption() = runBlocking {
        runTest {
            newDataStore(testFile, scope = backgroundScope).updateData { 1 }
        }

        runTest {
            val testingHandler = TestingCorruptionHandler()
            serializerConfig.failingRead = true
            val newStore = newDataStore(corruptionHandler = testingHandler, file = testFile)

            assertThrows<IOException> { newStore.updateData { 2 } }
            assertThrows<IOException> { newStore.data.first() }

            assertThat(testingHandler.numCalls).isEqualTo(0)
        }
    }

    @Test
    fun testHandlerCalledCorruptDataRead() = runBlocking {
        runTest {
            val newStore = newDataStore(testFile, scope = backgroundScope)
            newStore.updateData { 1 } // Pre-seed the data so the file exists.
        }

        runTest {
            val testingHandler: TestingCorruptionHandler = TestingCorruptionHandler()
            serializerConfig.failReadWithCorruptionException = true
            val newStore = newDataStore(corruptionHandler = testingHandler, file = testFile)

            assertThrows<IOException> { newStore.data.first() }.hasMessageThat().contains(
                "Handler thrown exception."
            )

            assertThat(testingHandler.numCalls).isEqualTo(1)
        }
    }

    @Test
    fun testHandlerCalledCorruptDataWrite() = runBlocking {
        runTest {
            val newStore = newDataStore(file = testFile, scope = backgroundScope)
            newStore.updateData { 1 }
        }

        runTest {
            val testingHandler: TestingCorruptionHandler = TestingCorruptionHandler()
            serializerConfig.failReadWithCorruptionException = true
            val newStore = newDataStore(corruptionHandler = testingHandler, file = testFile)

            assertThrows<IOException> { newStore.updateData { 1 } }.hasMessageThat().contains(
                "Handler thrown exception."
            )

            assertThat(testingHandler.numCalls).isEqualTo(1)
        }
    }

    @Test
    fun testHandlerReplaceData() = runBlocking {
        runTest {
            newDataStore(file = testFile, scope = backgroundScope).updateData { 1 }
        }

        runTest {
            val testingHandler: TestingCorruptionHandler =
                TestingCorruptionHandler(replaceWith = 10)
            serializerConfig.failReadWithCorruptionException = true
            val newStore = newDataStore(
                corruptionHandler = testingHandler, file = testFile,
                scope = backgroundScope
            )

            assertThat(newStore.data.first()).isEqualTo(10)
        }
    }

    @Test
    fun testDefaultValueUsedWhenNoDataOnDisk() = runTest {
        val dataStore = testIO.getStore(
            TestingSerializerConfig(defaultValue = 99),
            dataStoreScope,
            { MultiProcessCoordinator(dataStoreScope.coroutineContext, getJavaFile(testFile)) }) {
            testFile
        }

        assertThat(dataStore.data.first()).isEqualTo(99)
    }

    @Test
    fun testTransformRunInCallersContext() = runBlocking<Unit> {
        suspend fun getContext(): CoroutineContext {
            return kotlin.coroutines.coroutineContext
        }

        withContext(TestElement("123")) {
            store.updateData {
                val context = getContext()
                assertThat(context[TestElement.Key]!!.name).isEqualTo("123")
                it.inc()
            }
        }
    }

    private class TestElement(
        val name: String
    ) : AbstractCoroutineContextElement(Key) {
        companion object Key : CoroutineContext.Key<TestElement>
    }

    @Test
    fun testCancelInflightWrite() = doBlockingWithTimeout(1000) {
        val myScope =
            CoroutineScope(Job() + Executors.newSingleThreadExecutor().asCoroutineDispatcher())

        val updateStarted = CompletableDeferred<Unit>()
        myScope.launch {
            store.updateData {
                updateStarted.complete(Unit)
                awaitCancellation()
            }
        }
        updateStarted.await()
        myScope.coroutineContext[Job]!!.cancelAndJoin()
    }

    @Test
    fun testWrite_afterCanceledWrite_succeeds() = doBlockingWithTimeout(1000) {
        val myScope =
            CoroutineScope(Job() + Executors.newSingleThreadExecutor().asCoroutineDispatcher())

        val cancelNow = CompletableDeferred<Unit>()

        myScope.launch {
            store.updateData {
                cancelNow.complete(Unit)
                awaitCancellation()
            }
        }

        cancelNow.await()
        myScope.coroutineContext[Job]!!.cancelAndJoin()

        store.updateData { 123 }
    }

    @Test
    fun testWrite_fromOtherScope_doesntGetCancelledFromDifferentScope() =
    doBlockingWithTimeout(1000) {
        val otherScope = CoroutineScope(Job())

        val callerScope = CoroutineScope(Job())

        val firstUpdateStarted = CompletableDeferred<Unit>()
        val finishFirstUpdate = CompletableDeferred<Byte>()

        val firstUpdate = otherScope.async(Dispatchers.Unconfined) {
            store.updateData {
                firstUpdateStarted.complete(Unit)
                finishFirstUpdate.await()
            }
        }

        callerScope.launch(Dispatchers.Unconfined) {
            store.updateData {
                awaitCancellation()
            }
        }

        firstUpdateStarted.await()
        callerScope.coroutineContext.job.cancelAndJoin()
        finishFirstUpdate.complete(1)
        firstUpdate.await()

        // It's still usable:
        assertThat(store.updateData { it.inc() }).isEqualTo(2)
    }

    @Test
    fun testCreateDuplicateActiveDataStore() = runTest {
        val file = testIO.newTempFile(parentFile = tempFolder)
        val dataStore = newDataStore(file = file, scope = CoroutineScope(Job()))

        dataStore.data.first()

        val duplicateDataStore = newDataStore(file = file, scope = CoroutineScope(Job()))

        assertThrows<IllegalStateException> {
            duplicateDataStore.data.first()
        }
    }

    @Test
    fun testCreateDataStore_withSameFileAsInactiveDataStore() = runTest {
        val file = testIO.newTempFile(parentFile = tempFolder)
        val scope1 = CoroutineScope(Job())
        val dataStore1 = newDataStore(file = file, scope = scope1)

        dataStore1.data.first()

        scope1.coroutineContext.job.cancelAndJoin()

        val dataStore2 = newDataStore(file = file, scope = CoroutineScope(Job()))

        // This shouldn't throw an exception bc the scope1 has been cancelled.
        dataStore2.data.first()
    }

    @Test
    fun testCreateDataStoreAndRead_withStrictMode() = runTest {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().penaltyDeath()
                .build()
        )
        val dataStore =
            newDataStore(file = testFile, scope = CoroutineScope(newSingleThreadContext("test")))
        assertThat(dataStore.data.first()).isEqualTo(0)
        StrictMode.allowThreadDiskReads()
        StrictMode.allowThreadDiskWrites()
    }

    @Test
    fun testCreateDataStoreAndUpdate_withStrictMode() = runTest {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().penaltyDeath()
                .build()
        )
        val dataStore =
            newDataStore(file = testFile, scope = CoroutineScope(newSingleThreadContext("test")))
        dataStore.updateData { it.inc() }
        assertThat(dataStore.data.first()).isEqualTo(1)
        StrictMode.allowThreadDiskReads()
        StrictMode.allowThreadDiskWrites()
    }

    @Test
    fun testWriteSameValueSkipDisk() = runTest {
        // write a non-default value to force a disk write
        store.updateData { 10 }
        assertThat(serializerConfig.writeCount).isEqualTo(1)

        // write same value again
        store.updateData { 10 }
        assertThat(serializerConfig.writeCount).isEqualTo(1)
    }

    // Mutable wrapper around a byte
    data class ByteWrapper(var byte: Byte) {
        internal class ByteWrapperSerializer() : Serializer<ByteWrapper> {
            private val delegate = TestingSerializer()

            override val defaultValue = ByteWrapper(delegate.defaultValue)

            override suspend fun readFrom(input: InputStream): ByteWrapper {
                return ByteWrapper(delegate.readFrom(input))
            }

            override suspend fun writeTo(t: ByteWrapper, output: OutputStream) {
                delegate.writeTo(t.byte, output)
            }
        }
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
}

fun doBlockingWithTimeout(ms: Long, block: suspend () -> Unit): Unit = runBlocking<Unit> {
    withTimeout(ms) { block() }
}
