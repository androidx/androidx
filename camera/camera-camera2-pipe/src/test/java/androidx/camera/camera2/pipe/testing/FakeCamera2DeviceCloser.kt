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

package androidx.camera.camera2.pipe.testing

import android.hardware.camera2.CameraDevice
import androidx.camera.camera2.pipe.compat.AndroidCameraState
import androidx.camera.camera2.pipe.compat.AudioRestrictionController
import androidx.camera.camera2.pipe.compat.Camera2DeviceCloser
import androidx.camera.camera2.pipe.compat.CameraDeviceWrapper

internal class FakeCamera2DeviceCloser : Camera2DeviceCloser {
    /** Parameters recorded from a single [closeCamera] invocation on a camera device. */
    data class CloseCameraCall(
        val cameraDeviceWrapper: CameraDeviceWrapper?,
        val cameraDevice: CameraDevice?,
        val androidCameraState: AndroidCameraState,
        val audioRestrictionController: AudioRestrictionController,
        val shouldReopenCamera: Boolean,
        val shouldCreateEmptyCaptureSession: Boolean,
        val shouldWaitForCameraDeviceOnClosed: Boolean,
    )

    /** Mutable backing list for [closeCameraCalls]. */
    private val _closeCameraCalls = mutableListOf<CloseCameraCall>()

    /** Recorded [closeCamera] invocations on a camera device in call order. */
    val closeCameraCalls: List<CloseCameraCall>
        get() = _closeCameraCalls

    override fun closeCamera(
        cameraDeviceWrapper: CameraDeviceWrapper?,
        cameraDevice: CameraDevice?,
        androidCameraState: AndroidCameraState,
        audioRestrictionController: AudioRestrictionController,
        shouldReopenCamera: Boolean,
        shouldCreateEmptyCaptureSession: Boolean,
        shouldWaitForCameraDeviceOnClosed: Boolean,
    ) {
        if (cameraDeviceWrapper != null || cameraDevice != null) {
            _closeCameraCalls.add(
                CloseCameraCall(
                    cameraDeviceWrapper = cameraDeviceWrapper,
                    cameraDevice = cameraDevice,
                    androidCameraState = androidCameraState,
                    audioRestrictionController = audioRestrictionController,
                    shouldReopenCamera = shouldReopenCamera,
                    shouldCreateEmptyCaptureSession = shouldCreateEmptyCaptureSession,
                    shouldWaitForCameraDeviceOnClosed = shouldWaitForCameraDeviceOnClosed,
                )
            )
        }
        cameraDeviceWrapper?.onDeviceClosing()
        cameraDeviceWrapper?.onDeviceClosed()
    }
}
