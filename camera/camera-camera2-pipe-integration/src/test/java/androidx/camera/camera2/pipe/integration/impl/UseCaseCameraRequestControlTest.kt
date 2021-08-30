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
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.integration.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.integration.testing.FakeCameraGraph
import androidx.camera.camera2.pipe.integration.testing.FakeSurface
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.SessionConfig
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
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

        UseCaseThreads(
            cameraScope,
            dispatcher.asExecutor(),
            dispatcher
        )
    }
    private val fakeCameraGraph = FakeCameraGraph()
    private val requestControl = UseCaseCameraRequestControlImpl(
        fakeCameraGraph, surfaceToStreamMap, useCaseThreads
    )

    @Test
    fun testMergeRequestOptions(): Unit = runBlocking {
        // Arrange
        val sessionConfigBuilder = SessionConfig.Builder().also { sessionConfigBuilder ->
            sessionConfigBuilder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
            sessionConfigBuilder.addSurface(surface)
            sessionConfigBuilder.addImplementationOptions(
                Camera2ImplConfig.Builder()
                    .setCaptureRequestOption<Int>(
                        CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON
                    ).build()
            )
        }
        val camera2CameraControlConfig = Camera2ImplConfig.Builder()
            .setCaptureRequestOption(
                CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_SINGLE
            ).build()

        // Act
        requestControl.setSessionConfigAsync(
            sessionConfigBuilder.build()
        ).await()
        requestControl.appendParametersAsync(
            values = mapOf(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION to 5)
        ).await()
        requestControl.setConfigAsync(
            type = UseCaseCameraRequestControl.Type.CAMERA2_CAMERA_CONTROL,
            config = camera2CameraControlConfig
        ).await()

        // Assert
        assertThat(fakeCameraGraph.fakeCameraGraphSession.repeatingRequests.size).isEqualTo(3)

        val lastRequest = fakeCameraGraph.fakeCameraGraphSession.repeatingRequests.removeLast()
        assertThat(
            lastRequest.parameters[CaptureRequest.CONTROL_AE_MODE]
        ).isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON)
        assertThat(
            lastRequest.parameters[CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION]
        ).isEqualTo(5)
        assertThat(
            lastRequest.parameters[CaptureRequest.FLASH_MODE]
        ).isEqualTo(CaptureRequest.FLASH_MODE_SINGLE)
        assertThat(lastRequest.parameters.size).isEqualTo(3)

        val secondLastRequest =
            fakeCameraGraph.fakeCameraGraphSession.repeatingRequests.removeLast()
        assertThat(
            secondLastRequest.parameters[
                CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION
            ]
        ).isEqualTo(5)
        assertThat(
            secondLastRequest.parameters[CaptureRequest.CONTROL_AE_MODE]
        ).isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON)
        assertThat(secondLastRequest.parameters.size).isEqualTo(2)

        val firstRequest = fakeCameraGraph.fakeCameraGraphSession.repeatingRequests.last()
        assertThat(
            firstRequest.parameters[
                CaptureRequest.CONTROL_AE_MODE
            ]
        ).isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON)
        assertThat(firstRequest.parameters.size).isEqualTo(1)
    }

    @Test
    fun testMergeConflictRequestOptions(): Unit = runBlocking {
        // Arrange
        val sessionConfigBuilder = SessionConfig.Builder().also { sessionConfigBuilder ->
            sessionConfigBuilder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
            sessionConfigBuilder.addSurface(surface)
            sessionConfigBuilder.addImplementationOptions(
                Camera2ImplConfig.Builder()
                    .setCaptureRequestOption<Int>(
                        CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON
                    ).build()
            )
        }
        val camera2CameraControlConfig = Camera2ImplConfig.Builder()
            .setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH
            ).build()

        // Act
        requestControl.setConfigAsync(
            type = UseCaseCameraRequestControl.Type.CAMERA2_CAMERA_CONTROL,
            config = camera2CameraControlConfig
        )
        requestControl.appendParametersAsync(
            values = mapOf(CaptureRequest.CONTROL_AE_MODE to CaptureRequest.CONTROL_AE_MODE_OFF)
        )
        requestControl.setSessionConfigAsync(
            sessionConfigBuilder.build()
        ).await()

        // Assert. The option conflict, the last request should only keep the Camera2CameraControl
        // options.
        val lastRequest = fakeCameraGraph.fakeCameraGraphSession.repeatingRequests.last()
        assertThat(
            lastRequest.parameters[CaptureRequest.CONTROL_AE_MODE]
        ).isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH)
        assertThat(lastRequest.parameters.size).isEqualTo(1)
    }
}