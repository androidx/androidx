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
import android.os.Build
import androidx.camera.camera2.pipe.FrameMetadata
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Lock3ABehavior
import androidx.camera.camera2.pipe.RequestNumber
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor.Companion.isCapture
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor.Companion.isRepeating
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor.Companion.requests
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor.Companion.requiredParameters
import androidx.camera.camera2.pipe.testing.FakeFrameMetadata
import androidx.camera.camera2.pipe.testing.FakeGraphProcessor
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
internal class Controller3ALock3ATest {
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
    fun testLock3AFailsImmediatelyWithoutRepeatingRequest() = runTest {
        val graphProcessor2 = FakeGraphProcessor()
        val controller3A =
            Controller3A(graphProcessor2, fakeMetadata, graphProcessor2.graphState3A, listener3A)
        val result =
            controller3A.lock3A(
                afLockBehavior = Lock3ABehavior.IMMEDIATE,
                aeRegions = listOf(MeteringRectangle(0, 0, 100, 200, 10))
            )
        assertThat(result.await().status).isEqualTo(Result3A.Status.SUBMIT_FAILED)
        assertThat(graphProcessor2.graphState3A.aeRegions).isNotNull()
        assertThat(graphProcessor2.graphState3A.aeRegions)
            .containsExactly(MeteringRectangle(0, 0, 100, 200, 10))
    }

