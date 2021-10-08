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
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import androidx.annotation.GuardedBy
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraX
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.BackpressureStrategy
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.impl.ImageOutputConfig
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.testing.CameraUtil
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

private val DEFAULT_CAMERA_SELECTOR = CameraSelector.DEFAULT_BACK_CAMERA

@LargeTest
@RunWith(Parameterized::class)
internal class ImageAnalysisTest(
    private val implName: String,
    private val cameraConfig: CameraXConfig
) {

    @get:Rule
    val cameraRule = CameraUtil.grantCameraPermissionAndPreTest()

    companion object {
        private val GUARANTEED_RESOLUTION = Size(640, 480)

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
            arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig())
        )
    }

    private val analysisResultLock = Any()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @GuardedBy("analysisResultLock")
    private val analysisResults = mutableSetOf<ImageProperties>()
    private val analyzer = ImageAnalysis.Analyzer { image ->
        synchronized(analysisResultLock) {
            analysisResults.add(ImageProperties(image))
        }
        analysisResultsSemaphore.release()
        image.close()
    }
    private lateinit var analysisResultsSemaphore: Semaphore
    private lateinit var handlerThread: HandlerThread
    private lateinit var handler: Handler
    private var camera: CameraUseCaseAdapter? = null

    @Before
    fun setUp(): Unit = runBlocking {
        CameraX.initialize(context, cameraConfig).get(10, TimeUnit.SECONDS)
        handlerThread = HandlerThread("AnalysisThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)
        analysisResultsSemaphore = Semaphore(0)
    }

    @After
    fun tearDown(): Unit = runBlocking {
        camera?.let { camera ->
            // TODO: The removeUseCases() call might be removed after clarifying the
            //  abortCaptures() issue in b/162314023.
            withContext(Dispatchers.Main) {
                camera.removeUseCases(camera.useCases)
            }
        }
        CameraX.shutdown().get(10, TimeUnit.SECONDS)
        handlerThread.quitSafely()
    }

    @Test
    fun exceedMaxImagesWithoutClosing_doNotCrash() {
        // Arrange.
        val queueDepth = 3
        val semaphore = Semaphore(0)
        val useCase = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_BLOCK_PRODUCER)
            .setImageQueueDepth(queueDepth)
            .build()
        val imageProxyList = mutableListOf<ImageProxy>()
        useCase.setAnalyzer(
            CameraXExecutors.newHandlerExecutor(handler),
            { image ->
                imageProxyList.add(image)
                semaphore.release()
            }
        )

        // Act.
        camera = CameraUtil.createCameraAndAttachUseCase(
            context,
            CameraSelector.DEFAULT_FRONT_CAMERA,
            useCase
        )

        // Assert: waiting for images does not crash.
        assertThat(semaphore.tryAcquire(queueDepth + 1, 1, TimeUnit.SECONDS)).isFalse()

        // Clean it up.
        useCase.clearAnalyzer()
        for (image in imageProxyList) {
            image.close()
        }
    }

    @Ignore("TODO(b/183224022): Remove when resolution selection logic is ported to CameraPipe")
    @Test
    fun canSupportGuaranteedSizeFront() {
        // CameraSelector.LENS_FACING_FRONT/LENS_FACING_BACK are defined as constant int 0 and 1.
        // Using for-loop to check both front and back device cameras can support the guaranteed
        // 640x480 size.
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT))
        assumeTrue(!CameraUtil.requiresCorrectedAspectRatio(CameraSelector.LENS_FACING_FRONT))

        // Checks camera device sensor degrees to set correct target rotation value to make sure
        // the exactly matching result size 640x480 can be selected if the device supports it.
        val sensorOrientation = CameraUtil.getSensorOrientation(CameraSelector.LENS_FACING_FRONT)
        val isRotateNeeded = sensorOrientation!! % 180 != 0
        val useCase = ImageAnalysis.Builder()
            .setTargetResolution(GUARANTEED_RESOLUTION)
            .setTargetRotation(if (isRotateNeeded) Surface.ROTATION_90 else Surface.ROTATION_0)
            .build()
        camera = CameraUtil.createCameraAndAttachUseCase(
            context,
            CameraSelector.DEFAULT_FRONT_CAMERA,
            useCase
        )
        useCase.setAnalyzer(CameraXExecutors.newHandlerExecutor(handler), analyzer)

        assertThat(analysisResultsSemaphore.tryAcquire(5, TimeUnit.SECONDS)).isTrue()
        synchronized(analysisResultLock) {
            // Check the analyzed image exactly matches 640x480 size. This test can also check
            // whether the guaranteed resolution 640x480 is really supported for YUV_420_888
            // format on the devices when running the test.
            assertThat(GUARANTEED_RESOLUTION).isEqualTo(
                analysisResults.iterator().next().resolution
            )
        }
    }

    @Ignore("TODO(b/183224022): Remove when resolution selection logic is ported to CameraPipe")
    @Test
    fun canSupportGuaranteedSizeBack() {
        // CameraSelector.LENS_FACING_FRONT/LENS_FACING_BACK are defined as constant int 0 and 1.
        // Using for-loop to check both front and back device cameras can support the guaranteed
        // 640x480 size.
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))
        assumeTrue(!CameraUtil.requiresCorrectedAspectRatio(CameraSelector.LENS_FACING_BACK))

        // Checks camera device sensor degrees to set correct target rotation value to make sure
        // the exactly matching result size 640x480 can be selected if the device supports it.
        val sensorOrientation = CameraUtil.getSensorOrientation(CameraSelector.LENS_FACING_BACK)
        val isRotateNeeded = sensorOrientation!! % 180 != 0
        val useCase = ImageAnalysis.Builder()
            .setTargetResolution(GUARANTEED_RESOLUTION)
            .setTargetRotation(if (isRotateNeeded) Surface.ROTATION_90 else Surface.ROTATION_0)
            .build()
        camera = CameraUtil.createCameraAndAttachUseCase(
            context,
            CameraSelector.DEFAULT_BACK_CAMERA,
            useCase
        )
        useCase.setAnalyzer(CameraXExecutors.newHandlerExecutor(handler), analyzer)

        assertThat(analysisResultsSemaphore.tryAcquire(5, TimeUnit.SECONDS)).isTrue()
        synchronized(analysisResultLock) {
            // Check the analyzed image exactly matches 640x480 size. This test can also check
            // whether the guaranteed resolution 640x480 is really supported for YUV_420_888
            // format on the devices when running the test.
            assertThat(GUARANTEED_RESOLUTION).isEqualTo(
                analysisResults.iterator().next().resolution
            )
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

    private fun analyzerAnalyzesImagesWithStrategy(@BackpressureStrategy strategy: Int) {
        val useCase = ImageAnalysis.Builder()
            .setBackpressureStrategy(strategy)
            .build()
        camera = CameraUtil.createCameraAndAttachUseCase(context, DEFAULT_CAMERA_SELECTOR, useCase)
        useCase.setAnalyzer(CameraXExecutors.newHandlerExecutor(handler), analyzer)
        analysisResultsSemaphore.tryAcquire(5, TimeUnit.SECONDS)
        synchronized(analysisResultLock) { assertThat(analysisResults).isNotEmpty() }
    }

    @Test
    fun analyzerDoesNotAnalyzeImages_whenCameraIsNotOpen() {
        val useCase = ImageAnalysis.Builder().build()
        // Bind but do not start lifecycle
        camera = CameraUtil.createCameraAndAttachUseCase(context, DEFAULT_CAMERA_SELECTOR, useCase)
        camera!!.detachUseCases()
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
    fun canObtainDefaultImageQueueDepth() {
        val imageAnalysis = ImageAnalysis.Builder().build()

        // Should not be less than 1
        assertThat(imageAnalysis.imageQueueDepth).isAtLeast(1)
    }

    @Test
    fun defaultAspectRatioWillBeSet_whenTargetResolutionIsNotSet() {
        val useCase = ImageAnalysis.Builder().build()
        camera = CameraUtil.createCameraAndAttachUseCase(context, DEFAULT_CAMERA_SELECTOR, useCase)
        val config = useCase.currentConfig as ImageOutputConfig
        assertThat(config.targetAspectRatio).isEqualTo(AspectRatio.RATIO_4_3)
    }

    @Test
    fun defaultAspectRatioWontBeSet_whenTargetResolutionIsSet() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))
        val useCase = ImageAnalysis.Builder()
            .setTargetResolution(GUARANTEED_RESOLUTION)
            .build()
        assertThat(
            useCase.currentConfig.containsOption(ImageOutputConfig.OPTION_TARGET_ASPECT_RATIO)
        ).isFalse()

        camera = CameraUtil.createCameraAndAttachUseCase(
            context,
            CameraSelector.DEFAULT_BACK_CAMERA,
            useCase
        )
        assertThat(
            useCase.currentConfig.containsOption(ImageOutputConfig.OPTION_TARGET_ASPECT_RATIO)
        ).isFalse()
    }

    @Test
    fun targetRotationCanBeUpdatedAfterUseCaseIsCreated() {
        val imageAnalysis = ImageAnalysis.Builder().setTargetRotation(Surface.ROTATION_0).build()
        imageAnalysis.targetRotation = Surface.ROTATION_90
        assertThat(imageAnalysis.targetRotation).isEqualTo(Surface.ROTATION_90)
    }

    @Test
    fun targetResolutionIsUpdatedAfterTargetRotationIsUpdated() {
        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(GUARANTEED_RESOLUTION)
            .setTargetRotation(Surface.ROTATION_0)
            .build()
        camera =
            CameraUtil.createCameraAndAttachUseCase(context, DEFAULT_CAMERA_SELECTOR, imageAnalysis)

        // Updates target rotation from ROTATION_0 to ROTATION_90.
        imageAnalysis.targetRotation = Surface.ROTATION_90
        val newConfig = imageAnalysis.currentConfig as ImageOutputConfig
        val expectedTargetResolution = Size(
            GUARANTEED_RESOLUTION.height,
            GUARANTEED_RESOLUTION.width
        )

        // Expected targetResolution will be reversed from original target resolution.
        assertThat(newConfig.targetResolution == expectedTargetResolution).isTrue()
    }

    // TODO(b/162298517): change the test to be deterministic instead of depend upon timing.
    @Test
    fun analyzerSetMultipleTimesInKeepOnlyLatestMode() {
        val useCase = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        camera = CameraUtil.createCameraAndAttachUseCase(context, DEFAULT_CAMERA_SELECTOR, useCase)
        useCase.setAnalyzer(CameraXExecutors.newHandlerExecutor(handler), analyzer)
        analysisResultsSemaphore.tryAcquire(5, TimeUnit.SECONDS)

        val slowAnalyzer = ImageAnalysis.Analyzer { image ->
            try {
                Thread.sleep(200)
                image.close()
            } catch (e: Exception) {
            }
        }
        useCase.setAnalyzer(CameraXExecutors.newHandlerExecutor(handler), slowAnalyzer)
        Thread.sleep(100)

        useCase.setAnalyzer(CameraXExecutors.newHandlerExecutor(handler), slowAnalyzer)
        Thread.sleep(100)

        useCase.setAnalyzer(CameraXExecutors.newHandlerExecutor(handler), slowAnalyzer)
        Thread.sleep(100)

        useCase.setAnalyzer(CameraXExecutors.newHandlerExecutor(handler), slowAnalyzer)
        Thread.sleep(100)

        useCase.setAnalyzer(CameraXExecutors.newHandlerExecutor(handler), slowAnalyzer)
        Thread.sleep(100)
    }

    @Test
    fun useCaseConfigCanBeReset_afterUnbind() = runBlocking {
        val useCase = ImageAnalysis.Builder().build()
        val initialConfig = useCase.currentConfig
        camera = CameraUtil.createCameraAndAttachUseCase(context, DEFAULT_CAMERA_SELECTOR, useCase)

        withContext(Dispatchers.Main) {
            camera!!.removeUseCases(setOf(useCase))
        }
        val configAfterUnbinding = useCase.currentConfig
        assertThat(initialConfig == configAfterUnbinding).isTrue()
    }

    @Test
    fun targetRotationIsRetained_whenUseCaseIsReused() = runBlocking {
        val useCase = ImageAnalysis.Builder().build()
        camera = CameraUtil.createCameraAndAttachUseCase(context, DEFAULT_CAMERA_SELECTOR, useCase)

        // Generally, the device can't be rotated to Surface.ROTATION_180. Therefore,
        // use it to do the test.
        useCase.targetRotation = Surface.ROTATION_180
        withContext(Dispatchers.Main) {

            // Check the target rotation is kept when the use case is unbound.
            camera!!.removeUseCases(setOf(useCase))
            assertThat(useCase.targetRotation).isEqualTo(Surface.ROTATION_180)
        }

        // Check the target rotation is kept when the use case is rebound to the
        // lifecycle.
        camera = CameraUtil.createCameraAndAttachUseCase(context, DEFAULT_CAMERA_SELECTOR, useCase)
        assertThat(useCase.targetRotation).isEqualTo(Surface.ROTATION_180)
    }

    @Test
    @Throws(InterruptedException::class)
    fun useCaseCanBeReusedInSameCamera() = runBlocking {
        val useCase = ImageAnalysis.Builder().build()
        camera = CameraUtil.createCameraAndAttachUseCase(context, DEFAULT_CAMERA_SELECTOR, useCase)
        useCase.setAnalyzer(CameraXExecutors.newHandlerExecutor(handler), analyzer)
        assertThat(analysisResultsSemaphore.tryAcquire(5, TimeUnit.SECONDS)).isTrue()
        withContext(Dispatchers.Main) { camera!!.removeUseCases(setOf(useCase)) }
        analysisResultsSemaphore = Semaphore( /*permits=*/0)
        // Rebind the use case to the same camera.
        camera = CameraUtil.createCameraAndAttachUseCase(context, DEFAULT_CAMERA_SELECTOR, useCase)
        assertThat(analysisResultsSemaphore.tryAcquire(5, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    @Throws(InterruptedException::class)
    fun useCaseCanBeReusedInDifferentCamera() = runBlocking {
        val useCase = ImageAnalysis.Builder().build()
        camera = CameraUtil.createCameraAndAttachUseCase(
            context,
            CameraSelector.DEFAULT_BACK_CAMERA, useCase
        )
        useCase.setAnalyzer(CameraXExecutors.newHandlerExecutor(handler), analyzer)
        assertThat(analysisResultsSemaphore.tryAcquire(5, TimeUnit.SECONDS)).isTrue()
        withContext(Dispatchers.Main) { camera!!.removeUseCases(setOf(useCase)) }
        analysisResultsSemaphore = Semaphore( /*permits=*/0)
        // Rebind the use case to different camera.
        camera = CameraUtil.createCameraAndAttachUseCase(
            context,
            CameraSelector.DEFAULT_FRONT_CAMERA, useCase
        )
        assertThat(analysisResultsSemaphore.tryAcquire(5, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun returnValidTargetRotation_afterUseCaseIsCreated() {
        val imageCapture = ImageCapture.Builder().build()
        assertThat(imageCapture.targetRotation).isNotEqualTo(ImageOutputConfig.INVALID_ROTATION)
    }

    @Test
    fun returnCorrectTargetRotation_afterUseCaseIsAttached() {
        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetRotation(Surface.ROTATION_180)
            .build()
        camera =
            CameraUtil.createCameraAndAttachUseCase(context, DEFAULT_CAMERA_SELECTOR, imageAnalysis)
        assertThat(imageAnalysis.targetRotation).isEqualTo(Surface.ROTATION_180)
    }

    private data class ImageProperties(
        val resolution: Size,
        val format: Int,
        val timestamp: Long,
        val rotationDegrees: Int
    ) {

        constructor(image: ImageProxy) : this(
            Size(image.width, image.height),
            image.format,
            image.imageInfo.timestamp,
            image.imageInfo.rotationDegrees
        )
    }
}