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
import android.graphics.PointF
import android.os.Build
import android.os.Looper.getMainLooper
import android.util.Range
import android.util.Rational
import android.util.Size
import android.view.Surface
import androidx.camera.core.AspectRatio.RATIO_16_9
import androidx.camera.core.AspectRatio.RATIO_4_3
import androidx.camera.core.CameraSelector
import androidx.camera.core.DynamicRange
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.FocusMeteringResult
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL
import androidx.camera.core.ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.FLASH_MODE_ON
import androidx.camera.core.ImageCapture.ScreenFlash
import androidx.camera.core.ImageProxy
import androidx.camera.core.MirrorMode
import androidx.camera.core.Preview.SurfaceProvider
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.TorchState
import androidx.camera.core.ViewPort
import androidx.camera.core.impl.ImageAnalysisConfig
import androidx.camera.core.impl.ImageCaptureConfig
import androidx.camera.core.impl.ImageOutputConfig
import androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraControl
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.fakes.FakeSurfaceEffect
import androidx.camera.testing.impl.fakes.FakeSurfaceProcessor
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.view.CameraController.TAP_TO_FOCUS_FOCUSED
import androidx.camera.view.CameraController.TAP_TO_FOCUS_NOT_FOCUSED
import androidx.camera.view.CameraController.TAP_TO_FOCUS_NOT_STARTED
import androidx.camera.view.CameraController.TAP_TO_FOCUS_STARTED
import androidx.camera.view.internal.ScreenFlashUiInfo
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowSystemClock

/** Unit tests for [CameraController]. */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(
    minSdk = Build.VERSION_CODES.LOLLIPOP,
    instrumentedPackages = ["androidx.camera.view"], // required for shadow clock to work
)
class CameraControllerTest {
    companion object {
        const val LINEAR_ZOOM = .1F
        const val ZOOM_RATIO = .5F
        const val TORCH_ENABLED = true
        const val FOCUS_AUTO_CANCEL_DEFAULT_TIMEOUT_MILLIS =
            FocusMeteringAction.DEFAULT_AUTO_CANCEL_DURATION_MILLIS
        val TAP_POINT_1 = PointF(0F, 0F)
        val TAP_POINT_2 = PointF(1F, 2F)
    }

    private val previewViewTransform = Matrix().also { it.postRotate(90F) }
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var controller: LifecycleCameraController

    @Suppress("deprecation")
    private val targetSizeWithAspectRatio = CameraController.OutputSize(RATIO_16_9)
    private val resolutionSelector =
        ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
            .build()

    @Suppress("deprecation")
    private val targetSizeWithResolution = CameraController.OutputSize(Size(1080, 1960))
    private val targetVideoQuality = Quality.HIGHEST
    private val fakeViewPort = ViewPort.Builder(Rational(1, 1), Surface.ROTATION_0).build()
    private val fakeCameraControl = FakeCameraControl()
    private val fakeCamera = FakeCamera(fakeCameraControl)
    private val processCameraProviderWrapper = FakeProcessCameraProviderWrapper(fakeCamera)
    private lateinit var lifecycleCameraProviderCompleter:
        CallbackToFutureAdapter.Completer<ProcessCameraProviderWrapper>

    private val pointFactory = SurfaceOrientedMeteringPointFactory(1f, 1f)

    @Before
    fun setUp() {
        val lifecycleCameraProviderFuture =
            CallbackToFutureAdapter.getFuture { completer ->
                lifecycleCameraProviderCompleter = completer
                "CameraControllerTest.lifecycleCameraProviderFuture"
            }
        controller = LifecycleCameraController(context, lifecycleCameraProviderFuture)
        controller.bindToLifecycle(FakeLifecycleOwner())
        controller.attachPreviewSurface({}, fakeViewPort)
    }

    @Test
    fun setEffects_unbindInvoked() {
        // Arrange.
        completeCameraInitialization()
        assertThat(processCameraProviderWrapper.unbindInvoked()).isFalse()
        // Act.
        controller.setEffects(
            setOf(FakeSurfaceEffect(directExecutor(), FakeSurfaceProcessor(directExecutor())))
        )
        // Assert.
        assertThat(processCameraProviderWrapper.unbindInvoked()).isTrue()
    }

    @Test
    fun clearEffects_unbindInvoked() {
        // Arrange.
        completeCameraInitialization()
        assertThat(processCameraProviderWrapper.unbindInvoked()).isFalse()
        // Act.
        controller.clearEffects()
        // Assert.
        assertThat(processCameraProviderWrapper.unbindInvoked()).isTrue()
    }

    @Test
    fun setPendingValues_valuesPropagateAfterInit() {
        // Arrange: set pending values
        val linearZoomFuture = controller.setLinearZoom(LINEAR_ZOOM)
        val zoomRatioFuture = controller.setZoomRatio(ZOOM_RATIO)
        val torchFuture = controller.enableTorch(TORCH_ENABLED)
        assertThat(fakeCameraControl.linearZoom).isNotEqualTo(LINEAR_ZOOM)
        assertThat(fakeCameraControl.zoomRatio).isNotEqualTo(ZOOM_RATIO)
        assertThat(fakeCameraControl.torchEnabled).isNotEqualTo(TORCH_ENABLED)
        assertThat(linearZoomFuture.isDone).isFalse()
        assertThat(zoomRatioFuture.isDone).isFalse()
        assertThat(torchFuture.isDone).isFalse()

        // Act.
        completeCameraInitialization()

        // Assert:
        assertThat(fakeCameraControl.linearZoom).isEqualTo(LINEAR_ZOOM)
        assertThat(fakeCameraControl.zoomRatio).isEqualTo(ZOOM_RATIO)
        assertThat(fakeCameraControl.torchEnabled).isEqualTo(TORCH_ENABLED)
        assertThat(linearZoomFuture.isDone).isTrue()
        assertThat(zoomRatioFuture.isDone).isTrue()
        assertThat(torchFuture.isDone).isTrue()
    }