    @Test
    fun testAfImmediateAeImmediate() = runTest {
        val result =
            controller3A.lock3A(
                afLockBehavior = Lock3ABehavior.IMMEDIATE,
                aeLockBehavior = Lock3ABehavior.IMMEDIATE
            )
        assertThat(result.isCompleted).isFalse()

        // Since requirement of to lock both AE and AF immediately, the requests to lock AE and AF
        // are sent right away. The result of lock3A call will complete once AE and AF have reached
        // their desired states. In this response i.e cameraResponse1, AF is still scanning so the
        // result won't be complete.
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
                            CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_LOCKED
                        )
                )
            )
        }

        cameraResponse.await()
        assertThat(result.isCompleted).isFalse()

        // One we we are notified that the AE and AF are in locked state, the result of lock3A call
        // will complete.
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
                            CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_LOCKED
                        )
                )
            )
        }

        val result3A = result.await()
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)

        // We not check if the correct sequence of requests were submitted by lock3A call. The
        // request should be a repeating request to lock AE.
        val event1 = captureSequenceProcessor.nextEvent()
        assertThat(event1.requiredParameters).containsEntry(CaptureRequest.CONTROL_AE_LOCK, true)

        // The second request should be a single request to lock AF.
        val event2 = captureSequenceProcessor.nextEvent()
        assertThat(event1.requiredParameters).containsEntry(CaptureRequest.CONTROL_AE_LOCK, true)
        assertThat(event2.requiredParameters)
            .containsEntry(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_TRIGGER_START
            )
    }

    @Test
    fun testAfImmediateAeAfterCurrentScan() = runTest {
        val globalScope = CoroutineScope(UnconfinedTestDispatcher())

        val lock3AAsyncTask =
            globalScope.async {
                controller3A.lock3A(
                    afLockBehavior = Lock3ABehavior.IMMEDIATE,
                    aeLockBehavior = Lock3ABehavior.AFTER_CURRENT_SCAN
                )
            }
        assertThat(lock3AAsyncTask.isCompleted).isFalse()
        // Launch a task to repeatedly invoke a given capture result.
        globalScope.launch {
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
                                    CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN,
                                CaptureResult.CONTROL_AE_STATE to
                                    CaptureResult.CONTROL_AE_STATE_CONVERGED
                            )
                    )
                )
                delay(FRAME_RATE_MS)
            }
        }

        val result = lock3AAsyncTask.await()
        // Result of lock3A call shouldn't be complete yet since the AE and AF are not locked yet.
        assertThat(result.isCompleted).isFalse()

        // Check the correctness of the requests submitted by lock3A.
        // One repeating request was sent to monitor the state of AE to get converged.
        val event1 = captureSequenceProcessor.nextEvent()
        assertThat(event1.isRepeating).isTrue()

        // Once AE is converged, another repeating request is sent to lock AE.
        val event2 = captureSequenceProcessor.nextEvent()
        assertThat(event2.requiredParameters).containsEntry(CaptureRequest.CONTROL_AE_LOCK, true)

        globalScope.launch {
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
                            CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_LOCKED
                        )
                )
            )
        }

        val result3A = result.await()
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)

        // A single request to lock AF must have been used as well.
        val event3 = captureSequenceProcessor.nextEvent()
        assertThat(event3.requiredParameters)
            .containsEntry(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_TRIGGER_START
            )
        globalScope.cancel()
    }

    @Test
    fun testAfImmediateAeAfterNewScan() = runTest {
        val globalScope = CoroutineScope(UnconfinedTestDispatcher())

        val lock3AAsyncTask =
            globalScope.async {
                controller3A.lock3A(
                    afLockBehavior = Lock3ABehavior.IMMEDIATE,
                    aeLockBehavior = Lock3ABehavior.AFTER_NEW_SCAN
                )
            }
        assertThat(lock3AAsyncTask.isCompleted).isFalse()

        globalScope.launch {
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
                                    CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN,
                                CaptureResult.CONTROL_AE_STATE to
                                    CaptureResult.CONTROL_AE_STATE_CONVERGED
                            )
                    )
                )
                delay(FRAME_RATE_MS)
            }
        }

        val result = lock3AAsyncTask.await()
        assertThat(result.isCompleted).isFalse()

        // For a new AE scan we first send a request to unlock AE just in case it was
        // previously or internally locked.
        val event1 = captureSequenceProcessor.nextEvent()
        assertThat(event1.requiredParameters).containsEntry(CaptureRequest.CONTROL_AE_LOCK, false)

        globalScope.launch {
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
                            CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_LOCKED
                        )
                )
            )
        }

        val result3A = result.await()
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)

        // There should be one more request to lock AE after new scan is done.
        val event2 = captureSequenceProcessor.nextEvent()
        assertThat(event2.requiredParameters).containsEntry(CaptureRequest.CONTROL_AE_LOCK, true)

        // And one request to lock AF.
        val event3 = captureSequenceProcessor.nextEvent()
        assertThat(event3.requiredParameters).containsEntry(CaptureRequest.CONTROL_AE_LOCK, true)
        assertThat(event3.requiredParameters)
            .containsEntry(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_TRIGGER_START
            )

        globalScope.cancel()
    }

    @Test
    fun testAfAfterCurrentScanAeImmediate() = runTest {
        val globalScope = CoroutineScope(UnconfinedTestDispatcher())

        val lock3AAsyncTask =
            globalScope.async {
                controller3A.lock3A(
                    afLockBehavior = Lock3ABehavior.AFTER_CURRENT_SCAN,
                    aeLockBehavior = Lock3ABehavior.IMMEDIATE
                )
            }
        assertThat(lock3AAsyncTask.isCompleted).isFalse()
        globalScope.launch {
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
                                    CaptureResult.CONTROL_AE_STATE_CONVERGED
                            )
                    )
                )
                delay(FRAME_RATE_MS)
            }
        }

        val result = lock3AAsyncTask.await()
        assertThat(result.isCompleted).isFalse()

        globalScope.launch {
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
                            CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_LOCKED
                        )
                )
            )
        }

        val result3A = result.await()
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)

        // There should be one request to monitor AF to finish it's scan.
        captureSequenceProcessor.nextEvent()
        // One request to lock AE
        val event2 = captureSequenceProcessor.nextEvent()
        assertThat(event2.requiredParameters).containsEntry(CaptureRequest.CONTROL_AE_LOCK, true)

        // And one request to lock AF.
        val event3 = captureSequenceProcessor.nextEvent()
        assertThat(event3.requiredParameters).containsEntry(CaptureRequest.CONTROL_AE_LOCK, true)
        assertThat(event3.requiredParameters)
            .containsEntry(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_TRIGGER_START
            )
        globalScope.cancel()
    }

    @Test
    fun testAfAfterNewScanScanAeImmediate() = runTest {
        val globalScope = CoroutineScope(UnconfinedTestDispatcher())

        val lock3AAsyncTask =
            globalScope.async {
                controller3A.lock3A(
                    afLockBehavior = Lock3ABehavior.AFTER_NEW_SCAN,
                    aeLockBehavior = Lock3ABehavior.IMMEDIATE
                )
            }
        assertThat(lock3AAsyncTask.isCompleted).isFalse()
        globalScope.launch {
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
                                    CaptureResult.CONTROL_AE_STATE_CONVERGED
                            )
                    )
                )
                delay(FRAME_RATE_MS)
            }
        }

        val result = lock3AAsyncTask.await()
        assertThat(result.isCompleted).isFalse()

        globalScope.launch {
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
                            CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_LOCKED
                        )
                )
            )
        }

        val result3A = result.await()
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)

        // One request to cancel AF to start a new scan.
        val event1 = captureSequenceProcessor.nextEvent()
        assertThat(event1.requiredParameters)
            .containsEntry(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_TRIGGER_CANCEL
            )
        // There should be one request to monitor AF to finish it's scan.
        captureSequenceProcessor.nextEvent()

        // There should be one request to monitor lock AE.
        val event2 = captureSequenceProcessor.nextEvent()
        assertThat(event2.requiredParameters).containsEntry(CaptureRequest.CONTROL_AE_LOCK, true)

        // And one request to lock AF.
        val event3 = captureSequenceProcessor.nextEvent()
        assertThat(event3.requiredParameters).containsEntry(CaptureRequest.CONTROL_AE_LOCK, true)
        assertThat(event3.requiredParameters)
            .containsEntry(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_TRIGGER_START
            )
        globalScope.cancel()
    }

    @Test
    fun testAfAfterCurrentScanAeAfterCurrentScan() = runTest {
        val globalScope = CoroutineScope(UnconfinedTestDispatcher())

        val lock3AAsyncTask =
            globalScope.async {
                controller3A.lock3A(
                    afLockBehavior = Lock3ABehavior.AFTER_CURRENT_SCAN,
                    aeLockBehavior = Lock3ABehavior.AFTER_CURRENT_SCAN
                )
            }
        assertThat(lock3AAsyncTask.isCompleted).isFalse()
        globalScope.launch {
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
                                    CaptureResult.CONTROL_AE_STATE_CONVERGED
                            )
                    )
                )
                delay(FRAME_RATE_MS)
            }
        }

        val result = lock3AAsyncTask.await()
        assertThat(result.isCompleted).isFalse()

        globalScope.launch {
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
                            CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_LOCKED
                        )
                )
            )
        }

        val result3A = result.await()
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)

        // There should be one request to monitor AF to finish it's scan.
        val event = captureSequenceProcessor.nextEvent()
        assertThat(event.isRepeating).isTrue()

        // One request to lock AE (Repeating)
        val request2Event = captureSequenceProcessor.nextEvent()
        assertThat(request2Event.isRepeating).isTrue()
        assertThat(request2Event.requests.size).isEqualTo(1)
        assertThat(request2Event.requiredParameters)
            .containsEntry(CaptureRequest.CONTROL_AE_LOCK, true)

        // And one request to lock AF.
        val request3Event = captureSequenceProcessor.nextEvent()
        assertThat(request3Event.isCapture).isTrue()
        assertThat(request3Event.requests.size).isEqualTo(1)
        assertThat(request3Event.requiredParameters)
            .containsEntry(CaptureRequest.CONTROL_AE_LOCK, true)
        assertThat(request3Event.requiredParameters)
            .containsEntry(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_TRIGGER_START
            )

        globalScope.cancel()
    }

    @Test
    fun testAfAfterNewScanScanAeAfterNewScan() = runTest {
        val globalScope = CoroutineScope(UnconfinedTestDispatcher())
        val lock3AAsyncTask =
            globalScope.async {
                controller3A.lock3A(
                    afLockBehavior = Lock3ABehavior.AFTER_NEW_SCAN,
                    aeLockBehavior = Lock3ABehavior.AFTER_NEW_SCAN
                )
            }
        assertThat(lock3AAsyncTask.isCompleted).isFalse()
        globalScope.launch {
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
                                    CaptureResult.CONTROL_AE_STATE_CONVERGED
                            )
                    )
                )
                delay(FRAME_RATE_MS)
            }
        }

        val result = lock3AAsyncTask.await()
        assertThat(result.isCompleted).isFalse()

        globalScope.launch {
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
                            CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_LOCKED
                        )
                )
            )
        }

        val result3A = result.await()
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)

        // One request to cancel AF to start a new scan.
        val event1 = captureSequenceProcessor.nextEvent()
        assertThat(event1.requiredParameters)
            .containsEntry(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_TRIGGER_CANCEL
            )

        // There should be one request to unlock AE and monitor the current AF scan to finish.
        val event2 = captureSequenceProcessor.nextEvent()
        assertThat(event2.requiredParameters).containsEntry(CaptureRequest.CONTROL_AE_LOCK, false)

        // There should be one request to monitor lock AE.
        val event3 = captureSequenceProcessor.nextEvent()
        assertThat(event3.requiredParameters).containsEntry(CaptureRequest.CONTROL_AE_LOCK, true)

        // And one request to lock AF.
        val event4 = captureSequenceProcessor.nextEvent()
        assertThat(event4.requiredParameters).containsEntry(CaptureRequest.CONTROL_AE_LOCK, true)
        assertThat(event4.requiredParameters)
            .containsEntry(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_TRIGGER_START
            )
        globalScope.cancel()
    }

    @Test
    fun testLock3AWithRegions() = runTest {
        val afMeteringRegion = MeteringRectangle(1, 1, 100, 100, 2)
        val aeMeteringRegion = MeteringRectangle(10, 15, 140, 140, 3)
        val result =
            controller3A.lock3A(
                aeRegions = listOf(aeMeteringRegion),
                afRegions = listOf(afMeteringRegion),
                afLockBehavior = Lock3ABehavior.IMMEDIATE,
                aeLockBehavior = Lock3ABehavior.IMMEDIATE
            )
        assertThat(result.isCompleted).isFalse()

        // Since requirement of to lock both AE and AF immediately, the requests to lock AE and AF
        // are sent right away. The result of lock3A call will complete once AE and AF have reached
        // their desired states. In this response i.e cameraResponse1, AF is still scanning so the
        // result won't be complete.
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
                            CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_LOCKED
                        )
                )
            )
        }

        cameraResponse.await()
        assertThat(result.isCompleted).isFalse()

        // One we we are notified that the AE and AF are in locked state, the result of lock3A call
        // will complete.
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
                            CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_LOCKED
                        )
                )
            )
        }

        val result3A = result.await()
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)

        val aeRegions = graphState3A.aeRegions!!
        assertThat(aeRegions.size).isEqualTo(1)
        assertThat(aeRegions[0]).isEqualTo(aeMeteringRegion)

        val afRegions = graphState3A.afRegions!!
        assertThat(afRegions.size).isEqualTo(1)
        assertThat(afRegions[0]).isEqualTo(afMeteringRegion)

        // We not check if the correct sequence of requests were submitted by lock3A call. The
        // request should be a repeating request to lock AE.
        val event1 = captureSequenceProcessor.nextEvent()
        assertThat(event1.requiredParameters).containsEntry(CaptureRequest.CONTROL_AE_LOCK, true)

        // The second request should be a single request to lock AF.
        val event2 = captureSequenceProcessor.nextEvent()
        assertThat(event2.requiredParameters)
            .containsEntry(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_TRIGGER_START
            )
        assertThat(event1.requiredParameters).containsEntry(CaptureRequest.CONTROL_AE_LOCK, true)
    }

    @Test
    fun testLock3AWithUnsupportedAutoFocusTrigger() = runTest {
        val fakeMetadata =
            FakeCameraMetadata(
                mapOf(
                    CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES to
                        intArrayOf(CaptureRequest.CONTROL_AF_MODE_OFF)
                ),
            )
        val controller3A = Controller3A(graphProcessor, fakeMetadata, graphState3A, listener3A)
        val result = controller3A.lock3A(afLockBehavior = Lock3ABehavior.AFTER_NEW_SCAN).await()
        assertThat(result.status).isEqualTo(Result3A.Status.OK)
        assertThat(result.frameMetadata).isEqualTo(null)
    }

    @Test
    fun testCustomizedExitConditionForEmptyAeState_newScanLock3A() = runTest {
        // Arrange, set up exit conditions which allow 3A state to be empty.
        val convergeCondition: (FrameMetadata) -> Boolean = convergeCondition@{ frameMetadata ->
            val aeUnlocked =
                frameMetadata[CaptureResult.CONTROL_AE_STATE]?.let {
                    listOf(
                            CaptureResult.CONTROL_AE_STATE_CONVERGED,
                            CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED,
                            CaptureResult.CONTROL_AE_STATE_LOCKED
                        )
                        .contains(it)
                } ?: true

            val afUnlocked =
                frameMetadata[CaptureResult.CONTROL_AF_STATE]?.let {
                    listOf(
                            CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED,
                            CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED,
                            CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED
                        )
                        .contains(it)
                } ?: true

            val awbUnlocked =
                frameMetadata[CaptureResult.CONTROL_AWB_STATE]?.let {
                    listOf(
                            CaptureResult.CONTROL_AWB_STATE_CONVERGED,
                            CaptureResult.CONTROL_AWB_STATE_LOCKED
                        )
                        .contains(it)
                } ?: true

            return@convergeCondition aeUnlocked && afUnlocked && awbUnlocked
        }
        val lockCondition: (FrameMetadata) -> Boolean = lockCondition@{ frameMetadata ->
            val aeUnlocked =
                frameMetadata[CaptureResult.CONTROL_AE_STATE]?.let {
                    listOf(CaptureResult.CONTROL_AE_STATE_LOCKED).contains(it)
                } ?: true

            val afUnlocked =
                frameMetadata[CaptureResult.CONTROL_AF_STATE]?.let {
                    listOf(
                            CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED
                        )
                        .contains(it)
                } ?: true

            val awbUnlocked =
                frameMetadata[CaptureResult.CONTROL_AWB_STATE]?.let {
                    listOf(CaptureResult.CONTROL_AWB_STATE_LOCKED).contains(it)
                } ?: true

            return@lockCondition aeUnlocked && afUnlocked && awbUnlocked
        }

        // Act. lock3A with new scan
        val lock3AAsyncTask = async {
            controller3A.lock3A(
                afLockBehavior = Lock3ABehavior.AFTER_NEW_SCAN,
                aeLockBehavior = Lock3ABehavior.AFTER_NEW_SCAN,
                awbLockBehavior = Lock3ABehavior.AFTER_NEW_SCAN,
                convergedCondition = convergeCondition,
                lockedCondition = lockCondition,
            )
        }

        // Simulate repeatedly invoke without AE state.
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
                                    CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED,
                                CaptureResult.CONTROL_AWB_STATE to
                                    CaptureResult.CONTROL_AWB_STATE_CONVERGED
                            )
                    )
                )
                delay(FRAME_RATE_MS)
            }
        }
        val deferredResult = lock3AAsyncTask.await()
        repeatingJob.await()
        assertThat(deferredResult.isCompleted).isFalse()

        // Simulate locked AF, AWB invoke without AE locked info.
        listener3A.onRequestSequenceCreated(FakeRequestMetadata(requestNumber = RequestNumber(1)))
        listener3A.onPartialCaptureResult(
            FakeRequestMetadata(requestNumber = RequestNumber(1)),
            FrameNumber(120L),
            FakeFrameMetadata(
                frameNumber = FrameNumber(120L),
                resultMetadata =
                    mapOf(
                        CaptureResult.CONTROL_AF_STATE to
                            CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
                        CaptureResult.CONTROL_AWB_STATE to CaptureResult.CONTROL_AWB_STATE_LOCKED
                    )
            )
        )

        // Assert. lock3A task should be completed.
        val result3A = deferredResult.await()
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)
    }

    companion object {
        // The time duration in milliseconds between two frame results.
        private const val FRAME_RATE_MS = 33L
    }
}
