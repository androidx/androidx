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

import android.content.Context
import android.os.Bundle
import androidx.datastore.core.handlers.NoOpCorruptionHandler
import androidx.test.core.app.ApplicationProvider
import androidx.testing.TestMessageProto.FooProto
import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ExtensionRegistryLite
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import kotlin.jvm.Throws
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

private const val PATH_BUNDLE_KEY: String = "path"
private val PROTO_SERIALIZER: Serializer<FooProto> = ProtoSerializer<FooProto>(
    FooProto.getDefaultInstance(),
    ExtensionRegistryLite.getEmptyRegistry()
)
private const val TEST_TEXT: String = "abc"
internal val WRITE_TEXT: (FooProto) -> FooProto = { f: FooProto ->
    f.toBuilder().setText(TEST_TEXT).build()
}
private val WRITE_BOOLEAN: (FooProto) -> FooProto = { f: FooProto ->
    f.toBuilder().setBoolean(true).build()
}
private val INCREMENT_INTEGER: (FooProto) -> FooProto = { f: FooProto ->
    f.toBuilder().setInteger(f.getInteger() + 1).build()
}

private val DEFAULT_FOO: FooProto = FooProto.getDefaultInstance()
private val FOO_WITH_TEXT: FooProto =
    FooProto.newBuilder().setText(TEST_TEXT).build()
private val FOO_WITH_TEXT_AND_BOOLEAN: FooProto =
    FooProto.newBuilder().setText(TEST_TEXT).setBoolean(true).build()

@ExperimentalCoroutinesApi
private fun createDataStore(
    bundle: Bundle,
    scope: TestScope,
    corruptionHandler: CorruptionHandler<FooProto> = NoOpCorruptionHandler<FooProto>()
): MultiProcessDataStore<FooProto> {
    val produceFile = { File(bundle.getString(PATH_BUNDLE_KEY)!!) }
    return MultiProcessDataStore<FooProto>(
        storage = FileStorage(PROTO_SERIALIZER, produceFile),
        scope = scope,
        corruptionHandler = corruptionHandler,
        produceFile = produceFile
    )
}

