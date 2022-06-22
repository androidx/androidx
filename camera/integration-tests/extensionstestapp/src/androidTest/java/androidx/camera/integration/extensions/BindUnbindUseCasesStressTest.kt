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

package androidx.camera.integration.extensions

import android.content.Context
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.integration.extensions.util.ExtensionsTestUtil
import androidx.camera.integration.extensions.util.ExtensionsTestUtil.STRESS_TEST_OPERATION_REPEAT_COUNT
import androidx.camera.integration.extensions.util.ExtensionsTestUtil.STRESS_TEST_REPEAT_COUNT
import androidx.camera.integration.extensions.utils.CameraSelectorUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.GLUtil
import androidx.camera.testing.SurfaceTextureProvider
import androidx.camera.testing.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.RepeatRule
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class BindUnbindUseCasesStressTest(
    private val cameraId: String,
    private val extensionMode: Int
) {
    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(
        PreTestCameraIdList(Camera2Config.defaultConfig())
    )

    @get:Rule
    val repeatRule = RepeatRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var extensionsManager: ExtensionsManager
    private lateinit var camera: Camera
    private lateinit var baseCameraSelector: CameraSelector
    private lateinit var extensionCameraSelector: CameraSelector
    private lateinit var preview: Preview
    private lateinit var imageCapture: ImageCapture
    private lateinit var imageAnalysis: ImageAnalysis
    private var isImageAnalysisSupported = false
    private lateinit var lifecycleOwner: FakeLifecycleOwner

    @Before
    fun setUp(): Unit = runBlocking {
        if (extensionMode != ExtensionMode.NONE) {
            assumeTrue(ExtensionsTestUtil.isTargetDeviceAvailableForExtensions())
        }
        cameraProvider = ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]
        extensionsManager = ExtensionsManager.getInstanceAsync(
            context,
            cameraProvider
        )[10000, TimeUnit.MILLISECONDS]

        baseCameraSelector = CameraSelectorUtil.createCameraSelectorById(cameraId)
        assumeTrue(extensionsManager.isExtensionAvailable(baseCameraSelector, extensionMode))

        extensionCameraSelector = extensionsManager.getExtensionEnabledCameraSelector(
            baseCameraSelector,
            extensionMode
        )

        camera = withContext(Dispatchers.Main) {
            lifecycleOwner = FakeLifecycleOwner()
            lifecycleOwner.startAndResume()
            cameraProvider.bindToLifecycle(lifecycleOwner, extensionCameraSelector)
        }

        preview = Preview.Builder().build()
        imageCapture = ImageCapture.Builder().build()
        imageAnalysis = ImageAnalysis.Builder().build()

        isImageAnalysisSupported =
            camera.isUseCasesCombinationSupported(preview, imageCapture, imageAnalysis)
    }

    @After
    fun cleanUp(): Unit = runBlocking {
        if (::cameraProvider.isInitialized) {
            withContext(Dispatchers.Main) {
                cameraProvider.unbindAll()
                cameraProvider.shutdown()[10000, TimeUnit.MILLISECONDS]
            }
        }

        if (::extensionsManager.isInitialized) {
            extensionsManager.shutdown()[10000, TimeUnit.MILLISECONDS]
        }
    }

    companion object {
        @JvmStatic
        @get:Parameterized.Parameters(name = "cameraId = {0}, extensionMode = {1}")
        val parameters: Collection<Array<Any>>
            get() = ExtensionsTestUtil.getAllCameraIdModeCombinations()
    }

    @Test
    @RepeatRule.Repeat(times = STRESS_TEST_REPEAT_COUNT)
    fun bindUnbindUseCasesTenTimes_canCaptureImageInEachTime(): Unit = runBlocking {
        for (i in 1..STRESS_TEST_OPERATION_REPEAT_COUNT) {
            val previewFrameAvailableMonitor = PreviewFrameAvailableMonitor()
            val imageCaptureCaptureSuccessMonitor = ImageCaptureCaptureSuccessMonitor()
            var analyzerFrameAvailableMonitor: ImageAnalysisImageAvailableMonitor? = null

            withContext(Dispatchers.Main) {
                preview.setSurfaceProvider(
                    SurfaceTextureProvider.createSurfaceTextureProvider(
                        previewFrameAvailableMonitor.createSurfaceTextureCallback()
                    )
                )

                if (isImageAnalysisSupported) {
                    analyzerFrameAvailableMonitor = ImageAnalysisImageAvailableMonitor()
                    imageAnalysis.setAnalyzer(
                        Executors.newSingleThreadExecutor(),
                        analyzerFrameAvailableMonitor!!.createAnalyzer()
                    )

                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        extensionCameraSelector,
                        preview,
                        imageCapture,
                        imageAnalysis
                    )
                } else {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        extensionCameraSelector,
                        preview,
                        imageCapture
                    )
                }
            }

            previewFrameAvailableMonitor.awaitSurfaceTextureReadyAndAssert()
            previewFrameAvailableMonitor.awaitAvailableFramesAndAssert()

            imageCapture.takePicture(
                Executors.newSingleThreadExecutor(),
                imageCaptureCaptureSuccessMonitor.createCaptureCallback()
            )

            imageCaptureCaptureSuccessMonitor.awaitCaptureSuccessAndAssert()

            analyzerFrameAvailableMonitor?.awaitAvailableFramesAndAssert()

            withContext(Dispatchers.Main) {
                cameraProvider.unbindAll()
            }
        }
    }

    @Test
    @RepeatRule.Repeat(times = STRESS_TEST_REPEAT_COUNT)
    fun canCaptureImage_afterBindUnbindUseCasesTenTimes(): Unit = runBlocking {
        lateinit var previewFrameAvailableMonitor: PreviewFrameAvailableMonitor
        lateinit var imageCaptureCaptureSuccessMonitor: ImageCaptureCaptureSuccessMonitor
        var analyzerFrameAvailableMonitor: ImageAnalysisImageAvailableMonitor? = null

        for (i in 1..STRESS_TEST_OPERATION_REPEAT_COUNT) {
            previewFrameAvailableMonitor = PreviewFrameAvailableMonitor()
            imageCaptureCaptureSuccessMonitor = ImageCaptureCaptureSuccessMonitor()

            withContext(Dispatchers.Main) {
                preview.setSurfaceProvider(
                    SurfaceTextureProvider.createSurfaceTextureProvider(
                        previewFrameAvailableMonitor.createSurfaceTextureCallback()
                    )
                )

                if (isImageAnalysisSupported) {
                    analyzerFrameAvailableMonitor = ImageAnalysisImageAvailableMonitor()
                    imageAnalysis.setAnalyzer(
                        Executors.newSingleThreadExecutor(),
                        analyzerFrameAvailableMonitor!!.createAnalyzer()
                    )

                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        extensionCameraSelector,
                        preview,
                        imageCapture,
                        imageAnalysis
                    )
                } else {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        extensionCameraSelector,
                        preview,
                        imageCapture
                    )
                }
            }

            withContext(Dispatchers.Main) {
                if (i != STRESS_TEST_OPERATION_REPEAT_COUNT) {
                    cameraProvider.unbindAll()
                }
            }
        }

        previewFrameAvailableMonitor.awaitSurfaceTextureReadyAndAssert()
        previewFrameAvailableMonitor.awaitAvailableFramesAndAssert()

        imageCapture.takePicture(
            Executors.newSingleThreadExecutor(),
            imageCaptureCaptureSuccessMonitor.createCaptureCallback()
        )

        imageCaptureCaptureSuccessMonitor.awaitCaptureSuccessAndAssert()

        analyzerFrameAvailableMonitor?.awaitAvailableFramesAndAssert()
    }

    private class PreviewFrameAvailableMonitor {
        private var isSurfaceTextureReleased = false
        private val isSurfaceTextureReleasedLock = Any()

        private var surfaceTextureLatch = CountDownLatch(1)
        private var previewFrameCountDownLatch: CountDownLatch? = null

        private val onFrameAvailableListener = object : SurfaceTexture.OnFrameAvailableListener {
            private var complete = false

            override fun onFrameAvailable(surfaceTexture: SurfaceTexture): Unit = runBlocking {
                if (complete) {
                    return@runBlocking
                }

                withContext(Dispatchers.Main) {
                    synchronized(isSurfaceTextureReleasedLock) {
                        if (!isSurfaceTextureReleased) {
                            surfaceTexture.updateTexImage()
                        }
                    }
                }

                previewFrameCountDownLatch?.let {
                    it.countDown()
                    if (it.count == 0L) {
                        complete = true
                    }
                }
            }
        }

        private val frameAvailableHandler: Handler
        private val frameAvailableHandlerThread = HandlerThread("FrameAvailable").also {
            it.start()
            frameAvailableHandler = Handler(it.looper)
        }

        fun createSurfaceTextureCallback(): SurfaceTextureProvider.SurfaceTextureCallback =
            object : SurfaceTextureProvider.SurfaceTextureCallback {
                override fun onSurfaceTextureReady(
                    surfaceTexture: SurfaceTexture,
                    resolution: Size
                ) {
                    surfaceTexture.attachToGLContext(GLUtil.getTexIdFromGLContext())
                    surfaceTexture.setOnFrameAvailableListener(
                        onFrameAvailableListener,
                        frameAvailableHandler
                    )

                    surfaceTextureLatch.countDown()
                }

                override fun onSafeToRelease(surfaceTexture: SurfaceTexture) {
                    synchronized(isSurfaceTextureReleasedLock) {
                        isSurfaceTextureReleased = true
                        surfaceTexture.release()
                        frameAvailableHandlerThread.quitSafely()
                    }
                }
            }

        fun awaitSurfaceTextureReadyAndAssert(timeoutDurationMs: Long = 1000) {
            assertThat(surfaceTextureLatch.await(timeoutDurationMs, TimeUnit.MILLISECONDS)).isTrue()
        }

        fun awaitAvailableFramesAndAssert(count: Int = 10, timeoutDurationMs: Long = 3000) {
            previewFrameCountDownLatch = CountDownLatch(count)
            assertThat(
                previewFrameCountDownLatch!!.await(
                    timeoutDurationMs,
                    TimeUnit.MILLISECONDS
                )
            ).isTrue()
        }
    }

    private class ImageCaptureCaptureSuccessMonitor {
        private val captureSuccessCountDownLatch = CountDownLatch(1)

        fun createCaptureCallback() = object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                image.close()
                captureSuccessCountDownLatch.countDown()
            }
        }

        fun awaitCaptureSuccessAndAssert(timeoutDurationMs: Long = 10000) {
            assertThat(
                captureSuccessCountDownLatch.await(
                    timeoutDurationMs,
                    TimeUnit.MILLISECONDS
                )
            ).isTrue()
        }
    }

    private class ImageAnalysisImageAvailableMonitor {
        private var analyzerFrameCountDownLatch: CountDownLatch? = null

        fun createAnalyzer() = ImageAnalysis.Analyzer { image ->
            image.close()
            analyzerFrameCountDownLatch?.countDown()
        }

        fun awaitAvailableFramesAndAssert(count: Int = 10, timeoutDurationMs: Long = 3000) {
            analyzerFrameCountDownLatch = CountDownLatch(count)
            assertThat(
                analyzerFrameCountDownLatch!!.await(
                    timeoutDurationMs,
                    TimeUnit.MILLISECONDS
                )
            ).isTrue()
        }
    }
}
