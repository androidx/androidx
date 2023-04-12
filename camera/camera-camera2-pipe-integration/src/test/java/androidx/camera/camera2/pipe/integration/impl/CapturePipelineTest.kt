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

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureRequest.CONTROL_AE_MODE
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.params.MeteringRectangle
import android.os.Build
import androidx.camera.camera2.pipe.AeMode
import androidx.camera.camera2.pipe.AfMode
import androidx.camera.camera2.pipe.AwbMode
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Lock3ABehavior
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.integration.adapter.CameraStateAdapter
import androidx.camera.camera2.pipe.integration.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.integration.adapter.asListenableFuture
import androidx.camera.camera2.pipe.integration.compat.workaround.CapturePipelineTorchCorrection
import androidx.camera.camera2.pipe.integration.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.pipe.integration.compat.quirk.CameraQuirks
import androidx.camera.camera2.pipe.integration.compat.workaround.AeFpsRange
import androidx.camera.camera2.pipe.integration.compat.workaround.NoOpAutoFlashAEModeDisabler
import androidx.camera.camera2.pipe.integration.compat.workaround.NotUseTorchAsFlash
import androidx.camera.camera2.pipe.integration.compat.workaround.UseTorchAsFlashImpl
import androidx.camera.camera2.pipe.integration.compat.workaround.OutputSizesCorrector
import androidx.camera.camera2.pipe.integration.config.UseCaseGraphConfig
import androidx.camera.camera2.pipe.integration.testing.FakeCameraGraph
import androidx.camera.camera2.pipe.integration.testing.FakeCameraGraphSession
import androidx.camera.camera2.pipe.integration.testing.FakeCameraProperties
import androidx.camera.camera2.pipe.integration.testing.FakeUseCaseCamera
import androidx.camera.camera2.pipe.integration.testing.FakeUseCaseCameraRequestControl
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeFrameInfo
import androidx.camera.camera2.pipe.testing.FakeFrameMetadata
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.impl.utils.futures.Futures
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.StreamConfigurationMapBuilder
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class CapturePipelineTest {

    companion object {
        private val executor = Executors.newSingleThreadScheduledExecutor()
        private val fakeUseCaseThreads by lazy {
            val dispatcher = executor.asCoroutineDispatcher()
            val cameraScope = CoroutineScope(Job() + dispatcher)

            UseCaseThreads(
                cameraScope,
                executor,
                dispatcher
            )
        }

        @JvmStatic
        @AfterClass
        fun close() {
            executor.shutdown()
        }
    }

    private val fakeRequestControl = object : FakeUseCaseCameraRequestControl() {
        val torchUpdateEventList = mutableListOf<Boolean>()
        val setTorchSemaphore = Semaphore(0)

        override suspend fun setTorchAsync(enabled: Boolean): Deferred<Result3A> {
            torchUpdateEventList.add(enabled)
            setTorchSemaphore.release()
            return CompletableDeferred(Result3A(Result3A.Status.OK))
        }
    }
    private val comboRequestListener = ComboRequestListener()
    private val fakeCameraGraphSession = object : FakeCameraGraphSession() {
        var requestHandler: (List<Request>) -> Unit = { requests -> requests.complete() }
        val lock3ASemaphore = Semaphore(0)
        val unlock3ASemaphore = Semaphore(0)
        val lock3AForCaptureSemaphore = Semaphore(0)
        val unlock3APostCaptureSemaphore = Semaphore(0)
        val submitSemaphore = Semaphore(0)

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
            frameLimit: Int,
            timeLimitNs: Long
        ): Deferred<Result3A> {
            unlock3ASemaphore.release()
            return CompletableDeferred(Result3A(Result3A.Status.OK))
        }

        override suspend fun lock3AForCapture(
            frameLimit: Int,
            timeLimitNs: Long
        ): Deferred<Result3A> {
            lock3AForCaptureSemaphore.release()
            return CompletableDeferred(Result3A(Result3A.Status.OK))
        }

        override fun submit(requests: List<Request>) {
            requestHandler(requests)
            submitSemaphore.release()
        }

        override suspend fun unlock3APostCapture(): Deferred<Result3A> {
            unlock3APostCaptureSemaphore.release()
            return CompletableDeferred(Result3A(Result3A.Status.OK))
        }
    }
    private val singleRequest = Request(
        streams = emptyList(),
        listeners = emptyList(),
        parameters = emptyMap(),
        extras = emptyMap(),
        template = RequestTemplate(CameraDevice.TEMPLATE_STILL_CAPTURE),
    )
    private val fakeCameraProperties = FakeCameraProperties(
        FakeCameraMetadata(
            mapOf(CameraCharacteristics.FLASH_INFO_AVAILABLE to true),
        )
    )
    private val fakeUseCaseGraphConfig = UseCaseGraphConfig(
        graph = FakeCameraGraph(fakeCameraGraphSession = fakeCameraGraphSession),
        surfaceToStreamMap = emptyMap(),
        cameraStateAdapter = CameraStateAdapter(),
    )
    private var runningRepeatingStream: ScheduledFuture<*>? = null
        set(value) {
            runningRepeatingStream?.cancel(false)
            field = value
        }

    private lateinit var torchControl: TorchControl
    private lateinit var capturePipeline: CapturePipelineImpl

    private lateinit var fakeUseCaseCameraState: UseCaseCameraState

    @Before
    fun setUp() {
        val fakeUseCaseCamera = FakeUseCaseCamera(requestControl = fakeRequestControl)

        torchControl = TorchControl(
            fakeCameraProperties,
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
                )
            ).apply {
                useCaseCamera = fakeUseCaseCamera
            },
            fakeUseCaseThreads,
        ).also {
            it.useCaseCamera = fakeUseCaseCamera

            // Ensure the control is updated after the UseCaseCamera been set.
            assertThat(
                fakeRequestControl.setTorchSemaphore.tryAcquire(5, TimeUnit.SECONDS)
            ).isTrue()
            fakeRequestControl.torchUpdateEventList.clear()
        }

        fakeUseCaseCameraState = UseCaseCameraState(
            fakeUseCaseGraphConfig,
            fakeUseCaseThreads
        )

        capturePipeline = CapturePipelineImpl(
            cameraProperties = fakeCameraProperties,
            requestListener = comboRequestListener,
            threads = fakeUseCaseThreads,
            torchControl = torchControl,
            useCaseGraphConfig = fakeUseCaseGraphConfig,
            useCaseCameraState = fakeUseCaseCameraState,
            useTorchAsFlash = NotUseTorchAsFlash,
        )
    }

    @After
    fun tearDown() {
        runningRepeatingStream = null
    }

    @Test
    fun miniLatency_flashOn_shouldTriggerAePreCapture(): Unit = runBlocking {
        flashOn_shouldTriggerAePreCapture(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
    }

    @Test
    fun maxQuality_flashOn_shouldTriggerAePreCapture(): Unit = runBlocking {
        flashOn_shouldTriggerAePreCapture(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
    }

    private suspend fun flashOn_shouldTriggerAePreCapture(imageCaptureMode: Int) {
        // Arrange.
        val requestList = mutableListOf<Request>()
        fakeCameraGraphSession.requestHandler = { requests ->
            requestList.addAll(requests)
        }

        // Act.
        capturePipeline.submitStillCaptures(
            requests = listOf(singleRequest),
            captureMode = imageCaptureMode,
            flashMode = ImageCapture.FLASH_MODE_ON,
            flashType = ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
        )

        // Assert.
        assertThat(
            fakeCameraGraphSession.lock3AForCaptureSemaphore.tryAcquire(5, TimeUnit.SECONDS)
        ).isTrue()

        // Complete the capture request.
        assertThat(fakeCameraGraphSession.submitSemaphore.tryAcquire(5, TimeUnit.SECONDS)).isTrue()
        requestList.complete()

        // Assert 2, unlock3APostCapture should be called.
        assertThat(
            fakeCameraGraphSession.unlock3APostCaptureSemaphore.tryAcquire(5, TimeUnit.SECONDS)
        ).isTrue()
    }

    @Test
    fun miniLatency_flashAutoFlashRequired_shouldTriggerAePreCapture(): Unit = runBlocking {
        flashAutoFlashRequired_shouldTriggerAePreCapture(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
    }

    @Test
    fun maxQuality_flashAutoFlashRequired_shouldTriggerAePreCapture(): Unit = runBlocking {
        flashAutoFlashRequired_shouldTriggerAePreCapture(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
    }

    private suspend fun flashAutoFlashRequired_shouldTriggerAePreCapture(imageCaptureMode: Int) {
        // Arrange.
        comboRequestListener.simulateRepeatingResult(
            resultParameters = mapOf(
                CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED,
            )
        )
        val requestList = mutableListOf<Request>()
        fakeCameraGraphSession.requestHandler = { requests ->
            requestList.addAll(requests)
        }

        // Act.
        capturePipeline.submitStillCaptures(
            requests = listOf(singleRequest),
            captureMode = imageCaptureMode,
            flashMode = ImageCapture.FLASH_MODE_AUTO,
            flashType = ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
        )

        // Assert 1, lock3AForCapture should be called, but not call unlock3APostCapture
        // (before capturing is finished).
        assertThat(
            fakeCameraGraphSession.lock3AForCaptureSemaphore.tryAcquire(5, TimeUnit.SECONDS)
        ).isTrue()
        assertThat(
            fakeCameraGraphSession.unlock3APostCaptureSemaphore.tryAcquire(2, TimeUnit.SECONDS)
        ).isFalse()

        // Complete the capture request.
        assertThat(fakeCameraGraphSession.submitSemaphore.tryAcquire(5, TimeUnit.SECONDS)).isTrue()
        requestList.complete()

        // Assert 2, unlock3APostCapture should be called.
        assertThat(
            fakeCameraGraphSession.unlock3APostCaptureSemaphore.tryAcquire(5, TimeUnit.SECONDS)
        ).isTrue()
    }

    @Test
    fun miniLatency_withTorchAsFlashQuirk_shouldOpenTorch(): Unit = runBlocking {
        withTorchAsFlashQuirk_shouldOpenTorch(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
    }

    @Test
    fun maxQuality_withTorchAsFlashQuirk_shouldOpenTorch(): Unit = runBlocking {
        withTorchAsFlashQuirk_shouldOpenTorch(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
    }

    private suspend fun withTorchAsFlashQuirk_shouldOpenTorch(imageCaptureMode: Int) {
        // Arrange.
        capturePipeline = CapturePipelineImpl(
            cameraProperties = fakeCameraProperties,
            requestListener = comboRequestListener,
            threads = fakeUseCaseThreads,
            torchControl = torchControl,
            useCaseGraphConfig = fakeUseCaseGraphConfig,
            useCaseCameraState = fakeUseCaseCameraState,
            useTorchAsFlash = UseTorchAsFlashImpl,
        )

        val requestList = mutableListOf<Request>()
        fakeCameraGraphSession.requestHandler = { requests ->
            requestList.addAll(requests)
        }

        // Act.
        capturePipeline.submitStillCaptures(
            requests = listOf(singleRequest),
            captureMode = imageCaptureMode,
            flashMode = ImageCapture.FLASH_MODE_ON,
            flashType = ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
        )

        // Assert 1, torch should be turned on.
        assertThat(
            fakeRequestControl.setTorchSemaphore.tryAcquire(5, TimeUnit.SECONDS)
        ).isTrue()
        assertThat(fakeRequestControl.torchUpdateEventList.size).isEqualTo(1)
        assertThat(fakeRequestControl.torchUpdateEventList.removeFirst()).isTrue()

        // Complete the capture request.
        assertThat(fakeCameraGraphSession.submitSemaphore.tryAcquire(5, TimeUnit.SECONDS)).isTrue()
        requestList.complete()

        // Assert 2, torch should be turned off.
        assertThat(
            fakeRequestControl.setTorchSemaphore.tryAcquire(5, TimeUnit.SECONDS)
        ).isTrue()
        assertThat(fakeRequestControl.torchUpdateEventList.size).isEqualTo(1)
        assertThat(fakeRequestControl.torchUpdateEventList.removeFirst()).isFalse()
    }

    @Test
    fun miniLatency_withTemplateRecord_shouldOpenTorch(): Unit = runBlocking {
        withTemplateRecord_shouldOpenTorch(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
    }

    @Test
    fun maxQuality_withTemplateRecord_shouldOpenTorch(): Unit = runBlocking {
        withTemplateRecord_shouldOpenTorch(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
    }

    private suspend fun withTemplateRecord_shouldOpenTorch(imageCaptureMode: Int) {
        // Arrange.
        capturePipeline.template = CameraDevice.TEMPLATE_RECORD

        val requestList = mutableListOf<Request>()
        fakeCameraGraphSession.requestHandler = { requests ->
            requestList.addAll(requests)
        }

        // Act.
        capturePipeline.submitStillCaptures(
            requests = listOf(singleRequest),
            captureMode = imageCaptureMode,
            flashMode = ImageCapture.FLASH_MODE_ON,
            flashType = ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
        )

        // Assert 1, torch should be turned on.
        assertThat(
            fakeRequestControl.setTorchSemaphore.tryAcquire(5, TimeUnit.SECONDS)
        ).isTrue()
        assertThat(fakeRequestControl.torchUpdateEventList.size).isEqualTo(1)
        assertThat(fakeRequestControl.torchUpdateEventList.removeFirst()).isTrue()

        // Complete the capture request.
        assertThat(fakeCameraGraphSession.submitSemaphore.tryAcquire(5, TimeUnit.SECONDS)).isTrue()
        requestList.complete()

        // Assert 2, torch should be turned off.
        assertThat(
            fakeRequestControl.setTorchSemaphore.tryAcquire(5, TimeUnit.SECONDS)
        ).isTrue()
        assertThat(fakeRequestControl.torchUpdateEventList.size).isEqualTo(1)
        assertThat(fakeRequestControl.torchUpdateEventList.removeFirst()).isFalse()
    }

    @Test
    fun miniLatency_withFlashTypeTorch_shouldOpenTorch(): Unit = runBlocking {
        withFlashTypeTorch_shouldOpenTorch(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
    }

    @Test
    fun maxQuality_withFlashTypeTorch_shouldOpenTorch(): Unit = runBlocking {
        withFlashTypeTorch_shouldOpenTorch(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
    }

    private suspend fun withFlashTypeTorch_shouldOpenTorch(imageCaptureMode: Int) {
        // Arrange.
        val requestList = mutableListOf<Request>()
        fakeCameraGraphSession.requestHandler = { requests ->
            requestList.addAll(requests)
        }

        // Act.
        capturePipeline.submitStillCaptures(
            requests = listOf(singleRequest),
            captureMode = imageCaptureMode,
            flashMode = ImageCapture.FLASH_MODE_ON,
            flashType = ImageCapture.FLASH_TYPE_USE_TORCH_AS_FLASH,
        )

        // Assert 1, torch should be turned on.
        assertThat(
            fakeRequestControl.setTorchSemaphore.tryAcquire(5, TimeUnit.SECONDS)
        ).isTrue()
        assertThat(fakeRequestControl.torchUpdateEventList.size).isEqualTo(1)
        assertThat(fakeRequestControl.torchUpdateEventList.removeFirst()).isTrue()

        // Complete the capture request.
        assertThat(fakeCameraGraphSession.submitSemaphore.tryAcquire(5, TimeUnit.SECONDS)).isTrue()
        requestList.complete()

        // Assert 2, torch should be turned off.
        assertThat(
            fakeRequestControl.setTorchSemaphore.tryAcquire(5, TimeUnit.SECONDS)
        ).isTrue()
        assertThat(fakeRequestControl.torchUpdateEventList.size).isEqualTo(1)
        assertThat(fakeRequestControl.torchUpdateEventList.removeFirst()).isFalse()
    }

    @Test
    fun miniLatency_flashRequired_withFlashTypeTorch_shouldLock3A(): Unit = runBlocking {
        withFlashTypeTorch_shouldLock3A(
            ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
            ImageCapture.FLASH_MODE_ON
        )
    }

    @Test
    fun maxQuality_withFlashTypeTorch_shouldLock3A(): Unit = runBlocking {
        withFlashTypeTorch_shouldLock3A(
            ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY,
            ImageCapture.FLASH_MODE_OFF
        )
    }

    private suspend fun withFlashTypeTorch_shouldLock3A(imageCaptureMode: Int, flashMode: Int) {
        // Arrange.
        val requestList = mutableListOf<Request>()
        fakeCameraGraphSession.requestHandler = { requests ->
            requestList.addAll(requests)
        }

        // Act.
        capturePipeline.submitStillCaptures(
            requests = listOf(singleRequest),
            captureMode = imageCaptureMode,
            flashMode = flashMode,
            flashType = ImageCapture.FLASH_TYPE_USE_TORCH_AS_FLASH,
        )

        // Assert 1, should call lock3A, but not call unlock3A (before capturing is finished).
        assertThat(
            fakeCameraGraphSession.lock3ASemaphore.tryAcquire(5, TimeUnit.SECONDS)
        ).isTrue()
        assertThat(
            fakeCameraGraphSession.unlock3ASemaphore.tryAcquire(1, TimeUnit.SECONDS)
        ).isFalse()

        // Complete the capture request.
        assertThat(fakeCameraGraphSession.submitSemaphore.tryAcquire(5, TimeUnit.SECONDS)).isTrue()
        requestList.complete()

        // Assert 2, should call unlock3A.
        assertThat(
            fakeCameraGraphSession.unlock3ASemaphore.tryAcquire(5, TimeUnit.SECONDS)
        ).isTrue()
    }

    @Test
    fun miniLatency_withFlashTypeTorch_shouldNotLock3A(): Unit = runBlocking {
        // Act.
        capturePipeline.submitStillCaptures(
            requests = listOf(singleRequest),
            captureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
            flashMode = ImageCapture.FLASH_MODE_OFF,
            flashType = ImageCapture.FLASH_TYPE_USE_TORCH_AS_FLASH,
        ).awaitAllWithTimeout()

        // Assert, there is no invocation on lock3A().
        assertThat(
            fakeCameraGraphSession.lock3ASemaphore.tryAcquire(1, TimeUnit.SECONDS)
        ).isFalse()
    }

    @Test
    fun withFlashTypeTorch_torchAlreadyOn_skipTurnOnTorch(): Unit = runBlocking {
        // Arrange.
        // Ensure the torch is already turned on before capturing.
        torchControl.setTorchAsync(true)
        assertThat(
            fakeRequestControl.setTorchSemaphore.tryAcquire(2, TimeUnit.SECONDS)
        ).isTrue()

        // Act.
        capturePipeline.submitStillCaptures(
            requests = listOf(singleRequest),
            captureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
            flashMode = ImageCapture.FLASH_MODE_ON,
            flashType = ImageCapture.FLASH_TYPE_USE_TORCH_AS_FLASH,
        ).awaitAllWithTimeout()

        // Assert, there is no invocation on setTorch().
        assertThat(
            fakeRequestControl.setTorchSemaphore.tryAcquire(1, TimeUnit.SECONDS)
        ).isFalse()
    }

    @Test
    fun miniLatency_shouldNotAePreCapture(): Unit = runBlocking {
        // Act.
        capturePipeline.submitStillCaptures(
            requests = listOf(singleRequest),
            captureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
            flashMode = ImageCapture.FLASH_MODE_OFF,
            flashType = ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
        ).awaitAllWithTimeout()

        // Assert, there is only 1 single capture request.
        assertThat(
            fakeCameraGraphSession.lock3AForCaptureSemaphore.tryAcquire(1, TimeUnit.SECONDS)
        ).isFalse()
    }

    @Test
    fun captureFailure_taskShouldFailure(): Unit = runBlocking {
        // Arrange.
        fakeCameraGraphSession.requestHandler = { requests ->
            requests.forEach { request ->
                // Callback capture fail immediately.
                request.listeners.forEach {
                    it.onFailed(
                        requestMetadata = FakeRequestMetadata(),
                        frameNumber = FrameNumber(100L),
                        captureFailure = mock(CaptureFailure::class.java),
                    )
                }
            }
        }

        // Act.
        val resultDeferredList = capturePipeline.submitStillCaptures(
            requests = listOf(singleRequest),
            captureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
            flashMode = ImageCapture.FLASH_MODE_OFF,
            flashType = ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
        )

        // Assert.
        val exception = Assert.assertThrows(ImageCaptureException::class.java) {
            runBlocking { resultDeferredList.awaitAllWithTimeout() }
        }
        assertThat(exception.imageCaptureError).isEqualTo(ImageCapture.ERROR_CAPTURE_FAILED)
    }

    @Test
    fun captureCancel_taskShouldFailureWithCAMERA_CLOSED(): Unit = runBlocking {
        // Arrange.
        fakeCameraGraphSession.requestHandler = { requests ->
            requests.forEach { request ->
                // Callback capture abort immediately.
                request.listeners.forEach {
                    it.onAborted(
                        singleRequest
                    )
                }
            }
        }

        // Act.
        val resultDeferredList = capturePipeline.submitStillCaptures(
            requests = listOf(singleRequest),
            captureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
            flashMode = ImageCapture.FLASH_MODE_OFF,
            flashType = ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
        )

        // Assert.
        val exception = Assert.assertThrows(ExecutionException::class.java) {
            Futures.allAsList(resultDeferredList.map {
                it.asListenableFuture()
            }).get(2, TimeUnit.SECONDS)
        }
        Assert.assertTrue(exception.cause is ImageCaptureException)
        assertThat((exception.cause as ImageCaptureException).imageCaptureError).isEqualTo(
            ImageCapture.ERROR_CAMERA_CLOSED
        )
    }

    @Test
    fun stillCaptureWithFlashStopRepeatingQuirk_shouldStopRepeatingTemporarily() = runBlocking {
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
            requests = listOf(Request(
                streams = emptyList(),
                parameters = mapOf(CONTROL_AE_MODE to CONTROL_AE_MODE_ON_ALWAYS_FLASH),
                template = RequestTemplate(CameraDevice.TEMPLATE_STILL_CAPTURE)
            )),
            captureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
            flashMode = ImageCapture.FLASH_MODE_ON,
            flashType = ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
        )

        // Assert, stopRepeating -> submit -> startRepeating flow should be used.
        assertThat(
            fakeCameraGraphSession.stopRepeatingSemaphore.tryAcquire(5, TimeUnit.SECONDS)
        ).isTrue()

        assertThat(
            fakeCameraGraphSession.submitSemaphore.tryAcquire(5, TimeUnit.SECONDS)
        ).isTrue()

        // Completing the submitted capture request.
        submittedRequestList.complete()

        assertThat(
            fakeCameraGraphSession.repeatingRequestSemaphore.tryAcquire(5, TimeUnit.SECONDS)
        ).isTrue()
    }

    @Test
    fun stillCaptureWithFlashStopRepeatingQuirkNotEnabled_shouldNotStopRepeating() = runBlocking {
        // Arrange
        val submittedRequestList = mutableListOf<Request>()
        fakeCameraGraphSession.requestHandler = { requests ->
            submittedRequestList.addAll(requests)
        }
        fakeUseCaseCameraState.update(streams = setOf(StreamId(0)))

        // Act.
        capturePipeline.submitStillCaptures(
            requests = listOf(Request(
                streams = emptyList(),
                parameters = mapOf(CONTROL_AE_MODE to CONTROL_AE_MODE_ON_ALWAYS_FLASH),
                template = RequestTemplate(CameraDevice.TEMPLATE_STILL_CAPTURE)
            )),
            captureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
            flashMode = ImageCapture.FLASH_MODE_ON,
            flashType = ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
        )

        // Assert, repeating should not be stopped when quirk not enabled.
        assertThat(
            fakeCameraGraphSession.stopRepeatingSemaphore.tryAcquire(2, TimeUnit.SECONDS)
        ).isFalse()

        assertThat(
            fakeCameraGraphSession.submitSemaphore.tryAcquire(5, TimeUnit.SECONDS)
        ).isTrue()

        // Resetting repeatingRequestSemaphore because startRepeating can be called before
        fakeCameraGraphSession.repeatingRequestSemaphore = Semaphore(0)

        // Completing the submitted capture request.
        submittedRequestList.complete()

        assertThat(
            fakeCameraGraphSession.repeatingRequestSemaphore.tryAcquire(2, TimeUnit.SECONDS)
        ).isFalse()
    }

    @Test
    fun torchAsFlash_torchCorrection_shouldTurnsTorchOffOn(): Unit = runBlocking {
        torchStateCorrectionTest(ImageCapture.FLASH_TYPE_USE_TORCH_AS_FLASH)
    }

    @Test
    fun defaultCapture_torchCorrection_shouldTurnsTorchOffOn(): Unit = runBlocking {
        torchStateCorrectionTest(ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH)
    }

    private suspend fun torchStateCorrectionTest(flashType: Int) {
        // Arrange.
        torchControl.setTorchAsync(torch = true).join()
        verifyTorchState(true)

        val requestList = mutableListOf<Request>()
        fakeCameraGraphSession.requestHandler = { requests ->
            requestList.addAll(requests)
        }
        val capturePipelineTorchCorrection = CapturePipelineTorchCorrection(
            capturePipelineImpl = capturePipeline,
            threads = fakeUseCaseThreads,
            torchControl = torchControl,
        )

        // Act.
        capturePipelineTorchCorrection.submitStillCaptures(
            requests = listOf(singleRequest),
            captureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
            flashMode = ImageCapture.FLASH_MODE_ON,
            flashType = flashType,
        )

        assertThat(fakeCameraGraphSession.submitSemaphore.tryAcquire(5, TimeUnit.SECONDS)).isTrue()
        assertThat(fakeRequestControl.setTorchSemaphore.tryAcquire(1, TimeUnit.SECONDS)).isFalse()
        // Complete the capture request.
        requestList.complete()

        // Assert, the Torch should be turned off, and then turned on.
        verifyTorchState(false)
        verifyTorchState(true)
        // No more invocation to set Torch mode.
        assertThat(fakeRequestControl.torchUpdateEventList.size).isEqualTo(0)
    }

    private fun verifyTorchState(state: Boolean) {
        assertThat(
            fakeRequestControl.setTorchSemaphore.tryAcquire(5, TimeUnit.SECONDS)
        ).isTrue()
        assertThat(fakeRequestControl.torchUpdateEventList.removeFirst() == state).isTrue()
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
        executor.schedule({
            runningRepeatingStream = executor.scheduleAtFixedRate({
                val fakeRequestMetadata = FakeRequestMetadata(requestParameters = requestParameters)
                val fakeFrameMetadata = FakeFrameMetadata(resultMetadata = resultParameters)
                val fakeFrameInfo = FakeFrameInfo(
                    metadata = fakeFrameMetadata, requestMetadata = fakeRequestMetadata,
                )
                this.onTotalCaptureResult(
                    requestMetadata = fakeRequestMetadata,
                    frameNumber = FrameNumber(101L),
                    totalCaptureResult = fakeFrameInfo,
                )
            }, 0, period, TimeUnit.MILLISECONDS)
        }, initialDelay, TimeUnit.MILLISECONDS)
    }

    private suspend fun <T> Collection<Deferred<T>>.awaitAllWithTimeout(
        timeMillis: Long = TimeUnit.SECONDS.toMillis(5)
    ) = checkNotNull(withTimeoutOrNull(timeMillis) {
        awaitAll()
    }) { "Cannot complete the Deferred within $timeMillis" }
}
