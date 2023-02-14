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

@file:Suppress("DEPRECATION") // b/220884658

package androidx.datastore.core

import androidx.datastore.TestFile
import androidx.datastore.TestIO
import androidx.datastore.TestingSerializerConfig
import androidx.datastore.core.handlers.NoOpCorruptionHandler
import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.SupervisorJob

@OptIn(ExperimentalCoroutinesApi::class)
abstract class SingleProcessDataStoreTest<F : TestFile>(private val testIO: TestIO<F, *>) {

    protected lateinit var store: DataStore<Byte>
    private lateinit var serializerConfig: TestingSerializerConfig
    protected lateinit var testFile: F
    private lateinit var tempFolder: F
    protected lateinit var dataStoreScope: CoroutineScope

    @BeforeTest
    fun setUp() {
        serializerConfig = TestingSerializerConfig()
        tempFolder = testIO.tempDir()
        testFile = testIO.newTempFile(tempFolder)
        dataStoreScope = TestScope(UnconfinedTestDispatcher())
        store = testIO.getStore(serializerConfig, dataStoreScope) { testFile }
    }

    fun doTest(initDataStore: Boolean = false, test: suspend TestScope.() -> Unit) {
        if (initDataStore) {
            runTest(dispatchTimeoutMs = 10000) {
                initDataStore(this)
            }
        }
        runTest(dispatchTimeoutMs = 10000) {
            test(this)
        }
    }

    @Test
    fun testReadNewMessage() = doTest {
        assertThat(store.data.first()).isEqualTo(0)
    }

    @Test
    fun testReadWithNewInstance() = doTest {
        coroutineScope {
            val newStore = newDataStore(testFile, scope = this)
            newStore.updateData { 1 }
        }
        coroutineScope {
            val newStore = newDataStore(testFile, scope = this)
            assertThat(newStore.data.first()).isEqualTo(1)
        }
    }

    @Test
    fun testScopeCancelledWithActiveFlow() = doTest {
        val storeScope = CoroutineScope(Job())
        val store = testIO.getStore(serializerConfig, storeScope) { testFile }

        val collection = async {
            store.data.take(2).collect {
                // Do nothing, this will wait on another element which will never arrive
            }
        }

        storeScope.cancel()
        collection.join()

        assertThat(collection.isCompleted).isTrue()
        assertThat(collection.isActive).isFalse()
    }

    @Test
    fun testWriteAndRead() = doTest {
        store.updateData { 1 }
        assertThat(store.data.first()).isEqualTo(1)
    }

