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

package androidx.camera.integration.core

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON
import android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH
import android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureRequest.CONTROL_AE_MODE
import android.hardware.camera2.CaptureRequest.FLASH_MODE
import android.hardware.camera2.CaptureRequest.FLASH_MODE_OFF
import android.hardware.camera2.TotalCaptureResult
import android.os.Build
import android.util.Size
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.internal.compat.quirk.CrashWhenTakingPhotoWithAutoFlashAEModeQuirk
import androidx.camera.camera2.internal.compat.quirk.DeviceQuirks
import androidx.camera.camera2.internal.compat.quirk.ImageCaptureFailWithAutoFlashQuirk
import androidx.camera.camera2.internal.compat.quirk.ImageCaptureFlashNotFireQuirk
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.Camera
import androidx.camera.core.CameraEffect.IMAGE_CAPTURE
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.integration.core.util.CameraPipeUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.LabTestRule
import androidx.camera.testing.impl.StreamSharingForceEnabledEffect
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.hamcrest.CoreMatchers.equalTo
import org.junit.After
import org.junit.Assume
import org.junit.Assume.assumeThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private val BACK_SELECTOR = CameraSelector.DEFAULT_BACK_CAMERA
private const val BACK_LENS_FACING = CameraSelector.LENS_FACING_BACK
private const val CAPTURE_TIMEOUT = 15_000.toLong() //  15 seconds

@LargeTest
@RunWith(Parameterized::class)
class FlashTest(private val implName: String, private val cameraXConfig: CameraXConfig) {

    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(
            active = implName == CameraPipeConfig::class.simpleName,
        )

    @get:Rule
    val cameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(PreTestCameraIdList(cameraXConfig))

