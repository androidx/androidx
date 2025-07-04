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

package androidx.datastore.core

import androidx.datastore.TestFile
import androidx.datastore.TestIO
import androidx.datastore.TestingSerializerConfig
import androidx.datastore.core.UpdatingDataContextElement.Companion.NESTED_UPDATE_ERROR_MESSAGE
import androidx.datastore.core.handlers.NoOpCorruptionHandler
import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
abstract class SingleProcessDataStoreTest<F : TestFile<F>>(private val testIO: TestIO<F, *>) {

    protected lateinit var store: DataStore<Byte>
    private lateinit var serializerConfig: TestingSerializerConfig
    protected lateinit var testFile: F
    private lateinit var tempFolder: F
    protected lateinit var dataStoreScope: TestScope

    @BeforeTest
    fun setUp() {
        serializerConfig = TestingSerializerConfig()
        tempFolder = testIO.newTempFile().also { it.mkdirs() }
        testFile = testIO.newTempFile(parentFile = tempFolder)
        dataStoreScope = TestScope(UnconfinedTestDispatcher() + Job())
        store =
            testIO.getStore(
                serializerConfig,
                dataStoreScope,
                { createSingleProcessCoordinator(testFile.path()) },
            ) {
                testFile
            }
    }

    // Creates a data store at the testFile location and initializes it with a value of -1.
    private fun initAndCloseDatastore() {
        // running this separately to ensure the DS it is closed after initialization
        runTest {
            val dataStore = newDataStore(scope = backgroundScope)
            dataStore.updateData { -1 }
        }
    }

    @Test fun testReadNewMessage() = runTest { assertThat(store.data.first()).isEqualTo(0) }

    @Test
    fun testReadWithNewInstance() {
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
        val store =
            testIO.getStore(
                serializerConfig,
                storeScope,
                { createSingleProcessCoordinator(testFile.path()) },
            ) {
                testFile
            }

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
        store.updateData { it.dec() }

        assertThat(store.data.first()).isEqualTo(1)
    }

    @Test
    fun testReadAfterTransientBadWrite() {
        val file = testIO.newTempFile()
        runTest {
            val store = newDataStore(file = file, scope = backgroundScope)
            store.updateData { 1 }
            serializerConfig.failingWrite = true
            assertThrows(testIO.ioExceptionClass()) { store.updateData { 2 } }
        }

        runTest {
            val newStore = newDataStore(file, scope = backgroundScope)
            assertThat(newStore.data.first()).isEqualTo(1)
        }
    }

    @Test
    fun testWriteToNonExistentDir() {
        val fileInNonExistentDir = testIO.newTempFile(relativePath = "this/does/not/exist")

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
        val directoryFile =
            testIO.newTempFile(relativePath = "/this/is/a/directory").also {
                it.mkdirs(mustCreate = true)
            }
        assertThat(directoryFile.isDirectory()).isTrue()

        val newStore = newDataStore(directoryFile)
        assertThrows(testIO.ioExceptionClass()) { newStore.data.first() }
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
        val newStore =
            testIO.getStore(
                serializerConfig,
                dataStoreScope,
                { createSingleProcessCoordinator(testFile.path()) },
                fileProducer,
            )

        assertThrows<IOException> { newStore.data.first() }
            .hasMessageThat()
            .isEqualTo("Exception when producing file")

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
        initAndCloseDatastore()
        serializerConfig.failingRead = true

        assertThrows(testIO.ioExceptionClass()) { store.data.first() }

        serializerConfig.failingRead = false

        store.updateData { 1 }
        assertThat(store.data.first()).isEqualTo(1)
    }

    @Test
    fun testWriteWithBadReadFails() = runTest {
        initAndCloseDatastore()
        serializerConfig.failingRead = true

        assertThrows(testIO.ioExceptionClass()) { store.updateData { 1 } }
    }