    @Test
    fun testWritesDontBlockReadsInSameProcess() = doTest {
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
    fun testWriteMultiple() = doTest {
        store.updateData { 2 }
        store.updateData { it.dec() }

        assertThat(store.data.first()).isEqualTo(1)
    }

    @Test
    fun testReadAfterTransientBadWrite() = doTest {
        val file = testIO.newTempFile()
        coroutineScope {
            val store = newDataStore(file = file, scope = this)
            store.updateData { 1 }
            serializerConfig.failingWrite = true
            assertThrows(testIO.ioExceptionClass()) { store.updateData { 2 } }
        }

        coroutineScope {
            val newStore = newDataStore(file, scope = this)
            assertThat(newStore.data.first()).isEqualTo(1)
        }
    }

    @Test
    fun testWriteToNonExistentDir() = doTest {
        val fileInNonExistentDir = testIO.newTempFile(
            testIO.tempDir("/this/does/not/exist", makeDirs = false))

        coroutineScope {
            val newStore = newDataStore(fileInNonExistentDir, scope = this)

            newStore.updateData { 1 }

            assertThat(newStore.data.first()).isEqualTo(1)
        }

        coroutineScope {
            val newStore = newDataStore(fileInNonExistentDir, scope = this)
            assertThat(newStore.data.first()).isEqualTo(1)
        }
    }

    @Test
    fun testReadFromNonExistentFile() = doTest {
        assertThat(testFile.delete()).isTrue()
        val newStore = newDataStore(testFile)
        assertThat(newStore.data.first()).isEqualTo(0)
    }

    @Test
    fun testWriteToDirFails() = doTest {
        val directoryFile = testIO.tempDir("/this/is/a${Random.nextInt()}/directory")

        assertThat(testIO.isDirectory(directoryFile))

        val newStore = newDataStore(directoryFile)
        assertThrows(testIO.ioExceptionClass()) { newStore.data.first() }
    }

    @Test
    fun testExceptionWhenCreatingFilePropagates() = doTest {
        var failFileProducer = true

        val fileProducer = {
            if (failFileProducer) {
                throw IOException("Exception when producing file")
            }
            testFile
        }
        val newStore = testIO.getStore(serializerConfig, dataStoreScope, fileProducer)

        assertThrows<IOException> { newStore.data.first() }.hasMessageThat().isEqualTo(
            "Exception when producing file"
        )

        failFileProducer = false

        assertThat(newStore.data.first()).isEqualTo(0)
    }

    @Test
    fun testWriteTransformCancellation() = doTest {
        val transform = CompletableDeferred<Byte>()

        val write = async { store.updateData { transform.await() } }

        assertThat(write.isCompleted).isFalse()

        transform.cancel()

        assertThrows<CancellationException> { write.await() }

        // Check that the datastore's scope is still active:

        assertThat(store.updateData { it.inc().inc() }).isEqualTo(2)
    }

    @Test
    fun testWriteAfterTransientBadRead() = doTest(initDataStore = true) {
        serializerConfig.failingRead = true

        assertThrows(testIO.ioExceptionClass()) { store.data.first() }

        serializerConfig.failingRead = false

        store.updateData { 1 }
        assertThat(store.data.first()).isEqualTo(1)
    }

    @Test
    fun testWriteWithBadReadFails() = doTest(initDataStore = true) {
        serializerConfig.failingRead = true

        assertThrows(testIO.ioExceptionClass()) { store.updateData { 1 } }
    }

    @Test
    fun testCancellingDataStoreScopePropagatesToWrites() = doTest {
        val scope = CoroutineScope(Job())
        val store = newDataStore(scope = scope)
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

        scope.cancel()

        assertThrows<CancellationException> { slowUpdate.await() }

        assertThrows<CancellationException> { notStartedUpdate.await() }

        assertThrows<CancellationException> { store.updateData { 123 } }
    }

    @Test
    fun testCancellingCallerScopePropagatesToWrites() = doTest {
        val dsScope = CoroutineScope(Job())
        val callerScope = CoroutineScope(Job())

        val dataStore = newDataStore(scope = dsScope)

        val latch = CompletableDeferred<Unit>()

        // The ordering of the following are not guaranteed but I think they won't be flaky with
        // Dispatchers.Unconfined
        val awaitingCancellation = callerScope.async(Dispatchers.Unconfined) {
            dataStore.updateData { awaitCancellation() }
        }

        dsScope.launch(Dispatchers.Unconfined) {
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
    }

    @Test
    fun testCanWriteFromInitTask() = doTest {
        store = newDataStore(initTasksList = listOf<InitTaskList>({ api -> api.updateData { 1 } }))

        assertThat(store.data.first()).isEqualTo(1)
    }

    @Test
    fun testInitTaskFailsFirstTimeDueToReadFail() = doTest(initDataStore = true) {
        store = newDataStore(initTasksList = listOf<InitTaskList>({ api -> api.updateData { 1 } }))

        serializerConfig.failingRead = true
        assertThrows(testIO.ioExceptionClass()) { store.updateData { 2 } }

        serializerConfig.failingRead = false
        store.updateData { it.inc().inc() }

        assertThat(store.data.first()).isEqualTo(3)
    }

    @Test
    fun testInitTaskFailsFirstTimeDueToException() = doTest {
        val failInit = AtomicBoolean(true)
        store = newDataStore(
            initTasksList = listOf({ _ ->
                if (failInit.get()) {
                    throw IOException("I was asked to fail init")
                }
            })
        )
        assertThrows<IOException> { store.updateData { 5 } }

        failInit.set(false)

        store.updateData { it.inc() }
        assertThat(store.data.first()).isEqualTo(1)
    }

    @Test
    fun testInitTaskOnlyRunsOnce() = doTest {
        val count = AtomicInt()
        val newStore = newDataStore(
            testFile,
            initTasksList = listOf({ _ ->
                count.incrementAndGet()
            })
        )

        repeat(10) {
            newStore.updateData { it.inc() }
            newStore.data.first()
        }

        assertThat(count.get()).isEqualTo(1)
    }

    @Test
    fun testWriteDuringInit() = doTest {
        val continueInit = CompletableDeferred<Unit>()

        store = newDataStore(
            initTasksList = listOf<InitTaskList>({ api ->
                continueInit.await()
                api.updateData { 1 }
            })
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
    fun testCancelDuringInit() = doTest {
        val continueInit = CompletableDeferred<Unit>()

        store = newDataStore(
            initTasksList = listOf<InitTaskList>({ api ->
                continueInit.await()
                api.updateData { 1 }
            })
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
    fun testConcurrentUpdatesInit() = doTest {
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
    fun testUpdateSuccessfullyCommittedInit() = doTest {
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
        assertThrows(testIO.ioExceptionClass()) { store.data.first() }

        serializerConfig.failingWrite = false
        assertThat(store.data.first()).isEqualTo(123)
    }

    @Test
    fun testInitApiUpdateThrowsAfterInitTasksComplete() = doTest {
        var savedApi: InitializerApi<Byte>? = null

        val initializer: suspend (InitializerApi<Byte>) -> Unit = { api ->
            savedApi = api
        }

        val store = newDataStore(initTasksList = listOf(initializer))

        assertThat(store.data.first()).isEqualTo(0)

        assertThrows<IllegalStateException> { savedApi?.updateData { 123 } }
    }

    @Test
    fun testFlowReceivesUpdates() = doTest {
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
    fun testMultipleFlowsReceiveData() = doTest {
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
    fun testExceptionInFlowDoesNotBreakUpstream() = doTest {
        val flowOf8 = store.data.take(8)

        val collectedBytes = mutableListOf<Byte>()

        // Need to give this its own SupervisorJob so this failure doesn't fail the whole test
        val failedFlowCollection = async(SupervisorJob()) {
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
    fun testSlowConsumerDoesntBlockOtherConsumers() = doTest {
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
    fun testHandlerNotCalledGoodData() = doTest {
        coroutineScope {
            newDataStore(file = testFile, scope = this).updateData { 1 }
        }

        coroutineScope {
            val testingHandler: TestingCorruptionHandler = TestingCorruptionHandler()
            val newStore = newDataStore(corruptionHandler = testingHandler, file = testFile)

            newStore.updateData { 2 }
            newStore.data.first()

            assertThat(testingHandler.numCalls.get()).isEqualTo(0)
        }
    }

    @Test
    fun handlerNotCalledNonCorruption() = doTest {
        coroutineScope {
            newDataStore(file = testFile, scope = this).updateData { 1 }
        }

        coroutineScope {
            val testingHandler = TestingCorruptionHandler()
            serializerConfig.failingRead = true
            val newStore = newDataStore(corruptionHandler = testingHandler, file = testFile)

            assertThrows(testIO.ioExceptionClass()) { newStore.updateData { 2 } }
            assertThrows(testIO.ioExceptionClass()) { newStore.data.first() }

            assertThat(testingHandler.numCalls.get()).isEqualTo(0)
        }
    }

    @Test
    fun testHandlerCalledCorruptDataRead() = doTest {
        coroutineScope {
            val newStore = newDataStore(testFile, scope = this)
            newStore.updateData { 1 } // Pre-seed the data so the file exists.
        }

        coroutineScope {
            val testingHandler: TestingCorruptionHandler = TestingCorruptionHandler()
            serializerConfig.failReadWithCorruptionException = true
            val newStore = newDataStore(corruptionHandler = testingHandler, file = testFile)

            assertThrows<IOException> { newStore.data.first() }.hasMessageThat().contains(
                "Handler thrown exception."
            )

            assertThat(testingHandler.numCalls.get()).isEqualTo(1)
        }
    }

    @Test
    fun testHandlerCalledCorruptDataWrite() = doTest {
        coroutineScope {
            val newStore = newDataStore(file = testFile, scope = this)
            newStore.updateData { 1 }
        }

        coroutineScope {
            val testingHandler: TestingCorruptionHandler = TestingCorruptionHandler()
            serializerConfig.failReadWithCorruptionException = true
            val newStore = newDataStore(corruptionHandler = testingHandler, file = testFile)

            assertThrows<IOException> { newStore.updateData { 1 } }.hasMessageThat().contains(
                "Handler thrown exception."
            )

            assertThat(testingHandler.numCalls.get()).isEqualTo(1)
        }
    }

    @Test
    fun testHandlerReplaceData() = doTest {
        coroutineScope {
            newDataStore(file = testFile, scope = this).updateData { 1 }
        }

        coroutineScope {
            val testingHandler: TestingCorruptionHandler =
                TestingCorruptionHandler(replaceWith = 10)
            serializerConfig.failReadWithCorruptionException = true
            val newStore = newDataStore(
                corruptionHandler = testingHandler, file = testFile,
                scope = this
            )

            assertThat(newStore.data.first()).isEqualTo(10)
        }
    }

    @Test
    fun testDefaultValueUsedWhenNoDataOnDisk() = doTest {
        val dataStore = newDataStore(
            serializerConfig = TestingSerializerConfig(defaultValue = 99),
            scope = dataStoreScope)

        assertThat(testFile.delete()).isTrue()

        assertThat(dataStore.data.first()).isEqualTo(99)
    }

    @Test
    fun testTransformRunInCallersContext() = doTest {
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
    fun testCancelInflightWrite() = doTest {
        val myScope = CoroutineScope(Job() + UnconfinedTestDispatcher())

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
    fun testWrite_afterCanceledWrite_succeeds() = doTest {
        val dispatcher = UnconfinedTestDispatcher()
        dispatcher.limitedParallelism(1)
        val myScope = CoroutineScope(coroutineContext + dispatcher)
        val cancelNow = CompletableDeferred<Unit>()

        val coroutine = myScope.launch {
            store.updateData {
                cancelNow.complete(Unit)
                awaitCancellation()
            }
        }

        cancelNow.await()
        coroutine.cancelAndJoin()

        store.updateData { 123 }
    }

    @Test
    fun testWrite_fromOtherScope_doesntGetCancelledFromDifferentScope() = doTest {

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
    fun testCreateDuplicateActiveDataStore() = doTest {
        val file = testIO.newTempFile()
        val dataStore = newDataStore(
            file = file,
            scope = CoroutineScope(Job() + UnconfinedTestDispatcher())
        )

        dataStore.data.first()

        val duplicateDataStore = newDataStore(
            file = file,
            scope = CoroutineScope(Job() + UnconfinedTestDispatcher())
        )

        assertThrows<IllegalStateException> {
            duplicateDataStore.data.first()
        }
    }

    @Test
    fun testCreateDataStore_withSameFileAsInactiveDataStore() = doTest {
        val file = testIO.newTempFile()
        val scope1 = CoroutineScope(coroutineContext + Job())
        val dataStore1 = newDataStore(file = file, scope = scope1)

        dataStore1.data.first()

        scope1.coroutineContext.job.cancelAndJoin()

        val dataStore2 = newDataStore(file = file, scope = CoroutineScope(coroutineContext + Job()))

        // This shouldn't throw an exception bc the scope1 has been cancelled.
        dataStore2.data.first()
    }

    private class TestingCorruptionHandler(
        private val replaceWith: Byte? = null
    ) : CorruptionHandler<Byte> {

        var numCalls = AtomicInt(0)

        override suspend fun handleCorruption(ex: CorruptionException): Byte {
            numCalls.incrementAndGet()

            replaceWith?.let {
                return it
            }

            throw IOException("Handler thrown exception.")
        }
    }

    // Creates a data store at the testFile location and initializes it with a value of -1.
    private suspend fun initDataStore(scope: CoroutineScope) {
        val dataStore = newDataStore(scope = scope)
        dataStore.updateData { -1 }
    }

    private fun newDataStore(
        file: TestFile = testFile,
        serializerConfig: TestingSerializerConfig = this.serializerConfig,
        scope: CoroutineScope = dataStoreScope,
        initTasksList: List<InitTaskList> = listOf(),
        corruptionHandler: CorruptionHandler<Byte> = NoOpCorruptionHandler<Byte>()
    ): DataStore<Byte> {
        return SingleProcessDataStore(
            testIO.getStorage(serializerConfig) { file },
            scope = scope,
            initTasksList = initTasksList,
            corruptionHandler = corruptionHandler
        )
    }
}
private typealias InitTaskList = suspend (api: InitializerApi<Byte>) -> Unit
