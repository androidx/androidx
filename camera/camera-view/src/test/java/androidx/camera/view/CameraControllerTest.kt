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
import android.os.Build
import android.util.Size
import android.view.Surface
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraX
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.impl.ImageAnalysisConfig
import androidx.camera.core.impl.ImageCaptureConfig
import androidx.camera.core.impl.ImageOutputConfig
import androidx.camera.testing.fakes.FakeAppConfig
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import java.util.concurrent.Executors

/**
 * Unit tests for [CameraController].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class CameraControllerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var controller: CameraController
    private val targetSizeWithAspectRatio =
        CameraController.OutputSize(AspectRatio.RATIO_16_9)
    private val targetSizeWithResolution =
        CameraController.OutputSize(Size(1080, 1960))

    @Before
    public fun setUp() {
        val cameraXConfig = CameraXConfig.Builder.fromConfig(
            FakeAppConfig.create()
        ).build()
        CameraX.initialize(context, cameraXConfig).get()
        controller = LifecycleCameraController(context)
    }

    @After
    public fun shutDown() {
        CameraX.shutdown().get()
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
    public fun setVideoCaptureResolution() {
        controller.videoCaptureTargetSize = targetSizeWithResolution
        assertThat(controller.videoCaptureTargetSize).isEqualTo(targetSizeWithResolution)

        val config = controller.mVideoCapture.currentConfig as ImageOutputConfig
        assertThat(config.targetResolution).isEqualTo(targetSizeWithResolution.resolution)
    }

    @UiThreadTest
    @Test
    public fun setVideoCaptureAspectRatio() {
        controller.videoCaptureTargetSize = targetSizeWithAspectRatio
        assertThat(controller.videoCaptureTargetSize).isEqualTo(targetSizeWithAspectRatio)

        val config = controller.mVideoCapture.currentConfig as ImageOutputConfig
        assertThat(config.targetAspectRatio).isEqualTo(targetSizeWithAspectRatio.aspectRatio)
    }

    @UiThreadTest
    @Test
    public fun sensorRotationChanges_useCaseTargetRotationUpdated() {
        // Act.
        controller.mRotationReceiver.onRotationChanged(Surface.ROTATION_180)

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
