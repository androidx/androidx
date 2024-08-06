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

import android.hardware.camera2.params.MeteringRectangle
import androidx.camera.camera2.pipe.AeMode
import androidx.camera.camera2.pipe.AfMode
import androidx.camera.camera2.pipe.AwbMode
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.FrameCapture
import androidx.camera.camera2.pipe.FrameMetadata
import androidx.camera.camera2.pipe.Lock3ABehavior
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.TorchState
import androidx.camera.camera2.pipe.core.Token
import androidx.camera.camera2.pipe.internal.FrameCaptureQueue
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Deferred

internal val cameraGraphSessionIds = atomic(0)

internal class CameraGraphSessionImpl(
    private val token: Token,
    private val graphProcessor: GraphProcessor,
    private val controller3A: Controller3A,
    private val frameCaptureQueue: FrameCaptureQueue,
) : CameraGraph.Session {
    private val debugId = cameraGraphSessionIds.incrementAndGet()

    override fun submit(request: Request) {
        check(!token.released) { "Cannot call submit on $this after close." }
        graphProcessor.submit(request)
    }

    override fun submit(requests: List<Request>) {
        check(!token.released) { "Cannot call submit on $this after close." }
        check(requests.isNotEmpty()) { "Cannot call submit with an empty list of Requests!" }
        graphProcessor.submit(requests)
    }

    override fun capture(request: Request): FrameCapture {
        val frameCapture = frameCaptureQueue.enqueue(request)
        submit(request)
        return frameCapture
    }

    override fun capture(requests: List<Request>): List<FrameCapture> {
        val frameCaptures = frameCaptureQueue.enqueue(requests)
        submit(requests)
        return frameCaptures
    }

    override fun startRepeating(request: Request) {
        check(!token.released) { "Cannot call startRepeating on $this after close." }
        graphProcessor.startRepeating(request)
    }

    override fun abort() {
        check(!token.released) { "Cannot call abort on $this after close." }
        graphProcessor.abort()
    }

    override fun stopRepeating() {
        check(!token.released) { "Cannot call stopRepeating on $this after close." }
        graphProcessor.stopRepeating()
        controller3A.onStopRepeating()
    }

    override fun close() {
        token.release()
    }

    override fun update3A(
        aeMode: AeMode?,
        afMode: AfMode?,
        awbMode: AwbMode?,
        aeRegions: List<MeteringRectangle>?,
        afRegions: List<MeteringRectangle>?,
        awbRegions: List<MeteringRectangle>?
    ): Deferred<Result3A> {
        check(!token.released) { "Cannot call update3A on $this after close." }
        return controller3A.update3A(
            aeMode = aeMode,
            afMode = afMode,
            awbMode = awbMode,
            aeRegions = aeRegions,
            afRegions = afRegions,
            awbRegions = awbRegions
        )
    }

    override suspend fun submit3A(
        aeMode: AeMode?,
        afMode: AfMode?,
        awbMode: AwbMode?,
        aeRegions: List<MeteringRectangle>?,
        afRegions: List<MeteringRectangle>?,
        awbRegions: List<MeteringRectangle>?
    ): Deferred<Result3A> {
        check(!token.released) { "Cannot call submit3A on $this after close." }
        return controller3A.submit3A(aeMode, afMode, awbMode, aeRegions, afRegions, awbRegions)
    }

    override fun setTorch(torchState: TorchState): Deferred<Result3A> {
        check(!token.released) { "Cannot call setTorch on $this after close." }
        // TODO(sushilnath): First check whether the camera device has a flash unit. Ref:
        // https://developer.android.com/reference/android/hardware/camera2/CameraCharacteristics#FLASH_INFO_AVAILABLE
        return controller3A.setTorch(torchState)
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
        lockedTimeLimitNs: Long
    ): Deferred<Result3A> {
        check(!token.released) { "Cannot call lock3A on $this after close." }
        // TODO(sushilnath): check if the device or the current mode supports lock for each of
        // ae, af and awb respectively. If not supported return an exception or return early with
        // the right status code.
        return controller3A.lock3A(
            aeRegions,
            afRegions,
            awbRegions,
            aeLockBehavior,
            afLockBehavior,
            awbLockBehavior,
            afTriggerStartAeMode,
            convergedCondition,
            lockedCondition,
            frameLimit,
            convergedTimeLimitNs,
            lockedTimeLimitNs
        )
    }

    override suspend fun unlock3A(
        ae: Boolean?,
        af: Boolean?,
        awb: Boolean?,
        unlockedCondition: ((FrameMetadata) -> Boolean)?,
        frameLimit: Int,
        timeLimitNs: Long
    ): Deferred<Result3A> {
        check(!token.released) { "Cannot call unlock3A on $this after close." }
        return controller3A.unlock3A(ae, af, awb, unlockedCondition, frameLimit, timeLimitNs)
    }

    override suspend fun lock3AForCapture(
        lockedCondition: ((FrameMetadata) -> Boolean)?,
        frameLimit: Int,
        timeLimitNs: Long
    ): Deferred<Result3A> {
        check(!token.released) { "Cannot call lock3AForCapture on $this after close." }
        return controller3A.lock3AForCapture(lockedCondition, frameLimit, timeLimitNs)
    }

    override suspend fun lock3AForCapture(
        triggerAf: Boolean,
        waitForAwb: Boolean,
        frameLimit: Int,
        timeLimitNs: Long
    ): Deferred<Result3A> {
        check(!token.released) { "Cannot call lock3AForCapture on $this after close." }
        return controller3A.lock3AForCapture(triggerAf, waitForAwb, frameLimit, timeLimitNs)
    }

    override suspend fun unlock3APostCapture(cancelAf: Boolean): Deferred<Result3A> {
        check(!token.released) { "Cannot call unlock3APostCapture on $this after close." }
        return controller3A.unlock3APostCapture(cancelAf)
    }

    override fun toString(): String = "CameraGraph.Session-$debugId"
}
