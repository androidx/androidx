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

package androidx.camera.integration.core

import android.content.Context
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.util.Log
import android.util.Range
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ExperimentalSessionConfig
import androidx.camera.core.ExtendableBuilder
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.UseCase
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.SurfaceTextureProvider
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
import java.util.concurrent.TimeUnit
import kotlin.test.assertFailsWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify

@LargeTest
@RunWith(Parameterized::class)
@OptIn(ExperimentalSessionConfig::class)
class FrameRateTest(
    private val testName: String,
    private val cameraSelector: CameraSelector,
    private val implName: String,
    private val cameraConfig: CameraXConfig,
) {
    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = implName == CameraPipeConfig::class.simpleName)

    @get:Rule
    val cameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(cameraConfig)
        )

    companion object {
        private const val TAG = "FrameRateTest"
        private const val DEFAULT_VERIFY_TIMEOUT_MS = 10_000L

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
                    add(
                        arrayOf(
                            "config=${CameraPipeConfig::class.simpleName} lensFacing={$lens}",
                            cameraSelector,
                            CameraPipeConfig::class.simpleName,
                            CameraPipeConfig.defaultConfig(),
                        )
                    )
                }
            }
    }

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var cameraInfo: CameraInfoInternal
    private lateinit var lifecycleOwner: FakeLifecycleOwner

    @Before
    fun setup() = runBlocking {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(cameraSelector.lensFacing!!))

        ProcessCameraProvider.configureInstance(cameraConfig)

        cameraProvider = ProcessCameraProvider.getInstance(context).await()
        cameraInfo = cameraProvider.getCameraInfo(cameraSelector) as CameraInfoInternal

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

    // Scenario: set frame rate on both Preview and VideoCapture
    @Test
    fun setFrameRate_onUseCasesAndSessionConfig() = runBlocking {
        // Arrange.
        val supportedFpsRanges =
            cameraInfo.getSupportedFrameRateRanges(
                SessionConfig(createPreview(), createVideoCapture())
            )
        assumeTrue(supportedFpsRanges.isNotEmpty())
        val targetFps = supportedFpsRanges.first()
        Log.d(TAG, "Testing fps: $targetFps")

        // Arrange: set fps on both UseCases and SessionConfig.
        val sessionConfig =
            SessionConfig(
                useCases =
                    listOf(
                        createPreview(targetFps = targetFps),
                        createVideoCapture(targetFps = targetFps),
                    ),
                frameRateRange = targetFps,
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
        sessionConfigBuilderProvider: SessionConfigBuilderProvider,
    ) {
        val supportedFpsRanges =
            cameraInfo.getSupportedFrameRateRanges(
                sessionConfigBuilderProvider.provide(targetFps = null).build()
            )
        assumeTrue("No supported fps ranges", supportedFpsRanges.isNotEmpty())

        Log.d(TAG, "Testing supportedFpsRanges: $supportedFpsRanges")
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

            val captureResult = bindAndGetCaptureResult(sessionConfig = sessionConfig)

            assertThat(captureResult.request.getAeTargetFps()).isEqualTo(targetFps)
        }
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
    ): VideoCapture<*> {
        val captureCallback = mock(CaptureCallback::class.java)
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
