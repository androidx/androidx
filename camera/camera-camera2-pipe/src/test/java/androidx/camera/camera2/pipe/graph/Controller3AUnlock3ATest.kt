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
import androidx.camera.camera2.pipe.FrameMetadata
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.RequestNumber
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor.Companion.requiredParameters
import androidx.camera.camera2.pipe.testing.FakeFrameMetadata
import androidx.camera.camera2.pipe.testing.FakeGraphProcessor
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
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
    fun testUnlock3AFailsImmediatelyWithoutRepeatingRequest() = runTest {
        val graphProcessor2 = FakeGraphProcessor()
        val controller3A =
            Controller3A(graphProcessor2, fakeMetadata, graphProcessor2.graphState3A, listener3A)
        val result = controller3A.unlock3A(ae = true)
        assertThat(result.await().status).isEqualTo(Result3A.Status.SUBMIT_FAILED)
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
        val event1 = captureSequenceProcessor.nextEvent()
        assertThat(event1.requiredParameters).containsEntry(CaptureRequest.CONTROL_AE_LOCK, false)

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
        val event1 = captureSequenceProcessor.nextEvent()
        assertThat(event1.requiredParameters)
            .containsEntry(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_TRIGGER_CANCEL
            )

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
        val event1 = captureSequenceProcessor.nextEvent()
        assertThat(event1.requiredParameters).containsEntry(CaptureRequest.CONTROL_AWB_LOCK, false)

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
        val event1 = captureSequenceProcessor.nextEvent()
        assertThat(event1.requiredParameters)
            .containsEntry(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_TRIGGER_CANCEL
            )
        // Then request to unlock AE.
        val event2 = captureSequenceProcessor.nextEvent()
        assertThat(event2.requiredParameters).containsEntry(CaptureRequest.CONTROL_AE_LOCK, false)

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

    @Test
    fun testUnlockNeverConverge_frameLimitedReached() = runTest {
        // Arrange. Launch a task to repeatedly invoke AE Locked info.
        val frameLimit = 100
        val repeatingJob = launch {
            var frameNumber = 101L
            while (true) {
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
                                CaptureResult.CONTROL_AE_STATE to
                                    CaptureResult.CONTROL_AE_STATE_LOCKED
                            )
                    )
                )
                delay(FRAME_RATE_MS)
            }
        }

        // Act. Unlock AE
        val result3ADeferred = controller3A.unlock3A(ae = true, frameLimit = frameLimit)
        advanceTimeBy(FRAME_RATE_MS * frameLimit)
        result3ADeferred.await()

        // Assert. Result of unlock3A call should be completed with timeout result.
        assertThat(result3ADeferred.isCompleted).isTrue()
        val result = result3ADeferred.getCompleted()
        assertThat(result.status).isEqualTo(Result3A.Status.FRAME_LIMIT_REACHED)

        // Clean up
        repeatingJob.cancel()
        repeatingJob.join()
    }

    @Test
    fun testUnlockWithCustomizedExitCondition() = runTest {
        // Arrange. Launch a task to repeatedly invoke without AE state info.
        val repeatingJob = async {
            var frameNumber = 101L
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
                                    CaptureResult.CONTROL_AF_STATE_INACTIVE,
                                CaptureResult.CONTROL_AWB_STATE to
                                    CaptureResult.CONTROL_AWB_STATE_INACTIVE,
                            )
                    )
                )
                delay(FRAME_RATE_MS)
            }
        }

        val customizedExitCondition: (FrameMetadata) -> Boolean = exitCondition@{ frameMetadata ->
            val aeUnlocked =
                frameMetadata[CaptureResult.CONTROL_AE_STATE]?.let {
                    listOf(
                            CaptureResult.CONTROL_AE_STATE_INACTIVE,
                            CaptureResult.CONTROL_AE_STATE_SEARCHING,
                            CaptureResult.CONTROL_AE_STATE_CONVERGED,
                            CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED
                        )
                        .contains(it)
                } ?: true

            val afUnlocked =
                frameMetadata[CaptureResult.CONTROL_AF_STATE]?.let {
                    listOf(
                            CaptureResult.CONTROL_AF_STATE_INACTIVE,
                            CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN,
                            CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN,
                            CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED,
                            CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED
                        )
                        .contains(it)
                } ?: true

            val awbUnlocked =
                frameMetadata[CaptureResult.CONTROL_AWB_STATE]?.let {
                    listOf(
                            CaptureResult.CONTROL_AWB_STATE_INACTIVE,
                            CaptureResult.CONTROL_AWB_STATE_SEARCHING,
                            CaptureResult.CONTROL_AWB_STATE_CONVERGED
                        )
                        .contains(it)
                } ?: true

            return@exitCondition aeUnlocked && afUnlocked && awbUnlocked
        }

        // Act. Unlock AE
        val result3ADeferred =
            controller3A.unlock3A(
                ae = true,
                af = true,
                awb = true,
                unlockedCondition = customizedExitCondition,
            )
        repeatingJob.await()

        // Assert. Result of unlock3A call should be completed.
        assertThat(result3ADeferred.isCompleted).isTrue()
        assertThat(result3ADeferred.getCompleted().status).isEqualTo(Result3A.Status.OK)
    }

    companion object {
        // The time duration in milliseconds between two frame results.
        private const val FRAME_RATE_MS = 33L
    }
}
