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

package androidx.camera.camera2.pipe.compat

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraExtensionSession
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.ExtensionSessionConfiguration
import android.hardware.camera2.params.InputConfiguration
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.Build
import android.os.Handler
import android.util.Size
import android.view.Surface
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.camera.camera2.pipe.CameraMetadata
import java.util.concurrent.Executor

@RequiresApi(Build.VERSION_CODES.M)
internal object Api23Compat {
    @JvmStatic
    @DoNotInline
    @Throws(CameraAccessException::class)
    @Suppress("deprecation")
    fun createReprocessableCaptureSession(
        cameraDevice: CameraDevice,
        inputConfig: InputConfiguration,
        outputs: List<Surface>,
        callback: CameraCaptureSession.StateCallback,
        handler: Handler?
    ) {
        cameraDevice.createReprocessableCaptureSession(inputConfig, outputs, callback, handler)
    }

    @JvmStatic
    @DoNotInline
    @Throws(CameraAccessException::class)
    @Suppress("deprecation")
    fun createConstrainedHighSpeedCaptureSession(
        cameraDevice: CameraDevice,
        outputs: List<Surface>,
        stateCallback: CameraCaptureSession.StateCallback,
        handler: Handler?
    ) {
        cameraDevice.createConstrainedHighSpeedCaptureSession(outputs, stateCallback, handler)
    }

    @JvmStatic
    @DoNotInline
    @Throws(CameraAccessException::class)
    fun createReprocessCaptureRequest(
        cameraDevice: CameraDevice,
        inputResult: TotalCaptureResult,
    ): CaptureRequest.Builder {
        return cameraDevice.createReprocessCaptureRequest(inputResult)
    }

    @JvmStatic
    @DoNotInline
    fun isReprocessable(cameraCaptureSession: CameraCaptureSession): Boolean {
        return cameraCaptureSession.isReprocessable
    }

    @JvmStatic
    @DoNotInline
    fun getInputSurface(cameraCaptureSession: CameraCaptureSession): Surface? {
        return cameraCaptureSession.inputSurface
    }

    @JvmStatic
    @DoNotInline
    fun newInputConfiguration(width: Int, height: Int, format: Int): InputConfiguration {
        return InputConfiguration(width, height, format)
    }

    @JvmStatic
    @DoNotInline
    fun checkSelfPermission(context: Context, permission: String): Int {
        return context.checkSelfPermission(permission)
    }
}

@RequiresApi(Build.VERSION_CODES.N)
internal object Api24Compat {
    @JvmStatic
    @DoNotInline
    @Throws(CameraAccessException::class)
    @Suppress("deprecation")
    fun createCaptureSessionByOutputConfigurations(
        cameraDevice: CameraDevice,
        outputConfig: List<OutputConfiguration?>,
        stateCallback: CameraCaptureSession.StateCallback,
        handler: Handler?
    ) {
        cameraDevice.createCaptureSessionByOutputConfigurations(
            outputConfig, stateCallback, handler
        )
    }

    @JvmStatic
    @DoNotInline
    @Throws(CameraAccessException::class)
    @Suppress("deprecation")
    fun createCaptureSessionByOutputConfigurations(
        cameraDevice: CameraDevice,
        inputConfig: InputConfiguration,
        outputs: List<OutputConfiguration?>,
        stateCallback: CameraCaptureSession.StateCallback,
        handler: Handler?
    ) {
        cameraDevice.createReprocessableCaptureSessionByConfigurations(
            inputConfig, outputs, stateCallback, handler
        )
    }

    @JvmStatic
    @DoNotInline
    fun getSurfaceGroupId(outputConfiguration: OutputConfiguration): Int {
        return outputConfiguration.surfaceGroupId
    }
}

@RequiresApi(Build.VERSION_CODES.O)
internal object Api26Compat {
    @JvmStatic
    @DoNotInline
    @Throws(CameraAccessException::class)
    fun finalizeOutputConfigurations(
        cameraCaptureSession: CameraCaptureSession,
        outputConfiguration: List<OutputConfiguration?>
    ) {
        return cameraCaptureSession.finalizeOutputConfigurations(outputConfiguration)
    }

    @JvmStatic
    @DoNotInline
    fun newOutputConfiguration(size: Size, klass: Class<*>): OutputConfiguration {
        return OutputConfiguration(size, klass)
    }

    @JvmStatic
    @DoNotInline
    fun enableSurfaceSharing(outputConfig: OutputConfiguration) {
        outputConfig.enableSurfaceSharing()
    }

    @JvmStatic
    @DoNotInline
    fun getSurfaces(outputConfig: OutputConfiguration): List<Surface> {
        return outputConfig.surfaces
    }

    @JvmStatic
    @DoNotInline
    fun addSurfaces(outputConfig: OutputConfiguration, surface: Surface) {
        return outputConfig.addSurface(surface)
    }
}

@RequiresApi(Build.VERSION_CODES.P)
@Suppress("DEPRECATION")
internal object Api28Compat {
    @JvmStatic
    @Throws(CameraAccessException::class)
    @DoNotInline
    fun createCaptureSession(
        cameraDevice: CameraDevice,
        sessionConfig: SessionConfiguration,
    ) {
        cameraDevice.createCaptureSession(sessionConfig)
    }

