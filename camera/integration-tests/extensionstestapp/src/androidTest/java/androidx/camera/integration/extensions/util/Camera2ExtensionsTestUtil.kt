/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.integration.extensions.util

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraExtensionCharacteristics
import android.hardware.camera2.CameraExtensionSession
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.ExtensionSessionConfiguration
import android.hardware.camera2.params.OutputConfiguration
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.integration.extensions.utils.Camera2ExtensionsUtil.AVAILABLE_CAMERA2_EXTENSION_MODES
import androidx.camera.integration.extensions.utils.CameraIdExtensionModePair
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.LabTestRule
import androidx.camera.testing.SurfaceTextureProvider
import androidx.concurrent.futures.await
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executors
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assume.assumeTrue

@RequiresApi(31)
object Camera2ExtensionsTestUtil {
    private const val LAB_STRESS_TEST_OPERATION_REPEAT_COUNT = 10
    private const val STRESS_TEST_OPERATION_REPEAT_COUNT = 3
    const val EXTENSION_NOT_FOUND = -1

    /**
     * Returns whether the target device is excluded for extensions test
     */
    @JvmStatic
    fun isTargetDeviceExcludedForExtensionsTest(): Boolean {
        // Skips Cuttlefish device since actually it is not a real marketing device which supports
        // extensions and it will cause pre-submit failures.
        return !Build.MODEL.contains("Cuttlefish", true)
    }

    @JvmStatic
    fun getStressTestRepeatingCount() =
        if (LabTestRule.isInLabTest()) {
            LAB_STRESS_TEST_OPERATION_REPEAT_COUNT
        } else {
            STRESS_TEST_OPERATION_REPEAT_COUNT
        }

    /**
     * Gets a list of all camera id and extension mode combinations.
     */
    @JvmStatic
    fun getAllCameraIdExtensionModeCombinations(): List<CameraIdExtensionModePair> =
        CameraUtil.getBackwardCompatibleCameraIdListOrThrow().flatMap { cameraId ->
            AVAILABLE_CAMERA2_EXTENSION_MODES.map { extensionMode ->
                CameraIdExtensionModePair(cameraId, extensionMode)
            }
        }

    suspend fun assertCanOpenExtensionsSession(
        cameraManager: CameraManager,
        cameraId: String,
        extensionMode: Int,
        verifyOutput: Boolean = false
    ) {
        val extensionsCharacteristics = cameraManager.getCameraExtensionCharacteristics(cameraId)
        assumeCameraExtensionSupported(extensionMode, extensionsCharacteristics)

        // Preview surface
        val previewSize = extensionsCharacteristics
            .getExtensionSupportedSizes(extensionMode, SurfaceTexture::class.java)
            .maxBy { it.width * it.height }
        val deferredPreviewFrame = CompletableDeferred<SurfaceTexture>()

        val executorForGL = Executors.newSingleThreadExecutor()
        // Some OEM requires frames drain (updateTexImage being invoked) in SurfaceTexture,
        // otherwise it might cause still capture to fail.
        val surfaceTextureHolder = SurfaceTextureProvider.createAutoDrainingSurfaceTextureAsync(
            executorForGL,
            previewSize.width,
            previewSize.height, {
                if (!deferredPreviewFrame.isCompleted) {
                    deferredPreviewFrame.complete(it)
                }
            }) { executorForGL.shutdown() }.await()
        val previewSurface = Surface(surfaceTextureHolder.surfaceTexture)

        // Still capture surface
        val imageReader = createCaptureImageReader(extensionsCharacteristics, extensionMode)
        val captureSurface = imageReader.surface

        val cameraDevice = openCameraDevice(cameraManager, cameraId)
        val outputConfigurationPreview = OutputConfiguration(previewSurface)
        val outputConfigurationCapture = OutputConfiguration(captureSurface)
        val extensionSession = openExtensionSession(
            cameraDevice,
            extensionMode,
            listOf(outputConfigurationPreview, outputConfigurationCapture)
        )
        assertThat(extensionSession).isNotNull()

        val builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        builder.addTarget(previewSurface)

        extensionSession.setRepeatingRequest(builder.build(), CameraXExecutors.ioExecutor(),
            object : CameraExtensionSession.ExtensionCaptureCallback() {
                override fun onCaptureSequenceCompleted(
                    session: CameraExtensionSession,
                    sequenceId: Int
                ) {
                }

                override fun onCaptureStarted(
                    session: CameraExtensionSession,
                    request: CaptureRequest,
                    timestamp: Long
                ) {
                }

                override fun onCaptureProcessStarted(
                    session: CameraExtensionSession,
                    request: CaptureRequest
                ) {
                }

                override fun onCaptureFailed(
                    session: CameraExtensionSession,
                    request: CaptureRequest
                ) {
                }

                override fun onCaptureSequenceAborted(
                    session: CameraExtensionSession,
                    sequenceId: Int
                ) {
                }

                override fun onCaptureResultAvailable(
                    session: CameraExtensionSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                }
            })

        if (verifyOutput) {
            deferredPreviewFrame.await()
            val image = takePicture(cameraDevice, extensionSession, imageReader)
            assertThat(image).isNotNull()
            image!!.close()
        }

        extensionSession.close()
        cameraDevice.close()
        imageReader.close()
        previewSurface.release()
        captureSurface.release()
        surfaceTextureHolder.close()
    }

