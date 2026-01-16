/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.annotation.GuardedBy
import androidx.annotation.RestrictTo
import androidx.camera.camera2.pipe.CameraTimestamp
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestMetadata

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class LatestFrameNumberListener(private val onNextFrameNumber: (FrameNumber) -> Unit) :
    Request.Listener {
    @GuardedBy("this") private var latestFrameNumber = Long.MIN_VALUE

    override fun onStarted(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        timestamp: CameraTimestamp,
    ) {
        // Skip reprocessing requests which can often be out of order.
        if (requestMetadata.request.inputRequest != null) {
            return
        }

        synchronized(this) {
            if (frameNumber.value > latestFrameNumber) {
                latestFrameNumber = frameNumber.value
                onNextFrameNumber(frameNumber)
            }
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class LatestFrameInfoListener(private val onNextFrameInfo: (FrameInfo) -> Unit) :
    Request.Listener {
    @GuardedBy("this") private var latestFrameNumber = Long.MIN_VALUE

    override fun onTotalCaptureResult(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        totalCaptureResult: FrameInfo,
    ) {
        // Skip reprocessing requests which can often be out of order.
        if (requestMetadata.request.inputRequest != null) {
            return
        }

        synchronized(this) {
            if (totalCaptureResult.frameNumber.value > latestFrameNumber) {
                latestFrameNumber = totalCaptureResult.frameNumber.value
                onNextFrameInfo(totalCaptureResult)
            }
        }
    }
}
