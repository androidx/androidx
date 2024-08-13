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
import android.hardware.camera2.CameraExtensionCharacteristics
import android.hardware.camera2.CameraExtensionSession
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.ExtensionSessionConfiguration
import android.hardware.camera2.params.InputConfiguration
import android.hardware.camera2.params.MultiResolutionStreamInfo
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.media.ImageWriter
import android.os.Handler
import android.util.Size
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.camera.camera2.pipe.CameraMetadata
import java.util.concurrent.Executor

@RequiresApi(23)
internal object Api23Compat {
    @JvmStatic
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
    @Throws(CameraAccessException::class)
    fun createReprocessCaptureRequest(
        cameraDevice: CameraDevice,
        inputResult: TotalCaptureResult,
    ): CaptureRequest.Builder {
        return cameraDevice.createReprocessCaptureRequest(inputResult)
    }

    @JvmStatic
    fun isReprocessable(cameraCaptureSession: CameraCaptureSession): Boolean {
        return cameraCaptureSession.isReprocessable
    }

    @JvmStatic
    fun getInputSurface(cameraCaptureSession: CameraCaptureSession): Surface? {
        return cameraCaptureSession.inputSurface
    }

    @JvmStatic
    fun newInputConfiguration(width: Int, height: Int, format: Int): InputConfiguration {
        return InputConfiguration(width, height, format)
    }

    @JvmStatic
    fun checkSelfPermission(context: Context, permission: String): Int {
        return context.checkSelfPermission(permission)
    }
}

@RequiresApi(24)
internal object Api24Compat {
    @JvmStatic
    @Throws(CameraAccessException::class)
    @Suppress("deprecation")
    fun createCaptureSessionByOutputConfigurations(
        cameraDevice: CameraDevice,
        outputConfig: List<OutputConfiguration?>,
        stateCallback: CameraCaptureSession.StateCallback,
        handler: Handler?
    ) {
        cameraDevice.createCaptureSessionByOutputConfigurations(
            outputConfig,
            stateCallback,
            handler
        )
    }

    @JvmStatic
    @Throws(CameraAccessException::class)
    @Suppress("deprecation")
    fun createReprocessableCaptureSessionByConfigurations(
        cameraDevice: CameraDevice,
        inputConfig: InputConfiguration,
        outputs: List<OutputConfiguration?>,
        stateCallback: CameraCaptureSession.StateCallback,
        handler: Handler?
    ) {
        cameraDevice.createReprocessableCaptureSessionByConfigurations(
            inputConfig,
            outputs,
            stateCallback,
            handler
        )
    }

    @JvmStatic
    fun getSurfaceGroupId(outputConfiguration: OutputConfiguration): Int {
        return outputConfiguration.surfaceGroupId
    }
}

@RequiresApi(26)
internal object Api26Compat {
    @JvmStatic
    @Throws(CameraAccessException::class)
    fun finalizeOutputConfigurations(
        cameraCaptureSession: CameraCaptureSession,
        outputConfiguration: List<OutputConfiguration?>
    ) {
        return cameraCaptureSession.finalizeOutputConfigurations(outputConfiguration)
    }

    @JvmStatic
    fun newOutputConfiguration(size: Size, klass: Class<*>): OutputConfiguration {
        return OutputConfiguration(size, klass)
    }

    @JvmStatic
    fun enableSurfaceSharing(outputConfig: OutputConfiguration) {
        outputConfig.enableSurfaceSharing()
    }

    @JvmStatic
    fun getSurfaces(outputConfig: OutputConfiguration): List<Surface> {
        return outputConfig.surfaces
    }

    @JvmStatic
    fun addSurfaces(outputConfig: OutputConfiguration, surface: Surface) {
        return outputConfig.addSurface(surface)
    }

    @JvmStatic
    fun onCaptureQueueEmpty(
        interopSessionStateCallback: CameraCaptureSession.StateCallback?,
        session: CameraCaptureSession,
    ) {
        interopSessionStateCallback?.onCaptureQueueEmpty(session)
    }
}

@RequiresApi(28)
@Suppress("DEPRECATION")
internal object Api28Compat {
    @JvmStatic
    @Throws(CameraAccessException::class)
    fun createCaptureSession(
        cameraDevice: CameraDevice,
        sessionConfig: SessionConfiguration,
    ) {
        cameraDevice.createCaptureSession(sessionConfig)
    }

    @JvmStatic
    fun getAvailablePhysicalCameraRequestKeys(
        cameraCharacteristics: CameraCharacteristics
    ): List<CaptureRequest.Key<*>>? {
        return cameraCharacteristics.availablePhysicalCameraRequestKeys
    }

    @JvmStatic
    fun getAvailableSessionKeys(
        cameraCharacteristics: CameraCharacteristics
    ): List<CaptureRequest.Key<*>>? {
        return cameraCharacteristics.availableSessionKeys
    }

    @JvmStatic
    fun getPhysicalCameraIds(cameraCharacteristics: CameraCharacteristics): Set<String> {
        return cameraCharacteristics.physicalCameraIds
    }

