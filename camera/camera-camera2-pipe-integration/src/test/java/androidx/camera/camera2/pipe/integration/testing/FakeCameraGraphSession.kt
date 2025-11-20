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

import android.hardware.camera2.params.MeteringRectangle
import androidx.camera.camera2.pipe.AeMode
import androidx.camera.camera2.pipe.AfMode
import androidx.camera.camera2.pipe.AwbMode
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.Frame
import androidx.camera.camera2.pipe.FrameCapture
import androidx.camera.camera2.pipe.FrameMetadata
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Lock3ABehavior
import androidx.camera.camera2.pipe.OutputStatus
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.integration.testing.FakeCameraGraphSession.RequestStatus.ABORTED
import androidx.camera.camera2.pipe.integration.testing.FakeCameraGraphSession.RequestStatus.FAILED
import androidx.camera.camera2.pipe.integration.testing.FakeCameraGraphSession.RequestStatus.TOTAL_CAPTURE_DONE
import androidx.camera.camera2.pipe.testing.FakeFrameInfo
import androidx.camera.camera2.pipe.testing.FakeRequestFailure
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import java.util.concurrent.Semaphore
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking

open class FakeCameraGraphSession : CameraGraph.Session {

    val repeatingRequests = mutableListOf<Request>()
    var repeatingRequestSemaphore = Semaphore(0)
    var stopRepeatingSemaphore = Semaphore(0)

    enum class RequestStatus {
        TOTAL_CAPTURE_DONE,
        FAILED,
        ABORTED,
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
        convergedTimeLimitNs: Long,
        lockedTimeLimitNs: Long,
    ): Deferred<Result3A> {
        throw NotImplementedError("Not used in testing")
    }

    override suspend fun lock3AForCapture(
        lockedCondition: ((FrameMetadata) -> Boolean)?,
        frameLimit: Int,
        timeLimitNs: Long,
    ): Deferred<Result3A> {
        throw NotImplementedError("Not used in testing")
    }

    override suspend fun lock3AForCapture(
        triggerAf: Boolean,
        waitForAwb: Boolean,
        frameLimit: Int,
        timeLimitNs: Long,
    ): Deferred<Result3A> {
        throw NotImplementedError("Not used in testing")
    }

    override fun setTorchOn(): Deferred<Result3A> {
        throw NotImplementedError("Not used in testing")
    }

    override fun setTorchOff(aeMode: AeMode?): Deferred<Result3A> {
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

    override fun capture(request: Request): FrameCapture {
        val capture = FakeFrameCapture(request)
        submit(request)
        return capture
    }

    override fun capture(requests: List<Request>): List<FrameCapture> {
        val captures = requests.map { FakeFrameCapture(it) }
        submit(requests)
        return captures
    }

    override fun submit3A(
        aeMode: AeMode?,
        afMode: AfMode?,
        awbMode: AwbMode?,
        aeRegions: List<MeteringRectangle>?,
        afRegions: List<MeteringRectangle>?,
        awbRegions: List<MeteringRectangle>?,
    ): Deferred<Result3A> {
        throw NotImplementedError("Not used in testing")
    }

    override suspend fun unlock3A(
        ae: Boolean?,
        af: Boolean?,
        awb: Boolean?,
        unlockedCondition: ((FrameMetadata) -> Boolean)?,
        frameLimit: Int,
        timeLimitNs: Long,
    ): Deferred<Result3A> {
        throw NotImplementedError("Not used in testing")
    }

    override suspend fun unlock3APostCapture(cancelAf: Boolean): Deferred<Result3A> {
        throw NotImplementedError("Not used in testing")
    }

    override fun update3A(
        aeMode: AeMode?,
        afMode: AfMode?,
        awbMode: AwbMode?,
        aeRegions: List<MeteringRectangle>?,
        afRegions: List<MeteringRectangle>?,
        awbRegions: List<MeteringRectangle>?,
    ): Deferred<Result3A> {
        return CompletableDeferred(Result3A(Result3A.Status.OK))
    }

    private fun MutableList<Request>.notifyLastRequestListeners(
        request: Request,
        status: RequestStatus,
    ) {
        val requestMetadata = FakeRequestMetadata(request = request)
        last().listeners.forEach { listener ->
            when (status) {
                TOTAL_CAPTURE_DONE ->
                    listener.onTotalCaptureResult(requestMetadata, FrameNumber(0), FakeFrameInfo())
                FAILED ->
                    listener.onFailed(
                        requestMetadata,
                        FrameNumber(0),
                        FakeRequestFailure(requestMetadata, FrameNumber(0)),
                    )
                ABORTED -> listener.onRequestSequenceAborted(requestMetadata)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private class FakeFrameCapture(override val request: Request) : FrameCapture {
        private val result = CompletableDeferred<Frame?>()
        private val closed = atomic(false)
        private val listeners = mutableListOf<Frame.Listener>()
        override val status: OutputStatus
            get() {
                if (closed.value || result.isCancelled) return OutputStatus.UNAVAILABLE
                if (!result.isCompleted) return OutputStatus.PENDING
                return OutputStatus.AVAILABLE
            }

        override suspend fun awaitFrame(): Frame? = result.await()

        override fun getFrame(): Frame? {
            if (result.isCompleted && !result.isCancelled) {
                return result.getCompleted()
            }
            return null
        }

        override fun addListener(listener: Frame.Listener) {
            listeners.add(listener)
        }

        override fun close() {
            if (closed.compareAndSet(expect = false, update = true)) {
                result.cancel()
            }
        }
    }
}
