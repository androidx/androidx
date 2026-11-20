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

@file:Suppress("DEPRECATION")

package androidx.camera.integration.core

import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.os.SystemClock
import android.util.Log
import android.util.Range
import android.util.Size
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ExtendableBuilder
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.UseCase
import androidx.camera.core.impl.AdapterCameraInfo
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.ResolvedStreamInfo
import androidx.camera.core.impl.UseCaseAdditionSimulator
import androidx.camera.core.impl.UseCaseConfigFactory
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.integration.core.util.StressTestUtil.assumeNotRedfin64BitOnly
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.AndroidUtil
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.WakelockEmptyActivityRule
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.concurrent.futures.await
import androidx.lifecycle.LifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertFailsWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify

@LargeTest
@RunWith(Parameterized::class)
class FrameRateTest(
    private val testName: String,
    private val cameraSelector: CameraSelector,
    private val implName: String,
    private val cameraConfig: CameraXConfig,
) {
    @get:Rule
    val cameraRule: TestRule =
        RuleChain.outerRule(WakelockEmptyActivityRule())
            .around(
                CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
                    CameraUtil.PreTestCameraIdList(cameraConfig)
                )
            )

    companion object {
        private const val TAG = "FrameRateTest"
        private const val DEFAULT_VERIFY_TIMEOUT_MS = 10_000L
        private const val FPS_SETTLE_TIME_MS = 500L
        private const val FPS_MEASURE_WINDOW_MS = 3_000L
        private const val FPS_MEASURE_TIMEOUT_MS = 5_000L

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            mutableListOf<Array<Any?>>().apply {
                CameraUtil.getAvailableCameraSelectors().forEach { cameraSelector ->
                    val lens = cameraSelector.lensFacing
                    add(
                        arrayOf(
                            "config=${Camera2Config::class.simpleName} lensFacing={$lens}",
                            cameraSelector,
                            Camera2Config::class.simpleName,
                            Camera2Config.defaultConfig(),
                        )
                    )
                }
            }
    }

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var cameraInfo: CameraInfoInternal
    private lateinit var useCaseConfigFactory: UseCaseConfigFactory
    private lateinit var lifecycleOwner: FakeLifecycleOwner

    @Before
    fun setup() = runBlocking {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(cameraSelector.lensFacing!!))

        ProcessCameraProvider.configureInstance(cameraConfig)

        cameraProvider = ProcessCameraProvider.getInstance(context).await()
        cameraInfo = cameraProvider.getCameraInfo(cameraSelector) as CameraInfoInternal
        useCaseConfigFactory =
            checkNotNull(cameraConfig.getUseCaseConfigFactoryProvider(null))
                .newInstance(context, cameraConfig.isPreviewResolutionBypassEnabled)

        lifecycleOwner = FakeLifecycleOwner().apply { startAndResume() }
    }

    @After
    fun tearDown() {
        if (this::cameraProvider.isInitialized) {
            cameraProvider.shutdownAsync()[10, TimeUnit.SECONDS]
        }
    }

    // Scenario: a single multi-purpose stream (by SessionConfig)
    @Test
    fun setFrameRateOnSessionConfig_previewOnly() = runBlocking {
        testSupportedFrameRates(setFpsOnSessionConfig = true) {
            SessionConfig.Builder(createPreview())
        }
    }

    // Scenario: a single multi-purpose stream (by UseCase)
    @Test
    fun setFrameRateOnUseCase_previewOnly() = runBlocking {
        testSupportedFrameRates(setFpsOnSessionConfig = false) { targetFps ->
            SessionConfig.Builder(createPreview(targetFps = targetFps))
        }
    }

    // Scenario: common video recording (by SessionConfig)
    @Test
    fun setFrameRateOnSessionConfig_previewAndVideo() = runBlocking {
        testSupportedFrameRates(setFpsOnSessionConfig = true) {
            SessionConfig.Builder(createPreview(), createVideoCapture())
        }
    }

    // Scenario: common video recording (by UseCase)
    @Test
    fun setFrameRateOnUseCase_previewAndVideo() = runBlocking {
        testSupportedFrameRates(setFpsOnSessionConfig = false) { targetFps ->
            SessionConfig.Builder(createPreview(), createVideoCapture(targetFps = targetFps))
        }
    }

    // Scenario: UHD video recording (by SessionConfig)
    @Test
    fun setFrameRateOnSessionConfig_previewAndVideoUhd() = runBlocking {
        assumeNotRedfin64BitOnly()
        testSupportedFrameRates(setFpsOnSessionConfig = true) {
            SessionConfig.Builder(
                createPreview(),
                createVideoCapture(qualitySelector = QualitySelector.from(Quality.UHD)),
            )
        }
    }

    // Scenario: UHD video recording (by UseCase)
    @Test
    fun setFrameRateOnUseCase_previewAndVideoUhd() = runBlocking {
        assumeNotRedfin64BitOnly()
        testSupportedFrameRates(setFpsOnSessionConfig = false) { targetFps ->
            SessionConfig.Builder(
                createPreview(),
                createVideoCapture(
                    targetFps = targetFps,
                    qualitySelector = QualitySelector.from(Quality.UHD),
                ),
            )
        }
    }

    // Scenario: Preview + ImageCapture + VideoCapture (by SessionConfig)
    @Test
    fun setFrameRateOnSessionConfig_previewImageAndVideo() = runBlocking {
        testSupportedFrameRates(setFpsOnSessionConfig = true) {
            SessionConfig.Builder(createPreview(), createVideoCapture(), createImageCapture())
        }
    }

    // Scenario: Preview + ImageCapture + VideoCapture (by UseCase)
    @Test
    fun setFrameRateOnUseCase_previewImageAndVideo() = runBlocking {
        testSupportedFrameRates(setFpsOnSessionConfig = false) { targetFps ->
            SessionConfig.Builder(
                createPreview(),
                createVideoCapture(targetFps = targetFps),
                createImageCapture(),
            )
        }
    }

    // Scenario: video recording without Preview, where MeteringRepeating provides the repeating
    // stream (by SessionConfig)
    @Test
    fun setFrameRateOnSessionConfig_videoOnly() = runBlocking {
        testSupportedFrameRates(setFpsOnSessionConfig = true, requireMeteringRepeating = true) {
            SessionConfig.Builder(createVideoCapture())
        }
    }

    // Scenario: video recording without Preview, where MeteringRepeating provides the repeating
    // stream (by UseCase)
    @Test
    fun setFrameRateOnUseCase_videoOnly() = runBlocking {
        testSupportedFrameRates(setFpsOnSessionConfig = false, requireMeteringRepeating = true) {
            targetFps ->
            SessionConfig.Builder(createVideoCapture(targetFps = targetFps))
        }
    }

    // Scenario: VideoCapture + ImageCapture without Preview (by SessionConfig)
    @Test
    fun setFrameRateOnSessionConfig_videoAndImage() = runBlocking {
        testSupportedFrameRates(setFpsOnSessionConfig = true, requireMeteringRepeating = true) {
            SessionConfig.Builder(createVideoCapture(), createImageCapture())
        }
    }

    // Scenario: VideoCapture + ImageCapture without Preview (by UseCase)
    @Test
    fun setFrameRateOnUseCase_videoAndImage() = runBlocking {
        testSupportedFrameRates(setFpsOnSessionConfig = false, requireMeteringRepeating = true) {
            targetFps ->
            SessionConfig.Builder(createVideoCapture(targetFps = targetFps), createImageCapture())
        }
    }

    // Scenario: 60 FPS video recording without Preview must not be capped at 30 FPS by the
    // MeteringRepeating surface size (by SessionConfig)
    @Test
    fun setFrameRateOnSessionConfig_videoOnly_60FpsDeliversHighFrameRate() = runBlocking {
        // Arrange.
        assumeFalse(
            "Software emulator cannot sustain 60 FPS wall-clock delivery",
            AndroidUtil.isEmulator(),
        )
        assumeTrue(
            "Per-size output min frame durations require MANUAL_SENSOR capability",
            cameraInfo.availableCapabilities.contains(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR
            ),
        )
        val targetFps = Range(60, 60)
        assumeTrue(
            cameraInfo
                .getSupportedFrameRateRanges(SessionConfig(createVideoCapture()))
                .contains(targetFps)
        )
        val frameRateCallback = FrameRateMeasuringCallback(targetFps)
        val sessionConfig =
            SessionConfig(
                listOf(createVideoCapture(captureCallback = frameRateCallback)),
                frameRateRange = targetFps,
            )
        val meteringResolution = resolveMeteringRepeatingResolution(sessionConfig)
        assumeTrue(
            "MeteringRepeating is not supported alongside VideoCapture at 60 FPS",
            meteringResolution != null,
        )

        try {
            // Act.
            cameraProvider.bindToLifecycleOnMain(lifecycleOwner, cameraSelector, sessionConfig)
            assertWithMessage("Timed out waiting for capture result with AE target FPS $targetFps")
                .that(
                    frameRateCallback.targetFpsAppliedLatch.await(
                        DEFAULT_VERIFY_TIMEOUT_MS,
                        TimeUnit.MILLISECONDS,
                    )
                )
                .isTrue()
            // Wait a little bit for the frame rate to settle.
            delay(FPS_SETTLE_TIME_MS)
            frameRateCallback.startMeasuring()

            // Assert.
            assertWithMessage("Timed out measuring the capture result FPS")
                .that(
                    frameRateCallback.measuredLatch.await(
                        FPS_MEASURE_TIMEOUT_MS,
                        TimeUnit.MILLISECONDS,
                    )
                )
                .isTrue()
            val measuredFps = frameRateCallback.measuredFps
            Log.d(
                TAG,
                "Measured capture result FPS: $measuredFps " +
                    "(meteringRepeatingResolution=$meteringResolution)",
            )
            assertWithMessage(
                    "Actual capture result FPS was too low: $measuredFps " +
                        "(meteringRepeatingResolution=$meteringResolution)"
                )
                .that(measuredFps)
                .isGreaterThan(40.0)
        } finally {
            cameraProvider.unbindAllOnMain()
        }
    }

    // Scenario: set a custom frame rate (by UseCase)
    @Test
    fun setFrameRateOnUseCase_customFps_fallbackToSupportedFps() = runBlocking {
        // Arrange.
        val targetFps = Range(14, 14)
        val sessionConfig = SessionConfig(createPreview(targetFps = targetFps))
        val supportedFpsRanges = cameraInfo.getSupportedFrameRateRanges(sessionConfig)
        assumeFalse(supportedFpsRanges.contains(targetFps))

        // Act.
        val captureResult = bindAndGetCaptureResult(sessionConfig = sessionConfig)

        // Assert.
        assertThat(supportedFpsRanges).contains(captureResult.request.getAeTargetFps())
    }

    // Scenario: set a custom frame rate (by SessionConfig)
    @Test
    fun setFrameRateOnSessionConfig_customFps_throwException(): Unit = runBlocking {
        // Arrange.
        val targetFps = Range(14, 14)
        val sessionConfig = SessionConfig(listOf(createPreview()), frameRateRange = targetFps)
        val supportedFpsRanges = cameraInfo.getSupportedFrameRateRanges(sessionConfig)
        assumeFalse(supportedFpsRanges.contains(targetFps))

        // Assert.
        assertFailsWith<IllegalArgumentException> {
            // Act.
            bindAndGetCaptureResult(sessionConfig = sessionConfig)
        }
    }

    // Scenario: set frame rate on both Preview and VideoCapture
    @Test
    fun setFrameRate_onPreviewAndVideoCapture() = runBlocking {
        // Arrange.
        val supportedFpsRanges =
            cameraInfo.getSupportedFrameRateRanges(
                SessionConfig(createPreview(), createVideoCapture())
            )
        assumeTrue(supportedFpsRanges.isNotEmpty())
        val targetFps = supportedFpsRanges.first()
        Log.d(TAG, "Testing fps: $targetFps")

        // Arrange: set fps on Preview and VideoCapture.
        val sessionConfig =
            SessionConfig(
                createPreview(targetFps = targetFps),
                createVideoCapture(targetFps = targetFps),
            )

        // Act.
        val captureResult = bindAndGetCaptureResult(sessionConfig = sessionConfig)

        // Assert.
        assertThat(captureResult.request.getAeTargetFps()).isEqualTo(targetFps)
    }

    private fun interface SessionConfigBuilderProvider {
        suspend fun provide(targetFps: Range<Int>?): SessionConfig.Builder
    }

    private suspend fun testSupportedFrameRates(
        setFpsOnSessionConfig: Boolean,
        requireMeteringRepeating: Boolean = false,
        sessionConfigBuilderProvider: SessionConfigBuilderProvider,
    ) {
        val supportedFpsRanges =
            cameraInfo.getSupportedFrameRateRanges(
                sessionConfigBuilderProvider.provide(targetFps = null).build()
            )
        assumeTrue("No supported fps ranges", supportedFpsRanges.isNotEmpty())

        Log.d(TAG, "Testing supportedFpsRanges: $supportedFpsRanges")
        var testedAnyRange = false
        for (targetFps in supportedFpsRanges) {
            Log.d(TAG, "Testing fps: $targetFps")
            // Create new UseCase instances and SessionConfig in order to attach a clean
            // CaptureCallback on UseCase. Otherwise the test could be flaky due to the callback
            // from previous camera session.
            val sessionConfig =
                sessionConfigBuilderProvider
                    .provide(targetFps = targetFps)
                    .apply {
                        if (setFpsOnSessionConfig) {
                            setFrameRateRange(targetFps)
                        }
                    }
                    .build()
            if (requireMeteringRepeating && !isMeteringRepeatingSupported(sessionConfig)) {
                Log.d(TAG, "Skipping fps $targetFps: MeteringRepeating is not supported")
                continue
            }
            testedAnyRange = true

            val captureResult = bindAndGetCaptureResult(sessionConfig = sessionConfig)

            assertThat(captureResult.request.getAeTargetFps()).isEqualTo(targetFps)
        }
        if (requireMeteringRepeating) {
            assumeTrue(
                "MeteringRepeating is not supported for any supported FPS range",
                testedAnyRange,
            )
        }
    }

    private fun isMeteringRepeatingSupported(sessionConfig: SessionConfig): Boolean =
        resolveMeteringRepeatingResolution(sessionConfig) != null

    /**
     * Resolves the internal repeating stream (`MeteringRepeating`) resolution that [cameraInfo]
     * would attach alongside the non-repeating [UseCase]s in [sessionConfig], or returns `null` if
     * unsupported.
     *
     * Inactive [VideoCapture] and [ImageCapture] attach only non-repeating surfaces, so on cameras
     * where adding a `PRIV` repeating stream exceeds the guaranteed surface combination table (such
     * as `PRIV + PRIV + JPEG(MAXIMUM)` on `LEGACY` / `LIMITED` cameras), no repeating requests are
     * submitted.
     */
    private fun resolveMeteringRepeatingResolution(sessionConfig: SessionConfig): Size? {
        if (!cameraInfo.isRepeatingStreamForced) {
            return null
        }
        val unionInfo =
            try {
                UseCaseAdditionSimulator.simulateAddUseCases(cameraInfo, sessionConfig)
            } catch (_: IllegalArgumentException) {
                return null
            } catch (_: CameraUseCaseAdapter.CameraException) {
                return null
            }
        val adapterPhysicalCameraId = (cameraInfo as? AdapterCameraInfo)?.physicalCameraId
        val streams =
            unionInfo.primaryStreamSpecResult.streamSpecs.map { (useCase, streamSpec) ->
                val defaultConfig = useCase.getDefaultConfig(true, useCaseConfigFactory)
                val mergedConfig =
                    useCase.mergeConfigs(cameraInfo.implementation, null, defaultConfig)
                ResolvedStreamInfo(
                    mergedConfig,
                    streamSpec,
                    useCase.physicalCameraId ?: adapterPhysicalCameraId,
                )
            }
        return cameraInfo.resolveRepeatingStream(unionInfo, streams)?.streamSpec?.resolution
    }

    private suspend fun bindAndGetCaptureResult(sessionConfig: SessionConfig): TotalCaptureResult {
        try {
            cameraProvider.bindToLifecycleOnMain(lifecycleOwner, cameraSelector, sessionConfig)

            val captureCallback = sessionConfig.getCaptureCallback()
            val resultCaptor = argumentCaptor<TotalCaptureResult>()
            verify(captureCallback, timeout(DEFAULT_VERIFY_TIMEOUT_MS).atLeastOnce())
                .onCaptureCompleted(any(), any(), resultCaptor.capture())
            return resultCaptor.lastValue
        } finally {
            cameraProvider.unbindAllOnMain()
        }
    }

    private val captureCallbackMap: MutableMap<UseCase, CaptureCallback> = mutableMapOf()

    private val UseCase.captureCallback: CaptureCallback
        get() = captureCallbackMap[this]!!

    private suspend fun createPreview(targetFps: Range<Int>? = null): Preview {
        val captureCallback = mock(CaptureCallback::class.java)
        return Preview.Builder()
            .apply {
                targetFps?.let { setTargetFrameRate(it) }
                setCaptureCallback(captureCallback)
            }
            .build()
            .apply {
                withContext(Dispatchers.Main) {
                    surfaceProvider = SurfaceTextureProvider.createSurfaceTextureProvider()
                }
                captureCallbackMap.put(this, captureCallback)
            }
    }

    private fun createVideoCapture(
        targetFps: Range<Int>? = null,
        qualitySelector: QualitySelector? = null,
        captureCallback: CaptureCallback = mock(CaptureCallback::class.java),
    ): VideoCapture<*> {
        val recorder =
            Recorder.Builder().apply { qualitySelector?.let { setQualitySelector(it) } }.build()
        return VideoCapture.Builder<Recorder>(recorder)
            .apply {
                targetFps?.let { setTargetFrameRate(it) }
                setCaptureCallback(captureCallback)
            }
            .build()
            .apply { captureCallbackMap.put(this, captureCallback) }
    }

    private fun createImageCapture() = ImageCapture.Builder().build()

    private fun ExtendableBuilder<*>.setCaptureCallback(captureCallback: CaptureCallback) {
        Camera2Interop.Extender(this).setSessionCaptureCallback(captureCallback)
    }

    private fun SessionConfig.getCaptureCallback() =
        useCases.find { it is Preview || it is VideoCapture<*> }!!.captureCallback

    private fun CaptureRequest.getAeTargetFps() = get(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE)

    /**
     * Measures the completed frame rate over [FPS_MEASURE_WINDOW_MS], once [startMeasuring] is
     * called after [expectedFps] has been applied on [CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE].
     */
    private class FrameRateMeasuringCallback(private val expectedFps: Range<Int>) :
        CaptureCallback() {
        val targetFpsAppliedLatch = CountDownLatch(1)
        val measuredLatch = CountDownLatch(1)
        @Volatile var measuredFps = 0.0

        @Volatile private var isMeasuring = false
        private var windowStartMs = 0L
        private var lastFrameNumber = -1L
        private var completedFrames = 0

        fun startMeasuring() {
            isMeasuring = true
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult,
        ) {
            if (request.get(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE) == expectedFps) {
                targetFpsAppliedLatch.countDown()
            }
            if (!isMeasuring || measuredLatch.count == 0L) {
                return
            }
            synchronized(this) {
                if (measuredLatch.count == 0L) {
                    return
                }
                val now = SystemClock.elapsedRealtime()
                if (windowStartMs == 0L) {
                    windowStartMs = now
                    lastFrameNumber = result.frameNumber
                    completedFrames = 0
                    return
                }
                if (result.frameNumber != lastFrameNumber) {
                    lastFrameNumber = result.frameNumber
                    completedFrames++
                }
                val elapsedMs = now - windowStartMs
                if (elapsedMs >= FPS_MEASURE_WINDOW_MS) {
                    measuredFps = completedFrames * 1000.0 / elapsedMs
                    measuredLatch.countDown()
                }
            }
        }
    }

    private suspend fun ProcessCameraProvider.bindToLifecycleOnMain(
        lifecycleOwner: LifecycleOwner,
        cameraSelector: CameraSelector,
        sessionConfig: SessionConfig,
    ): Camera =
        withContext(Dispatchers.Main) {
            bindToLifecycle(lifecycleOwner, cameraSelector, sessionConfig)
        }

    private suspend fun ProcessCameraProvider.unbindAllOnMain() =
        withContext(Dispatchers.Main) { unbindAll() }
}
