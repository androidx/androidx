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

package androidx.camera.camera2.pipe.integration.testing

import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.params.MeteringRectangle
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.AeMode
import androidx.camera.camera2.pipe.AfMode
import androidx.camera.camera2.pipe.AwbMode
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.FrameMetadata
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Lock3ABehavior
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.TorchState
import androidx.camera.camera2.pipe.integration.impl.FakeCaptureFailure
import androidx.camera.camera2.pipe.integration.testing.FakeCameraGraphSession.RequestStatus.ABORTED
import androidx.camera.camera2.pipe.integration.testing.FakeCameraGraphSession.RequestStatus.FAILED
import androidx.camera.camera2.pipe.integration.testing.FakeCameraGraphSession.RequestStatus.TOTAL_CAPTURE_DONE
import androidx.camera.camera2.pipe.testing.FakeFrameInfo
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import java.util.concurrent.Semaphore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking

@RequiresApi(21)
open class FakeCameraGraphSession : CameraGraph.Session {

    val repeatingRequests = mutableListOf<Request>()
    var repeatingRequestSemaphore = Semaphore(0)
    var stopRepeatingSemaphore = Semaphore(0)

    enum class RequestStatus {
        TOTAL_CAPTURE_DONE,
        FAILED,
        ABORTED
    }

    var startRepeatingSignal = CompletableDeferred(TOTAL_CAPTURE_DONE) // already completed

    val submittedRequests = mutableListOf<Request>()

    override fun abort() {
        // No-op
    }

    override fun close() {
        // No-op
    }

    override suspend fun lock3A(
        aeMode: AeMode?,
        afMode: AfMode?,
        awbMode: AwbMode?,
        aeRegions: List<MeteringRectangle>?,
        afRegions: List<MeteringRectangle>?,
        awbRegions: List<MeteringRectangle>?,
        aeLockBehavior: Lock3ABehavior?,
        afLockBehavior: Lock3ABehavior?,
        awbLockBehavior: Lock3ABehavior?,
        afTriggerStartAeMode: AeMode?,
        convergedCondition: ((FrameMetadata) -> Boolean)?,
        lockedCondition: ((FrameMetadata) -> Boolean)?,
        frameLimit: Int,
        timeLimitNs: Long
    ): Deferred<Result3A> {
        throw NotImplementedError("Not used in testing")
    }

    override suspend fun lock3AForCapture(
        lockedCondition: ((FrameMetadata) -> Boolean)?,
        frameLimit: Int,
        timeLimitNs: Long
    ): Deferred<Result3A> {
        throw NotImplementedError("Not used in testing")
    }

    override fun setTorch(torchState: TorchState): Deferred<Result3A> {
        throw NotImplementedError("Not used in testing")
    }

    override fun startRepeating(request: Request) {
        repeatingRequests.add(request)
        repeatingRequestSemaphore.release()

        startRepeatingSignal.invokeOnCompletion {
            // completes immediately if startRepeatingListenerInvoker is the initial one
            runBlocking {
                // the real GraphSession processes only the last successful repeating request
                repeatingRequests.notifyLastRequestListeners(request, startRepeatingSignal.await())
            }
        }
    }

    override fun stopRepeating() {
        stopRepeatingSemaphore.release()
    }

    override fun submit(request: Request) {
        submittedRequests.add(request)
    }

    override fun submit(requests: List<Request>) {
        submittedRequests.addAll(requests)
    }

    override suspend fun submit3A(
        aeMode: AeMode?,
        afMode: AfMode?,
        awbMode: AwbMode?,
        aeRegions: List<MeteringRectangle>?,
        afRegions: List<MeteringRectangle>?,
        awbRegions: List<MeteringRectangle>?
    ): Deferred<Result3A> {
        throw NotImplementedError("Not used in testing")
    }

    override suspend fun unlock3A(
        ae: Boolean?,
        af: Boolean?,
        awb: Boolean?,
        unlockedCondition: ((FrameMetadata) -> Boolean)?,
        frameLimit: Int,
        timeLimitNs: Long
    ): Deferred<Result3A> {
        throw NotImplementedError("Not used in testing")
    }

    override suspend fun unlock3APostCapture(): Deferred<Result3A> {
        throw NotImplementedError("Not used in testing")
    }

    override fun update3A(
        aeMode: AeMode?,
        afMode: AfMode?,
        awbMode: AwbMode?,
        aeRegions: List<MeteringRectangle>?,
        afRegions: List<MeteringRectangle>?,
        awbRegions: List<MeteringRectangle>?
    ): Deferred<Result3A> {
        return CompletableDeferred(Result3A(Result3A.Status.OK))
    }

    // CaptureFailure is package-private so this workaround is used
    private fun getFakeCaptureFailure(): CaptureFailure {
        val c = Class.forName("android.hardware.camera2.CaptureFailure")
        val constructor = c.getDeclaredConstructor()
        constructor.isAccessible = true // Make the constructor accessible.
        return (constructor.newInstance() as CaptureFailure)
    }

    private fun MutableList<Request>.notifyLastRequestListeners(
        request: Request,
        status: RequestStatus
    ) {
        val requestMetadata = FakeRequestMetadata(request = request)
        last().listeners.forEach { listener ->
            when (status) {
                TOTAL_CAPTURE_DONE -> listener.onTotalCaptureResult(
                    requestMetadata, FrameNumber(0), FakeFrameInfo()
                )

                FAILED -> listener.onFailed(
                    requestMetadata,
                    FrameNumber(0),
                    FakeCaptureFailure(
                        requestMetadata,
                        false,
                        FrameNumber(0),
                        CaptureFailure.REASON_ERROR,
                        null
                    )
                )

                ABORTED -> listener.onRequestSequenceAborted(requestMetadata)
            }
        }
    }
}
