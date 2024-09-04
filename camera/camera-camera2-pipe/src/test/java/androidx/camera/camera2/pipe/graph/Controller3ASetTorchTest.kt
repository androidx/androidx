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

import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
import android.hardware.camera2.CaptureResult
import android.os.Build
import androidx.camera.camera2.pipe.AeMode
import androidx.camera.camera2.pipe.FlashMode
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.RequestNumber
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeFrameMetadata
import androidx.camera.camera2.pipe.testing.FakeGraphProcessor
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
internal class Controller3ASetTorchTest {
    private val graphTestContext = GraphTestContext()
    private val graphState3A = graphTestContext.graphProcessor.graphState3A
    private val graphProcessor = graphTestContext.graphProcessor
    private val listener3A = Listener3A()
    private val controller3A =
        Controller3A(graphProcessor, FakeCameraMetadata(), graphState3A, listener3A)

    @After
    fun teardown() {
        graphTestContext.close()
    }

    @Test
    fun setTorchOn_withoutRepeatingRequest_failsImmediatelyWithNoGraphStateChange() = runTest {
        val graphProcessor2 = FakeGraphProcessor()
        val controller3A =
            Controller3A(
                graphProcessor2,
                FakeCameraMetadata(),
                graphProcessor2.graphState3A,
                listener3A
            )
        val result = controller3A.setTorchOn()
        assertThat(result.await().status).isEqualTo(Result3A.Status.SUBMIT_FAILED)
        assertThat(graphProcessor2.graphState3A.flashMode).isEqualTo(FlashMode.TORCH)
    }

    @Test
    fun setTorchOff_withoutRepeatingRequest_failsImmediatelyWithNoGraphStateChange() = runTest {
        val graphProcessor2 = FakeGraphProcessor()
        val controller3A =
            Controller3A(
                graphProcessor2,
                FakeCameraMetadata(),
                graphProcessor2.graphState3A,
                listener3A
            )
        val result = controller3A.setTorchOff()
        assertThat(result.await().status).isEqualTo(Result3A.Status.SUBMIT_FAILED)
        assertThat(graphProcessor2.graphState3A.flashMode).isEqualTo(FlashMode.OFF)
    }

    @Test
    fun setTorchOn_updatesGraphStateWithAeModeOnAndFlashModeTorch() = runTest {
        controller3A.setTorchOn()
        assertThat(graphState3A.aeMode!!.value).isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON)
        assertThat(graphState3A.flashMode!!.value).isEqualTo(CaptureRequest.FLASH_MODE_TORCH)
    }

    @Test
    fun setTorchOn_noCaptureResultProvided_resultIncomplete() = runTest {
        val result = controller3A.setTorchOn()
        assertThat(result.isCompleted).isFalse()
    }

    @Test
    fun setTorchOn_captureResultProvidedWithAeOnAndFlashTorch_returnsOkResult() = runTest {
        val result = controller3A.setTorchOn()

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
                            CaptureResult.CONTROL_AE_MODE to CaptureResult.CONTROL_AE_MODE_ON,
                            CaptureResult.FLASH_MODE to CaptureResult.FLASH_MODE_TORCH
                        )
                )
            )
        }

        val result3A = result.await()
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)
    }

    @Test
    fun setTorchOn_captureResultProvidedWithOnlyFlashTorch_resultIncomplete() = runTest {
        val result = controller3A.setTorchOn()

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
                            mapOf(CaptureResult.FLASH_MODE to CaptureResult.FLASH_MODE_TORCH)
                    )
                )
            }
            .join()

        assertThat(result.isCompleted).isFalse()
    }

    @Test
    fun setTorchOff_updatesGraphStateWithFlashModeOff() = runTest {
        controller3A.setTorchOff()
        assertThat(graphState3A.flashMode!!.value).isEqualTo(CaptureRequest.FLASH_MODE_OFF)
    }

    @Test
    fun setTorchOffWithoutAeMode_graphStateAeModeStaysNull() = runTest {
        controller3A.setTorchOff()
        assertThat(graphState3A.aeMode?.value).isNull() // null is default value here
    }

    @Test
    fun setTorchOffWithAutoFlashAeMode_graphStateAeModeUpdatedToAutoFlash() = runTest {
        controller3A.setTorchOff(aeMode = AeMode.ON_AUTO_FLASH)
        assertThat(graphState3A.aeMode?.value).isEqualTo(CONTROL_AE_MODE_ON_AUTO_FLASH)
    }

    @Test
    fun setTorchOff_noCaptureResultWithUpdatedStates_resultIncomplete() = runTest {
        val result = controller3A.setTorchOff()
        assertThat(result.isCompleted).isFalse()
    }

    @Test
    fun setTorchOffWithoutAeMode_captureResultProvidedWithFlashOff_returnsOkResult() = runTest {
        val result = controller3A.setTorchOff()

        launch {
            listener3A.onRequestSequenceCreated(
                FakeRequestMetadata(requestNumber = RequestNumber(1))
            )
            listener3A.onPartialCaptureResult(
                FakeRequestMetadata(requestNumber = RequestNumber(1)),
                FrameNumber(101L),
                FakeFrameMetadata(
                    frameNumber = FrameNumber(101L),
                    resultMetadata = mapOf(CaptureResult.FLASH_MODE to CaptureResult.FLASH_MODE_OFF)
                )
            )
        }
        val result3A = result.await()
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)
    }

    @Test
    fun setTorchOffWithAutoFlashAe_captureResultProvidedWithOnlyFlashOff_resultIncomplete() =
        runTest {
            val result = controller3A.setTorchOff(aeMode = AeMode.ON_AUTO_FLASH)

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
                                mapOf(CaptureResult.FLASH_MODE to CaptureResult.FLASH_MODE_OFF)
                        )
                    )
                }
                .join()

            assertThat(result.isCompleted).isFalse()
        }

    @Test
    fun setTorchOffWithAutoFlashAe_captureResultProvidedWithAutoAeAndFlashOff_returnsOkResult() =
        runTest {
            val result = controller3A.setTorchOff(aeMode = AeMode.ON_AUTO_FLASH)

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
                                    CaptureResult.CONTROL_AE_MODE_ON_AUTO_FLASH,
                                CaptureResult.FLASH_MODE to CaptureResult.FLASH_MODE_OFF
                            )
                    )
                )
            }
            val result3A = result.await()
            assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
            assertThat(result3A.status).isEqualTo(Result3A.Status.OK)
        }

    @Test
    fun setTorchOn_graphStateAlreadyAeOffSoNoChangeNeeded_aeModeUnchangedButFlashChangedToTorch() =
        runTest {
            graphState3A.update(aeMode = AeMode.OFF)

            controller3A.setTorchOn()
            assertThat(graphState3A.aeMode!!.value).isEqualTo(CaptureRequest.CONTROL_AE_MODE_OFF)
            assertThat(graphState3A.flashMode!!.value).isEqualTo(CaptureRequest.FLASH_MODE_TORCH)
        }

    @Test
    fun setTorchOnWithGraphStateAlreadyAeOff_captureResultProvidedWithFlashTorch_returnsOkResult() =
        runTest {
            graphState3A.update(aeMode = AeMode.OFF)

            val result = controller3A.setTorchOn()

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
                            mapOf(CaptureResult.FLASH_MODE to CaptureResult.FLASH_MODE_TORCH)
                    )
                )
            }
            val result3A = result.await()
            assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
            assertThat(result3A.status).isEqualTo(Result3A.Status.OK)
        }
}
