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

package androidx.camera.camera2.pipe.integration.adapter

import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import android.util.Size
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.integration.config.CameraConfig
import androidx.camera.camera2.pipe.integration.impl.CameraCallbackMap
import androidx.camera.camera2.pipe.integration.impl.CameraProperties
import androidx.camera.camera2.pipe.integration.impl.EvCompControl
import androidx.camera.camera2.pipe.integration.impl.TorchControl
import androidx.camera.camera2.pipe.integration.impl.UseCaseThreads
import androidx.camera.camera2.pipe.integration.impl.ZoomControl
import androidx.camera.camera2.pipe.integration.testing.FakeCameraProperties
import androidx.camera.camera2.pipe.integration.testing.FakeEvCompCompat
import androidx.camera.camera2.pipe.integration.testing.FakeZoomCompat
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.core.impl.ImageFormatConstants
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.StreamConfigurationMapBuilder

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class CameraInfoAdapterTest {

    private val useCaseThreads by lazy {
        val executor = MoreExecutors.directExecutor()
        val dispatcher = executor.asCoroutineDispatcher()
        val cameraScope = CoroutineScope(
            SupervisorJob() + dispatcher + CoroutineName("CameraInfoAdapterTest")
        )
        UseCaseThreads(cameraScope, executor, dispatcher)
    }
    private lateinit var cameraProperties0: CameraProperties

    @Before
    fun setUp() {
        initCamera()
    }

    @Test
    fun getSupportedResolutions() {
        // Arrange.
        val cameraInfoAdapter = createCameraInfoAdapter()

        // Act.
        val resolutions: List<Size> = cameraInfoAdapter.getSupportedResolutions(
            ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
        )

        // Assert.
        assertThat(resolutions).containsExactly(
            Size(1920, 1080),
            Size(1280, 720),
            Size(640, 480)
        )
    }

    private fun initCamera() {
        val formatPrivate = ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
        val streamConfigurationMap = StreamConfigurationMapBuilder.newBuilder()
            .addOutputSize(formatPrivate, Size(1920, 1080))
            .addOutputSize(formatPrivate, Size(1280, 720))
            .addOutputSize(formatPrivate, Size(640, 480))
            .build()
        val metadata = FakeCameraMetadata(
            cameraId = CAMERA_ID_0,
            characteristics = mapOf(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP to streamConfigurationMap
            )
        )
        cameraProperties0 = FakeCameraProperties(metadata = metadata)
    }

    private fun createCameraInfoAdapter(
        cameraId: CameraId = cameraProperties0.cameraId,
        cameraProperties: CameraProperties = cameraProperties0,
    ): CameraInfoAdapter {
        return CameraInfoAdapter(
            cameraProperties,
            CameraConfig(cameraId),
            CameraStateAdapter(),
            CameraControlStateAdapter(
                ZoomControl(FakeZoomCompat()),
                EvCompControl(FakeEvCompCompat()),
                TorchControl(cameraProperties, useCaseThreads),
            ),
            CameraCallbackMap(),
        )
    }

    companion object {
        private val CAMERA_ID_0 = CameraId("0")
    }
}
