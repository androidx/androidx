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

package androidx.camera.camera2.pipe.integration.impl

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.OutputConfiguration
import android.os.Handler
import android.view.Surface

/**
 * A CameraCaptureSession implementation that will throw IllegalArgumentException for all the
 * function invocations.
 */
public class RejectOperationCameraCaptureSession : CameraCaptureSession() {

    override fun getDevice(): CameraDevice {
        throw IllegalArgumentException(createExceptionMessage("getDevice"))
    }

    override fun prepare(surface: Surface) {
        throw IllegalArgumentException(createExceptionMessage("prepare"))
    }

    override fun finalizeOutputConfigurations(outputConfigs: List<OutputConfiguration?>?) {
        throw IllegalArgumentException(createExceptionMessage("finalizeOutputConfigurations"))
    }

    override fun capture(
        request: CaptureRequest,
        listener: CaptureCallback?,
        handler: Handler?,
    ): Int {
        throw IllegalArgumentException(createExceptionMessage("capture"))
    }

    override fun captureBurst(
        requests: List<CaptureRequest>,
        listener: CaptureCallback?,
        handler: Handler?,
    ): Int {
        throw IllegalArgumentException(createExceptionMessage("captureBurst"))
    }

    override fun setRepeatingRequest(
        request: CaptureRequest,
        listener: CaptureCallback?,
        handler: Handler?,
    ): Int {
        throw IllegalArgumentException(createExceptionMessage("setRepeatingRequest"))
    }

    override fun setRepeatingBurst(
        requests: List<CaptureRequest>,
        listener: CaptureCallback?,
        handler: Handler?,
    ): Int {
        throw IllegalArgumentException(createExceptionMessage("setRepeatingBurst"))
    }

    override fun stopRepeating() {
        throw IllegalArgumentException(createExceptionMessage("stopRepeating"))
    }

    override fun abortCaptures() {
        throw IllegalArgumentException(createExceptionMessage("abortCaptures"))
    }

    override fun isReprocessable(): Boolean {
        throw IllegalArgumentException(createExceptionMessage("isReprocessable"))
    }

    override fun getInputSurface(): Surface? {
        throw IllegalArgumentException(createExceptionMessage("getInputSurface"))
    }

    override fun close() {
        throw IllegalArgumentException(createExceptionMessage("close"))
    }

    private fun createExceptionMessage(funcName: String) =
        "Current capture session is running on extensions mode which isn't allowed to invoke the" +
            " $funcName function!"
}