    @JvmStatic
    fun getPhysicalCaptureResults(
        totalCaptureResult: TotalCaptureResult
    ): Map<String, CaptureResult>? {
        return totalCaptureResult.physicalCameraResults
    }

    @JvmStatic
    fun newSessionConfiguration(
        sessionType: Int,
        outputs: List<OutputConfiguration?>,
        executor: Executor,
        stateCallback: CameraCaptureSession.StateCallback
    ): SessionConfiguration {
        return SessionConfiguration(sessionType, outputs, executor, stateCallback)
    }

    @JvmStatic
    fun setInputConfiguration(
        sessionConfig: SessionConfiguration,
        inputConfig: InputConfiguration
    ) {
        sessionConfig.inputConfiguration = inputConfig
    }

    @JvmStatic
    fun setSessionParameters(sessionConfig: SessionConfiguration, params: CaptureRequest) {
        sessionConfig.sessionParameters = params
    }

    @JvmStatic
    fun getMaxSharedSurfaceCount(outputConfig: OutputConfiguration): Int {
        return outputConfig.maxSharedSurfaceCount
    }

    @JvmStatic
    fun setPhysicalCameraId(outputConfig: OutputConfiguration, cameraId: String?) {
        outputConfig.setPhysicalCameraId(cameraId)
    }

    @JvmStatic
    fun removeSurface(outputConfig: OutputConfiguration, surface: Surface) {
        return outputConfig.removeSurface(surface)
    }

    @JvmStatic
    @Throws(CameraAccessException::class)
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
    fun registerAvailabilityCallback(
        cameraManager: CameraManager,
        executor: Executor,
        callback: CameraManager.AvailabilityCallback
    ) {
        cameraManager.registerAvailabilityCallback(executor, callback)
    }

    @JvmStatic
    fun discardFreeBuffers(imageReader: ImageReader) {
        imageReader.discardFreeBuffers()
    }
}

@RequiresApi(29)
internal object Api29Compat {
    @JvmStatic
    fun imageReaderNewInstance(
        width: Int,
        height: Int,
        format: Int,
        capacity: Int,
        usage: Long
    ): ImageReader {
        return ImageReader.newInstance(width, height, format, capacity, usage)
    }

    @JvmStatic
    fun imageWriterNewInstance(surface: Surface, maxImages: Int, format: Int): ImageWriter {
        return ImageWriter.newInstance(surface, maxImages, format)
    }
}

@RequiresApi(30)
internal object Api30Compat {
    @JvmStatic
    fun getConcurrentCameraIds(cameraManager: CameraManager): Set<Set<String>> {
        return cameraManager.concurrentCameraIds
    }

    @JvmStatic
    fun getCameraAudioRestriction(cameraDevice: CameraDevice): Int {
        return cameraDevice.cameraAudioRestriction
    }

    @JvmStatic
    fun setCameraAudioRestriction(cameraDevice: CameraDevice, mode: Int) {
        cameraDevice.cameraAudioRestriction = mode
    }
}

@RequiresApi(31)
internal object Api31Compat {
    @JvmStatic
    fun newInputConfiguration(
        inputConfigData: List<InputConfigData>,
        cameraId: String
    ): InputConfiguration {
        check(inputConfigData.isNotEmpty()) {
            "Call to create InputConfiguration but list of InputConfigData is empty."
        }

        if (inputConfigData.size == 1) {
            val inputData = inputConfigData.first()
            return InputConfiguration(inputData.width, inputData.height, inputData.format)
        }
        val multiResolutionInput =
            inputConfigData.map { input ->
                MultiResolutionStreamInfo(input.width, input.height, cameraId)
            }
        return InputConfiguration(multiResolutionInput, inputConfigData.first().format)
    }

    @JvmStatic
    fun newMultiResolutionStreamInfo(
        streamWidth: Int,
        streamHeight: Int,
        physicalCameraId: String
    ): MultiResolutionStreamInfo {
        return MultiResolutionStreamInfo(streamWidth, streamHeight, physicalCameraId)
    }

    @JvmStatic
    fun getPhysicalCameraTotalResults(
        totalCaptureResult: TotalCaptureResult
    ): Map<String, CaptureResult>? {
        return totalCaptureResult.physicalCameraTotalResults
    }

    @JvmStatic
    fun addSensorPixelModeUsed(
        outputConfiguration: OutputConfiguration,
        sensorPixelMode: Int,
    ) {
        outputConfiguration.addSensorPixelModeUsed(sensorPixelMode)
    }

    @JvmStatic
    fun createExtensionCaptureSession(
        cameraDevice: CameraDevice,
        extensionConfiguration: ExtensionSessionConfiguration
    ) {
        cameraDevice.createExtensionSession(extensionConfiguration)
    }

    @JvmStatic
    fun getCameraExtensionCharacteristics(
        cameraManager: CameraManager,
        cameraId: String
    ): CameraExtensionCharacteristics = cameraManager.getCameraExtensionCharacteristics(cameraId)