    @get:Rule val labTest: LabTestRule = LabTestRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            listOf(
                arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
                arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig())
            )
    }

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var cameraProvider: ProcessCameraProvider

    @Volatile private var isReadyToCaptureImage = false

    @Before
    fun setUp() {
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(BACK_LENS_FACING))
        ProcessCameraProvider.configureInstance(cameraXConfig)
        cameraProvider = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]
        isReadyToCaptureImage = false
    }

    @After
    fun tearDown(): Unit = runBlocking {
        if (::cameraProvider.isInitialized) {
            withContext(Dispatchers.Main) { cameraProvider.shutdownAsync()[10, TimeUnit.SECONDS] }
        }
    }

    @LabTestRule.LabTestRearCamera
    @Test
    fun canCaptureWithFlashOn() {
        canTakePicture(
            flashMode = ImageCapture.FLASH_MODE_ON,
            captureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
            addSharedEffect = false
        )
    }

    @LabTestRule.LabTestRearCamera
    @Test
    fun canCaptureWithFlashAuto() {
        canTakePicture(
            flashMode = ImageCapture.FLASH_MODE_AUTO,
            captureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
            addSharedEffect = false
        )
    }

    // Camera gets stuck when taking pictures with flash ON or AUTO in dark environment. See
    // b/193336562 for details. The test simulates taking photo in a dark environment by
    // allocating the devices that the rear camera is blocked. It needs to use the annotation
    // @LabTestRule.LabTestFrontCamera and run the test with the rear camera.
    @LabTestRule.LabTestFrontCamera
    @Test
    fun canCaptureWithFlashOnInDarkEnvironment() {
        canTakePicture(
            flashMode = ImageCapture.FLASH_MODE_ON,
            captureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
            addSharedEffect = false
        )
    }

    @LabTestRule.LabTestFrontCamera
    @Test
    fun canCaptureWithFlashAutoInDarkEnvironment() {
        canTakePicture(
            flashMode = ImageCapture.FLASH_MODE_AUTO,
            captureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
            addSharedEffect = false
        )
    }

    // Camera gets stuck when taking maximum quality mode picture with flash ON or AUTO in dark
    // environment. See b/194046401 for details. The test simulates taking photo in a dark
    // environment by allocating the devices that the rear camera is blocked. It needs to use the
    // annotation @LabTestRule.LabTestFrontCamera and run the test with the rear camera.
    @LabTestRule.LabTestFrontCamera
    @Test
    fun canCaptureMaxQualityPhoto_withFlashOn_inDarkEnvironment() {
        canTakePicture(
            flashMode = ImageCapture.FLASH_MODE_ON,
            captureMode = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY,
            addSharedEffect = false
        )
    }

    @LabTestRule.LabTestFrontCamera
    @Test
    fun canCaptureMaxQualityPhoto_withFlashAuto_inDarkEnvironment() {
        canTakePicture(
            flashMode = ImageCapture.FLASH_MODE_AUTO,
            captureMode = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY,
            addSharedEffect = false
        )
    }

    @Test
    fun requestAeModeIsOnAlwaysFlash_whenCapturedWithFlashOn() {
        verifyRequestAeOrFlashModeForFlashModeCapture(ImageCapture.FLASH_MODE_ON)
    }

    @Test
    fun requestAeModeIsOnAutoFlash_whenCapturedWithFlashAuto() {
        verifyRequestAeOrFlashModeForFlashModeCapture(ImageCapture.FLASH_MODE_AUTO)
    }

    @Test
    fun flashEnabledInRequest_whenCapturedWithFlashOnAndSharedEffect() {
        verifyRequestAeOrFlashModeForFlashModeCapture(
            ImageCapture.FLASH_MODE_ON,
            addSharedEffect = true,
            // In this test, torch as flash workaround should always be used
            expectedAeMode = CONTROL_AE_MODE_ON,
        )
    }

    private fun verifyRequestAeOrFlashModeForFlashModeCapture(
        @ImageCapture.FlashMode flashMode: Int,
        addSharedEffect: Boolean = false,
        expectedAeMode: Int? = null,
    ) {
        Assume.assumeFalse(
            "Cuttlefish API 29 has AE mode availability issue for flash enabled modes." +
                "Unable to test.",
            Build.MODEL.contains("Cuttlefish") && Build.VERSION.SDK_INT == 29
        )

        Assume.assumeTrue(
            "Flash unit not available with back lens facing camera",
            CameraUtil.hasFlashUnitWithLensFacing(BACK_LENS_FACING)
        )

        val captureCallback =
            object : CameraCaptureSession.CaptureCallback() {
                @Volatile var isFlashModeSet = false
                @Volatile var isAeModeExpected = true

                private val expectedAeMode =
                    expectedAeMode
                        ?: when (flashMode) {
                            ImageCapture.FLASH_MODE_ON -> CONTROL_AE_MODE_ON_ALWAYS_FLASH
                            ImageCapture.FLASH_MODE_AUTO -> CONTROL_AE_MODE_ON_AUTO_FLASH
                            else -> CONTROL_AE_MODE_ON
                        }

                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    if (!isReadyToCaptureImage) return

                    if (request[FLASH_MODE] != null && request[FLASH_MODE] != FLASH_MODE_OFF) {
                        isFlashModeSet = true
                    }

                    if (request[CONTROL_AE_MODE] != this.expectedAeMode) {
                        isAeModeExpected = false
                    }
                }
            }

        canTakePicture(
            flashMode = flashMode,
            captureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
            captureCallback = captureCallback,
            flashMustBeSupported = true,
            assertCaptureCount = false,
            addSharedEffect = addSharedEffect,
        )

        Truth.assertThat(captureCallback.isAeModeExpected || captureCallback.isFlashModeSet)
            .isTrue()
    }

    private fun canTakePicture(
        flashMode: Int,
        captureMode: Int,
        captureCallback: CameraCaptureSession.CaptureCallback? = null,
        flashMustBeSupported: Boolean = false,
        assertCaptureCount: Boolean = true,
        addSharedEffect: Boolean
    ) = runBlocking {
        val imageCapture =
            ImageCapture.Builder()
                .also { builder ->
                    captureCallback?.let {
                        CameraPipeUtil.setCameraCaptureSessionCallback(implName, builder, it)
                    }
                }
                .setFlashMode(flashMode)
                .setCaptureMode(captureMode)
                .build()

        val preview = Preview.Builder().build()

        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())

        val useCaseGroup =
            UseCaseGroup.Builder()
                .addUseCase(preview)
                .addUseCase(imageCapture)
                .apply {
                    if (addSharedEffect) {
                        addUseCase(videoCapture)
                        addEffect(StreamSharingForceEnabledEffect(IMAGE_CAPTURE))
                    }
                }
                .build()

        withContext(Dispatchers.Main) {
            preview.setSurfaceProvider(getSurfaceProvider())

            val fakeLifecycleOwner = FakeLifecycleOwner()
            fakeLifecycleOwner.startAndResume()
            val camera =
                cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCaseGroup)

            if (flashMustBeSupported) {
                Assume.assumeTrue(
                    "Test with flashMode($flashMode) is not supported on this device",
                    isFlashTestSupported(camera, flashMode),
                )
            }
        }

        // Take picture after preview is ready for a while. It can cause issue on some devices when
        // flash is on.
        delay(2_000)

        val callback = FakeImageCaptureCallback(capturesCount = 1)

        isReadyToCaptureImage = true
        imageCapture.takePicture(Dispatchers.Main.asExecutor(), callback)

        // Wait for the signal that the image has been captured.
        callback.awaitCapturesAndAssert(
            capturedImagesCount = 1,
            assertCaptureCount = assertCaptureCount
        )
    }

    private fun getSurfaceProvider(): Preview.SurfaceProvider {
        return SurfaceTextureProvider.createSurfaceTextureProvider(
            object : SurfaceTextureProvider.SurfaceTextureCallback {
                override fun onSurfaceTextureReady(
                    surfaceTexture: SurfaceTexture,
                    resolution: Size
                ) {
                    // No-op
                }

                override fun onSafeToRelease(surfaceTexture: SurfaceTexture) {
                    surfaceTexture.release()
                }
            }
        )
    }

    private fun isFlashTestSupported(
        camera: Camera,
        @ImageCapture.FlashMode flashMode: Int
    ): Boolean {
        when (flashMode) {
            ImageCapture.FLASH_MODE_AUTO -> {
                val cameraInfo: CameraInfo = camera.cameraInfo
                if (cameraInfo is CameraInfoInternal) {
                    val deviceQuirks = DeviceQuirks.getAll()
                    val cameraQuirks = cameraInfo.cameraQuirks
                    if (
                        deviceQuirks.contains(
                            CrashWhenTakingPhotoWithAutoFlashAEModeQuirk::class.java
                        ) ||
                            cameraQuirks.contains(ImageCaptureFailWithAutoFlashQuirk::class.java) ||
                            cameraQuirks.contains(ImageCaptureFlashNotFireQuirk::class.java)
                    ) {
                        return false
                    }
                }
            }
            else -> {}
        }
        return true
    }

    private class FakeImageCaptureCallback(capturesCount: Int) :
        ImageCapture.OnImageCapturedCallback() {

        private val latch = CountDownLatch(capturesCount)
        val errors = mutableListOf<ImageCaptureException>()
        private var numImages = 0

        override fun onCaptureSuccess(image: ImageProxy) {
            numImages++
            image.close()
            latch.countDown()
        }

        override fun onError(exception: ImageCaptureException) {
            errors.add(exception)
            latch.countDown()
        }

        fun awaitCapturesAndAssert(
            timeout: Long = CAPTURE_TIMEOUT,
            capturedImagesCount: Int = 0,
            errorsCount: Int = 0,
            assertCaptureCount: Boolean = true
        ) {
            latch.await(timeout, TimeUnit.MILLISECONDS)

            if (assertCaptureCount) {
                Truth.assertThat(numImages).isEqualTo(capturedImagesCount)
            } else {
                assumeThat(
                    "$numImages image(s) captured within $timeout MS",
                    numImages,
                    equalTo(capturedImagesCount)
                )
            }

            Truth.assertThat(errors.size).isEqualTo(errorsCount)
        }
    }
}
