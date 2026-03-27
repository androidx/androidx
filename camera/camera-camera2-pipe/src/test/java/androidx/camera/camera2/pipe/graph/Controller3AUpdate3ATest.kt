/*
 * Copyright 2021 The Android Open Source Project
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

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.params.MeteringRectangle
import androidx.camera.camera2.pipe.AeMode
import androidx.camera.camera2.pipe.AfMode
import androidx.camera.camera2.pipe.AwbMode
import androidx.camera.camera2.pipe.ControlMode
import androidx.camera.camera2.pipe.FlashMode
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Lock3ABehavior
import androidx.camera.camera2.pipe.RequestNumber
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor.Companion.requiredParameters
import androidx.camera.camera2.pipe.testing.FakeFrameMetadata
import androidx.camera.camera2.pipe.testing.FakeGraphProcessor
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
internal class Controller3AUpdate3ATest {
    private val graphTestContext = GraphTestContext()
    private val graphState3A = GraphState3A()
    private val graphProcessor = graphTestContext.graphProcessor
    private val fakeCaptureSequenceProcessor = graphTestContext.captureSequenceProcessor
    private val fakeGraphRequestProcessor = GraphRequestProcessor.from(fakeCaptureSequenceProcessor)
    private val listener3A = Listener3A()

    private val fakeMetadata =
        FakeCameraMetadata(
            mapOf(
                CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES to
                    intArrayOf(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE),
                CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE to 0.1f,
            )
        )
    private val controller3A = Controller3A(graphProcessor, fakeMetadata, graphState3A, listener3A)

    @After
    fun teardown() {
        graphTestContext.close()
    }

    @Test
    fun testUpdate3AFailsImmediatelyWithoutRepeatingRequest() = runTest {
        val graphProcessor2 = FakeGraphProcessor()
        val graphState3A2 = GraphState3A()
        val controller3A =
            Controller3A(graphProcessor2, FakeCameraMetadata(), graphState3A2, listener3A)
        val result = controller3A.update3A(afMode = AfMode.OFF)
        assertThat(result.await().status).isEqualTo(Result3A.Status.SUBMIT_FAILED)
        assertThat(graphState3A2.current.afMode).isEqualTo(AfMode.OFF)
    }

    @Test
    fun testUpdate3AUpdatesState3A() {
        val result = controller3A.update3A(afMode = AfMode.OFF)
        assertThat(graphState3A.current.afMode!!.value)
            .isEqualTo(CaptureRequest.CONTROL_AF_MODE_OFF)
        assertThat(result.isCompleted).isFalse()
    }

    @ExperimentalCoroutinesApi
    @Test
    fun testUpdate3ACancelsPreviousInProgressUpdate() {
        val result = controller3A.update3A(afMode = AfMode.OFF)
        // Invoking update3A before the previous one is complete will cancel the result of the
        // previous call.
        controller3A.update3A(afMode = AfMode.CONTINUOUS_PICTURE)
        assertThat(result.getCompletionExceptionOrNull())
            .isInstanceOf(CancellationException::class.java)
    }

    @Test
    fun testAfModeUpdate() = runTest {
        val result = controller3A.update3A(afMode = AfMode.OFF)
        launch {
            listener3A.onRequestSequenceCreated(
                FakeRequestMetadata(requestNumber = RequestNumber(1))
            )
            listener3A.onPartialCaptureResult(
                FakeRequestMetadata(requestNumber = RequestNumber(1)),
                FrameNumber(101L),
                FakeFrameMetadata(
                    frameNumber = FrameNumber(101L),
                    resultMetadata =
                        mapOf(CaptureResult.CONTROL_AF_MODE to CaptureResult.CONTROL_AF_MODE_OFF),
                ),
            )
        }
        val result3A = result.await()
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)
    }

    @Test
    fun testAeModeUpdate() = runTest {
        val result = controller3A.update3A(aeMode = AeMode.ON_ALWAYS_FLASH)
        launch {
            listener3A.onRequestSequenceCreated(
                FakeRequestMetadata(requestNumber = RequestNumber(1))
            )
            listener3A.onPartialCaptureResult(
                FakeRequestMetadata(requestNumber = RequestNumber(1)),
                FrameNumber(101L),
                FakeFrameMetadata(
                    frameNumber = FrameNumber(101L),
                    resultMetadata =
                        mapOf(
                            CaptureResult.CONTROL_AE_MODE to
                                CaptureResult.CONTROL_AE_MODE_ON_ALWAYS_FLASH
                        ),
                ),
            )
        }
        val result3A = result.await()
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)
    }

    @Test
    fun testAwbModeUpdate() = runTest {
        val result = controller3A.update3A(awbMode = AwbMode.CLOUDY_DAYLIGHT)
        launch {
            listener3A.onRequestSequenceCreated(
                FakeRequestMetadata(requestNumber = RequestNumber(1))
            )
            listener3A.onPartialCaptureResult(
                FakeRequestMetadata(requestNumber = RequestNumber(1)),
                FrameNumber(101L),
                FakeFrameMetadata(
                    frameNumber = FrameNumber(101L),
                    resultMetadata =
                        mapOf(
                            CaptureResult.CONTROL_AWB_MODE to
                                CaptureResult.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT
                        ),
                ),
            )
        }
        val result3A = result.await()
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)
    }

    @Test
    fun testControlModeUpdate() = runTest {
        val result = controller3A.update3A(controlMode = ControlMode.OFF)
        launch {
            listener3A.onRequestSequenceCreated(
                FakeRequestMetadata(requestNumber = RequestNumber(1))
            )
            listener3A.onPartialCaptureResult(
                FakeRequestMetadata(requestNumber = RequestNumber(1)),
                FrameNumber(101L),
                FakeFrameMetadata(
                    frameNumber = FrameNumber(101L),
                    resultMetadata =
                        mapOf(CaptureResult.CONTROL_MODE to CaptureResult.CONTROL_MODE_OFF),
                ),
            )
        }
        val result3A = result.await()
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)
    }

    @Test
    fun testFlashModeUpdate() = runTest {
        val result = controller3A.update3A(flashMode = FlashMode.SINGLE)
        launch {
            listener3A.onRequestSequenceCreated(
                FakeRequestMetadata(requestNumber = RequestNumber(1))
            )
            listener3A.onPartialCaptureResult(
                FakeRequestMetadata(requestNumber = RequestNumber(1)),
                FrameNumber(101L),
                FakeFrameMetadata(
                    frameNumber = FrameNumber(101L),
                    resultMetadata =
                        mapOf(CaptureResult.FLASH_MODE to CaptureResult.FLASH_MODE_SINGLE),
                ),
            )
        }
        val result3A = result.await()
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)
    }

    @Test
    fun testAfRegionsUpdate() = runTest {
        val result = controller3A.update3A(afRegions = listOf(MeteringRectangle(1, 1, 100, 100, 2)))
        launch {
            listener3A.onRequestSequenceCreated(
                FakeRequestMetadata(requestNumber = RequestNumber(1))
            )
            listener3A.onPartialCaptureResult(
                FakeRequestMetadata(requestNumber = RequestNumber(1)),
                FrameNumber(101L),
                FakeFrameMetadata(
                    frameNumber = FrameNumber(101L),
                    resultMetadata =
                        mapOf(
                            CaptureResult.CONTROL_AF_REGIONS to
                                Array(1) { MeteringRectangle(1, 1, 99, 99, 2) }
                        ),
                ),
            )
        }
        val result3A = result.await()
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)
    }

    @Test
    fun testAeRegionsUpdate() = runTest {
        val result = controller3A.update3A(aeRegions = listOf(MeteringRectangle(1, 1, 100, 100, 2)))
        launch {
            listener3A.onRequestSequenceCreated(
                FakeRequestMetadata(requestNumber = RequestNumber(1))
            )
            listener3A.onPartialCaptureResult(
                FakeRequestMetadata(requestNumber = RequestNumber(1)),
                FrameNumber(101L),
                FakeFrameMetadata(
                    frameNumber = FrameNumber(101L),
                    resultMetadata =
                        mapOf(
                            CaptureResult.CONTROL_AE_REGIONS to
                                Array(1) { MeteringRectangle(1, 1, 99, 99, 2) }
                        ),
                ),
            )
        }
        val result3A = result.await()
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)
    }

    @Test
    fun testAwbRegionsUpdate() = runTest {
        val result =
            controller3A.update3A(awbRegions = listOf(MeteringRectangle(1, 1, 100, 100, 2)))
        launch {
            listener3A.onRequestSequenceCreated(
                FakeRequestMetadata(requestNumber = RequestNumber(1))
            )
            listener3A.onPartialCaptureResult(
                FakeRequestMetadata(requestNumber = RequestNumber(1)),
                FrameNumber(101L),
                FakeFrameMetadata(
                    frameNumber = FrameNumber(101L),
                    resultMetadata =
                        mapOf(
                            CaptureResult.CONTROL_AWB_REGIONS to
                                Array(1) { MeteringRectangle(1, 1, 99, 99, 2) }
                        ),
                ),
            )
        }
        val result3A = result.await()
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)
    }

    @Test
    fun testRetainLocksTrue_3ALockedAndAfContinuous_retains3ALock() = runTest {
        val lockResult =
            controller3A.lock3A(
                aeLockBehavior = Lock3ABehavior.IMMEDIATE,
                afLockBehavior = Lock3ABehavior.IMMEDIATE,
                awbLockBehavior = Lock3ABehavior.IMMEDIATE,
            )
        launch {
            listener3A.onRequestSequenceCreated(
                FakeRequestMetadata(requestNumber = RequestNumber(1))
            )
            listener3A.onPartialCaptureResult(
                FakeRequestMetadata(requestNumber = RequestNumber(1)),
                FrameNumber(101L),
                FakeFrameMetadata(
                    frameNumber = FrameNumber(101L),
                    resultMetadata =
                        mapOf(
                            CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_LOCKED,
                            CaptureResult.CONTROL_AWB_STATE to
                                CaptureResult.CONTROL_AWB_STATE_LOCKED,
                            CaptureResult.CONTROL_AF_STATE to
                                CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
                        ),
                ),
            )
        }
        lockResult.await()
        fakeCaptureSequenceProcessor.nextEvent() // repeating request event
        fakeCaptureSequenceProcessor.nextEvent() // lock request event
        fakeCaptureSequenceProcessor.nextEvent()

        val result = controller3A.update3A(afMode = AfMode.CONTINUOUS_PICTURE, retainLocks = true)
        launch {
            listener3A.onRequestSequenceCreated(
                FakeRequestMetadata(requestNumber = RequestNumber(1))
            )
            listener3A.onPartialCaptureResult(
                FakeRequestMetadata(requestNumber = RequestNumber(1)),
                FrameNumber(101L),
                FakeFrameMetadata(
                    frameNumber = FrameNumber(101L),
                    resultMetadata =
                        mapOf(
                            CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_LOCKED,
                            CaptureResult.CONTROL_AWB_STATE to
                                CaptureResult.CONTROL_AWB_STATE_LOCKED,
                            CaptureResult.CONTROL_AF_MODE to
                                CaptureResult.CONTROL_AF_MODE_CONTINUOUS_PICTURE,
                            CaptureResult.CONTROL_AF_TRIGGER to
                                CaptureResult.CONTROL_AF_TRIGGER_START,
                        ),
                ),
            )
        }
        val result3A = result.await()
        val event = fakeCaptureSequenceProcessor.nextEvent() // update3a request event

        assertThat(event.requiredParameters).containsEntry(CaptureRequest.CONTROL_AE_LOCK, true)
        assertThat(event.requiredParameters).containsEntry(CaptureRequest.CONTROL_AWB_LOCK, true)
        assertThat(event.requiredParameters)
            .containsEntry(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_TRIGGER_START,
            )
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)
    }

    @Test
    fun testRetainLocksTrue_3ALockedAndAfNotContinuous_doesNotRetainAfLockButRetainsAeAndAwbLock() =
        runTest {
            val lockResult =
                controller3A.lock3A(
                    aeLockBehavior = Lock3ABehavior.IMMEDIATE,
                    afLockBehavior = Lock3ABehavior.IMMEDIATE,
                    awbLockBehavior = Lock3ABehavior.IMMEDIATE,
                )
            launch {
                listener3A.onRequestSequenceCreated(
                    FakeRequestMetadata(requestNumber = RequestNumber(1))
                )
                listener3A.onPartialCaptureResult(
                    FakeRequestMetadata(requestNumber = RequestNumber(1)),
                    FrameNumber(101L),
                    FakeFrameMetadata(
                        frameNumber = FrameNumber(101L),
                        resultMetadata =
                            mapOf(
                                CaptureResult.CONTROL_AE_STATE to
                                    CaptureResult.CONTROL_AE_STATE_LOCKED,
                                CaptureResult.CONTROL_AWB_STATE to
                                    CaptureResult.CONTROL_AWB_STATE_LOCKED,
                                CaptureResult.CONTROL_AF_STATE to
                                    CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
                            ),
                    ),
                )
            }
            lockResult.await()
            fakeCaptureSequenceProcessor.nextEvent() // repeating request event
            fakeCaptureSequenceProcessor.nextEvent() // lock request event
            fakeCaptureSequenceProcessor.nextEvent() // lock request event

            val result = controller3A.update3A(afMode = AfMode.OFF, retainLocks = true)
            launch {
                listener3A.onRequestSequenceCreated(
                    FakeRequestMetadata(requestNumber = RequestNumber(1))
                )
                listener3A.onPartialCaptureResult(
                    FakeRequestMetadata(requestNumber = RequestNumber(1)),
                    FrameNumber(101L),
                    FakeFrameMetadata(
                        frameNumber = FrameNumber(101L),
                        resultMetadata =
                            mapOf(
                                CaptureResult.CONTROL_AE_STATE to
                                    CaptureResult.CONTROL_AE_STATE_LOCKED,
                                CaptureResult.CONTROL_AWB_STATE to
                                    CaptureResult.CONTROL_AWB_STATE_LOCKED,
                                CaptureResult.CONTROL_AF_MODE to CaptureResult.CONTROL_AF_MODE_OFF,
                            ),
                    ),
                )
            }
            val result3A = result.await()
            val event = fakeCaptureSequenceProcessor.nextEvent() // update3a request event

            assertThat(event.requiredParameters).containsEntry(CaptureRequest.CONTROL_AE_LOCK, true)
            assertThat(event.requiredParameters)
                .containsEntry(CaptureRequest.CONTROL_AWB_LOCK, true)
            assertThat(event.requiredParameters)
                .doesNotContainEntry(
                    CaptureRequest.CONTROL_AF_TRIGGER,
                    CaptureRequest.CONTROL_AF_TRIGGER_START,
                )
            assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
            assertThat(result3A.status).isEqualTo(Result3A.Status.OK)
        }

    @Test
    fun testRetainLocksTrue_3AUnlockedWithContinuousAfMode_leaves3AUnlocked() = runTest {
        val lockResult = controller3A.unlock3A(ae = true, af = true, awb = true)
        launch {
            listener3A.onRequestSequenceCreated(
                FakeRequestMetadata(requestNumber = RequestNumber(1))
            )
            listener3A.onPartialCaptureResult(
                FakeRequestMetadata(requestNumber = RequestNumber(1)),
                FrameNumber(101L),
                FakeFrameMetadata(
                    frameNumber = FrameNumber(101L),
                    resultMetadata =
                        mapOf(
                            CaptureResult.CONTROL_AE_STATE to
                                CaptureResult.CONTROL_AE_STATE_SEARCHING,
                            CaptureResult.CONTROL_AWB_STATE to
                                CaptureResult.CONTROL_AWB_STATE_SEARCHING,
                            CaptureResult.CONTROL_AF_STATE to
                                CaptureResult.CONTROL_AF_STATE_INACTIVE,
                        ),
                ),
            )
        }
        lockResult.await()

        val result = controller3A.update3A(afMode = AfMode.CONTINUOUS_PICTURE, retainLocks = true)
        launch {
            listener3A.onRequestSequenceCreated(
                FakeRequestMetadata(requestNumber = RequestNumber(1))
            )
            listener3A.onPartialCaptureResult(
                FakeRequestMetadata(requestNumber = RequestNumber(1)),
                FrameNumber(101L),
                FakeFrameMetadata(
                    frameNumber = FrameNumber(101L),
                    resultMetadata =
                        mapOf(
                            CaptureResult.CONTROL_AF_MODE to
                                CaptureResult.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                        ),
                ),
            )
        }
        val result3A = result.await()

        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)
    }

    @Test
    fun testRetainLocksFalse_3ALocked_doesNotRetainLocksFor3A() = runTest {
        val lockResult =
            controller3A.lock3A(
                aeLockBehavior = Lock3ABehavior.IMMEDIATE,
                afLockBehavior = Lock3ABehavior.IMMEDIATE,
                awbLockBehavior = Lock3ABehavior.IMMEDIATE,
            )
        launch {
            listener3A.onRequestSequenceCreated(
                FakeRequestMetadata(requestNumber = RequestNumber(1))
            )
            listener3A.onPartialCaptureResult(
                FakeRequestMetadata(requestNumber = RequestNumber(1)),
                FrameNumber(101L),
                FakeFrameMetadata(
                    frameNumber = FrameNumber(101L),
                    resultMetadata =
                        mapOf(
                            CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_LOCKED,
                            CaptureResult.CONTROL_AWB_STATE to
                                CaptureResult.CONTROL_AWB_STATE_LOCKED,
                            CaptureResult.CONTROL_AF_STATE to
                                CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
                        ),
                ),
            )
        }
        lockResult.await()

        val result = controller3A.update3A(retainLocks = false)
        launch {
            listener3A.onRequestSequenceCreated(
                FakeRequestMetadata(requestNumber = RequestNumber(1))
            )
            listener3A.onPartialCaptureResult(
                FakeRequestMetadata(requestNumber = RequestNumber(1)),
                FrameNumber(101L),
                FakeFrameMetadata(
                    frameNumber = FrameNumber(101L),
                    resultMetadata =
                        mapOf(
                            CaptureResult.CONTROL_AE_STATE to
                                CaptureResult.CONTROL_AE_STATE_SEARCHING,
                            CaptureResult.CONTROL_AWB_STATE to
                                CaptureResult.CONTROL_AWB_STATE_SEARCHING,
                            CaptureResult.CONTROL_AF_STATE to
                                CaptureResult.CONTROL_AF_STATE_INACTIVE,
                        ),
                ),
            )
        }
        val result3A = result.await()

        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)
    }
}