    @Test
    fun testCancellingDataStoreScopePropagatesToWrites() = runTest {
        val scope = CoroutineScope(Job())
        val store = newDataStore(scope = scope)
        val latch = CompletableDeferred<Unit>()

        val slowUpdate = async {
            store.updateData {
                latch.await()
                it.inc()
            }
        }

        val notStartedUpdate = async { store.updateData { it.inc() } }

        scope.cancel()

        assertThrows<CancellationException> { slowUpdate.await() }

        assertThrows<CancellationException> { notStartedUpdate.await() }

        assertThrows<CancellationException> { store.updateData { 123 } }
    }

    @Test
    fun testCancellingCallerScopePropagatesToWrites() = runTest {
        val dsScope = CoroutineScope(Job())
        val callerScope = CoroutineScope(Job())

        val dataStore = newDataStore(scope = dsScope)

        val latch = CompletableDeferred<Unit>()

        // The ordering of the following are not guaranteed but I think they won't be flaky with
        // Dispatchers.Unconfined
        val awaitingCancellation =
            callerScope.async(Dispatchers.Unconfined) {
                dataStore.updateData { awaitCancellation() }
            }

        dsScope.launch(Dispatchers.Unconfined) {
            dataStore.updateData {
                latch.await()
                it.inc()
            }
        }

        val notStarted =
            callerScope.async(Dispatchers.Unconfined) { dataStore.updateData { it.inc() } }

        callerScope.coroutineContext.job.cancelAndJoin()

        assertThat(awaitingCancellation.isCancelled).isTrue()
        assertThat(notStarted.isCancelled).isTrue()
    }

    @Test
    fun testCanWriteFromInitTask() = runTest {
        store = newDataStore(initTasksList = listOf({ api -> api.updateData { 1 } }))

        assertThat(store.data.first()).isEqualTo(1)
    }

    @Test
    fun testInitTaskFailsFirstTimeDueToReadFail() = runTest {
        initAndCloseDatastore()
        store = newDataStore(initTasksList = listOf({ api -> api.updateData { 1 } }))

        serializerConfig.failingRead = true
        assertThrows(testIO.ioExceptionClass()) { store.updateData { 2 } }

        serializerConfig.failingRead = false
        store.updateData { it.inc().inc() }

        assertThat(store.data.first()).isEqualTo(3)
    }