    /**
     * Check if the device supports the [extensionMode] and other extension specific characteristics
     * required for testing. Halt the test if any criteria is not satisfied.
     */
    fun assumeCameraExtensionSupported(
        extensionMode: Int,
        extensionsCharacteristics: CameraExtensionCharacteristics
    ) {
        assumeTrue(extensionsCharacteristics.supportedExtensions.contains(extensionMode))
        assumeTrue(
            extensionsCharacteristics
                .getExtensionSupportedSizes(extensionMode, SurfaceTexture::class.java).isNotEmpty()
        )
        assumeTrue(
            extensionsCharacteristics
                .getExtensionSupportedSizes(extensionMode, ImageFormat.JPEG).isNotEmpty()
        )
    }

    fun createCaptureImageReader(
        extensionsCharacteristics: CameraExtensionCharacteristics,
        extensionMode: Int
    ): ImageReader {
        val captureSize = extensionsCharacteristics
            .getExtensionSupportedSizes(extensionMode, ImageFormat.JPEG)
            .maxBy { it.width * it.height }
        return ImageReader
            .newInstance(captureSize.width, captureSize.height, ImageFormat.JPEG, 2)
    }

    /**
     * Open the camera device and return the [CameraDevice] instance.
     */
    suspend fun openCameraDevice(
        cameraManager: CameraManager,
        cameraId: String
    ): CameraDevice {
        val deferred = CompletableDeferred<CameraDevice>()
        cameraManager.openCamera(
            cameraId,
            CameraXExecutors.ioExecutor(),
            object : CameraDevice.StateCallback() {
                override fun onOpened(cameraDevice: CameraDevice) {
                    deferred.complete(cameraDevice)
                }

                override fun onDisconnected(cameraDevice: CameraDevice) {
                    deferred.completeExceptionally(RuntimeException("Camera Disconnected"))
                }

                override fun onError(cameraDevice: CameraDevice, error: Int) {
                    deferred.completeExceptionally(
                        RuntimeException("Camera onError(error=$cameraDevice)")
                    )
                }
            })
        return deferred.await()
    }

