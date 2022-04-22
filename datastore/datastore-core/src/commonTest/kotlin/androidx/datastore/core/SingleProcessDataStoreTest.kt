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

import androidx.datastore.core.handlers.NoOpCorruptionHandler
import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import kotlinx.coroutines.CancellationException
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.Volatile
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@ExperimentalCoroutinesApi
class SingleProcessDataStoreTestCommon {

    // TODO add timeout
//    @get:Rule
//    val timeout = Timeout(10, TimeUnit.SECONDS)
    val testIO = TestIO()

    private lateinit var store: DataStore<Byte>
    private lateinit var testingSerializer: TestingSerializer
    private lateinit var dataStoreScope: TestScope
    private lateinit var testScope: TestScope
    private lateinit var testStorage: StorageImpl<Byte>


    @BeforeTest
    fun setUp() {
        testingSerializer = TestingSerializer()
        testStorage = testIO.newFileStorage(testingSerializer)
        dataStoreScope = TestScope(UnconfinedTestDispatcher())
        testScope = TestScope(UnconfinedTestDispatcher())
        store =
            SingleProcessDataStore<Byte>(
                storage = testStorage,
                scope = dataStoreScope
            )
    }

    @Test
    fun testReadNewMessage() = testScope.runTest {
        assertEquals(0, store.data.first())
    }

    @Test
    fun testReadWithNewInstance() = testScope.runTest {
        coroutineScope {
            val newStore = newDataStore(scope = this)
            newStore.updateData { 1 }
        }
        coroutineScope() {
            val newStore = newDataStore(scope = this)
            assertEquals(1, newStore.data.first())
        }
    }

    //Unable to convert these as okio doesn't support file permissions.
//    @Test
//    fun testReadUnreadableFile() = runBlockingTest {
//        testFile.setReadable(false)
//        val result = runCatching {
//            store.data.first()
//        }
//
//        assertThat(result.exceptionOrNull()).isInstanceOf(IOException::class.java)
//        assertThat(result.exceptionOrNull()).hasMessageThat().contains("Permission denied")
//    }
//
//    @Test
//    fun testReadAfterTransientBadRead() = runBlockingTest {
//        testFile.setReadable(false)
//
//        assertThrows<IOException> { store.data.first() }.hasMessageThat()
//            .contains("Permission denied")
//
//        testFile.setReadable(true)
//        assertThat(store.data.first()).isEqualTo(0)
//    }

    @Test
    fun testScopeCancelledWithActiveFlow() = testScope.runTest {
        val newScope = TestScope(UnconfinedTestDispatcher())
        val collection = newScope.async {
            store.data.take(2).collect {
                // Do nothing, this will wait on another element which will never arrive
            }
        }
        newScope.cancel()

        assertTrue { collection.isCompleted }
        assertFalse { collection.isActive }
    }

    @Test
    fun testWriteAndRead() = testScope.runTest {
        store.updateData { 1 }
        assertEquals(1, store.data.first())
    }

    @Test
    fun testWritesDontBlockReadsInSameProcess() = testScope.runTest {
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
        assertEquals(0, store.data.first())

        continueTransform.complete(Unit)
        slowUpdate.await()

        // After update completes, update runs, and read shows new data.
        assertEquals(1, store.data.first())
    }

    @Test
    fun testWriteMultiple() = testScope.runTest {
        store.updateData { 2 }
        store.updateData { it.dec() }

        assertThat(store.data.first()).isEqualTo(1)
    }

    @Test
    fun testReadAfterTransientBadWrite() = testScope.runTest {
        val storage = testIO.newFileStorage(testingSerializer)
        coroutineScope {
            val store = newDataStore(storage, scope = this)
            store.updateData { 1 }
            testingSerializer.failingWrite = true
            assertThrows<IOException> { store.updateData { 2 } }
        }

        coroutineScope {
            val newStore = newDataStore(storage, scope = this)
            assertThat(newStore.data.first()).isEqualTo(1)
        }
    }

