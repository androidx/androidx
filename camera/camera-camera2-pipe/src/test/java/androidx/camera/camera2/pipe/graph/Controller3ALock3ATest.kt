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
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.params.MeteringRectangle
import android.os.Build
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Lock3ABehavior
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestNumber
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.testing.FakeFrameMetadata
import androidx.camera.camera2.pipe.testing.FakeGraphProcessor
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import androidx.camera.camera2.pipe.testing.FakeRequestProcessor
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
internal class Controller3ALock3ATest {
    private val graphState3A = GraphState3A()
    private val graphProcessor = FakeGraphProcessor(graphState3A = graphState3A)
    private val requestProcessor = FakeRequestProcessor()
    private val listener3A = Listener3A()
    private val controller3A = Controller3A(graphProcessor, graphState3A, listener3A)

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun testAfImmediateAeImmediate(): Unit = runBlocking {
        initGraphProcessor()

        val result = controller3A.lock3A(
            afLockBehavior = Lock3ABehavior.IMMEDIATE,
            aeLockBehavior = Lock3ABehavior.IMMEDIATE
        )
        assertThat(result.isCompleted).isFalse()

        // Since requirement of to lock both AE and AF immediately, the requests to lock AE and AF
        // are sent right away. The result of lock3A call will complete once AE and AF have reached
        // their desired states. In this response i.e cameraResponse1, AF is still scanning so the
        // result won't be complete.
        val cameraResponse = GlobalScope.async {
            listener3A.onRequestSequenceCreated(
                FakeRequestMetadata(
                    requestNumber = RequestNumber(1)
                )
            )
            listener3A.onPartialCaptureResult(
                FakeRequestMetadata(requestNumber = RequestNumber(1)),
                FrameNumber(101L),
                FakeFrameMetadata(
                    frameNumber = FrameNumber(101L),
                    resultMetadata = mapOf(
                        CaptureResult.CONTROL_AF_STATE to CaptureResult
                            .CONTROL_AF_STATE_PASSIVE_SCAN,
                        CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_LOCKED
                    )
                )
            )
        }

        cameraResponse.await()
        assertThat(result.isCompleted).isFalse()

        // One we we are notified that the AE and AF are in locked state, the result of lock3A call
        // will complete.
        GlobalScope.launch {
            listener3A.onRequestSequenceCreated(
                FakeRequestMetadata(
                    requestNumber = RequestNumber(1)
                )
            )
            listener3A.onPartialCaptureResult(
                FakeRequestMetadata(requestNumber = RequestNumber(1)),
                FrameNumber(101L),
                FakeFrameMetadata(
                    frameNumber = FrameNumber(101L),
                    resultMetadata = mapOf(
                        CaptureResult.CONTROL_AF_STATE to CaptureResult
                            .CONTROL_AF_STATE_FOCUSED_LOCKED,
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
        val request1 = requestProcessor.nextEvent().requestSequence
        assertThat(request1!!.requiredParameters[CaptureRequest.CONTROL_AE_LOCK]).isEqualTo(
            true
        )

        // The second request should be a single request to lock AF.
        val request2 = requestProcessor.nextEvent().requestSequence
        assertThat(request2!!.requiredParameters[CaptureRequest.CONTROL_AF_TRIGGER]).isEqualTo(
            CaptureRequest.CONTROL_AF_TRIGGER_START
        )
        assertThat(request2.requiredParameters[CaptureRequest.CONTROL_AE_LOCK]).isEqualTo(
            true
        )
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun testAfImmediateAeAfterCurrentScan(): Unit = runBlocking {
        initGraphProcessor()

        val lock3AAsyncTask = GlobalScope.async {
            controller3A.lock3A(
                afLockBehavior = Lock3ABehavior.IMMEDIATE,
                aeLockBehavior = Lock3ABehavior.AFTER_CURRENT_SCAN
            )
        }
        assertThat(lock3AAsyncTask.isCompleted).isFalse()
        // Launch a task to repeatedly invoke a given capture result.
        GlobalScope.launch {
            while (true) {
                listener3A.onRequestSequenceCreated(
                    FakeRequestMetadata(
                        requestNumber = RequestNumber(1)
                    )
                )
                listener3A.onPartialCaptureResult(
                    FakeRequestMetadata(requestNumber = RequestNumber(1)),
                    FrameNumber(101L),
                    FakeFrameMetadata(
                        frameNumber = FrameNumber(101L),
                        resultMetadata = mapOf(
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
        requestProcessor.nextEvent().requestSequence
        // Once AE is converged, another repeatingrequest is sent to lock AE.
        val request1 = requestProcessor.nextEvent().requestSequence
        assertThat(request1!!.requiredParameters[CaptureRequest.CONTROL_AE_LOCK]).isEqualTo(
            true
        )

        GlobalScope.launch {
            listener3A.onRequestSequenceCreated(
                FakeRequestMetadata(
                    requestNumber = RequestNumber(1)
                )
            )
            listener3A.onPartialCaptureResult(
                FakeRequestMetadata(requestNumber = RequestNumber(1)),
                FrameNumber(101L),
                FakeFrameMetadata(
                    frameNumber = FrameNumber(101L),
                    resultMetadata = mapOf(
                        CaptureResult.CONTROL_AF_STATE to CaptureResult
                            .CONTROL_AF_STATE_FOCUSED_LOCKED,
                        CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_LOCKED
                    )
                )
            )
        }

        val result3A = result.await()
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)

        // A single request to lock AF must have been used as well.
        val request2 = requestProcessor.nextEvent().requestSequence
        assertThat(request2!!.requiredParameters[CaptureRequest.CONTROL_AF_TRIGGER]).isEqualTo(
            CaptureRequest.CONTROL_AF_TRIGGER_START
        )
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun testAfImmediateAeAfterNewScan(): Unit = runBlocking {
        initGraphProcessor()

        val lock3AAsyncTask = GlobalScope.async {
            controller3A.lock3A(
                afLockBehavior = Lock3ABehavior.IMMEDIATE,
                aeLockBehavior = Lock3ABehavior.AFTER_NEW_SCAN
            )
        }
        assertThat(lock3AAsyncTask.isCompleted).isFalse()
        GlobalScope.launch {
            while (true) {
                listener3A.onRequestSequenceCreated(
                    FakeRequestMetadata(
                        requestNumber = RequestNumber(1)
                    )
                )
                listener3A.onPartialCaptureResult(
                    FakeRequestMetadata(requestNumber = RequestNumber(1)),
                    FrameNumber(101L),
                    FakeFrameMetadata(
                        frameNumber = FrameNumber(101L),
                        resultMetadata = mapOf(
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
        val request1 = requestProcessor.nextEvent().requestSequence
        assertThat(request1!!.requiredParameters[CaptureRequest.CONTROL_AE_LOCK]).isEqualTo(
            false
        )

        GlobalScope.launch {
            listener3A.onRequestSequenceCreated(
                FakeRequestMetadata(
                    requestNumber = RequestNumber(1)
                )
            )
            listener3A.onPartialCaptureResult(
                FakeRequestMetadata(requestNumber = RequestNumber(1)),
                FrameNumber(101L),
                FakeFrameMetadata(
                    frameNumber = FrameNumber(101L),
                    resultMetadata = mapOf(
                        CaptureResult.CONTROL_AF_STATE to CaptureResult
                            .CONTROL_AF_STATE_FOCUSED_LOCKED,
                        CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_LOCKED
                    )
                )
            )
        }

        val result3A = result.await()
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)

        // There should be one more request to lock AE after new scan is done.
        val request2 = requestProcessor.nextEvent().requestSequence
        assertThat(request2!!.requiredParameters[CaptureRequest.CONTROL_AE_LOCK]).isEqualTo(
            true
        )

        // And one request to lock AF.
        val request3 = requestProcessor.nextEvent().requestSequence
        assertThat(request3!!.requiredParameters[CaptureRequest.CONTROL_AF_TRIGGER]).isEqualTo(
            CaptureRequest.CONTROL_AF_TRIGGER_START
        )
        assertThat(request3.requiredParameters[CaptureRequest.CONTROL_AE_LOCK]).isEqualTo(
            true
        )
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun testAfAfterCurrentScanAeImmediate(): Unit = runBlocking {
        initGraphProcessor()

        val lock3AAsyncTask = GlobalScope.async {
            controller3A.lock3A(
                afLockBehavior = Lock3ABehavior.AFTER_CURRENT_SCAN,
                aeLockBehavior = Lock3ABehavior.IMMEDIATE
            )
        }
        assertThat(lock3AAsyncTask.isCompleted).isFalse()
        GlobalScope.launch {
            while (true) {
                listener3A.onRequestSequenceCreated(
                    FakeRequestMetadata(
                        requestNumber = RequestNumber(1)
                    )
                )
                listener3A.onPartialCaptureResult(
                    FakeRequestMetadata(requestNumber = RequestNumber(1)),
                    FrameNumber(101L),
                    FakeFrameMetadata(
                        frameNumber = FrameNumber(101L),
                        resultMetadata = mapOf(
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

        GlobalScope.launch {
            listener3A.onRequestSequenceCreated(
                FakeRequestMetadata(
                    requestNumber = RequestNumber(1)
                )
            )
            listener3A.onPartialCaptureResult(
                FakeRequestMetadata(requestNumber = RequestNumber(1)),
                FrameNumber(101L),
                FakeFrameMetadata(
                    frameNumber = FrameNumber(101L),
                    resultMetadata = mapOf(
                        CaptureResult.CONTROL_AF_STATE to CaptureResult
                            .CONTROL_AF_STATE_FOCUSED_LOCKED,
                        CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_LOCKED
                    )
                )
            )
        }

        val result3A = result.await()
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)

        // There should be one request to monitor AF to finish it's scan.
        requestProcessor.nextEvent()
        // One request to lock AE
        val request2 = requestProcessor.nextEvent().requestSequence
        assertThat(request2!!.requiredParameters[CaptureRequest.CONTROL_AE_LOCK]).isEqualTo(
            true
        )

        // And one request to lock AF.
        val request3 = requestProcessor.nextEvent().requestSequence
        assertThat(request3!!.requiredParameters[CaptureRequest.CONTROL_AF_TRIGGER]).isEqualTo(
            CaptureRequest.CONTROL_AF_TRIGGER_START
        )
        assertThat(request3.requiredParameters[CaptureRequest.CONTROL_AE_LOCK]).isEqualTo(
            true
        )
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun testAfAfterNewScanScanAeImmediate(): Unit = runBlocking {
        initGraphProcessor()

        val lock3AAsyncTask = GlobalScope.async {
            controller3A.lock3A(
                afLockBehavior = Lock3ABehavior.AFTER_NEW_SCAN,
                aeLockBehavior = Lock3ABehavior.IMMEDIATE
            )
        }
        assertThat(lock3AAsyncTask.isCompleted).isFalse()
        GlobalScope.launch {
            while (true) {
                listener3A.onRequestSequenceCreated(
                    FakeRequestMetadata(
                        requestNumber = RequestNumber(1)
                    )
                )
                listener3A.onPartialCaptureResult(
                    FakeRequestMetadata(requestNumber = RequestNumber(1)),
                    FrameNumber(101L),
                    FakeFrameMetadata(
                        frameNumber = FrameNumber(101L),
                        resultMetadata = mapOf(
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

        GlobalScope.launch {
            listener3A.onRequestSequenceCreated(
                FakeRequestMetadata(
                    requestNumber = RequestNumber(1)
                )
            )
            listener3A.onPartialCaptureResult(
                FakeRequestMetadata(requestNumber = RequestNumber(1)),
                FrameNumber(101L),
                FakeFrameMetadata(
                    frameNumber = FrameNumber(101L),
                    resultMetadata = mapOf(
                        CaptureResult.CONTROL_AF_STATE to CaptureResult
                            .CONTROL_AF_STATE_FOCUSED_LOCKED,
                        CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_LOCKED
                    )
                )
            )
        }

        val result3A = result.await()
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)

        // One request to cancel AF to start a new scan.
        val request1 = requestProcessor.nextEvent().requestSequence
        assertThat(request1!!.requiredParameters[CaptureRequest.CONTROL_AF_TRIGGER]).isEqualTo(
            CaptureRequest.CONTROL_AF_TRIGGER_CANCEL
        )
        // There should be one request to monitor AF to finish it's scan.
        requestProcessor.nextEvent()

        // There should be one request to monitor lock AE.
        val request2 = requestProcessor.nextEvent().requestSequence
        assertThat(request2!!.requiredParameters[CaptureRequest.CONTROL_AE_LOCK]).isEqualTo(
            true
        )

        // And one request to lock AF.
        val request3 = requestProcessor.nextEvent().requestSequence
        assertThat(request3!!.requiredParameters[CaptureRequest.CONTROL_AF_TRIGGER]).isEqualTo(
            CaptureRequest.CONTROL_AF_TRIGGER_START
        )
        assertThat(request3.requiredParameters[CaptureRequest.CONTROL_AE_LOCK]).isEqualTo(
            true
        )
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun testAfAfterCurrentScanAeAfterCurrentScan(): Unit = runBlocking {
        initGraphProcessor()

        val lock3AAsyncTask = GlobalScope.async {
            controller3A.lock3A(
                afLockBehavior = Lock3ABehavior.AFTER_CURRENT_SCAN,
                aeLockBehavior = Lock3ABehavior.AFTER_CURRENT_SCAN
            )
        }
        assertThat(lock3AAsyncTask.isCompleted).isFalse()
        GlobalScope.launch {
            while (true) {
                listener3A.onRequestSequenceCreated(
                    FakeRequestMetadata(
                        requestNumber = RequestNumber(1)
                    )
                )
                listener3A.onPartialCaptureResult(
                    FakeRequestMetadata(requestNumber = RequestNumber(1)),
                    FrameNumber(101L),
                    FakeFrameMetadata(
                        frameNumber = FrameNumber(101L),
                        resultMetadata = mapOf(
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

        GlobalScope.launch {
            listener3A.onRequestSequenceCreated(
                FakeRequestMetadata(
                    requestNumber = RequestNumber(1)
                )
            )
            listener3A.onPartialCaptureResult(
                FakeRequestMetadata(requestNumber = RequestNumber(1)),
                FrameNumber(101L),
                FakeFrameMetadata(
                    frameNumber = FrameNumber(101L),
                    resultMetadata = mapOf(
                        CaptureResult.CONTROL_AF_STATE to CaptureResult
                            .CONTROL_AF_STATE_FOCUSED_LOCKED,
                        CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_LOCKED
                    )
                )
            )
        }

        val result3A = result.await()
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)

        // There should be one request to monitor AF to finish it's scan.
        val event = requestProcessor.nextEvent()
        assertThat(event.startRepeating).isTrue()
        assertThat(event.rejected).isFalse()
        assertThat(event.abort).isFalse()
        assertThat(event.close).isFalse()
        assertThat(event.submit).isFalse()

        // One request to lock AE
        val request2Event = requestProcessor.nextEvent()
        assertThat(request2Event.startRepeating).isTrue()
        assertThat(request2Event.submit).isFalse()
        val request2 = request2Event.requestSequence!!
        assertThat(request2).isNotNull()
        assertThat(request2.requiredParameters).isNotEmpty()
        assertThat(request2.requiredParameters[CaptureRequest.CONTROL_AE_LOCK]).isEqualTo(
            true
        )

        // And one request to lock AF.
        val request3Event = requestProcessor.nextEvent()
        assertThat(request3Event.startRepeating).isFalse()
        assertThat(request3Event.submit).isTrue()
        val request3 = request3Event.requestSequence!!
        assertThat(request3.requiredParameters[CaptureRequest.CONTROL_AF_TRIGGER]).isEqualTo(
            CaptureRequest.CONTROL_AF_TRIGGER_START
        )
        assertThat(request3.requiredParameters[CaptureRequest.CONTROL_AE_LOCK]).isEqualTo(
            true
        )
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun testAfAfterNewScanScanAeAfterNewScan(): Unit = runBlocking {
        initGraphProcessor()

        val lock3AAsyncTask = GlobalScope.async {
            controller3A.lock3A(
                afLockBehavior = Lock3ABehavior.AFTER_NEW_SCAN,
                aeLockBehavior = Lock3ABehavior.AFTER_NEW_SCAN
            )
        }
        assertThat(lock3AAsyncTask.isCompleted).isFalse()
        GlobalScope.launch {
            while (true) {
                listener3A.onRequestSequenceCreated(
                    FakeRequestMetadata(
                        requestNumber = RequestNumber(1)
                    )
                )
                listener3A.onPartialCaptureResult(
                    FakeRequestMetadata(requestNumber = RequestNumber(1)),
                    FrameNumber(101L),
                    FakeFrameMetadata(
                        frameNumber = FrameNumber(101L),
                        resultMetadata = mapOf(
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

        GlobalScope.launch {
            listener3A.onRequestSequenceCreated(
                FakeRequestMetadata(
                    requestNumber = RequestNumber(1)
                )
            )
            listener3A.onPartialCaptureResult(
                FakeRequestMetadata(requestNumber = RequestNumber(1)),
                FrameNumber(101L),
                FakeFrameMetadata(
                    frameNumber = FrameNumber(101L),
                    resultMetadata = mapOf(
                        CaptureResult.CONTROL_AF_STATE to CaptureResult
                            .CONTROL_AF_STATE_FOCUSED_LOCKED,
                        CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_LOCKED
                    )
                )
            )
        }

        val result3A = result.await()
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)

        // One request to cancel AF to start a new scan.
        val request1 = requestProcessor.nextEvent().requestSequence
        assertThat(request1!!.requiredParameters[CaptureRequest.CONTROL_AF_TRIGGER]).isEqualTo(
            CaptureRequest.CONTROL_AF_TRIGGER_CANCEL
        )
        // There should be one request to unlock AE and monitor the current AF scan to finish.
        val request2 = requestProcessor.nextEvent().requestSequence
        assertThat(request2!!.requiredParameters[CaptureRequest.CONTROL_AE_LOCK]).isEqualTo(
            false
        )

        // There should be one request to monitor lock AE.
        val request3 = requestProcessor.nextEvent().requestSequence
        assertThat(request3!!.requiredParameters[CaptureRequest.CONTROL_AE_LOCK]).isEqualTo(
            true
        )

        // And one request to lock AF.
        val request4 = requestProcessor.nextEvent().requestSequence
        assertThat(request4!!.requiredParameters[CaptureRequest.CONTROL_AF_TRIGGER]).isEqualTo(
            CaptureRequest.CONTROL_AF_TRIGGER_START
        )
        assertThat(request4.requiredParameters[CaptureRequest.CONTROL_AE_LOCK]).isEqualTo(
            true
        )
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun testLock3AWithRegions(): Unit = runBlocking {
        initGraphProcessor()

        val afMeteringRegion = MeteringRectangle(1, 1, 100, 100, 2)
        val aeMeteringRegion = MeteringRectangle(10, 15, 140, 140, 3)
        val result = controller3A.lock3A(
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
        val cameraResponse = GlobalScope.async {
            listener3A.onRequestSequenceCreated(
                FakeRequestMetadata(
                    requestNumber = RequestNumber(1)
                )
            )
            listener3A.onPartialCaptureResult(
                FakeRequestMetadata(requestNumber = RequestNumber(1)),
                FrameNumber(101L),
                FakeFrameMetadata(
                    frameNumber = FrameNumber(101L),
                    resultMetadata = mapOf(
                        CaptureResult.CONTROL_AF_STATE to CaptureResult
                            .CONTROL_AF_STATE_PASSIVE_SCAN,
                        CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_LOCKED
                    )
                )
            )
        }

        cameraResponse.await()
        assertThat(result.isCompleted).isFalse()

        // One we we are notified that the AE and AF are in locked state, the result of lock3A call
        // will complete.
        GlobalScope.launch {
            listener3A.onRequestSequenceCreated(
                FakeRequestMetadata(
                    requestNumber = RequestNumber(1)
                )
            )
            listener3A.onPartialCaptureResult(
                FakeRequestMetadata(requestNumber = RequestNumber(1)),
                FrameNumber(101L),
                FakeFrameMetadata(
                    frameNumber = FrameNumber(101L),
                    resultMetadata = mapOf(
                        CaptureResult.CONTROL_AF_STATE to CaptureResult
                            .CONTROL_AF_STATE_FOCUSED_LOCKED,
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
        val request1 = requestProcessor.nextEvent().requestSequence
        assertThat(request1!!.requiredParameters[CaptureRequest.CONTROL_AE_LOCK]).isEqualTo(
            true
        )

        // The second request should be a single request to lock AF.
        val request2 = requestProcessor.nextEvent().requestSequence
        assertThat(request2!!.requiredParameters[CaptureRequest.CONTROL_AF_TRIGGER]).isEqualTo(
            CaptureRequest.CONTROL_AF_TRIGGER_START
        )
        assertThat(request2.requiredParameters[CaptureRequest.CONTROL_AE_LOCK]).isEqualTo(
            true
        )
    }

    private fun initGraphProcessor() {
        graphProcessor.onGraphStarted(requestProcessor)
        graphProcessor.startRepeating(Request(streams = listOf(StreamId(1))))
    }

    companion object {
        // The time duration in milliseconds between two frame results.
        private const val FRAME_RATE_MS = 33L
    }
}