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

package androidx.camera.camera2.pipe.testing

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.os.Handler
import android.view.Surface
import androidx.camera.camera2.pipe.compat.CameraCaptureSessionWrapper
import androidx.camera.camera2.pipe.compat.CameraDeviceWrapper
import androidx.camera.camera2.pipe.compat.OutputConfigurationWrapper

internal class FakeCaptureSessionWrapper(
    override val device: CameraDeviceWrapper,
    override val isReprocessable: Boolean = false,
    override val inputSurface: Surface? = null
) : CameraCaptureSessionWrapper {
    var closed = false
    var lastSequenceNumber = 0

    var lastCapture: List<CaptureRequest>? = null
    var lastCaptureCallback: CameraCaptureSession.CaptureCallback? = null
    var lastRepeating: List<CaptureRequest>? = null
    var lastRepeatingCallback: CameraCaptureSession.CaptureCallback? = null

    var stopRepeatingInvoked = false
    var abortCapturesInvoked = false

    override fun abortCaptures() {
        abortCapturesInvoked = true
    }

    override fun capture(
        request: CaptureRequest,
        listener: CameraCaptureSession.CaptureCallback,
        handler: Handler?
    ): Int {
        lastCapture = listOf(request)
        lastCaptureCallback = listener

        lastSequenceNumber++
        return lastSequenceNumber
    }

    override fun captureBurst(
        requests: List<CaptureRequest>,
        listener: CameraCaptureSession.CaptureCallback,
        handler: Handler?
    ): Int {
        lastCapture = requests.toList()
        lastCaptureCallback = listener

        lastSequenceNumber++
        return lastSequenceNumber
    }

    override fun setRepeatingBurst(
        requests: List<CaptureRequest>,
        listener: CameraCaptureSession.CaptureCallback,
        handler: Handler?
    ): Int {
        lastRepeating = requests.toList()
        lastRepeatingCallback = listener

        lastSequenceNumber++
        return lastSequenceNumber
    }

    override fun setRepeatingRequest(
        request: CaptureRequest,
        listener: CameraCaptureSession.CaptureCallback,
        handler: Handler?
    ): Int {
        lastRepeating = listOf(request)
        lastRepeatingCallback = listener

        lastSequenceNumber++
        return lastSequenceNumber
    }

    override fun stopRepeating() {
        stopRepeatingInvoked = true
    }

    override fun finalizeOutputConfigurations(outputConfigs: List<OutputConfigurationWrapper>) {
        throw UnsupportedOperationException(
            "finalizeOutputConfigurations is not supported"
        )
    }

    override fun unwrap(): CameraCaptureSession? {
        throw UnsupportedOperationException(
            "FakeCaptureSessionWrapper does not wrap CameraCaptureSession"
        )
    }

    override fun close() {
        closed = true
    }
}