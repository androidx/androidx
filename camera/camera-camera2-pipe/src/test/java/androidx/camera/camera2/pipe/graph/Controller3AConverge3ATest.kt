/*
 * Copyright 2026 The Android Open Source Project
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
import androidx.camera.camera2.pipe.Converge3ABehavior
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.RequestNumber
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor.Companion.isCapture
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor.Companion.isRepeating
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor.Companion.requiredParameters
import androidx.camera.camera2.pipe.testing.FakeFrameMetadata
import androidx.camera.camera2.pipe.testing.FakeGraphProcessor
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import com.google.common.truth.Truth.assertThat
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
internal class Controller3AConverge3ATest {
    private val graphTestContext = GraphTestContext()
    private val graphState3A = GraphState3A()
    private val graphProcessor = graphTestContext.graphProcessor
    private val captureSequenceProcessor = graphTestContext.captureSequenceProcessor

    private val listener3A = Listener3A()
    private val fakeMetadata =
        FakeCameraMetadata(
            mapOf(
                CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES to
                    intArrayOf(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            )
        )
    private val controller3A = Controller3A(graphProcessor, fakeMetadata, graphState3A, listener3A)

    @After
    fun teardown() {
        graphTestContext.close()
    }

    @Test
    fun testConverge3AFailsImmediatelyWithoutRepeatingRequest() = runTest {
        val graphProcessor2 = FakeGraphProcessor()
        val graphState3A = GraphState3A()
        val controller3A = Controller3A(graphProcessor2, fakeMetadata, graphState3A, listener3A)
        val result =
            controller3A.converge3A(
                afBehavior = Converge3ABehavior.AFTER_NEW_SCAN,
                aeRegions = listOf(MeteringRectangle(0, 0, 100, 200, 10)),
            )
        assertThat(result.await().status).isEqualTo(Result3A.Status.SUBMIT_FAILED)
    }

    @Test
    fun testAfAfterCurrentScanAeAfterCurrentScan() = runTest {
        val result3ADeferred =
            controller3A.converge3A(
                afBehavior = Converge3ABehavior.AFTER_CURRENT_SCAN,
                aeBehavior = Converge3ABehavior.AFTER_CURRENT_SCAN,
            )

        // One repeating request to ensure listener applies
        val event0 = captureSequenceProcessor.nextEvent()
        assertThat(event0.isRepeating).isTrue()

        // Simulate convergence
        backgroundScope.launch {
            while (true) {
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
                                CaptureResult.CONTROL_AF_STATE to
                                    CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED,
                                CaptureResult.CONTROL_AE_STATE to
                                    CaptureResult.CONTROL_AE_STATE_CONVERGED,
                            ),
                    ),
                )
                delay(FRAME_RATE_MS)
            }
        }

        val result3A = result3ADeferred.await()
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)

        backgroundScope.cancel()
    }

    @Test
    fun testAfAfterNewScanAeAfterNewScan() = runTest {
        val result3ADeferred =
            controller3A.converge3A(
                afBehavior = Converge3ABehavior.AFTER_NEW_SCAN,
                aeBehavior = Converge3ABehavior.AFTER_NEW_SCAN,
            )

        // 1. One request update metering regions and ae/awb locks.
        val event0 = captureSequenceProcessor.nextEvent()
        assertThat(event0.isCapture).isFalse()
        assertThat(event0.requiredParameters).containsEntry(CaptureRequest.CONTROL_AE_LOCK, false)

        // 2. One request to cancel AF to start a new scan.
        val event1 = captureSequenceProcessor.nextEvent()
        assertThat(event1.isCapture).isTrue()
        assertThat(event1.requiredParameters)
            .containsEntry(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_TRIGGER_CANCEL,
            )

        // 3. There should be one repeating request to attach the exit listener, and it should have
        // retained the unlock parameter in the repeating request.
        val event2 = captureSequenceProcessor.nextEvent()
        assertThat(event2.isRepeating).isTrue()
        assertThat(event2.requiredParameters).containsEntry(CaptureRequest.CONTROL_AE_LOCK, false)

        backgroundScope.launch {
            val frameCounter = atomic(0L)
            while (true) {
                val frameNumber = frameCounter.incrementAndGet()
                listener3A.onRequestSequenceCreated(
                    FakeRequestMetadata(requestNumber = RequestNumber(frameNumber))
                )
                listener3A.onPartialCaptureResult(
                    FakeRequestMetadata(requestNumber = RequestNumber(frameNumber)),
                    FrameNumber(101L),
                    FakeFrameMetadata(
                        frameNumber = FrameNumber(101L),
                        resultMetadata =
                            mapOf(
                                CaptureResult.CONTROL_AF_STATE to
                                    CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED,
                                CaptureResult.CONTROL_AE_STATE to
                                    CaptureResult.CONTROL_AE_STATE_CONVERGED,
                            ),
                    ),
                )
                delay(FRAME_RATE_MS)
            }
        }

        val result3A = result3ADeferred.await()

        // Ensure result completes correctly when the new scan converges.
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)

        backgroundScope.cancel()
    }

    @Test
    fun testConverge3AWithRegions() = runTest {
        val afMeteringRegion = MeteringRectangle(1, 1, 100, 100, 2)
        val aeMeteringRegion = MeteringRectangle(10, 15, 140, 140, 3)

        val result3ADeferred =
            controller3A.converge3A(
                aeRegions = listOf(aeMeteringRegion),
                afRegions = listOf(afMeteringRegion),
                afBehavior = Converge3ABehavior.AFTER_CURRENT_SCAN,
                aeBehavior = Converge3ABehavior.AFTER_CURRENT_SCAN,
            )
        assertThat(result3ADeferred.isCompleted).isFalse()

        // Verify GraphState3A gets correctly updated with regions
        val aeRegions = graphState3A.current.aeRegions!!
        assertThat(aeRegions.size).isEqualTo(1)
        assertThat(aeRegions[0]).isEqualTo(aeMeteringRegion)

        val afRegions = graphState3A.current.afRegions!!
        assertThat(afRegions.size).isEqualTo(1)
        assertThat(afRegions[0]).isEqualTo(afMeteringRegion)

        val event0 = captureSequenceProcessor.nextEvent()
        assertThat(event0.isRepeating).isTrue()

        // Trigger convergence manually
        listener3A.onRequestSequenceCreated(FakeRequestMetadata(requestNumber = RequestNumber(1)))
        listener3A.onPartialCaptureResult(
            FakeRequestMetadata(requestNumber = RequestNumber(1)),
            FrameNumber(101L),
            FakeFrameMetadata(
                frameNumber = FrameNumber(101L),
                resultMetadata =
                    mapOf(
                        CaptureResult.CONTROL_AF_STATE to
                            CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED,
                        CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_CONVERGED,
                    ),
            ),
        )

        val result3A = result3ADeferred.await()
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)
    }

    @Test
    fun testConverge3AWithUnsupportedAutoFocusTrigger() = runTest {
        val fakeMetadataNoAf =
            FakeCameraMetadata(
                mapOf(
                    CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES to
                        intArrayOf(CaptureRequest.CONTROL_AF_MODE_OFF)
                )
            )
        val controller3A = Controller3A(graphProcessor, fakeMetadataNoAf, graphState3A, listener3A)

        val result3ADeferred =
            controller3A.converge3A(afBehavior = Converge3ABehavior.AFTER_NEW_SCAN).await()

        // Because AF is sanitized to null on unsupported devices and other behaviors are null,
        // it should complete immediately.
        assertThat(result3ADeferred.status).isEqualTo(Result3A.Status.OK)
        assertThat(result3ADeferred.frameMetadata).isEqualTo(null)
    }

    companion object {
        // The time duration in milliseconds between two frame results.
        private const val FRAME_RATE_MS = 33L
    }
}
