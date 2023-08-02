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
import androidx.camera.camera2.pipe.Lock3ABehavior
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.TorchState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import java.util.concurrent.Semaphore

open class FakeCameraGraphSession : CameraGraph.Session {

    val repeatingRequests = mutableListOf<Request>()
    val repeatingRequestSemaphore = Semaphore(0)
    val stopRepeatingSemaphore = Semaphore(0)

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
        frameLimit: Int,
        timeLimitNs: Long
    ): Deferred<Result3A> {
        throw NotImplementedError("Not used in testing")
    }

    override suspend fun lock3AForCapture(frameLimit: Int, timeLimitNs: Long): Deferred<Result3A> {
        throw NotImplementedError("Not used in testing")
    }

    override fun setTorch(torchState: TorchState): Deferred<Result3A> {
        throw NotImplementedError("Not used in testing")
    }

    override fun startRepeating(request: Request) {
        repeatingRequests.add(request)
        repeatingRequestSemaphore.release()
    }

    override fun stopRepeating() {
        stopRepeatingSemaphore.release()
    }

    override fun submit(request: Request) {
        throw NotImplementedError("Not used in testing")
    }

    override fun submit(requests: List<Request>) {
        // No-op
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

    override suspend fun unlock3A(ae: Boolean?, af: Boolean?, awb: Boolean?): Deferred<Result3A> {
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
}
