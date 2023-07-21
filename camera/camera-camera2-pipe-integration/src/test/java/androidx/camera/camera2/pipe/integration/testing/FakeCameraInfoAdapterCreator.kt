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
import android.util.Range
import android.util.Size
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.integration.adapter.CameraControlStateAdapter
import androidx.camera.camera2.pipe.integration.adapter.CameraInfoAdapter
import androidx.camera.camera2.pipe.integration.adapter.CameraStateAdapter
import androidx.camera.camera2.pipe.integration.adapter.EncoderProfilesProviderAdapter
import androidx.camera.camera2.pipe.integration.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.pipe.integration.compat.quirk.CameraQuirks
import androidx.camera.camera2.pipe.integration.compat.workaround.AeFpsRange
import androidx.camera.camera2.pipe.integration.compat.workaround.MeteringRegionCorrection
import androidx.camera.camera2.pipe.integration.compat.workaround.NoOpAutoFlashAEModeDisabler
import androidx.camera.camera2.pipe.integration.compat.workaround.OutputSizesCorrector
import androidx.camera.camera2.pipe.integration.config.CameraConfig
import androidx.camera.camera2.pipe.integration.impl.CameraCallbackMap
import androidx.camera.camera2.pipe.integration.impl.CameraProperties
import androidx.camera.camera2.pipe.integration.impl.EvCompControl
import androidx.camera.camera2.pipe.integration.impl.FocusMeteringControl
import androidx.camera.camera2.pipe.integration.impl.State3AControl
import androidx.camera.camera2.pipe.integration.impl.TorchControl
import androidx.camera.camera2.pipe.integration.impl.UseCaseThreads
import androidx.camera.camera2.pipe.integration.impl.ZoomControl
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.core.impl.ImageFormatConstants
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import org.robolectric.shadows.StreamConfigurationMapBuilder

object FakeCameraInfoAdapterCreator {
    private val CAMERA_ID_0 = CameraId("0")

    val useCaseThreads by lazy {
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
        CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE to Rect(0, 0, 640, 480),
        CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES to arrayOf(
            Range(12, 30),
            Range(24, 24),
            Range(30, 30),
            Range(60, 60)
        )
    )

    private val zoomControl = ZoomControl(useCaseThreads, FakeZoomCompat())

    fun createCameraInfoAdapter(
        cameraId: CameraId = CAMERA_ID_0,
        cameraProperties: CameraProperties = FakeCameraProperties(
            FakeCameraMetadata(
                cameraId = cameraId,
                characteristics = cameraCharacteristics
            ),
            cameraId
        ),
        zoomControl: ZoomControl = this.zoomControl,
    ): CameraInfoAdapter {
        val fakeUseCaseCamera = FakeUseCaseCamera()
        val fakeStreamConfigurationMap = StreamConfigurationMapCompat(
            streamConfigurationMap,
            OutputSizesCorrector(cameraProperties.metadata, streamConfigurationMap)
        )
        val fakeCameraQuirks = CameraQuirks(
            cameraProperties.metadata,
            fakeStreamConfigurationMap,
        )
        val state3AControl = State3AControl(
            cameraProperties,
            NoOpAutoFlashAEModeDisabler,
            AeFpsRange(fakeCameraQuirks)
        ).apply {
            useCaseCamera = fakeUseCaseCamera
        }
        return CameraInfoAdapter(
            cameraProperties,
            CameraConfig(cameraId),
            CameraStateAdapter(),
            CameraControlStateAdapter(
                zoomControl,
                EvCompControl(FakeEvCompCompat()),
                TorchControl(cameraProperties, state3AControl, useCaseThreads),
            ),
            CameraCallbackMap(),
            FocusMeteringControl(
                cameraProperties,
                MeteringRegionCorrection.Bindings.provideMeteringRegionCorrection(
                    fakeCameraQuirks
                ),
                state3AControl,
                useCaseThreads,
                FakeZoomCompat(),
            ).apply {
                useCaseCamera = fakeUseCaseCamera
            },
            fakeCameraQuirks,
            EncoderProfilesProviderAdapter(cameraId.value),
            fakeStreamConfigurationMap,
        )
    }
}
