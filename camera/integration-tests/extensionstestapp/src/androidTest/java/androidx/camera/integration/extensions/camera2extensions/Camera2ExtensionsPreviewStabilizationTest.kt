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

package androidx.camera.integration.extensions.camera2extensions

import android.content.Context
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraExtensionCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.Handler
import android.os.Looper
import android.view.Surface
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.integration.extensions.util.Camera2ExtensionsTestUtil
import androidx.camera.integration.extensions.util.Camera2ExtensionsTestUtil.assumeCameraExtensionSupported
import androidx.camera.integration.extensions.util.Camera2ExtensionsTestUtil.openCameraDevice
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.concurrent.futures.await
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executors
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Tests for the presence of a valid crop rect region when preview stabilization is enabled. A valid
 * crop rect region means that the crop rect is available in the capture result when preview
 * stabilization is enabled and that it is not the same as the crop rect when there is no video
 * stabilization mode applied.
 */
@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 31)
class Camera2ExtensionsPreviewStabilizationTest(private val cameraId: String) {
    @get:Rule
    val useCamera =
        CameraUtil.grantCameraPermissionAndPreTest(
            CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
        )

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private lateinit var cameraDevice: CameraDevice

    companion object {
        @Parameterized.Parameters(name = "cameraId = {0}")
        @JvmStatic
        fun parameters(): List<String> = CameraUtil.getBackwardCompatibleCameraIdListOrThrow()
    }

    @Before
    fun setUp(): Unit = runBlocking {
        assumeTrue(Camera2ExtensionsTestUtil.isTargetDeviceExcludedForExtensionsTest())

        val extensionMode = CameraExtensionCharacteristics.EXTENSION_NIGHT

        val extensionsCharacteristics = cameraManager.getCameraExtensionCharacteristics(cameraId)
        assumeCameraExtensionSupported(extensionMode, extensionsCharacteristics)

        val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
        val availableVideoStabilizationModes = cameraCharacteristics.get(
            CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES
        )
        assumeTrue(availableVideoStabilizationModes?.contains(
            CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION) == true)
    }

    @Test
    fun enablePreviewStabilization_verifyCropRectIsInCaptureResult(): Unit = runBlocking {
        cameraDevice = openCameraDevice(cameraManager, cameraId)

        // Preview surface
        val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
        val configs = cameraCharacteristics.get(
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
        val previewSize = configs.getOutputSizes(SurfaceTexture::class.java)
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

        val cameraDevice = openCameraDevice(cameraManager, cameraId)
        val outputConfigurationPreview = OutputConfiguration(previewSurface)

        val cameraSession = openCameraSession(cameraDevice, listOf(outputConfigurationPreview))
        assertThat(cameraSession).isNotNull()

        val cameraHandler = Handler(Looper.getMainLooper())

        val requestBuilder =
            cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(previewSurface)
                set(
                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION
                )
            }

        // Get the crop rect with preview stabilization enabled
        val cropRectWithStabilizationDeferred = CompletableDeferred<Rect?>()
        cameraSession.setRepeatingRequest(
            requestBuilder.build(),
            CaptureCallback(cropRectWithStabilizationDeferred),
            cameraHandler
        )

        val cropRectWithStabilization = cropRectWithStabilizationDeferred.await()
        assertThat(cropRectWithStabilization).isNotNull()

        // Get the crop rect without any video stabilization enabled
        requestBuilder.set(
            CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
            CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF
        )

        val cropRectNormalDeferred = CompletableDeferred<Rect?>()
        cameraSession.setRepeatingRequest(
            requestBuilder.build(),
            CaptureCallback(cropRectNormalDeferred),
            cameraHandler
        )

        val cropRectNormal = cropRectNormalDeferred.await()

        // Verify that the two crop rect regions are not the same
        assertThat(cropRectNormal).isNotEqualTo(cropRectWithStabilization)

        // Verify that the video stabilization rect is within the bounds of the normal crop rect
        if (cropRectNormal != null) {
            assertThat(cropRectNormal.contains(cropRectWithStabilization!!))
        }

        cameraSession.close()
        cameraDevice.close()
        previewSurface.release()
        surfaceTextureHolder.close()
    }

    private suspend fun openCameraSession(
        cameraDevice: CameraDevice,
        outputConfigs: List<OutputConfiguration>
    ): CameraCaptureSession {
        val deferred = CompletableDeferred<CameraCaptureSession>()

        val sessionConfiguration = SessionConfiguration(
            SessionConfiguration.SESSION_REGULAR,
            outputConfigs,
            CameraXExecutors.ioExecutor(),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    deferred.complete(session)
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    deferred.completeExceptionally(RuntimeException("onConfigurationFailed"))
                }
            })

        cameraDevice.createCaptureSession(sessionConfiguration)
        return deferred.await()
    }

    private class CaptureCallback(private val deferred: CompletableDeferred<Rect?>) :
        CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            deferred.complete(result.get(CaptureResult.SCALER_CROP_REGION))
        }
    }
}
