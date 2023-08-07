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

package androidx.datastore.core.multiprocess

import androidx.datastore.core.multiprocess.ipcActions.ReadTextAction
import androidx.datastore.core.multiprocess.ipcActions.SetTextAction
import androidx.datastore.core.multiprocess.ipcActions.StorageVariant
import androidx.datastore.core.multiprocess.ipcActions.createMultiProcessTestDatastore
import androidx.datastore.core.twoWayIpc.InterProcessCompletable
import androidx.datastore.core.twoWayIpc.IpcUnit
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MultiProcessDataStoreIpcTest {
    @get:Rule
    val multiProcessRule = MultiProcessTestRule()

    @get:Rule
    val tmpFolder = TemporaryFolder()

    @Test
    fun testSimpleUpdateData_file() = testSimpleUpdateData(StorageVariant.FILE)

    @Test
    fun testSimpleUpdateData_okio() = testSimpleUpdateData(StorageVariant.OKIO)

    private fun testSimpleUpdateData(storageVariant: StorageVariant) = multiProcessRule.runTest {
        val connection = multiProcessRule.createConnection()
        val subject = connection.createSubject(this)
        val file = tmpFolder.newFile()
        val datastore = createMultiProcessTestDatastore(
            filePath = file.canonicalPath,
            storageVariant = storageVariant,
            hostDatastoreScope = multiProcessRule.datastoreScope,
            subjects = arrayOf(subject)
        )
        subject.invokeInRemoteProcess(SetTextAction("abc"))
        assertThat(datastore.data.first().text).isEqualTo("abc")
        datastore.updateData {
            it.toBuilder().setText("hostValue").build()
        }
        // read from remote process
        assertThat(
            subject.invokeInRemoteProcess(
                ReadTextAction()
            ).value
        ).isEqualTo("hostValue")
    }

    @Test
    fun testConcurrentReadUpdate_file() = testConcurrentReadUpdate(StorageVariant.FILE)

    @Test
    fun testConcurrentReadUpdate_okio() = testConcurrentReadUpdate(StorageVariant.OKIO)

    private fun testConcurrentReadUpdate(storageVariant: StorageVariant) =
        multiProcessRule.runTest {
            val subject1 = multiProcessRule.createConnection().createSubject(
                multiProcessRule.datastoreScope
            )
            val subject2 = multiProcessRule.createConnection().createSubject(
                multiProcessRule.datastoreScope
            )
            val file = tmpFolder.newFile()
            val dataStore = createMultiProcessTestDatastore(
                filePath = file.canonicalPath,
                storageVariant = storageVariant,
                hostDatastoreScope = multiProcessRule.datastoreScope,
                subjects = arrayOf(subject1, subject2)
            )
            // start with data
            dataStore.updateData {
                it.toBuilder().setText("hostData").build()
            }
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
            assertThat(
                subject1.invokeInRemoteProcess(
                    ReadTextAction()
                ).value
            ).isEqualTo("hostData")
            // another process can read data
            assertThat(
                subject2.invokeInRemoteProcess(
                    ReadTextAction()
                ).value
            ).isEqualTo("hostData")
            commitWriteLatch.complete(subject1, IpcUnit)
            setTextAction.await()
            // now everyone should see the new value
            assertThat(dataStore.data.first().text).isEqualTo("remoteValue")
            assertThat(
                subject1.invokeInRemoteProcess(
                    ReadTextAction()
                ).value
            ).isEqualTo("remoteValue")
            assertThat(
                subject2.invokeInRemoteProcess(
                    ReadTextAction()
                ).value
            ).isEqualTo("remoteValue")
        }
}