    /**
     * Open the [CameraExtensionSession] and return the instance.
     */
    suspend fun openExtensionSession(
        cameraDevice: CameraDevice,
        extensionMode: Int,
        outputConfigs: List<OutputConfiguration>
    ): CameraExtensionSession {
        val deferred = CompletableDeferred<CameraExtensionSession>()

        val extensionSessionConfiguration = ExtensionSessionConfiguration(
            extensionMode,
            outputConfigs,
            CameraXExecutors.ioExecutor(),
            object : CameraExtensionSession.StateCallback() {
                override fun onConfigured(cameraExtensionSession: CameraExtensionSession) {
                    deferred.complete(cameraExtensionSession)
                }

                override fun onConfigureFailed(session: CameraExtensionSession) {
                    deferred.completeExceptionally(RuntimeException("onConfigureFailed"))
                }

                override fun onClosed(session: CameraExtensionSession) {
                }
            }
        )
        cameraDevice.createExtensionSession(extensionSessionConfiguration)
        return deferred.await()
    }

    /**
     * Take a picture with the provided [session] and output the contents to the [imageReader]. The
     * latest image written to the [imageReader] is returned.
     */
    suspend fun takePicture(
        cameraDevice: CameraDevice,
        session: CameraExtensionSession,
        imageReader: ImageReader
    ): Image? {
        val builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        builder.addTarget(imageReader.surface)
        val deferredCapture = CompletableDeferred<Int>()
        session.capture(builder.build(), CameraXExecutors.ioExecutor(),
            object : CameraExtensionSession.ExtensionCaptureCallback() {
                override fun onCaptureSequenceCompleted(
                    session: CameraExtensionSession,
                    sequenceId: Int
                ) {
                    deferredCapture.complete(sequenceId)
                }

                override fun onCaptureStarted(
                    session: CameraExtensionSession,
                    request: CaptureRequest,
                    timestamp: Long
                ) {
                }

                override fun onCaptureProcessStarted(
                    session: CameraExtensionSession,
                    request: CaptureRequest
                ) {
                }

                override fun onCaptureFailed(
                    session: CameraExtensionSession,
                    request: CaptureRequest
                ) {
                    deferredCapture.completeExceptionally(RuntimeException("onCaptureFailed"))
                }

                override fun onCaptureSequenceAborted(
                    session: CameraExtensionSession,
                    sequenceId: Int
                ) {
                    deferredCapture.completeExceptionally(
                        RuntimeException("onCaptureSequenceAborted")
                    )
                }
            })

        val deferredImage = CompletableDeferred<Image?>()
        imageReader.setOnImageAvailableListener({
            val image = imageReader.acquireNextImage()
            deferredImage.complete(image)
        }, Handler(Looper.getMainLooper()))
        deferredCapture.await()
        return deferredImage.await()
    }

    fun findNextSupportedCameraId(
        context: Context,
        currentCameraId: String,
        extensionsMode: Int
    ): String? {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val supportedCameraIdList = cameraManager.cameraIdList.filter {
                val characteristics = cameraManager.getCameraCharacteristics(it)
                val backwardCompatible = characteristics
                    .get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)!!
                    .toList()
                    .contains(CameraCharacteristics
                        .REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE)
                if (!backwardCompatible) {
                    return@filter false
                }
                val extCharacteristics = cameraManager.getCameraExtensionCharacteristics((it))
                return@filter extCharacteristics.supportedExtensions.contains(extensionsMode)
            }

            if (supportedCameraIdList.size <= 1) {
                return null
            }
            val currentIndex = supportedCameraIdList.indexOf(currentCameraId)
            return supportedCameraIdList[(currentIndex + 1) % supportedCameraIdList.size]
        } catch (e: CameraAccessException) {
        }
        return null
    }

    fun findNextEffectMode(
        context: Context,
        cameraId: String,
        extensionsMode: Int
    ): Int {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val characteristics = cameraManager.getCameraExtensionCharacteristics((cameraId))
            val supportedExtensions = ArrayList(characteristics.supportedExtensions).apply {
                sort()
            }
            val currentIndex = supportedExtensions.indexOf(extensionsMode)
            if (currentIndex >= 0 && supportedExtensions.size > 1) {
                return supportedExtensions[(currentIndex + 1) % supportedExtensions.size]
            }
        } catch (e: CameraAccessException) {
        }
        return EXTENSION_NOT_FOUND
    }
}