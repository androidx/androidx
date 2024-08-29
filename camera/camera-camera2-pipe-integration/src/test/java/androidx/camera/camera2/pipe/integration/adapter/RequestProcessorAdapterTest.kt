/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.adapter

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_OFF
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.compat.CameraPipeKeys
import androidx.camera.camera2.pipe.integration.config.UseCaseGraphConfig
import androidx.camera.camera2.pipe.integration.impl.Camera2ImplConfig
import androidx.camera.camera2.pipe.testing.CameraGraphSimulator
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.RequestProcessor
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.SessionProcessorSurface
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
@DoNotInstrument
class RequestProcessorAdapterTest {
    private val context = ApplicationProvider.getApplicationContext() as Context

    private val cameraId = CameraId.fromCamera2Id("0")
    private val size = Size(1280, 720)
    private val previewStreamConfig = CameraStream.Config.create(size, StreamFormat.UNKNOWN)
    private val imageCaptureStreamConfig = CameraStream.Config.create(size, StreamFormat.JPEG)
    private val graphConfig =
        CameraGraph.Config(
            camera = cameraId,
            streams = listOf(previewStreamConfig, imageCaptureStreamConfig),
            sessionParameters = mapOf(CameraPipeKeys.ignore3ARequiredParameters to true),
        )

    private val previewSurfaceTexture =
        SurfaceTexture(0).apply { setDefaultBufferSize(size.width, size.height) }
    private val previewSurface = Surface(previewSurfaceTexture)
    private val imageCaptureSurfaceTexture =
        SurfaceTexture(0).apply { setDefaultBufferSize(size.width, size.height) }
    private val imageCaptureSurface = Surface(imageCaptureSurfaceTexture)

    private val previewProcessorSurface =
        SessionProcessorSurface(previewSurface, previewOutputConfigId)
    private val imageCaptureProcessorSurface =
        SessionProcessorSurface(imageCaptureSurface, imageCaptureOutputConfigId)

    private val fakeSessionConfig =
        SessionConfig.Builder()
            .apply {
                addSurface(previewProcessorSurface)
                addSurface(imageCaptureProcessorSurface)
            }
            .build()

    private val sessionProcessorSurfaces =
        listOf(
            previewProcessorSurface,
            imageCaptureProcessorSurface,
        )

    private var cameraGraphSimulator: CameraGraphSimulator? = null
    private var requestProcessorAdapter: RequestProcessorAdapter? = null

    @After
    fun tearDown() {
        previewSurface.release()
        previewSurfaceTexture.release()
        imageCaptureSurface.release()
        imageCaptureSurfaceTexture.release()
    }

    private fun initialize(scope: TestScope) {
        val simulator =
            CameraGraphSimulator.create(
                    scope,
                    context,
                    FakeCameraMetadata(cameraId = cameraId),
                    graphConfig,
                )
                .also {
                    it.start()
                    it.simulateCameraStarted()
                    it.initializeSurfaces()
                }
        cameraGraphSimulator = simulator
        val surfaceToStreamMap =
            buildMap<DeferrableSurface, StreamId> {
                put(
                    previewProcessorSurface,
                    checkNotNull(simulator.streams[previewStreamConfig]).id
                )
                put(
                    imageCaptureProcessorSurface,
                    checkNotNull(simulator.streams[imageCaptureStreamConfig]).id
                )
            }
        val useCaseGraphConfig =
            UseCaseGraphConfig(simulator, surfaceToStreamMap, CameraStateAdapter())

        requestProcessorAdapter =
            RequestProcessorAdapter(
                    useCaseGraphConfig,
                    sessionProcessorSurfaces,
                    scope,
                )
                .apply { sessionConfig = fakeSessionConfig }
        scope.advanceUntilIdle()
    }

    @Test
    fun canSetRepeating() = runTest {
        val requestToSet =
            object : RequestProcessor.Request {
                override fun getTargetOutputConfigIds(): MutableList<Int> {
                    return mutableListOf(previewOutputConfigId)
                }

                override fun getParameters(): androidx.camera.core.impl.Config {
                    return Camera2ImplConfig.Builder().build()
                }

                override fun getTemplateId(): Int {
                    return CameraDevice.TEMPLATE_PREVIEW
                }
            }
        initialize(this)
        val callback: RequestProcessor.Callback = mock()

        requestProcessorAdapter!!.setRepeating(requestToSet, callback)
        advanceUntilIdle()

        val frame = cameraGraphSimulator!!.simulateNextFrame()
        val request = frame.request
        assertThat(request.streams.size).isEqualTo(1)
        assertThat(request.streams.first())
            .isEqualTo(checkNotNull(cameraGraphSimulator!!.streams[previewStreamConfig]).id)

        verify(callback, times(1)).onCaptureStarted(eq(requestToSet), any(), any())

        frame.simulateComplete(emptyMap())
        verify(callback, times(1)).onCaptureCompleted(eq(requestToSet), any())
        advanceUntilIdle()
    }

    @Test
    fun canSubmitRequests() = runTest {
        val requestToSubmit =
            object : RequestProcessor.Request {
                override fun getTargetOutputConfigIds(): MutableList<Int> {
                    return mutableListOf(imageCaptureOutputConfigId)
                }

                override fun getParameters(): androidx.camera.core.impl.Config {
                    return Camera2ImplConfig.Builder()
                        .apply {
                            setCaptureRequestOption(
                                CaptureRequest.CONTROL_AE_MODE,
                                CONTROL_AE_MODE_OFF
                            )
                        }
                        .build()
                }

                override fun getTemplateId(): Int {
                    return CameraDevice.TEMPLATE_STILL_CAPTURE
                }
            }

        initialize(this)
        val callback: RequestProcessor.Callback = mock()

        requestProcessorAdapter!!.submit(mutableListOf(requestToSubmit), callback)
        advanceUntilIdle()

        val frame = cameraGraphSimulator!!.simulateNextFrame()
        val request = frame.request
        assertThat(request.streams.size).isEqualTo(1)
        assertThat(request.streams.first())
            .isEqualTo(checkNotNull(cameraGraphSimulator!!.streams[imageCaptureStreamConfig]).id)
        assertThat(request.parameters[CaptureRequest.CONTROL_AE_MODE])
            .isEqualTo(CONTROL_AE_MODE_OFF)

        verify(callback, times(1)).onCaptureStarted(eq(requestToSubmit), any(), any())

        frame.simulateComplete(emptyMap())
        verify(callback, times(1)).onCaptureCompleted(eq(requestToSubmit), any())
        advanceUntilIdle()
    }

    companion object {
        private val previewOutputConfigId = 0
        private val imageCaptureOutputConfigId = 1
    }
}
