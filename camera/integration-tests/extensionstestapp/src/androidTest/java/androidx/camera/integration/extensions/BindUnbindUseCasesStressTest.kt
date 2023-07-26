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
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil.VERIFICATION_TARGET_IMAGE_ANALYSIS
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil.VERIFICATION_TARGET_IMAGE_CAPTURE
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil.VERIFICATION_TARGET_PREVIEW
import androidx.camera.integration.extensions.utils.CameraIdExtensionModePair
import androidx.camera.integration.extensions.utils.CameraSelectorUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.GLUtil
import androidx.camera.testing.impl.StressTestRule
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
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
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private const val INVALID_TEX_ID = -1
private var texId = INVALID_TEX_ID

@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class BindUnbindUseCasesStressTest(private val config: CameraIdExtensionModePair) {
    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(
        PreTestCameraIdList(Camera2Config.defaultConfig())
    )

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var extensionsManager: ExtensionsManager
    private lateinit var camera: Camera
    private lateinit var baseCameraSelector: CameraSelector
    private lateinit var extensionCameraSelector: CameraSelector
    private lateinit var preview: Preview
    private lateinit var imageCapture: ImageCapture
    private lateinit var lifecycleOwner: FakeLifecycleOwner

    @Before
    fun setUp(): Unit = runBlocking {
        assumeTrue(CameraXExtensionsTestUtil.isTargetDeviceAvailableForExtensions())
        cameraProvider = ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]
        extensionsManager = ExtensionsManager.getInstanceAsync(
            context,
            cameraProvider
        )[10000, TimeUnit.MILLISECONDS]

        val (cameraId, extensionMode) = config
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
    }

    @After
    fun cleanUp(): Unit = runBlocking {
        if (::cameraProvider.isInitialized) {
            withContext(Dispatchers.Main) {
                cameraProvider.shutdown()[10000, TimeUnit.MILLISECONDS]
            }
        }

        if (::extensionsManager.isInitialized) {
            extensionsManager.shutdown()[10000, TimeUnit.MILLISECONDS]
        }
    }

    companion object {
        @ClassRule
        @JvmField val stressTest = StressTestRule()

        @JvmStatic
        @get:Parameterized.Parameters(name = "config = {0}")
        val parameters: Collection<CameraIdExtensionModePair>
            get() = CameraXExtensionsTestUtil.getAllCameraIdExtensionModeCombinations()
    }

    @Test
    fun bindUnbindUseCases_checkPreviewInEachTime_withPreviewImageCapture(): Unit = runBlocking {
        bindUseCases_checkOutput_thenUnbindAll_repeatedly(
            preview,
            imageCapture,
            verificationTarget = VERIFICATION_TARGET_PREVIEW
        )
    }

    @Test
    fun bindUnbindUseCases_checkImageCaptureInEachTime_withPreviewImageCapture(): Unit =
        runBlocking {
            bindUseCases_checkOutput_thenUnbindAll_repeatedly(
                preview,
                imageCapture,
                verificationTarget = VERIFICATION_TARGET_IMAGE_CAPTURE
            )
        }

    @Test
    fun bindUnbindUseCases_checkPreviewInEachTime_withPreviewImageCaptureImageAnalysis():
        Unit = runBlocking {
        val imageAnalysis = createImageAnalysis()
        assumeTrue(camera.isUseCasesCombinationSupported(preview, imageCapture, imageAnalysis))
        bindUseCases_checkOutput_thenUnbindAll_repeatedly(
            preview,
            imageCapture,
            imageAnalysis,
            verificationTarget = VERIFICATION_TARGET_PREVIEW
        )
    }

    @Test
    fun bindUnbindUseCases_checkImageCaptureInEachTime_withPreviewImageCaptureImageAnalysis():
        Unit = runBlocking {
        val imageAnalysis = createImageAnalysis()
        assumeTrue(camera.isUseCasesCombinationSupported(preview, imageCapture, imageAnalysis))
        bindUseCases_checkOutput_thenUnbindAll_repeatedly(
            preview,
            imageCapture,
            imageAnalysis,
            verificationTarget = VERIFICATION_TARGET_IMAGE_CAPTURE
        )
    }

    @Test
    fun bindUnbindUseCases_checkImageAnalysisInEachTime_withPreviewImageCaptureImageAnalysis():
        Unit = runBlocking {
        val imageAnalysis = createImageAnalysis()
        assumeTrue(camera.isUseCasesCombinationSupported(preview, imageCapture, imageAnalysis))
        bindUseCases_checkOutput_thenUnbindAll_repeatedly(
            preview,
            imageCapture,
            imageAnalysis,
            verificationTarget = VERIFICATION_TARGET_IMAGE_ANALYSIS
        )
    }

    /**
     * Repeatedly binds use cases, checks the input use cases' capture functions can work well, and
     * unbind all use cases.
     *
     * <p>This function checks the nullability of the input ImageAnalysis to determine whether it
     * will be bound together to run the test.
     */
    private fun bindUseCases_checkOutput_thenUnbindAll_repeatedly(
        preview: Preview,
        imageCapture: ImageCapture,
        imageAnalysis: ImageAnalysis? = null,
        verificationTarget: Int,
        repeatCount: Int = CameraXExtensionsTestUtil.getStressTestRepeatingCount()
    ): Unit = runBlocking {
        for (i in 1..repeatCount) {
            // Arrange.
            // Sets up Preview frame available monitor
            val previewFrameAvailableMonitor = PreviewFrameAvailableMonitor()

            // Act: binds use cases
            withContext(Dispatchers.Main) {
                preview.setSurfaceProvider(
                    SurfaceTextureProvider.createSurfaceTextureProvider(
                        previewFrameAvailableMonitor.createSurfaceTextureCallback()
                    )
                )

                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    extensionCameraSelector,
                    *listOfNotNull(preview, imageCapture, imageAnalysis).toTypedArray()
                )
            }

            // Assert: checks that Preview frames can be received
            if (verificationTarget.and(VERIFICATION_TARGET_PREVIEW) != 0) {
                previewFrameAvailableMonitor.awaitSurfaceTextureReadyAndAssert()
                previewFrameAvailableMonitor.awaitAvailableFramesAndAssert()
            }

            if (verificationTarget.and(VERIFICATION_TARGET_IMAGE_CAPTURE) != 0) {
                val imageCaptureCaptureSuccessMonitor = ImageCaptureCaptureSuccessMonitor()
                imageCapture.takePicture(
                    Executors.newSingleThreadExecutor(),
                    imageCaptureCaptureSuccessMonitor.createCaptureCallback()
                )

                // Assert: checks that the captured image of ImageCapture can be received
                imageCaptureCaptureSuccessMonitor.awaitCaptureSuccessAndAssert()
            }

            // Assert: checks that images can be received by the ImageAnalysis.Analyzer
            if (verificationTarget.and(VERIFICATION_TARGET_IMAGE_ANALYSIS) != 0) {
                imageAnalysis!!.let {
                    val analyzerFrameAvailableMonitor = ImageAnalysisImageAvailableMonitor()
                    it.setAnalyzer(
                        Executors.newSingleThreadExecutor(),
                        analyzerFrameAvailableMonitor.createAnalyzer()
                    )
                }
            }

            // Clean it up.
            withContext(Dispatchers.Main) {
                cameraProvider.unbindAll()
            }
        }
    }

    @Test
    fun checkPreview_afterBindUnbindUseCasesRepeatedly_withPreviewImageCapture(): Unit =
        runBlocking {
            bindUseCases_unbindAll_repeatedly_thenCheckOutput(
                preview,
                imageCapture,
                verificationTarget = VERIFICATION_TARGET_PREVIEW
            )
        }

    @Test
    fun checkImageCapture_afterBindUnbindUseCasesRepeatedly_withPreviewImageCapture(): Unit =
        runBlocking {
            bindUseCases_unbindAll_repeatedly_thenCheckOutput(
                preview,
                imageCapture,
                verificationTarget = VERIFICATION_TARGET_IMAGE_CAPTURE
            )
        }

    @Test
    fun checkPreview_afterBindUnbindUseCasesRepeatedly_withPreviewImageCaptureImageAnalysis():
        Unit = runBlocking {
        val imageAnalysis = createImageAnalysis()
        assumeTrue(camera.isUseCasesCombinationSupported(preview, imageCapture, imageAnalysis))
        bindUseCases_unbindAll_repeatedly_thenCheckOutput(
            preview,
            imageCapture,
            imageAnalysis,
            verificationTarget = VERIFICATION_TARGET_PREVIEW
        )
    }

    @Test
    fun checkImageCapture_afterBindUnbindUseCasesRepeatedly_withPreviewImageCaptureImageAnalysis():
        Unit = runBlocking {
        val imageAnalysis = createImageAnalysis()
        assumeTrue(camera.isUseCasesCombinationSupported(preview, imageCapture, imageAnalysis))
        bindUseCases_unbindAll_repeatedly_thenCheckOutput(
            preview,
            imageCapture,
            imageAnalysis,
            verificationTarget = VERIFICATION_TARGET_IMAGE_CAPTURE
        )
    }

    @Test
    fun checkImageAnalysis_afterBindUnbindUseCasesRepeatedly_withPreviewImageCaptureImageAnalysis():
        Unit = runBlocking {
        val imageAnalysis = createImageAnalysis()
        assumeTrue(camera.isUseCasesCombinationSupported(preview, imageCapture, imageAnalysis))
        bindUseCases_unbindAll_repeatedly_thenCheckOutput(
            preview,
            imageCapture,
            imageAnalysis,
            verificationTarget = VERIFICATION_TARGET_IMAGE_ANALYSIS
        )
    }

    /**
     * Repeatedly binds use cases and unbind all, then checks the input use cases' capture
     * functions can work well.
     *
     * <p>This function checks the nullability of the input ImageAnalysis to determine whether it
     * will be bound together to run the test.
     */
    private fun bindUseCases_unbindAll_repeatedly_thenCheckOutput(
        preview: Preview,
        imageCapture: ImageCapture,
        imageAnalysis: ImageAnalysis? = null,
        verificationTarget: Int,
        repeatCount: Int = CameraXExtensionsTestUtil.getStressTestRepeatingCount()
    ): Unit = runBlocking {
        lateinit var previewFrameAvailableMonitor: PreviewFrameAvailableMonitor

        for (i in 1..repeatCount) {
            // Arrange.
            // Sets up Preview frame available monitor
            previewFrameAvailableMonitor = PreviewFrameAvailableMonitor()

            // Act: binds use cases
            withContext(Dispatchers.Main) {
                preview.setSurfaceProvider(
                    SurfaceTextureProvider.createSurfaceTextureProvider(
                        previewFrameAvailableMonitor.createSurfaceTextureCallback()
                    )
                )

                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    extensionCameraSelector,
                    *listOfNotNull(preview, imageCapture, imageAnalysis).toTypedArray()
                )

                // Clean it up: do not unbind at the last time
                if (i != repeatCount) {
                    cameraProvider.unbindAll()
                }
            }
        }

        // Assert: checks that Preview frames can be received
        if (verificationTarget.and(VERIFICATION_TARGET_PREVIEW) != 0) {
            previewFrameAvailableMonitor.awaitSurfaceTextureReadyAndAssert()
            previewFrameAvailableMonitor.awaitAvailableFramesAndAssert()
        }

        if (verificationTarget.and(VERIFICATION_TARGET_IMAGE_CAPTURE) != 0) {
            val imageCaptureCaptureSuccessMonitor = ImageCaptureCaptureSuccessMonitor()
            imageCapture.takePicture(
                Executors.newSingleThreadExecutor(),
                imageCaptureCaptureSuccessMonitor.createCaptureCallback()
            )

            // Assert: checks that the captured image of ImageCapture can be received
            imageCaptureCaptureSuccessMonitor.awaitCaptureSuccessAndAssert()
        }

        if (verificationTarget.and(VERIFICATION_TARGET_IMAGE_ANALYSIS) != 0) {
            imageAnalysis!!.let {
                val analyzerFrameAvailableMonitor = ImageAnalysisImageAvailableMonitor()
                it.setAnalyzer(
                    Executors.newSingleThreadExecutor(),
                    analyzerFrameAvailableMonitor.createAnalyzer()
                )

                // Assert: checks that images can be received by the ImageAnalysis.Analyzer
                analyzerFrameAvailableMonitor.awaitAvailableFramesAndAssert()
            }
        }
    }

    private fun createImageAnalysis() =
        ImageAnalysis.Builder().build().also {
            it.setAnalyzer(CameraXExecutors.directExecutor()) { image -> image.close() }
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
                    if (!complete) {
                        it.countDown()
                        if (it.count == 0L) {
                            complete = true
                        }
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
                    if (texId == INVALID_TEX_ID) {
                        texId = GLUtil.getTexIdFromGLContext()
                    }
                    surfaceTexture.attachToGLContext(texId)
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
