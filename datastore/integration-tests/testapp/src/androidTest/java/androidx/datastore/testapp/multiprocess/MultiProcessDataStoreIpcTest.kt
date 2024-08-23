/*
 * Copyright 2023 The Android Open Source Project
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

// Parcelize object is testing internal implementation of datastore-core library
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "CANNOT_OVERRIDE_INVISIBLE_MEMBER")

package androidx.datastore.testapp.multiprocess

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.CorruptionHandler
import androidx.datastore.core.IOException
import androidx.datastore.core.SharedCounter
import androidx.datastore.testapp.multiprocess.ipcActions.ReadTextAction
import androidx.datastore.testapp.multiprocess.ipcActions.SetTextAction
import androidx.datastore.testapp.multiprocess.ipcActions.StorageVariant
import androidx.datastore.testapp.multiprocess.ipcActions.createMultiProcessTestDatastore
import androidx.datastore.testapp.multiprocess.ipcActions.datastore
import androidx.datastore.testapp.twoWayIpc.InterProcessCompletable
import androidx.datastore.testapp.twoWayIpc.IpcAction
import androidx.datastore.testapp.twoWayIpc.IpcUnit
import androidx.datastore.testapp.twoWayIpc.TwoWayIpcSubject
import androidx.datastore.testing.TestMessageProto.FooProto
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MultiProcessDataStoreIpcTest {
    @get:Rule val multiProcessRule = MultiProcessTestRule()

    @get:Rule val tmpFolder = TemporaryFolder()

    @Test fun testSimpleUpdateData_file() = testSimpleUpdateData(StorageVariant.FILE)

    @Test fun testSimpleUpdateData_okio() = testSimpleUpdateData(StorageVariant.OKIO)

    private fun testSimpleUpdateData(storageVariant: StorageVariant) =
        multiProcessRule.runTest {
            val connection = multiProcessRule.createConnection()
            val subject = connection.createSubject(this)
            val file = tmpFolder.newFile()
            val datastore =
                createMultiProcessTestDatastore(
                    filePath = file.canonicalPath,
                    storageVariant = storageVariant,
                    hostDatastoreScope = multiProcessRule.datastoreScope,
                    subjects = arrayOf(subject)
                )
            subject.invokeInRemoteProcess(SetTextAction("abc"))
            assertThat(datastore.data.first().text).isEqualTo("abc")
            datastore.updateData { it.toBuilder().setText("hostValue").build() }
            // read from remote process
            assertThat(subject.invokeInRemoteProcess(ReadTextAction()).value).isEqualTo("hostValue")
        }

    @Test fun testConcurrentReadUpdate_file() = testConcurrentReadUpdate(StorageVariant.FILE)

    @Test fun testConcurrentReadUpdate_okio() = testConcurrentReadUpdate(StorageVariant.OKIO)

    private fun testConcurrentReadUpdate(storageVariant: StorageVariant) =
        multiProcessRule.runTest {
            val subject1 =
                multiProcessRule.createConnection().createSubject(multiProcessRule.datastoreScope)
            val subject2 =
                multiProcessRule.createConnection().createSubject(multiProcessRule.datastoreScope)
            val file = tmpFolder.newFile()
            val dataStore =
                createMultiProcessTestDatastore(
                    filePath = file.canonicalPath,
                    storageVariant = storageVariant,
                    hostDatastoreScope = multiProcessRule.datastoreScope,
                    subjects = arrayOf(subject1, subject2)
                )
            // start with data
            dataStore.updateData { it.toBuilder().setText("hostData").build() }
            val commitWriteLatch = InterProcessCompletable<IpcUnit>()
            val writeStartedLatch = InterProcessCompletable<IpcUnit>()
            val setTextAction = async {
                subject1.invokeInRemoteProcess(
                    SetTextAction(
                        value = "remoteValue",
                        commitTransactionLatch = commitWriteLatch,
                        transactionStartedLatch = writeStartedLatch
                    )
                )
            }
            writeStartedLatch.await(subject1)
            // we can still read
            assertThat(dataStore.data.first().text).isEqualTo("hostData")
            // writer process can read data
            assertThat(subject1.invokeInRemoteProcess(ReadTextAction()).value).isEqualTo("hostData")
            // another process can read data
            assertThat(subject2.invokeInRemoteProcess(ReadTextAction()).value).isEqualTo("hostData")
            commitWriteLatch.complete(subject1, IpcUnit)
            setTextAction.await()
            // now everyone should see the new value
            assertThat(dataStore.data.first().text).isEqualTo("remoteValue")
            assertThat(subject1.invokeInRemoteProcess(ReadTextAction()).value)
                .isEqualTo("remoteValue")
            assertThat(subject2.invokeInRemoteProcess(ReadTextAction()).value)
                .isEqualTo("remoteValue")
        }

    @Test fun testInterleavedUpdateData_file() = testInterleavedUpdateData(StorageVariant.FILE)

    @Test fun testInterleavedUpdateData_okio() = testInterleavedUpdateData(StorageVariant.OKIO)

    private fun testInterleavedUpdateData(storageVariant: StorageVariant) =
        multiProcessRule.runTest {
            val subject =
                multiProcessRule.createConnection().createSubject(multiProcessRule.datastoreScope)
            val file = tmpFolder.newFile()
            val dataStore =
                createMultiProcessTestDatastore(
                    filePath = file.canonicalPath,
                    storageVariant = storageVariant,
                    hostDatastoreScope = multiProcessRule.datastoreScope,
                    subjects = arrayOf(subject)
                )
            val remoteWriteStarted = InterProcessCompletable<IpcUnit>()
            val allowRemoteCommit = InterProcessCompletable<IpcUnit>()
            val remoteUpdate = async {
                // update text in remote
                subject.invokeInRemoteProcess(
                    SetTextAction(
                        value = "remoteValue",
                        transactionStartedLatch = remoteWriteStarted,
                        commitTransactionLatch = allowRemoteCommit
                    )
                )
            }
            // wait for remote write to start
            remoteWriteStarted.await(subject)
            // start a host update, which will be blocked
            val hostUpdateStarted = CompletableDeferred<Unit>()
            val hostUpdate = async {
                hostUpdateStarted.complete(Unit)
                dataStore.updateData { it.toBuilder().setInteger(99).build() }
            }
            // let our host update start
            hostUpdateStarted.await()
            // give it some to be blocked
            delay(100)
            // both are running
            assertThat(hostUpdate.isActive).isTrue()
            assertThat(remoteUpdate.isActive).isTrue()
            // commit remote transaction
            allowRemoteCommit.complete(subject, IpcUnit)
            // wait for both
            listOf(hostUpdate, remoteUpdate).awaitAll()
            dataStore.data.first().let {
                assertThat(it.text).isEqualTo("remoteValue")
                assertThat(it.integer).isEqualTo(99)
            }
        }

    @Test
    fun testInterleavedUpdateDataWithLocalRead_file() =
        testInterleavedUpdateDataWithLocalRead(StorageVariant.FILE)

    @Test
    fun testInterleavedUpdateDataWithLocalRead_okio() =
        testInterleavedUpdateDataWithLocalRead(StorageVariant.OKIO)

    @Parcelize
    private data class InterleavedDoubleUpdateAction(
        val updatedInteger: InterProcessCompletable<IpcUnit> = InterProcessCompletable(),
        val unblockBooleanWrite: InterProcessCompletable<IpcUnit> = InterProcessCompletable(),
        val willWriteBooleanData: InterProcessCompletable<IpcUnit> = InterProcessCompletable(),
    ) : IpcAction<IpcUnit>() {
        override suspend fun invokeInRemoteProcess(subject: TwoWayIpcSubject): IpcUnit {
            subject.datastore.updateData { it.toBuilder().setInteger(it.integer + 1).build() }
            updatedInteger.complete(subject, IpcUnit)
            unblockBooleanWrite.await(subject)
            willWriteBooleanData.complete(subject, IpcUnit)
            subject.datastore.updateData { it.toBuilder().setBoolean(true).build() }
            return IpcUnit
        }
    }

    private fun testInterleavedUpdateDataWithLocalRead(storageVariant: StorageVariant) =
        multiProcessRule.runTest {
            val subject =
                multiProcessRule.createConnection().createSubject(multiProcessRule.datastoreScope)
            val file = tmpFolder.newFile()
            val dataStore =
                createMultiProcessTestDatastore(
                    filePath = file.canonicalPath,
                    storageVariant = storageVariant,
                    hostDatastoreScope = multiProcessRule.datastoreScope,
                    subjects = arrayOf(subject)
                )
            // invalidate local cache
            assertThat(dataStore.data.first()).isEqualTo(FooProto.getDefaultInstance())
            val remoteAction = InterleavedDoubleUpdateAction()
            val remoteActionExecution = async { subject.invokeInRemoteProcess(remoteAction) }
            // Queue and start local write
            val writeStarted = CompletableDeferred<Unit>()
            val finishWrite = CompletableDeferred<Unit>()

            // wait for remote to write the int value
            remoteAction.updatedInteger.await(subject)

            val hostWrite = async {
                dataStore.updateData {
                    writeStarted.complete(Unit)
                    finishWrite.await()
                    FooProto.newBuilder().setText("hostValue").build()
                }
            }
            writeStarted.await()
            // our write is blocked so we should only see the int value for now
            assertThat(dataStore.data.first())
                .isEqualTo(FooProto.newBuilder().setInteger(1).build())
            // unblock the remote write but it will be blocked as we already have a write
            // lock in host process
            remoteAction.unblockBooleanWrite.complete(subject, IpcUnit)
            // wait for remote to be ready to write
            remoteAction.willWriteBooleanData.await(subject)
            // delay some to ensure remote is really blocked
            delay(200)
            finishWrite.complete(Unit)
            // wait for both
            listOf(hostWrite, remoteActionExecution).awaitAll()
            // both writes committed
            assertThat(dataStore.data.first())
                .isEqualTo(
                    FooProto.getDefaultInstance()
                        .toBuilder()
                        .setText("hostValue")
                        // int is not set since local did override it w/ default
                        .setBoolean(true)
                        .build()
                )
        }

    @Test
    fun testUpdateDataExceptionUnblocksOtherProcessFromWriting_file() =
        testUpdateDataExceptionUnblocksOtherProcessFromWriting(StorageVariant.FILE)

    @Test
    fun testUpdateDataExceptionUnblocksOtherProcessFromWriting_okio() =
        testUpdateDataExceptionUnblocksOtherProcessFromWriting(StorageVariant.OKIO)

    private fun testUpdateDataExceptionUnblocksOtherProcessFromWriting(
        storageVariant: StorageVariant
    ) =
        multiProcessRule.runTest {
            val connection = multiProcessRule.createConnection()
            val subject = connection.createSubject(this)
            val file = tmpFolder.newFile()
            val dataStore =
                createMultiProcessTestDatastore(
                    filePath = file.canonicalPath,
                    storageVariant = storageVariant,
                    hostDatastoreScope = multiProcessRule.datastoreScope,
                    subjects = arrayOf(subject)
                )
            val blockWrite = CompletableDeferred<Unit>()
            val localWriteStarted = CompletableDeferred<Unit>()

            val write = async {
                try {
                    dataStore.updateData {
                        localWriteStarted.complete(Unit)
                        blockWrite.await()
                        throw IOException("Something went wrong")
                    }
                } catch (_: IOException) {}
            }
            localWriteStarted.await()
            val setTextAction = async {
                subject.invokeInRemoteProcess(SetTextAction(value = "remoteValue"))
            }
            delay(100)
            // cannot start since we are holding the lock
            assertThat(setTextAction.isActive).isTrue()
            blockWrite.complete(Unit)
            listOf(write, setTextAction).awaitAll()
            assertThat(dataStore.data.first().text).isEqualTo("remoteValue")
        }

    @Test
    fun testUpdateDataCancellationUnblocksOtherProcessFromWriting_file() =
        testUpdateDataCancellationUnblocksOtherProcessFromWriting(StorageVariant.FILE)

    @Test
    fun testUpdateDataCancellationUnblocksOtherProcessFromWriting_okio() =
        testUpdateDataCancellationUnblocksOtherProcessFromWriting(StorageVariant.OKIO)

    private fun testUpdateDataCancellationUnblocksOtherProcessFromWriting(
        storageVariant: StorageVariant
    ) =
        multiProcessRule.runTest {
            val connection = multiProcessRule.createConnection()
            val subject = connection.createSubject(this)
            val file = tmpFolder.newFile()
            val localScope = CoroutineScope(Dispatchers.IO)
            val dataStore =
                createMultiProcessTestDatastore(
                    filePath = file.canonicalPath,
                    storageVariant = storageVariant,
                    hostDatastoreScope = multiProcessRule.datastoreScope,
                    subjects = arrayOf(subject)
                )
            val blockWrite = CompletableDeferred<Unit>()
            val startedWrite = CompletableDeferred<Unit>()

            localScope.launch {
                dataStore.updateData {
                    startedWrite.complete(Unit)
                    blockWrite.await()
                    it.toBuilder().setInteger(3).build()
                }
            }
            startedWrite.await()
            val setTextAction = async {
                subject.invokeInRemoteProcess(SetTextAction(value = "remoteValue"))
            }
            delay(100)
            // cannot start since we are holding the lock
            assertThat(setTextAction.isActive).isTrue()
            // cancel the scope that is holding the write lock
            localScope.cancel()
            // wait for remote to finish
            setTextAction.await()
            assertThat(dataStore.data.first())
                .isEqualTo(FooProto.getDefaultInstance().toBuilder().setText("remoteValue").build())
        }

    @Test fun testReadUpdateCorrupt_file() = testReadUpdateCorrupt(StorageVariant.FILE)

    @Test fun testReadUpdateCorrupt_okio() = testReadUpdateCorrupt(StorageVariant.OKIO)

    private class TestCorruptionHandler : CorruptionHandler<FooProto> {
        override suspend fun handleCorruption(ex: CorruptionException): FooProto {
            return createRecoveryValue(inMainProcess)
        }

        companion object {
            var inMainProcess = false

            fun createRecoveryValue(mainProcess: Boolean): FooProto {
                return FooProto.getDefaultInstance()
                    .toBuilder()
                    .setText("defaultCorruptValue, main process? $mainProcess")
                    .build()
            }
        }
    }

    private fun testReadUpdateCorrupt(storageVariant: StorageVariant) =
        multiProcessRule.runTest {
            val connection = multiProcessRule.createConnection()
            val subject = connection.createSubject(this)
            val file = tmpFolder.newFile()
            // corrupt file
            file.writeText("garbage")
            // set a shared value so we can know which corruption handler did run
            TestCorruptionHandler.inMainProcess = true
            val dataStore =
                createMultiProcessTestDatastore(
                    filePath = file.canonicalPath,
                    storageVariant = storageVariant,
                    hostDatastoreScope = multiProcessRule.datastoreScope,
                    corruptionHandler = TestCorruptionHandler::class.java,
                    subjects = arrayOf(subject)
                )
            val blockSetText = InterProcessCompletable<IpcUnit>()
            val writeStarted = InterProcessCompletable<IpcUnit>()
            val setTextAction = async {
                subject.invokeInRemoteProcess(
                    SetTextAction(
                        value = "remoteValue",
                        commitTransactionLatch = blockSetText,
                        transactionStartedLatch = writeStarted
                    )
                )
            }
            writeStarted.await(subject)
            // we read the corruption handler value since the write hasn't happened yet
            // the write in the remote process already started, hence we'll read its recovery value
            assertThat(dataStore.data.first())
                .isEqualTo(TestCorruptionHandler.createRecoveryValue(mainProcess = false))
            // version file should be ready at this point
            val sharedCounter =
                SharedCounter.create { file.parentFile!!.resolve("${file.name}.version") }
            // only 1 write should be done to handle the corruption, so version is incremented by 1
            assertThat(sharedCounter.getValue()).isEqualTo(1)
            // unblock write
            blockSetText.complete(subject, IpcUnit)
            // wait for write to finish
            setTextAction.await()
            // now we can see the value written there
            assertThat(dataStore.data.first().text).isEqualTo("remoteValue")
        }

    @Test
    fun testConcurrentUpdateNoDeadlock_file() = testConcurrentUpdateNoDeadlock(StorageVariant.FILE)

    @Test
    fun testConcurrentUpdateNoDeadlock_okio() = testConcurrentUpdateNoDeadlock(StorageVariant.OKIO)

    /**
     * Reproduce the false alarm on deadlock by Linux. It happens in the case where:<br>
     * 1. process A holds file lock 1;<br>
     * 2. process B holds file lock 2;<br>
     * 3. process B (could be another thread than 2.) waits to hold file lock 1 (still held by
     *    A);<br>
     * 4. process A (could be another thread than 1.) waits to hold file lock 2 (still held by B) -
     *    exception "Resource deadlock would occur" is thrown - caught and retried with exponential
     *    backoff.
     */
    private fun testConcurrentUpdateNoDeadlock(storageVariant: StorageVariant) =
        multiProcessRule.runTest {
            val connection = multiProcessRule.createConnection()
            val subject1 = connection.createSubject(multiProcessRule.datastoreScope)
            val subject2 = connection.createSubject(multiProcessRule.datastoreScope)

            val file1 = tmpFolder.newFile()
            val file2 = tmpFolder.newFile()
            val datastore1 =
                createMultiProcessTestDatastore(
                    filePath = file1.canonicalPath,
                    storageVariant = storageVariant,
                    hostDatastoreScope = multiProcessRule.datastoreScope,
                    subjects = arrayOf(subject1)
                )
            val datastore2 =
                createMultiProcessTestDatastore(
                    filePath = file2.canonicalPath,
                    storageVariant = storageVariant,
                    hostDatastoreScope = multiProcessRule.datastoreScope,
                    subjects = arrayOf(subject2)
                )

            // setup real data and lock file
            datastore1.updateData { it.toBuilder().setText("hostData").build() }
            datastore2.updateData { it.toBuilder().setText("hostData").build() }

            // process A holds file lock 1
            val blockWrite = CompletableDeferred<Unit>()
            val startedWrite = CompletableDeferred<Unit>()

            val localUpdate1 = async {
                datastore1.updateData {
                    startedWrite.complete(Unit)
                    blockWrite.await()
                    it.toBuilder().setInteger(3).build()
                }
            }
            startedWrite.await()

            // process B holds file lock 2
            val commitWriteLatch1 = InterProcessCompletable<IpcUnit>()
            val writeStartedLatch1 = InterProcessCompletable<IpcUnit>()
            val setTextAction1 = async {
                subject2.invokeInRemoteProcess(
                    SetTextAction(
                        value = "remoteValue",
                        commitTransactionLatch = commitWriteLatch1,
                        transactionStartedLatch = writeStartedLatch1
                    )
                )
            }
            writeStartedLatch1.await(subject2)

            // process B (could be another thread than 2.) waits to hold file lock 1
            val commitWriteLatch2 = InterProcessCompletable<IpcUnit>()
            val actionStartedLatch = InterProcessCompletable<IpcUnit>()
            val setTextAction2 = async {
                subject1.invokeInRemoteProcess(
                    SetTextAction(
                        value = "remoteValue",
                        commitTransactionLatch = commitWriteLatch2,
                        actionStartedLatch = actionStartedLatch
                    )
                )
            }
            actionStartedLatch.await(subject1)
            // wait a bit to let the other process get into updateData, might be flaky
            delay(100)

            // process A (could be another thread than 1.) waits to hold file lock 2 (still held by
            // B)
            val localUpdate2 = async {
                datastore2.updateData { it.toBuilder().setInteger(4).build() }
            }

            blockWrite.complete(Unit)
            commitWriteLatch1.complete(subject2, IpcUnit)
            commitWriteLatch2.complete(subject1, IpcUnit)

            setTextAction1.await()
            setTextAction2.await()
            localUpdate1.await()
            localUpdate2.await()

            assertThat(datastore1.data.first().text).isEqualTo("remoteValue")
            assertThat(datastore1.data.first().integer).isEqualTo(3)
            assertThat(datastore2.data.first().text).isEqualTo("remoteValue")
            assertThat(datastore2.data.first().integer).isEqualTo(4)
        }
}
