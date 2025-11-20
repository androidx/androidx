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
import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import android.util.Rational
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.camera2.pipe.integration.internal.StreamUseCaseUtil.STREAM_USE_CASE_STREAM_SPEC_OPTION
import androidx.camera.core.AspectRatio
import androidx.camera.core.AspectRatio.Ratio
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraX
import androidx.camera.core.CameraXConfig
import androidx.camera.core.DynamicRange
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.impl.StreamUseCase
import androidx.camera.core.impl.utils.AspectRatioUtil
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.IgnoreVideoRecordingProblematicDeviceRule.Companion.skipVideoRecordingTestIfNotSupportedByEmulator
import androidx.camera.testing.impl.SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider
import androidx.camera.testing.impl.WakelockEmptyActivityRule
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
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
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = implName.contains(CameraPipeConfig::class.simpleName!!))

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

    private val audioStreamAvailable by lazy {
        AudioChecker.canAudioStreamBeStarted(videoCapabilities, Recorder.DEFAULT_QUALITY_SELECTOR)
    }

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
        verifyStreamSpecStreamUseCase(preview, StreamUseCase.PREVIEW_VIDEO_STILL)

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
