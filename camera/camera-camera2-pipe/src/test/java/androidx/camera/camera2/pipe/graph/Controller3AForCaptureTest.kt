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

@file:OptIn(ExperimentalCoroutinesApi::class)

package androidx.camera.camera2.pipe.graph

import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.os.Build
import androidx.camera.camera2.pipe.FrameMetadata
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.RequestNumber
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeFrameMetadata
import androidx.camera.camera2.pipe.testing.FakeGraphProcessor
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class Controller3AForCaptureTest {
    private val graphTestContext = GraphTestContext()
    private val graphState3A = graphTestContext.graphProcessor.graphState3A
    private val graphProcessor = graphTestContext.graphProcessor
    private val captureSequenceProcessor = graphTestContext.captureSequenceProcessor

    private val listener3A = Listener3A()
    private val controller3A =
        Controller3A(graphProcessor, FakeCameraMetadata(), graphState3A, listener3A)

    @After
    fun teardown() {
        graphTestContext.close()
    }

    @Test
    fun testLock3AForCaptureFailsImmediatelyWithoutRepeatingRequest() = runTest {
        val graphProcessor2 = FakeGraphProcessor()
        val controller3A =
            Controller3A(
                graphProcessor2,
                FakeCameraMetadata(),
                graphProcessor2.graphState3A,
                listener3A
            )
        val result = controller3A.lock3AForCapture()
        assertThat(result.await().status).isEqualTo(Result3A.Status.SUBMIT_FAILED)
    }

    @Test
    fun testLock3AForCapture() = runTest {
        val result = controller3A.lock3AForCapture()
        assertThat(result.isCompleted).isFalse()

        // Since requirement is to trigger both AF and AE precapture metering. The result of
        // lock3AForCapture call will complete once AE and AF have reached their desired states. In
        // this response i.e cameraResponse1, AF is still scanning so the result won't be complete.
        val cameraResponse = async {
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
                            CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN,
                        CaptureResult.CONTROL_AE_STATE to
                            CaptureResult.CONTROL_AE_STATE_SEARCHING
                    )
                )
            )
        }

        cameraResponse.await()
        assertThat(result.isCompleted).isFalse()

        // One we are notified that the AE and AF are in the desired states, the result of
        // lock3AForCapture call will complete.
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
                        CaptureResult.CONTROL_AF_STATE to
                            CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
                        CaptureResult.CONTROL_AE_STATE to
                            CaptureResult.CONTROL_AE_STATE_CONVERGED
                    )
                )
            )
        }

        val result3A = result.await()
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)

        // We now check if the correct sequence of requests were submitted by lock3AForCapture call.
        // There should be a request to trigger AF and AE precapture metering.
        val request1 = captureSequenceProcessor.nextEvent().requestSequence
        assertThat(request1!!.requiredParameters[CaptureRequest.CONTROL_AF_TRIGGER])
            .isEqualTo(CaptureRequest.CONTROL_AF_TRIGGER_START)
        assertThat(request1.requiredParameters[CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER])
            .isEqualTo(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START)
    }

    @Test
    fun testCustomizedExitCondition_lock3AForCaptureWithoutAeState() = runTest {
        // Arrange, prepare customized locked conditions which allow an empty AE/AF state.
        val lockCondition: (FrameMetadata) -> Boolean = lockCondition@{ frameMetadata ->
            val aeUnlocked = frameMetadata[CaptureResult.CONTROL_AE_STATE]?.let {
                listOf(
                    CaptureResult.CONTROL_AE_STATE_CONVERGED,
                    CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED,
                    CaptureResult.CONTROL_AE_STATE_LOCKED
                ).contains(it)
            } ?: true

            val afUnlocked = frameMetadata[CaptureResult.CONTROL_AF_STATE]?.let {
                listOf(
                    CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
                    CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED
                ).contains(it)
            } ?: true

            return@lockCondition aeUnlocked && afUnlocked
        }
        val result = controller3A.lock3AForCapture(lockedCondition = lockCondition)
        assertThat(result.isCompleted).isFalse()

        // Simulate repeatedly invoke the scanning state.
        val repeatingJob = async {
            var frameNumber = 100L
            while (frameNumber < 110L) {
                listener3A.onRequestSequenceCreated(
                    FakeRequestMetadata(requestNumber = RequestNumber(1))
                )
                listener3A.onPartialCaptureResult(
                    FakeRequestMetadata(requestNumber = RequestNumber(1)),
                    FrameNumber(frameNumber),
                    FakeFrameMetadata(
                        frameNumber = FrameNumber(frameNumber++),
                        resultMetadata =
                        mapOf(
                            CaptureResult.CONTROL_AF_STATE to
                                CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN,
                        )
                    )
                )
                delay(FRAME_RATE_MS)
            }
        }
        repeatingJob.await()
        assertThat(result.isCompleted).isFalse()

        // Act, simulate the locked result without AE state.
        listener3A.onRequestSequenceCreated(
            FakeRequestMetadata(requestNumber = RequestNumber(1))
        )
        listener3A.onPartialCaptureResult(
            FakeRequestMetadata(requestNumber = RequestNumber(1)),
            FrameNumber(120L),
            FakeFrameMetadata(
                frameNumber = FrameNumber(120L),
                resultMetadata =
                mapOf(
                    CaptureResult.CONTROL_AF_STATE to
                        CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
                )
            )
        )

        // Assert, task should be completed with Status.Ok
        val result3A = result.await()
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)
    }

    @Test
    fun testUnlock3APostCapture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            testUnlock3APostCaptureAndroidMAndAbove()
        } else {
            testUnlock3APostCaptureAndroidLAndBelow()
        }
    }

    private fun testUnlock3APostCaptureAndroidMAndAbove() = runTest {
        val result = controller3A.unlock3APostCapture()
        assertThat(result.isCompleted).isFalse()

        // In this response i.e cameraResponse1, AF is still scanning so the result won't be
        // complete.
        val cameraResponse = async {
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
                            CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
                        CaptureResult.CONTROL_AE_STATE to
                            CaptureResult.CONTROL_AE_STATE_CONVERGED
                    )
                )
            )
        }

        cameraResponse.await()
        assertThat(result.isCompleted).isFalse()

        // Once we are notified that the AF is in unlocked state, the result of unlock3APostCapture
        // call will complete. For AE we don't need to to check for a specific state, receiving the
        // capture result corresponding to the submitted request suffices.
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
                        CaptureResult.CONTROL_AF_STATE to
                            CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN,
                        CaptureResult.CONTROL_AE_STATE to
                            CaptureResult.CONTROL_AE_STATE_CONVERGED
                    )
                )
            )
        }

        val result3A = result.await()
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)

        // We now check if the correct sequence of requests were submitted by unlock3APostCapture
        // call. There should be a request to cancel AF and AE precapture metering.
        val request1 = captureSequenceProcessor.nextEvent().requestSequence
        assertThat(request1!!.requiredParameters[CaptureRequest.CONTROL_AF_TRIGGER])
            .isEqualTo(CaptureRequest.CONTROL_AF_TRIGGER_CANCEL)
        assertThat(request1.requiredParameters[CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER])
            .isEqualTo(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL)
    }

    private fun testUnlock3APostCaptureAndroidLAndBelow() = runTest {
        val result = controller3A.unlock3APostCapture()
        assertThat(result.isCompleted).isFalse()

        val cameraResponse = async {
            listener3A.onRequestSequenceCreated(
                FakeRequestMetadata(requestNumber = RequestNumber(1))
            )
            listener3A.onPartialCaptureResult(
                FakeRequestMetadata(requestNumber = RequestNumber(1)),
                FrameNumber(101L),
                FakeFrameMetadata(frameNumber = FrameNumber(101L), resultMetadata = mapOf())
            )
        }

        cameraResponse.await()
        val result3A = result.await()
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)

        // We now check if the correct sequence of requests were submitted by unlock3APostCapture
        // call. There should be a request to cancel AF and lock ae.
        val request1 = captureSequenceProcessor.nextEvent().requestSequence
        assertThat(request1!!.requiredParameters[CaptureRequest.CONTROL_AF_TRIGGER])
            .isEqualTo(CaptureRequest.CONTROL_AF_TRIGGER_CANCEL)
        assertThat(request1.requiredParameters[CaptureRequest.CONTROL_AE_LOCK]).isEqualTo(true)

        // Then another request to unlock ae.
        val request2 = captureSequenceProcessor.nextEvent().requestSequence
        assertThat(request2!!.requiredParameters[CaptureRequest.CONTROL_AE_LOCK]).isEqualTo(false)
    }

    companion object {
        // The time duration in milliseconds between two frame results.
        private const val FRAME_RATE_MS = 33L
    }
}