    @Test
    fun testInitTaskFailsFirstTimeDueToException() = runTest {
        val failInit = AtomicBoolean(true)
        store =
            newDataStore(
                initTasksList =
                    listOf({ _ ->
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
    fun testInitTaskOnlyRunsOnce() = runTest {
        val count = AtomicInt()
        val newStore =
            newDataStore(testFile, initTasksList = listOf({ _ -> count.incrementAndGet() }))

        repeat(10) {
            newStore.updateData { it.inc() }
            newStore.data.first()
        }

        assertThat(count.get()).isEqualTo(1)
    }

    @Test
    fun testWriteDuringInit() = runTest {
        val continueInit = CompletableDeferred<Unit>()

        store =
            newDataStore(
                initTasksList =
                    listOf({ api ->
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
    fun testCancelDuringInit() = runTest {
        val continueInit = CompletableDeferred<Unit>()

        store =
            newDataStore(
                initTasksList =
                    listOf({ api ->
                        continueInit.await()
                        api.updateData { 1 }
                    })
            )

        val update = async { store.updateData { it } }

        val read = async { store.data.first() }

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
            api.updateData { it.inc() }
            update1.await()
        }

        store = newDataStore(initTasksList = listOf(concurrentUpdateInitializer))
        val getData = async { store.data.first() }
        continueUpdate.complete(Unit)

        assertThat(getData.await()).isEqualTo(3)
    }

    @Test
    fun testUpdateSuccessfullyCommittedInit() = runTest {
        var otherStorage: Byte = 123

        val initializer: suspend (InitializerApi<Byte>) -> Unit = { api ->
            api.updateData { otherStorage }
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
    fun testInitApiUpdateThrowsAfterInitTasksComplete() = runTest {
        var savedApi: InitializerApi<Byte>? = null

        val initializer: suspend (InitializerApi<Byte>) -> Unit = { api -> savedApi = api }

        val store = newDataStore(initTasksList = listOf(initializer))

        assertThat(store.data.first()).isEqualTo(0)

        assertThrows<IllegalStateException> { savedApi?.updateData { 123 } }
    }

    @Test
    fun testFlowReceivesUpdates() = runTest {
        val collectedBytes = mutableListOf<Byte>()

        val flowCollectionJob = async { store.data.take(8).toList(collectedBytes) }
        runCurrent()

        repeat(7) { store.updateData { it.inc() } }

        flowCollectionJob.join()

        assertThat(collectedBytes).isEqualTo(mutableListOf<Byte>(0, 1, 2, 3, 4, 5, 6, 7))
    }

    @Test
    fun testMultipleFlowsReceiveData() = runTest {
        val flowOf8 = store.data.take(8)

        val bytesFromFirstCollect = mutableListOf<Byte>()
        val bytesFromSecondCollect = mutableListOf<Byte>()

        val flowCollection1 = async { flowOf8.toList(bytesFromFirstCollect) }

        val flowCollection2 = async { flowOf8.toList(bytesFromSecondCollect) }

        runCurrent()
        repeat(7) { store.updateData { it.inc() } }

        flowCollection1.join()
        flowCollection2.join()

        assertThat(bytesFromFirstCollect).isEqualTo(mutableListOf<Byte>(0, 1, 2, 3, 4, 5, 6, 7))
        assertThat(bytesFromSecondCollect).isEqualTo(mutableListOf<Byte>(0, 1, 2, 3, 4, 5, 6, 7))
    }

    @Test
    fun testExceptionInFlowDoesNotBreakUpstream() = runTest {
        val flowOf8 = store.data.take(8)

        val collectedBytes = mutableListOf<Byte>()

        // Need to give this its own SupervisorJob so this failure doesn't fail the whole test
        val failedFlowCollection =
            async(SupervisorJob()) {
                flowOf8.collect { throw Exception("Failure while collecting") }
            }

        val successfulFlowCollection = async { flowOf8.take(8).toList(collectedBytes) }

        runCurrent()
        repeat(7) { store.updateData { it.inc() } }

        successfulFlowCollection.join()
        assertThrows<Exception> { failedFlowCollection.await() }
            .hasMessageThat()
            .contains("Failure while collecting")

        assertThat(collectedBytes).isEqualTo(mutableListOf<Byte>(0, 1, 2, 3, 4, 5, 6, 7))
    }

    @Test
    fun testSlowConsumerDoesntBlockOtherConsumers() = runTest {
        val flowOf8 = store.data.take(8)

        val collectedBytes = mutableListOf<Byte>()

        val flowCollection2 = async { flowOf8.toList(collectedBytes) }

        val blockedCollection = async { flowOf8.collect { flowCollection2.await() } }
        runCurrent()
        repeat(15) { store.updateData { it.inc() } }

        flowCollection2.await()
        assertThat(collectedBytes).isEqualTo(mutableListOf<Byte>(0, 1, 2, 3, 4, 5, 6, 7))

        blockedCollection.await()
    }

    @Test
    fun testHandlerNotCalledGoodData() {
        runTest { newDataStore(file = testFile, scope = backgroundScope).updateData { 1 } }

        runTest {
            val testingHandler = TestingCorruptionHandler()
            val newStore = newDataStore(corruptionHandler = testingHandler, file = testFile)

            newStore.updateData { 2 }
            newStore.data.first()

            assertThat(testingHandler.numCalls.get()).isEqualTo(0)
        }
    }

    @Test
    fun handlerNotCalledNonCorruption() {
        runTest { newDataStore(file = testFile, scope = backgroundScope).updateData { 1 } }

        runTest {
            val testingHandler = TestingCorruptionHandler()
            serializerConfig.failingRead = true
            val newStore = newDataStore(corruptionHandler = testingHandler, file = testFile)

            assertThrows(testIO.ioExceptionClass()) { newStore.updateData { 2 } }
            assertThrows(testIO.ioExceptionClass()) { newStore.data.first() }

            assertThat(testingHandler.numCalls.get()).isEqualTo(0)
        }
    }

    @Test
    fun testHandlerCalledCorruptDataRead() {
        runTest {
            val newStore = newDataStore(testFile, scope = backgroundScope)
            newStore.updateData { 1 } // Pre-seed the data so the file exists.
        }

        runTest {
            val testingHandler = TestingCorruptionHandler()
            serializerConfig.failReadWithCorruptionException = true
            val newStore = newDataStore(corruptionHandler = testingHandler, file = testFile)

            assertThrows<IOException> { newStore.data.first() }
                .hasMessageThat()
                .contains("Handler thrown exception.")

            assertThat(testingHandler.numCalls.get()).isEqualTo(1)
        }
    }

    @Test
    fun testHandlerCalledCorruptDataWrite() {
        runTest {
            val newStore = newDataStore(file = testFile, scope = backgroundScope)
            newStore.updateData { 1 }
        }

        runTest {
            val testingHandler = TestingCorruptionHandler()
            serializerConfig.failReadWithCorruptionException = true
            val newStore = newDataStore(corruptionHandler = testingHandler, file = testFile)

            assertThrows<IOException> { newStore.updateData { 1 } }
                .hasMessageThat()
                .contains("Handler thrown exception.")

            assertThat(testingHandler.numCalls.get()).isEqualTo(1)
        }
    }

    @Test
    fun testHandlerReplaceData() {
        runTest { newDataStore(file = testFile, scope = backgroundScope).updateData { 1 } }

        runTest {
            val testingHandler = TestingCorruptionHandler(replaceWith = 10)
            serializerConfig.failReadWithCorruptionException = true
            val newStore =
                newDataStore(
                    corruptionHandler = testingHandler,
                    file = testFile,
                    scope = backgroundScope,
                )

            assertThat(newStore.data.first()).isEqualTo(10)
        }
    }

    @Test
    fun testDefaultValueUsedWhenNoDataOnDisk() = runTest {
        val dataStore =
            newDataStore(
                serializerConfig = TestingSerializerConfig(defaultValue = 99),
                scope = dataStoreScope,
            )

        assertThat(dataStore.data.first()).isEqualTo(99)
    }

    @Test
    fun testTransformRunInCallersContext() = runTest {
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

    private class TestElement(val name: String) : AbstractCoroutineContextElement(Key) {
        companion object Key : CoroutineContext.Key<TestElement>
    }

    @Test
    fun testCancelInflightWrite() = runTest {
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
    fun testWrite_afterCanceledWrite_succeeds() = runTest {
        val dispatcher = UnconfinedTestDispatcher()
        dispatcher.limitedParallelism(1)
        val myScope = CoroutineScope(coroutineContext + dispatcher)
        val cancelNow = CompletableDeferred<Unit>()

        val coroutine =
            myScope.launch {
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
    fun testWrite_fromOtherScope_doesntGetCancelledFromDifferentScope() = runTest {
        val otherScope = CoroutineScope(Job())

        val callerScope = CoroutineScope(Job())

        val firstUpdateStarted = CompletableDeferred<Unit>()
        val finishFirstUpdate = CompletableDeferred<Byte>()

        val firstUpdate =
            otherScope.async(Dispatchers.Unconfined) {
                store.updateData {
                    firstUpdateStarted.complete(Unit)
                    finishFirstUpdate.await()
                }
            }

        callerScope.launch(Dispatchers.Unconfined) { store.updateData { awaitCancellation() } }

        firstUpdateStarted.await()
        callerScope.coroutineContext.job.cancelAndJoin()
        finishFirstUpdate.complete(1)
        firstUpdate.await()

        // It's still usable:
        assertThat(store.updateData { it.inc() }).isEqualTo(2)
    }

    @Test
    fun testCreateDuplicateActiveDataStore() = runTest {
        val datastoreFile = testIO.newTempFile()
        @Suppress("UNUSED_VARIABLE") // keep it in memory
        val original =
            newDataStore(
                    file = datastoreFile,
                    scope = CoroutineScope(Job() + UnconfinedTestDispatcher()),
                )
                .also { it.data.first() }

        suspend fun DataStore<Byte>.assertFailsToOpen() {
            assertThrows<IllegalStateException> { data.first() }
                .hasMessageThat()
                .contains("There are multiple DataStores active for the same file")
        }

        newDataStore(
                file = datastoreFile,
                scope = CoroutineScope(Job() + UnconfinedTestDispatcher()),
            )
            .also { it.assertFailsToOpen() }

        newDataStore(
                file = datastoreFile.resolve("../${datastoreFile.name}"),
                scope = CoroutineScope(Job() + UnconfinedTestDispatcher()),
            )
            .also { it.assertFailsToOpen() }

        newDataStore(
                file = datastoreFile.resolve(".././${datastoreFile.name}"),
                scope = CoroutineScope(Job() + UnconfinedTestDispatcher()),
            )
            .also { it.assertFailsToOpen() }

        newDataStore(
                file = datastoreFile.resolve("../nonExisting/../${datastoreFile.name}"),
                scope = CoroutineScope(Job() + UnconfinedTestDispatcher()),
            )
            .also { it.assertFailsToOpen() }
        // in different folder, hence can read
        newDataStore(
                file = datastoreFile.resolve("../newFolder/${datastoreFile.name}"),
                scope = CoroutineScope(Job() + UnconfinedTestDispatcher()),
            )
            .also { it.data.first() }
    }

    @Test
    fun testCreateDataStore_withSameFileAsInactiveDataStore() = runTest {
        val file = testIO.newTempFile()
        val scope1 = CoroutineScope(coroutineContext + Job())
        val dataStore1 = newDataStore(file = file, scope = scope1)

        dataStore1.data.first()

        scope1.coroutineContext.job.cancelAndJoin()

        val dataStore2 = newDataStore(file = file, scope = CoroutineScope(coroutineContext + Job()))

        // This shouldn't throw an exception bc the scope1 has been cancelled.
        dataStore2.data.first()
    }

    /** test that if read fails, all collectors are notified with it. */
    @Test
    fun readFailsAfter_successfulUpdate() = runTest {
        val asyncCollector =
            async(coroutineContext + Job()) {
                // this uses a separate independent job not to cancel the test scope when
                // the expected exception happens
                store.data.collect()
            }
        store.updateData { 2 }
        // update the version so the next read thinks the data has changed.
        // ideally, this test should create another datastore instance that will change the data but
        // we don't allow multiple instances on the same file so this is an easy workaround to
        // create the test case.
        store.incrementSharedCounter()
        serializerConfig.failingRead = true
        // trigger read
        assertThrows(testIO.ioExceptionClass()) { store.data.first() }
        runCurrent()
        // should cancel due to exception
        assertThat(asyncCollector.isCancelled).isTrue()
        // recover from failure
        serializerConfig.failingRead = false
        assertThat(store.data.first()).isEqualTo(2)
    }

    /**
     * test that failed updateData calls do not affect the cache or do not affect other collectors
     */
    @Test
    fun readFailsAfter_failedUpdate() = runTest {
        // fill cache
        store.data.first()
        serializerConfig.failingWrite = true
        val asyncCollector = async { store.data.collect() }
        // set cache to failure
        assertThrows(testIO.ioExceptionClass()) { store.updateData { 3 } }
        runCurrent()
        // existing collector does not get an error due to failed write
        assertThat(asyncCollector.isActive).isTrue()
        assertThat(store.data.first()).isEqualTo(0)
        asyncCollector.cancelAndJoin()
    }

    @Test
    fun finalValueIsReceived() = runTest {
        val datastoreScope = TestScope()
        val store =
            newDataStore(file = testIO.newTempFile(), scope = datastoreScope.backgroundScope)

        suspend fun <R> runAndPumpInStore(block: suspend () -> R): R {
            val async = datastoreScope.async { block() }
            datastoreScope.runCurrent()
            check(async.isCompleted) { "Async block did not complete." }
            return async.await()
        }
        runAndPumpInStore { store.updateData { 2 } }
        val asyncCollector = async { store.data.toList() }
        datastoreScope.runCurrent()
        runCurrent()
        assertThat(asyncCollector.isActive).isTrue()
        runAndPumpInStore { store.updateData { 3 } }
        datastoreScope.runCurrent()
        runCurrent()

        assertThat(asyncCollector.isActive).isTrue()
        // finalize the store
        runAndPumpInStore { datastoreScope.backgroundScope.coroutineContext[Job]!!.cancelAndJoin() }
        datastoreScope.runCurrent()
        runCurrent()
        assertThat(asyncCollector.isActive).isFalse()
        assertThat(asyncCollector.await()).containsExactly(2.toByte(), 3.toByte()).inOrder()
    }

    @Test
    fun testCancelledDataStoreScopeCantRead() = runTest {
        // TODO(b/273990827): decide the contract of accessing when state is Final
        dataStoreScope.cancel()

        val flowCollector = async { store.data.toList() }
        runCurrent()
        assertThrows<CancellationException> { flowCollector.await() }

        assertThrows<CancellationException> { store.data.first() }
    }

    @Test
    fun observeFileOnlyWhenDatastoreIsObserved() = runTest {
        class InterProcessCoordinatorWithInfiniteUpdates(
            val delegate: InterProcessCoordinator,
            val observerCount: MutableStateFlow<Int> = MutableStateFlow(0),
        ) : InterProcessCoordinator by delegate {
            override val updateNotifications: Flow<Unit>
                get() {
                    return flow<Unit> {
                            // never emit but never finish either so we know when we are being
                            // collected
                            suspendCancellableCoroutine {}
                        }
                        .onStart { observerCount.update { it + 1 } }
                        .onCompletion { observerCount.update { it - 1 } }
                }
        }
        val observerCount = MutableStateFlow(0)
        store =
            testIO.getStore(
                serializerConfig,
                dataStoreScope,
                {
                    InterProcessCoordinatorWithInfiniteUpdates(
                        delegate = createSingleProcessCoordinator(testFile.path()),
                        observerCount = observerCount,
                    )
                },
            ) {
                testFile
            }
        fun hasObservers(): Boolean {
            runCurrent()
            return observerCount.value > 0
        }
        assertThat(hasObservers()).isFalse()
        val collector1 = async { store.data.collect {} }
        runCurrent()
        assertThat(hasObservers()).isTrue()
        val collector2 = async { store.data.collect {} }
        assertThat(hasObservers()).isTrue()
        collector1.cancelAndJoin()
        assertThat(hasObservers()).isTrue()
        collector2.cancelAndJoin()
        assertThat(hasObservers()).isFalse()
        val collector3 = async { store.data.collect {} }
        assertThat(hasObservers()).isTrue()
        collector3.cancelAndJoin()
        assertThat(hasObservers()).isFalse()
    }

    @Test
    fun nestedUpdateCallsShouldntDeadlock() = runTest {
        val result =
            store.updateData {
                assertThrows<IllegalStateException> {
                        store.updateData {
                            // won't execute
                            2.toByte()
                        }
                    }
                    .hasMessageThat()
                    .isEqualTo(NESTED_UPDATE_ERROR_MESSAGE)
                1.toByte()
            }
        assertThat(result).isEqualTo(1.toByte())
        assertThat(store.data.first()).isEqualTo(1.toByte())
        // write again, make sure we allow new transactions
        store.updateData { 3.toByte() }
        assertThat(store.data.first()).isEqualTo(3.toByte())
    }

    @Test
    fun nestedUpdatesInDifferentStores() = runTest {
        val testFile2 = testIO.newTempFile(parentFile = tempFolder)
        val store2 =
            testIO.getStore(
                serializerConfig,
                dataStoreScope,
                { createSingleProcessCoordinator(testFile2.path()) },
            ) {
                testFile2
            }
        store.updateData {
            store2.updateData { 2.toByte() }
            1.toByte()
        }
        assertThat(store.data.first()).isEqualTo(1.toByte())
        assertThat(store2.data.first()).isEqualTo(2.toByte())
    }

    @Test
    fun nestedUpdatesInDifferentStores_fail() = runTest {
        val testFile2 = testIO.newTempFile(parentFile = tempFolder)
        val store2 =
            testIO.getStore(
                serializerConfig,
                dataStoreScope,
                { createSingleProcessCoordinator(testFile2.path()) },
            ) {
                testFile2
            }
        store.updateData {
            store2.updateData {
                assertThrows<IllegalStateException> {
                        // we are in a nested transaction from store, hence this won't be allowed
                        store.updateData { 3.toByte() }
                    }
                    .hasMessageThat()
                    .isEqualTo(NESTED_UPDATE_ERROR_MESSAGE)
                2.toByte()
            }
            1.toByte()
        }
        assertThat(store.data.first()).isEqualTo(1.toByte())
        assertThat(store2.data.first()).isEqualTo(2.toByte())
    }

    @Test
    fun testReadHandlesCorruptionAfterInit() {
        runTest {
            val corruptionHandler = TestingCorruptionHandler(replaceWith = 3.toByte())
            val newStore = newDataStore(testFile, corruptionHandler = corruptionHandler)

            // create the file to prevent serializer from returning default value
            newStore.updateData { 1 }
            assertThat(newStore.data.first()).isEqualTo(1)

            // increment version to force non-cached read which gets automatically reset from a
            // [CorruptionException], the current state is [Data]
            serializerConfig.failReadWithCorruptionException = true
            serializerConfig.defaultValue = 2
            newStore.incrementSharedCounter()
            assertThat(newStore.data.first()).isEqualTo(3.toByte())
            assertThat(corruptionHandler.numCalls.get()).isEqualTo(1)
        }
    }

    @Test
    fun testUpdateHandlesCorruptionAfterInit() {
        runTest {
            val corruptionHandler = TestingCorruptionHandler(replaceWith = 3.toByte())
            val newStore = newDataStore(testFile, corruptionHandler = corruptionHandler)

            // create the file to prevent serializer from returning default value
            newStore.updateData { 1 }
            assertThat(newStore.data.first()).isEqualTo(1)

            // increment version to force non-cached read which gets [CorruptionException], the
            // current state is [Data]
            serializerConfig.failReadWithCorruptionException = true
            serializerConfig.defaultValue = 2
            newStore.incrementSharedCounter()
            assertThat(newStore.updateData { it.inc() }).isEqualTo(4)
            assertThat(corruptionHandler.numCalls.get()).isEqualTo(1)
        }
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

    private class TestingCorruptionHandler(private val replaceWith: Byte? = null) :
        CorruptionHandler<Byte> {

        var numCalls = AtomicInt(0)

        override suspend fun handleCorruption(ex: CorruptionException): Byte {
            numCalls.incrementAndGet()

            replaceWith?.let {
                return it
            }

            throw IOException("Handler thrown exception.")
        }
    }

    private fun newDataStore(
        file: F = testFile,
        serializerConfig: TestingSerializerConfig = this.serializerConfig,
        scope: CoroutineScope = dataStoreScope,
        initTasksList: List<InitTaskList> = listOf(),
        corruptionHandler: CorruptionHandler<Byte> = NoOpCorruptionHandler(),
    ): DataStore<Byte> {
        return DataStoreImpl(
            testIO.getStorage(serializerConfig, { createSingleProcessCoordinator(file.path()) }) {
                file
            },
            scope = scope,
            initTasksList = initTasksList,
            corruptionHandler = corruptionHandler,
        )
    }
}

private typealias InitTaskList = suspend (api: InitializerApi<Byte>) -> Unit

/** Utility method to increment shared counter using internal APIs. */
private suspend fun <T> DataStore<T>.incrementSharedCounter() {
    val coordinator = (this as DataStoreImpl).storageConnection.coordinator
    val currentVersion = coordinator.getVersion()
    assertThat(coordinator.incrementAndGetVersion()).isEqualTo(currentVersion + 1)
}
