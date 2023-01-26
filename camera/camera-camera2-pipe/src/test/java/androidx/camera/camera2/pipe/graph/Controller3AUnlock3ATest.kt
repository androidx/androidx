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
import android.os.Build
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.RequestNumber
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeFrameMetadata
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
internal class Controller3AUnlock3ATest {
    private val graphTestContext = GraphTestContext()
    private val graphState3A = graphTestContext.graphProcessor.graphState3A
    private val graphProcessor = graphTestContext.graphProcessor
    private val captureSequenceProcessor = graphTestContext.captureSequenceProcessor
    private val listener3A = Listener3A()
    private val fakeMetadata =
        FakeCameraMetadata(
            mapOf(
                CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES to
                    intArrayOf(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            ),
        )
    private val controller3A = Controller3A(graphProcessor, fakeMetadata, graphState3A, listener3A)

    @After
    fun teardown() {
        graphTestContext.close()
    }

    @Test
    fun testUnlockAe() = runTest {
        val unLock3AAsyncTask = async { controller3A.unlock3A(ae = true) }

        // Launch a task to repeatedly invoke a given capture result.
        val repeatingJob = launch {
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
                            CaptureResult.CONTROL_AE_STATE to
                                CaptureResult.CONTROL_AE_STATE_LOCKED
                        )
                    )
                )
                delay(FRAME_RATE_MS)
            }
        }

        val result = unLock3AAsyncTask.await()
        // Result of unlock3A call shouldn't be complete yet since the AE is locked.
        assertThat(result.isCompleted).isFalse()

        // There should be one request to lock AE.
        val request1 = captureSequenceProcessor.nextEvent().requestSequence
        assertThat(request1!!.requiredParameters[CaptureRequest.CONTROL_AE_LOCK]).isEqualTo(false)

        repeatingJob.cancel()
        repeatingJob.join()

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
                            CaptureResult.CONTROL_AE_STATE_SEARCHING
                    )
                )
            )
        }

        val result3A = result.await()
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)
    }

    @Test
    fun testUnlockAf() = runTest {
        val unLock3AAsyncTask = async { controller3A.unlock3A(af = true) }

        // Launch a task to repeatedly invoke a given capture result.
        val repeatingJob = launch {
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
                                CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
                        )
                    )
                )
                delay(FRAME_RATE_MS)
            }
        }

        val result = unLock3AAsyncTask.await()
        // Result of unlock3A call shouldn't be complete yet since the AF is locked.
        assertThat(result.isCompleted).isFalse()

        // There should be one request to unlock AF.
        val request1 = captureSequenceProcessor.nextEvent().requestSequence
        assertThat(request1!!.requiredParameters[CaptureRequest.CONTROL_AF_TRIGGER])
            .isEqualTo(CaptureRequest.CONTROL_AF_TRIGGER_CANCEL)

        repeatingJob.cancel()
        repeatingJob.join()

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
                            CaptureResult.CONTROL_AF_STATE_INACTIVE
                    )
                )
            )
        }

        val result3A = result.await()
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)
    }

    @Test
    fun testUnlockAwb() = runTest {
        val unLock3AAsyncTask = async { controller3A.unlock3A(awb = true) }

        // Launch a task to repeatedly invoke a given capture result.
        val repeatingJob = launch {
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
                            CaptureResult.CONTROL_AWB_STATE to
                                CaptureResult.CONTROL_AWB_STATE_LOCKED
                        )
                    )
                )
                delay(FRAME_RATE_MS)
            }
        }

        val result = unLock3AAsyncTask.await()
        // Result of unlock3A call shouldn't be complete yet since the AWB is locked.
        assertThat(result.isCompleted).isFalse()

        // There should be one request to lock AWB.
        val request1 = captureSequenceProcessor.nextEvent().requestSequence
        assertThat(request1!!.requiredParameters[CaptureRequest.CONTROL_AWB_LOCK]).isEqualTo(false)

        repeatingJob.cancel()
        repeatingJob.join()

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
                        CaptureResult.CONTROL_AWB_STATE to
                            CaptureResult.CONTROL_AWB_STATE_SEARCHING
                    )
                )
            )
        }

        val result3A = result.await()
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)
    }

    @Test
    fun testUnlockAeAf() = runTest {
        val unLock3AAsyncTask = async { controller3A.unlock3A(ae = true, af = true) }

        // Launch a task to repeatedly invoke a given capture result.
        val repeatingJob = launch {
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
                            CaptureResult.CONTROL_AE_STATE to
                                CaptureResult.CONTROL_AE_STATE_LOCKED,
                            CaptureResult.CONTROL_AF_STATE to
                                CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
                        )
                    )
                )
                delay(FRAME_RATE_MS)
            }
        }

        val result = unLock3AAsyncTask.await()
        // Result of unlock3A call shouldn't be complete yet since the AF is locked.
        assertThat(result.isCompleted).isFalse()

        // There should be one request to unlock AF.
        val request1 = captureSequenceProcessor.nextEvent().requestSequence
        assertThat(request1!!.requiredParameters[CaptureRequest.CONTROL_AF_TRIGGER])
            .isEqualTo(CaptureRequest.CONTROL_AF_TRIGGER_CANCEL)
        // Then request to unlock AE.
        val request2 = captureSequenceProcessor.nextEvent().requestSequence
        assertThat(request2!!.requiredParameters[CaptureRequest.CONTROL_AE_LOCK]).isEqualTo(false)

        repeatingJob.cancel()
        repeatingJob.join()

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
                            CaptureResult.CONTROL_AF_STATE_INACTIVE,
                        CaptureResult.CONTROL_AE_STATE to
                            CaptureResult.CONTROL_AE_STATE_SEARCHING
                    )
                )
            )
        }

        val result3A = result.await()
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)
    }

    @Test
    fun testUnlockAfWhenAfNotSupported() = runTest {
        val fakeMetadata =
            FakeCameraMetadata(
                mapOf(
                    CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES to
                        intArrayOf(CaptureRequest.CONTROL_AF_MODE_OFF)
                ),
            )
        val controller3A = Controller3A(graphProcessor, fakeMetadata, graphState3A, listener3A)
        val result = controller3A.unlock3A(af = true).await()
        assertThat(result.status).isEqualTo(Result3A.Status.OK)
        assertThat(result.frameMetadata).isEqualTo(null)
    }

    companion object {
        // The time duration in milliseconds between two frame results.
        private const val FRAME_RATE_MS = 33L
    }
}