    @Test
    fun unbindController_canSetPendingValueAgain() {
        // Arrange: set pending values
        var linearZoomFuture = controller.setLinearZoom(LINEAR_ZOOM)

        // Act: complete initialization.
        completeCameraInitialization()
        // Assert: pending value is set.
        assertThat(fakeCameraControl.linearZoom).isEqualTo(LINEAR_ZOOM)
        assertThat(linearZoomFuture.isDone).isTrue()

        // Act: unbind controller, set pending value again and rebind.
        controller.unbind()
        linearZoomFuture = controller.setLinearZoom(1F)
        controller.bindToLifecycle(FakeLifecycleOwner())
        // Assert: pending value is set to new value.
        assertThat(fakeCameraControl.linearZoom).isEqualTo(1F)
        assertThat(linearZoomFuture.isDone).isTrue()
    }

    @Test
    fun initCompletes_torchStatePropagated() {
        // Arrange: get LiveData before init completes
        val torchState = controller.torchState
        // State is null.
        assertThat(torchState.value).isNull()
        // Act: complete initialization.
        completeCameraInitialization()
        // Assert: LiveData gets a value update.
        assertThat(torchState.value).isEqualTo(TorchState.OFF)
    }

    @Test
    fun initCompletes_zoomStatePropagated() {
        // Arrange: get LiveData before init completes
        val zoomState = controller.zoomState
        // State is null.
        assertThat(zoomState.value).isNull()
        // Act: complete initialization.
        completeCameraInitialization()
        // Assert: LiveData gets a value update.
        assertThat(zoomState.value).isEqualTo(fakeCamera.cameraInfo.zoomState.value)
    }

