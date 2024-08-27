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
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package androidx.datastore.testapp.multiprocess

import androidx.datastore.testapp.multiprocess.ipcActions.ReadTextAction
import androidx.datastore.testapp.multiprocess.ipcActions.SetTextAction
import androidx.datastore.testapp.multiprocess.ipcActions.StorageVariant
import androidx.datastore.testapp.multiprocess.ipcActions.createMultiProcessTestDatastore
import androidx.datastore.testapp.multiprocess.ipcActions.datastore
import androidx.datastore.testapp.twoWayIpc.CompositeServiceSubjectModel
import androidx.datastore.testapp.twoWayIpc.InterProcessCompletable
import androidx.datastore.testapp.twoWayIpc.IpcAction
import androidx.datastore.testapp.twoWayIpc.IpcUnit
import androidx.datastore.testapp.twoWayIpc.SubjectReadWriteProperty
import androidx.datastore.testapp.twoWayIpc.TwoWayIpcSubject
import androidx.datastore.testing.TestMessageProto.FooProto
import androidx.kruth.assertThat
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.SharingCommand
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.withTimeout
import kotlinx.parcelize.Parcelize
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
internal class MultipleDataStoresInMultipleProcessesTest(
    private val storageVariant: StorageVariant,
    /**
     * if set to true, we'll run remote subjects in 2 different processes. if set to false, we'll
     * run them on the same remote process.
     */
    private val useMultipleRemoteProcesses: Boolean,
    /**
     * If true, both datastores will be created in the same folder. If false, their parent folders
     * will be different.
     */
    private val useTheSameParentFolder: Boolean,
) {

    companion object {
        @Suppress("unused") // test parameters
        @get:JvmStatic
        @get:Parameters(name = "storage_{0}_multipleProcesses={1}_sameParentFolder={2}")
        val params = buildList {
            for (storageVariant in StorageVariant.values()) {
                for (useMultipleProcesses in arrayOf(true, false)) {
                    for (useTheSameParentFolder in arrayOf(true, false)) {
                        add(arrayOf(storageVariant, useMultipleProcesses, useTheSameParentFolder))
                    }
                }
            }
        }
    }

    @get:Rule val multiProcessRule = MultiProcessTestRule()

    @get:Rule val tmpFolder = TemporaryFolder()

    @Test
    fun test() =
        multiProcessRule.runTest {
            // create subjects in 2 different processes.
            // our main process serves as the subject that has the case of observing 2 different
            // files
            // in the same folder.
            val (subject1, subject2) =
                if (useMultipleRemoteProcesses) {
                    // create a process per remote subject
                    multiProcessRule.createConnection().createSubject(this) to
                        multiProcessRule.createConnection().createSubject(this)
                } else {
                    // reuse the same remote process for both remote subjects
                    val connection = multiProcessRule.createConnection()
                    connection.createSubject(this) to connection.createSubject(this)
                }
            val (file1, file2) =
                if (useTheSameParentFolder) {
                    val parent = tmpFolder.newFolder()
                    parent.resolve("ds1.pb") to parent.resolve("ds2.pb")
                } else {
                    val parent1 = tmpFolder.newFolder()
                    val parent2 = tmpFolder.newFolder()
                    parent1.resolve("ds1.pb") to parent2.resolve("ds2.pb")
                }

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
            val ds1Value = datastore1.data.stateIn(multiProcessRule.datastoreScope)
            val ds2Value = datastore2.data.stateIn(multiProcessRule.datastoreScope)
            ds1Value.awaitValue("")
            ds2Value.awaitValue("")
            // simple assertions of host process reading the value after an after in the remote
            // process
            subject1.invokeInRemoteProcess(SetTextAction("ds1-version-1"))
            subject2.invokeInRemoteProcess(SetTextAction("ds2-version-1"))
            ds1Value.awaitValue("ds1-version-1")
            ds2Value.awaitValue("ds2-version-1")

            // create an observer in subject1
            val subject1Observer = ObserveFileAction()
            subject1.invokeInRemoteProcess(subject1Observer)
            subject1.assertRemoteObservedValue("ds1-version-1")

            // create an observer in subject2
            val subject2Observer = ObserveFileAction()
            subject2.invokeInRemoteProcess(subject2Observer)
            subject2.assertRemoteObservedValue("ds2-version-1")

            // while the observers are active in the subjects, update the value in main process and
            // ensure they get the new value
            datastore1.updateData { it.toBuilder().setText("ds1-version-2").build() }
            datastore2.updateData { it.toBuilder().setText("ds2-version-2").build() }
            // everyone gets the value
            ds1Value.awaitValue("ds1-version-2")
            subject1.assertRemoteObservedValue("ds1-version-2")
            ds2Value.awaitValue("ds2-version-2")
            subject2.assertRemoteObservedValue("ds2-version-2")

            // stop subject 1, it should not get the update
            subject1Observer.stopObserving.complete(subject1, IpcUnit)
            subject1Observer.stoppedObserving.await(subject1)
            subject1.invokeInRemoteProcess(SetTextAction(value = "ds1-version-3"))
            ds1Value.awaitValue("ds1-version-3")
            // observation is stopped so the observed value should stay the same
            assertThat(subject1.invokeInRemoteProcess(ReadRemoteObservedValue()).value)
                .isEqualTo("ds1-version-2")
            // a new observer in subject1 process would see the new value
            assertThat(subject1.invokeInRemoteProcess(ReadTextAction()).value)
                .isEqualTo("ds1-version-3")
            // make sure the observer for the other process is still working well even after we
            // stopped
            // the observer in process 1
            subject2.invokeInRemoteProcess(SetTextAction("ds2-version-3"))
            ds2Value.awaitValue("ds2-version-3")
            subject2.assertRemoteObservedValue("ds2-version-3")
            datastore2.updateData { it.toBuilder().setText("ds2-version-4").build() }
            subject2.assertRemoteObservedValue("ds2-version-4")
        }
}

