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

package androidx.camera.camera2.pipe.integration.impl

import android.hardware.camera2.CaptureFailure
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.integration.adapter.CaptureResultAdapter
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.core.impl.CameraCaptureCallback
import androidx.camera.core.impl.CameraCaptureFailure
import java.util.concurrent.Executor
import javax.inject.Inject

/**
 * A map of [CameraCaptureCallback] that are invoked on each [Request].
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@CameraScope
class CameraCallbackMap @Inject constructor() : Request.Listener {
    private val callbackMap = mutableMapOf<CameraCaptureCallback, Executor>()
    @Volatile
    private var callbacks: Map<CameraCaptureCallback, Executor> = mapOf()

    fun addCaptureCallback(callback: CameraCaptureCallback, executor: Executor) {
        check(!callbacks.contains(callback)) { "$callback was already registered!" }

        synchronized(callbackMap) {
            callbackMap[callback] = executor
            callbacks = callbackMap.toMap()
        }
    }

    fun removeCaptureCallback(callback: CameraCaptureCallback) {
        synchronized(callbackMap) {
            callbackMap.remove(callback)
            callbacks = callbackMap.toMap()
        }
    }

    override fun onComplete(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        result: FrameInfo
    ) {
        val captureResult = CaptureResultAdapter(requestMetadata, frameNumber, result)
        for ((callback, executor) in callbacks) {
            executor.execute { callback.onCaptureCompleted(captureResult) }
        }
    }

    override fun onFailed(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        captureFailure: CaptureFailure
    ) {
        val failure = CameraCaptureFailure(CameraCaptureFailure.Reason.ERROR)
        for ((callback, executor) in callbacks) {
            executor.execute { callback.onCaptureFailed(failure) }
        }
    }

    override fun onAborted(request: Request) {
        for ((callback, executor) in callbacks) {
            executor.execute { callback.onCaptureCancelled() }
        }
    }
}