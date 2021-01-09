/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2.pipe.impl

import android.hardware.camera2.params.MeteringRectangle
import androidx.camera.camera2.pipe.AeMode
import androidx.camera.camera2.pipe.AfMode
import androidx.camera.camera2.pipe.AwbMode
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Lock3ABehavior
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.TorchState
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Deferred

internal val cameraGraphSessionIds = atomic(0)

internal class CameraGraphSessionImpl(
    private val token: TokenLock.Token,
    private val graphProcessor: GraphProcessor,
    private val controller3A: Controller3A
) : CameraGraph.Session {
    private val debugId = cameraGraphSessionIds.incrementAndGet()

    override fun submit(request: Request) {
        graphProcessor.submit(request)
    }

    override fun submit(requests: List<Request>) {
        graphProcessor.submit(requests)
    }

    override fun startRepeating(request: Request) {
        graphProcessor.startRepeating(request)
    }

    override fun abort() {
        graphProcessor.abort()
    }

    override fun stopRepeating() {
        graphProcessor.stopRepeating()
    }

    override fun close() {
        // Release the token so that a new instance of session can be created.
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
        return controller3A.submit3A(aeMode, afMode, awbMode, aeRegions, afRegions, awbRegions)
    }

    override fun setTorch(torchState: TorchState): Deferred<Result3A> {
        // TODO(sushilnath): First check whether the camera device has a flash unit. Ref:
        // https://developer.android.com/reference/android/hardware/camera2/CameraCharacteristics#FLASH_INFO_AVAILABLE
        return controller3A.setTorch(torchState)
    }

    override suspend fun lock3A(
        aeLockBehavior: Lock3ABehavior?,
        afLockBehavior: Lock3ABehavior?,
        awbLockBehavior: Lock3ABehavior?,
        frameLimit: Int,
        timeLimitNs: Long
    ): Deferred<Result3A> {
        // TODO(sushilnath): check if the device or the current mode supports lock for each of
        // ae, af and awb respectively. If not supported return an exception or return early with
        // the right status code.
        return controller3A.lock3A(
            aeLockBehavior, afLockBehavior, awbLockBehavior, frameLimit,
            timeLimitNs
        )
    }

    override fun lock3A(
        aeMode: AeMode?,
        afMode: AfMode?,
        awbMode: AwbMode?,
        aeRegions: List<MeteringRectangle>?,
        afRegions: List<MeteringRectangle>?,
        awbRegions: List<MeteringRectangle>?,
        aeLockBehavior: Lock3ABehavior?,
        afLockBehavior: Lock3ABehavior?,
        awbLockBehavior: Lock3ABehavior?,
        frameLimit: Int,
        timeLimitMs: Int
    ): Deferred<Result3A> {
        TODO("Implement lock3A")
    }

    override fun unlock3A(ae: Boolean?, af: Boolean?, awb: Boolean?): Deferred<FrameNumber> {
        throw UnsupportedOperationException()
    }

    override suspend fun lock3AForCapture(
        frameLimit: Int,
        timeLimitNs: Long
    ): Deferred<Result3A> {
        return controller3A.lock3AForCapture(frameLimit, timeLimitNs)
    }

    override suspend fun unlock3APostCapture(): Deferred<Result3A> {
        return controller3A.unlock3APostCapture()
    }

    override fun toString(): String = "CameraGraph.Session-$debugId"
}