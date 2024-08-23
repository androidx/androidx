/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.camera2.pipe.graph

import android.os.Build
import androidx.camera.camera2.pipe.CameraGraphId
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.testing.FakeFrameInfo
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.robolectric.annotation.Config

@RunWith(JUnit4::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class CaptureLimiterTest {
    private val testScope = TestScope()
    private val testDispatcher = StandardTestDispatcher(testScope.testScheduler)
    private val graphState3A = GraphState3A()

    private val fakeRequestMetadata = FakeRequestMetadata()
    private val fakeFrameInfo = FakeFrameInfo()

    private val captureLimiter = CaptureLimiter(3)
    private val cameraGraphId = CameraGraphId.nextId()

    private val graphLoop =
        GraphLoop(
            cameraGraphId = cameraGraphId,
            defaultParameters = emptyMap<Any, Any?>(),
            requiredParameters = emptyMap<Any, Any?>(),
            graphListeners = listOf(),
            graphState3A = graphState3A,
            listeners = listOf(captureLimiter),
            shutdownScope = testScope,
            dispatcher = testDispatcher,
        )

    init {
        captureLimiter.graphLoop = graphLoop
    }

    @Test
    fun captureLimiterEnablesCaptureProcessingAfterFrameCount() {
        assertThat(graphLoop.captureProcessingEnabled).isFalse()

        // ACT
        simulateFrames(2)
        assertThat(graphLoop.captureProcessingEnabled).isFalse()

        // ACT
        simulateFrames(1)
        assertThat(graphLoop.captureProcessingEnabled).isTrue()
    }

    @Test
    fun captureLimiterResetsAfterGraphProcessorIsRemoved() {
        simulateFrames(3)
        assertThat(graphLoop.captureProcessingEnabled).isTrue()

        // ACT
        graphLoop.requestProcessor = null

        assertThat(graphLoop.captureProcessingEnabled).isFalse()
        simulateFrames(3)
        assertThat(graphLoop.captureProcessingEnabled).isTrue()
    }

    @Test
    fun captureLimiterPermanentlyDisablesAfterClose() =
        testScope.runTest {
            simulateFrames(3)
            assertThat(graphLoop.captureProcessingEnabled).isTrue()

            // ACT
            graphLoop.close()
            assertThat(graphLoop.captureProcessingEnabled).isFalse()
            simulateFrames(3)
            assertThat(graphLoop.captureProcessingEnabled).isFalse()
        }

    private fun simulateFrames(count: Long) {
        for (i in 1L..count) {
            captureLimiter.onComplete(fakeRequestMetadata, FrameNumber(i), fakeFrameInfo)
        }
    }
}
