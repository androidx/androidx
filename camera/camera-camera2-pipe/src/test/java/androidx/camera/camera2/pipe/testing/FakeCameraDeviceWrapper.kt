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

import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.InputConfiguration
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.AudioRestrictionMode
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.compat.CameraCaptureSessionWrapper
import androidx.camera.camera2.pipe.compat.CameraDeviceWrapper
import androidx.camera.camera2.pipe.compat.CameraExtensionSessionWrapper
import androidx.camera.camera2.pipe.compat.ExtensionSessionConfigData
import androidx.camera.camera2.pipe.compat.InputConfigData
import androidx.camera.camera2.pipe.compat.OutputConfigurationWrapper
import androidx.camera.camera2.pipe.compat.SessionConfigData
import kotlin.reflect.KClass

/** Fake implementation of [CameraDeviceWrapper] for tests. */
internal class FakeCameraDeviceWrapper(val fakeCamera: RobolectricCameras.FakeCamera) :
    CameraDeviceWrapper {
    override val cameraId: CameraId
        get() = fakeCamera.cameraId

    var currentStateCallback: CameraCaptureSessionWrapper.StateCallback? = null
    var currentExtensionStateCallback: CameraExtensionSessionWrapper.StateCallback? = null
    var currentSession: FakeCaptureSessionWrapper? = null

    override fun createCaptureRequest(template: RequestTemplate): CaptureRequest.Builder {
        return fakeCamera.cameraDevice.createCaptureRequest(template.value)
    }

    override fun createReprocessCaptureRequest(
        inputResult: TotalCaptureResult
    ): CaptureRequest.Builder {
        return fakeCamera.cameraDevice.createReprocessCaptureRequest(inputResult)
    }

    override fun createCaptureSession(
        outputs: List<Surface>,
        stateCallback: CameraCaptureSessionWrapper.StateCallback,
    ): Boolean {
        createFakeCaptureSession(stateCallback)
        return true
    }

    override fun createCaptureSession(config: SessionConfigData): Boolean {
        createFakeCaptureSession(config.stateCallback)
        return true
    }

    override fun createReprocessableCaptureSession(
        input: InputConfiguration,
        outputs: List<Surface>,
        stateCallback: CameraCaptureSessionWrapper.StateCallback,
    ): Boolean {
        createFakeCaptureSession(stateCallback)
        return true
    }

    override fun createConstrainedHighSpeedCaptureSession(
        outputs: List<Surface>,
        stateCallback: CameraCaptureSessionWrapper.StateCallback,
    ): Boolean {
        createFakeCaptureSession(stateCallback)
        return true
    }

    override fun createCaptureSessionByOutputConfigurations(
        outputConfigurations: List<OutputConfigurationWrapper>,
        stateCallback: CameraCaptureSessionWrapper.StateCallback,
    ): Boolean {
        createFakeCaptureSession(stateCallback)
        return true
    }

    override fun createReprocessableCaptureSessionByConfigurations(
        inputConfig: InputConfigData,
        outputs: List<OutputConfigurationWrapper>,
        stateCallback: CameraCaptureSessionWrapper.StateCallback,
    ): Boolean {
        createFakeCaptureSession(stateCallback)
        return true
    }

    override fun createExtensionSession(config: ExtensionSessionConfigData): Boolean {
        createFakeExtensionSession(config.extensionStateCallback)
        return true
    }

    @RequiresApi(30)
    override fun getCameraAudioRestriction(): AudioRestrictionMode {
        return AudioRestrictionMode(fakeCamera.cameraDevice.cameraAudioRestriction)
    }

    @RequiresApi(30)
    override fun onCameraAudioRestrictionUpdated(mode: AudioRestrictionMode) {
        fakeCamera.cameraDevice.cameraAudioRestriction = mode.value
    }

    override fun onDeviceClosing() {
        currentStateCallback?.onSessionDisconnected()
        currentExtensionStateCallback?.onSessionDisconnected()
    }

    override fun onDeviceClosed() {
        currentStateCallback?.onSessionFinalized()
        currentExtensionStateCallback?.onSessionFinalized()
    }

    fun createFakeCaptureSession(
        stateCallback: CameraCaptureSessionWrapper.StateCallback? = null
    ): FakeCaptureSessionWrapper {
        val nextSession = FakeCaptureSessionWrapper(this)
        currentSession = nextSession
        currentStateCallback = stateCallback
        return nextSession
    }

    private fun createFakeExtensionSession(
        stateCallback: CameraExtensionSessionWrapper.StateCallback? = null
    ): FakeCaptureSessionWrapper {
        val nextSession = FakeCaptureSessionWrapper(this)
        currentSession = nextSession
        currentExtensionStateCallback = stateCallback
        return nextSession
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: KClass<T>): T? =
        when (type) {
            CameraDevice::class -> fakeCamera.cameraDevice as T
            else -> null
        }
}