/** key used in test to keep a StateFlow of the datastore value */
private val REMOTE_OBSERVER_KEY = CompositeServiceSubjectModel.Key<StateFlow<FooProto>>()

/** The StateFlow value for test that is created by the [ObserveFileAction]. */
private var TwoWayIpcSubject.remoteProcessStateFlow by SubjectReadWriteProperty(REMOTE_OBSERVER_KEY)

/**
 * An IPC action that will create a StateFlow of the DataStore value and keep it active until
 * [stopObserving] is completed. The value of that StateFlow ([remoteProcessStateFlow]) can be
 * asserted via [AssertRemoteObservedValue] or read via [ReadRemoteObservedValue].
 *
 * @see AssertRemoteObservedValue
 * @see ReadRemoteObservedValue
 */
@Parcelize
internal class ObserveFileAction(
    /** When completed, we'll stop the StateFlow */
    val stopObserving: InterProcessCompletable<IpcUnit> = InterProcessCompletable(),
    /** We'll complete this action when the StateFlow is stopped. */
    val stoppedObserving: InterProcessCompletable<IpcUnit> = InterProcessCompletable(),
) : IpcAction<IpcUnit>() {
    override suspend fun invokeInRemoteProcess(subject: TwoWayIpcSubject): IpcUnit {
        subject.remoteProcessStateFlow =
            subject.datastore.data.stateIn(
                subject.datastoreScope,
                started = {
                    flow {
                        // immediately start observing
                        emit(SharingCommand.START)
                        // wait until stop observing is called
                        stopObserving.await(subject)
                        // stop observing
                        emit(SharingCommand.STOP)
                        stoppedObserving.complete(subject, IpcUnit)
                    }
                },
                initialValue = FooProto.getDefaultInstance()
            )
        return IpcUnit
    }
}

/** Asserts the value of the [remoteProcessStateFlow] by waiting it to dispatch [expectedValue]. */
@Parcelize
internal class AssertRemoteObservedValue(
    private val expectedValue: String,
) : IpcAction<IpcUnit>() {
    override suspend fun invokeInRemoteProcess(subject: TwoWayIpcSubject): IpcUnit {
        subject.remoteProcessStateFlow.awaitValue(expectedValue)
        return IpcUnit
    }
}

/** Reads the current value of [remoteProcessStateFlow]. */
@Parcelize
internal class ReadRemoteObservedValue : IpcAction<ReadTextAction.TextValue>() {
    override suspend fun invokeInRemoteProcess(
        subject: TwoWayIpcSubject
    ): ReadTextAction.TextValue {
        return ReadTextAction.TextValue(subject.remoteProcessStateFlow.value.text)
    }
}

/**
 * Collects [this] until the [StateFlow.value] is equal to [value].
 *
 * @see assertRemoteObservedValue
 */
private suspend fun StateFlow<FooProto>.awaitValue(value: String) {
    try {
        // 5 seconds is what we use for IPC action timeouts, hence we pick a lower number
        // here to get this timeout before the IPC
        withTimeout(4.seconds) { this@awaitValue.takeWhile { it.text != value }.collect() }
    } catch (timeout: TimeoutCancellationException) {
        throw AssertionError(
            """
                expected "$value" didn't arrive, current value: "${this@awaitValue.value.text}"
            """
                .trimIndent()
        )
    }
}

/**
 * Asserts the value of [remoteProcessStateFlow] to be equal to [expectedValue]
 *
 * @see awaitValue
 */
private suspend fun TwoWayIpcSubject.assertRemoteObservedValue(expectedValue: String) {
    invokeInRemoteProcess(AssertRemoteObservedValue(expectedValue = expectedValue))
}
