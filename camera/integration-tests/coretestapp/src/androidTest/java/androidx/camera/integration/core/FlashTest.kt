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
import android.util.Size
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.LabTestRule
import androidx.camera.testing.SurfaceTextureProvider
import androidx.camera.testing.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private val BACK_SELECTOR = CameraSelector.DEFAULT_BACK_CAMERA
private const val BACK_LENS_FACING = CameraSelector.LENS_FACING_BACK
private const val CAPTURE_TIMEOUT = 10_000.toLong() //  10 seconds

@LargeTest
@RunWith(Parameterized::class)
class FlashTest(private val implName: String, private val cameraXConfig: CameraXConfig) {

    @get:Rule
    val cameraRule = CameraUtil.grantCameraPermissionAndPreTest()

    @get:Rule
    val labTest: LabTestRule = LabTestRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
            arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig())
        )
    }

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var cameraProvider: ProcessCameraProvider

    @Before
    fun setUp() {
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(BACK_LENS_FACING))
        ProcessCameraProvider.configureInstance(cameraXConfig)
        cameraProvider = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]
    }

    @After
    fun tearDown(): Unit = runBlocking {
        if (::cameraProvider.isInitialized) {
            withContext(Dispatchers.Main) {
                cameraProvider.unbindAll()
                cameraProvider.shutdown()[10, TimeUnit.SECONDS]
            }
        }
    }

    @LabTestRule.LabTestRearCamera
    @Test
    fun canCaptureWithFlashOn() {
        skipTestOnCameraPipeConfig()
        canTakePicture(
            flashMode = ImageCapture.FLASH_MODE_ON,
            captureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
        )
    }

    @LabTestRule.LabTestRearCamera
    @Test
    fun canCaptureWithFlashAuto() {
        skipTestOnCameraPipeConfig()
        canTakePicture(
            flashMode = ImageCapture.FLASH_MODE_AUTO,
            captureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
        )
    }

    // Camera gets stuck when taking pictures with flash ON or AUTO in dark environment. See
    // b/193336562 for details. The test simulates taking photo in a dark environment by
    // allocating the devices that the rear camera is blocked. It needs to use the annotation
    // @LabTestRule.LabTestFrontCamera and run the test with the rear camera.
    @LabTestRule.LabTestFrontCamera
    @Test
    fun canCaptureWithFlashOnInDarkEnvironment() {
        skipTestOnCameraPipeConfig()
        canTakePicture(
            flashMode = ImageCapture.FLASH_MODE_ON,
            captureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
        )
    }

    @LabTestRule.LabTestFrontCamera
    @Test
    fun canCaptureWithFlashAutoInDarkEnvironment() {
        skipTestOnCameraPipeConfig()
        canTakePicture(
            flashMode = ImageCapture.FLASH_MODE_AUTO,
            captureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
        )
    }

    // Camera gets stuck when taking maximum quality mode picture with flash ON or AUTO in dark
    // environment. See b/194046401 for details. The test simulates taking photo in a dark
    // environment by allocating the devices that the rear camera is blocked. It needs to use the
    // annotation @LabTestRule.LabTestFrontCamera and run the test with the rear camera.
    @LabTestRule.LabTestFrontCamera
    @Test
    fun canCaptureMaxQualityPhoto_withFlashOn_inDarkEnvironment() {
        skipTestOnCameraPipeConfig()
        canTakePicture(
            flashMode = ImageCapture.FLASH_MODE_ON,
            captureMode = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
        )
    }

    @LabTestRule.LabTestFrontCamera
    @Test
    fun canCaptureMaxQualityPhoto_withFlashAuto_inDarkEnvironment() {
        skipTestOnCameraPipeConfig()
        canTakePicture(
            flashMode = ImageCapture.FLASH_MODE_AUTO,
            captureMode = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
        )
    }

    private fun canTakePicture(flashMode: Int, captureMode: Int) = runBlocking {
        val imageCapture = ImageCapture.Builder()
            .setFlashMode(flashMode)
            .setCaptureMode(captureMode)
            .build()

        val preview = Preview.Builder().build()

        withContext(Dispatchers.Main) {
            preview.setSurfaceProvider(getSurfaceProvider())
        }

        var fakeLifecycleOwner = FakeLifecycleOwner()
        fakeLifecycleOwner.startAndResume()
        cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, imageCapture, preview)

        // Take picture after preview is ready for a while. It can cause issue on some devices when
        // flash is on.
        delay(2_000)

        val callback = FakeImageCaptureCallback(capturesCount = 1)
        imageCapture.takePicture(Dispatchers.Main.asExecutor(), callback)

        // Wait for the signal that the image has been captured.
        callback.awaitCapturesAndAssert(capturedImagesCount = 1)
    }

    // TODO(b/185913412): Remove when setting the flash mode is added to Camera-pipe-integration
    private fun skipTestOnCameraPipeConfig() {
        Assume.assumeFalse(
            "Setting the flash mode isn't supported on Camera-pipe-integration (b/185913412)",
            implName == CameraPipeConfig::class.simpleName
        )
    }

    private fun getSurfaceProvider(): Preview.SurfaceProvider {
        return SurfaceTextureProvider.createSurfaceTextureProvider(object :
                SurfaceTextureProvider.SurfaceTextureCallback {
                override fun onSurfaceTextureReady(
                    surfaceTexture: SurfaceTexture,
                    resolution: Size
                ) {
                    // No-op
                }

                override fun onSafeToRelease(surfaceTexture: SurfaceTexture) {
                    surfaceTexture.release()
                }
            })
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
            errorsCount: Int = 0
        ) {
            latch.await(timeout, TimeUnit.MILLISECONDS)
            Truth.assertThat(numImages).isEqualTo(capturedImagesCount)
            Truth.assertThat(errors.size).isEqualTo(errorsCount)
        }
    }
}