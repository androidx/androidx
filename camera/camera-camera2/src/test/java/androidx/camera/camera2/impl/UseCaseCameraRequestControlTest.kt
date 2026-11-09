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

package androidx.camera.camera2.impl

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.MeteringRectangle
import android.util.Range
import androidx.camera.camera2.adapter.CameraSessionLifecycleAdapter
import androidx.camera.camera2.adapter.CameraStateAdapter
import androidx.camera.camera2.adapter.GraphStateToCameraStateAdapter
import androidx.camera.camera2.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.compat.workaround.NoOpAutoFlashAEModeDisabler
import androidx.camera.camera2.compat.workaround.NoOpTemplateParamsOverride
import androidx.camera.camera2.config.UseCaseCameraContext
import androidx.camera.camera2.interop.setCamera2CaptureRequestConfigurator
import androidx.camera.camera2.pipe.AeMode
import androidx.camera.camera2.pipe.AfMode
import androidx.camera.camera2.pipe.AwbMode
import androidx.camera.camera2.pipe.CameraGraph.Constants3A.METERING_REGIONS_DEFAULT
import androidx.camera.camera2.pipe.ControlMode
import androidx.camera.camera2.pipe.FlashMode
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameMetadata
import androidx.camera.camera2.pipe.Lock3ABehavior
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.testing.FakeFrameInfo
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import androidx.camera.camera2.testing.FakeCameraGraph
import androidx.camera.camera2.testing.FakeCameraGraphSession
import androidx.camera.camera2.testing.FakeCameraProperties
import androidx.camera.camera2.testing.FakeCapturePipeline
import androidx.camera.camera2.testing.FakeSurface
import androidx.camera.camera2.testing.FakeUseCaseSurfaceManager
import androidx.camera.common.CameraFrameNumber
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.impl.CameraCaptureCallback
import androidx.camera.core.impl.CameraCaptureResult
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.TagBundle
import androidx.camera.testing.impl.fakes.FakeUseCase
import com.google.common.truth.Truth.assertThat
import java.util.ArrayDeque
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import kotlin.collections.removeLast as removeLastKt
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class UseCaseCameraRequestControlTest {
    private val surface = FakeSurface()
    private val surfaceToStreamMap: Map<DeferrableSurface, StreamId> = mapOf(surface to StreamId(0))
    private val useCaseThreads by lazy {
        val dispatcher = Dispatchers.Default
        val cameraScope = CoroutineScope(Job() + dispatcher)

        UseCaseThreads(cameraScope, dispatcher.asExecutor(), dispatcher)
    }
    private val fakeCameraGraph = FakeCameraGraph()
    val cameraStateAdapter = CameraStateAdapter(CameraSessionLifecycleAdapter())
    val fakeUseCaseCameraContext =
        UseCaseCameraContext(
            cameraGraphProvider = { fakeCameraGraph },
            cameraStateAdapter = cameraStateAdapter,
            graphStateToCameraStateAdapter = GraphStateToCameraStateAdapter(cameraStateAdapter),
            streamConfigMapProvider = { emptyMap() },
            defaultSurfaceToStreamMap = surfaceToStreamMap,
        )
    private val fakeUseCaseCameraState =
        UseCaseCameraState(
            useCaseCameraContext = fakeUseCaseCameraContext,
            templateParamsOverride = NoOpTemplateParamsOverride,
        )
    private val requestControl =
        UseCaseCameraRequestControlImpl(
            capturePipelineProvider = { FakeCapturePipeline() },
            useCaseCameraStateProvider = { fakeUseCaseCameraState },
            useCaseCameraContext = fakeUseCaseCameraContext,
            useCaseSurfaceManagerProvider = { FakeUseCaseSurfaceManager(threads = useCaseThreads) },
            threads = useCaseThreads,
        )

    @After
    fun tearDown() {
        surface.close()
    }

    @Test
    fun testMergeRequestOptions(): Unit = runBlocking {
        // Arrange
        val sessionConfigBuilder =
            SessionConfig.Builder().also { sessionConfigBuilder ->
                sessionConfigBuilder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
                sessionConfigBuilder.addSurface(surface)
                sessionConfigBuilder.addImplementationOptions(
                    Camera2ImplConfig.Builder()
                        .setCaptureRequestOption<Int>(
                            CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_ON,
                        )
                        .build()
                )
            }
        val camera2CameraControlConfig =
            Camera2ImplConfig.Builder()
                .setCaptureRequestOption(
                    CaptureRequest.FLASH_MODE,
                    CaptureRequest.FLASH_MODE_SINGLE,
                )
                .build()

        // Act
        requestControl.setSessionConfigAsync(sessionConfigBuilder.build()).await()
        requestControl
            .setParametersAsync(
                values = mapOf(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION to 5)
            )
            .await()
        requestControl
            .updateCamera2ConfigAsync(config = camera2CameraControlConfig, tags = emptyMap())
            .await()

        // Assert
        assertThat(fakeCameraGraph.fakeCameraGraphSession.repeatingRequests.size).isEqualTo(3)

        val lastRequest = fakeCameraGraph.fakeCameraGraphSession.repeatingRequests.removeLastKt()
        assertThat(lastRequest.parameters[CaptureRequest.CONTROL_AE_MODE])
            .isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON)
        assertThat(lastRequest.parameters[CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION])
            .isEqualTo(5)
        assertThat(lastRequest.parameters[CaptureRequest.FLASH_MODE])
            .isEqualTo(CaptureRequest.FLASH_MODE_SINGLE)
        assertThat(lastRequest.parameters.size).isEqualTo(3)

        val secondLastRequest =
            fakeCameraGraph.fakeCameraGraphSession.repeatingRequests.removeLastKt()
        assertThat(secondLastRequest.parameters[CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION])
            .isEqualTo(5)
        assertThat(secondLastRequest.parameters[CaptureRequest.CONTROL_AE_MODE])
            .isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON)
        assertThat(secondLastRequest.parameters.size).isEqualTo(2)

        val firstRequest = fakeCameraGraph.fakeCameraGraphSession.repeatingRequests.last()
        assertThat(firstRequest.parameters[CaptureRequest.CONTROL_AE_MODE])
            .isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON)
        assertThat(firstRequest.parameters.size).isEqualTo(1)
    }

    @Test
    fun testMergeConflictRequestOptions(): Unit = runBlocking {
        // Arrange
        val sessionConfigBuilder =
            SessionConfig.Builder().also { sessionConfigBuilder ->
                sessionConfigBuilder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
                sessionConfigBuilder.addSurface(surface)
                sessionConfigBuilder.addImplementationOptions(
                    Camera2ImplConfig.Builder()
                        .setCaptureRequestOption<Int>(
                            CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_ON,
                        )
                        .build()
                )
            }
        val camera2CameraControlConfig =
            Camera2ImplConfig.Builder()
                .setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH,
                )
                .build()

        // Act
        requestControl.updateCamera2ConfigAsync(
            config = camera2CameraControlConfig,
            tags = emptyMap(),
        )
        requestControl.setParametersAsync(
            values = mapOf(CaptureRequest.CONTROL_AE_MODE to CaptureRequest.CONTROL_AE_MODE_OFF)
        )
        requestControl.setSessionConfigAsync(sessionConfigBuilder.build()).await()

        // Assert. The option conflict, the last request should only keep the Camera2CameraControl
        // options.
        val lastRequest = fakeCameraGraph.fakeCameraGraphSession.repeatingRequests.last()
        assertThat(lastRequest.parameters[CaptureRequest.CONTROL_AE_MODE])
            .isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH)
        assertThat(lastRequest.parameters.size).isEqualTo(1)
    }

    @Test
    fun testMergeTag(): Unit = runBlocking {
        // Arrange
        val testSessionTagKey = "testSessionTagKey"
        val testSessionTagValue = "testSessionTagValue"

        val testCamera2InteropTagKey = "testCamera2InteropTagKey"
        val testCamera2InteropTagValue = "testCamera2InteropTagValue"

        val sessionConfigBuilder =
            SessionConfig.Builder().also { sessionConfigBuilder ->
                sessionConfigBuilder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
                sessionConfigBuilder.addSurface(surface)
                sessionConfigBuilder.addTag(testSessionTagKey, testSessionTagValue)
            }

        // Act
        requestControl.updateCamera2ConfigAsync(
            config =
                Camera2ImplConfig.Builder()
                    .setCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH,
                    )
                    .build(),
            tags = mapOf(testCamera2InteropTagKey to testCamera2InteropTagValue),
        )
        requestControl.setSessionConfigAsync(sessionConfigBuilder.build()).await()

        // Assert.
        val lastRequest = fakeCameraGraph.fakeCameraGraphSession.repeatingRequests.last()
        val tagBundle = lastRequest.extras[CAMERAX_TAG_BUNDLE] as TagBundle
        assertThat(tagBundle).isNotNull()
        assertThat(tagBundle.getTag(testSessionTagKey)).isEqualTo(testSessionTagValue)
        assertThat(tagBundle.getTag(testCamera2InteropTagKey)).isEqualTo(testCamera2InteropTagValue)
    }

    @Test
    fun testMergeListener(): Unit = runBlocking {
        // Arrange
        val testCaptureCallback =
            object : CameraCaptureCallback() {
                val latch = CountDownLatch(1)

                override fun onCaptureCompleted(
                    captureConfigId: Int,
                    cameraCaptureResult: CameraCaptureResult,
                ) {
                    latch.countDown()
                }
            }
        val sessionConfigBuilder =
            SessionConfig.Builder().also { sessionConfigBuilder ->
                sessionConfigBuilder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
                sessionConfigBuilder.addSurface(surface)
                sessionConfigBuilder.addCameraCaptureCallback(testCaptureCallback)
            }

        // Act
        requestControl.updateCamera2ConfigAsync(
            config =
                Camera2ImplConfig.Builder()
                    .setCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH,
                    )
                    .build()
        )
        requestControl.setSessionConfigAsync(sessionConfigBuilder.build()).await()

        // Invoke the onComplete on all the listeners.
        fakeCameraGraph.fakeCameraGraphSession.repeatingRequests.last().listeners.forEach {
            it.onComplete(FakeRequestMetadata(), CameraFrameNumber(0), FakeFrameInfo())
        }

        // Assert. All the listeners should receive the onComplete signal.
        assertThat(testCaptureCallback.latch.await(1, TimeUnit.SECONDS)).isTrue()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun updateCamera2ConfigAsync_invokesCaptureCallbacksBeforeRequestListeners(): Unit = runTest {
        // Arrange: Use deterministic queued executors for both CameraX sequentialExecutor and the
        // caller's callback/future listener executor to verify strict happens-before ordering.
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testThreads =
            UseCaseThreads(this, testDispatcher.asExecutor(), testDispatcher).apply {
                sequentialScope = this@runTest
            }
        val localRequestControl =
            UseCaseCameraRequestControlImpl(
                capturePipelineProvider = { FakeCapturePipeline() },
                useCaseCameraStateProvider = { fakeUseCaseCameraState },
                useCaseCameraContext = fakeUseCaseCameraContext,
                useCaseSurfaceManagerProvider = {
                    FakeUseCaseSurfaceManager(threads = testThreads)
                },
                threads = testThreads,
            )

        val userQueue = ArrayDeque<Runnable>()
        val userExecutor = Executor { runnable -> userQueue.addLast(runnable) }
        val executionOrder = mutableListOf<String>()

        val sessionCaptureCallback =
            object : CameraCaptureCallback() {
                override fun onCaptureCompleted(
                    captureConfigId: Int,
                    cameraCaptureResult: CameraCaptureResult,
                ) {
                    executionOrder.add("session_capture_callback")
                }
            }
        val sessionConfig =
            SessionConfig.Builder()
                .apply {
                    setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
                    addSurface(surface)
                    addCameraCaptureCallback(sessionCaptureCallback)
                }
                .build()

        val interopRepeatingCallback =
            object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult,
                ) {
                    userExecutor.execute { executionOrder.add("interop_repeating_callback") }
                }
            }
        val interopFuture = CompletableDeferred<Unit>()
        interopFuture.invokeOnCompletion {
            userExecutor.execute { executionOrder.add("interop_future_listener") }
        }
        val interopRequestListener =
            object : Request.Listener {
                override fun onComplete(
                    requestMetadata: RequestMetadata,
                    frameNumber: CameraFrameNumber,
                    result: FrameInfo,
                ) {
                    executionOrder.add("interop_request_listener")
                    interopFuture.complete(Unit)
                }
            }

        val interopConfig =
            Camera2ImplConfig.Builder()
                .apply {
                    setCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_OFF,
                    )
                    mutableConfig.insertOption(
                        Camera2ImplConfig.SESSION_REPEATING_CAPTURE_CALLBACK_OPTION,
                        interopRepeatingCallback,
                    )
                }
                .build()

        // Act: Apply SessionConfig and Camera2Interop config with completion listener.
        val sessionDeferred = localRequestControl.setSessionConfigAsync(sessionConfig)
        val interopDeferred =
            localRequestControl.updateCamera2ConfigAsync(
                config = interopConfig,
                tags = mapOf("Camera2CameraControl.tag" to 1L),
                listeners = setOf(interopRequestListener),
            )
        advanceUntilIdle()
        sessionDeferred.await()
        interopDeferred.await()

        val fakeCaptureSession = RejectOperationCameraCaptureSession()
        val fakeCaptureRequest = ReflectionHelpers.callConstructor(CaptureRequest::class.java)
        val fakeTotalResult = ReflectionHelpers.callConstructor(TotalCaptureResult::class.java)
        val baseReqMeta = FakeRequestMetadata()
        val requestMetadata =
            object : RequestMetadata by baseReqMeta {
                @Suppress("UNCHECKED_CAST")
                override fun <T : Any> unwrapAs(type: Class<T>): T? =
                    when (type) {
                        CameraCaptureSession::class.java -> fakeCaptureSession as T
                        CaptureRequest::class.java -> fakeCaptureRequest as T
                        else -> baseReqMeta.unwrapAs(type)
                    }
            }
        val baseFrameInfo = FakeFrameInfo(requestMetadata = requestMetadata)
        val frameInfo =
            object : FrameInfo by baseFrameInfo {
                @Suppress("UNCHECKED_CAST")
                override fun <T : Any> unwrapAs(type: Class<T>): T? =
                    if (type == TotalCaptureResult::class.java) fakeTotalResult as T
                    else baseFrameInfo.unwrapAs(type)
            }

        // Simulate CameraPipe firing Request.Listener.onComplete on camera2Handler thread BEFORE
        // sequentialExecutor or userExecutor drains queued tasks.
        val lastRequest = fakeCameraGraph.fakeCameraGraphSession.repeatingRequests.last()
        lastRequest.listeners.forEach { listener ->
            listener.onComplete(requestMetadata, CameraFrameNumber(1L), frameInfo)
        }

        // Drain CameraX sequentialExecutor, then drain caller's userExecutor FIFO queue.
        advanceUntilIdle()
        while (userQueue.isNotEmpty()) {
            userQueue.removeFirst().run()
        }

        // Assert: Both SessionConfig callbacks (on sequentialExecutor) and Camera2Interop
        // repeating callbacks (enqueued on userExecutor synchronously from onComplete) MUST
        // precede interop_request_listener and interop_future_listener!
        assertThat(executionOrder)
            .containsExactly(
                "session_capture_callback",
                "interop_request_listener",
                "interop_repeating_callback",
                "interop_future_listener",
            )
            .inOrder()
    }

    @Test
    fun sessionConfigTemplateShouldSetToRequest(): Unit = runBlocking {
        // Arrange
        val template = CameraDevice.TEMPLATE_RECORD

        val sessionConfigBuilder =
            SessionConfig.Builder().also { sessionConfigBuilder ->
                sessionConfigBuilder.setTemplateType(template)
                sessionConfigBuilder.addSurface(surface)
            }

        // Act
        requestControl.setSessionConfigAsync(sessionConfigBuilder.build()).await()

        // Assert.
        val lastRequest = fakeCameraGraph.fakeCameraGraphSession.repeatingRequests.last()
        assertThat(lastRequest.template!!.value).isEqualTo(template)
    }

    @Test
    fun sessionConfigTemplate_propagatesToState3AControl(): Unit = runBlocking {
        // Arrange
        val state3AControl =
            State3AControl(FakeCameraProperties(), NoOpAutoFlashAEModeDisabler, useCaseThreads)
        val controlWith3A =
            UseCaseCameraRequestControlImpl(
                capturePipelineProvider = { FakeCapturePipeline() },
                state3AControlProvider = { state3AControl },
                useCaseCameraStateProvider = { fakeUseCaseCameraState },
                useCaseCameraContext = fakeUseCaseCameraContext,
                useCaseSurfaceManagerProvider = {
                    FakeUseCaseSurfaceManager(threads = useCaseThreads)
                },
                threads = useCaseThreads,
            )
        state3AControl.requestControl = controlWith3A

        val sessionConfigBuilder =
            SessionConfig.Builder().also {
                it.setTemplateType(CameraDevice.TEMPLATE_RECORD)
                it.addSurface(surface)
            }

        // Act
        controlWith3A.setSessionConfigAsync(sessionConfigBuilder.build()).await()

        // Assert
        assertThat(state3AControl.template).isEqualTo(CameraDevice.TEMPLATE_RECORD)
    }

    @Test
    fun testMergeTemplate(): Unit = runBlocking {
        // Arrange
        val sessionConfigBuilder =
            SessionConfig.Builder().also { sessionConfigBuilder ->
                sessionConfigBuilder.setTemplateType(CameraDevice.TEMPLATE_RECORD)
                sessionConfigBuilder.addSurface(surface)
                sessionConfigBuilder.addImplementationOptions(
                    Camera2ImplConfig.Builder()
                        .setCaptureRequestOption<Int>(
                            CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_ON,
                        )
                        .build()
                )
            }
        val camera2CameraControlConfig =
            Camera2ImplConfig.Builder()
                .setCaptureRequestOption(
                    CaptureRequest.FLASH_MODE,
                    CaptureRequest.FLASH_MODE_SINGLE,
                )
                .build()

        // Act
        requestControl.setSessionConfigAsync(sessionConfigBuilder.build()).await()
        requestControl
            .setParametersAsync(
                values = mapOf(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION to 5)
            )
            .await()
        requestControl
            .updateCamera2ConfigAsync(config = camera2CameraControlConfig, tags = emptyMap())
            .await()

        // Assert
        assertThat(fakeCameraGraph.fakeCameraGraphSession.repeatingRequests.size).isEqualTo(3)
        val lastRequest = fakeCameraGraph.fakeCameraGraphSession.repeatingRequests.removeLastKt()
        assertThat(lastRequest.template!!.value)
            .isEqualTo(RequestTemplate(CameraDevice.TEMPLATE_RECORD).value)
    }

    @Test
    fun sessionConfigExpectedFrameRateRangeShouldSetToRequest(): Unit = runBlocking {
        // Arrange
        val expectedFrameRateRange = Range(60, 60)

        val sessionConfigBuilder =
            SessionConfig.Builder().also { sessionConfigBuilder ->
                sessionConfigBuilder.setExpectedFrameRateRange(expectedFrameRateRange)
                sessionConfigBuilder.addSurface(surface)
                sessionConfigBuilder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
            }

        // Act
        requestControl.setSessionConfigAsync(sessionConfigBuilder.build()).await()

        // Assert.
        val lastRequest = fakeCameraGraph.fakeCameraGraphSession.repeatingRequests.last()
        assertThat(lastRequest[CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE])
            .isEqualTo(expectedFrameRateRange)
    }

    @Test
    fun cameraXConfig_setParametersAsync_camera2CaptureRequestConfiguratorCalled() = runBlocking {
        // Arrange.
        val fpsRange = Range(15, 15)
        lateinit var resultFpsRange: Range<Int>
        val cameraXConfig =
            CameraXConfig.Builder()
                .setCamera2CaptureRequestConfigurator { parameters ->
                    parameters.forEach { (key, value) ->
                        if (key == CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE) {
                            @Suppress("UNCHECKED_CAST")
                            resultFpsRange = value as Range<Int>
                        }
                    }
                }
                .build()
        val requestControl =
            UseCaseCameraRequestControlImpl(
                capturePipelineProvider = { FakeCapturePipeline() },
                useCaseCameraStateProvider = { fakeUseCaseCameraState },
                useCaseCameraContext = fakeUseCaseCameraContext,
                useCaseSurfaceManagerProvider = {
                    FakeUseCaseSurfaceManager(threads = useCaseThreads)
                },
                threads = useCaseThreads,
                cameraXConfig = cameraXConfig,
            )

        // Act.
        requestControl
            .setParametersAsync(
                values = mapOf(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE to fpsRange)
            )
            .await()

        // Assert.
        assertThat(resultFpsRange).isEqualTo(fpsRange)
    }

    @Test
    fun cameraXConfig_setConfigAsync_camera2CaptureRequestConfiguratorCalled() = runBlocking {
        // Arrange.
        val fpsRange = Range(15, 15)
        lateinit var resultFpsRange: Range<Int>
        val cameraXConfig =
            CameraXConfig.Builder()
                .setCamera2CaptureRequestConfigurator { parameters ->
                    parameters.forEach { (key, value) ->
                        if (key == CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE) {
                            @Suppress("UNCHECKED_CAST")
                            resultFpsRange = value as Range<Int>
                        }
                    }
                }
                .build()
        val requestControl =
            UseCaseCameraRequestControlImpl(
                capturePipelineProvider = { FakeCapturePipeline() },
                useCaseCameraStateProvider = { fakeUseCaseCameraState },
                useCaseCameraContext = fakeUseCaseCameraContext,
                useCaseSurfaceManagerProvider = {
                    FakeUseCaseSurfaceManager(threads = useCaseThreads)
                },
                threads = useCaseThreads,
                cameraXConfig = cameraXConfig,
            )

        val sessionConfigBuilder =
            SessionConfig.Builder().also { sessionConfigBuilder ->
                sessionConfigBuilder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
                sessionConfigBuilder.addSurface(surface)
                sessionConfigBuilder.setExpectedFrameRateRange(fpsRange)
            }

        // Act.
        requestControl.setSessionConfigAsync(sessionConfigBuilder.build()).await()

        // Assert.
        assertThat(resultFpsRange).isEqualTo(fpsRange)
    }

    @Test
    fun issueSingleCaptureAsync_whenClosed_failsWithCameraClosedError(): Unit = runBlocking {
        requestControl.close()

        val results =
            requestControl.issueSingleCaptureAsync(
                captureSequence = listOf(CaptureConfig.Builder().build()),
                captureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                flashType = ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
                flashMode = ImageCapture.FLASH_MODE_OFF,
            )

        assertThat(results.size).isEqualTo(1)
        val exception = assertFailsWith<ImageCaptureException> { results[0].await() }
        assertThat(exception.imageCaptureError).isEqualTo(ImageCapture.ERROR_CAMERA_CLOSED)
    }

    @Test
    fun issueSingleCaptureAsync_withInvalidSurface_failsWithoutSubmittingToPipeline(): Unit =
        runBlocking {
            val fakeCapturePipeline = FakeCapturePipeline()
            val localRequestControl =
                UseCaseCameraRequestControlImpl(
                    capturePipelineProvider = { fakeCapturePipeline },
                    useCaseCameraStateProvider = { fakeUseCaseCameraState },
                    useCaseCameraContext = fakeUseCaseCameraContext,
                    useCaseSurfaceManagerProvider = {
                        FakeUseCaseSurfaceManager(threads = useCaseThreads)
                    },
                    threads = useCaseThreads,
                )
            val closedSurface = FakeSurface().apply { close() }
            val captureConfig = CaptureConfig.Builder().apply { addSurface(closedSurface) }.build()

            val results =
                localRequestControl.issueSingleCaptureAsync(
                    captureSequence = listOf(captureConfig),
                    captureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                    flashType = ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
                    flashMode = ImageCapture.FLASH_MODE_OFF,
                )

            assertThat(results.size).isEqualTo(1)
            val exception = assertFailsWith<ImageCaptureException> { results[0].await() }
            assertThat(exception.imageCaptureError).isEqualTo(ImageCapture.ERROR_CAPTURE_FAILED)
            assertThat(fakeCapturePipeline.submitStillCapturesCallCount).isEqualTo(0)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun startFocusAndMeteringAsync_whenLock3AStalled_timesOutAndReleasesSession(): Unit = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testThreads =
            UseCaseThreads(this, testDispatcher.asExecutor(), testDispatcher).apply {
                sequentialScope = this@runTest
            }
        val stalledSession =
            object : FakeCameraGraphSession() {
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
                    convergedTimeLimitNs: Long,
                    lockedTimeLimitNs: Long,
                ): Deferred<Result3A> {
                    // Simulate Controller3A.lock3A suspending forever waiting for 3A convergence
                    CompletableDeferred<Result3A>().await()
                    return CompletableDeferred(Result3A(Result3A.Status.OK))
                }
            }
        val stalledGraph = FakeCameraGraph(fakeCameraGraphSession = stalledSession)
        val stalledContext =
            UseCaseCameraContext(
                cameraGraphProvider = { stalledGraph },
                cameraStateAdapter = cameraStateAdapter,
                graphStateToCameraStateAdapter = GraphStateToCameraStateAdapter(cameraStateAdapter),
                streamConfigMapProvider = { emptyMap() },
                defaultSurfaceToStreamMap = surfaceToStreamMap,
            )
        val localRequestControl =
            UseCaseCameraRequestControlImpl(
                capturePipelineProvider = { FakeCapturePipeline() },
                useCaseCameraStateProvider = {
                    UseCaseCameraState(
                        useCaseCameraContext = stalledContext,
                        templateParamsOverride = NoOpTemplateParamsOverride,
                    )
                },
                useCaseCameraContext = stalledContext,
                useCaseSurfaceManagerProvider = {
                    FakeUseCaseSurfaceManager(threads = testThreads)
                },
                threads = testThreads,
            )

        val resultDeferred =
            localRequestControl.startFocusAndMeteringAsync(
                aeRegions = null,
                afRegions = null,
                awbRegions = null,
                aeLockBehavior = Lock3ABehavior.AFTER_NEW_SCAN,
                afLockBehavior = null,
                awbLockBehavior = null,
                afTriggerStartAeMode = null,
                timeLimitNs = TimeUnit.MILLISECONDS.toNanos(1000L),
            )

        advanceUntilIdle()
        assertThat(resultDeferred.await().status).isEqualTo(Result3A.Status.TIME_LIMIT_REACHED)
        assertThat(stalledGraph.activeSessionCount).isEqualTo(0)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun startFocusAndMeteringAsync_whenLock3ADeferredStalled_releasesSessionImmediatelyAndTimesOut():
        Unit = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testThreads =
            UseCaseThreads(this, testDispatcher.asExecutor(), testDispatcher).apply {
                sequentialScope = this@runTest
            }
        val stalledSession =
            object : FakeCameraGraphSession() {
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
                    convergedTimeLimitNs: Long,
                    lockedTimeLimitNs: Long,
                ): Deferred<Result3A> {
                    // Simulate Controller3A.lock3ANow returning an uncompleted Deferred
                    return CompletableDeferred()
                }
            }
        val stalledGraph = FakeCameraGraph(fakeCameraGraphSession = stalledSession)
        val stalledContext =
            UseCaseCameraContext(
                cameraGraphProvider = { stalledGraph },
                cameraStateAdapter = cameraStateAdapter,
                graphStateToCameraStateAdapter = GraphStateToCameraStateAdapter(cameraStateAdapter),
                streamConfigMapProvider = { emptyMap() },
                defaultSurfaceToStreamMap = surfaceToStreamMap,
            )
        val localRequestControl =
            UseCaseCameraRequestControlImpl(
                capturePipelineProvider = { FakeCapturePipeline() },
                useCaseCameraStateProvider = {
                    UseCaseCameraState(
                        useCaseCameraContext = stalledContext,
                        templateParamsOverride = NoOpTemplateParamsOverride,
                    )
                },
                useCaseCameraContext = stalledContext,
                useCaseSurfaceManagerProvider = {
                    FakeUseCaseSurfaceManager(threads = testThreads)
                },
                threads = testThreads,
            )

        val resultDeferred =
            localRequestControl.startFocusAndMeteringAsync(
                aeRegions = null,
                afRegions = null,
                awbRegions = null,
                aeLockBehavior = null,
                afLockBehavior = Lock3ABehavior.IMMEDIATE,
                awbLockBehavior = null,
                afTriggerStartAeMode = null,
                timeLimitNs = TimeUnit.MILLISECONDS.toNanos(1000L),
            )

        advanceUntilIdle()
        assertThat(resultDeferred.await().status).isEqualTo(Result3A.Status.TIME_LIMIT_REACHED)
        assertThat(stalledGraph.activeSessionCount).isEqualTo(0)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun cancelFocusAndMeteringAsync_whenUnlock3AStalled_timesOutAndUpdates3A(): Unit = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testThreads =
            UseCaseThreads(this, testDispatcher.asExecutor(), testDispatcher).apply {
                sequentialScope = this@runTest
            }
        var update3ACalled = false
        val stalledSession =
            object : FakeCameraGraphSession() {
                override suspend fun unlock3A(
                    ae: Boolean?,
                    af: Boolean?,
                    awb: Boolean?,
                    unlockedCondition: ((FrameMetadata) -> Boolean)?,
                    frameLimit: Int,
                    timeLimitNs: Long,
                ): Deferred<Result3A> {
                    // Simulate Result3AStateListenerImpl never receiving a frame
                    return CompletableDeferred()
                }

                override fun update3A(
                    aeMode: AeMode?,
                    afMode: AfMode?,
                    awbMode: AwbMode?,
                    controlMode: ControlMode?,
                    flashMode: FlashMode?,
                    aeRegions: List<MeteringRectangle>?,
                    afRegions: List<MeteringRectangle>?,
                    awbRegions: List<MeteringRectangle>?,
                    retainLocks: Boolean,
                ): Deferred<Result3A> {
                    update3ACalled = true
                    return CompletableDeferred(Result3A(Result3A.Status.OK))
                }
            }
        val stalledGraph = FakeCameraGraph(fakeCameraGraphSession = stalledSession)
        val stalledContext =
            UseCaseCameraContext(
                cameraGraphProvider = { stalledGraph },
                cameraStateAdapter = cameraStateAdapter,
                graphStateToCameraStateAdapter = GraphStateToCameraStateAdapter(cameraStateAdapter),
                streamConfigMapProvider = { emptyMap() },
                defaultSurfaceToStreamMap = surfaceToStreamMap,
            )
        val localRequestControl =
            UseCaseCameraRequestControlImpl(
                capturePipelineProvider = { FakeCapturePipeline() },
                useCaseCameraStateProvider = {
                    UseCaseCameraState(
                        useCaseCameraContext = stalledContext,
                        templateParamsOverride = NoOpTemplateParamsOverride,
                    )
                },
                useCaseCameraContext = stalledContext,
                useCaseSurfaceManagerProvider = {
                    FakeUseCaseSurfaceManager(threads = testThreads)
                },
                threads = testThreads,
            )

        val resultDeferred =
            localRequestControl.cancelFocusAndMeteringAsync(
                aeRegions = METERING_REGIONS_DEFAULT.asList(),
                afRegions = METERING_REGIONS_DEFAULT.asList(),
                awbRegions = METERING_REGIONS_DEFAULT.asList(),
            )

        advanceUntilIdle()
        assertThat(resultDeferred.await().status).isEqualTo(Result3A.Status.OK)
        assertThat(update3ACalled).isTrue()
        assertThat(stalledGraph.activeSessionCount).isEqualTo(0)
    }

    @Test
    fun update3aRegions_nullUnsupportedRegions_doesNotFallbackToDefault(): Unit = runBlocking {
        requestControl
            .update3aRegions(
                aeRegions = METERING_REGIONS_DEFAULT.asList(),
                afRegions = null,
                awbRegions = null,
            )
            .await()

        assertThat(fakeCameraGraph.fakeCameraGraphSession.update3ACalls).hasSize(1)
        val updateCall = fakeCameraGraph.fakeCameraGraphSession.update3ACalls.single()
        assertThat(updateCall.aeRegions).isEqualTo(METERING_REGIONS_DEFAULT.asList())
        assertThat(updateCall.afRegions).isNull()
        assertThat(updateCall.awbRegions).isNull()
    }

    @Test
    fun cancelFocusAndMeteringAsync_allRegionsNull_skipsUpdate3A(): Unit = runBlocking {
        requestControl
            .cancelFocusAndMeteringAsync(aeRegions = null, afRegions = null, awbRegions = null)
            .await()

        assertThat(fakeCameraGraph.fakeCameraGraphSession.unlock3ACallCount).isEqualTo(1)
        assertThat(fakeCameraGraph.fakeCameraGraphSession.update3ACalls).isEmpty()
    }

    @Test
    fun startFocusAndMeteringAsync_cancelledBeforeExecution_doesNotCallLock3A(): Unit =
        runBlocking {
            val blockGate = CountDownLatch(1)
            val blockedEntered = CountDownLatch(1)
            useCaseThreads.sequentialExecutor.execute {
                blockedEntered.countDown()
                blockGate.await(5, TimeUnit.SECONDS)
            }
            assertThat(blockedEntered.await(5, TimeUnit.SECONDS)).isTrue()

            val focusDeferred =
                requestControl.startFocusAndMeteringAsync(
                    aeRegions = METERING_REGIONS_DEFAULT.asList(),
                    afRegions = METERING_REGIONS_DEFAULT.asList(),
                    awbRegions = null,
                )
            focusDeferred.cancel()
            blockGate.countDown()

            // Flush sequential queue
            requestControl
                .cancelFocusAndMeteringAsync(aeRegions = null, afRegions = null, awbRegions = null)
                .await()

            assertThat(fakeCameraGraph.fakeCameraGraphSession.lock3ACallCount).isEqualTo(0)
        }

    @Test
    fun startFocusAndMeteringAsync_cancelledWhileAwaitingResult_cancelsInnerDeferred(): Unit =
        runBlocking {
            val innerResult = CompletableDeferred<Result3A>()
            fakeCameraGraph.fakeCameraGraphSession.lock3AResult = innerResult

            val focusDeferred =
                requestControl.startFocusAndMeteringAsync(
                    aeRegions = METERING_REGIONS_DEFAULT.asList(),
                    afRegions = METERING_REGIONS_DEFAULT.asList(),
                    awbRegions = null,
                )
            // Ensure startFocusAndMeteringAsync has executed lock3A on sequentialScope
            useCaseThreads.sequentialScope.launch {}.join()
            assertThat(fakeCameraGraph.fakeCameraGraphSession.lock3ACallCount).isEqualTo(1)

            focusDeferred.cancel()

            assertThat(innerResult.isCancelled).isTrue()
        }

    @Test
    fun startFocusAndMeteringAsync_cancelledWhileSuspendedInLock3A_releasesSession(): Unit =
        runBlocking {
            val suspendGate = CompletableDeferred<Unit>()
            fakeCameraGraph.fakeCameraGraphSession.lock3ASuspendGate = suspendGate

            val focusDeferred =
                requestControl.startFocusAndMeteringAsync(
                    aeRegions = METERING_REGIONS_DEFAULT.asList(),
                    afRegions = METERING_REGIONS_DEFAULT.asList(),
                    awbRegions = null,
                )
            // Ensure startFocusAndMeteringAsync has entered lock3A and suspended on suspendGate
            useCaseThreads.sequentialScope.launch {}.join()
            assertThat(fakeCameraGraph.fakeCameraGraphSession.lock3ACallCount).isEqualTo(1)
            assertThat(fakeCameraGraph.fakeCameraGraphSession.activeCount).isEqualTo(1)

            // Cancel while suspended inside useGraphSession { it.lock3A(...) }
            focusDeferred.cancel()
            useCaseThreads.sequentialScope.launch {}.join()

            assertThat(fakeCameraGraph.fakeCameraGraphSession.activeCount).isEqualTo(0)

            // Subsequent cancelFocusAndMeteringAsync can acquire the session and execute unlock3A
            requestControl
                .cancelFocusAndMeteringAsync(aeRegions = null, afRegions = null, awbRegions = null)
                .await()
            assertThat(fakeCameraGraph.fakeCameraGraphSession.unlock3ACallCount).isEqualTo(1)
        }

    @Test
    fun cancelThenStartFocusAndMetering_issuesUnlockAndUpdateBeforeLock3A(): Unit = runBlocking {
        val blockGate = CountDownLatch(1)
        val blockedEntered = CountDownLatch(1)
        useCaseThreads.sequentialExecutor.execute {
            blockedEntered.countDown()
            blockGate.await(5, TimeUnit.SECONDS)
        }
        assertThat(blockedEntered.await(5, TimeUnit.SECONDS)).isTrue()

        val unlockDeferred = CompletableDeferred<Result3A>()
        fakeCameraGraph.fakeCameraGraphSession.unlock3AResult = unlockDeferred

        val cancelDeferred =
            requestControl.cancelFocusAndMeteringAsync(
                aeRegions = METERING_REGIONS_DEFAULT.asList(),
                afRegions = METERING_REGIONS_DEFAULT.asList(),
                awbRegions = null,
            )
        val startDeferred =
            requestControl.startFocusAndMeteringAsync(
                aeRegions = METERING_REGIONS_DEFAULT.asList(),
                afRegions = METERING_REGIONS_DEFAULT.asList(),
                awbRegions = null,
            )

        blockGate.countDown()
        startDeferred.await()

        assertThat(fakeCameraGraph.fakeCameraGraphSession.unlock3ACallCount).isEqualTo(1)
        assertThat(fakeCameraGraph.fakeCameraGraphSession.update3ACalls).hasSize(1)
        assertThat(fakeCameraGraph.fakeCameraGraphSession.lock3ACallCount).isEqualTo(1)
        assertThat(fakeCameraGraph.fakeCameraGraphSession.callOrder)
            .containsExactly("unlock3A", "update3A", "lock3A")
            .inOrder()
        assertThat(cancelDeferred.isActive).isTrue()

        unlockDeferred.complete(Result3A(Result3A.Status.OK))
        cancelDeferred.await()
        assertThat(fakeCameraGraph.fakeCameraGraphSession.update3ACalls).hasSize(1)
    }

    @Test
    fun cancelSequentialScope_cancelsConfinedDeferred(): Unit = runBlocking {
        val suspendGate = CompletableDeferred<Deferred<Unit>>()
        val confinedDeferred =
            useCaseThreads.confineDeferredSuspend(block = { suspendGate.await() })
        useCaseThreads.sequentialScope.launch {}.join()
        assertThat(confinedDeferred.isActive).isTrue()

        useCaseThreads.sequentialScope.cancel()
        val drainLatch = CountDownLatch(1)
        useCaseThreads.sequentialExecutor.execute { drainLatch.countDown() }
        assertThat(drainLatch.await(5, TimeUnit.SECONDS)).isTrue()

        assertThat(confinedDeferred.isCancelled).isTrue()
    }

    private fun UseCaseCameraRequestControl.setSessionConfigAsync(
        sessionConfig: SessionConfig
    ): Deferred<Unit> {
        val fakeUseCase = FakeUseCase()
        fakeUseCase.updateSessionConfigForTesting(sessionConfig)
        return updateRepeatingRequestAsync(isPrimary = true, runningUseCases = listOf(fakeUseCase))
    }
}