@OptIn(DelicateCoroutinesApi::class)
@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class MultiProcessDataStoreMultiProcessTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var testFile: File
    private lateinit var dataStoreScope: TestScope

    private val TAG = "MPDS test"

    private val protoSerializer: Serializer<FooProto> = ProtoSerializer<FooProto>(
        FooProto.getDefaultInstance(),
        ExtensionRegistryLite.getEmptyRegistry()
    )
    private val mainContext: Context = ApplicationProvider.getApplicationContext()

    private fun createDataStoreBundle(path: String): Bundle {
        val data = Bundle()
        data.putString(PATH_BUNDLE_KEY, path)
        return data
    }

    internal fun createDataStore(
        bundle: Bundle,
        scope: TestScope
    ): MultiProcessDataStore<FooProto> {
        val produceFile = { File(bundle.getString(PATH_BUNDLE_KEY)!!) }
        return MultiProcessDataStore<FooProto>(
            storage = FileStorage(protoSerializer, produceFile),
            scope = scope,
            produceFile = produceFile
        )
    }

    @Before
    fun setUp() {
        testFile = tempFolder.newFile()
        dataStoreScope = TestScope(UnconfinedTestDispatcher() + Job())
    }

    @Test
    fun testSimpleUpdateData() = runTest {
        val testData: Bundle = createDataStoreBundle(testFile.absolutePath)
        val dataStore: MultiProcessDataStore<FooProto> =
            createDataStore(testData, dataStoreScope)
        val connection: BlockingServiceConnection =
            setUpService(mainContext, SimpleUpdateService::class.java, testData)

        assertThat(dataStore.data.first()).isEqualTo(DEFAULT_FOO)

        // Other proc commits TEST_TEXT update
        signalService(connection)

        assertThat(dataStore.data.first()).isEqualTo(FOO_WITH_TEXT)
    }

    class SimpleUpdateService(
        private val scope: TestScope = TestScope(UnconfinedTestDispatcher() + Job())
    ) : DirectTestService() {
        override fun beforeTest(testData: Bundle) {
            store = createDataStore(testData, scope)
        }

        override fun runTest() = runBlocking<Unit> {
            store.updateData {
                it.let { WRITE_TEXT(it) }
            }
        }
    }

    @Test
    fun testConcurrentReadUpdate() = runTest {
        val testData: Bundle = createDataStoreBundle(testFile.absolutePath)
        val dataStore: MultiProcessDataStore<FooProto> =
            createDataStore(testData, dataStoreScope)
        val writerConnection: BlockingServiceConnection =
            setUpService(mainContext, ConcurrentReadUpdateWriterService::class.java, testData)

        // Start with TEST_TEXT
        dataStore.updateData { f: FooProto -> WRITE_TEXT(f) }
        assertThat(dataStore.data.first()).isEqualTo(FOO_WITH_TEXT)

        // Writer process starts (but does not yet commit) "true"
        signalService(writerConnection)

        // We can continue reading datastore while the writer process is mid-write
        assertThat(dataStore.data.first()).isEqualTo(FOO_WITH_TEXT)

        // New processes that start in the meantime can also read
        val readerConnection: BlockingServiceConnection =
            setUpService(mainContext, ConcurrentReadUpdateReaderService::class.java, testData)
        signalService(readerConnection)

        // The other process finishes writing "true"; we (and other readers) should pick up the new data
        signalService(writerConnection)

        assertThat(dataStore.data.first()).isEqualTo(FOO_WITH_TEXT_AND_BOOLEAN)
        signalService(readerConnection)
    }

    class ConcurrentReadUpdateWriterService(
        private val scope: TestScope = TestScope(UnconfinedTestDispatcher() + Job())
    ) : DirectTestService() {
        override fun beforeTest(testData: Bundle) {
            store = createDataStore(testData, scope)
        }

        override fun runTest() = runBlocking<Unit> {
            store.updateData {
                waitForSignal()
                it.let { WRITE_BOOLEAN(it) }
            }
        }
    }

    class ConcurrentReadUpdateReaderService(
        private val scope: TestScope = TestScope(UnconfinedTestDispatcher() + Job())
    ) : DirectTestService() {
        override fun beforeTest(testData: Bundle) {
            store = createDataStore(testData, scope)
        }

        override fun runTest() = runBlocking<Unit> {
            assertThat(store.data.first()).isEqualTo(FOO_WITH_TEXT)
            waitForSignal()
            assertThat(store.data.first()).isEqualTo(FOO_WITH_TEXT_AND_BOOLEAN)
        }
    }

    @Test
    fun testInterleavedUpdateData() = runTest(UnconfinedTestDispatcher()) {
        val testData: Bundle = createDataStoreBundle(testFile.absolutePath)
        val dataStore: MultiProcessDataStore<FooProto> =
            createDataStore(testData, dataStoreScope)
        val connection: BlockingServiceConnection =
            setUpService(mainContext, InterleavedUpdateDataService::class.java, testData)

        // Other proc starts TEST_TEXT update, then waits for signal
        signalService(connection)

        // We start "true" update, then wait for condition
        val condition = CompletableDeferred<Unit>()
        val write = async(newSingleThreadContext("blockedWriter")) {
            dataStore.updateData {
                condition.await()
                it.let { WRITE_BOOLEAN(it) }
            }
        }

        // Allow the other proc's update to run to completion, then allow ours to run to completion
        val unblockOurUpdate = async {
            delay(100)
            signalService(connection)
            condition.complete(Unit)
        }

        unblockOurUpdate.await()
        write.await()

        assertThat(dataStore.data.first()).isEqualTo(FOO_WITH_TEXT_AND_BOOLEAN)
    }

    class InterleavedUpdateDataService(
        private val scope: TestScope = TestScope(UnconfinedTestDispatcher() + Job())
    ) : DirectTestService() {
        override fun beforeTest(testData: Bundle) {
            store = createDataStore(testData, scope)
        }

        override fun runTest() = runBlocking<Unit> {
            store.updateData {
                waitForSignal()
                it.let { WRITE_TEXT(it) }
            }
        }
    }

    @Ignore // b/242765757
    @Test
    fun testInterleavedUpdateDataWithLocalRead() = runTest(UnconfinedTestDispatcher()) {
        val testData: Bundle = createDataStoreBundle(testFile.absolutePath)
        val dataStore: MultiProcessDataStore<FooProto> =
            createDataStore(testData, dataStoreScope)
        val connection: BlockingServiceConnection =
            setUpService(mainContext, InterleavedUpdateDataWithReadService::class.java, testData)

        // Invalidate any local cache
        assertThat(dataStore.data.first()).isEqualTo(DEFAULT_FOO)
        signalService(connection)

        // Queue and start local write
        val writeStarted = CompletableDeferred<Unit>()
        val finishWrite = CompletableDeferred<Unit>()

        val write = async {
            dataStore.updateData {
                writeStarted.complete(Unit)
                finishWrite.await()
                FOO_WITH_TEXT
            }
        }
        writeStarted.await()

        // Queue remote write
        signalService(connection)

        // Local uncached read; this should see data initially written remotely.
        assertThat(dataStore.data.first()).isEqualTo(FooProto.newBuilder().setInteger(1).build())

        // Unblock writes; the local write is delayed to ensure the remote write remains blocked.
        val remoteWrite = async(newSingleThreadContext("blockedWriter")) {
            signalService(connection)
        }

        val localWrite = async(newSingleThreadContext("unblockLocalWrite")) {
            delay(500)
            finishWrite.complete(Unit)
            write.await()
        }

        localWrite.await()
        remoteWrite.await()

        assertThat(dataStore.data.first()).isEqualTo(FOO_WITH_TEXT_AND_BOOLEAN)
    }

    class InterleavedUpdateDataWithReadService(
        private val scope: TestScope = TestScope(UnconfinedTestDispatcher() + Job())
    ) : DirectTestService() {
        override fun beforeTest(testData: Bundle) {
            store = createDataStore(testData, scope)
        }

        override fun runTest() = runBlocking<Unit> {
            store.updateData {
                it.let { INCREMENT_INTEGER(it) }
            }

            waitForSignal()

            val write = async {
                store.updateData {
                    it.let { WRITE_BOOLEAN(it) }
                }
            }
            waitForSignal()
            write.await()
        }
    }

    @Test
    fun testUpdateDataExceptionUnblocksOtherProcessFromWriting() = runTest {
        val testData: Bundle = createDataStoreBundle(testFile.absolutePath)
        val dataStore: MultiProcessDataStore<FooProto> =
            createDataStore(testData, dataStoreScope)
        val connection: BlockingServiceConnection =
            setUpService(mainContext, FailedUpdateDataService::class.java, testData)

        val blockWrite = CompletableDeferred<Unit>()
        val waitForWrite = CompletableDeferred<Unit>()

        val write = async {
            try {
                dataStore.updateData {
                    blockWrite.await()
                    throw IOException("Something went wrong")
                }
            } catch (e: IOException) {
                waitForWrite.complete(Unit)
            }
        }

        assertThat(write.isActive).isTrue()
        assertThat(write.isCompleted).isFalse()

        blockWrite.complete(Unit)
        waitForWrite.await()

        assertThat(write.isActive).isFalse()
        assertThat(write.isCompleted).isTrue()

        signalService(connection)

        assertThat(dataStore.data.first()).isEqualTo(FOO_WITH_TEXT)
    }

    class FailedUpdateDataService(
        private val scope: TestScope = TestScope(UnconfinedTestDispatcher() + Job())
    ) : DirectTestService() {
        override fun beforeTest(testData: Bundle) {
            store = createDataStore(testData, scope)
        }

        override fun runTest() = runBlocking<Unit> {
            store.updateData {
                it.let { WRITE_TEXT(it) }
            }
        }
    }

    @Test
    fun testUpdateDataCancellationUnblocksOtherProcessFromWriting() = runTest(
        UnconfinedTestDispatcher()
    ) {
        val localScope = TestScope(UnconfinedTestDispatcher() + Job())
        val testData: Bundle = createDataStoreBundle(testFile.absolutePath)
        val dataStore: MultiProcessDataStore<FooProto> =
            createDataStore(testData, localScope)
        val connection: BlockingServiceConnection =
            setUpService(mainContext, CancelledUpdateDataService::class.java, testData)

        val blockWrite = CompletableDeferred<Unit>()

        val write = localScope.async {
            dataStore.updateData {
                blockWrite.await()
                it.let { WRITE_BOOLEAN(it) }
            }
        }

        assertThat(write.isActive).isTrue()
        assertThat(write.isCompleted).isFalse()

        // dataStore.updateData cancelled immediately
        localScope.coroutineContext.cancelChildren()

        assertThat(write.isActive).isFalse()
        assertThat(write.isCompleted).isTrue()

        signalService(connection)

        // able to read the new value written from the other process
        assertThat(dataStore.data.first()).isEqualTo(FOO_WITH_TEXT)
    }

    // A duplicate from CancelledUpdateDataService to make sure Android framework would create a
    // new process for this test. Otherwise the test would hang infinitely because the tests bind
    // to an existing service created by the previous test.
    class CancelledUpdateDataService(
        private val scope: TestScope = TestScope(UnconfinedTestDispatcher() + Job())
    ) : DirectTestService() {
        override fun beforeTest(testData: Bundle) {
            store = createDataStore(testData, scope)
        }

        override fun runTest() = runBlocking<Unit> {
            store.updateData {
                it.let { WRITE_TEXT(it) }
            }
        }
    }

    @Test
    fun testReadUpdateCorrupt() = runTest {
        FileOutputStream(testFile).use {
            OutputStreamWriter(it).write("garbage")
        }
        val testData: Bundle = createDataStoreBundle(testFile.absolutePath)
        val connection: BlockingServiceConnection =
            setUpService(mainContext, InterleavedHandlerUpdateDataService::class.java, testData)
        val corruptionHandler = ReplaceFileCorruptionHandler<FooProto> {
            signalService(connection)
            FOO_WITH_TEXT_AND_BOOLEAN
        }
        val dataStore: MultiProcessDataStore<FooProto> =
            createDataStore(testData, dataStoreScope, corruptionHandler)

        // Other proc starts TEST_TEXT then waits for signal within handler
        signalService(connection)

        assertThat(dataStore.data.first()).isEqualTo(FOO_WITH_TEXT)

        // version file should be ready at this point
        val sharedCounter = SharedCounter.create(/* enableMlock = */ false) {
            File(testFile.absolutePath + ".version")
        }
        // only 1 write should be done to handle the corruption, so version is incremented by 1
        assertThat(sharedCounter.getValue()).isEqualTo(1)
    }

    class InterleavedHandlerUpdateDataService(
        private val scope: TestScope = TestScope(UnconfinedTestDispatcher() + Job())
    ) : DirectTestService() {
        override fun beforeTest(testData: Bundle) {
            val corruptionHandler: CorruptionHandler<FooProto> =
                ReplaceFileCorruptionHandler<FooProto> {
                    waitForSignal()
                    DEFAULT_FOO
                }
            store = createDataStore(testData, scope, corruptionHandler)
        }

        override fun runTest() = runBlocking<Unit> {
            store.updateData {
                it.let { WRITE_TEXT(it) }
            }
        }
    }

    /**
     * A corruption handler that attempts to replace the on-disk data with data from produceNewData.
     *
     * TODO(zhiyuanwang): replace with androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
     */
    private class ReplaceFileCorruptionHandler<T>(
        private val produceNewData: (CorruptionException) -> T
    ) : CorruptionHandler<T> {

        @Throws(IOException::class)
        override suspend fun handleCorruption(ex: CorruptionException): T {
            return produceNewData(ex)
        }
    }
}