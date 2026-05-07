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
package androidx.camera.camera2.pipe.internal

import android.graphics.SurfaceTexture
import android.hardware.camera2.CaptureRequest
import android.view.Surface
import androidx.camera.camera2.pipe.CameraGraphId
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraTimestamp
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.ParameterUpdateListener
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestFailure
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.StrictMode
import androidx.camera.camera2.pipe.compat.Camera2Quirks
import androidx.camera.camera2.pipe.graph.GraphProcessorImpl
import androidx.camera.camera2.pipe.graph.GraphRequestProcessor
import androidx.camera.camera2.pipe.graph.Listener3A
import androidx.camera.camera2.pipe.testing.FakeCamera2MetadataProvider
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor.Companion.graphParameters
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor.Companion.isRepeating
import androidx.camera.camera2.pipe.testing.FakeFrameInfo
import androidx.camera.camera2.pipe.testing.FakeGraphConfigs
import androidx.camera.camera2.pipe.testing.FakeGraphProcessor
import androidx.camera.camera2.pipe.testing.FakeMetadata.Companion.TEST_KEY
import androidx.camera.camera2.pipe.testing.FakeRequestFailure
import androidx.camera.camera2.pipe.testing.FakeRequestListener
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import androidx.camera.camera2.pipe.testing.FakeThreads
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [CameraGraphParametersImpl] */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class CameraGraphParametersImplTest {
    private val testScope = TestScope()

    private var parameters =
        CameraGraphParametersImpl(GraphSessionLock(), FakeGraphProcessor(), testScope)

    private val graphProcessor =
        GraphProcessorImpl(
            FakeThreads.fromTestScope(testScope),
            CameraGraphId.nextId(),
            FakeGraphConfigs.graphConfig,
            Listener3A(),
            arrayListOf(FakeRequestListener()),
            Camera2Quirks(
                metadataProvider =
                    FakeCamera2MetadataProvider(
                        mapOf(CameraId("0") to FakeCameraMetadata(cameraId = CameraId("0")))
                    ),
                strictMode = StrictMode(false),
            ),
        )
    private val surfaceMap = mapOf(StreamId(0) to Surface(SurfaceTexture(1)))
    private val csp1 = FakeCaptureSequenceProcessor().also { it.surfaceMap = surfaceMap }
    private val grp1 = GraphRequestProcessor.from(csp1)
    private val request1 = Request(listOf(StreamId(0)), listeners = listOf(FakeRequestListener()))
    private val requestMetadata = FakeRequestMetadata()
    private val frameNumber = FrameNumber(1)
    private val timestamp = CameraTimestamp(100)
    private val frameInfo = FakeFrameInfo(requestMetadata = requestMetadata)
    private val failure = FakeRequestFailure(requestMetadata, frameNumber)
    private val listener1 = FakeParameterUpdateListener(CAPTURE_REQUEST_KEY)
    private val listener2 = FakeParameterUpdateListener(CAPTURE_REQUEST_KEY)

    @Test
    fun get_returnLatestValue() {
        parameters[TEST_KEY] = 42
        parameters[CAPTURE_REQUEST_KEY] = 2
        parameters[TEST_NULLABLE_KEY] = null

        assertThat(parameters[TEST_KEY]).isEqualTo(42)
        assertThat(parameters[CAPTURE_REQUEST_KEY]).isEqualTo(2)
        assertThat(parameters[TEST_NULLABLE_KEY]).isNull()
    }

    @Test
    fun setAll_multipleEntriesSet() {
        parameters.setAll(
            mapOf(TEST_KEY to 42, CAPTURE_REQUEST_KEY to 2, TEST_NULLABLE_KEY to null)
        )

        assertThat(parameters[TEST_KEY]).isEqualTo(42)
        assertThat(parameters[CAPTURE_REQUEST_KEY]).isEqualTo(2)
        assertThat(parameters[TEST_NULLABLE_KEY]).isNull()
    }

    @Test
    fun remove_parameterRemoved() {
        parameters[TEST_KEY] = 42

        parameters.remove(TEST_KEY)

        assertThat(parameters[TEST_KEY]).isNull()
    }

    @Test
    fun removeAll_valuesEmpty() {
        parameters[TEST_KEY] = 42
        parameters[CAPTURE_REQUEST_KEY] = 2

        parameters.clear()

        assertThat(parameters[TEST_KEY]).isNull()
        assertThat(parameters[CAPTURE_REQUEST_KEY]).isNull()
    }

    @Test
    fun set_invokesUpdate() =
        testScope.runTest {
            graphProcessor.onGraphStarted(grp1)
            graphProcessor.repeatingRequest = request1

            val parameters =
                CameraGraphParametersImpl(GraphSessionLock(), graphProcessor, testScope)
            parameters[TEST_KEY] = 42
            advanceUntilIdle()

            // Check that the latest request with existing repeatingRequest has graphParameters
            assertThat(csp1.events.size).isEqualTo(2)
            assertThat(csp1.events[1].isRepeating).isTrue()
            assertThat(csp1.events[1].graphParameters).containsExactly(TEST_KEY, 42)
        }

    @Test
    fun setMultipleTimes_invokesUpdate() =
        testScope.runTest {
            graphProcessor.onGraphStarted(grp1)
            graphProcessor.repeatingRequest = request1

            val parameters =
                CameraGraphParametersImpl(GraphSessionLock(), graphProcessor, testScope)

            parameters[TEST_KEY] = 1
            parameters[TEST_KEY] = 2
            parameters[TEST_KEY] = 3

            advanceUntilIdle()

            // Verify that the final applied parameter is 3
            val lastEvent = csp1.events.last()
            assertThat(lastEvent.graphParameters[TEST_KEY]).isEqualTo(3)
        }

    @Test
    fun flush_applyUpdates() =
        testScope.runTest {
            graphProcessor.onGraphStarted(grp1)
            graphProcessor.repeatingRequest = request1

            val parameters =
                CameraGraphParametersImpl(GraphSessionLock(), graphProcessor, testScope)
            parameters[TEST_KEY] = 42

            parameters.flush()

            advanceUntilIdle()

            assertThat(csp1.events.last().graphParameters[TEST_KEY]).isEqualTo(42)
        }

    @Test
    fun flush_updatesWithoutSessionLockToken() =
        testScope.runTest {
            graphProcessor.onGraphStarted(grp1)
            graphProcessor.repeatingRequest = request1
            advanceUntilIdle()

            val lock = GraphSessionLock()
            val token = lock.tryAcquireToken()!!
            val parameters = CameraGraphParametersImpl(lock, graphProcessor, testScope)

            val eventsBeforeFlush = csp1.events.size

            parameters[TEST_KEY] = 42

            parameters.flush()

            advanceUntilIdle()

            assertThat(csp1.events.size).isGreaterThan(eventsBeforeFlush)
            assertThat(csp1.events.last().graphParameters[TEST_KEY]).isEqualTo(42)

            token.release()
        }

    @Test
    fun applyRequestComplete_invokesCallback() =
        testScope.runTest {
            graphProcessor.onGraphStarted(grp1)
            graphProcessor.repeatingRequest = request1
            advanceUntilIdle()
            val parameters =
                CameraGraphParametersImpl(GraphSessionLock(), graphProcessor, testScope)

            parameters.apply(CAPTURE_REQUEST_KEY, 42, listener1)
            advanceUntilIdle()

            val submitEvent =
                csp1.events.filterIsInstance<FakeCaptureSequenceProcessor.Submit>().last()
            val requestListener =
                submitEvent.captureSequence.listeners
                    .filterIsInstance<ParameterUpdateRequestListener>()
                    .last()
            requestListener.onStarted(requestMetadata, frameNumber, timestamp)
            assertThat(listener1.updateStarted).isTrue()

            requestListener.onComplete(requestMetadata, frameNumber, frameInfo)
            assertThat(listener1.updateCompleted).isTrue()
        }

    @Test
    fun applyRequestFailed_invokesCallback() =
        testScope.runTest {
            graphProcessor.onGraphStarted(grp1)
            graphProcessor.repeatingRequest = request1
            advanceUntilIdle()
            val parameters =
                CameraGraphParametersImpl(GraphSessionLock(), graphProcessor, testScope)

            parameters.apply(CAPTURE_REQUEST_KEY, 42, listener1)
            advanceUntilIdle()

            val submitEvent =
                csp1.events.filterIsInstance<FakeCaptureSequenceProcessor.Submit>().last()
            val requestListener =
                submitEvent.captureSequence.listeners
                    .filterIsInstance<ParameterUpdateRequestListener>()
                    .last()
            requestListener.onStarted(requestMetadata, frameNumber, timestamp)
            assertThat(listener1.updateStarted).isTrue()

            requestListener.onFailed(requestMetadata, frameNumber, failure)
            assertThat(listener1.updateSkipped).isTrue()
        }

    @Test
    fun apply_multipleListenersOnSameKeyDifferentValue_purgesPriorBeforeLastRequestComplete() =
        testScope.runTest {
            graphProcessor.onGraphStarted(grp1)
            graphProcessor.repeatingRequest = request1
            advanceUntilIdle()
            val parameters =
                CameraGraphParametersImpl(GraphSessionLock(), graphProcessor, testScope)

            parameters.apply(CAPTURE_REQUEST_KEY, 42, listener1)
            parameters.apply(CAPTURE_REQUEST_KEY, 43, listener2)
            advanceUntilIdle()

            assertThat(listener1.updateSkipped).isTrue()

            val submitEvent =
                csp1.events.filterIsInstance<FakeCaptureSequenceProcessor.Submit>().last()
            val updateListeners =
                submitEvent.captureSequence.listeners.filterIsInstance<
                    ParameterUpdateRequestListener
                >()
            assertThat(updateListeners).hasSize(1)

            val requestListener = updateListeners[0]
            assertThat(requestListener.clientListener).isEqualTo(listener2)

            requestListener.onComplete(requestMetadata, frameNumber, frameInfo)
            assertThat(listener2.updateCompleted).isTrue()
        }

    @Test
    fun apply_thenSet_purgesPriorListener() =
        testScope.runTest {
            val parameters =
                CameraGraphParametersImpl(GraphSessionLock(), graphProcessor, testScope)

            parameters.apply(CAPTURE_REQUEST_KEY, 42, listener1)
            advanceUntilIdle()
            parameters.set(CAPTURE_REQUEST_KEY, 43)
            advanceUntilIdle()

            assertThat(listener1.updateSkipped).isTrue()
        }

    @Test
    fun apply_thenRemove_purgesPriorListener() =
        testScope.runTest {
            val parameters =
                CameraGraphParametersImpl(GraphSessionLock(), graphProcessor, testScope)

            parameters.apply(CAPTURE_REQUEST_KEY, 42, listener1)
            advanceUntilIdle()
            parameters.remove(CAPTURE_REQUEST_KEY)
            advanceUntilIdle()

            assertThat(listener1.updateSkipped).isTrue()
        }

    @Test
    fun apply_thenClear_purgesPriorListener() =
        testScope.runTest {
            val parameters =
                CameraGraphParametersImpl(GraphSessionLock(), graphProcessor, testScope)

            parameters.apply(CAPTURE_REQUEST_KEY, 42, listener1)
            advanceUntilIdle()
            parameters.clear()
            advanceUntilIdle()

            assertThat(listener1.updateSkipped).isTrue()
        }

    @Test
    fun applyRequestAborted_invokesCallback() =
        testScope.runTest {
            graphProcessor.onGraphStarted(grp1)
            graphProcessor.repeatingRequest = request1
            advanceUntilIdle()
            val parameters =
                CameraGraphParametersImpl(GraphSessionLock(), graphProcessor, testScope)

            parameters.apply(CAPTURE_REQUEST_KEY, 42, listener1)
            advanceUntilIdle()

            val submitEvent =
                csp1.events.filterIsInstance<FakeCaptureSequenceProcessor.Submit>().last()
            val requestListener =
                submitEvent.captureSequence.listeners
                    .filterIsInstance<ParameterUpdateRequestListener>()
                    .last()

            requestListener.onAborted(request1)
            assertThat(listener1.updateSkipped).isTrue()
        }

    @Test
    fun applyRequestSequenceLifecycle_invokesCallbacks() =
        testScope.runTest {
            graphProcessor.onGraphStarted(grp1)
            graphProcessor.repeatingRequest = request1
            advanceUntilIdle()
            val parameters =
                CameraGraphParametersImpl(GraphSessionLock(), graphProcessor, testScope)

            parameters.apply(CAPTURE_REQUEST_KEY, 42, listener1)
            advanceUntilIdle()

            val submitEvent =
                csp1.events.filterIsInstance<FakeCaptureSequenceProcessor.Submit>().last()
            val requestListener =
                submitEvent.captureSequence.listeners
                    .filterIsInstance<ParameterUpdateRequestListener>()
                    .last()

            requestListener.onRequestSequenceCreated(requestMetadata)
            assertThat(listener1.updateRequestCreated).isTrue()

            requestListener.onRequestSequenceSubmitted(requestMetadata)
            assertThat(listener1.updateRequestSubmitted).isTrue()
        }

    private class FakeParameterUpdateListener(override val key: CaptureRequest.Key<*>) :
        ParameterUpdateListener {
        var updateStarted = false
        var updateCompleted = false
        var updateSkipped = false
        var updateRequestCreated = false
        var updateRequestSubmitted = false

        override fun onUpdateStarted(
            requestMetadata: RequestMetadata,
            frameNumber: FrameNumber,
            timestamp: CameraTimestamp,
        ) {
            updateStarted = true
        }

        override fun onUpdateCompleted(
            requestMetadata: RequestMetadata,
            frameNumber: FrameNumber,
            result: FrameInfo,
        ) {
            updateCompleted = true
        }

        override fun onUpdateRequestSubmitted(requestMetadata: RequestMetadata) {
            updateRequestSubmitted = true
        }

        override fun onUpdateRequestCreated(requestMetadata: RequestMetadata) {
            updateRequestCreated = true
        }

        override fun onUpdateSkipped(failure: RequestFailure?) {
            updateSkipped = true
        }
    }

    companion object {
        private val CAPTURE_REQUEST_KEY = CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION
        private val TEST_NULLABLE_KEY = CaptureRequest.BLACK_LEVEL_LOCK
    }
}
