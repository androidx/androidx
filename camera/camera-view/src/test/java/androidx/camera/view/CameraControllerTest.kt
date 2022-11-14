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

package androidx.camera.view

import android.content.Context
import android.graphics.Matrix
import android.os.Build
import android.util.Size
import android.view.Surface
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.impl.ImageAnalysisConfig
import androidx.camera.core.impl.ImageCaptureConfig
import androidx.camera.core.impl.ImageOutputConfig
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.fakes.FakeAppConfig
import androidx.camera.view.CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED
import androidx.camera.view.transform.OutputTransform
import androidx.camera.video.Quality
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [CameraController].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class CameraControllerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var controller: CameraController
    private val targetSizeWithAspectRatio =
        CameraController.OutputSize(AspectRatio.RATIO_16_9)
    private val targetSizeWithResolution =
        CameraController.OutputSize(Size(1080, 1960))
    private val targetVideoQuality = Quality.HIGHEST

    @Before
    public fun setUp() {
        val cameraXConfig = CameraXConfig.Builder.fromConfig(
            FakeAppConfig.create()
        ).build()
        ProcessCameraProvider.configureInstance(cameraXConfig)
        cameraProvider = ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]
        controller = LifecycleCameraController(context)
    }

    @After
    public fun shutDown() {
        if (::cameraProvider.isInitialized) {
            cameraProvider.shutdown()[10000, TimeUnit.MILLISECONDS]
        }
    }

    @Test
    fun setAnalyzerWithNewResolutionOverride_imageAnalysisIsRecreated() {
        // Arrange: record the original ImageAnalysis
        val originalImageAnalysis = controller.mImageAnalysis
        // Act: set a Analyzer with overridden size.
        controller.setImageAnalysisAnalyzer(mainThreadExecutor(), createAnalyzer(Size(1, 1)))
        // Assert: the ImageAnalysis has be recreated.
        assertThat(controller.mImageAnalysis).isNotEqualTo(originalImageAnalysis)
        val newImageAnalysis = controller.mImageAnalysis
        // Act: set a Analyzer with a different overridden size.
        controller.setImageAnalysisAnalyzer(mainThreadExecutor(), createAnalyzer(Size(1, 2)))
        // Assert: the ImageAnalysis has be recreated, again.
        assertThat(controller.mImageAnalysis).isNotEqualTo(newImageAnalysis)
    }

    @Test
    fun clearAnalyzerWithResolutionOverride_imageAnalysisIsRecreated() {
        // Arrange: set a Analyzer with resolution and record the ImageAnalysis.
        controller.setImageAnalysisAnalyzer(mainThreadExecutor(), createAnalyzer(Size(1, 1)))
        val originalImageAnalysis = controller.mImageAnalysis
        // Act: clear Analyzer
        controller.clearImageAnalysisAnalyzer()
        // Assert: the ImageAnalysis has been recreated.
        assertThat(controller.mImageAnalysis).isNotEqualTo(originalImageAnalysis)
    }

    @Test
    fun setAnalyzerWithNoOverride_imageAnalysisIsNotRecreated() {
        // Arrange: record the original ImageAnalysis
        val originalImageAnalysis = controller.mImageAnalysis
        // Act: setAnalyzer with no resolution.
        controller.setImageAnalysisAnalyzer(mainThreadExecutor(), createAnalyzer(null))
        // Assert: the ImageAnalysis is the same.
        assertThat(controller.mImageAnalysis).isEqualTo(originalImageAnalysis)
    }

    /**
     * Creates a [ImageAnalysis.Analyzer] with the given resolution override.
     */
    private fun createAnalyzer(size: Size?): ImageAnalysis.Analyzer {
        return object : ImageAnalysis.Analyzer {
            override fun analyze(image: ImageProxy) {
                // no-op
            }

            override fun getDefaultTargetResolution(): Size? {
                return size
            }
        }
    }

    @Test
    fun viewTransform_valueIsPassedToAnalyzer() {
        val previewTransform = Matrix()
        assertThat(
            getPreviewTransformPassedToAnalyzer(
                COORDINATE_SYSTEM_VIEW_REFERENCED,
                previewTransform
            )
        ).isEqualTo(previewTransform)

        assertThat(
            getPreviewTransformPassedToAnalyzer(
                COORDINATE_SYSTEM_VIEW_REFERENCED,
                null
            )
        ).isEqualTo(null)
    }

    @Test
    fun originalTransform_valueIsNotPassedToAnalyzer() {
        assertThat(
            getPreviewTransformPassedToAnalyzer(
                COORDINATE_SYSTEM_ORIGINAL,
                Matrix()
            )
        ).isNull()
    }

    private fun getPreviewTransformPassedToAnalyzer(
        coordinateSystem: Int,
        previewTransform: Matrix?
    ): Matrix? {
        var matrix: Matrix? = null
        val analyzer = object : ImageAnalysis.Analyzer {
            override fun analyze(image: ImageProxy) {
                // no-op
            }

            override fun updateTransform(newMatrix: Matrix?) {
                matrix = newMatrix
            }

            override fun getTargetCoordinateSystem(): Int {
                return coordinateSystem
            }
        }
        controller.setImageAnalysisAnalyzer(mainThreadExecutor(), analyzer)
        val outputTransform = previewTransform?.let {
            OutputTransform(it, Size(1, 1))
        }
        controller.updatePreviewViewTransform(outputTransform)
        return matrix
    }

    @UiThreadTest
    @Test
    public fun setPreviewAspectRatio() {
        controller.previewTargetSize = targetSizeWithAspectRatio
        assertThat(controller.previewTargetSize).isEqualTo(targetSizeWithAspectRatio)

        val config = controller.mPreview.currentConfig as ImageOutputConfig
        assertThat(config.targetAspectRatio).isEqualTo(targetSizeWithAspectRatio.aspectRatio)
    }

    @UiThreadTest
    @Test
    public fun setPreviewResolution() {
        controller.previewTargetSize = targetSizeWithResolution
        assertThat(controller.previewTargetSize).isEqualTo(targetSizeWithResolution)

        val config = controller.mPreview.currentConfig as ImageOutputConfig
        assertThat(config.targetResolution).isEqualTo(targetSizeWithResolution.resolution)
    }

    @UiThreadTest
    @Test
    public fun setAnalysisAspectRatio() {
        controller.imageAnalysisTargetSize = targetSizeWithAspectRatio
        assertThat(controller.imageAnalysisTargetSize).isEqualTo(targetSizeWithAspectRatio)

        val config = controller.mImageAnalysis.currentConfig as ImageOutputConfig
        assertThat(config.targetAspectRatio).isEqualTo(targetSizeWithAspectRatio.aspectRatio)
    }

    @UiThreadTest
    @Test
    public fun setAnalysisBackgroundExecutor() {
        val executor = Executors.newSingleThreadExecutor()
        controller.imageAnalysisBackgroundExecutor = executor
        assertThat(controller.imageAnalysisBackgroundExecutor).isEqualTo(executor)
        val config = controller.mImageAnalysis.currentConfig as ImageAnalysisConfig
        assertThat(config.backgroundExecutor).isEqualTo(executor)
    }

    @UiThreadTest
    @Test
    public fun setAnalysisQueueDepth() {
        controller.imageAnalysisImageQueueDepth = 100
        assertThat(controller.imageAnalysisImageQueueDepth).isEqualTo(100)
        assertThat(controller.mImageAnalysis.imageQueueDepth).isEqualTo(100)
    }

    @UiThreadTest
    @Test
    public fun setAnalysisBackpressureStrategy() {
        controller.imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_BLOCK_PRODUCER
        assertThat(controller.imageAnalysisBackpressureStrategy)
            .isEqualTo(ImageAnalysis.STRATEGY_BLOCK_PRODUCER)
        assertThat(controller.mImageAnalysis.backpressureStrategy)
            .isEqualTo(ImageAnalysis.STRATEGY_BLOCK_PRODUCER)
    }

    @UiThreadTest
    @Test
    public fun setImageCaptureResolution() {
        controller.imageCaptureTargetSize = targetSizeWithResolution
        assertThat(controller.imageCaptureTargetSize).isEqualTo(targetSizeWithResolution)

        val config = controller.mImageCapture.currentConfig as ImageOutputConfig
        assertThat(config.targetResolution).isEqualTo(targetSizeWithResolution.resolution)
    }

    @UiThreadTest
    @Test
    public fun setImageCaptureAspectRatio() {
        controller.imageCaptureTargetSize = targetSizeWithAspectRatio
        assertThat(controller.imageCaptureTargetSize).isEqualTo(targetSizeWithAspectRatio)

        val config = controller.mImageCapture.currentConfig as ImageOutputConfig
        assertThat(config.targetAspectRatio).isEqualTo(targetSizeWithAspectRatio.aspectRatio)
    }

    @UiThreadTest
    @Test
    public fun setImageCaptureMode() {
        controller.imageCaptureMode = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
        assertThat(controller.imageCaptureMode)
            .isEqualTo(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
        assertThat(controller.mImageCapture.captureMode)
            .isEqualTo(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
    }

    @UiThreadTest
    @Test
    public fun setImageCaptureIoExecutor() {
        val ioExecutor = Executors.newSingleThreadExecutor()
        controller.imageCaptureIoExecutor = ioExecutor
        assertThat(controller.imageCaptureIoExecutor).isEqualTo(ioExecutor)
        val config = controller.mImageCapture.currentConfig as ImageCaptureConfig
        assertThat(config.ioExecutor).isEqualTo(ioExecutor)
    }

    @UiThreadTest
    @Test
    fun setVideoCaptureQuality() {
        controller.videoCaptureTargetQuality = targetVideoQuality
        assertThat(controller.videoCaptureTargetQuality).isEqualTo(targetVideoQuality)
    }

    @UiThreadTest
    @Test
    public fun sensorRotationChanges_useCaseTargetRotationUpdated() {
        // Act.
        controller.mDeviceRotationListener.onRotationChanged(Surface.ROTATION_180)

        // Assert.
        assertThat(controller.mImageAnalysis.targetRotation).isEqualTo(Surface.ROTATION_180)
        assertThat(controller.mImageCapture.targetRotation).isEqualTo(Surface.ROTATION_180)
        val videoConfig = controller.mVideoCapture.currentConfig as ImageOutputConfig
        assertThat(videoConfig.targetRotation).isEqualTo(Surface.ROTATION_180)
    }

    @UiThreadTest
    @Test
    public fun setSelectorBeforeBound_selectorSet() {
        // Arrange.
        assertThat(controller.cameraSelector.lensFacing).isEqualTo(CameraSelector.LENS_FACING_BACK)

        // Act.
        controller.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        // Assert.
        assertThat(controller.cameraSelector.lensFacing).isEqualTo(CameraSelector.LENS_FACING_FRONT)
    }
}
