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

import androidx.datastore.core.twoWayIpc.InterProcessCompletable
import androidx.datastore.core.twoWayIpc.IpcAction
import androidx.datastore.core.twoWayIpc.IpcUnit
import androidx.datastore.core.twoWayIpc.TwoWayIpcSubject
import com.google.common.truth.Truth.assertThat
import java.io.Serializable
import kotlinx.coroutines.async
import kotlinx.coroutines.yield
import org.junit.Rule
import org.junit.Test

class InterProcessCompletableTest {
    @get:Rule val multiProcess = MultiProcessTestRule()

    private data class Value(val value: String) : Serializable

    private data class Complete(
        val hostLatch: InterProcessCompletable<Value>,
        val remoteLatch: InterProcessCompletable<Value>,
        val hostValue: Value,
        val remoteValue: Value,
    ) : IpcAction<IpcUnit>() {
        override suspend fun invokeInRemoteProcess(subject: TwoWayIpcSubject): IpcUnit {
            assertThat(hostLatch.await(subject)).isEqualTo(hostValue)
            remoteLatch.complete(subject, remoteValue)
            return IpcUnit
        }
    }

    @Test
    fun completeInRemoteProcess() =
        multiProcess.runTest {
            val subject = multiProcess.createConnection().createSubject(this)
            val hostLatch = InterProcessCompletable<Value>()
            val remoteLatch = InterProcessCompletable<Value>()
            val remoteInvocation = async {
                subject.invokeInRemoteProcess(
                    Complete(
                        hostLatch = hostLatch,
                        remoteLatch = remoteLatch,
                        hostValue = Value("host"),
                        remoteValue = Value("remote"),
                    )
                )
            }
            yield()
            // cannot complete, we didn't release the host latch
            assertThat(remoteInvocation.isActive).isTrue()
            hostLatch.complete(subject, Value("host"))
            remoteInvocation.await()
            assertThat(remoteLatch.await(subject)).isEqualTo(Value("remote"))
        }
}
