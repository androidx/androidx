/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.pipe.compat

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraExtensionSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import androidx.camera.camera2.pipe.FrameNumber

/**
 * Interface for merging functionality of [CameraCaptureSession.CaptureCallback] and
 * [CameraExtensionSession.ExtensionCaptureCallback].
 *
 * [CameraCaptureSession.CaptureCallback] and [CameraExtensionSession.ExtensionCaptureCallback] are
 * abstract classes, so a class cannot extend both of them. This interface prevents duplication of
 * code and developer facing endpoints because it is agnostic of which session type it is used for.
 */
internal interface Camera2CaptureCallback {
    fun onCaptureStarted(
        captureRequest: CaptureRequest,
        captureFrameNumber: Long,
        captureTimestamp: Long
    )

    fun onCaptureProgressed(captureRequest: CaptureRequest, partialCaptureResult: CaptureResult)

    fun onCaptureCompleted(
        captureRequest: CaptureRequest,
        captureResult: TotalCaptureResult,
        frameNumber: FrameNumber
    )

    fun onCaptureProcessProgressed(captureRequest: CaptureRequest, progress: Int)

    fun onCaptureFailed(captureRequest: CaptureRequest, frameNumber: FrameNumber)

    fun onCaptureSequenceCompleted(captureSequenceId: Int, captureFrameNumber: Long)

    fun onCaptureSequenceAborted(captureSequenceId: Int)
}
