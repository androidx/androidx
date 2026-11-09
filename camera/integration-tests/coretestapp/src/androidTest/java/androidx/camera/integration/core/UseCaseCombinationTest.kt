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

import android.Manifest
import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.os.Build
import android.util.Rational
import android.view.Surface
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.compat.quirk.LowLightBoostStreamUseCaseQuirk
import androidx.camera.camera2.internal.StreamUseCaseUtil.STREAM_USE_CASE_STREAM_SPEC_OPTION
import androidx.camera.camera2.interop.applyCamera2InteropAsync
import androidx.camera.camera2.interop.camera2Interop
import androidx.camera.core.AspectRatio
import androidx.camera.core.AspectRatio.Ratio
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraX
import androidx.camera.core.CameraXConfig
import androidx.camera.core.DynamicRange
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.TorchState
import androidx.camera.core.UseCase
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.StreamUseCase
import androidx.camera.core.impl.utils.AspectRatioUtil
import androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.IgnoreVideoRecordingProblematicDeviceRule.Companion.skipVideoRecordingTestIfNotSupportedByEmulator
import androidx.camera.testing.impl.SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider
import androidx.camera.testing.impl.WakelockEmptyActivityRule
import androidx.camera.testing.impl.WakelockRule
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.mocks.MockScreenFlash
import androidx.camera.testing.impl.video.AudioChecker
import androidx.camera.testing.impl.video.RecordingSession
import androidx.camera.testing.impl.video.RecordingSession.Companion.DEFAULT_VERIFY_STATUS_COUNT
import androidx.camera.testing.impl.video.RecordingSession.Companion.DEFAULT_VERIFY_STATUS_TIMEOUT_MS
import androidx.camera.testing.impl.video.RecordingSession.Companion.DEFAULT_VERIFY_TIMEOUT_MS
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.Quality.ConstantQuality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapabilities
import androidx.camera.video.VideoCapture
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Contains tests for [CameraX] which varies use case combinations to run. */
@LargeTest
@RunWith(Parameterized::class)
class UseCaseCombinationTest(
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

    @get:Rule
    val temporaryFolder =
        TemporaryFolder(ApplicationProvider.getApplicationContext<Context>().cacheDir)

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    @get:Rule
    val wakelockRule: TestRule =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            WakelockRule()
        } else {
            WakelockEmptyActivityRule()
        }

    companion object {
        private const val FRAME_TIMEOUT_MS = 5000L
        private const val NO_FRAME_TIMEOUT_MS = 1000L
        // Preview frames to wait for after a mode switch, so that the frames of the previous
        // repeating request that were still in flight are delivered.
        private const val DRAIN_FRAME_COUNT = 10

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
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var fakeLifecycleOwner: FakeLifecycleOwner
    private lateinit var camera: Camera
    private lateinit var previewMonitor: PreviewMonitor
    private lateinit var preview: Preview
    private lateinit var imageCapture: ImageCapture
    private lateinit var imageAnalysisMonitor: AnalysisMonitor
    private lateinit var imageAnalysis: ImageAnalysis
    private lateinit var videoCapture: VideoCapture<Recorder>
    private lateinit var recordingSession: RecordingSession
    private lateinit var cameraInfo: CameraInfo
    private lateinit var videoCapabilities: VideoCapabilities

    private val audioStreamAvailable by lazy { AudioChecker.canAudioStreamBeStarted() }

    @Before
    fun initializeCameraX() {
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
        imageCapture = initImageCapture()
        imageAnalysisMonitor = AnalysisMonitor()
        imageAnalysis = initImageAnalysis(imageAnalysisMonitor)

        videoCapabilities = Recorder.getVideoCapabilities(cameraInfo)
    }

    @After
    fun shutdownCameraX() {
        if (this::recordingSession.isInitialized) {
            recordingSession.release(timeoutMs = 5000)
        }
        if (::cameraProvider.isInitialized) {
            cameraProvider.shutdownAsync()[10, TimeUnit.SECONDS]
        }
    }

    /** Test Combination: Preview + ImageCapture */
    @Test
    fun previewCombinesImageCapture() {
        // Arrange.
        checkAndBindUseCases(preview, imageCapture)

        // Assert.
        imageCapture.waitForCapturing()
        previewMonitor.waitForStream()
    }

    /** Test Combination: Preview (no surface provider) + ImageCapture */
    @Test
    fun previewCombinesImageCapture_withNoSurfaceProvider() {
        // Arrange.
        preview = initPreview(previewMonitor, /* setSurfaceProvider= */ false)
        checkAndBindUseCases(preview, imageCapture)

        // Assert.
        imageCapture.waitForCapturing()
        previewMonitor.assertNoResultReceived()
    }

    /** Test Combination: Preview + ImageAnalysis */
    @Test
    fun previewCombinesImageAnalysis() {
        // Arrange.
        checkAndBindUseCases(preview, imageAnalysis)

        // Assert.
        previewMonitor.waitForStream()
        imageAnalysisMonitor.waitForImageAnalysis()
    }

    /** Test Combination: Preview (no surface provider) + ImageAnalysis */
    @Test
    fun previewCombinesImageAnalysis_withNoSurfaceProvider() {
        // Arrange.
        preview = initPreview(previewMonitor, /* setSurfaceProvider= */ false)
        checkAndBindUseCases(preview, imageAnalysis)

        // Assert.
        previewMonitor.assertNoResultReceived()
        imageAnalysisMonitor.waitForImageAnalysis()
    }

    /** Test Combination: Preview + ImageAnalysis + ImageCapture */
    @Test
    fun previewCombinesImageAnalysisAndImageCapture() {
        // Arrange.
        checkAndBindUseCases(preview, imageCapture, imageAnalysis)

        // Assert.
        imageCapture.waitForCapturing()
        previewMonitor.waitForStream()
        imageAnalysisMonitor.waitForImageAnalysis()
    }

    @Test
    fun sequentialBindPreviewImageCaptureAndImageAnalysis() {
        // Arrange.
        assumeTrue(camera.isUseCasesCombinationSupported(preview, imageCapture, imageAnalysis))

        // Bind Preview and verify
        bindUseCases(preview)
        previewMonitor.waitForStream()

        // Bind additional ImageCapture and verify
        bindUseCases(preview, imageCapture)
        previewMonitor.waitForStream()
        imageCapture.waitForCapturing()

        // Bind additional ImageAnalysis and verify
        bindUseCases(preview, imageCapture, imageAnalysis)
        previewMonitor.waitForStream()
        imageCapture.waitForCapturing()
        imageAnalysisMonitor.waitForImageAnalysis()
    }

    @Test
    fun unbindImageAnalysis_captureAndPreviewStillWorking() {
        // Arrange.
        checkAndBindUseCases(preview, imageCapture, imageAnalysis)

        previewMonitor.waitForStream()
        imageAnalysisMonitor.waitForImageAnalysis()
        imageCapture.waitForCapturing()

        // Act.
        unbindUseCases(imageAnalysis)

        // Assert
        imageCapture.waitForCapturing()
        previewMonitor.waitForStream()
    }

    @Test
    fun unbindPreview_captureAndAnalysisStillWorking() {
        // Arrange.
        checkAndBindUseCases(preview, imageCapture, imageAnalysis)

        previewMonitor.waitForStream()
        imageAnalysisMonitor.waitForImageAnalysis()
        imageCapture.waitForCapturing()

        // Act.
        unbindUseCases(preview)
        previewMonitor.waitForStreamIdle()

        // Assert
        imageCapture.waitForCapturing()
        imageAnalysisMonitor.waitForImageAnalysis()
    }

    @Test
    fun unbindImageCapture_previewAndAnalysisStillWorking() {
        // Arrange.
        checkAndBindUseCases(preview, imageCapture, imageAnalysis)

        previewMonitor.waitForStream()
        imageAnalysisMonitor.waitForImageAnalysis()
        imageCapture.waitForCapturing()

        // Act.
        unbindUseCases(imageCapture)

        // Assert
        imageAnalysisMonitor.waitForImageAnalysis()
        previewMonitor.waitForStream()
    }

    @Test
    fun previewCombinesVideoCapture() {
        // Arrange.
        checkAndPrepareVideoCaptureSources()
        checkAndBindUseCases(preview, videoCapture)

        // Assert.
        previewMonitor.waitForStream()
        recordingSession.createRecording().recordAndVerify()
    }

    @Test
    fun previewCombinesVideoCaptureAndImageCapture_withoutRecording() {
        // Arrange.
        checkAndPrepareVideoCaptureSources()
        checkAndBindUseCases(preview, videoCapture, imageCapture)

        // Assert.
        previewMonitor.waitForStream()
        imageCapture.waitForCapturing()
    }

    @Test
    fun previewCombinesVideoCaptureAndImageCapture_withRecording() {
        // Arrange.
        checkAndPrepareVideoCaptureSources()
        checkAndBindUseCases(preview, videoCapture, imageCapture)

        // Assert.
        previewMonitor.waitForStream()
        recordingSession.createRecording().recordAndVerify()
        imageCapture.waitForCapturing()
    }

    @Test
    fun previewCombinesVideoCaptureAndFlashImageCapture_withoutRecording() {
        // Arrange.
        checkAndPrepareVideoCaptureSources()
        checkAndBindUseCases(preview, videoCapture, imageCapture)

        // Assert.
        previewMonitor.waitForStream()
        imageCapture.waitForCapturing(useFlash = true)
    }

    @Test
    fun previewCombinesVideoCaptureAndFlashImageCapture_withRecording() {
        // Arrange.
        checkAndPrepareVideoCaptureSources()
        checkAndBindUseCases(preview, videoCapture, imageCapture)

        // Assert.
        previewMonitor.waitForStream()
        recordingSession.createRecording().recordAndVerify()
        imageCapture.waitForCapturing(useFlash = true)
    }

    @Test
    fun previewCombinesVideoCaptureAndImageAnalysis() {
        // Arrange.
        checkAndPrepareVideoCaptureSources()
        checkAndBindUseCases(preview, videoCapture, imageAnalysis)

        // Assert.
        previewMonitor.waitForStream()
        recordingSession.createRecording().recordAndVerify()
        imageAnalysisMonitor.waitForImageAnalysis()
    }

    @Test
    fun previewCombinesVideoCaptureImageCaptureAndImageAnalysis() {
        // Arrange.
        checkAndPrepareVideoCaptureSources()
        checkAndBindUseCases(preview, videoCapture, imageCapture, imageAnalysis)

        // Assert.
        previewMonitor.waitForStream()
        recordingSession.createRecording().recordAndVerify()
        imageCapture.waitForCapturing()
        imageAnalysisMonitor.waitForImageAnalysis()
    }

    @Test
    fun sequentialBindPreviewVideoCaptureImageCaptureAndImageAnalysis() {
        // Arrange.
        checkAndPrepareVideoCaptureSources()
        assumeTrue(
            camera.isUseCasesCombinationSupported(
                preview,
                imageCapture,
                imageAnalysis,
                videoCapture,
            )
        )

        // Bind Preview and verify
        bindUseCases(preview)
        previewMonitor.waitForStream()

        // Bind additional VideoCapture and Verify
        bindUseCases(preview, videoCapture)
        previewMonitor.waitForStream()
        recordingSession.createRecording().recordAndVerify()

        // Bind additional VideoCapture and Verify
        bindUseCases(preview, videoCapture, imageCapture)
        previewMonitor.waitForStream()
        recordingSession.createRecording().recordAndVerify()
        imageCapture.waitForCapturing()

        // Bind additional ImageAnalysis and Verify
        bindUseCases(preview, videoCapture, imageCapture, imageAnalysis)
        previewMonitor.waitForStream()
        recordingSession.createRecording().recordAndVerify()
        imageCapture.waitForCapturing()
        imageAnalysisMonitor.waitForImageAnalysis()
    }

    // Preview + ImageCapture -> Preview + VideoCapture -> Preview + ImageCapture
    @Test
    fun switchImageCaptureVideoCaptureWithTwoUseCasesBound() {
        // Arrange.
        checkAndPrepareVideoCaptureSources()
        assumeTrue(camera.isUseCasesCombinationSupported(preview, imageCapture, videoCapture))

        bindUseCases(preview, imageCapture)
        previewMonitor.waitForStream()
        imageCapture.waitForCapturing()

        // Unbind ImageCapture and switches to VideoCapture
        unbindUseCases(imageCapture)
        bindUseCases(preview, videoCapture)
        previewMonitor.waitForStream()
        recordingSession.createRecording().recordAndVerify()

        // Unbind VideoCapture and switches back to ImageCapture
        unbindUseCases(videoCapture)
        bindUseCases(preview, imageCapture)
        previewMonitor.waitForStream()
        imageCapture.waitForCapturing()
    }

    // Preview + ImageCapture -> Preview + ImageCapture + VideoCapture -> Preview + ImageCapture
    @Test
    fun addVideoCaptureToPreviewAndImageCapture_thenRemove() {
        // Arrange.
        checkAndPrepareVideoCaptureSources()
        assumeTrue(camera.isUseCasesCombinationSupported(preview, imageCapture, videoCapture))

        bindUseCases(preview, imageCapture)
        previewMonitor.waitForStream()
        imageCapture.waitForCapturing()

        // Bind additional VideoCapture and verify
        bindUseCases(preview, imageCapture, videoCapture)
        previewMonitor.waitForStream()
        imageCapture.waitForCapturing()
        recordingSession.createRecording().recordAndVerify()

        // Unbind VideoCapture and verify
        unbindUseCases(videoCapture)
        previewMonitor.waitForStream()
        imageCapture.waitForCapturing()
    }

    // Possible for QR code scanning use case.
    @Test
    fun sequentialBindPreviewAndImageAnalysis() {
        bindUseCases(preview)
        previewMonitor.waitForStream()

        bindUseCases(preview, imageAnalysis)
        previewMonitor.waitForStream()
        imageAnalysisMonitor.waitForImageAnalysis()
    }

    @Test
    fun canRecordVideoAfterImageCaptureCompletes_whenAllUseCasesCombined() {
        // Arrange.
        checkAndPrepareVideoCaptureSources(
            verifyStatusCount = 30, // records video for around 1s assuming 30 FPS,
            verifyTimeoutMs = 8000L, // increased timeout for higher status count
            verifyStatusTimeoutMs = 18000L,
        )
        checkAndBindUseCases(preview, videoCapture, imageCapture, imageAnalysis)

        // Act & assert.
        imageCapture.waitForCapturing()
        recordingSession.createRecording().recordAndVerify()
    }

    @Test
    fun canRecordVideoAfterTwoImageCapturesRequested_whenAllUseCasesCombined() {
        // Arrange.
        checkAndPrepareVideoCaptureSources(
            verifyStatusCount = 60, // records video for around 2s assuming 30 FPS,
            verifyTimeoutMs = 10000L, // increased timeout for higher status count
            verifyStatusTimeoutMs = 20000L,
        )
        checkAndBindUseCases(preview, videoCapture, imageCapture, imageAnalysis)

        // Act & assert.
        val callback1 = imageCapture.triggerCapturing()
        val callback2 = imageCapture.triggerCapturing()
        recordingSession.createRecording().recordAndVerify()
        callback1.verifyCapture()
        callback2.verifyCapture()
        imageCapture.waitForCapturing()
    }

    @Ignore("b/415195621")
    @Test
    fun canCaptureImageIfSupported_whenAllUseCasesCombinedWithExactly4x3SdAndNoFallback() {
        // Arrange.
        val quality = Quality.SD as ConstantQuality
        assumeTrue(videoCapabilities.getSupportedQualities(DynamicRange.SDR).contains(quality))
        val qualitySize =
            quality.getTypicalSizes().first {
                AspectRatioUtil.hasMatchingAspectRatio(it, Rational(4, 3))
            }
        assumeTrue("4:3 SD is not supported", qualitySize != null)
        checkAndPrepareVideoCaptureSources(
            verifyStatusCount = 30, // records video for around 1s assuming 30 FPS,
            verifyTimeoutMs = 8000L, // increased timeout for higher sftatus count
            verifyStatusTimeoutMs = 18000L,
            qualitySelector = QualitySelector.from(quality),
            aspectRatio = AspectRatio.RATIO_4_3,
        )
        val selector =
            ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        checkNotNull(qualitySize),
                        ResolutionStrategy.FALLBACK_RULE_NONE,
                    )
                )
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .build()
        val preview = initPreview(previewMonitor, resolutionSelector = selector)
        val imageCapture = initImageCapture(resolutionSelector = selector)
        val imageAnalysis = initImageAnalysis(imageAnalysisMonitor, resolutionSelector = selector)
        checkAndBindUseCases(preview, videoCapture, imageCapture, imageAnalysis)
        // Act & assert.
        imageCapture.waitForCapturing()
    }

    @Ignore("b/415195621")
    @Test
    fun canCaptureImageIfSupported_whenAllUseCasesCombinedWithExactlyHdAndNoFallback() {
        // Arrange.
        val quality = Quality.HD as ConstantQuality
        assumeTrue(videoCapabilities.getSupportedQualities(DynamicRange.SDR).contains(quality))
        checkAndPrepareVideoCaptureSources(
            verifyStatusCount = 30, // records video for around 1s assuming 30 FPS,
            verifyTimeoutMs = 8000L, // increased timeout for higher sftatus count
            verifyStatusTimeoutMs = 18000L,
            qualitySelector = QualitySelector.from(quality),
        )
        val selector =
            ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        quality.getTypicalSizes().first(),
                        ResolutionStrategy.FALLBACK_RULE_NONE,
                    )
                )
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                .build()
        val preview = initPreview(previewMonitor, resolutionSelector = selector)
        val imageCapture = initImageCapture(resolutionSelector = selector)
        val imageAnalysis = initImageAnalysis(imageAnalysisMonitor, resolutionSelector = selector)
        checkAndBindUseCases(preview, videoCapture, imageCapture, imageAnalysis)
        // Act & assert.
        imageCapture.waitForCapturing()
    }

    @Ignore("b/415195621")
    @Test
    fun canCaptureImageIfSupported_whenAllUseCasesCombinedWithExactlyFhdAndNoFallback() {
        // Arrange.
        val quality = Quality.FHD as ConstantQuality
        checkAndPrepareVideoCaptureSources(
            verifyStatusCount = 30, // records video for around 1s assuming 30 FPS,
            verifyTimeoutMs = 8000L, // increased timeout for higher sftatus count
            verifyStatusTimeoutMs = 18000L,
            qualitySelector = QualitySelector.from(quality),
        )
        val selector =
            ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        quality.getTypicalSizes().first(),
                        ResolutionStrategy.FALLBACK_RULE_NONE,
                    )
                )
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                .build()
        val preview = initPreview(previewMonitor, resolutionSelector = selector)
        val imageCapture = initImageCapture(resolutionSelector = selector)
        val imageAnalysis = initImageAnalysis(imageAnalysisMonitor, resolutionSelector = selector)
        checkAndBindUseCases(preview, videoCapture, imageCapture, imageAnalysis)
        // Act & assert.
        imageCapture.waitForCapturing()
    }

    @Ignore("b/415195621")
    @Test
    fun canCaptureImageIfSupported_whenAllUseCasesCombinedWithExactlyUhdAndNoFallback() {
        // Arrange.
        val quality = Quality.UHD as ConstantQuality
        assumeTrue(videoCapabilities.getSupportedQualities(DynamicRange.SDR).contains(quality))
        checkAndPrepareVideoCaptureSources(
            verifyStatusCount = 30, // records video for around 1s assuming 30 FPS,
            verifyTimeoutMs = 8000L, // increased timeout for higher sftatus count
            verifyStatusTimeoutMs = 18000L,
            qualitySelector = QualitySelector.from(quality),
        )
        val selector =
            ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        quality.getTypicalSizes().first(),
                        ResolutionStrategy.FALLBACK_RULE_NONE,
                    )
                )
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                .build()
        val preview = initPreview(previewMonitor, resolutionSelector = selector)
        val imageCapture = initImageCapture(resolutionSelector = selector)
        val imageAnalysis = initImageAnalysis(imageAnalysisMonitor, resolutionSelector = selector)
        checkAndBindUseCases(preview, videoCapture, imageCapture, imageAnalysis)
        // Act & assert.
        imageCapture.waitForCapturing()
    }

    @Ignore("b/415195621")
    @Test
    fun canCaptureImageIfSupported_whenPrevCaptureVideoCombinedWithExactly4x3SdAndNoFallback() {
        // Arrange.
        val quality = Quality.SD as ConstantQuality
        assumeTrue(videoCapabilities.getSupportedQualities(DynamicRange.SDR).contains(quality))
        val qualitySize =
            quality.getTypicalSizes().first {
                AspectRatioUtil.hasMatchingAspectRatio(it, Rational(4, 3))
            }
        assumeTrue("4:3 SD is not supported", qualitySize != null)
        checkAndPrepareVideoCaptureSources(
            verifyStatusCount = 30, // records video for around 1s assuming 30 FPS,
            verifyTimeoutMs = 8000L, // increased timeout for higher sftatus count
            verifyStatusTimeoutMs = 18000L,
            qualitySelector = QualitySelector.from(quality),
            aspectRatio = AspectRatio.RATIO_4_3,
        )
        val selector =
            ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        checkNotNull(qualitySize),
                        ResolutionStrategy.FALLBACK_RULE_NONE,
                    )
                )
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .build()
        val preview = initPreview(previewMonitor, resolutionSelector = selector)
        val imageCapture = initImageCapture(resolutionSelector = selector)
        checkAndBindUseCases(preview, videoCapture, imageCapture)
        // Act & assert.
        imageCapture.waitForCapturing()
    }

    @Ignore("b/415195621")
    @Test
    fun canCaptureImageIfSupported_whenPrevCaptureVideoCombinedWithExactlyHdAndNoFallback() {
        // Arrange.
        val quality = Quality.HD as ConstantQuality
        assumeTrue(videoCapabilities.getSupportedQualities(DynamicRange.SDR).contains(quality))
        checkAndPrepareVideoCaptureSources(
            verifyStatusCount = 30, // records video for around 1s assuming 30 FPS,
            verifyTimeoutMs = 8000L, // increased timeout for higher sftatus count
            verifyStatusTimeoutMs = 18000L,
            qualitySelector = QualitySelector.from(quality),
        )
        val selector =
            ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        quality.getTypicalSizes().first(),
                        ResolutionStrategy.FALLBACK_RULE_NONE,
                    )
                )
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                .build()
        val preview = initPreview(previewMonitor, resolutionSelector = selector)
        val imageCapture = initImageCapture(resolutionSelector = selector)
        checkAndBindUseCases(preview, videoCapture, imageCapture)
        // Act & assert.
        imageCapture.waitForCapturing()
    }

    @Ignore("b/415195621")
    @Test
    fun canCaptureImageIfSupported_whenPrevCaptureVideoCombinedWithExactlyFhdAndNoFallback() {
        // Arrange.
        val quality = Quality.FHD as ConstantQuality
        assumeTrue(videoCapabilities.getSupportedQualities(DynamicRange.SDR).contains(quality))
        checkAndPrepareVideoCaptureSources(
            verifyStatusCount = 30, // records video for around 1s assuming 30 FPS,
            verifyTimeoutMs = 8000L, // increased timeout for higher sftatus count
            verifyStatusTimeoutMs = 18000L,
            qualitySelector = QualitySelector.from(quality),
        )
        val selector =
            ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        quality.getTypicalSizes().first(),
                        ResolutionStrategy.FALLBACK_RULE_NONE,
                    )
                )
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                .build()
        val preview = initPreview(previewMonitor, resolutionSelector = selector)
        val imageCapture = initImageCapture(resolutionSelector = selector)
        checkAndBindUseCases(preview, videoCapture, imageCapture)
        // Act & assert.
        imageCapture.waitForCapturing()
    }

    @Ignore("b/415195621")
    @Test
    fun canCaptureImageIfSupported_whenPrevCaptureVideoCombinedWithExactlyUhdAndNoFallback() {
        // Arrange.
        val quality = Quality.UHD as ConstantQuality
        assumeTrue(videoCapabilities.getSupportedQualities(DynamicRange.SDR).contains(quality))
        checkAndPrepareVideoCaptureSources(
            verifyStatusCount = 30, // records video for around 1s assuming 30 FPS,
            verifyTimeoutMs = 8000L, // increased timeout for higher sftatus count
            verifyStatusTimeoutMs = 18000L,
            qualitySelector = QualitySelector.from(quality),
        )
        val selector =
            ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        quality.getTypicalSizes().first(),
                        ResolutionStrategy.FALLBACK_RULE_NONE,
                    )
                )
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                .build()
        val preview = initPreview(previewMonitor, resolutionSelector = selector)
        val imageCapture = initImageCapture(resolutionSelector = selector)
        checkAndBindUseCases(preview, videoCapture, imageCapture)
        // Act & assert.
        imageCapture.waitForCapturing()
    }

    @Test
    fun canCaptureImageIfSupported_whenPrevCaptureAnalysisCombinedWithExactly4x3SdAndNoFallback() {
        // Arrange.
        val quality = Quality.SD as ConstantQuality
        assumeTrue(videoCapabilities.getSupportedQualities(DynamicRange.SDR).contains(quality))
        val qualitySize =
            quality.getTypicalSizes().firstOrNull {
                AspectRatioUtil.hasMatchingAspectRatio(it, Rational(4, 3))
            }
        assumeTrue("4:3 SD is not supported", qualitySize != null)
        val selector =
            ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        checkNotNull(qualitySize),
                        ResolutionStrategy.FALLBACK_RULE_NONE,
                    )
                )
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .build()
        val preview = initPreview(previewMonitor, resolutionSelector = selector)
        val imageCapture = initImageCapture(resolutionSelector = selector)
        val imageAnalysis = initImageAnalysis(imageAnalysisMonitor, resolutionSelector = selector)
        checkAndBindUseCases(preview, imageCapture, imageAnalysis)
        // Act & assert.
        imageCapture.waitForCapturing()
    }

    @Test
    fun canCaptureImageIfSupported_whenPrevCaptureAnalysisCombinedWithExactlyHdAndNoFallback() {
        // Arrange.
        val quality = Quality.HD as ConstantQuality
        assumeTrue(videoCapabilities.getSupportedQualities(DynamicRange.SDR).contains(quality))
        val selector =
            ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        quality.getTypicalSizes().first(),
                        ResolutionStrategy.FALLBACK_RULE_NONE,
                    )
                )
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                .build()
        val preview = initPreview(previewMonitor, resolutionSelector = selector)
        val imageCapture = initImageCapture(resolutionSelector = selector)
        val imageAnalysis = initImageAnalysis(imageAnalysisMonitor, resolutionSelector = selector)
        checkAndBindUseCases(preview, imageCapture, imageAnalysis)
        // Act & assert.
        imageCapture.waitForCapturing()
    }

    @Test
    fun canCaptureImageIfSupported_whenPrevCaptureAnalysisCombinedWithExactlyFhdAndNoFallback() {
        // Arrange.
        val quality = Quality.FHD as ConstantQuality
        assumeTrue(videoCapabilities.getSupportedQualities(DynamicRange.SDR).contains(quality))
        val selector =
            ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        quality.getTypicalSizes().first(),
                        ResolutionStrategy.FALLBACK_RULE_NONE,
                    )
                )
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                .build()
        val preview = initPreview(previewMonitor, resolutionSelector = selector)
        val imageCapture = initImageCapture(resolutionSelector = selector)
        val imageAnalysis = initImageAnalysis(imageAnalysisMonitor, resolutionSelector = selector)
        checkAndBindUseCases(preview, imageCapture, imageAnalysis)
        // Act & assert.
        imageCapture.waitForCapturing()
    }

    @Test
    fun canCaptureImageIfSupported_whenPrevCaptureAnalysisCombinedWithExactlyUhdAndNoFallback() {
        // Arrange.
        val quality = Quality.UHD as ConstantQuality
        assumeTrue(videoCapabilities.getSupportedQualities(DynamicRange.SDR).contains(quality))
        val selector =
            ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        quality.getTypicalSizes().first(),
                        ResolutionStrategy.FALLBACK_RULE_NONE,
                    )
                )
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                .build()
        val preview = initPreview(previewMonitor, resolutionSelector = selector)
        val imageCapture = initImageCapture(resolutionSelector = selector)
        val imageAnalysis = initImageAnalysis(imageAnalysisMonitor, resolutionSelector = selector)
        checkAndBindUseCases(preview, imageCapture, imageAnalysis)
        // Act & assert.
        imageCapture.waitForCapturing()
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    fun canPopulateStreamUseCaseStreamSpecOption_sequentialBindFourUseCases() {
        assumeTrue(isStreamUseCaseSupported())
        // Arrange.
        assumeTrue(camera.isUseCasesCombinationSupported(preview, imageCapture, imageAnalysis))

        // Bind Preview and verify
        bindUseCases(preview)
        // PREVIEW_VIDEO_STILL is selected for single preview use case in the beginning
        // (or PREVIEW when LowLightBoostStreamUseCaseQuirk is active)
        verifyStreamSpecStreamUseCase(preview, expectedSinglePreviewStreamUseCase())

        // Bind additional ImageCapture and verify
        bindUseCases(preview, imageCapture)
        // PREVIEW is re-selected for preview use case after imageCapture is bound
        verifyStreamSpecStreamUseCase(preview, StreamUseCase.PREVIEW)
        verifyStreamSpecStreamUseCase(imageCapture, StreamUseCase.STILL_CAPTURE)

        // Bind additional ImageAnalysis and verify
        bindUseCases(preview, imageCapture, imageAnalysis)
        verifyStreamSpecStreamUseCase(preview, StreamUseCase.PREVIEW)
        verifyStreamSpecStreamUseCase(imageCapture, StreamUseCase.STILL_CAPTURE)
        verifyStreamSpecStreamUseCase(imageAnalysis, StreamUseCase.PREVIEW)

        checkAndPrepareVideoCaptureSources()
        assumeTrue(
            camera.isUseCasesCombinationSupported(
                preview,
                imageCapture,
                imageAnalysis,
                videoCapture,
            )
        )

        // Bind additional VideoCapture and verify
        bindUseCases(preview, imageCapture, imageAnalysis, videoCapture)
        verifyStreamSpecStreamUseCase(imageCapture, StreamUseCase.STILL_CAPTURE)
        verifyStreamSpecStreamUseCase(imageAnalysis, StreamUseCase.PREVIEW)
        // StreamSharing is enabled and preview and videoCapture stream use case will be null.
        verifyStreamSpecStreamUseCase(preview, null)
        verifyStreamSpecStreamUseCase(videoCapture, null)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    fun canPopulateStreamUseCaseStreamSpecOption_sequentialBindImageAnalysisAndPreview() {
        assumeTrue(isStreamUseCaseSupported())
        // Arrange.
        assumeTrue(camera.isUseCasesCombinationSupported(preview, imageAnalysis))

        // Bind ImageAnalysis and verify
        bindUseCases(imageAnalysis)
        // PREVIEW_VIDEO_STILL is selected for single imageAnalysis use case in the beginning
        verifyStreamSpecStreamUseCase(imageAnalysis, StreamUseCase.PREVIEW_VIDEO_STILL)

        // Bind additional Preview and verify
        bindUseCases(imageAnalysis, preview)
        // PREVIEW is re-selected for imageAnalysis use case after preview is bound
        verifyStreamSpecStreamUseCase(imageAnalysis, StreamUseCase.PREVIEW)
        verifyStreamSpecStreamUseCase(preview, StreamUseCase.PREVIEW)
    }

    /**
     * Verifies that [SessionConfig] with [SessionConfig.Builder.setInitialEnabledUseCases] and
     * [androidx.camera.core.CameraControl.setEnabledUseCasesAsync] switches between active use case
     * subsets by only updating the repeating request: the capture session is configured once,
     * [ImageAnalysis] only receives frames while enabled, [ImageCapture] captures photos when
     * enabled, and [Preview] keeps streaming.
     */
    @Test
    fun sessionConfig_setEnabledUseCasesSwitchesStreamsWithoutReconfiguringSession() {
        val sessionCounter = SessionConfiguredCounter()
        val repeatingFramesLatch = CountDownLatch(5)
        val stillIntentOnRepeatingCallbackCount = AtomicInteger(0)
        val repeatingCallback =
            object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult,
                ) {
                    repeatingFramesLatch.countDown()
                    if (
                        request.get(CaptureRequest.CONTROL_CAPTURE_INTENT) ==
                            CaptureRequest.CONTROL_CAPTURE_INTENT_STILL_CAPTURE
                    ) {
                        stillIntentOnRepeatingCallbackCount.incrementAndGet()
                    }
                }
            }
        val analyzer = FrameRecorder()
        val analysis = initImageAnalysis(analyzer)
        assumeTrue(camera.isUseCasesCombinationSupported(preview, imageCapture, analysis))
        val config =
            SessionConfig.Builder(imageCapture, preview, analysis)
                .setInitialEnabledUseCases(preview, imageCapture)
                .camera2Interop {
                    setSessionStateCallback(directExecutor(), sessionCounter)
                    setRepeatingCaptureCallback(directExecutor(), repeatingCallback)
                }
                .build()
        assumeTrue(cameraInfo.isSessionConfigSupported(config))

        val boundCamera = bindSessionConfig(config)
        previewMonitor.waitForStream()
        assertThat(repeatingFramesLatch.await(5, TimeUnit.SECONDS)).isTrue()
        imageCapture.waitForCapturing()
        assertThat(stillIntentOnRepeatingCallbackCount.get()).isEqualTo(0)
        val configuredSessions = sessionCounter.count
        assertWithMessage("The session state callback is not invoked")
            .that(configuredSessions)
            .isAtLeast(1)
        assertWithMessage("ImageAnalysis received frames in PHOTO mode")
            .that(analyzer.awaitFrames(timeoutMs = NO_FRAME_TIMEOUT_MS))
            .isFalse()

        // Switch to SCAN (preview + analysis).
        boundCamera.cameraControl.setEnabledUseCasesAsync(preview, analysis)[5, TimeUnit.SECONDS]
        assertWithMessage("ImageAnalysis received no frames in SCAN mode")
            .that(analyzer.awaitFrames(count = 10))
            .isTrue()
        previewMonitor.waitForStream()

        // Switch back to PHOTO (preview + imageCapture).
        boundCamera.cameraControl
            .setEnabledUseCasesAsync(preview, imageCapture)[5, TimeUnit.SECONDS]
        // Frames of the previous repeating request may still be in flight.
        previewMonitor.waitForStream(count = DRAIN_FRAME_COUNT)
        assertWithMessage("ImageAnalysis received frames after switching back to PHOTO")
            .that(analyzer.awaitFrames(timeoutMs = NO_FRAME_TIMEOUT_MS))
            .isFalse()
        previewMonitor.waitForStream()
        imageCapture.waitForCapturing()

        // Switch to preview only.
        boundCamera.cameraControl.setEnabledUseCasesAsync(preview)[5, TimeUnit.SECONDS]
        previewMonitor.waitForStream()

        // Taking a picture while ImageCapture is disabled fails with ERROR_INVALID_CAMERA.
        val disabledCaptureCallback = imageCapture.triggerCapturing()
        assertThat(disabledCaptureCallback.latch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(disabledCaptureCallback.errors).hasSize(1)
        assertThat(disabledCaptureCallback.errors.single().imageCaptureError)
            .isEqualTo(ImageCapture.ERROR_INVALID_CAMERA)

        // CameraControl zoom state applied while in preview-only mode is preserved across switches.
        boundCamera.cameraControl.setLinearZoom(0.5f)[5, TimeUnit.SECONDS]

        // Switch back to PHOTO (preview + imageCapture) and verify zoom state and still capture.
        boundCamera.cameraControl
            .setEnabledUseCasesAsync(preview, imageCapture)[5, TimeUnit.SECONDS]
        previewMonitor.waitForStream()
        assertThat(boundCamera.cameraInfo.zoomState.value?.linearZoom).isWithin(0.05f).of(0.5f)
        imageCapture.waitForCapturing()
        boundCamera.cameraControl.setLinearZoom(0f)[5, TimeUnit.SECONDS]

        // Rapid back-to-back setEnabledUseCasesAsync stress sequence without awaiting intermediate
        // futures settles cleanly on the final subset without reconfiguring the capture session.
        val stressSubsets =
            listOf(
                listOf(preview),
                listOf(preview, analysis),
                listOf(preview, imageCapture),
                listOf(preview),
                listOf(preview, analysis),
                listOf(preview, imageCapture),
            )
        val stressFutures = stressSubsets.map { subset ->
            boundCamera.cameraControl.setEnabledUseCasesAsync(subset)
        }
        stressFutures.last()[5, TimeUnit.SECONDS]
        previewMonitor.waitForStream()
        imageCapture.waitForCapturing()

        // Verify switching to imageCapture only matches whether the device supports a 4th
        // MeteringRepeating stream alongside PRIV + JPEG + YUV.
        val imageCaptureOnlySupported =
            cameraInfo.isSessionConfigSupported(
                SessionConfig.Builder(imageCapture, preview, analysis)
                    .setInitialEnabledUseCases(imageCapture)
                    .build()
            )
        if (imageCaptureOnlySupported) {
            boundCamera.cameraControl.setEnabledUseCasesAsync(imageCapture)[5, TimeUnit.SECONDS]
            imageCapture.waitForCapturing()
            boundCamera.cameraControl
                .setEnabledUseCasesAsync(preview, imageCapture)[5, TimeUnit.SECONDS]
            previewMonitor.waitForStream()
        } else {
            val meteringUnsupportedException =
                assertThrows(ExecutionException::class.java) {
                    boundCamera.cameraControl
                        .setEnabledUseCasesAsync(imageCapture)[5, TimeUnit.SECONDS]
                }
            assertThat(meteringUnsupportedException.cause)
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        // A use case not part of the SessionConfig is rejected without changing the active set.
        val unboundPreview = Preview.Builder().build()
        val exception =
            assertThrows(ExecutionException::class.java) {
                boundCamera.cameraControl
                    .setEnabledUseCasesAsync(preview, unboundPreview)[5, TimeUnit.SECONDS]
            }
        assertThat(exception.cause).isInstanceOf(IllegalArgumentException::class.java)

        assertWithMessage("The capture session is reconfigured by setEnabledUseCasesAsync")
            .that(sessionCounter.count)
            .isEqualTo(configuredSessions)

        // Switch to SCAN (preview + analysis) and verify the active set is retained across
        // Lifecycle ON_STOP -> ON_START, while calls made during ON_STOP fail with
        // OperationCanceledException without mutating the retained set.
        boundCamera.cameraControl.setEnabledUseCasesAsync(preview, analysis)[5, TimeUnit.SECONDS]
        instrumentation.runOnMainSync { fakeLifecycleOwner.pauseAndStop() }
        val stoppedException =
            assertThrows(ExecutionException::class.java) {
                boundCamera.cameraControl.setEnabledUseCasesAsync(preview)[5, TimeUnit.SECONDS]
            }
        assertThat(stoppedException.cause)
            .isInstanceOf(androidx.camera.core.CameraControl.OperationCanceledException::class.java)
        instrumentation.runOnMainSync { fakeLifecycleOwner.startAndResume() }
        assertWithMessage("ImageAnalysis received no frames after ON_STOP -> ON_START")
            .that(analyzer.awaitFrames(count = 10))
            .isTrue()
        previewMonitor.waitForStream()

        // After unbindAll(), setEnabledUseCasesAsync fails with IllegalStateException.
        instrumentation.runOnMainSync { cameraProvider.unbindAll() }
        val unbindException =
            assertThrows(ExecutionException::class.java) {
                boundCamera.cameraControl.setEnabledUseCasesAsync(preview)[5, TimeUnit.SECONDS]
            }
        assertThat(unbindException.cause).isInstanceOf(IllegalStateException::class.java)

        // On FULL / LEVEL_3 cameras, a 2-stream SessionConfig (PRIV + JPEG) supports pre-attaching
        // MeteringRepeating (PRIV + PRIV + JPEG), allowing a switch to imageCapture only without
        // reconfiguring the capture session.
        val hardwareLevel =
            CameraUtil.getCameraCharacteristics(cameraSelector.lensFacing!!)
                ?.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
        if (
            hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL ||
                hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3
        ) {
            val twoStreamCounter = SessionConfiguredCounter()
            val twoStreamConfig =
                SessionConfig.Builder(preview, imageCapture)
                    .setInitialEnabledUseCases(preview)
                    .camera2Interop { setSessionStateCallback(directExecutor(), twoStreamCounter) }
                    .build()
            assertThat(cameraInfo.isSessionConfigSupported(twoStreamConfig)).isTrue()
            assertThat(
                    cameraInfo.isSessionConfigSupported(
                        SessionConfig.Builder(preview, imageCapture)
                            .setInitialEnabledUseCases(imageCapture)
                            .build()
                    )
                )
                .isTrue()
            val twoStreamCamera = bindSessionConfig(twoStreamConfig)
            previewMonitor.waitForStream()
            val twoStreamConfiguredSessions = twoStreamCounter.count

            twoStreamCamera.cameraControl.setEnabledUseCasesAsync(imageCapture)[5, TimeUnit.SECONDS]
            imageCapture.waitForCapturing()

            twoStreamCamera.cameraControl
                .setEnabledUseCasesAsync(preview, imageCapture)[5, TimeUnit.SECONDS]
            previewMonitor.waitForStream()
            imageCapture.waitForCapturing()

            assertWithMessage("The capture session is reconfigured on 2-stream switch")
                .that(twoStreamCounter.count)
                .isEqualTo(twoStreamConfiguredSessions)
        }
    }

    /**
     * Verifies that [SessionConfig] with [SessionConfig.Builder.setInitialEnabledUseCases] and
     * [androidx.camera.core.CameraControl.setEnabledUseCasesAsync] switches between photo
     * ([Preview] + [ImageCapture]) and video ([Preview] + [VideoCapture]) modes and records video
     * without reconfiguring the capture session.
     */
    @Test
    fun sessionConfig_setEnabledUseCasesSwitchesPhotoAndVideoWithoutReconfiguringSession() {
        checkAndPrepareVideoCaptureSources()
        assumeTrue(camera.isUseCasesCombinationSupported(preview, imageCapture, videoCapture))
        val sessionCounter = SessionConfiguredCounter()
        val config =
            SessionConfig.Builder(preview, imageCapture, videoCapture)
                .setInitialEnabledUseCases(preview, imageCapture)
                .camera2Interop { setSessionStateCallback(directExecutor(), sessionCounter) }
                .build()
        assumeTrue(cameraInfo.isSessionConfigSupported(config))

        val boundCamera = bindSessionConfig(config)
        previewMonitor.waitForStream()
        imageCapture.waitForCapturing()
        val configuredSessions = sessionCounter.count
        assertWithMessage("The session state callback is not invoked")
            .that(configuredSessions)
            .isAtLeast(1)

        // Switch to VIDEO (preview + videoCapture) and record a video.
        boundCamera.cameraControl
            .setEnabledUseCasesAsync(preview, videoCapture)[5, TimeUnit.SECONDS]
        previewMonitor.waitForStream()
        recordingSession.createRecording().recordAndVerify()

        // Switch back to PHOTO (preview + imageCapture) and capture a still image.
        boundCamera.cameraControl
            .setEnabledUseCasesAsync(preview, imageCapture)[5, TimeUnit.SECONDS]
        previewMonitor.waitForStream()
        imageCapture.waitForCapturing()

        assertWithMessage("The capture session is reconfigured by setEnabledUseCasesAsync")
            .that(sessionCounter.count)
            .isEqualTo(configuredSessions)
    }

    /**
     * Verifies that a 4-use-case [SessionConfig]
     * ([Preview] + [ImageCapture] + [ImageAnalysis] + [VideoCapture], backed by StreamSharing)
     * switches across subsets without reconfiguring the capture session.
     */
    @Test
    fun sessionConfig_setEnabledUseCasesWithStreamSharing_switchesWithoutReconfiguringSession() {
        checkAndPrepareVideoCaptureSources()
        val analyzer = FrameRecorder()
        val analysis = initImageAnalysis(analyzer)
        assumeTrue(
            camera.isUseCasesCombinationSupported(preview, imageCapture, analysis, videoCapture)
        )
        val sessionCounter = SessionConfiguredCounter()
        val config =
            SessionConfig.Builder(preview, imageCapture, analysis, videoCapture)
                .setInitialEnabledUseCases(preview, imageCapture)
                .camera2Interop { setSessionStateCallback(directExecutor(), sessionCounter) }
                .build()
        assumeTrue(cameraInfo.isSessionConfigSupported(config))

        val boundCamera = bindSessionConfig(config)
        previewMonitor.waitForStream()
        imageCapture.waitForCapturing()
        val configuredSessions = sessionCounter.count
        assertWithMessage("The session state callback is not invoked")
            .that(configuredSessions)
            .isAtLeast(1)
        assertWithMessage("ImageAnalysis received frames in initial PHOTO mode")
            .that(analyzer.awaitFrames(timeoutMs = NO_FRAME_TIMEOUT_MS))
            .isFalse()

        // Switch to SCAN (preview + analysis) while ImageCapture is disabled.
        boundCamera.cameraControl.setEnabledUseCasesAsync(preview, analysis)[5, TimeUnit.SECONDS]
        assertWithMessage("ImageAnalysis received no frames in SCAN mode")
            .that(analyzer.awaitFrames(count = 10))
            .isTrue()
        previewMonitor.waitForStream()
        val disabledCapture = imageCapture.triggerCapturing()
        assertThat(disabledCapture.latch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(disabledCapture.errors).hasSize(1)
        assertThat(disabledCapture.errors.single().imageCaptureError)
            .isEqualTo(ImageCapture.ERROR_INVALID_CAMERA)

        // Switch to VIDEO (preview + videoCapture) and record a video via StreamSharing.
        boundCamera.cameraControl
            .setEnabledUseCasesAsync(preview, videoCapture)[5, TimeUnit.SECONDS]
        previewMonitor.waitForStream(count = DRAIN_FRAME_COUNT)
        assertWithMessage("ImageAnalysis received frames in VIDEO mode")
            .that(analyzer.awaitFrames(timeoutMs = NO_FRAME_TIMEOUT_MS))
            .isFalse()
        recordingSession.createRecording().recordAndVerify()

        // Switch to all 4 use cases enabled simultaneously.
        boundCamera.cameraControl
            .setEnabledUseCasesAsync(preview, imageCapture, analysis, videoCapture)[
                5, TimeUnit.SECONDS]
        previewMonitor.waitForStream()
        assertWithMessage("ImageAnalysis received no frames with all 4 use cases enabled")
            .that(analyzer.awaitFrames(count = 10))
            .isTrue()
        imageCapture.waitForCapturing()

        // Switch back to PHOTO (preview + imageCapture).
        boundCamera.cameraControl
            .setEnabledUseCasesAsync(preview, imageCapture)[5, TimeUnit.SECONDS]
        previewMonitor.waitForStream(count = DRAIN_FRAME_COUNT)
        assertWithMessage("ImageAnalysis received frames after switching back to PHOTO")
            .that(analyzer.awaitFrames(timeoutMs = NO_FRAME_TIMEOUT_MS))
            .isFalse()
        imageCapture.waitForCapturing()

        assertWithMessage("The capture session is reconfigured by setEnabledUseCasesAsync")
            .that(sessionCounter.count)
            .isEqualTo(configuredSessions)
    }

    /**
     * Stress test verifying repeated [CameraControl.setEnabledUseCasesAsync] transitions combined
     * with concurrent and interleaved CameraX API calls ([CameraControl.setLinearZoom],
     * [CameraControl.setZoomRatio], [CameraControl.enableTorch],
     * [CameraControl.setExposureCompensationIndex], [CameraControl.startFocusAndMetering],
     * [CameraControl.cancelFocusAndMetering], [CameraControl.applyCamera2InteropAsync], and dynamic
     * [UseCase] configuration updates) without timing races or capture session reconfigurations.
     */
    @Test
    fun sessionConfig_fastSwitchStressWithCameraControlAndUseCaseApis() {
        val sessionCounter = SessionConfiguredCounter()
        val analyzer = FrameRecorder()
        val analysis = initImageAnalysis(analyzer)
        assumeTrue(camera.isUseCasesCombinationSupported(preview, imageCapture, analysis))
        val config =
            SessionConfig.Builder(preview, imageCapture, analysis)
                .setInitialEnabledUseCases(preview, imageCapture)
                .camera2Interop { setSessionStateCallback(directExecutor(), sessionCounter) }
                .build()
        assumeTrue(cameraInfo.isSessionConfigSupported(config))

        val boundCamera = bindSessionConfig(config)
        val control = boundCamera.cameraControl
        val info = boundCamera.cameraInfo
        previewMonitor.waitForStream()
        imageCapture.waitForCapturing()
        val configuredSessions = sessionCounter.count
        assertWithMessage("The session state callback is not invoked")
            .that(configuredSessions)
            .isAtLeast(1)

        val hasFlash = info.hasFlashUnit()
        val exposureState = info.exposureState
        val evRange = exposureState.exposureCompensationRange
        val supportsEv =
            exposureState.isExposureCompensationSupported &&
                (evRange.lower != 0 || evRange.upper != 0)
        val meteringPoint = SurfaceOrientedMeteringPointFactory(1.0f, 1.0f).createPoint(0.5f, 0.5f)
        val focusAction = FocusMeteringAction.Builder(meteringPoint).disableAutoCancel().build()
        val supportsFocusMetering = info.isFocusMeteringSupported(focusAction)
        val rotations =
            listOf(
                Surface.ROTATION_0,
                Surface.ROTATION_90,
                Surface.ROTATION_180,
                Surface.ROTATION_270,
            )
        // b/578857056: Exercise representative zoom levels up to 0.5f; capturing at linearZoom
        // 0.8f (3.33x) with 3 streams (Preview + ImageCapture + ImageAnalysis) bound triggers a
        // pre-existing Qualcomm CamX JPEGEncodeLiveSnapshotPreview_IPE1 output fence timeout on
        // Samsung Galaxy Note10+ (sm-n975u1) front camera (camId=1), even with plain
        // bindToLifecycle (no fast switching).
        val linearZooms = listOf(0.1f, 0.25f, 0.5f, 0.0f)

        // Phase 1: Multi-iteration stress loop interleaving mode switches with concurrent
        // CameraControl, Camera2Interop, and UseCase updates.
        for (iteration in 0 until 4) {
            val targetZoom = linearZooms[iteration % linearZooms.size]
            val targetRotation = rotations[iteration % rotations.size]
            val torchEnabled = (iteration % 2 == 0)

            // 1. Switch to SCAN (preview + analysis) concurrently with zoom, torch, EV, interop,
            // focus/metering, and UseCase targetRotation updates.
            val scanSwitchFuture = control.setEnabledUseCasesAsync(preview, analysis)
            val zoomFuture = control.setLinearZoom(targetZoom)
            val torchFuture = if (hasFlash) control.enableTorch(torchEnabled) else null
            if (supportsEv) {
                val targetEv = if (iteration % 2 == 0) evRange.upper.coerceAtMost(1) else 0
                // Submit EV concurrently to exercise UseCaseCameraRequestControl parameter merging;
                // EvCompControl updates ExposureState.exposureCompensationIndex synchronously while
                // UseCaseCameraRequestControl remains attached across fast switches. Do not block
                // on AE convergence since shared-pool racks may be dark (0 lux).
                @Suppress("FutureReturnValueIgnored") control.setExposureCompensationIndex(targetEv)
                assertThat(info.exposureState.exposureCompensationIndex).isEqualTo(targetEv)
            }
            val interopFuture = control.applyCamera2InteropAsync {
                setCaptureRequestOption(
                    CaptureRequest.CONTROL_AWB_MODE,
                    CaptureRequest.CONTROL_AWB_MODE_AUTO,
                )
            }
            val focusFutures =
                if (supportsFocusMetering) {
                    control.startFocusAndMetering(focusAction) to control.cancelFocusAndMetering()
                } else {
                    null
                }
            preview.targetRotation = targetRotation
            analysis.targetRotation = targetRotation
            imageCapture.targetRotation = targetRotation

            scanSwitchFuture[5, TimeUnit.SECONDS]
            zoomFuture[5, TimeUnit.SECONDS]
            torchFuture?.get(5, TimeUnit.SECONDS)
            interopFuture[5, TimeUnit.SECONDS]
            focusFutures?.let { (startFuture, cancelFuture) ->
                cancelFuture[5, TimeUnit.SECONDS]
                try {
                    startFuture[5, TimeUnit.SECONDS]
                } catch (e: ExecutionException) {
                    assertThat(e.cause)
                        .isInstanceOf(CameraControl.OperationCanceledException::class.java)
                }
            }

            assertWithMessage("ImageAnalysis received no frames in SCAN iteration $iteration")
                .that(analyzer.awaitFrames(count = 5))
                .isTrue()
            previewMonitor.waitForStream(count = 5)
            assertThat(info.zoomState.value?.linearZoom).isWithin(0.05f).of(targetZoom)
            if (hasFlash) {
                assertThat(info.torchState.value)
                    .isEqualTo(if (torchEnabled) TorchState.ON else TorchState.OFF)
            }

            // 2. Switch to preview-only concurrently with clearing interop options and zoom ratio.
            val minZoomRatio = info.zoomState.value?.minZoomRatio ?: 1.0f
            val previewOnlyFuture = control.setEnabledUseCasesAsync(preview)
            val zoomRatioFuture = control.setZoomRatio(minZoomRatio)
            val clearInteropFuture = control.applyCamera2InteropAsync {
                clearAllCaptureRequestOptions()
            }
            previewOnlyFuture[5, TimeUnit.SECONDS]
            zoomRatioFuture[5, TimeUnit.SECONDS]
            clearInteropFuture[5, TimeUnit.SECONDS]
            previewMonitor.waitForStream(count = 5)

            // 3. Switch to all 3 use cases (preview + imageCapture + analysis) and verify all
            // streams operate concurrently.
            val allThreeFuture = control.setEnabledUseCasesAsync(preview, imageCapture, analysis)
            val midZoomFuture = control.setLinearZoom(targetZoom)
            allThreeFuture[5, TimeUnit.SECONDS]
            midZoomFuture[5, TimeUnit.SECONDS]
            assertWithMessage("ImageAnalysis received no frames in ALL-3 iteration $iteration")
                .that(analyzer.awaitFrames(count = 5))
                .isTrue()
            previewMonitor.waitForStream(count = 5)
            imageCapture.waitForCapturing()

            // 4. Switch to PHOTO (preview + imageCapture) and verify still capture works.
            control.setEnabledUseCasesAsync(preview, imageCapture)[5, TimeUnit.SECONDS]
            previewMonitor.waitForStream(count = 5)
            imageCapture.waitForCapturing()
        }

        // Phase 2: Rapid unawaited burst interleaving mode switches and control updates
        // back-to-back without waiting between calls; the final futures must complete cleanly.
        val subsets =
            listOf(
                listOf(preview),
                listOf(preview, analysis),
                listOf(preview, imageCapture),
                listOf(preview, imageCapture, analysis),
                listOf(preview, analysis),
                listOf(preview, imageCapture),
            )
        val (lastSwitchFuture, lastZoomFuture, lastInteropFuture) =
            subsets
                .mapIndexed { index, subset ->
                    Triple(
                        control.setEnabledUseCasesAsync(subset),
                        control.setLinearZoom((index % 3) * 0.25f),
                        control.applyCamera2InteropAsync {
                            setCaptureRequestOption(
                                CaptureRequest.CONTROL_AWB_MODE,
                                CaptureRequest.CONTROL_AWB_MODE_AUTO,
                            )
                        },
                    )
                }
                .last()
        val finalTorchFuture = if (hasFlash) control.enableTorch(false) else null
        val finalClearInteropFuture = control.applyCamera2InteropAsync {
            clearAllCaptureRequestOptions()
        }
        val finalZoomResetFuture = control.setLinearZoom(0f)

        lastSwitchFuture[5, TimeUnit.SECONDS]
        try {
            lastZoomFuture[5, TimeUnit.SECONDS]
        } catch (e: ExecutionException) {
            // Superseded by finalZoomResetFuture.
            assertThat(e.cause).isInstanceOf(CameraControl.OperationCanceledException::class.java)
        }
        try {
            lastInteropFuture[5, TimeUnit.SECONDS]
        } catch (e: ExecutionException) {
            // Superseded by finalClearInteropFuture.
            assertThat(e.cause).isInstanceOf(CameraControl.OperationCanceledException::class.java)
        }
        finalTorchFuture?.get(5, TimeUnit.SECONDS)
        finalClearInteropFuture[5, TimeUnit.SECONDS]
        finalZoomResetFuture[5, TimeUnit.SECONDS]

        previewMonitor.waitForStream()
        imageCapture.waitForCapturing()

        assertWithMessage("The capture session is reconfigured during fast switch stress test")
            .that(sessionCounter.count)
            .isEqualTo(configuredSessions)
    }

    /**
     * Stress test verifying repeated fast session switches between photo
     * ([Preview] + [ImageCapture]) and video ([Preview] + [VideoCapture]) modes combined with
     * concurrent [CameraControl] updates, video recordings, and still image captures without
     * reconfiguring the capture session.
     */
    @Test
    fun sessionConfig_fastSwitchPhotoAndVideoStressWithApis() {
        checkAndPrepareVideoCaptureSources()
        assumeTrue(camera.isUseCasesCombinationSupported(preview, imageCapture, videoCapture))
        val sessionCounter = SessionConfiguredCounter()
        val config =
            SessionConfig.Builder(preview, imageCapture, videoCapture)
                .setInitialEnabledUseCases(preview, imageCapture)
                .camera2Interop { setSessionStateCallback(directExecutor(), sessionCounter) }
                .build()
        assumeTrue(cameraInfo.isSessionConfigSupported(config))

        val boundCamera = bindSessionConfig(config)
        val control = boundCamera.cameraControl
        val info = boundCamera.cameraInfo
        previewMonitor.waitForStream()
        imageCapture.waitForCapturing()
        val configuredSessions = sessionCounter.count
        assertWithMessage("The session state callback is not invoked")
            .that(configuredSessions)
            .isAtLeast(1)

        val hasFlash = info.hasFlashUnit()

        for (iteration in 0 until 3) {
            val targetZoom = if (iteration % 2 == 0) 0.4f else 0.0f
            val torchOn = (iteration % 2 == 0)

            // Switch to VIDEO (preview + videoCapture) concurrently with zoom, torch, and interop.
            val videoSwitchFuture = control.setEnabledUseCasesAsync(preview, videoCapture)
            val videoZoomFuture = control.setLinearZoom(targetZoom)
            val videoTorchFuture = if (hasFlash) control.enableTorch(torchOn) else null
            val videoInteropFuture = control.applyCamera2InteropAsync {
                setCaptureRequestOption(
                    CaptureRequest.CONTROL_AWB_MODE,
                    CaptureRequest.CONTROL_AWB_MODE_AUTO,
                )
            }
            videoSwitchFuture[5, TimeUnit.SECONDS]
            videoZoomFuture[5, TimeUnit.SECONDS]
            videoTorchFuture?.get(5, TimeUnit.SECONDS)
            videoInteropFuture[5, TimeUnit.SECONDS]

            previewMonitor.waitForStream(count = 5)
            recordingSession.createRecording().recordAndVerify()

            // Switch back to PHOTO (preview + imageCapture) concurrently with zoom and interop
            // reset.
            val photoSwitchFuture = control.setEnabledUseCasesAsync(preview, imageCapture)
            val photoZoomFuture = control.setLinearZoom(0f)
            val photoTorchFuture = if (hasFlash) control.enableTorch(false) else null
            val photoInteropFuture = control.applyCamera2InteropAsync {
                clearAllCaptureRequestOptions()
            }
            photoSwitchFuture[5, TimeUnit.SECONDS]
            photoZoomFuture[5, TimeUnit.SECONDS]
            photoTorchFuture?.get(5, TimeUnit.SECONDS)
            photoInteropFuture[5, TimeUnit.SECONDS]

            previewMonitor.waitForStream(count = 5)
            imageCapture.waitForCapturing()
        }

        assertWithMessage(
                "The capture session is reconfigured during photo/video fast switch stress test"
            )
            .that(sessionCounter.count)
            .isEqualTo(configuredSessions)
    }

    private fun bindSessionConfig(sessionConfig: SessionConfig): Camera {
        lateinit var boundCamera: Camera
        instrumentation.runOnMainSync {
            boundCamera =
                cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, sessionConfig)
        }
        return boundCamera
    }

    private class SessionConfiguredCounter : CameraCaptureSession.StateCallback() {
        private val configuredCount = AtomicInteger(0)

        val count: Int
            get() = configuredCount.get()

        override fun onConfigured(session: CameraCaptureSession) {
            configuredCount.incrementAndGet()
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {}
    }

    private class FrameRecorder : ImageAnalysis.Analyzer {
        @Volatile private var latch: CountDownLatch? = null

        override fun analyze(image: ImageProxy) {
            image.close()
            latch?.countDown()
        }

        fun awaitFrames(count: Int = 1, timeoutMs: Long = FRAME_TIMEOUT_MS): Boolean {
            val countDown = CountDownLatch(count)
            latch = countDown
            return countDown.await(timeoutMs, TimeUnit.MILLISECONDS)
        }
    }

    /**
     * Returns the stream use case expected for a standalone Preview. On devices with
     * [LowLightBoostStreamUseCaseQuirk], PREVIEW is prioritized over PREVIEW_VIDEO_STILL so that
     * Low Light Boost can be enabled.
     */
    private fun expectedSinglePreviewStreamUseCase(): StreamUseCase =
        if (
            (camera.cameraInfo as CameraInfoInternal)
                .cameraQuirks
                .contains(LowLightBoostStreamUseCaseQuirk::class.java)
        ) {
            StreamUseCase.PREVIEW
        } else {
            StreamUseCase.PREVIEW_VIDEO_STILL
        }

    private fun isStreamUseCaseSupported(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val characteristics = CameraUtil.getCameraCharacteristics(cameraSelector.lensFacing!!)
            characteristics?.get(CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES)?.let {
                return it.isNotEmpty()
            }
        }
        return false
    }

    private fun verifyStreamSpecStreamUseCase(useCase: UseCase, streamUseCase: StreamUseCase?) {
        assertThat(useCase.getStreamUseCaseTypeValue()).isEqualTo(streamUseCase?.value)
    }

    private fun UseCase.getStreamUseCaseTypeValue(): Long? =
        attachedStreamSpec
            ?.implementationOptions
            ?.retrieveOption(STREAM_USE_CASE_STREAM_SPEC_OPTION, null)

    private fun initPreview(
        monitor: PreviewMonitor,
        setSurfaceProvider: Boolean = true,
        resolutionSelector: ResolutionSelector? = null,
    ): Preview {
        return Preview.Builder()
            .apply {
                if (resolutionSelector != null) {
                    setResolutionSelector(resolutionSelector)
                }
            }
            .setTargetName("Preview")
            .build()
            .apply {
                if (setSurfaceProvider) {
                    instrumentation.runOnMainSync { surfaceProvider = monitor.getSurfaceProvider() }
                }
            }
    }

    private fun initImageAnalysis(
        analyzer: ImageAnalysis.Analyzer?,
        resolutionSelector: ResolutionSelector? = null,
    ): ImageAnalysis {
        return ImageAnalysis.Builder()
            .apply {
                if (resolutionSelector != null) {
                    setResolutionSelector(resolutionSelector)
                }
            }
            .setTargetName("ImageAnalysis")
            .build()
            .apply {
                analyzer?.let { analyzer -> setAnalyzer(Dispatchers.IO.asExecutor(), analyzer) }
            }
    }

    private fun initImageCapture(resolutionSelector: ResolutionSelector? = null): ImageCapture {
        return ImageCapture.Builder()
            .apply {
                if (resolutionSelector != null) {
                    setResolutionSelector(resolutionSelector)
                }
            }
            .build()
    }

    private fun ImageCapture.waitForCapturing(timeMillis: Long = 10000, useFlash: Boolean = false) {
        val callback = triggerCapturing(useFlash = useFlash)
        callback.verifyCapture(timeMillis = timeMillis)
    }

    private fun ImageCapture.triggerCapturing(useFlash: Boolean = false): ImageCaptureCallback {
        val callback = ImageCaptureCallback()

        if (useFlash) {
            if (cameraSelector.lensFacing == CameraSelector.LENS_FACING_FRONT) {
                screenFlash = MockScreenFlash()
                flashMode = ImageCapture.FLASH_MODE_SCREEN
            } else {
                flashMode = ImageCapture.FLASH_MODE_ON
            }
        } else {
            flashMode = ImageCapture.FLASH_MODE_OFF
        }

        takePicture(
            Dispatchers.Main.asExecutor(),
            callback.apply {
                invokeOnComplete {
                    // Just in case same imageCapture is bound to rear camera later
                    screenFlash = null
                }
            },
        )

        return callback
    }

    private fun ImageCaptureCallback.verifyCapture(timeMillis: Long = 10000) {
        assertThat(latch.await(timeMillis, TimeUnit.MILLISECONDS) && errors.isEmpty()).isTrue()
    }

    class PreviewMonitor {
        private var countDown: CountDownLatch? = null
        private val surfaceProvider = createAutoDrainingSurfaceTextureProvider {
            countDown?.countDown()
        }

        fun getSurfaceProvider(): Preview.SurfaceProvider = surfaceProvider

        fun waitForStream(count: Int = 10, timeMillis: Long = TimeUnit.SECONDS.toMillis(5)) {
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

        fun assertNoResultReceived() =
            Truth.assertWithMessage("There is still some capture results received unexpectedly.")
                .that(waitForCaptureResultReceived(/* count= */ 1, /* timeSeconds= */ 2))
                .isFalse()

        fun waitForStreamIdle() {
            // Monitor 2 seconds to confirm that less than 10 capture results are received. (It
            // might take some time to stop the preview)
            Truth.assertWithMessage(
                    "There are still more than 10 capture results received in the" +
                        " recent 2 seconds."
                )
                .that(waitForCaptureResultReceived())
                .isFalse()
            // Monitor 2 seconds to confirm that no any capture result can be received.
            assertNoResultReceived()
        }

        private fun waitForCaptureResultReceived(count: Int = 10, timeSeconds: Long = 2) =
            synchronized(this) {
                    countDown = CountDownLatch(count)
                    countDown
                }!!
                .await(timeSeconds, TimeUnit.SECONDS)
    }

    class AnalysisMonitor : ImageAnalysis.Analyzer {
        private var countDown: CountDownLatch? = null

        fun waitForImageAnalysis(count: Int = 10, timeMillis: Long = TimeUnit.SECONDS.toMillis(5)) {
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

        override fun analyze(image: ImageProxy) {
            image.close()
            synchronized(this) { countDown?.countDown() }
        }
    }

    private fun checkAndBindUseCases(vararg useCases: UseCase) {
        assumeTrue(camera.isUseCasesCombinationSupported(*useCases))
        bindUseCases(*useCases)
    }

    private fun bindUseCases(vararg useCases: UseCase) {
        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, *useCases)
        }
    }

    private fun unbindUseCases(vararg useCases: UseCase) {
        instrumentation.runOnMainSync { cameraProvider.unbind(*useCases) }
    }

    private fun checkAndPrepareVideoCaptureSources(
        verifyStatusCount: Int = DEFAULT_VERIFY_STATUS_COUNT,
        verifyTimeoutMs: Long = DEFAULT_VERIFY_TIMEOUT_MS,
        verifyStatusTimeoutMs: Long = DEFAULT_VERIFY_STATUS_TIMEOUT_MS,
        qualitySelector: QualitySelector = Recorder.DEFAULT_QUALITY_SELECTOR,
        @Ratio aspectRatio: Int = AspectRatio.RATIO_DEFAULT,
    ) {
        skipVideoRecordingTestIfNotSupportedByEmulator()
        videoCapture =
            VideoCapture.withOutput(
                Recorder.Builder()
                    .setQualitySelector(qualitySelector)
                    .setAspectRatio(aspectRatio)
                    .build()
            )
        recordingSession =
            RecordingSession(
                RecordingSession.Defaults(
                    context = context,
                    recorder = videoCapture.output,
                    outputOptionsProvider = {
                        FileOutputOptions.Builder(temporaryFolder.newFile()).build()
                    },
                    withAudio = audioStreamAvailable,
                    verifyStatusCount = verifyStatusCount,
                    verifyTimeoutMs = verifyTimeoutMs,
                    verifyStatusTimeoutMs = verifyStatusTimeoutMs,
                )
            )
    }

    class ImageCaptureCallback : ImageCapture.OnImageCapturedCallback() {
        val latch = CountDownLatch(1)
        val errors = mutableListOf<ImageCaptureException>()

        private val onCompleteBlocks = mutableListOf<() -> Unit>()

        override fun onCaptureSuccess(image: ImageProxy) {
            image.close()
            latch.countDown()
            onCompleteBlocks.forEach { it.invoke() }
        }

        override fun onError(exception: ImageCaptureException) {
            errors.add(exception)
            latch.countDown()
            onCompleteBlocks.forEach { it.invoke() }
        }

        fun invokeOnComplete(block: () -> Unit) {
            onCompleteBlocks.add(block)
        }
    }
}
