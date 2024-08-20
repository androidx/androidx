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

package androidx.camera.camera2.pipe.integration.impl

import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.os.Build
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.integration.adapter.CameraStateAdapter
import androidx.camera.camera2.pipe.integration.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.integration.compat.workaround.NoOpTemplateParamsOverride
import androidx.camera.camera2.pipe.integration.config.UseCaseGraphConfig
import androidx.camera.camera2.pipe.integration.testing.FakeCameraGraph
import androidx.camera.camera2.pipe.integration.testing.FakeCameraProperties
import androidx.camera.camera2.pipe.integration.testing.FakeCapturePipeline
import androidx.camera.camera2.pipe.integration.testing.FakeSurface
import androidx.camera.camera2.pipe.testing.FakeFrameInfo
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import androidx.camera.core.impl.CameraCaptureCallback
import androidx.camera.core.impl.CameraCaptureResult
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.TagBundle
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.collections.removeLast as removeLastKt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
@DoNotInstrument
class UseCaseCameraRequestControlTest {
    private val surface = FakeSurface()
    private val surfaceToStreamMap: Map<DeferrableSurface, StreamId> = mapOf(surface to StreamId(0))
    private val useCaseThreads by lazy {
        val dispatcher = Dispatchers.Default
        val cameraScope = CoroutineScope(Job() + dispatcher)

        UseCaseThreads(cameraScope, dispatcher.asExecutor(), dispatcher)
    }
    private val fakeCameraProperties = FakeCameraProperties()
    private val fakeCameraGraph = FakeCameraGraph()
    private val fakeUseCaseGraphConfig =
        UseCaseGraphConfig(
            graph = fakeCameraGraph,
            surfaceToStreamMap = surfaceToStreamMap,
            cameraStateAdapter = CameraStateAdapter(),
        )
    private val fakeUseCaseCameraState =
        UseCaseCameraState(
            useCaseGraphConfig = fakeUseCaseGraphConfig,
            threads = useCaseThreads,
            sessionProcessorManager = null,
            templateParamsOverride = NoOpTemplateParamsOverride,
        )
    private val requestControl =
        UseCaseCameraRequestControlImpl(
            capturePipeline = FakeCapturePipeline(),
            state = fakeUseCaseCameraState,
            useCaseGraphConfig = fakeUseCaseGraphConfig,
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
                            CaptureRequest.CONTROL_AE_MODE_ON
                        )
                        .build()
                )
            }
        val camera2CameraControlConfig =
            Camera2ImplConfig.Builder()
                .setCaptureRequestOption(
                    CaptureRequest.FLASH_MODE,
                    CaptureRequest.FLASH_MODE_SINGLE
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
            .setConfigAsync(
                type = UseCaseCameraRequestControl.Type.CAMERA2_CAMERA_CONTROL,
                config = camera2CameraControlConfig
            )
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
                            CaptureRequest.CONTROL_AE_MODE_ON
                        )
                        .build()
                )
            }
        val camera2CameraControlConfig =
            Camera2ImplConfig.Builder()
                .setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH
                )
                .build()

        // Act
        requestControl.setConfigAsync(
            type = UseCaseCameraRequestControl.Type.CAMERA2_CAMERA_CONTROL,
            config = camera2CameraControlConfig
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
        requestControl.setConfigAsync(
            type = UseCaseCameraRequestControl.Type.CAMERA2_CAMERA_CONTROL,
            config =
                Camera2ImplConfig.Builder()
                    .setCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH
                    )
                    .build(),
            tags = mapOf(testCamera2InteropTagKey to testCamera2InteropTagValue)
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
        val testRequestListener = TestRequestListener()
        val testCaptureCallback =
            object : CameraCaptureCallback() {
                val latch = CountDownLatch(1)

                override fun onCaptureCompleted(
                    captureConfigId: Int,
                    cameraCaptureResult: CameraCaptureResult
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
        requestControl.setConfigAsync(
            type = UseCaseCameraRequestControl.Type.CAMERA2_CAMERA_CONTROL,
            config =
                Camera2ImplConfig.Builder()
                    .setCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH
                    )
                    .build(),
            listeners = setOf(testRequestListener)
        )
        requestControl.setSessionConfigAsync(sessionConfigBuilder.build()).await()

        // Invoke the onComplete on all the listeners.
        fakeCameraGraph.fakeCameraGraphSession.repeatingRequests.last().listeners.forEach {
            it.onComplete(FakeRequestMetadata(), FrameNumber(0), FakeFrameInfo())
        }

        // Assert. All the listeners should receive the onComplete signal.
        assertThat(testRequestListener.latch.await(1, TimeUnit.SECONDS)).isTrue()
        assertThat(testCaptureCallback.latch.await(1, TimeUnit.SECONDS)).isTrue()
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
                            CaptureRequest.CONTROL_AE_MODE_ON
                        )
                        .build()
                )
            }
        val camera2CameraControlConfig =
            Camera2ImplConfig.Builder()
                .setCaptureRequestOption(
                    CaptureRequest.FLASH_MODE,
                    CaptureRequest.FLASH_MODE_SINGLE
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
            .setConfigAsync(
                type = UseCaseCameraRequestControl.Type.CAMERA2_CAMERA_CONTROL,
                config = camera2CameraControlConfig
            )
            .await()

        // Assert
        assertThat(fakeCameraGraph.fakeCameraGraphSession.repeatingRequests.size).isEqualTo(3)
        val lastRequest = fakeCameraGraph.fakeCameraGraphSession.repeatingRequests.removeLastKt()
        assertThat(lastRequest.template!!.value)
            .isEqualTo(RequestTemplate(CameraDevice.TEMPLATE_RECORD).value)
    }

    private fun UseCaseCameraRequestControl.setSessionConfigAsync(
        sessionConfig: SessionConfig
    ): Deferred<Unit> =
        setConfigAsync(
            type = UseCaseCameraRequestControl.Type.SESSION_CONFIG,
            config = sessionConfig.implementationOptions,
            tags = sessionConfig.repeatingCaptureConfig.tagBundle.toMap(),
            listeners =
                setOf(
                    CameraCallbackMap.createFor(
                        sessionConfig.repeatingCameraCaptureCallbacks,
                        useCaseThreads.backgroundExecutor
                    )
                ),
            template = RequestTemplate(sessionConfig.repeatingCaptureConfig.templateType),
            streams =
                fakeUseCaseGraphConfig.getStreamIdsFromSurfaces(
                    sessionConfig.repeatingCaptureConfig.surfaces
                )
        )
}

private class TestRequestListener : Request.Listener {
    val latch = CountDownLatch(1)

    override fun onComplete(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        result: FrameInfo
    ) {
        latch.countDown()
    }
}