    @JvmStatic
    fun newExtensionSessionConfiguration(
        extensionMode: Int,
        outputs: List<OutputConfiguration?>,
        executor: Executor,
        stateCallback: CameraExtensionSession.StateCallback
    ): ExtensionSessionConfiguration {
        return ExtensionSessionConfiguration(extensionMode, outputs, executor, stateCallback)
    }

    @JvmStatic
    fun getSupportedExtensions(
        extensionCharacteristics: CameraExtensionCharacteristics
    ): List<Int> = extensionCharacteristics.supportedExtensions

    @JvmStatic
    fun getExtensionSupportedSizes(
        extensionCharacteristics: CameraExtensionCharacteristics,
        extension: Int,
        imageFormat: Int
    ): List<Size> = extensionCharacteristics.getExtensionSupportedSizes(extension, imageFormat)

    @JvmStatic
    fun getExtensionSupportedSizes(
        extensionCharacteristics: CameraExtensionCharacteristics,
        extension: Int,
        klass: Class<*>
    ): List<Size> = extensionCharacteristics.getExtensionSupportedSizes(extension, klass)
}

@RequiresApi(33)
internal object Api33Compat {
    @JvmStatic
    fun setDynamicRangeProfile(outputConfig: OutputConfiguration, dynamicRangeProfile: Long) {
        outputConfig.dynamicRangeProfile = dynamicRangeProfile
    }

    @JvmStatic
    fun getDynamicRangeProfile(outputConfig: OutputConfiguration): Long {
        return outputConfig.dynamicRangeProfile
    }

    @JvmStatic
    fun setMirrorMode(outputConfig: OutputConfiguration, mirrorMode: Int) {
        outputConfig.mirrorMode = mirrorMode
    }

    @JvmStatic
    fun getMirrorMode(outputConfig: OutputConfiguration): Int {
        return outputConfig.mirrorMode
    }

    @JvmStatic
    fun setStreamUseCase(outputConfig: OutputConfiguration, streamUseCase: Long) {
        outputConfig.streamUseCase = streamUseCase
    }

    @JvmStatic
    fun getAvailableStreamUseCases(cameraMetadata: CameraMetadata): LongArray? {
        return cameraMetadata[CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES]
    }

    @JvmStatic
    fun getStreamUseCase(outputConfig: OutputConfiguration): Long {
        return outputConfig.streamUseCase
    }

    @JvmStatic
    fun setTimestampBase(outputConfig: OutputConfiguration, timestampBase: Int) {
        outputConfig.timestampBase = timestampBase
    }

    @JvmStatic
    fun getTimestampBase(outputConfig: OutputConfiguration): Int {
        return outputConfig.timestampBase
    }

    @JvmStatic
    fun getAvailableCaptureRequestKeys(
        extensionCharacteristics: CameraExtensionCharacteristics,
        extension: Int
    ): Set<CaptureRequest.Key<Any>> =
        extensionCharacteristics.getAvailableCaptureRequestKeys(extension)

    @JvmStatic
    fun getAvailableCaptureResultKeys(
        extensionCharacteristics: CameraExtensionCharacteristics,
        extension: Int
    ): Set<CaptureResult.Key<Any>> =
        extensionCharacteristics.getAvailableCaptureResultKeys(extension)

    @JvmStatic
    fun newImageReaderFromImageReaderBuilder(
        width: Int,
        height: Int,
        imageFormat: Int? = null,
        maxImages: Int? = null,
        usage: Long? = null,
        defaultDataSpace: Int? = null,
        defaultHardwareBufferFormat: Int? = null
    ): ImageReader {
        return ImageReader.Builder(width, height)
            .apply {
                if (imageFormat != null) setImageFormat(imageFormat)
                if (maxImages != null) setMaxImages(maxImages)
                if (usage != null) setUsage(usage)
                if (defaultDataSpace != null) setDefaultDataSpace(defaultDataSpace)
                if (defaultHardwareBufferFormat != null)
                    setDefaultHardwareBufferFormat(defaultHardwareBufferFormat)
            }
            .build()
    }
}

@RequiresApi(34)
internal object Api34Compat {
    @JvmStatic
    fun isPostviewAvailable(
        extensionCharacteristics: CameraExtensionCharacteristics,
        extension: Int
    ): Boolean = extensionCharacteristics.isPostviewAvailable(extension)

    @JvmStatic
    fun isCaptureProcessProgressAvailable(
        extensionCharacteristics: CameraExtensionCharacteristics,
        extension: Int
    ): Boolean = extensionCharacteristics.isCaptureProcessProgressAvailable(extension)

    @JvmStatic
    fun getPostviewSupportedSizes(
        extensionCharacteristics: CameraExtensionCharacteristics,
        extension: Int,
        captureSize: Size,
        format: Int
    ): List<Size> =
        extensionCharacteristics.getPostviewSupportedSizes(extension, captureSize, format)

    @JvmStatic
    fun setPostviewOutputConfiguration(
        extensionSessionConfiguration: ExtensionSessionConfiguration,
        postviewOutputConfiguration: OutputConfiguration
    ) {
        extensionSessionConfiguration.postviewOutputConfiguration = postviewOutputConfiguration
    }
}
