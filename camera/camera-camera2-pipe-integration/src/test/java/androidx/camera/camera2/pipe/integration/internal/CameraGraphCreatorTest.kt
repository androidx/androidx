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

package androidx.camera.camera2.pipe.integration.internal

import android.content.Context
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.os.Build
import android.util.Size
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.integration.adapter.RobolectricCameraPipeTestRunner
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowCameraCharacteristics
import org.robolectric.shadows.ShadowCameraManager
import org.robolectric.shadows.StreamConfigurationMapBuilder

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class CameraGraphCreatorTest {

    private val cameraGraphCreator: CameraGraphCreator = CameraGraphCreator()
    private val context = ApplicationProvider.getApplicationContext() as Context
    private val cameraPipe = CameraPipe(CameraPipe.Config(context))

    private val stream1Config = CameraStream.Config.create(
        Size(640, 480), StreamFormat.YUV_420_888)
    private val stream2Config = CameraStream.Config.create(
        Size(1280, 720), StreamFormat.YUV_420_888)
    private val cameraGraph1Config = CameraGraph.Config(CameraId("0"), listOf(stream1Config))
    private val cameraGraph2Config = CameraGraph.Config(CameraId("1"), listOf(stream2Config))

    @Before
    fun setUp() {
        setupCameras()
    }

    @Test
    fun createCameraGraph_singleMode() = runTest {
        cameraGraphCreator.setConcurrentModeOn(false)
        val cameraGraph = cameraGraphCreator.createCameraGraph(cameraPipe, cameraGraph1Config)

        advanceUntilIdle()
        assertThat(cameraGraph).isNotNull()
    }

    @Test
    fun createCameraGraph_concurrentMode() = runTest {
        cameraGraphCreator.setConcurrentModeOn(true)

        val cameraGraph0 = async {
            cameraGraphCreator.createCameraGraph(cameraPipe, cameraGraph1Config)
        }
        advanceUntilIdle()
        assertThat(cameraGraph0.isCompleted).isFalse()

        val cameraGraph1 = async {
            cameraGraphCreator.createCameraGraph(cameraPipe, cameraGraph2Config)
        }
        advanceUntilIdle()

        assertThat(cameraGraph0.isCompleted).isTrue()
        assertThat(cameraGraph1.isCompleted).isTrue()
        assertThat(cameraGraph0.await()).isNotNull()
        assertThat(cameraGraph1.await()).isNotNull()
    }

    private fun setupCameras() {
        val capabilities =
            intArrayOf(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE)

        initCharacteristics("0", CameraCharacteristics.LENS_FACING_BACK, capabilities)
        initCharacteristics("1", CameraCharacteristics.LENS_FACING_FRONT, capabilities)
    }

    private fun initCharacteristics(cameraId: String, lensFacing: Int, capabilities: IntArray?) {
        val sensorWidth = 640
        val sensorHeight = 480

        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        val shadowCharacteristics =
            Shadow.extract<ShadowCameraCharacteristics>(characteristics).apply {

                set(CameraCharacteristics.LENS_FACING, lensFacing)

                set(
                    CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE,
                    Rect(0, 0, sensorWidth, sensorHeight)
                )

                set(
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
                )

                set(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP,
                    StreamConfigurationMapBuilder.newBuilder().build()
                )
            }

        capabilities?.let {
            shadowCharacteristics.set(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES, capabilities
            )
        }

        // Add the camera to the camera service
        (Shadow.extract<Any>(
            ApplicationProvider.getApplicationContext<Context>()
                .getSystemService(Context.CAMERA_SERVICE)
        ) as ShadowCameraManager).addCamera(cameraId, characteristics)
    }
}
