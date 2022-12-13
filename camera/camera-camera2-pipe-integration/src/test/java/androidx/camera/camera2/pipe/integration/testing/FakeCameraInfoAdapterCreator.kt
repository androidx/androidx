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

package androidx.camera.camera2.pipe.integration.testing

import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.params.StreamConfigurationMap
import android.util.Size
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.integration.adapter.CameraControlStateAdapter
import androidx.camera.camera2.pipe.integration.adapter.CameraInfoAdapter
import androidx.camera.camera2.pipe.integration.adapter.CameraStateAdapter
import androidx.camera.camera2.pipe.integration.compat.Camera2CameraControlCompatImpl
import androidx.camera.camera2.pipe.integration.config.CameraConfig
import androidx.camera.camera2.pipe.integration.config.UseCaseCameraComponent
import androidx.camera.camera2.pipe.integration.config.UseCaseCameraConfig
import androidx.camera.camera2.pipe.integration.impl.CameraCallbackMap
import androidx.camera.camera2.pipe.integration.impl.CameraProperties
import androidx.camera.camera2.pipe.integration.impl.ComboRequestListener
import androidx.camera.camera2.pipe.integration.impl.DisplayInfoManager
import androidx.camera.camera2.pipe.integration.impl.EvCompControl
import androidx.camera.camera2.pipe.integration.impl.FocusMeteringControl
import androidx.camera.camera2.pipe.integration.impl.TorchControl
import androidx.camera.camera2.pipe.integration.impl.UseCaseCameraControl
import androidx.camera.camera2.pipe.integration.impl.UseCaseManager
import androidx.camera.camera2.pipe.integration.impl.UseCaseThreads
import androidx.camera.camera2.pipe.integration.impl.ZoomControl
import androidx.camera.camera2.pipe.integration.interop.Camera2CameraControl
import androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.core.impl.ImageFormatConstants
import androidx.test.core.app.ApplicationProvider
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import org.robolectric.shadows.StreamConfigurationMapBuilder

object FakeCameraInfoAdapterCreator {
    private val CAMERA_ID_0 = CameraId("0")

    private val useCaseThreads by lazy {
        val executor = MoreExecutors.directExecutor()
        val dispatcher = executor.asCoroutineDispatcher()
        val cameraScope = CoroutineScope(
            SupervisorJob() + dispatcher + CoroutineName("CameraInfoAdapterUtil")
        )
        UseCaseThreads(cameraScope, executor, dispatcher)
    }

    private const val formatPrivate = ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE

    private val streamConfigurationMap: StreamConfigurationMap =
        StreamConfigurationMapBuilder.newBuilder()
            .addOutputSize(formatPrivate, Size(1920, 1080))
            .addOutputSize(formatPrivate, Size(1280, 720))
            .addOutputSize(formatPrivate, Size(640, 480))
            .build()

    private val cameraCharacteristics = mapOf(
        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP to streamConfigurationMap,
        CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE to Rect(0, 0, 640, 480)
    )

    @OptIn(ExperimentalCamera2Interop::class)
    fun createCameraInfoAdapter(
        cameraId: CameraId = CAMERA_ID_0,
        cameraProperties: CameraProperties = FakeCameraProperties(
            FakeCameraMetadata(
                cameraId = cameraId,
                characteristics = cameraCharacteristics
            ),
            cameraId
        )
    ): CameraInfoAdapter {
        val zoomControl = ZoomControl(FakeZoomCompat())
        val evCompControl = EvCompControl(FakeEvCompCompat())
        val torchControl = TorchControl(cameraProperties, useCaseThreads)

        val cameraControlStateAdapter = CameraControlStateAdapter(
            zoomControl,
            evCompControl,
            torchControl,
        )

        val camera2CameraControl = Camera2CameraControl.create(
            Camera2CameraControlCompatImpl(useCaseThreads),
            useCaseThreads,
            ComboRequestListener()
        )

        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "UNCHECKED_CAST")
        val useCaseManager = UseCaseManager(
            CameraConfig(cameraId),
            object : UseCaseCameraComponent.Builder {
                override fun config(
                    config: UseCaseCameraConfig
                ): UseCaseCameraComponent.Builder {
                    TODO("Not yet implemented")
                }

                override fun build(): UseCaseCameraComponent {
                    TODO("Not yet implemented")
                }
            },
            setOf(evCompControl, torchControl, zoomControl)
                as java.util.Set<UseCaseCameraControl>,
            camera2CameraControl,
            CameraStateAdapter(),
            cameraProperties,
            DisplayInfoManager(ApplicationProvider.getApplicationContext())
        )

        return CameraInfoAdapter(
            cameraProperties,
            CameraConfig(cameraId),
            CameraStateAdapter(),
            cameraControlStateAdapter,
            CameraCallbackMap(),
            FocusMeteringControl(
                cameraProperties,
                useCaseManager,
                useCaseThreads
            )
        )
    }
}
