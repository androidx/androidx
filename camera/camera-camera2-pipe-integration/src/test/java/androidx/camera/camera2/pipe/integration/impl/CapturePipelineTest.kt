/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.impl

import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureRequest.CONTROL_AE_MODE
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.params.MeteringRectangle
import android.media.Image
import android.os.Build
import android.os.Looper
import android.view.Surface
import androidx.camera.camera2.pipe.AeMode
import androidx.camera.camera2.pipe.AfMode
import androidx.camera.camera2.pipe.AwbMode
import androidx.camera.camera2.pipe.FrameMetadata
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Lock3ABehavior
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.integration.adapter.CameraStateAdapter
import androidx.camera.camera2.pipe.integration.adapter.CaptureConfigAdapter
import androidx.camera.camera2.pipe.integration.adapter.CaptureResultAdapter
import androidx.camera.camera2.pipe.integration.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.integration.adapter.ZslControl
import androidx.camera.camera2.pipe.integration.adapter.asListenableFuture
import androidx.camera.camera2.pipe.integration.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.pipe.integration.compat.quirk.CameraQuirks
import androidx.camera.camera2.pipe.integration.compat.workaround.AeFpsRange
import androidx.camera.camera2.pipe.integration.compat.workaround.CapturePipelineTorchCorrection
import androidx.camera.camera2.pipe.integration.compat.workaround.NoOpAutoFlashAEModeDisabler
import androidx.camera.camera2.pipe.integration.compat.workaround.NoOpTemplateParamsOverride
import androidx.camera.camera2.pipe.integration.compat.workaround.NotUseFlashModeTorchFor3aUpdate
import androidx.camera.camera2.pipe.integration.compat.workaround.NotUseTorchAsFlash
import androidx.camera.camera2.pipe.integration.compat.workaround.OutputSizesCorrector
import androidx.camera.camera2.pipe.integration.compat.workaround.UseTorchAsFlashImpl
import androidx.camera.camera2.pipe.integration.config.UseCaseGraphConfig
import androidx.camera.camera2.pipe.integration.interop.CaptureRequestOptions
import androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop
import androidx.camera.camera2.pipe.integration.testing.FakeCameraGraph
import androidx.camera.camera2.pipe.integration.testing.FakeCameraGraphSession
import androidx.camera.camera2.pipe.integration.testing.FakeCameraProperties
import androidx.camera.camera2.pipe.integration.testing.FakeUseCaseCamera
import androidx.camera.camera2.pipe.integration.testing.FakeUseCaseCameraRequestControl
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeFrameInfo
import androidx.camera.camera2.pipe.testing.FakeFrameMetadata
import androidx.camera.camera2.pipe.testing.FakeRequestFailure
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.ImmediateSurface
import androidx.camera.core.impl.MutableOptionsBundle
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.core.internal.CameraCaptureResultImageInfo
import androidx.camera.testing.impl.mocks.MockScreenFlash
import androidx.testutils.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.ExecutionException
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.collections.removeFirst as removeFirstKt
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.StreamConfigurationMapBuilder
import org.robolectric.util.ReflectionHelpers

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalCamera2Interop::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class CapturePipelineTest {
    private val testScope = TestScope()
    private val testDispatcher = StandardTestDispatcher(testScope.testScheduler)

    @get:Rule val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val fakeUseCaseThreads by lazy {
        UseCaseThreads(testScope, testDispatcher.asExecutor(), testDispatcher)
    }

    private val fakeRequestControl =
        object : FakeUseCaseCameraRequestControl() {
            val torchUpdateEventList = mutableListOf<Boolean>()
            val setTorchSemaphore = Semaphore(0)

            override suspend fun setTorchAsync(enabled: Boolean): Deferred<Result3A> {
                torchUpdateEventList.add(enabled)
                setTorchSemaphore.release()
                return CompletableDeferred(Result3A(Result3A.Status.OK))
            }
        }
    private val comboRequestListener = ComboRequestListener()
    private val fakeCameraGraphSession =
        object : FakeCameraGraphSession() {
            var requestHandler: (List<Request>) -> Unit = { requests -> requests.complete() }
            val lock3ASemaphore = Semaphore(0)
            val unlock3ASemaphore = Semaphore(0)
            val lock3AForCaptureSemaphore = Semaphore(0)
            val unlock3APostCaptureSemaphore = Semaphore(0)
            val submitSemaphore = Semaphore(0)

            var virtualTimeAtLock3AForCapture: Long = -1
            var triggerAfAtLock3AForCapture: Boolean = false
            var waitForAwbAtLock3AForCapture: Boolean = false

            var cancelAfAtUnlock3AForCapture: Boolean = false

            override suspend fun lock3A(
                aeMode: AeMode?,
                afMode: AfMode?,
                awbMode: AwbMode?,
                aeRegions: List<MeteringRectangle>?,
                afRegions: List<MeteringRectangle>?,
                awbRegions: List<MeteringRectangle>?,
                aeLockBehavior: Lock3ABehavior?,
                afLockBehavior: Lock3ABehavior?,
                awbLockBehavior: Lock3ABehavior?,
                afTriggerStartAeMode: AeMode?,
                convergedCondition: ((FrameMetadata) -> Boolean)?,
                lockedCondition: ((FrameMetadata) -> Boolean)?,
                frameLimit: Int,
                timeLimitNs: Long
            ): Deferred<Result3A> {
                lock3ASemaphore.release()
                return CompletableDeferred(Result3A(Result3A.Status.OK))
            }

            override suspend fun unlock3A(
                ae: Boolean?,
                af: Boolean?,
                awb: Boolean?,
                unlockedCondition: ((FrameMetadata) -> Boolean)?,
                frameLimit: Int,
                timeLimitNs: Long
            ): Deferred<Result3A> {
                unlock3ASemaphore.release()
                return CompletableDeferred(Result3A(Result3A.Status.OK))
            }

            override suspend fun lock3AForCapture(
                lockedCondition: ((FrameMetadata) -> Boolean)?,
                frameLimit: Int,
                timeLimitNs: Long
            ): Deferred<Result3A> {
                lock3AForCaptureSemaphore.release()
                return CompletableDeferred(Result3A(Result3A.Status.OK))
            }

            override suspend fun lock3AForCapture(
                triggerAf: Boolean,
                waitForAwb: Boolean,
                frameLimit: Int,
                timeLimitNs: Long
            ): Deferred<Result3A> {
                virtualTimeAtLock3AForCapture = testScope.currentTime
                triggerAfAtLock3AForCapture = triggerAf
                waitForAwbAtLock3AForCapture = waitForAwb
                lock3AForCaptureSemaphore.release()
                return CompletableDeferred(Result3A(Result3A.Status.OK))
            }

            override fun submit(requests: List<Request>) {
                requestHandler(requests)
                submitSemaphore.release()
            }

            override suspend fun unlock3APostCapture(cancelAf: Boolean): Deferred<Result3A> {
                cancelAfAtUnlock3AForCapture = cancelAf
                unlock3APostCaptureSemaphore.release()
                return CompletableDeferred(Result3A(Result3A.Status.OK))
            }
        }
    private val fakeStreamId = StreamId(0)
    private val fakeSurfaceTexture = SurfaceTexture(0).apply { setDefaultBufferSize(640, 480) }
    private val fakeSurface = Surface(fakeSurfaceTexture)
    private val fakeDeferrableSurface = ImmediateSurface(fakeSurface)
    private val singleConfig =
        CaptureConfig.Builder().apply { addSurface(fakeDeferrableSurface) }.build()
    private val singleRequest =
        Request(
            streams = emptyList(),
            listeners = emptyList(),
            parameters = emptyMap(),
            extras = emptyMap(),
            template = RequestTemplate(CameraDevice.TEMPLATE_STILL_CAPTURE),
        )
    private val fakeCameraProperties =
        FakeCameraProperties(
            FakeCameraMetadata(
                mapOf(CameraCharacteristics.FLASH_INFO_AVAILABLE to true),
            )
        )
    private val fakeUseCaseGraphConfig =
        UseCaseGraphConfig(
            graph = FakeCameraGraph(fakeCameraGraphSession = fakeCameraGraphSession),
            surfaceToStreamMap = mapOf(fakeDeferrableSurface to fakeStreamId),
            cameraStateAdapter = CameraStateAdapter(),
        )
    private val fakeZslControl =
        object : ZslControl {
            var _isZslDisabledByUseCaseConfig = false
            var _isZslDisabledByFlashMode = false
            var imageProxyToDequeue: ImageProxy? = null

            override fun addZslConfig(sessionConfigBuilder: SessionConfig.Builder) {
                // Do nothing
            }

            override fun isZslSurface(
                surface: DeferrableSurface,
                sessionConfig: SessionConfig
            ): Boolean {
                return false
            }

            override fun setZslDisabledByUserCaseConfig(disabled: Boolean) {
                _isZslDisabledByUseCaseConfig = disabled
            }

            override fun isZslDisabledByUserCaseConfig(): Boolean {
                return _isZslDisabledByUseCaseConfig
            }

            override fun setZslDisabledByFlashMode(disabled: Boolean) {
                _isZslDisabledByFlashMode = disabled
            }

            override fun isZslDisabledByFlashMode(): Boolean {
                return _isZslDisabledByFlashMode
            }

            override fun dequeueImageFromBuffer(): ImageProxy? {
                return imageProxyToDequeue
            }
        }
    private val fakeCaptureConfigAdapter =
        CaptureConfigAdapter(
            fakeCameraProperties,
            fakeUseCaseGraphConfig,
            fakeZslControl,
            fakeUseCaseThreads,
            NoOpTemplateParamsOverride,
        )
    private var runningRepeatingJob: Job? = null
        set(value) {
            runningRepeatingJob?.cancel()
            field = value
        }

    private lateinit var flashControl: FlashControl
    private lateinit var state3AControl: State3AControl
    private lateinit var torchControl: TorchControl
    private lateinit var capturePipeline: CapturePipelineImpl

    private lateinit var fakeUseCaseCameraState: UseCaseCameraState

    private val screenFlash = MockScreenFlash()

    @Before
    fun setUp() {
        val fakeUseCaseCamera = FakeUseCaseCamera(requestControl = fakeRequestControl)

        state3AControl =
            State3AControl(
                    fakeCameraProperties,
                    NoOpAutoFlashAEModeDisabler,
                    AeFpsRange(
                        CameraQuirks(
                            FakeCameraMetadata(),
                            StreamConfigurationMapCompat(
                                StreamConfigurationMapBuilder.newBuilder().build(),
                                OutputSizesCorrector(
                                    FakeCameraMetadata(),
                                    StreamConfigurationMapBuilder.newBuilder().build()
                                )
                            )
                        )
                    ),
                )
                .apply { useCaseCamera = fakeUseCaseCamera }

        torchControl =
            TorchControl(
                    fakeCameraProperties,
                    state3AControl,
                    fakeUseCaseThreads,
                )
                .also {
                    it.useCaseCamera = fakeUseCaseCamera

                    // Ensure the control is updated after the UseCaseCamera been set.
                    assertThat(fakeRequestControl.setTorchSemaphore.tryAcquire(testScope)).isTrue()
                    fakeRequestControl.torchUpdateEventList.clear()
                }

        flashControl =
            FlashControl(
                    cameraProperties = fakeCameraProperties,
                    state3AControl = state3AControl,
                    threads = fakeUseCaseThreads,
                    torchControl = torchControl,
                    useFlashModeTorchFor3aUpdate = NotUseFlashModeTorchFor3aUpdate,
                )
                .apply { setScreenFlash(this@CapturePipelineTest.screenFlash) }

        fakeUseCaseCameraState =
            UseCaseCameraState(
                fakeUseCaseGraphConfig,
                fakeUseCaseThreads,
                sessionProcessorManager = null,
                templateParamsOverride = NoOpTemplateParamsOverride,
            )

        capturePipeline =
            CapturePipelineImpl(
                configAdapter = fakeCaptureConfigAdapter,
                cameraProperties = fakeCameraProperties,
                requestListener = comboRequestListener,
                threads = fakeUseCaseThreads,
                torchControl = torchControl,
                useCaseGraphConfig = fakeUseCaseGraphConfig,
                useCaseCameraState = fakeUseCaseCameraState,
                useTorchAsFlash = NotUseTorchAsFlash,
                sessionProcessorManager = null,
                flashControl = flashControl,
            )
    }

    @After
    fun tearDown() {
        runningRepeatingJob = null
        fakeSurface.release()
        fakeSurfaceTexture.release()
    }

    @Test
    fun miniLatency_flashOn_shouldTriggerAePreCapture(): Unit = runTest {
        flashOn_shouldTriggerAePreCapture(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
    }

    @Test
    fun maxQuality_flashOn_shouldTriggerAePreCapture(): Unit = runTest {
        flashOn_shouldTriggerAePreCapture(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
    }

    private suspend fun TestScope.flashOn_shouldTriggerAePreCapture(imageCaptureMode: Int) {
        // Arrange.
        val requestList = mutableListOf<Request>()
        fakeCameraGraphSession.requestHandler = { requests -> requestList.addAll(requests) }

        // Act.
        capturePipeline.submitStillCaptures(
            configs = listOf(singleConfig),
            requestTemplate = RequestTemplate(CameraDevice.TEMPLATE_STILL_CAPTURE),
            sessionConfigOptions = MutableOptionsBundle.create(),
            captureMode = imageCaptureMode,
            flashMode = ImageCapture.FLASH_MODE_ON,
            flashType = ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
        )

        // Assert.
        assertThat(fakeCameraGraphSession.lock3AForCaptureSemaphore.tryAcquire(this)).isTrue()

        // Complete the capture request.
        assertThat(fakeCameraGraphSession.submitSemaphore.tryAcquire(this)).isTrue()
        requestList.complete()

        // Assert 2, unlock3APostCapture should be called.
        assertThat(fakeCameraGraphSession.unlock3APostCaptureSemaphore.tryAcquire(this)).isTrue()
    }

    @Test
    fun miniLatency_flashAutoFlashRequired_shouldTriggerAePreCapture(): Unit = runTest {
        flashAutoFlashRequired_shouldTriggerAePreCapture(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
    }

    @Test
    fun maxQuality_flashAutoFlashRequired_shouldTriggerAePreCapture(): Unit = runTest {
        flashAutoFlashRequired_shouldTriggerAePreCapture(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
    }

    private suspend fun TestScope.flashAutoFlashRequired_shouldTriggerAePreCapture(
        imageCaptureMode: Int
    ) {
        // Arrange.
        comboRequestListener.simulateRepeatingResult(
            resultParameters =
                mapOf(
                    CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED,
                )
        )
        val requestList = mutableListOf<Request>()
        fakeCameraGraphSession.requestHandler = { requests -> requestList.addAll(requests) }

        // Act.
        capturePipeline.submitStillCaptures(
            configs = listOf(singleConfig),
            requestTemplate = RequestTemplate(CameraDevice.TEMPLATE_STILL_CAPTURE),
            sessionConfigOptions = MutableOptionsBundle.create(),
            captureMode = imageCaptureMode,
            flashMode = ImageCapture.FLASH_MODE_AUTO,
            flashType = ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
        )

        // Assert 1, lock3AForCapture should be called, but not call unlock3APostCapture
        // (before capturing is finished).
        assertThat(fakeCameraGraphSession.lock3AForCaptureSemaphore.tryAcquire(this)).isTrue()
        assertThat(fakeCameraGraphSession.unlock3APostCaptureSemaphore.tryAcquire(this)).isFalse()

        // Complete the capture request.
        assertThat(fakeCameraGraphSession.submitSemaphore.tryAcquire(this)).isTrue()
        requestList.complete()

        // Assert 2, unlock3APostCapture should be called.
        assertThat(fakeCameraGraphSession.unlock3APostCaptureSemaphore.tryAcquire(this)).isTrue()
    }

    @Test
    fun miniLatency_withTorchAsFlashQuirk_shouldOpenTorch(): Unit = runTest {
        withTorchAsFlashQuirk_shouldOpenTorch(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
    }

    @Test
    fun maxQuality_withTorchAsFlashQuirk_shouldOpenTorch(): Unit = runTest {
        withTorchAsFlashQuirk_shouldOpenTorch(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
    }

    private suspend fun TestScope.withTorchAsFlashQuirk_shouldOpenTorch(imageCaptureMode: Int) {
        // Arrange.
        capturePipeline =
            CapturePipelineImpl(
                configAdapter = fakeCaptureConfigAdapter,
                cameraProperties = fakeCameraProperties,
                requestListener = comboRequestListener,
                threads = fakeUseCaseThreads,
                torchControl = torchControl,
                useCaseGraphConfig = fakeUseCaseGraphConfig,
                useCaseCameraState = fakeUseCaseCameraState,
                useTorchAsFlash = UseTorchAsFlashImpl,
                sessionProcessorManager = null,
                flashControl = flashControl,
            )

        val requestList = mutableListOf<Request>()
        fakeCameraGraphSession.requestHandler = { requests -> requestList.addAll(requests) }

        // Act.
        capturePipeline.submitStillCaptures(
            configs = listOf(singleConfig),
            requestTemplate = RequestTemplate(CameraDevice.TEMPLATE_STILL_CAPTURE),
            sessionConfigOptions = MutableOptionsBundle.create(),
            captureMode = imageCaptureMode,
            flashMode = ImageCapture.FLASH_MODE_ON,
            flashType = ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
        )

        // Assert 1, torch should be turned on.
        assertThat(fakeRequestControl.setTorchSemaphore.tryAcquire(this)).isTrue()
        assertThat(fakeRequestControl.torchUpdateEventList.size).isEqualTo(1)
        assertThat(fakeRequestControl.torchUpdateEventList.removeFirstKt()).isTrue()

        // Complete the capture request.
        assertThat(fakeCameraGraphSession.submitSemaphore.tryAcquire(this)).isTrue()
        requestList.complete()

        // Assert 2, torch should be turned off.
        assertThat(fakeRequestControl.setTorchSemaphore.tryAcquire(this)).isTrue()
        assertThat(fakeRequestControl.torchUpdateEventList.size).isEqualTo(1)
        assertThat(fakeRequestControl.torchUpdateEventList.removeFirstKt()).isFalse()
    }

    @Test
    fun miniLatency_withTemplateRecord_shouldOpenTorch(): Unit = runTest {
        withTemplateRecord_shouldOpenTorch(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
    }

    @Test
    fun maxQuality_withTemplateRecord_shouldOpenTorch(): Unit = runTest {
        withTemplateRecord_shouldOpenTorch(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
    }

    private suspend fun TestScope.withTemplateRecord_shouldOpenTorch(imageCaptureMode: Int) {
        // Arrange.
        capturePipeline.template = CameraDevice.TEMPLATE_RECORD

        val requestList = mutableListOf<Request>()
        fakeCameraGraphSession.requestHandler = { requests -> requestList.addAll(requests) }

        // Act.
        capturePipeline.submitStillCaptures(
            configs = listOf(singleConfig),
            requestTemplate = RequestTemplate(CameraDevice.TEMPLATE_STILL_CAPTURE),
            sessionConfigOptions = MutableOptionsBundle.create(),
            captureMode = imageCaptureMode,
            flashMode = ImageCapture.FLASH_MODE_ON,
            flashType = ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
        )

        // Assert 1, torch should be turned on.
        assertThat(fakeRequestControl.setTorchSemaphore.tryAcquire(this)).isTrue()
        assertThat(fakeRequestControl.torchUpdateEventList.size).isEqualTo(1)
        assertThat(fakeRequestControl.torchUpdateEventList.removeFirstKt()).isTrue()

        // Complete the capture request.
        assertThat(fakeCameraGraphSession.submitSemaphore.tryAcquire(this)).isTrue()
        requestList.complete()

        // Assert 2, torch should be turned off.
        assertThat(fakeRequestControl.setTorchSemaphore.tryAcquire(this)).isTrue()
        assertThat(fakeRequestControl.torchUpdateEventList.size).isEqualTo(1)
        assertThat(fakeRequestControl.torchUpdateEventList.removeFirstKt()).isFalse()
    }

    @Test
    fun miniLatency_withFlashTypeTorch_shouldOpenTorch(): Unit = runTest {
        withFlashTypeTorch_shouldOpenTorch(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
    }

    @Test
    fun maxQuality_withFlashTypeTorch_shouldOpenTorch(): Unit = runTest {
        withFlashTypeTorch_shouldOpenTorch(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
    }

    private suspend fun TestScope.withFlashTypeTorch_shouldOpenTorch(imageCaptureMode: Int) {
        // Arrange.
        val requestList = mutableListOf<Request>()
        fakeCameraGraphSession.requestHandler = { requests -> requestList.addAll(requests) }

        // Act.
        capturePipeline.submitStillCaptures(
            configs = listOf(singleConfig),
            requestTemplate = RequestTemplate(CameraDevice.TEMPLATE_STILL_CAPTURE),
            sessionConfigOptions = MutableOptionsBundle.create(),
            captureMode = imageCaptureMode,
            flashMode = ImageCapture.FLASH_MODE_ON,
            flashType = ImageCapture.FLASH_TYPE_USE_TORCH_AS_FLASH,
        )

        // Assert 1, torch should be turned on.
        assertThat(fakeRequestControl.setTorchSemaphore.tryAcquire(this)).isTrue()
        assertThat(fakeRequestControl.torchUpdateEventList.size).isEqualTo(1)
        assertThat(fakeRequestControl.torchUpdateEventList.removeFirstKt()).isTrue()

        // Complete the capture request.
        assertThat(fakeCameraGraphSession.submitSemaphore.tryAcquire(this)).isTrue()
        requestList.complete()

        // Assert 2, torch should be turned off.
        assertThat(fakeRequestControl.setTorchSemaphore.tryAcquire(this)).isTrue()
        assertThat(fakeRequestControl.torchUpdateEventList.size).isEqualTo(1)
        assertThat(fakeRequestControl.torchUpdateEventList.removeFirstKt()).isFalse()
    }

    @Test
    fun miniLatency_flashRequired_withFlashTypeTorch_shouldLock3A(): Unit = runTest {
        withFlashTypeTorch_shouldLock3A(
            ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
            ImageCapture.FLASH_MODE_ON
        )
    }

    @Test
    fun maxQuality_withFlashTypeTorch_shouldLock3A(): Unit = runTest {
        withFlashTypeTorch_shouldLock3A(
            ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY,
            ImageCapture.FLASH_MODE_OFF
        )
    }

    private suspend fun TestScope.withFlashTypeTorch_shouldLock3A(
        imageCaptureMode: Int,
        flashMode: Int
    ) {
        // Arrange.
        val requestList = mutableListOf<Request>()
        fakeCameraGraphSession.requestHandler = { requests -> requestList.addAll(requests) }

        // Act.
        capturePipeline.submitStillCaptures(
            configs = listOf(singleConfig),
            requestTemplate = RequestTemplate(CameraDevice.TEMPLATE_STILL_CAPTURE),
            sessionConfigOptions = MutableOptionsBundle.create(),
            captureMode = imageCaptureMode,
            flashMode = flashMode,
            flashType = ImageCapture.FLASH_TYPE_USE_TORCH_AS_FLASH,
        )

        // Assert 1, should call lock3A, but not call unlock3A (before capturing is finished).
        assertThat(fakeCameraGraphSession.lock3ASemaphore.tryAcquire(this)).isTrue()
        assertThat(fakeCameraGraphSession.unlock3ASemaphore.tryAcquire(this)).isFalse()

        // Complete the capture request.
        assertThat(fakeCameraGraphSession.submitSemaphore.tryAcquire(this)).isTrue()
        requestList.complete()

        advanceUntilIdle()
        // Assert 2, should call unlock3A.
        assertThat(fakeCameraGraphSession.unlock3ASemaphore.tryAcquire(this)).isTrue()
    }

    @Test
    fun miniLatency_withFlashTypeTorch_shouldNotLock3A(): Unit = runTest {
        // Act.
        capturePipeline
            .submitStillCaptures(
                configs = listOf(singleConfig),
                requestTemplate = RequestTemplate(CameraDevice.TEMPLATE_STILL_CAPTURE),
                sessionConfigOptions = MutableOptionsBundle.create(),
                captureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                flashMode = ImageCapture.FLASH_MODE_OFF,
                flashType = ImageCapture.FLASH_TYPE_USE_TORCH_AS_FLASH,
            )
            .awaitAllWithTimeout()

        // Assert, there is no invocation on lock3A().
        assertThat(fakeCameraGraphSession.lock3ASemaphore.tryAcquire(this)).isFalse()
    }

    @Test
    fun withFlashTypeTorch_torchAlreadyOn_skipTurnOnTorch(): Unit = runTest {
        // Arrange.
        // Ensure the torch is already turned on before capturing.
        torchControl.setTorchAsync(true)
        assertThat(fakeRequestControl.setTorchSemaphore.tryAcquire(this)).isTrue()

        // Act.
        capturePipeline
            .submitStillCaptures(
                configs = listOf(singleConfig),
                requestTemplate = RequestTemplate(CameraDevice.TEMPLATE_STILL_CAPTURE),
                sessionConfigOptions = MutableOptionsBundle.create(),
                captureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                flashMode = ImageCapture.FLASH_MODE_ON,
                flashType = ImageCapture.FLASH_TYPE_USE_TORCH_AS_FLASH,
            )
            .awaitAllWithTimeout()

        // Assert, there is no invocation on setTorch().
        assertThat(fakeRequestControl.setTorchSemaphore.tryAcquire(this)).isFalse()
    }

    @Test
    fun miniLatency_shouldNotAePreCapture(): Unit = runTest {
        // Act.
        capturePipeline
            .submitStillCaptures(
                configs = listOf(singleConfig),
                requestTemplate = RequestTemplate(CameraDevice.TEMPLATE_STILL_CAPTURE),
                sessionConfigOptions = MutableOptionsBundle.create(),
                captureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                flashMode = ImageCapture.FLASH_MODE_OFF,
                flashType = ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
            )
            .awaitAllWithTimeout()

        // Assert, there is only 1 single capture request.
        assertThat(fakeCameraGraphSession.lock3AForCaptureSemaphore.tryAcquire(this)).isFalse()
    }

    @Config(minSdk = 23)
    @Test
    fun submitZslCaptureRequests_withZslTemplate_templateZeroShutterLagSent(): Unit = runTest {
        // Arrange.
        val requestList = mutableListOf<Request>()
        fakeCameraGraphSession.requestHandler = { requests ->
            requestList.addAll(requests)
            requests.complete()
        }
        val imageCaptureConfig =
            CaptureConfig.Builder().let {
                it.addSurface(fakeDeferrableSurface)
                it.templateType = CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG
                it.build()
            }
        configureZslControl()

        // Act.
        capturePipeline
            .submitStillCaptures(
                listOf(imageCaptureConfig),
                RequestTemplate(CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG),
                MutableOptionsBundle.create(),
                captureMode = ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG,
                flashMode = ImageCapture.FLASH_MODE_OFF,
                flashType = ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
            )
            .awaitAllWithTimeout()
        advanceUntilIdle()

        // Assert.
        val request = requestList.single()
        assertThat(request.streams.single()).isEqualTo(fakeStreamId)
        assertThat(request.template)
            .isEqualTo(RequestTemplate(CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG))
    }

    @Config(minSdk = 23)
    @Test
    fun submitZslCaptureRequests_withNoTemplate_templateStillPictureSent(): Unit = runTest {
        // Arrange.
        val requestList = mutableListOf<Request>()
        fakeCameraGraphSession.requestHandler = { requests ->
            requestList.addAll(requests)
            requests.complete()
        }
        val imageCaptureConfig =
            CaptureConfig.Builder().let {
                it.addSurface(fakeDeferrableSurface)
                it.build()
            }
        configureZslControl()

        // Act.
        capturePipeline
            .submitStillCaptures(
                listOf(imageCaptureConfig),
                RequestTemplate(CameraDevice.TEMPLATE_PREVIEW),
                MutableOptionsBundle.create(),
                captureMode = ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG,
                flashMode = ImageCapture.FLASH_MODE_OFF,
                flashType = ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
            )
            .awaitAllWithTimeout()

        // Assert.
        val request = requestList.single()
        assertThat(request.streams.single()).isEqualTo(fakeStreamId)
        assertThat(request.template).isEqualTo(RequestTemplate(CameraDevice.TEMPLATE_STILL_CAPTURE))
    }

    @Config(minSdk = 23)
    @Test
    fun submitZslCaptureRequests_withZslDisabledByUseCaseConfig_templateStillPictureSent(): Unit =
        runTest {
            // Arrange.
            val requestList = mutableListOf<Request>()
            fakeCameraGraphSession.requestHandler = { requests ->
                requestList.addAll(requests)
                requests.complete()
            }
            val imageCaptureConfig =
                CaptureConfig.Builder().let {
                    it.addSurface(fakeDeferrableSurface)
                    it.templateType = CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG
                    it.build()
                }
            configureZslControl()
            fakeZslControl.setZslDisabledByUserCaseConfig(true)

            // Act.
            capturePipeline
                .submitStillCaptures(
                    listOf(imageCaptureConfig),
                    RequestTemplate(CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG),
                    MutableOptionsBundle.create(),
                    captureMode = ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG,
                    flashMode = ImageCapture.FLASH_MODE_OFF,
                    flashType = ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
                )
                .awaitAllWithTimeout()

            // Assert.
            val request = requestList.single()
            assertThat(request.streams.single()).isEqualTo(fakeStreamId)
            assertThat(request.template)
                .isEqualTo(RequestTemplate(CameraDevice.TEMPLATE_STILL_CAPTURE))
        }

    @Config(minSdk = 23)
    @Test
    fun submitZslCaptureRequests_withZslDisabledByFlashMode_templateStillPictureSent(): Unit =
        runTest {
            // Arrange.
            val requestList = mutableListOf<Request>()
            fakeCameraGraphSession.requestHandler = { requests ->
                requestList.addAll(requests)
                requests.complete()
            }
            val imageCaptureConfig =
                CaptureConfig.Builder().let {
                    it.addSurface(fakeDeferrableSurface)
                    it.templateType = CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG
                    it.build()
                }
            configureZslControl()
            fakeZslControl.setZslDisabledByFlashMode(true)

            // Act.
            capturePipeline
                .submitStillCaptures(
                    listOf(imageCaptureConfig),
                    RequestTemplate(CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG),
                    MutableOptionsBundle.create(),
                    captureMode = ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG,
                    flashMode = ImageCapture.FLASH_MODE_OFF,
                    flashType = ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
                )
                .awaitAllWithTimeout()

            // Assert.
            val request = requestList.single()
            assertThat(request.streams.single()).isEqualTo(fakeStreamId)
            assertThat(request.template)
                .isEqualTo(RequestTemplate(CameraDevice.TEMPLATE_STILL_CAPTURE))
        }

    private fun configureZslControl() {
        val fakeImageProxy: ImageProxy = mock()
        val fakeCaptureResult =
            CaptureResultAdapter(FakeRequestMetadata(), FrameNumber(1), FakeFrameInfo())
        val fakeImageInfo = CameraCaptureResultImageInfo(fakeCaptureResult)
        val fakeImage: Image = mock()
        whenever(fakeImageProxy.imageInfo).thenReturn(fakeImageInfo)
        whenever(fakeImageProxy.image).thenReturn(fakeImage)
        fakeZslControl.imageProxyToDequeue = fakeImageProxy
    }

    @Test
    fun captureFailure_taskShouldFailure(): Unit = runTest {
        // Arrange.
        fakeCameraGraphSession.requestHandler = { requests ->
            requests.forEach { request ->
                // Callback capture fail immediately.
                request.listeners.forEach {
                    val requestMetadata = FakeRequestMetadata()
                    val frameNumber = FrameNumber(100L)
                    it.onFailed(
                        requestMetadata = requestMetadata,
                        frameNumber = frameNumber,
                        requestFailure = FakeRequestFailure(requestMetadata, frameNumber)
                    )
                }
            }
        }

        // Act.
        val resultDeferredList =
            capturePipeline.submitStillCaptures(
                configs = listOf(singleConfig),
                requestTemplate = RequestTemplate(CameraDevice.TEMPLATE_STILL_CAPTURE),
                sessionConfigOptions = MutableOptionsBundle.create(),
                captureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                flashMode = ImageCapture.FLASH_MODE_OFF,
                flashType = ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
            )

        // Assert.
        advanceUntilIdle()
        val exception =
            assertFailsWith(ImageCaptureException::class) {
                resultDeferredList.awaitAllWithTimeout()
            }
        assertThat(exception.imageCaptureError).isEqualTo(ImageCapture.ERROR_CAPTURE_FAILED)
    }

    @Test
    fun captureCancel_taskShouldFailureWithCAMERA_CLOSED(): Unit = runTest {
        // Arrange.
        fakeCameraGraphSession.requestHandler = { requests ->
            requests.forEach { request ->
                // Callback capture abort immediately.
                request.listeners.forEach { it.onAborted(singleRequest) }
            }
        }

        // Act.
        val resultDeferredList =
            capturePipeline.submitStillCaptures(
                configs = listOf(singleConfig),
                requestTemplate = RequestTemplate(CameraDevice.TEMPLATE_STILL_CAPTURE),
                sessionConfigOptions = MutableOptionsBundle.create(),
                captureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                flashMode = ImageCapture.FLASH_MODE_OFF,
                flashType = ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
            )

        // Assert.
        advanceUntilIdle()
        val exception =
            Assert.assertThrows(ExecutionException::class.java) {
                Futures.allAsList(resultDeferredList.map { it.asListenableFuture() })
                    .get(2, TimeUnit.SECONDS)
            }
        Assert.assertTrue(exception.cause is ImageCaptureException)
        assertThat((exception.cause as ImageCaptureException).imageCaptureError)
            .isEqualTo(ImageCapture.ERROR_CAMERA_CLOSED)
    }

    @Test
    fun stillCaptureWithFlashStopRepeatingQuirk_shouldStopRepeatingTemporarily() = runTest {
        // Arrange
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "SAMSUNG")
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "SM-A716")

        val submittedRequestList = mutableListOf<Request>()
        fakeCameraGraphSession.requestHandler = { requests ->
            submittedRequestList.addAll(requests)
        }
        fakeUseCaseCameraState.update(streams = setOf(StreamId(0)))

        // Act.
        capturePipeline.submitStillCaptures(
            configs =
                listOf(
                    CaptureConfig.Builder()
                        .apply {
                            addSurface(fakeDeferrableSurface)
                            implementationOptions =
                                CaptureRequestOptions.Builder()
                                    .apply {
                                        setCaptureRequestOption(
                                            CONTROL_AE_MODE,
                                            CONTROL_AE_MODE_ON_ALWAYS_FLASH
                                        )
                                    }
                                    .build()
                        }
                        .build()
                ),
            requestTemplate = RequestTemplate(CameraDevice.TEMPLATE_STILL_CAPTURE),
            sessionConfigOptions = MutableOptionsBundle.create(),
            captureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
            flashMode = ImageCapture.FLASH_MODE_ON,
            flashType = ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
        )

        // Assert, stopRepeating -> submit -> startRepeating flow should be used.
        assertThat(fakeCameraGraphSession.stopRepeatingSemaphore.tryAcquire(this)).isTrue()

        assertThat(fakeCameraGraphSession.submitSemaphore.tryAcquire(this)).isTrue()

        // Completing the submitted capture request.
        submittedRequestList.complete()

        assertThat(fakeCameraGraphSession.repeatingRequestSemaphore.tryAcquire(this)).isTrue()
    }

    @Test
    fun stillCaptureWithFlashStopRepeatingQuirkNotEnabled_shouldNotStopRepeating() = runTest {
        // Arrange
        val submittedRequestList = mutableListOf<Request>()
        fakeCameraGraphSession.requestHandler = { requests ->
            submittedRequestList.addAll(requests)
        }
        fakeUseCaseCameraState.update(streams = setOf(StreamId(0)))

        // Act.
        capturePipeline.submitStillCaptures(
            configs =
                listOf(
                    CaptureConfig.Builder()
                        .apply {
                            addSurface(fakeDeferrableSurface)
                            implementationOptions =
                                CaptureRequestOptions.Builder()
                                    .apply {
                                        setCaptureRequestOption(
                                            CONTROL_AE_MODE,
                                            CONTROL_AE_MODE_ON_ALWAYS_FLASH
                                        )
                                    }
                                    .build()
                        }
                        .build()
                ),
            requestTemplate = RequestTemplate(CameraDevice.TEMPLATE_STILL_CAPTURE),
            sessionConfigOptions = MutableOptionsBundle.create(),
            captureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
            flashMode = ImageCapture.FLASH_MODE_ON,
            flashType = ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
        )

        // Assert, repeating should not be stopped when quirk not enabled.
        assertThat(fakeCameraGraphSession.stopRepeatingSemaphore.tryAcquire(this)).isFalse()

        assertThat(fakeCameraGraphSession.submitSemaphore.tryAcquire(this)).isTrue()

        // Resetting repeatingRequestSemaphore because startRepeating can be called before
        fakeCameraGraphSession.repeatingRequestSemaphore = Semaphore(0)

        // Completing the submitted capture request.
        submittedRequestList.complete()

        assertThat(fakeCameraGraphSession.repeatingRequestSemaphore.tryAcquire(this)).isFalse()
    }

    @Test
    fun torchAsFlash_torchCorrection_shouldTurnsTorchOffOn(): Unit = runTest {
        torchStateCorrectionTest(ImageCapture.FLASH_TYPE_USE_TORCH_AS_FLASH)
    }

    @Test
    fun defaultCapture_torchCorrection_shouldTurnsTorchOffOn(): Unit = runTest {
        torchStateCorrectionTest(ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH)
    }

    private suspend fun TestScope.torchStateCorrectionTest(flashType: Int) {
        // Arrange.
        torchControl.setTorchAsync(torch = true).join()
        verifyTorchState(true)

        val requestList = mutableListOf<Request>()
        fakeCameraGraphSession.requestHandler = { requests -> requestList.addAll(requests) }
        val capturePipelineTorchCorrection =
            CapturePipelineTorchCorrection(
                cameraProperties = FakeCameraProperties(),
                capturePipelineImpl = capturePipeline,
                threads = fakeUseCaseThreads,
                torchControl = torchControl,
            )

        // Act.
        capturePipelineTorchCorrection.submitStillCaptures(
            configs = listOf(singleConfig),
            requestTemplate = RequestTemplate(CameraDevice.TEMPLATE_STILL_CAPTURE),
            sessionConfigOptions = MutableOptionsBundle.create(),
            captureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
            flashMode = ImageCapture.FLASH_MODE_ON,
            flashType = flashType,
        )

        assertThat(fakeCameraGraphSession.submitSemaphore.tryAcquire(this)).isTrue()
        assertThat(fakeRequestControl.setTorchSemaphore.tryAcquire(this)).isFalse()
        // Complete the capture request.
        requestList.complete()

        // Assert, the Torch should be turned off, and then turned on.
        verifyTorchState(false)
        verifyTorchState(true)
        // No more invocation to set Torch mode.
        assertThat(fakeRequestControl.torchUpdateEventList.size).isEqualTo(0)
    }

    private fun TestScope.verifyTorchState(state: Boolean) {
        assertThat(fakeRequestControl.setTorchSemaphore.tryAcquire(this)).isTrue()
        assertThat(fakeRequestControl.torchUpdateEventList.removeFirstKt() == state).isTrue()
    }

    // TODO(b/326170400): port torch related precapture tests

    @Test
    fun lock3aTriggered_whenScreenFlashPreCaptureCalled() = runTest {
        capturePipeline.invokeScreenFlashPreCaptureTasks(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)

        assertThat(fakeCameraGraphSession.lock3AForCaptureSemaphore.tryAcquire(this)).isTrue()
    }

    @Test
    fun lock3aTriggeredAfterTimeout_whenScreenFlashApplyNotCompleted() = runTest {
        screenFlash.setApplyCompletedInstantly(false)

        capturePipeline.invokeScreenFlashPreCaptureTasks(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)

        assertThat(fakeCameraGraphSession.virtualTimeAtLock3AForCapture)
            .isEqualTo(
                TimeUnit.SECONDS.toMillis(ImageCapture.SCREEN_FLASH_UI_APPLY_TIMEOUT_SECONDS)
            )
    }

    @Test
    fun afNotTriggered_whenScreenFlashPreCaptureCalledWithMinimizeLatency() = runTest {
        capturePipeline.invokeScreenFlashPreCaptureTasks(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)

        assumeTrue(fakeCameraGraphSession.lock3AForCaptureSemaphore.tryAcquire(this))
        assertThat(fakeCameraGraphSession.triggerAfAtLock3AForCapture).isFalse()
    }

    @Test
    fun waitsForAwb_whenScreenFlashPreCaptureCalledWithMinimizeLatency() = runTest {
        capturePipeline.invokeScreenFlashPreCaptureTasks(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)

        assumeTrue(fakeCameraGraphSession.lock3AForCaptureSemaphore.tryAcquire(this))
        assertThat(fakeCameraGraphSession.waitForAwbAtLock3AForCapture).isTrue()
    }

    @Test
    fun afTriggered_whenScreenFlashPreCaptureCalledWithMaximumQuality() = runTest {
        capturePipeline.invokeScreenFlashPreCaptureTasks(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)

        assumeTrue(fakeCameraGraphSession.lock3AForCaptureSemaphore.tryAcquire(this))
        assertThat(fakeCameraGraphSession.triggerAfAtLock3AForCapture).isTrue()
    }

    @Test
    fun screenFlashClearInvokedInMainThread_whenScreenFlashPostCaptureCalled() = runTest {
        capturePipeline.invokeScreenFlashPostCaptureTasks(
            ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
        )

        assertThat(screenFlash.lastClearThreadLooper).isEqualTo(Looper.getMainLooper())
    }

    // TODO(b/326170400): port torch related postcapture tests

    @Test
    fun unlock3aTriggered_whenPostCaptureCalled() = runTest {
        capturePipeline.invokeScreenFlashPostCaptureTasks(
            ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
        )

        assertThat(fakeCameraGraphSession.unlock3APostCaptureSemaphore.tryAcquire(this)).isTrue()
    }

    @Test
    fun doesNotCancelAf_whenPostCaptureCalledWithMinimizeLatency() = runTest {
        capturePipeline.invokeScreenFlashPostCaptureTasks(
            ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
        )

        assumeTrue(fakeCameraGraphSession.unlock3APostCaptureSemaphore.tryAcquire(this))
        assertThat(fakeCameraGraphSession.cancelAfAtUnlock3AForCapture).isFalse()
    }

    @Test
    fun cancelsAf_whenPostCaptureCalledWithMaximumQuality() = runTest {
        capturePipeline.invokeScreenFlashPostCaptureTasks(
            ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY,
        )

        assumeTrue(fakeCameraGraphSession.unlock3APostCaptureSemaphore.tryAcquire(this))
        assertThat(fakeCameraGraphSession.cancelAfAtUnlock3AForCapture).isTrue()
    }

    @Test
    fun screenFlashApplyInvoked_whenStillCaptureSubmittedWithScreenFlash() = runTest {
        capturePipeline
            .submitStillCaptures(
                configs = listOf(singleConfig),
                requestTemplate = RequestTemplate(CameraDevice.TEMPLATE_STILL_CAPTURE),
                sessionConfigOptions = MutableOptionsBundle.create(),
                captureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                flashMode = ImageCapture.FLASH_MODE_SCREEN,
                flashType = ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
            )
            .joinAll()

        assertThat(screenFlash.lastApplyThreadLooper).isNotNull()
    }

    @Test
    fun mainCaptureRequestSubmitted_whenSubmittedWithScreenFlash() = runTest {
        capturePipeline
            .submitStillCaptures(
                configs = listOf(singleConfig),
                requestTemplate = RequestTemplate(CameraDevice.TEMPLATE_STILL_CAPTURE),
                sessionConfigOptions = MutableOptionsBundle.create(),
                captureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                flashMode = ImageCapture.FLASH_MODE_SCREEN,
                flashType = ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
            )
            .joinAll()

        assertThat(fakeCameraGraphSession.submitSemaphore.tryAcquire(this)).isTrue()
    }

    @Test
    fun screenFlashClearInvoked_whenStillCaptureSubmittedWithScreenFlash() = runTest {
        capturePipeline
            .submitStillCaptures(
                configs = listOf(singleConfig),
                requestTemplate = RequestTemplate(CameraDevice.TEMPLATE_STILL_CAPTURE),
                sessionConfigOptions = MutableOptionsBundle.create(),
                captureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                flashMode = ImageCapture.FLASH_MODE_SCREEN,
                flashType = ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
            )
            .joinAll()

        // submitStillCaptures method does not wait for post-capture to be completed, so need to
        // wait a little to ensure it is completed
        delay(1000)

        assertThat(screenFlash.awaitClear(3000)).isTrue()
    }

    // TODO(wenhungteng@): Porting overrideAeModeForStillCapture_quirkAbsent_notOverride,
    //  overrideAeModeForStillCapture_aePrecaptureStarted_override,
    //  overrideAeModeForStillCapture_aePrecaptureFinish_notOverride,
    //  overrideAeModeForStillCapture_noAePrecaptureTriggered_notOverride

    private fun List<Request>.complete() {
        // Callback capture complete.
        forEach { request ->
            request.listeners.forEach {
                it.onTotalCaptureResult(
                    requestMetadata = FakeRequestMetadata(),
                    frameNumber = FrameNumber(100L),
                    totalCaptureResult = FakeFrameInfo(),
                )
            }
        }
    }

    private fun ComboRequestListener.simulateRepeatingResult(
        initialDelay: Long = 100,
        period: Long = 100, // in milliseconds
        requestParameters: Map<CaptureRequest.Key<*>, Any> = mutableMapOf(),
        resultParameters: Map<CaptureResult.Key<*>, Any> = mutableMapOf(),
    ) {
        let { listener ->
            runningRepeatingJob =
                fakeUseCaseThreads.scope.launch {
                    delay(initialDelay)

                    // the counter uses 1000 frames for repeating request instead of infinity so
                    // that
                    // coroutine can complete and lead to an idle state, should be sufficient for
                    // all
                    // our testing purposes here
                    var counter = 1000
                    while (counter-- > 0) {
                        val fakeRequestMetadata =
                            FakeRequestMetadata(requestParameters = requestParameters)
                        val fakeFrameMetadata = FakeFrameMetadata(resultMetadata = resultParameters)
                        val fakeFrameInfo =
                            FakeFrameInfo(
                                metadata = fakeFrameMetadata,
                                requestMetadata = fakeRequestMetadata,
                            )
                        listener.onTotalCaptureResult(
                            requestMetadata = fakeRequestMetadata,
                            frameNumber = FrameNumber(101L),
                            totalCaptureResult = fakeFrameInfo,
                        )
                        delay(period)
                    }
                }
        }
    }

    private suspend fun <T> Collection<Deferred<T>>.awaitAllWithTimeout(
        timeMillis: Long = TimeUnit.SECONDS.toMillis(5)
    ) =
        checkNotNull(withTimeoutOrNull(timeMillis) { awaitAll() }) {
            "Cannot complete the Deferred within $timeMillis"
        }

    /**
     * Advances TestScope coroutine to idle state (i.e. all tasks completed) before trying to
     * acquire semaphore immediately.
     *
     * This saves time by not having to explicitly wait for a semaphore status to be updated.
     */
    private fun Semaphore.tryAcquire(testScope: TestScope): Boolean {
        testScope.advanceUntilIdle()
        return tryAcquire()
    }
}
