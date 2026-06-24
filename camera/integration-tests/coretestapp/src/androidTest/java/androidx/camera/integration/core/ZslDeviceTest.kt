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
import androidx.camera.core.Camera
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.DynamicRange
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.processing.DefaultSurfaceProcessor
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.AndroidUtil
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.GLUtil
import androidx.camera.testing.impl.SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider
import androidx.camera.testing.impl.WakelockEmptyActivityRule
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.fakes.FakeOnImageCapturedCallback
import androidx.camera.testing.impl.fakes.FakeSurfaceEffect
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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
    private var tempVideoFile: File? = null

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
        tempVideoFile?.let {
            if (it.exists()) {
                it.delete()
            }
        }
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

    @Test
    fun zslFallback_whenVideoCaptureBound() = runBlocking {
        assumeTrue("ZSL is not supported on this device sensor.", cameraInfo.isZslSupported)

        // Reset lifecycle for clean binding of multiple use-cases
        withContext(Dispatchers.Main) { cameraProvider.unbindAll() }

        // Configure JCA-like VideoCapture
        val recorder = Recorder.Builder().build()
        val videoCapture = VideoCapture.Builder(recorder).build()

        assumeTrue(camera.isUseCasesCombinationSupported(preview, imageCaptureZsl, videoCapture))

        // Bind the standard JCA use case group (Preview + ZSL Image + Video)
        bindUseCases(preview, imageCaptureZsl, videoCapture)

        // Verify bind succeeds
        assertThat(cameraProvider.isBound(preview)).isTrue()
        assertThat(cameraProvider.isBound(imageCaptureZsl)).isTrue()
        assertThat(cameraProvider.isBound(videoCapture)).isTrue()

        val executor = Executors.newSingleThreadExecutor()

        try {
            // Capture multiple images with ZSL and verify the preview continues to stream/function
            // concurrently
            for (i in 0 until 2) {
                previewMonitor.waitForStream()
                Log.d(TAG, "JCA ZSL Capture pre round successful: $i")
                imageCaptureZsl.verifyCaptures(3)
                Log.d(TAG, "JCA ZSL Capture round successful: $i")
                previewMonitor.waitForStream()

                try {
                    tempVideoFile = File.createTempFile("zsl_fallback_test", ".mp4")
                    val options = FileOutputOptions.Builder(tempVideoFile!!).build()
                    val startLatch = CountDownLatch(1)
                    val statusLatch = CountDownLatch(5) // Ensure active recording is ongoing
                    val finalizeLatch = CountDownLatch(1)

                    val recording =
                        videoCapture.output.prepareRecording(context, options).start(executor) {
                            event ->
                            when (event) {
                                is VideoRecordEvent.Start -> startLatch.countDown()
                                is VideoRecordEvent.Status -> statusLatch.countDown()
                                is VideoRecordEvent.Finalize -> {
                                    assertThat(event.hasError()).isFalse()
                                    finalizeLatch.countDown()
                                }
                            }
                        }

                    // Verify successful start and active status events
                    assertThat(startLatch.await(10, TimeUnit.SECONDS)).isTrue()
                    assertThat(statusLatch.await(15, TimeUnit.SECONDS)).isTrue()

                    // Stop recording
                    recording.stop()

                    // Wait for it to finalize correctly
                    assertThat(finalizeLatch.await(10, TimeUnit.SECONDS)).isTrue()

                    // Assert the file was created and has content
                    assertThat(tempVideoFile!!.length()).isGreaterThan(0L)
                } finally {
                    tempVideoFile?.let {
                        if (it.exists()) {
                            it.delete()
                        }
                    }
                }
                Log.d(TAG, "JCA ZSL Capture + recording round successful: $i")
            }
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun zslFallback_whenVideoCaptureBound_withStreamSharing() = runBlocking {
        assumeTrue("ZSL is not supported on this device sensor.", cameraInfo.isZslSupported)
        assumeEglSupported()

        // Reset lifecycle for clean binding of multiple use-cases
        withContext(Dispatchers.Main) { cameraProvider.unbindAll() }

        // Configure JCA-like VideoCapture
        val recorder = Recorder.Builder().build()
        val videoCapture = VideoCapture.Builder(recorder).build()

        val effect =
            FakeSurfaceEffect(
                CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE,
                DefaultSurfaceProcessor.Factory.newInstance(DynamicRange.SDR),
            )

        val useCaseGroup =
            UseCaseGroup.Builder()
                .addUseCase(preview)
                .addUseCase(imageCaptureZsl)
                .addUseCase(videoCapture)
                .addEffect(effect)
                .build()

        assumeTrue(camera.isUseCasesCombinationSupported(preview, imageCaptureZsl, videoCapture))

        // Bind the standard JCA use case group (Preview + ZSL Image + Video) to force StreamSharing
        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, useCaseGroup)
        }

        // Verify bind succeeds
        assertThat(cameraProvider.isBound(preview)).isTrue()
        assertThat(cameraProvider.isBound(imageCaptureZsl)).isTrue()
        assertThat(cameraProvider.isBound(videoCapture)).isTrue()

        val executor = Executors.newSingleThreadExecutor()

        try {
            // Capture multiple images with ZSL and verify the preview continues to stream/function
            // concurrently
            for (i in 0 until 2) {
                previewMonitor.waitForStream()
                Log.d(TAG, "JCA ZSL Capture pre round successful: $i")
                imageCaptureZsl.verifyCaptures(3)
                Log.d(TAG, "JCA ZSL Capture round successful: $i")
                previewMonitor.waitForStream()

                try {
                    tempVideoFile = File.createTempFile("zsl_fallback_test", ".mp4")
                    val options = FileOutputOptions.Builder(tempVideoFile!!).build()
                    val startLatch = CountDownLatch(1)
                    val statusLatch = CountDownLatch(5) // Ensure active recording is ongoing
                    val finalizeLatch = CountDownLatch(1)

                    val recording =
                        videoCapture.output.prepareRecording(context, options).start(executor) {
                            event ->
                            when (event) {
                                is VideoRecordEvent.Start -> startLatch.countDown()
                                is VideoRecordEvent.Status -> statusLatch.countDown()
                                is VideoRecordEvent.Finalize -> {
                                    assertThat(event.hasError()).isFalse()
                                    finalizeLatch.countDown()
                                }
                            }
                        }

                    // Verify successful start and active status events
                    assertThat(startLatch.await(10, TimeUnit.SECONDS)).isTrue()
                    assertThat(statusLatch.await(15, TimeUnit.SECONDS)).isTrue()

                    // Stop recording
                    recording.stop()

                    // Wait for it to finalize correctly
                    assertThat(finalizeLatch.await(10, TimeUnit.SECONDS)).isTrue()

                    // Assert the file was created and has content
                    assertThat(tempVideoFile!!.length()).isGreaterThan(0L)
                } finally {
                    tempVideoFile?.let {
                        if (it.exists()) {
                            it.delete()
                        }
                    }
                }
                Log.d(TAG, "JCA ZSL Capture + recording round successful: $i")
            }
        } finally {
            executor.shutdownNow()
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

    private fun assumeEglSupported() {
        if (AndroidUtil.isEmulator()) {
            try {
                GLUtil.getTexIdFromGLContext()
            } catch (_: RuntimeException) {
                assumeTrue("Emulator does not support EGL properly for StreamSharing", false)
            }
        }
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
