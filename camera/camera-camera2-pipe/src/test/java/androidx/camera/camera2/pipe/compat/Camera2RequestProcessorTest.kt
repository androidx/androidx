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

package androidx.camera.camera2.pipe.compat

import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
import android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.os.Looper
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.testing.FakeCameraDeviceWrapper
import androidx.camera.camera2.pipe.testing.FakeThreads
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.testing.RobolectricCameras
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.P, maxSdk = 29)
internal class Camera2RequestProcessorTest {
    // TODO: This fails with "Failed to allocate native CameraMetadata" on robolectric prior
    //  to Android P. Update the test class to include support for older versions when a new
    //  version of robolectric is dropped into androidX.

    private val mainLooper = Shadows.shadowOf(Looper.getMainLooper())
    private val cameraId = RobolectricCameras.create(
        mapOf(INFO_SUPPORTED_HARDWARE_LEVEL to INFO_SUPPORTED_HARDWARE_LEVEL_FULL)
    )
    private val testCamera = RobolectricCameras.open(cameraId)

    private val stream1Config = CameraStream.Config.create(
        Size(640, 480),
        StreamFormat.YUV_420_888
    )

    private val stream2Config = CameraStream.Config.create(
        Size(1024, 768),
        StreamFormat.RAW_PRIVATE
    )

    private val graphConfig = CameraGraph.Config(
        camera = testCamera.cameraId,
        streams = listOf(stream1Config, stream2Config),
    )

    private val streamGraph = Camera2StreamGraph(
        testCamera.metadata,
        graphConfig
    )

    private val surface1 = Surface(
        SurfaceTexture(0).also {
            val output = stream1Config.outputs.first()
            it.setDefaultBufferSize(output.size.width, output.size.height)
        }
    )

    private val surface2 = Surface(
        SurfaceTexture(0).also {
            val output = stream2Config.outputs.first()
            it.setDefaultBufferSize(output.size.width, output.size.height)
        }
    )

    private val stream1 = streamGraph[stream1Config]!!
    private val stream2 = streamGraph[stream2Config]!!

    private val fakeCameraDevice = FakeCameraDeviceWrapper(testCamera)
    private val fakeCaptureSession = fakeCameraDevice.createFakeCaptureSession()

    @After
    fun teardown() {
        mainLooper.idle()
        RobolectricCameras.clear()
    }

    @Test
    fun robolectricCanBuildCaptureRequest() {
        val requestBuilder = fakeCameraDevice.fakeCamera.cameraDevice.createCaptureRequest(1)
        // Parameters
        requestBuilder.set(
            CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH
        )
        requestBuilder.set(
            CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
        )
        requestBuilder.set(
            CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO
        )

        // Surfaces
        requestBuilder.addTarget(surface1)
        requestBuilder.addTarget(surface2)

        val request = requestBuilder.build()
        assertThat(request).isNotNull()

        // TODO: Add support for checking parameters when robolectric supports it.
        // assertThat(request[CaptureRequest.CONTROL_AE_MODE])
        //    .isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH)
        // assertThat(request[CaptureRequest.CONTROL_AF_MODE])
        //    .isEqualTo(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
    }

    @Test
    fun requestIsCreatedAndSubmitted() {
        val requestProcessor = Camera2RequestProcessor(
            fakeCaptureSession,
            FakeThreads.forTests,
            RequestTemplate(1),
            mapOf(
                stream1.id to surface1,
                stream2.id to surface2
            )
        )

        val result = requestProcessor.submit(
            Request(listOf(stream1.id, stream2.id)),
            defaultParameters = mapOf<Any, Any?>(
                CaptureRequest.CONTROL_AE_MODE to CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH,
                CaptureRequest.CONTROL_AF_MODE to CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            ),
            requiredParameters = mapOf<Any, Any?>(
                CaptureRequest.CONTROL_AF_MODE to CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO
            ),
            defaultListeners = listOf()
        )

        assertThat(result).isTrue()
        assertThat(fakeCaptureSession.lastCapture).hasSize(1)
        assertThat(fakeCaptureSession.lastRepeating).isNull()

        // TODO: Add support for checking parameters when robolectric supports it.
    }

    @Test
    fun requestIsSubmittedWithPartialSurfaces() {
        val requestProcessor = Camera2RequestProcessor(
            fakeCaptureSession,
            FakeThreads.forTests,
            RequestTemplate(1),
            mapOf(
                stream1.id to surface1
            )
        )
        val result = requestProcessor.submit(
            Request(listOf(stream1.id, stream2.id)),
            defaultParameters = mapOf<Any, Any?>(),
            requiredParameters = mapOf<Any, Any?>(),
            defaultListeners = listOf()
        )
        assertThat(result).isTrue()
    }

    @Test
    fun requestIsNotSubmittedWithEmptySurfaceList() {
        val requestProcessor = Camera2RequestProcessor(
            fakeCaptureSession,
            FakeThreads.forTests,
            RequestTemplate(1),
            mapOf(
                stream1.id to surface1
            )
        )
        val result = requestProcessor.submit(
            Request(listOf(stream2.id)),
            defaultParameters = mapOf<Any, Any?>(),
            requiredParameters = mapOf<Any, Any?>(),
            defaultListeners = listOf()
        )
        assertThat(result).isFalse()
    }
}