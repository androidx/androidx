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
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.await
import androidx.work.inspection.WorkManagerInspectorProtocol.Command
import androidx.work.inspection.WorkManagerInspectorProtocol.TrackWorkManagerCommand
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
        testEnvironment.workManager.enqueue(request)
        testEnvironment.receiveEvent().let { event ->
            assertThat(event.hasWorkAdded()).isTrue()
            assertThat(event.workAdded.work.id).isEqualTo(request.stringId)
        }
        clearAllWorks()
        testEnvironment.receiveEvent().let { event ->
            assertThat(event.hasWorkRemoved()).isTrue()
            assertThat(event.workRemoved.id).isEqualTo(request.stringId)
        }
    }
}
