/*
 * Copyright (C) 2021 The Android Open Source Project
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
import android.graphics.ImageFormat
import android.os.Handler
import android.os.HandlerThread
import android.util.Rational
import android.util.Size
import android.view.Surface
import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ExperimentalUseCaseApi
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.BackpressureStrategy
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.core.impl.ImageOutputConfig
import androidx.camera.core.impl.ImageOutputConfig.OPTION_RESOLUTION_SELECTOR
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.utils.Threads.runOnMainSync
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.internal.utils.SizeUtil
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionFilter
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.integration.core.util.CameraInfoUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.LabTestRule
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.WakelockEmptyActivityRule
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
internal class ImageAnalysisTest(
    private val implName: String,
    private val cameraConfig: CameraXConfig,
) {

    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = implName == CameraPipeConfig::class.simpleName)

    @get:Rule
    val cameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(PreTestCameraIdList(cameraConfig))

    @get:Rule val labTest: LabTestRule = LabTestRule()

    @get:Rule val wakelockEmptyActivityRule = WakelockEmptyActivityRule()

    companion object {
        private val DEFAULT_RESOLUTION = Size(640, 480)

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            listOf(
                arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
                arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig()),
            )
    }

    private val analysisResultLock = Any()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @GuardedBy("analysisResultLock") private val analysisResults = mutableSetOf<ImageProperties>()
    private val analyzer =
        ImageAnalysis.Analyzer { image ->
            synchronized(analysisResultLock) { analysisResults.add(ImageProperties(image)) }
            analysisResultsSemaphore.release()
            image.close()
        }
    private lateinit var analysisResultsSemaphore: Semaphore
    private lateinit var handlerThread: HandlerThread
    private lateinit var handler: Handler
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var fakeLifecycleOwner: FakeLifecycleOwner
    private lateinit var cameraSelector: CameraSelector

    @Before
    fun setUp(): Unit = runBlocking {
        cameraSelector = CameraUtil.assumeFirstAvailableCameraSelector()
        ProcessCameraProvider.configureInstance(cameraConfig)

        cameraProvider = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]
        handlerThread = HandlerThread("AnalysisThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)
        analysisResultsSemaphore = Semaphore(0)

        withContext(Dispatchers.Main) {
            fakeLifecycleOwner = FakeLifecycleOwner()
            fakeLifecycleOwner.startAndResume()
        }
    }

    @After
    fun tearDown(): Unit = runBlocking {
        if (::cameraProvider.isInitialized) {
            withContext(Dispatchers.Main) { cameraProvider.shutdownAsync()[10, TimeUnit.SECONDS] }
        }

        if (::handler.isInitialized) {
            handlerThread.quitSafely()
        }
    }

    @Test
    fun exceedMaxImagesWithoutClosing_doNotCrash() = runBlocking {
        // Arrange.
        val queueDepth = 3
        val semaphore = Semaphore(0)
        val useCase =
            ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_BLOCK_PRODUCER)
                .setImageQueueDepth(queueDepth)
                .build()
        val imageProxyList = mutableListOf<ImageProxy>()
        useCase.setAnalyzer(
            CameraXExecutors.newHandlerExecutor(handler),
            { image ->
                imageProxyList.add(image)
                semaphore.release()
            },
        )

        // Act.
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, useCase)
        }

        // Assert: waiting for images does not crash.
        assertThat(semaphore.tryAcquire(queueDepth + 1, 1, TimeUnit.SECONDS)).isFalse()

        // Clean it up.
        useCase.clearAnalyzer()
        for (image in imageProxyList) {
            image.close()
        }
    }

    @Test
    fun analyzesImages_withKEEP_ONLY_LATEST_whenCameraIsOpen() {
        analyzerAnalyzesImagesWithStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
    }

    @Test
    fun analyzesImages_withBLOCK_PRODUCER_whenCameraIsOpen() {
        analyzerAnalyzesImagesWithStrategy(ImageAnalysis.STRATEGY_BLOCK_PRODUCER)
    }

    private fun analyzerAnalyzesImagesWithStrategy(@BackpressureStrategy strategy: Int) =
        runBlocking {
            val useCase = ImageAnalysis.Builder().setBackpressureStrategy(strategy).build()
            withContext(Dispatchers.Main) {
                cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, useCase)
            }
            useCase.setAnalyzer(CameraXExecutors.newHandlerExecutor(handler), analyzer)
            analysisResultsSemaphore.tryAcquire(5, TimeUnit.SECONDS)
            synchronized(analysisResultLock) { assertThat(analysisResults).isNotEmpty() }
        }

    @LabTestRule.LabTestOnly
    // TODO(b/221321202): flaky on AndroidX test, @LabTestOnly should be removed after resolved.
    @Test
    fun analyzerDoesNotAnalyzeImages_whenCameraIsNotOpen() = runBlocking {
        val useCase = ImageAnalysis.Builder().build()
        // Bind but do not start lifecycle
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, useCase)
            cameraProvider.unbindAll()
        }
        useCase.setAnalyzer(CameraXExecutors.newHandlerExecutor(handler), analyzer)
        // Keep the lifecycle in an inactive state.
        // Wait a little while for frames to be analyzed.
        analysisResultsSemaphore.tryAcquire(5, TimeUnit.SECONDS)

        // No frames should have been analyzed.
        synchronized(analysisResultLock) { assertThat(analysisResults).isEmpty() }
    }

    @Test
    fun canObtainDefaultBackpressureStrategy() {
        val imageAnalysis = ImageAnalysis.Builder().build()
        assertThat(imageAnalysis.backpressureStrategy)
            .isEqualTo(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
    }

    @Test
    @ExperimentalUseCaseApi
    fun canObtainBackgroundExecutor() {
        val ioExecutor = CameraXExecutors.ioExecutor()
        val imageAnalysis = ImageAnalysis.Builder().setBackgroundExecutor(ioExecutor).build()
        val imageAnalysis2 = ImageAnalysis.Builder().build()

        // check return when provided an Executor
        assertThat(imageAnalysis.backgroundExecutor).isSameInstanceAs(ioExecutor)

        // check default return
        assertThat(imageAnalysis2.backgroundExecutor).isNull()
    }

    @Test
    fun canObtainDefaultImageQueueDepth() {
        val imageAnalysis = ImageAnalysis.Builder().build()

        // Should not be less than 1
        assertThat(imageAnalysis.imageQueueDepth).isAtLeast(1)
    }

    @Test
    fun defaultAspectRatioWillBeSet_whenTargetResolutionIsNotSet() = runBlocking {
        val useCase = ImageAnalysis.Builder().build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, useCase)
        }
        val config = useCase.currentConfig as ImageOutputConfig
        assertThat(config.targetAspectRatio).isEqualTo(AspectRatio.RATIO_4_3)
    }

    @Suppress("DEPRECATION") // test for legacy resolution API
    @Test
    fun defaultAspectRatioWillBeSet_whenRatioDefaultIsSet() = runBlocking {
        val useCase =
            ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_DEFAULT).build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, useCase)
        }
        val config = useCase.currentConfig as ImageOutputConfig
        assertThat(config.targetAspectRatio).isEqualTo(AspectRatio.RATIO_4_3)
    }

    @Suppress("DEPRECATION") // legacy resolution API
    @Test
    fun defaultAspectRatioWontBeSet_whenTargetResolutionIsSet() = runBlocking {
        val useCase = ImageAnalysis.Builder().setTargetResolution(DEFAULT_RESOLUTION).build()
        assertThat(
                useCase.currentConfig.containsOption(ImageOutputConfig.OPTION_TARGET_ASPECT_RATIO)
            )
            .isFalse()

        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, useCase)
        }
        assertThat(
                useCase.currentConfig.containsOption(ImageOutputConfig.OPTION_TARGET_ASPECT_RATIO)
            )
            .isFalse()
    }

    @Test
    fun viewPort_OverwriteCropRect(): Unit = runBlocking {
        // Arrange.
        val rotation =
            if (CameraUtil.getSensorOrientation(cameraSelector.lensFacing!!)!! % 180 != 0)
                Surface.ROTATION_90
            else Surface.ROTATION_0
        val imageProxyDeferred = CompletableDeferred<ImageProxy>()
        val imageAnalysis =
            ImageAnalysis.Builder().setTargetRotation(rotation).build().apply {
                setAnalyzer(CameraXExecutors.newHandlerExecutor(handler)) { image ->
                    imageProxyDeferred.complete(image)
                    image.close()
                }
            }
        val viewPort = ViewPort.Builder(Rational(2, 1), imageAnalysis.targetRotation).build()

        // Act.
        withContext(Dispatchers.Main) {
            val useCaseGroup =
                UseCaseGroup.Builder().setViewPort(viewPort).addUseCase(imageAnalysis).build()
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, useCaseGroup)
        }
        val imageProxy = withTimeoutOrNull(5000) { imageProxyDeferred.await() }

        // Assert.
        val aspectRatioThreshold = 0.01
        assertThat(Rational(imageProxy!!.cropRect.width(), imageProxy.cropRect.height()).toDouble())
            .isWithin(aspectRatioThreshold)
            .of(viewPort.aspectRatio.toDouble())
    }

    @Test
    fun targetRotationCanBeUpdatedAfterUseCaseIsCreated() {
        val imageAnalysis = ImageAnalysis.Builder().setTargetRotation(Surface.ROTATION_0).build()
        imageAnalysis.targetRotation = Surface.ROTATION_90
        assertThat(imageAnalysis.targetRotation).isEqualTo(Surface.ROTATION_90)
    }

    @Suppress("DEPRECATION") // test for legacy resolution API
    @Test
    fun targetResolutionIsUpdatedAfterTargetRotationIsUpdated() = runBlocking {
        val imageAnalysis =
            ImageAnalysis.Builder()
                .setTargetResolution(DEFAULT_RESOLUTION)
                .setTargetRotation(Surface.ROTATION_0)
                .build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, imageAnalysis)
        }

        // Updates target rotation from ROTATION_0 to ROTATION_90.
        imageAnalysis.targetRotation = Surface.ROTATION_90
        val newConfig = imageAnalysis.currentConfig as ImageOutputConfig
        val expectedTargetResolution = Size(DEFAULT_RESOLUTION.height, DEFAULT_RESOLUTION.width)

        // Expected targetResolution will be reversed from original target resolution.
        assertThat(newConfig.targetResolution == expectedTargetResolution).isTrue()
    }

    @Test
    fun useCaseConfigCanBeReset_afterUnbind() = runBlocking {
        val useCase = ImageAnalysis.Builder().build()
        val initialConfig = useCase.currentConfig

        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, useCase)
            cameraProvider.unbind(useCase)
        }
        val configAfterUnbinding = useCase.currentConfig
        assertThat(initialConfig == configAfterUnbinding).isTrue()
    }

    @Test
    fun targetRotationIsRetained_whenUseCaseIsReused() = runBlocking {
        // Generally, the device can't be rotated to Surface.ROTATION_180. Therefore,
        // use it to do the test.
        withContext(Dispatchers.Main) {
            val useCase = ImageAnalysis.Builder().build()

            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, useCase)
            useCase.targetRotation = Surface.ROTATION_180

            // Check the target rotation is kept when the use case is unbound.
            cameraProvider.unbind(useCase)
            assertThat(useCase.targetRotation).isEqualTo(Surface.ROTATION_180)

            // Check the target rotation is kept when the use case is rebound to the
            // lifecycle.
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, useCase)
            assertThat(useCase.targetRotation).isEqualTo(Surface.ROTATION_180)
        }
    }

    @Test
    @Throws(InterruptedException::class)
    fun useCaseCanBeReusedInSameCamera() = runBlocking {
        val useCase = ImageAnalysis.Builder().build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, useCase)
        }
        useCase.setAnalyzer(CameraXExecutors.newHandlerExecutor(handler), analyzer)
        assertThat(analysisResultsSemaphore.tryAcquire(5, TimeUnit.SECONDS)).isTrue()
        withContext(Dispatchers.Main) { cameraProvider.unbind(useCase) }
        analysisResultsSemaphore = Semaphore(/* permits= */ 0)
        // Rebind the use case to the same camera.
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, useCase)
        }
        assertThat(analysisResultsSemaphore.tryAcquire(5, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    @Throws(InterruptedException::class)
    fun useCaseCanBeReusedInDifferentCamera() = runBlocking {
        val useCase = ImageAnalysis.Builder().build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, useCase)
        }
        useCase.setAnalyzer(CameraXExecutors.newHandlerExecutor(handler), analyzer)
        assertThat(analysisResultsSemaphore.tryAcquire(5, TimeUnit.SECONDS)).isTrue()
        withContext(Dispatchers.Main) { cameraProvider.unbind(useCase) }
        analysisResultsSemaphore = Semaphore(/* permits= */ 0)
        // Rebind the use case to different camera.
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, useCase)
        }
        assertThat(analysisResultsSemaphore.tryAcquire(5, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun returnValidTargetRotation_afterUseCaseIsCreated() {
        val imageCapture = ImageCapture.Builder().build()
        assertThat(imageCapture.targetRotation).isNotEqualTo(ImageOutputConfig.INVALID_ROTATION)
    }

    @Test
    fun returnCorrectTargetRotation_afterUseCaseIsAttached() = runBlocking {
        val imageAnalysis = ImageAnalysis.Builder().setTargetRotation(Surface.ROTATION_180).build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, imageAnalysis)
        }
        assertThat(imageAnalysis.targetRotation).isEqualTo(Surface.ROTATION_180)
    }

    @Test
    fun analyzerAnalyzesImages_withHighResolutionEnabled() = runBlocking {
        val cameraInfo =
            withContext(Dispatchers.Main) {
                cameraProvider
                    .bindToLifecycle(fakeLifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA)
                    .cameraInfo
            }
        val maxHighResolutionOutputSize =
            CameraInfoUtil.getMaxHighResolutionOutputSize(cameraInfo, ImageFormat.YUV_420_888)
        // Only runs the test when the device has high resolution output sizes
        assumeTrue(maxHighResolutionOutputSize != null)

        val resolutionSelector =
            ResolutionSelector.Builder()
                .setAllowedResolutionMode(PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE)
                .setResolutionFilter { _, _ -> listOf(maxHighResolutionOutputSize) }
                .build()

        val imageAnalysis =
            ImageAnalysis.Builder().setResolutionSelector(resolutionSelector).build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, imageAnalysis)
        }
        assertThat(imageAnalysis.resolutionInfo!!.resolution).isEqualTo(maxHighResolutionOutputSize)
        imageAnalysis.setAnalyzer(CameraXExecutors.newHandlerExecutor(handler), analyzer)
        analysisResultsSemaphore.tryAcquire(5, TimeUnit.SECONDS)
        synchronized(analysisResultLock) {
            assertThat(analysisResults).isNotEmpty()
            assertThat(analysisResults.elementAt(0).resolution)
                .isEqualTo(maxHighResolutionOutputSize)
        }
    }

    @Test
    fun resolutionSelectorConfigCorrectlyMerged_afterBindToLifecycle() = runBlocking {
        val resolutionFilter = ResolutionFilter { supportedSizes, _ -> supportedSizes }
        val useCase =
            ImageAnalysis.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionFilter(resolutionFilter)
                        .setAllowedResolutionMode(PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE)
                        .build()
                )
                .build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, useCase)
        }
        val resolutionSelector = useCase.currentConfig.retrieveOption(OPTION_RESOLUTION_SELECTOR)
        // The default 4:3 AspectRatioStrategy is kept
        assertThat(resolutionSelector!!.aspectRatioStrategy)
            .isEqualTo(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
        // The default ResolutionStrategy with VGA bound size is kept
        assertThat(resolutionSelector.resolutionStrategy!!.boundSize)
            .isEqualTo(SizeUtil.RESOLUTION_VGA)
        assertThat(resolutionSelector.resolutionStrategy!!.fallbackRule)
            .isEqualTo(ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
        // The set resolutionFilter is kept
        assertThat(resolutionSelector.resolutionFilter).isEqualTo(resolutionFilter)
        // The set allowedResolutionMode is kept
        assertThat(resolutionSelector.allowedResolutionMode)
            .isEqualTo(PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE)
    }

    @Test
    fun analyzerAnalyzesImages_whenSessionErrorListenerReceivesError() = runBlocking {
        val cameraSelectors = CameraUtil.getAvailableCameraSelectors()
        assumeTrue("No enough cameras to test.", cameraSelectors.size >= 2)

        val imageAnalysis = ImageAnalysis.Builder().build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelectors[0], imageAnalysis)
        }

        imageAnalysis.setAnalyzer(CameraXExecutors.newHandlerExecutor(handler), analyzer)
        // Retrieves the initial session config
        val initialSessionConfig = imageAnalysis.sessionConfig

        // Checks that image can be received successfully when onError is received.
        triggerOnErrorAndVerifyNewImageReceived(initialSessionConfig)

        withContext(Dispatchers.Main) {
            cameraProvider.unbind(imageAnalysis)
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelectors[1], imageAnalysis)
        }

        // Checks that image can be received successfully when onError is received by the old
        // error listener.
        triggerOnErrorAndVerifyNewImageReceived(initialSessionConfig)

        // Checks that image can be received successfully when onError is received by the new
        // error listener.
        triggerOnErrorAndVerifyNewImageReceived(imageAnalysis.sessionConfig)
    }

    @Test
    fun analyzerAnalyzesYUVImages_withRotationEnabledAndReusedToHaveDifferentSize() {
        analyzerAnalyzesImages_withRotationEnabledAndReusedToHaveDifferentSize(
            ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888
        )
    }

    @Test
    fun analyzerAnalyzesYUVNV21Images_withRotationEnabledAndReusedToHaveDifferentSize() {
        analyzerAnalyzesImages_withRotationEnabledAndReusedToHaveDifferentSize(
            ImageAnalysis.OUTPUT_IMAGE_FORMAT_NV21
        )
    }

    @Test
    fun analyzerAnalyzesRGBAImages_withRotationEnabledAndReusedToHaveDifferentSize() {
        analyzerAnalyzesImages_withRotationEnabledAndReusedToHaveDifferentSize(
            ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
        )
    }

    @RequiresApi(23)
    private fun analyzerAnalyzesImages_withRotationEnabledAndReusedToHaveDifferentSize(
        outputImageFormat: Int
    ) {
        var camera: Camera? = null
        val resolutionSelector =
            ResolutionSelector.Builder()
                .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                .build()

        val imageAnalysis =
            ImageAnalysis.Builder()
                .setResolutionSelector(resolutionSelector)
                .setOutputImageFormat(outputImageFormat)
                .setOutputImageRotationEnabled(true)
                .build()
        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder().build()

        // Binds three UseCase to make imageAnalysis have a PREVIEW size resolution
        runOnMainSync {
            preview.surfaceProvider = SurfaceTextureProvider.createSurfaceTextureProvider()
            camera =
                cameraProvider.bindToLifecycle(
                    fakeLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalysis,
                )
        }

        val expectedOutputResolution1 = getRotatedResolution(camera!!, imageAnalysis)
        setAnalyzerAndVerifyNewImageReceivedWithCorrectResolution(
            imageAnalysis,
            expectedOutputResolution1,
        )

        // Unbinds all and rebind the imageAnalysis only to make imageAnalysis have a MAXIMUM size
        // resolution
        runOnMainSync {
            // Clears analyzer and analysisResults first to make sure the old resolution frame data
            // will not be kept to cause test failure
            imageAnalysis.clearAnalyzer()
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, imageAnalysis)
        }

        val expectedOutputResolution2 = getRotatedResolution(camera!!, imageAnalysis)
        assumeTrue(expectedOutputResolution2 != expectedOutputResolution1)
        setAnalyzerAndVerifyNewImageReceivedWithCorrectResolution(
            imageAnalysis,
            expectedOutputResolution2,
        )
    }

    private fun getRotatedResolution(camera: Camera, imageAnalysis: ImageAnalysis): Size {
        val resolution = imageAnalysis.resolutionInfo!!.resolution
        val rotationDegrees =
            camera.cameraInfo.getSensorRotationDegrees(imageAnalysis.targetRotation)
        return if (rotationDegrees % 180 == 0) {
            resolution
        } else {
            Size(resolution.height, resolution.width)
        }
    }

    private fun setAnalyzerAndVerifyNewImageReceivedWithCorrectResolution(
        imageAnalysis: ImageAnalysis,
        expectedResolution: Size,
    ) {
        analysisResultsSemaphore = Semaphore(0)
        synchronized(analysisResultLock) { analysisResults.clear() }
        imageAnalysis.setAnalyzer(CameraXExecutors.newHandlerExecutor(handler), analyzer)
        analysisResultsSemaphore.tryAcquire(5, TimeUnit.SECONDS)
        synchronized(analysisResultLock) {
            assertThat(analysisResults).isNotEmpty()
            assertThat(analysisResults.last().resolution).isEqualTo(expectedResolution)
        }
    }

    private fun triggerOnErrorAndVerifyNewImageReceived(sessionConfig: SessionConfig) {
        // Forces invoke the onError callback
        runOnMainSync {
            sessionConfig.errorListener!!.onError(
                sessionConfig,
                SessionConfig.SessionError.SESSION_ERROR_UNKNOWN,
            )
        }
        // Resets the semaphore
        analysisResultsSemaphore = Semaphore(0)
        analysisResultsSemaphore.tryAcquire(5, TimeUnit.SECONDS)

        // Verifies image can be received
        synchronized(analysisResultLock) { assertThat(analysisResults).isNotEmpty() }
    }

    private data class ImageProperties(
        val resolution: Size,
        val format: Int,
        val timestamp: Long,
        val rotationDegrees: Int,
    ) {

        constructor(
            image: ImageProxy
        ) : this(
            Size(image.width, image.height),
            image.format,
            image.imageInfo.timestamp,
            image.imageInfo.rotationDegrees,
        )
    }
}
