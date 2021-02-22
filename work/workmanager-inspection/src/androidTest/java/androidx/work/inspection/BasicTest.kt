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
import androidx.work.inspection.WorkManagerInspectorProtocol.Command
import androidx.work.inspection.WorkManagerInspectorProtocol.TrackWorkManagerCommand
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class BasicTest {
    @get:Rule
    val testEnvironment = WorkManagerInspectorTestEnvironment()

    @Test
    fun createInspector() {
        // no crash means the inspector was successfully injected
        testEnvironment.assertNoQueuedEvents()
    }

    @Test
    fun sendUnsetCommand() = runBlocking {
        testEnvironment.sendCommand(Command.getDefaultInstance())
            .let { response ->
                assertThat(response.hasError()).isEqualTo(true)
                assertThat(response.error.content)
                    .contains("Unrecognised command type: ONEOF_NOT_SET")
            }
    }

    @Test
    fun sendTrackWorkManagerCommand() = runBlocking {
        val trackCommand = TrackWorkManagerCommand.getDefaultInstance()
        testEnvironment.sendCommand(
            Command.newBuilder().setTrackWorkManager(trackCommand).build()
        )
            .let { response ->
                assertThat(response.hasTrackWorkManager()).isEqualTo(true)
            }
    }
}
