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
import java.io.InputStream
import java.io.OutputStream

@kotlinx.coroutines.ExperimentalCoroutinesApi
@kotlinx.coroutines.InternalCoroutinesApi
@kotlinx.coroutines.ObsoleteCoroutinesApi
@kotlinx.coroutines.FlowPreview
@RunWith(JUnit4::class)
class SingleProcessDataStoreTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var store: DataStore<Byte>
    private lateinit var serializer: TestingSerializer
    private lateinit var testFile: File
    private lateinit var dataStoreScope: TestCoroutineScope

    @Before
    fun setUp() {
        serializer = TestingSerializer()
        testFile = tmp.newFile()
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
        assertThat(store.dataFlow.first()).isEqualTo(0)
    }

    @Test
    fun testReadWithNewInstance() = runBlockingTest {
        store.updateData { 1 }
        val newStore = newDataStore(testFile)
        assertThat(newStore.dataFlow.first()).isEqualTo(1)
    }

    @Test
    fun testReadUnreadableFile() = runBlockingTest {
        testFile.setReadable(false)
        val result = runCatching {
            store.dataFlow.first()
        }

        assertThat(result.exceptionOrNull()).isInstanceOf(IOException::class.java)
        assertThat(result.exceptionOrNull()).hasMessageThat().contains("Permission denied")
    }

    @Test
    fun testReadAfterTransientBadRead() = runBlockingTest {
        testFile.setReadable(false)

        assertThrows<IOException> { store.dataFlow.first() }.hasMessageThat()
            .contains("Permission denied")

        testFile.setReadable(true)
        assertThat(store.dataFlow.first()).isEqualTo(0)
    }

    @Test
    fun testScopeCancelledWithActiveFlow() = runBlockingTest {
        val collection = async {
            store.dataFlow.take(2).collect {
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
        assertThat(store.dataFlow.first()).isEqualTo(1)
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
        assertThat(store.dataFlow.first()).isEqualTo(0)

        continueTransform.complete(Unit)
        slowUpdate.await()

        // After update completes, update runs, and read shows new data.
        assertThat(store.dataFlow.first()).isEqualTo(1)
    }

    @Test
    fun testWriteMultiple() = runBlockingTest {
        store.updateData { 2 }
        store.updateData { it.dec() }

        assertThat(store.dataFlow.first()).isEqualTo(1)
    }

    @Test
    fun testReadAfterTransientBadWrite() = runBlockingTest {
        store.updateData { 1 }
        serializer.failingWrite = true

        assertThrows<IOException> { store.updateData { 2 } }

        val newStore = newDataStore(testFile)
        assertThat(newStore.dataFlow.first()).isEqualTo(1)
    }

    @Test
    fun testWriteToNonExistentDir() = runBlockingTest {
        val fileInNonExistentDir = File(tmp.newFolder(), "/this/does/not/exist/foo.pb")
        val newStore = newDataStore(fileInNonExistentDir)

        newStore.updateData { 1 }

        assertThat(newStore.dataFlow.first()).isEqualTo(1)
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

        assertThrows<IOException> { store.dataFlow.first() }

        serializer.failingRead = false

        store.updateData { 1 }
        assertThat(store.dataFlow.first()).isEqualTo(1)
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
    fun testFlowReceivesUpdates() = runBlockingTest {
        val collectedBytes = mutableListOf<Byte>()

        val flowCollectionJob = async {
            store.dataFlow.take(8).toList(collectedBytes)
        }

        repeat(7) {
            store.updateData { it.inc() }
        }

        flowCollectionJob.join()

        assertThat(collectedBytes).isEqualTo(mutableListOf<Byte>(0, 1, 2, 3, 4, 5, 6, 7))
    }

    @Test
    fun testMultipleFlowsReceiveData() = runBlockingTest {
        val flowOf8 = store.dataFlow.take(8)

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
        val flowOf8 = store.dataFlow.take(8)

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
        val flowOf8 = store.dataFlow.take(8)

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

    private fun newDataStore(
        file: File = tmp.newFile(),
        scope: CoroutineScope = dataStoreScope
    ): DataStore<Byte> {
        return SingleProcessDataStore(
            { file },
            serializer = serializer,
            scope = scope
        )
    }

    private class TestingSerializer(
        override val defaultValue: Byte = 0,
        @Volatile var failingRead: Boolean = false,
        @Volatile var failingWrite: Boolean = false
    ) : DataStore.Serializer<Byte> {
        override fun readFrom(input: InputStream): Byte {
            if (failingRead) {
                throw IOException("I was asked to fail on reads")
            }
            val read = input.read()
            if (read == -1) {
                return defaultValue
            }
            return read.toByte()
        }

        override fun writeTo(t: Byte, output: OutputStream) {
            if (failingWrite) {
                throw IOException("I was asked to fail on writes")
            }
            output.write(t.toInt())
        }
    }
}
