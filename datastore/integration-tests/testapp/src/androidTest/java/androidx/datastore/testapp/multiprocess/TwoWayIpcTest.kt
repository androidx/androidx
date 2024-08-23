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

package androidx.datastore.testapp.multiprocess

import android.os.Parcelable
import androidx.datastore.testapp.twoWayIpc.CompositeServiceSubjectModel
import androidx.datastore.testapp.twoWayIpc.IpcAction
import androidx.datastore.testapp.twoWayIpc.TwoWayIpcSubject
import com.google.common.truth.Truth.assertThat
import kotlinx.parcelize.Parcelize
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TwoWayIpcTest {
    @get:Rule val multiProcessRule = MultiProcessTestRule()

    @Parcelize
    internal class MultiplyBy3Action(var input: Int) : IpcAction<MultiplyBy3Action.Output>() {
        @Parcelize data class Output(val value: Int) : Parcelable

        override suspend fun invokeInRemoteProcess(subject: TwoWayIpcSubject): Output {
            return Output(input * 3)
        }
    }

    @Test
    fun sample() =
        multiProcessRule.runTest {
            val connection = multiProcessRule.createConnection()
            val subject = connection.createSubject(this)
            assertThat(subject.invokeInRemoteProcess(MultiplyBy3Action(3)))
                .isEqualTo(MultiplyBy3Action.Output(9))
        }

    @Parcelize
    internal class ThrowingAction : IpcAction<ThrowingAction>() {
        override suspend fun invokeInRemoteProcess(subject: TwoWayIpcSubject): ThrowingAction {
            error("some error i got")
        }
    }

    @Test
    fun exceptionThrown() =
        multiProcessRule.runTest {
            val connection = multiProcessRule.createConnection()
            val subject = connection.createSubject(this)
            val result = runCatching { subject.invokeInRemoteProcess(ThrowingAction()) }
            assertThat(result.exceptionOrNull()).hasMessageThat().contains("some error i got")
        }

    @Parcelize
    internal data class ValueInRemoteAction(val id: String, val value: String, val set: Boolean) :
        IpcAction<ValueInRemoteAction.Output>() {

        @Parcelize data class Output(val value: String) : Parcelable

        override suspend fun invokeInRemoteProcess(subject: TwoWayIpcSubject): Output {
            if (set) {
                subject.data[StringKey(id)] = value
            }
            return Output(subject.data[StringKey(id)])
        }

        data class StringKey(val id: String) : CompositeServiceSubjectModel.Key<String>()
    }

    @Test
    fun multipleSubjects() =
        multiProcessRule.runTest {
            val connection = multiProcessRule.createConnection()
            val subject1 = connection.createSubject(this)
            val subject2 = connection.createSubject(this)
            val action = ValueInRemoteAction(id = "a", value = "b", set = true)
            assertThat(subject1.invokeInRemoteProcess(action).value).isEqualTo("b")
            assertThat(subject2.invokeInRemoteProcess(action).value).isEqualTo("b")

            assertThat(subject1.invokeInRemoteProcess(action.copy(value = "c")).value)
                .isEqualTo("c")

            assertThat(
                    // don't set
                    subject1.invokeInRemoteProcess(action.copy(value = "d", set = false)).value
                )
                .isEqualTo("c")
            assertThat(
                    // don't set
                    subject2.invokeInRemoteProcess(action.copy(value = "d", set = false)).value
                )
                .isEqualTo("b")
        }

    @Parcelize
    internal class SendFromRemoteProcess(val value: String) : IpcAction<SendFromRemoteProcess>() {

        @Parcelize
        internal class ActionInMainProcess(val value: String) : IpcAction<ActionInMainProcess>() {
            override suspend fun invokeInRemoteProcess(
                subject: TwoWayIpcSubject
            ): ActionInMainProcess {
                subject.data[VALUE_KEY] = value
                return this
            }
        }

        override suspend fun invokeInRemoteProcess(
            subject: TwoWayIpcSubject
        ): SendFromRemoteProcess {
            subject.invokeInRemoteProcess(ActionInMainProcess("$value-$value"))
            return this
        }

        companion object {
            val VALUE_KEY = CompositeServiceSubjectModel.Key<String>()
        }
    }

    @Test
    fun getMessageFromRemoteProcess() =
        multiProcessRule.runTest {
            val connection = multiProcessRule.createConnection()
            val hostSubject = connection.createSubject(this)
            hostSubject.invokeInRemoteProcess(SendFromRemoteProcess("hello"))
            assertThat(hostSubject.data[SendFromRemoteProcess.VALUE_KEY]).isEqualTo("hello-hello")
        }
}
