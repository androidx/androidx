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

package androidx.camera.integration.core

import android.content.Context
import android.util.Log
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider
import androidx.camera.testing.impl.WakelockEmptyActivityRule
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.fakes.FakeOnImageCapturedCallback
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private const val TAG = "ZslDeviceTest"

/** Tests ZSL capture on real devices. */
@LargeTest
@RunWith(Parameterized::class)
class ZslDeviceTest(
    private val testName: String,
    private val cameraSelector: CameraSelector,
    private val implName: String,
    private val cameraConfig: CameraXConfig,
) {

    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = implName == CameraPipeConfig::class.simpleName)

    @get:Rule
    val cameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(cameraConfig)
        )

    @get:Rule val wakelockEmptyActivityRule = WakelockEmptyActivityRule()

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            mutableListOf<Array<Any?>>().apply {
                CameraUtil.getAvailableCameraSelectors().forEach { selector ->
                    val lens = selector.lensFacing
                    add(
                        arrayOf(
                            "config=${Camera2Config::class.simpleName} lensFacing={$lens}",
                            selector,
                            Camera2Config::class.simpleName,
                            Camera2Config.defaultConfig(),
                        )
                    )
                    add(
                        arrayOf(
                            "config=${CameraPipeConfig::class.simpleName} lensFacing={$lens}",
                            selector,
                            CameraPipeConfig::class.simpleName,
                            CameraPipeConfig.defaultConfig(),
                        )
                    )
                }
            }
    }

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val captureTimeout = 15.seconds
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var fakeLifecycleOwner: FakeLifecycleOwner
    private lateinit var camera: Camera
    private lateinit var previewMonitor: PreviewMonitor
    private lateinit var preview: Preview
    private lateinit var imageCaptureZsl: ImageCapture
    private lateinit var cameraInfo: CameraInfo

    @Before
    fun setup() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(cameraSelector.lensFacing!!))
        ProcessCameraProvider.configureInstance(cameraConfig)
        cameraProvider = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]

        instrumentation.runOnMainSync {
            fakeLifecycleOwner = FakeLifecycleOwner()
            fakeLifecycleOwner.startAndResume()

            camera = cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector)
            cameraInfo = camera.cameraInfo
        }

        previewMonitor = PreviewMonitor()
        preview = initPreview(previewMonitor)
        imageCaptureZsl = initImageCaptureZsl()
    }

    @After
    fun tearDown() {
        if (::cameraProvider.isInitialized) {
            cameraProvider.shutdownAsync()[10, TimeUnit.SECONDS]
        }
    }

    @Test
    fun previewImageCaptureZsl() {
        // Arrange.
        assumeTrue(camera.isUseCasesCombinationSupported(preview, imageCaptureZsl))
        bindUseCases(preview, imageCaptureZsl)

        // Capture images with ZSL and verify each capture.
        for (i in 0 until 10) {
            previewMonitor.waitForStream()
            imageCaptureZsl.waitForCapturing()
            Log.d(TAG, "Test ZSL capture round: $i")
            // Assert. Verifies the preview is still outputting after capture
            previewMonitor.waitForStream()
        }
    }

    @Test
    fun imageCaptureZsl() = runBlocking {
        // Arrange.
        bindUseCases(imageCaptureZsl)
        val numImages = 10
        val callback = FakeOnImageCapturedCallback(captureCount = numImages)

        // Act. Capture images with ZSL.
        for (i in 0 until numImages) {
            imageCaptureZsl.takePicture(Dispatchers.IO.asExecutor(), callback)
        }

        // Assert. Verify captures.
        callback.awaitCapturesAndAssert(
            timeout = captureTimeout.times(numImages),
            capturedImagesCount = numImages,
        )
    }

    @Test
    fun bindUnbindImageCaptureZsl() = runBlocking {
        val imageCapture = ImageCapture.Builder().build()
        val numImages = 3

        for (i in 0 until 5) {
            Log.d(TAG, "Loop $i ZSL capture")
            bindUseCases(imageCaptureZsl)
            imageCaptureZsl.verifyCaptures(numImages) // Act & Assert. Verify ZSL captures.
            unbindUseCases(imageCaptureZsl)
            Log.d(TAG, "Loop $i ZSL capture done")

            Log.d(TAG, "Loop $i regular capture")
            bindUseCases(imageCapture)
            imageCapture.verifyCaptures(numImages) // Act & Assert. Verify regular captures.
            unbindUseCases(imageCapture)
            Log.d(TAG, "Loop $i regular capture done")
        }
    }

    private suspend fun ImageCapture.verifyCaptures(numImages: Int) {
        val callback = FakeOnImageCapturedCallback(captureCount = numImages)

        for (i in 0 until numImages) {
            Log.d(TAG, "Test ZSL capture round: $i")
            takePicture(Dispatchers.IO.asExecutor(), callback)
        }

        // Assert. Verify captures.
        callback.awaitCapturesAndAssert(
            timeout = captureTimeout.times(numImages),
            capturedImagesCount = numImages,
        )
    }

    private fun initPreview(monitor: PreviewMonitor, setSurfaceProvider: Boolean = true): Preview {
        return Preview.Builder().setTargetName("Preview").build().apply {
            if (setSurfaceProvider) {
                instrumentation.runOnMainSync { surfaceProvider = monitor.getSurfaceProvider() }
            }
        }
    }

    private fun initImageCaptureZsl(): ImageCapture {
        return ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG)
            .build()
    }

    private fun ImageCapture.waitForCapturing(timeMillis: Long = 10000) {
        val callback =
            object : ImageCapture.OnImageCapturedCallback() {
                val latch = CountDownLatch(1)
                val errors = mutableListOf<ImageCaptureException>()

                override fun onCaptureSuccess(image: ImageProxy) {
                    image.close()
                    latch.countDown()
                }

                override fun onError(exception: ImageCaptureException) {
                    errors.add(exception)
                    latch.countDown()
                }
            }

        takePicture(Dispatchers.IO.asExecutor(), callback)

        assertThat(
                callback.latch.await(timeMillis, TimeUnit.MILLISECONDS) && callback.errors.isEmpty()
            )
            .isTrue()
    }

    class PreviewMonitor {
        private var countDown: CountDownLatch? = null
        private val surfaceProvider = createAutoDrainingSurfaceTextureProvider {
            countDown?.countDown()
        }

        fun getSurfaceProvider(): Preview.SurfaceProvider = surfaceProvider

        fun waitForStream(count: Int = 10, timeMillis: Long = TimeUnit.SECONDS.toMillis(8)) {
            Truth.assertWithMessage("Preview doesn't start")
                .that(
                    synchronized(this) {
                            countDown = CountDownLatch(count)
                            countDown
                        }!!
                        .await(timeMillis, TimeUnit.MILLISECONDS)
                )
                .isTrue()
        }
    }

    private fun bindUseCases(vararg useCases: UseCase) {
        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, *useCases)
        }
    }

    private fun unbindUseCases(vararg useCases: UseCase) {
        instrumentation.runOnMainSync { cameraProvider.unbind(*useCases) }
    }
}