    @Test
    fun testWriteToNonExistentDir() = testScope.runTest {
        val fileInNonExistentDir = TestIO("newDir")
            .newFileStorage(testingSerializer)
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
    fun testReadFromNonExistentFile() = testScope.runTest {

        assertThat(testStorage.delete()).isTrue()
        val newStore = newDataStore(testStorage)
        assertThat(newStore.data.first()).isEqualTo(0)
    }

    //    @Test
//    fun testWriteToDirFails() = testScope.runTest {
//        val directoryFile =
//            File(tempFolder.newFolder(), "/this/is/a/directory")
//        directoryFile.mkdirs()
//        assertThat(directoryFile.isDirectory)
//
//        val newStore = newDataStore(directoryFile)
//        assertThrows<IOException> { newStore.data.first() }
//    }
//
    @Test
    fun testExceptionWhenCreatingFilePropagates() = testScope.runTest {
        var failFileProducer = true

        testIO.onProduceFileCallback = {
            if (failFileProducer) {
                throw IOException("Exception when producing file")
            }
        }

        val newStore = SingleProcessDataStore(
            testStorage,
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
    fun testWriteTransformCancellation() = testScope.runTest {
        val transform = CompletableDeferred<Byte>()

        val write = async { store.updateData { transform.await() } }

        assertThat(write.isCompleted).isFalse()

        transform.cancel()

        assertThrows<CancellationException> { write.await() }

        // Check that the datastore's scope is still active:

        assertThat(store.updateData { it.inc().inc() }).isEqualTo(2)
    }

    @Test
    fun testWriteAfterTransientBadRead() = testScope.runTest {
        testingSerializer.failingRead = true

        assertThrows<IOException> { store.data.first() }

        testingSerializer.failingRead = false

        store.updateData { 1 }
        assertThat(store.data.first()).isEqualTo(1)
    }

    @Test
    fun testWriteWithBadReadFails() = testScope.runTest {
        testingSerializer.failingRead = true

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
    fun testCanWriteFromInitTask() = testScope.runTest {
        store = newDataStore(initTasksList = listOf ({ api -> api.updateData { 1 } }))
        val one:Byte = 1

        assertThat(store.data.first()).isEqualTo(one)
    }

    @Test
    fun testInitTaskFailsFirstTimeDueToReadFail() = testScope.runTest {
        store = newDataStore(initTasksList = listOf ({ api -> api.updateData { 1 } }))

        testingSerializer.failingRead = true
        assertThrows<IOException> { store.updateData { 2 } }

        testingSerializer.failingRead = false
        store.updateData { it.inc().inc() }

        assertThat(store.data.first()).isEqualTo(3)
    }

    @Test
    fun testInitTaskFailsFirstTimeDueToException() = testScope.runTest {
        val failInit = AtomicInt(0)
        store = newDataStore(
            initTasksList = listOf( {
                if (failInit.get() == 0) {
                    throw IOException("I was asked to fail init")
                }
            })
        )
        assertThrows<IOException> { store.updateData { 5 } }

        failInit.getAndIncrement()

        store.updateData { it.inc() }
        assertThat(store.data.first()).isEqualTo(1)
    }

    @Test
    fun testInitTaskOnlyRunsOnce() = testScope.runTest {
        val count = AtomicInt(0)
        val newStore = newDataStore(
            initTasksList = listOf ({
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
    fun testWriteDuringInit() = testScope.runTest {
        val continueInit = CompletableDeferred<Unit>()

        store = newDataStore(
            initTasksList = listOf ({ api ->
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
    fun testCancelDuringInit() = testScope.runTest {
        val continueInit = CompletableDeferred<Unit>()

        store = newDataStore(
            initTasksList = listOf ({ api ->
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
    fun testConcurrentUpdatesInit() = testScope.runTest {
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
    fun testUpdateSuccessfullyCommittedInit() = testScope.runTest {
        var otherStorage: Byte = 123

        val initializer: suspend (InitializerApi<Byte>) -> Unit = { api ->
            api.updateData {
                otherStorage
            }
            // Similar to cleanUp():
            otherStorage = 0
        }

        val store = newDataStore(initTasksList = listOf(initializer))

        testingSerializer.failingWrite = true
        assertThrows<IOException> { store.data.first() }

        testingSerializer.failingWrite = false
        assertThat(store.data.first()).isEqualTo(123)
    }

    @Test
    fun testInitApiUpdateThrowsAfterInitTasksComplete() = testScope.runTest {
        var savedApi: InitializerApi<Byte>? = null

        val initializer: suspend (InitializerApi<Byte>) -> Unit = { api ->
            savedApi = api
        }

        val store = newDataStore(initTasksList = listOf(initializer))

        assertThat(store.data.first()).isEqualTo(0)

        assertThrows<IllegalStateException> { savedApi?.updateData { 123 } }
    }

    @Test
    fun testFlowReceivesUpdates() = testScope.runTest {
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
    fun testMultipleFlowsReceiveData() = testScope.runTest {
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

    //coroutine failure issue
//    @Test
//    fun testExceptionInFlowDoesNotBreakUpstream() = testScope.runTest {
//        val flowOf8 = store.data.take(8)
//
//        val collectedBytes = mutableListOf<Byte>()
//
//        val failedFlowCollection = async {
//            flowOf8.collect {
//                throw Exception("Failure while collecting")
//            }
//        }
//
//        val successfulFlowCollection = async {
//            flowOf8.take(8).toList(collectedBytes)
//        }
//
//        repeat(7) {
//            store.updateData { it.inc() }
//        }
//
//        successfulFlowCollection.join()
//        assertThrows<Exception> { failedFlowCollection.await() }.hasMessageThat()
//            .contains("Failure while collecting")
//
//        assertThat(collectedBytes).isEqualTo(mutableListOf<Byte>(0, 1, 2, 3, 4, 5, 6, 7))
//    }

    @Test
    fun testSlowConsumerDoesntBlockOtherConsumers() = testScope.runTest {
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
    fun testHandlerNotCalledGoodData() = testScope.runTest {
        coroutineScope {
            newDataStore(scope = this).updateData { 1 }
        }

        coroutineScope {
            val testingHandler: TestingCorruptionHandler = TestingCorruptionHandler()
            val newStore = newDataStore(corruptionHandler = testingHandler)

            newStore.updateData { 2 }
            newStore.data.first()

            assertThat(testingHandler.numCalls).isEqualTo(0)
        }
    }

    @Test
    fun handlerNotCalledNonCorruption() = testScope.runTest {
        coroutineScope {
            newDataStore(scope = this).updateData { 1 }
        }

        coroutineScope {
            val testingHandler = TestingCorruptionHandler()
            testingSerializer.failingRead = true
            val newStore = newDataStore(corruptionHandler = testingHandler)

            assertThrows<IOException> { newStore.updateData { 2 } }
            assertThrows<IOException> { newStore.data.first() }

            assertThat(testingHandler.numCalls).isEqualTo(0)
        }
    }

    @Test
    fun testHandlerCalledCorruptDataRead() = testScope.runTest {
        coroutineScope {
            val newStore = newDataStore(scope = this)
            newStore.updateData { 1 } // Pre-seed the data so the file exists.
        }

        coroutineScope {
            val testingHandler: TestingCorruptionHandler = TestingCorruptionHandler()
            testingSerializer.failReadWithCorruptionException = true
            val newStore = newDataStore(corruptionHandler = testingHandler)

            assertThrows<IOException> { newStore.data.first() }.hasMessageThat().contains(
                "Handler thrown exception."
            )

            assertThat(testingHandler.numCalls).isEqualTo(1)
        }
    }

    @Test
    fun testHandlerCalledCorruptDataWrite() = testScope.runTest {
        coroutineScope {
            val newStore = newDataStore(scope = this)
            newStore.updateData { 1 }
        }

        coroutineScope {
            val testingHandler: TestingCorruptionHandler = TestingCorruptionHandler()
            testingSerializer.failReadWithCorruptionException = true
            val newStore = newDataStore(corruptionHandler = testingHandler)

            assertThrows<IOException> { newStore.updateData { 1 } }.hasMessageThat().contains(
                "Handler thrown exception."
            )

            assertThat(testingHandler.numCalls).isEqualTo(1)
        }
    }

    @Test
    fun testHandlerReplaceData() = testScope.runTest {
        coroutineScope {
            newDataStore(scope = this).updateData { 1 }
        }

        coroutineScope {
            val testingHandler: TestingCorruptionHandler =
                TestingCorruptionHandler(replaceWith = 10)
            testingSerializer.failReadWithCorruptionException = true
            val newStore = newDataStore(
                corruptionHandler = testingHandler,
                scope = this
            )

            assertThat(newStore.data.first()).isEqualTo(10)
        }
    }

    @Test
    fun testMutatingDataStoreFails() = testScope.runTest {

        val dataStore = DataStoreFactory.create(
            testIO.newFileStorage( ByteWrapper.ByteWrapperSerializer()),
            scope = dataStoreScope
        )

        assertThrows<IllegalStateException> {
            dataStore.updateData { input: ByteWrapper ->
                // mutating our wrapper causes us to fail
                input.byte = 123.toByte()
                input
            }
        }
    }

    @Test
    fun testDefaultValueUsedWhenNoDataOnDisk() = testScope.runTest {
        val storage = testIO.newFileStorage(TestingSerializer(defaultValue = 99))
        val dataStore = DataStoreFactory.create(
            storage,
            scope = dataStoreScope
        )

        assertThat(storage.delete()).isTrue()

        assertThat(dataStore.data.first()).isEqualTo(99)
    }

    // can't call close cause doesn't exist in common
//    @Test
//    fun testClosingOutputStreamDoesntCloseUnderlyingStream() = testScope.runTest {
//        val delegate = TestingSerializer()
//        val serializer = object : Serializer<Byte> by delegate {
//            override suspend fun writeTo(t: Byte, output: OutputStream) {
//                delegate.writeTo(t, output)
//                output.close() // This will be a no-op so the fd.sync() call will succeed.
//            }
//        }
//
//        val dataStore = newDataStore(serializer = serializer)
//
//        // Shouldn't throw:
//        dataStore.data.first()
//
//        // Shouldn't throw:
//        dataStore.updateData { it.inc() }
//    }

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

    //figure out common coroutine equiv
//    @Test
//    fun testCancelInflightWrite() = runBlocking<Unit> {
//        val myScope =
//            CoroutineScope(Job() + Executors.newSingleThreadExecutor().asCoroutineDispatcher())
//
//        val updateStarted = CompletableDeferred<Unit>()
//        myScope.launch {
//            store.updateData {
//                updateStarted.complete(Unit)
//                awaitCancellation()
//            }
//        }
//        updateStarted.await()
//        myScope.coroutineContext[Job]!!.cancelAndJoin()
//    }
//
//    @Test
//    fun testWrite_afterCanceledWrite_succeeds() = runBlocking<Unit> {
//        val myScope =
//            CoroutineScope(Job() + Executors.newSingleThreadExecutor().asCoroutineDispatcher())
//
//        val cancelNow = CompletableDeferred<Unit>()
//
//        myScope.launch {
//            store.updateData {
//                cancelNow.complete(Unit)
//                awaitCancellation()
//            }
//        }
//
//        cancelNow.await()
//        myScope.coroutineContext[Job]!!.cancelAndJoin()
//
//        store.updateData { 123 }
//    }
//
    @Test
    fun testWrite_fromOtherScope_doesntGetCancelledFromDifferentScope() = runBlocking<Unit> {

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

//    @Test
//    fun testCreateDuplicateActiveDataStore() = runBlocking<Unit> {
//        val file = tempFolder.newFile()
//        val dataStore = newDataStore(file = file, scope = CoroutineScope(Job()))
//
//        dataStore.data.first()
//
//        val duplicateDataStore = newDataStore(file = file, scope = CoroutineScope(Job()))
//
//        assertThrows<IllegalStateException> {
//            duplicateDataStore.data.first()
//        }
//    }
//
//    @Test
//    fun testCreateDataStore_withSameFileAsInactiveDataStore() = runBlocking<Unit> {
//        val file = tempFolder.newFile()
//        val scope1 = CoroutineScope(Job())
//        val dataStore1 = newDataStore(file = file, scope = scope1)
//
//        dataStore1.data.first()
//
//        scope1.coroutineContext.job.cancelAndJoin()
//
//        val dataStore2 = newDataStore(file = file, scope = CoroutineScope(Job()))
//
//        // This shouldn't throw an exception bc the scope1 has been cancelled.
//        dataStore2.data.first()
//    }

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

    private fun newDataStore(
        storage: Storage<Byte> = testStorage,
        scope: CoroutineScope = dataStoreScope,
        initTasksList: List<suspend (api: InitializerApi<Byte>) -> Unit> = listOf(),
        corruptionHandler: CorruptionHandler<Byte> = NoOpCorruptionHandler<Byte>()
    ): DataStore<Byte> {
        return SingleProcessDataStore(
            storage,
            scope = scope,
            initTasksList = initTasksList,
            corruptionHandler = corruptionHandler
        )
    }
}