    private fun completeCameraInitialization() {
        lifecycleCameraProviderCompleter.set(processCameraProviderWrapper)
        shadowOf(getMainLooper()).idle()
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

    @Test
    fun setAnalysisFormat_setSuccessfully() {
        // Act: set the format to RGBA.
        controller.imageAnalysisOutputImageFormat = ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
        // Assert: returned format is RGBA.
        assertThat(controller.imageAnalysisOutputImageFormat)
            .isEqualTo(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
    }

    /** Creates a [ImageAnalysis.Analyzer] with the given resolution override. */
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
        // Non-null value passed to analyzer.
        assertThat(
                getPreviewTransformPassedToAnalyzer(
                    COORDINATE_SYSTEM_VIEW_REFERENCED,
                    previewViewTransform,
                )
            )
            .isEqualTo(previewViewTransform)

        // Null value passed to analyzer.
        assertThat(getPreviewTransformPassedToAnalyzer(COORDINATE_SYSTEM_VIEW_REFERENCED, null))
            .isEqualTo(null)
    }

    @Test
    fun originalTransform_valueIsNotPassedToAnalyzer() {
        // Value not passed to analyzer. Analyzer still has it's original value which is identity
        // matrix.
        assertThat(
                getPreviewTransformPassedToAnalyzer(
                        COORDINATE_SYSTEM_ORIGINAL,
                        previewViewTransform,
                    )!!
                    .isIdentity
            )
            .isTrue()
    }

    private fun getPreviewTransformPassedToAnalyzer(
        coordinateSystem: Int,
        previewTransform: Matrix?,
    ): Matrix? {
        var matrix: Matrix? = Matrix()
        val analyzer =
            object : ImageAnalysis.Analyzer {
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
        controller.updatePreviewViewTransform(previewTransform)
        return matrix
    }

    @UiThreadTest
    @Test
    fun setPreviewResolutionSelector() {
        controller.previewResolutionSelector = resolutionSelector
        assertThat(controller.previewResolutionSelector).isEqualTo(resolutionSelector)

        val config = controller.mPreview.currentConfig as ImageOutputConfig
        assertThat(config.resolutionSelector).isEqualTo(resolutionSelector)
    }

    @UiThreadTest
    @Test
    fun setAnalysisResolutionSelector() {
        controller.imageAnalysisResolutionSelector = resolutionSelector
        assertThat(controller.imageAnalysisResolutionSelector).isEqualTo(resolutionSelector)

        val config = controller.mImageAnalysis.currentConfig as ImageOutputConfig
        assertThat(config.resolutionSelector).isEqualTo(resolutionSelector)
    }

    @UiThreadTest
    @Test
    fun setImageCaptureResolutionSelector() {
        controller.imageCaptureResolutionSelector = resolutionSelector
        assertThat(controller.imageCaptureResolutionSelector).isEqualTo(resolutionSelector)

        val config = controller.mImageCapture.currentConfig as ImageOutputConfig
        assertThat(config.resolutionSelector).isEqualTo(resolutionSelector)
    }

    @UiThreadTest
    @Test
    @Suppress("deprecation")
    fun setPreviewAspectRatio() {
        controller.previewTargetSize = targetSizeWithAspectRatio
        assertThat(controller.previewTargetSize).isEqualTo(targetSizeWithAspectRatio)

        val config = controller.mPreview.currentConfig as ImageOutputConfig
        assertThat(config.targetAspectRatio).isEqualTo(targetSizeWithAspectRatio.aspectRatio)
    }

    @UiThreadTest
    @Test
    @Suppress("deprecation")
    fun setPreviewResolution() {
        controller.previewTargetSize = targetSizeWithResolution
        assertThat(controller.previewTargetSize).isEqualTo(targetSizeWithResolution)

        val config = controller.mPreview.currentConfig as ImageOutputConfig
        assertThat(config.targetResolution).isEqualTo(targetSizeWithResolution.resolution)
    }

    @UiThreadTest
    @Test
    @Suppress("deprecation")
    fun setAnalysisAspectRatio() {
        controller.imageAnalysisTargetSize = targetSizeWithAspectRatio
        assertThat(controller.imageAnalysisTargetSize).isEqualTo(targetSizeWithAspectRatio)

        val config = controller.mImageAnalysis.currentConfig as ImageOutputConfig
        assertThat(config.targetAspectRatio).isEqualTo(targetSizeWithAspectRatio.aspectRatio)
    }

    @UiThreadTest
    @Test
    fun setAnalysisBackgroundExecutor() {
        val executor = Executors.newSingleThreadExecutor()
        controller.imageAnalysisBackgroundExecutor = executor
        assertThat(controller.imageAnalysisBackgroundExecutor).isEqualTo(executor)
        val config = controller.mImageAnalysis.currentConfig as ImageAnalysisConfig
        assertThat(config.backgroundExecutor).isEqualTo(executor)
    }

    @UiThreadTest
    @Test
    fun setAnalysisQueueDepth() {
        controller.imageAnalysisImageQueueDepth = 100
        assertThat(controller.imageAnalysisImageQueueDepth).isEqualTo(100)
        assertThat(controller.mImageAnalysis.imageQueueDepth).isEqualTo(100)
    }

    @UiThreadTest
    @Test
    fun setAnalysisBackpressureStrategy() {
        controller.imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_BLOCK_PRODUCER
        assertThat(controller.imageAnalysisBackpressureStrategy)
            .isEqualTo(ImageAnalysis.STRATEGY_BLOCK_PRODUCER)
        assertThat(controller.mImageAnalysis.backpressureStrategy)
            .isEqualTo(ImageAnalysis.STRATEGY_BLOCK_PRODUCER)
    }

    @UiThreadTest
    @Test
    @Suppress("deprecation")
    fun setImageCaptureResolution() {
        controller.imageCaptureTargetSize = targetSizeWithResolution
        assertThat(controller.imageCaptureTargetSize).isEqualTo(targetSizeWithResolution)

        val config = controller.mImageCapture.currentConfig as ImageOutputConfig
        assertThat(config.targetResolution).isEqualTo(targetSizeWithResolution.resolution)
    }

    @UiThreadTest
    @Test
    @Suppress("deprecation")
    fun setImageCaptureAspectRatio() {
        controller.imageCaptureTargetSize = targetSizeWithAspectRatio
        assertThat(controller.imageCaptureTargetSize).isEqualTo(targetSizeWithAspectRatio)

        val config = controller.mImageCapture.currentConfig as ImageOutputConfig
        assertThat(config.targetAspectRatio).isEqualTo(targetSizeWithAspectRatio.aspectRatio)
    }

    @UiThreadTest
    @Test
    fun setImageCaptureMode() {
        controller.imageCaptureMode = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
        assertThat(controller.imageCaptureMode)
            .isEqualTo(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
        assertThat(controller.mImageCapture.captureMode)
            .isEqualTo(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
    }

    @UiThreadTest
    @Test
    fun setImageCaptureIoExecutor() {
        val ioExecutor = Executors.newSingleThreadExecutor()
        controller.imageCaptureIoExecutor = ioExecutor
        assertThat(controller.imageCaptureIoExecutor).isEqualTo(ioExecutor)
        val config = controller.mImageCapture.currentConfig as ImageCaptureConfig
        assertThat(config.ioExecutor).isEqualTo(ioExecutor)
    }

    @UiThreadTest
    @Test
    fun setVideoCaptureQuality() {
        val qualitySelector = QualitySelector.from(targetVideoQuality)
        controller.videoCaptureQualitySelector = qualitySelector
        assertThat(controller.videoCaptureQualitySelector).isEqualTo(qualitySelector)
    }

    @UiThreadTest
    @Test
    fun setVideoCaptureMirrorMode() {
        controller.videoCaptureMirrorMode = MirrorMode.MIRROR_MODE_ON_FRONT_ONLY
        assertThat(controller.videoCaptureMirrorMode)
            .isEqualTo(MirrorMode.MIRROR_MODE_ON_FRONT_ONLY)
        assertThat(controller.mVideoCapture.mirrorMode)
            .isEqualTo(MirrorMode.MIRROR_MODE_ON_FRONT_ONLY)
    }

    @UiThreadTest
    @Test
    fun setVideoCaptureDynamicRange() {
        controller.videoCaptureDynamicRange = DynamicRange.HDR10_10_BIT
        assertThat(controller.videoCaptureDynamicRange).isEqualTo(DynamicRange.HDR10_10_BIT)
        assertThat(controller.mVideoCapture.dynamicRange).isEqualTo(DynamicRange.HDR10_10_BIT)
    }

    @UiThreadTest
    @Test
    fun setPreviewDynamicRange() {
        controller.previewDynamicRange = DynamicRange.HDR10_10_BIT
        assertThat(controller.previewDynamicRange).isEqualTo(DynamicRange.HDR10_10_BIT)
        assertThat(controller.mPreview.dynamicRange).isEqualTo(DynamicRange.HDR10_10_BIT)
    }

    @UiThreadTest
    @Test
    fun setVideoCaptureFrameRate() {
        controller.videoCaptureTargetFrameRate = Range.create(60, 120)
        assertThat(controller.videoCaptureTargetFrameRate).isEqualTo(Range.create(60, 120))
        assertThat(controller.mVideoCapture.targetFrameRate).isEqualTo(Range.create(60, 120))
    }

    @UiThreadTest
    @Test
    fun sensorRotationChanges_useCaseTargetRotationUpdated() {
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
    fun setSelectorBeforeBound_selectorSet() {
        // Arrange.
        assertThat(controller.cameraSelector.lensFacing).isEqualTo(CameraSelector.LENS_FACING_BACK)

        // Act.
        controller.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        // Assert.
        assertThat(controller.cameraSelector.lensFacing).isEqualTo(CameraSelector.LENS_FACING_FRONT)
    }

    @Test
    fun throwsException_whenScreenFlashModeSetWithBackCamera() {
        controller.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        Assert.assertThrows(IllegalArgumentException::class.java) {
            controller.imageCaptureFlashMode = ImageCapture.FLASH_MODE_SCREEN
        }
    }

    @Test
    fun canSetScreenFlashMode_whenScreenFlashUiInfoNotSetYet() {
        controller.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        controller.imageCaptureFlashMode = ImageCapture.FLASH_MODE_SCREEN

        assertThat(controller.imageCaptureFlashMode).isEqualTo(ImageCapture.FLASH_MODE_SCREEN)
    }

    @Test
    fun canTakePictureWithScreenFlashMode_whenFrontCameraAndScreenFlashUiInfoSet() {
        controller.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        controller.setScreenFlashUiInfo(
            ScreenFlashUiInfo(
                ScreenFlashUiInfo.ProviderType.SCREEN_FLASH_VIEW,
                object : ScreenFlash {
                    override fun apply(
                        expirationTimeMillis: Long,
                        screenFlashListener: ImageCapture.ScreenFlashListener,
                    ) {
                        screenFlashListener.onCompleted()
                    }

                    override fun clear() {}
                },
            )
        )

        controller.imageCaptureFlashMode = ImageCapture.FLASH_MODE_SCREEN
        completeCameraInitialization()

        controller.takePicture(
            MoreExecutors.directExecutor(),
            object : ImageCapture.OnImageCapturedCallback() {},
        )

        // ensure FLASH_MODE_SCREEN was retained
        assertThat(controller.imageCaptureFlashMode).isEqualTo(ImageCapture.FLASH_MODE_SCREEN)
    }

    @Test
    fun throwException_whenTakePictureWithScreenFlashModeButWithoutScreenFlashUiInfo() {
        controller.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        controller.imageCaptureFlashMode = ImageCapture.FLASH_MODE_SCREEN
        completeCameraInitialization()

        Assert.assertThrows(IllegalStateException::class.java) {
            controller.takePicture(
                MoreExecutors.directExecutor(),
                object : ImageCapture.OnImageCapturedCallback() {},
            )
        }
    }

    @Test
    fun throwsException_whenSwitchToBackCameraAfterScreenFlashSetToFrontCamera() {
        controller.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        controller.imageCaptureFlashMode = ImageCapture.FLASH_MODE_SCREEN

        Assert.assertThrows(IllegalStateException::class.java) {
            controller.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    @UiThreadTest
    @Test
    fun preview_surfaceProviderIsPreserved_afterRebind() {
        // Arrange.
        val surfaceProvider = SurfaceProvider {}
        controller.attachPreviewSurface(surfaceProvider, fakeViewPort)

        // Act: Setting a different resolution selector triggers a rebinding.
        controller.previewResolutionSelector = resolutionSelector

        // Assert.
        assertThat(controller.mPreview.surfaceProvider).isSameInstanceAs(surfaceProvider)
    }

    @UiThreadTest
    @Test
    fun imageCapture_flashModeIsPreserved_afterRebind() {
        // Arrange.
        controller.imageCaptureFlashMode = FLASH_MODE_ON

        // Act: Setting a different resolution selector triggers a rebinding.
        controller.imageCaptureResolutionSelector = resolutionSelector

        // Assert.
        assertThat(controller.imageCaptureFlashMode).isEqualTo(FLASH_MODE_ON)
    }

    @Suppress("deprecation")
    @Test
    fun setResolutionSelectorAndOutputSizeAtTheSameTime() {
        // Arrange & Act: Set resolution selector and target size together.
        controller.previewResolutionSelector = resolutionSelector
        controller.imageCaptureResolutionSelector = resolutionSelector
        controller.imageAnalysisResolutionSelector = resolutionSelector
        controller.previewTargetSize = targetSizeWithResolution
        controller.imageCaptureTargetSize = targetSizeWithResolution
        controller.imageAnalysisTargetSize = targetSizeWithResolution

        // Assert: The resolution selector should be set, while the target resolution should not.
        val previewConfig = controller.mPreview.currentConfig as ImageOutputConfig
        assertThat(previewConfig.resolutionSelector).isEqualTo(resolutionSelector)
        assertThat(previewConfig.getTargetResolution(null)).isNull()
        val imageCaptureConfig = controller.mImageCapture.currentConfig as ImageOutputConfig
        assertThat(imageCaptureConfig.resolutionSelector).isEqualTo(resolutionSelector)
        assertThat(imageCaptureConfig.getTargetResolution(null)).isNull()
        val imageAnalysisConfig = controller.mImageAnalysis.currentConfig as ImageOutputConfig
        assertThat(imageAnalysisConfig.resolutionSelector).isEqualTo(resolutionSelector)
        assertThat(imageAnalysisConfig.getTargetResolution(null)).isNull()
    }

    @Test
    fun setViewport_overrideUseCasesAspectRatio() {
        // Arrange & Act: Set a 16:9 viewport.
        controller.attachPreviewSurface(
            {},
            ViewPort.Builder(Rational(9, 16), Surface.ROTATION_90).build(),
        )

        // Assert: The aspect ratio of the use case configs should be override by viewport,
        // which should be 16:9.
        val previewConfig = controller.mPreview.currentConfig as ImageOutputConfig
        assertThat(previewConfig.resolutionSelector.aspectRatioStrategy.preferredAspectRatio)
            .isEqualTo(RATIO_16_9)
        val imageCaptureConfig = controller.mImageCapture.currentConfig as ImageOutputConfig
        assertThat(imageCaptureConfig.resolutionSelector.aspectRatioStrategy.preferredAspectRatio)
            .isEqualTo(RATIO_16_9)
        val imageAnalysisConfig = controller.mImageAnalysis.currentConfig as ImageOutputConfig
        assertThat(imageAnalysisConfig.resolutionSelector.aspectRatioStrategy.preferredAspectRatio)
            .isEqualTo(RATIO_16_9)
        assertThat(controller.mVideoCapture.output.aspectRatio).isEqualTo(RATIO_16_9)
    }

    @Test
    fun setViewport_notOverrideUseCasesAspectRatioIfResolutionSelectorAlreadySet() {
        // Arrange: Set a 4:3 viewport.
        controller.attachPreviewSurface(
            {},
            ViewPort.Builder(Rational(4, 3), Surface.ROTATION_0).build(),
        )

        // Act: Explicitly set a 16:9 resolution selector.
        controller.previewResolutionSelector = resolutionSelector
        controller.imageCaptureResolutionSelector = resolutionSelector
        controller.imageAnalysisResolutionSelector = resolutionSelector
        controller.videoCaptureQualitySelector = QualitySelector.from(targetVideoQuality)

        // Assert: The aspect ratio of the use case configs should not be override.
        val previewConfig = controller.mPreview.currentConfig as ImageOutputConfig
        assertThat(previewConfig.resolutionSelector.aspectRatioStrategy.preferredAspectRatio)
            .isNotEqualTo(RATIO_4_3)
        val imageCaptureConfig = controller.mImageCapture.currentConfig as ImageOutputConfig
        assertThat(imageCaptureConfig.resolutionSelector.aspectRatioStrategy.preferredAspectRatio)
            .isNotEqualTo(RATIO_4_3)
        val imageAnalysisConfig = controller.mImageAnalysis.currentConfig as ImageOutputConfig
        assertThat(imageAnalysisConfig.resolutionSelector.aspectRatioStrategy.preferredAspectRatio)
            .isNotEqualTo(RATIO_4_3)
        assertThat(controller.mVideoCapture.output.aspectRatio).isNotEqualTo(RATIO_4_3)
    }

    @Suppress("deprecation")
    @Test
    fun setViewport_notOverrideUseCasesAspectRatioIfOutputSizeAlreadySet() {
        // Arrange: Set a 4:3 viewport.
        controller.attachPreviewSurface(
            {},
            ViewPort.Builder(Rational(4, 3), Surface.ROTATION_0).build(),
        )

        // Act: Explicitly set a 16:9 target size.
        controller.previewTargetSize = targetSizeWithAspectRatio
        controller.imageCaptureTargetSize = targetSizeWithAspectRatio
        controller.imageAnalysisTargetSize = targetSizeWithAspectRatio

        // Assert: The resolution selector should not exist in the config.
        val previewConfig = controller.mPreview.currentConfig as ImageOutputConfig
        assertThat(previewConfig.getResolutionSelector(null)).isNull()
        val imageCaptureConfig = controller.mImageCapture.currentConfig as ImageOutputConfig
        assertThat(imageCaptureConfig.getResolutionSelector(null)).isNull()
        val imageAnalysisConfig = controller.mImageAnalysis.currentConfig as ImageOutputConfig
        assertThat(imageAnalysisConfig.getResolutionSelector(null)).isNull()
    }

    @Test
    fun onTapToFocus_focusMeteringActionSubmittedToCamera() {
        completeCameraInitialization()

        controller.onTapToFocus(pointFactory, 0f, 0f)
        assertThat(fakeCameraControl.lastSubmittedFocusMeteringAction).isNotNull()
    }

    @Test
    fun getTapToFocusInfoState_defaultStateIsTapToFocusNotStarted() {
        assertThat(controller.tapToFocusInfoState.value?.focusState)
            .isEqualTo(TAP_TO_FOCUS_NOT_STARTED)
    }

    @Test
    fun getTapToFocusInfoState_tapPointIsSameAsOriginal_whenOnTapToFocusIsCalled() {
        val tapPoint = PointF(1F, 2F)
        completeCameraInitialization()

        controller.onTapToFocus(pointFactory, tapPoint.x, tapPoint.y)

        shadowOf(getMainLooper()).idle()
        assertThat(controller.tapToFocusInfoState.value?.tapPoint).isEqualTo(tapPoint)
    }

    @Test
    fun getTapToFocusInfoState_tapPointIsSameAsLastOne_whenOnTapToFocusIsCalledTwice() {
        completeCameraInitialization()

        controller.onTapToFocus(pointFactory, TAP_POINT_1.x, TAP_POINT_1.y)
        controller.onTapToFocus(pointFactory, TAP_POINT_2.x, TAP_POINT_2.y)

        shadowOf(getMainLooper()).idle()
        assertThat(controller.tapToFocusInfoState.value?.tapPoint).isEqualTo(TAP_POINT_2)
    }

    @Test
    fun getTapToFocusInfoState_stateIsTapToFocusStarted_whenOnTapToFocusIsCalled() {
        completeCameraInitialization()
        fakeCameraControl.disableFocusMeteringAutoComplete(true)

        controller.onTapToFocus(pointFactory, 0f, 0f)

        shadowOf(getMainLooper()).idle()
        assertThat(controller.tapToFocusInfoState.value?.focusState).isEqualTo(TAP_TO_FOCUS_STARTED)
    }

    @Suppress("DEPRECATION")
    @Test
    fun getTapToFocusState_stateIsTapToFocusStarted_whenOnTapToFocusIsCalled() {
        completeCameraInitialization()
        fakeCameraControl.disableFocusMeteringAutoComplete(true)

        controller.onTapToFocus(pointFactory, 0f, 0f)

        shadowOf(getMainLooper()).idle()
        assertThat(controller.tapToFocusState.value).isEqualTo(TAP_TO_FOCUS_STARTED)
    }

    @Test
    fun getTapToFocusInfoState_stateIsTapToFocusFocused_whenSuccessfulResultIsSubmitted() {
        completeCameraInitialization()
        fakeCameraControl.disableFocusMeteringAutoComplete(true)

        controller.onTapToFocus(pointFactory, 0f, 0f)
        fakeCameraControl.submitFocusMeteringResult(FocusMeteringResult.create(true))

        shadowOf(getMainLooper()).idle()
        assertThat(controller.tapToFocusInfoState.value?.focusState).isEqualTo(TAP_TO_FOCUS_FOCUSED)
    }

    @Suppress("DEPRECATION")
    @Test
    fun getTapToFocusState_stateIsTapToFocusFocused_whenSuccessfulResultIsSubmitted() {
        completeCameraInitialization()
        fakeCameraControl.disableFocusMeteringAutoComplete(true)

        controller.onTapToFocus(pointFactory, 0f, 0f)
        fakeCameraControl.submitFocusMeteringResult(FocusMeteringResult.create(true))

        shadowOf(getMainLooper()).idle()
        assertThat(controller.tapToFocusState.value).isEqualTo(TAP_TO_FOCUS_FOCUSED)
    }

    @Test
    fun getTapToFocusInfoState_stateIsTapToFocusNotFocused_whenUnsuccessfulResultIsSubmitted() {
        completeCameraInitialization()
        fakeCameraControl.disableFocusMeteringAutoComplete(true)

        controller.onTapToFocus(pointFactory, 0f, 0f)
        fakeCameraControl.submitFocusMeteringResult(FocusMeteringResult.create(false))

        shadowOf(getMainLooper()).idle()
        assertThat(controller.tapToFocusInfoState.value?.focusState)
            .isEqualTo(TAP_TO_FOCUS_NOT_FOCUSED)
    }

    @Suppress("DEPRECATION")
    @Test
    fun getTapToFocusState_stateIsTapToFocusNotFocused_whenUnsuccessfulResultIsSubmitted() {
        completeCameraInitialization()
        fakeCameraControl.disableFocusMeteringAutoComplete(true)

        controller.onTapToFocus(pointFactory, 0f, 0f)
        fakeCameraControl.submitFocusMeteringResult(FocusMeteringResult.create(false))

        shadowOf(getMainLooper()).idle()
        assertThat(controller.tapToFocusState.value).isEqualTo(TAP_TO_FOCUS_NOT_FOCUSED)
    }

    @Test
    fun getTapToFocusInfoState_stateIsTapToFocusStarted_beforeAutoCancelDurationIsElapsed() {
        completeCameraInitialization()
        fakeCameraControl.disableFocusMeteringAutoComplete(true)

        controller.onTapToFocus(pointFactory, 0f, 0f)
        ShadowSystemClock.advanceBy(
            FOCUS_AUTO_CANCEL_DEFAULT_TIMEOUT_MILLIS - 1,
            TimeUnit.MILLISECONDS,
        )

        shadowOf(getMainLooper()).idle()
        assertThat(controller.tapToFocusInfoState.value?.focusState).isEqualTo(TAP_TO_FOCUS_STARTED)
    }

    @Suppress("DEPRECATION")
    @Test
    fun getTapToFocusState_stateIsTapToFocusStarted_beforeAutoCancelDurationIsElapsed() {
        completeCameraInitialization()
        fakeCameraControl.disableFocusMeteringAutoComplete(true)

        controller.onTapToFocus(pointFactory, 0f, 0f)
        ShadowSystemClock.advanceBy(
            FOCUS_AUTO_CANCEL_DEFAULT_TIMEOUT_MILLIS - 1,
            TimeUnit.MILLISECONDS,
        )

        shadowOf(getMainLooper()).idle()
        assertThat(controller.tapToFocusState.value).isEqualTo(TAP_TO_FOCUS_STARTED)
    }

    @Test
    fun getTapToFocusInfoState_stateIsNotStarted_whenAutoCancelDurationIsElapsedAfterStarted() {
        completeCameraInitialization()
        fakeCameraControl.disableFocusMeteringAutoComplete(true)

        controller.onTapToFocus(pointFactory, 0f, 0f)

        shadowOf(getMainLooper()).idle()
        assumeTrue(controller.tapToFocusInfoState.value?.focusState == TAP_TO_FOCUS_STARTED)

        ShadowSystemClock.advanceBy(FOCUS_AUTO_CANCEL_DEFAULT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)

        shadowOf(getMainLooper()).idle()
        assertThat(controller.tapToFocusInfoState.value?.focusState)
            .isEqualTo(TAP_TO_FOCUS_NOT_STARTED)
    }

    @Suppress("DEPRECATION")
    @Test
    fun getTapToFocusState_stateIsNotStarted_whenAutoCancelDurationIsElapsedAfterStarted() {
        completeCameraInitialization()
        fakeCameraControl.disableFocusMeteringAutoComplete(true)

        controller.onTapToFocus(pointFactory, 0f, 0f)

        shadowOf(getMainLooper()).idle()
        assumeTrue(controller.tapToFocusState.value == TAP_TO_FOCUS_STARTED)

        ShadowSystemClock.advanceBy(FOCUS_AUTO_CANCEL_DEFAULT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)

        shadowOf(getMainLooper()).idle()
        assertThat(controller.tapToFocusState.value).isEqualTo(TAP_TO_FOCUS_NOT_STARTED)
    }

    @Test
    fun getTapToFocusInfoState_stateIsNotStarted_whenAutoCancelDurationIsElapsedAfterFocused() {
        completeCameraInitialization()
        fakeCameraControl.disableFocusMeteringAutoComplete(true)

        controller.onTapToFocus(pointFactory, 0f, 0f)
        fakeCameraControl.submitFocusMeteringResult(FocusMeteringResult.create(true))
        shadowOf(getMainLooper()).idle()
        assumeTrue(controller.tapToFocusInfoState.value?.focusState == TAP_TO_FOCUS_FOCUSED)

        ShadowSystemClock.advanceBy(FOCUS_AUTO_CANCEL_DEFAULT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)

        shadowOf(getMainLooper()).idle()
        assertThat(controller.tapToFocusInfoState.value?.focusState)
            .isEqualTo(TAP_TO_FOCUS_NOT_STARTED)
    }

    @Suppress("DEPRECATION")
    @Test
    fun getTapToFocusState_stateIsNotStarted_whenAutoCancelDurationIsElapsedAfterFocused() {
        completeCameraInitialization()
        fakeCameraControl.disableFocusMeteringAutoComplete(true)

        controller.onTapToFocus(pointFactory, 0f, 0f)
        fakeCameraControl.submitFocusMeteringResult(FocusMeteringResult.create(true))
        shadowOf(getMainLooper()).idle()
        assumeTrue(controller.tapToFocusState.value == TAP_TO_FOCUS_FOCUSED)

        ShadowSystemClock.advanceBy(FOCUS_AUTO_CANCEL_DEFAULT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)

        shadowOf(getMainLooper()).idle()
        assertThat(controller.tapToFocusState.value).isEqualTo(TAP_TO_FOCUS_NOT_STARTED)
    }

    @Test
    fun getTapToFocusInfoState_stateIsNotStarted_whenAutoCancelDurationIsElapsedAfterNotFocused() {
        completeCameraInitialization()
        fakeCameraControl.disableFocusMeteringAutoComplete(true)

        controller.onTapToFocus(pointFactory, 0f, 0f)
        fakeCameraControl.submitFocusMeteringResult(FocusMeteringResult.create(false))
        shadowOf(getMainLooper()).idle()
        assumeTrue(controller.tapToFocusInfoState.value?.focusState == TAP_TO_FOCUS_NOT_FOCUSED)

        ShadowSystemClock.advanceBy(FOCUS_AUTO_CANCEL_DEFAULT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)

        shadowOf(getMainLooper()).idle()
        assertThat(controller.tapToFocusInfoState.value?.focusState)
            .isEqualTo(TAP_TO_FOCUS_NOT_STARTED)
    }

    @Suppress("DEPRECATION")
    @Test
    fun getTapToFocusState_stateIsNotStarted_whenAutoCancelDurationIsElapsedAfterNotFocused() {
        completeCameraInitialization()
        fakeCameraControl.disableFocusMeteringAutoComplete(true)

        controller.onTapToFocus(pointFactory, 0f, 0f)
        fakeCameraControl.submitFocusMeteringResult(FocusMeteringResult.create(false))
        shadowOf(getMainLooper()).idle()
        assumeTrue(controller.tapToFocusState.value == TAP_TO_FOCUS_NOT_FOCUSED)

        ShadowSystemClock.advanceBy(FOCUS_AUTO_CANCEL_DEFAULT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)

        shadowOf(getMainLooper()).idle()
        assertThat(controller.tapToFocusState.value).isEqualTo(TAP_TO_FOCUS_NOT_STARTED)
    }

    @Test
    fun setTapToFocusAutoCancelDuration_stateIsNotStarted_whenSetDurationIsElapsedAfterStarted() {
        val autoCancelDurationSeconds = 2L

        completeCameraInitialization()
        fakeCameraControl.disableFocusMeteringAutoComplete(true)

        controller.setTapToFocusAutoCancelDuration(autoCancelDurationSeconds, TimeUnit.SECONDS)

        controller.onTapToFocus(pointFactory, 0f, 0f)

        // Ensure focus state has is not the initial NOT_STARTED state.
        shadowOf(getMainLooper()).idle()
        assumeTrue(controller.tapToFocusInfoState.value?.focusState == TAP_TO_FOCUS_STARTED)

        ShadowSystemClock.advanceBy(autoCancelDurationSeconds, TimeUnit.SECONDS)

        shadowOf(getMainLooper()).idle()
        assertThat(controller.tapToFocusInfoState.value?.focusState)
            .isEqualTo(TAP_TO_FOCUS_NOT_STARTED)
    }

    @Ignore // b/425365173
    @Test
    fun setTapToFocusAutoCancelDuration_stateNeverResetsToNotStarted_whenDurationIsZero() {
        completeCameraInitialization()
        fakeCameraControl.disableFocusMeteringAutoComplete(true)

        controller.setTapToFocusAutoCancelDuration(0, TimeUnit.SECONDS)

        controller.onTapToFocus(pointFactory, 0f, 0f)

        // Ensure focus state has is not the initial NOT_STARTED state.
        shadowOf(getMainLooper()).idle()
        assumeTrue(controller.tapToFocusInfoState.value?.focusState == TAP_TO_FOCUS_STARTED)

        // Advance to 1 hour of time
        ShadowSystemClock.advanceBy(1, TimeUnit.HOURS)

        // State is still the previous STARTED state
        shadowOf(getMainLooper()).idle()
        assertThat(controller.tapToFocusInfoState.value?.focusState).isEqualTo(TAP_TO_FOCUS_STARTED)
    }

    @Test
    fun getTapToFocusInfoState_stateIsStartedAfterAutoCancelTimeOfFirstTap_whenTappedTwice() {
        completeCameraInitialization()
        fakeCameraControl.disableFocusMeteringAutoComplete(true)
        val tapInterval = FOCUS_AUTO_CANCEL_DEFAULT_TIMEOUT_MILLIS / 2

        controller.onTapToFocus(pointFactory, TAP_POINT_1.x, TAP_POINT_1.y)

        // Advance the clock by `tapInterval` to that first tap is supposed to be auto-canceled at
        // FOCUS_AUTO_CANCEL_DEFAULT_TIMEOUT_MILLIS while 2nd tap is supposed to be canceled at
        // FOCUS_AUTO_CANCEL_DEFAULT_TIMEOUT_MILLIS + tapInterval
        ShadowSystemClock.advanceBy(tapInterval, TimeUnit.MILLISECONDS)
        controller.onTapToFocus(pointFactory, TAP_POINT_2.x, TAP_POINT_2.y)

        // Advance the clock to the 1st tap cancellation time by advancing by the remaining time.
        ShadowSystemClock.advanceBy(
            FOCUS_AUTO_CANCEL_DEFAULT_TIMEOUT_MILLIS - tapInterval,
            TimeUnit.MILLISECONDS,
        )

        shadowOf(getMainLooper()).idle()
        assertThat(controller.tapToFocusInfoState.value?.focusState).isEqualTo(TAP_TO_FOCUS_STARTED)
    }

    @Test
    fun getTapToFocusInfoState_stateIsNotStartedAfterAutoCancelTimeOfSecondTap_whenTappedTwice() {
        completeCameraInitialization()
        fakeCameraControl.disableFocusMeteringAutoComplete(true)
        val tapInterval = FOCUS_AUTO_CANCEL_DEFAULT_TIMEOUT_MILLIS / 2

        controller.onTapToFocus(pointFactory, TAP_POINT_1.x, TAP_POINT_1.y)

        // Ensure focus state has is not the initial NOT_STARTED state.
        shadowOf(getMainLooper()).idle()
        assumeTrue(controller.tapToFocusInfoState.value?.focusState == TAP_TO_FOCUS_STARTED)

        // Advance the clock by `tapInterval` to that first tap is supposed to be auto-canceled at
        // FOCUS_AUTO_CANCEL_DEFAULT_TIMEOUT_MILLIS while 2nd tap is supposed to be canceled at
        // FOCUS_AUTO_CANCEL_DEFAULT_TIMEOUT_MILLIS + tapInterval
        ShadowSystemClock.advanceBy(tapInterval, TimeUnit.MILLISECONDS)
        controller.onTapToFocus(pointFactory, TAP_POINT_2.x, TAP_POINT_2.y)

        // Advance the clock to the 2nd tap cancellation time.
        ShadowSystemClock.advanceBy(FOCUS_AUTO_CANCEL_DEFAULT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)

        shadowOf(getMainLooper()).idle()
        assertThat(controller.tapToFocusInfoState.value?.focusState)
            .isEqualTo(TAP_TO_FOCUS_NOT_STARTED)
    }
}