    @JvmStatic
    @DoNotInline
    fun getAvailablePhysicalCameraRequestKeys(
        cameraCharacteristics: CameraCharacteristics
    ): List<CaptureRequest.Key<*>>? {
        return cameraCharacteristics.availablePhysicalCameraRequestKeys
    }

    @JvmStatic
    @DoNotInline
    fun getAvailableSessionKeys(
        cameraCharacteristics: CameraCharacteristics
    ): List<CaptureRequest.Key<*>>? {
        return cameraCharacteristics.availableSessionKeys
    }

    @JvmStatic
    @DoNotInline
    fun getPhysicalCameraIds(cameraCharacteristics: CameraCharacteristics): Set<String> {
        return cameraCharacteristics.physicalCameraIds
    }

    @JvmStatic
    @DoNotInline
    fun getPhysicalCaptureResults(
        totalCaptureResult: TotalCaptureResult
    ): Map<String, CaptureResult>? {
        return totalCaptureResult.physicalCameraResults
    }

    @JvmStatic
    @DoNotInline
    fun newSessionConfiguration(
        sessionType: Int,
        outputs: List<OutputConfiguration?>,
        executor: Executor,
        stateCallback: CameraCaptureSession.StateCallback
    ): SessionConfiguration {
        return SessionConfiguration(sessionType, outputs, executor, stateCallback)
    }

    @JvmStatic
    @DoNotInline
    fun setInputConfiguration(
        sessionConfig: SessionConfiguration,
        inputConfig: InputConfiguration
    ) {
        sessionConfig.inputConfiguration = inputConfig
    }

    @JvmStatic
    @DoNotInline
    fun setSessionParameters(sessionConfig: SessionConfiguration, params: CaptureRequest) {
        sessionConfig.sessionParameters = params
    }

    @JvmStatic
    @DoNotInline
    fun getMaxSharedSurfaceCount(outputConfig: OutputConfiguration): Int {
        return outputConfig.maxSharedSurfaceCount
    }

    @JvmStatic
    @DoNotInline
    fun setPhysicalCameraId(outputConfig: OutputConfiguration, cameraId: String?) {
        outputConfig.setPhysicalCameraId(cameraId)
    }

    @JvmStatic
    @DoNotInline
    fun removeSurface(outputConfig: OutputConfiguration, surface: Surface) {
        return outputConfig.removeSurface(surface)
    }

    @JvmStatic
    @Throws(CameraAccessException::class)
    @DoNotInline
    @RequiresPermission(android.Manifest.permission.CAMERA)
    fun openCamera(
        cameraManager: CameraManager,
        cameraId: String,
        executor: Executor,
        callback: CameraDevice.StateCallback
    ) {
        cameraManager.openCamera(cameraId, executor, callback)
    }

    @JvmStatic
    @DoNotInline
    fun registerAvailabilityCallback(
        cameraManager: CameraManager,
        executor: Executor,
        callback: CameraManager.AvailabilityCallback
    ) {
        cameraManager.registerAvailabilityCallback(executor, callback)
    }
}

@RequiresApi(Build.VERSION_CODES.S)
internal object Api31Compat {
    @JvmStatic
    @DoNotInline
    fun createExtensionCaptureSession(
        cameraDevice: CameraDevice,
        extensionConfiguration: ExtensionSessionConfiguration
    ) {
        cameraDevice.createExtensionSession(extensionConfiguration)
    }

    @JvmStatic
    @DoNotInline
    fun newExtensionSessionConfiguration(
        extensionMode: Int,
        outputs: List<OutputConfiguration?>,
        executor: Executor,
        stateCallback: CameraExtensionSession.StateCallback
    ): ExtensionSessionConfiguration {
        return ExtensionSessionConfiguration(extensionMode, outputs, executor, stateCallback)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal object Api33Compat {
    @JvmStatic
    @DoNotInline
    fun setDynamicRangeProfile(outputConfig: OutputConfiguration, dynamicRangeProfile: Long) {
        outputConfig.dynamicRangeProfile = dynamicRangeProfile
    }

    @JvmStatic
    @DoNotInline
    fun getDynamicRangeProfile(outputConfig: OutputConfiguration): Long {
        return outputConfig.dynamicRangeProfile
    }

    @JvmStatic
    @DoNotInline
    fun setMirrorMode(outputConfig: OutputConfiguration, mirrorMode: Int) {
        outputConfig.mirrorMode = mirrorMode
    }

    @JvmStatic
    @DoNotInline
    fun getMirrorMode(outputConfig: OutputConfiguration): Int {
        return outputConfig.mirrorMode
    }

    @JvmStatic
    @DoNotInline
    fun setStreamUseCase(outputConfig: OutputConfiguration, streamUseCase: Long) {
        outputConfig.streamUseCase = streamUseCase
    }

    @JvmStatic
    @DoNotInline
    fun getAvailableStreamUseCases(cameraMetadata: CameraMetadata): LongArray? {
        return cameraMetadata[CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES]
    }

    @JvmStatic
    @DoNotInline
    fun getStreamUseCase(outputConfig: OutputConfiguration): Long {
        return outputConfig.streamUseCase
    }

    @JvmStatic
    @DoNotInline
    fun setTimestampBase(outputConfig: OutputConfiguration, timestampBase: Int) {
        outputConfig.timestampBase = timestampBase
    }

    @JvmStatic
    @DoNotInline
    fun getTimestampBase(outputConfig: OutputConfiguration): Int {
        return outputConfig.timestampBase
    }
}
