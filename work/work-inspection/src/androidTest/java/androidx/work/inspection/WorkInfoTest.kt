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

package androidx.work.inspection

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.await
import androidx.work.impl.model.WorkSpec
import androidx.work.inspection.WorkManagerInspectorProtocol.Command
import androidx.work.inspection.WorkManagerInspectorProtocol.DataEntry
import androidx.work.inspection.WorkManagerInspectorProtocol.TrackWorkManagerCommand
import androidx.work.inspection.WorkManagerInspectorProtocol.WorkInfo.State
import androidx.work.inspection.worker.EmptyWorker
import androidx.work.inspection.worker.IdleWorker
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class WorkInfoTest {
    @get:Rule
    val testEnvironment = WorkManagerInspectorTestEnvironment()

    private suspend fun clearAllWorks() {
        testEnvironment.workManager.cancelAllWork().await()
        testEnvironment.workManager.pruneWork().await()
    }

    private suspend fun inspectWorkManager() {
        val command = Command.newBuilder()
            .setTrackWorkManager(TrackWorkManagerCommand.getDefaultInstance())
            .build()
        testEnvironment.sendCommand(command)
            .let { response ->
                assertThat(response.hasTrackWorkManager()).isEqualTo(true)
            }
    }

    @Test
    fun addAndRemoveWork() = runBlocking {
        inspectWorkManager()
        val request = OneTimeWorkRequestBuilder<EmptyWorker>().build()
        testEnvironment.workManager.enqueue(request).await()
        testEnvironment.receiveEvent().let { event ->
            assertThat(event.hasWorkAdded()).isTrue()
            assertThat(event.workAdded.work.id).isEqualTo(request.stringId)
        }
        clearAllWorks()
        testEnvironment.receiveFilteredEvent { event ->
            event.hasWorkRemoved()
        }.let { event ->
            assertThat(event.hasWorkRemoved()).isTrue()
            assertThat(event.workRemoved.id).isEqualTo(request.stringId)
        }
    }

    @Test
    fun sendWorkAddedEvent() = runBlocking {
        inspectWorkManager()
        val request = OneTimeWorkRequestBuilder<EmptyWorker>().build()
        testEnvironment.workManager.enqueue(request).await()
        testEnvironment.receiveEvent().let { event ->
            assertThat(event.hasWorkAdded()).isTrue()
            val workInfo = event.workAdded.work
            assertThat(workInfo.id).isEqualTo(request.stringId)
            assertThat(workInfo.isPeriodic).isFalse()
        }
    }

    @Test
    fun updateWorkInfoState() = runBlocking {
        inspectWorkManager()
        val request = OneTimeWorkRequestBuilder<EmptyWorker>().build()
        testEnvironment.workManager.enqueue(request).await()
        testEnvironment.receiveFilteredEvent { event ->
            event.hasWorkUpdated() && event.workUpdated.state == State.SUCCEEDED
        }.let { event ->
            assertThat(event.workUpdated.id).isEqualTo(request.stringId)
        }
    }

    @Test
    fun updateWorkInfoRetryCount() = runBlocking {
        inspectWorkManager()
        val request = OneTimeWorkRequestBuilder<EmptyWorker>().build()
        testEnvironment.workManager.enqueue(request).await()
        testEnvironment.receiveFilteredEvent { event ->
            event.hasWorkUpdated() && event.workUpdated.runAttemptCount == 1
        }.let { event ->
            assertThat(event.workUpdated.id).isEqualTo(request.stringId)
        }
    }

    @Test
    fun updateWorkInfoOutputData() = runBlocking {
        inspectWorkManager()
        val request = OneTimeWorkRequestBuilder<EmptyWorker>().build()
        testEnvironment.workManager.enqueue(request).await()
        testEnvironment.receiveFilteredEvent { event ->
            event.hasWorkUpdated() &&
                event.workUpdated.hasData() &&
                event.workUpdated.data.entriesCount == 1
        }.let { event ->
            assertThat(event.workUpdated.id).isEqualTo(request.stringId)
            val expectedEntry = DataEntry.newBuilder()
                .setKey("key")
                .setValue("value")
                .build()
            assertThat(event.workUpdated.data.getEntries(0)).isEqualTo(expectedEntry)
        }
    }

    @Test
    fun updateWorkInfoScheduleRequestedAt() = runBlocking {
        inspectWorkManager()
        val request = OneTimeWorkRequestBuilder<EmptyWorker>().build()
        testEnvironment.workManager.enqueue(request).await()
        testEnvironment.receiveFilteredEvent { event ->
            event.hasWorkUpdated() &&
                event.workUpdated.scheduleRequestedAt != WorkSpec.SCHEDULE_NOT_REQUESTED_YET
        }.let { event ->
            assertThat(event.workUpdated.id).isEqualTo(request.stringId)
        }
    }

    @Test
    fun runEntryHook_getCallStackWithWorkAddedEvent() = runBlocking {
        inspectWorkManager()
        val request = OneTimeWorkRequestBuilder<EmptyWorker>().build()
        val workContinuation = testEnvironment.workManager.beginWith(request)
        // a call stack should be recorded from WorkManagerInspector.
        testEnvironment.consumeRegisteredHooks()
            .first()
            .asEntryHook
            .onEntry(workContinuation, listOf())
        workContinuation.enqueue().await()

        testEnvironment.receiveEvent().let { event ->
            val workInfo = event.workAdded.work
            // onEntry is a SAM lambda and therefore the Kotlin compiler
            // can use invoke-dynamic to generate the lambda class. When
            // that happens the top stack frame is not
            // `WorkManagerInspector.onEntry`, instead it is a randomly named
            // lambda method. Therefore, we just check that there is a frame
            // on the stack with the method name `onEntry`. That can be
            // on a synthetic lambda class generated by D8 desugaring.
            val hasOnEntryStackFrame = workInfo.callStack.framesList.any {
                it.methodName.equals("onEntry")
            }
            assertThat(hasOnEntryStackFrame).isTrue()
        }
    }

    @Test
    fun addChainingWorkWithUniqueName() = runBlocking {
        inspectWorkManager()
        val work1 = OneTimeWorkRequestBuilder<EmptyWorker>().build()
        val work2 = OneTimeWorkRequestBuilder<EmptyWorker>().build()
        val name = "myName"
        testEnvironment.workManager.beginUniqueWork(name, ExistingWorkPolicy.REPLACE, work1)
            .then(work2)
            .enqueue()
            .await()
        for (count in 1..2) {
            testEnvironment.receiveEvent().let { event ->
                assertThat(event.hasWorkAdded()).isTrue()
                val workInfo = event.workAdded.work
                assertThat(workInfo.namesCount).isEqualTo(1)
                assertThat(workInfo.getNames(0)).isEqualTo(name)
                if (workInfo.id == work1.stringId) {
                    assertThat(workInfo.dependentsCount).isEqualTo(1)
                    assertThat(workInfo.getDependents(0)).isEqualTo(work2.stringId)
                }
                if (workInfo.id == work2.stringId) {
                    assertThat(workInfo.prerequisitesCount).isEqualTo(1)
                    assertThat(workInfo.getPrerequisites(0)).isEqualTo(work1.stringId)
                }
            }
        }
    }

    @Test
    fun cancelWork() = runBlocking {
        inspectWorkManager()
        val request = OneTimeWorkRequestBuilder<IdleWorker>().build()
        testEnvironment.workManager.enqueue(request).await()

        val cancelCommand = WorkManagerInspectorProtocol.CancelWorkCommand
            .newBuilder()
            .setId(request.stringId)
            .build()
        val command = Command.newBuilder().setCancelWork(cancelCommand).build()
        testEnvironment.sendCommand(command)

        testEnvironment.receiveFilteredEvent { event ->
            event.hasWorkUpdated() && event.workUpdated.state == State.CANCELLED
        }.let { event ->
            assertThat(event.workUpdated.id).isEqualTo(request.stringId)
        }
    }
}
