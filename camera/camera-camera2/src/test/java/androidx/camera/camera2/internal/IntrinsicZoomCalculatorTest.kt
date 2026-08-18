/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.camera2.internal

import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.util.Size
import android.util.SizeF
import androidx.camera.camera2.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.CameraBackendId
import androidx.camera.camera2.pipe.testing.FakeCameraDevices
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.HighEndDeviceTemplate
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class IntrinsicZoomCalculatorTest {

    private val defaultCameraId = "0"
    private val otherCameraId = "1"

    private val baseCameraCharacteristics: Map<CameraCharacteristics.Key<*>, Any?> =
        mapOf(
            CameraCharacteristics.LENS_FACING to CameraCharacteristics.LENS_FACING_BACK,
            CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS to floatArrayOf(5.0f),
            CameraCharacteristics.SENSOR_ORIENTATION to 0,
            CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE to Size(4000, 3000),
            CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE to Rect(0, 0, 4000, 3000),
            CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE to SizeF(6.4f, 4.8f),
        )

    @Test
    fun calculateIntrinsicZoomRatio_returnsOneForDefaultCamera() {
        val defaultMetadata =
            FakeCameraMetadata.fromTemplate(
                template = HighEndDeviceTemplate,
                characteristicsOverrides = baseCameraCharacteristics,
            )
        val fakeCameraDevices =
            FakeCameraDevices(
                defaultCameraBackendId = CameraBackendId(defaultCameraId),
                concurrentCameraBackendIds = emptySet(),
                cameraMetadataMap =
                    mapOf(CameraBackendId(defaultCameraId) to listOf(defaultMetadata)),
            )
        val calculator = IntrinsicZoomCalculatorImpl(fakeCameraDevices)

        val ratio = calculator.calculateIntrinsicZoomRatio(defaultMetadata)

        assertThat(ratio).isEqualTo(1.0f)
    }

    @Test
    fun calculateIntrinsicZoomRatio_roundsToTwoDecimalPlaces() {
        // Default camera: focal length 5.0mm, sensor width 6.4mm
        // atan(6.4 / (2 * 5.0)) = atan(0.64) ≈ 32.619° -> 2 * 32.619° = 65.238° -> 65° (toInt)
        val defaultMetadata =
            FakeCameraMetadata.fromTemplate(
                template = HighEndDeviceTemplate,
                characteristicsOverrides =
                    baseCameraCharacteristics.toMutableMap().apply {
                        put(
                            CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS,
                            floatArrayOf(5.0f),
                        )
                        put(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE, SizeF(6.4f, 4.8f))
                    },
            )

        // Other camera: focal length 2.0mm, sensor width 6.4mm
        // atan(6.4 / (2 * 2.0)) = atan(1.6) ≈ 57.9946° -> 2 * 57.9946° = 115.989° -> 115° (toInt)
        // Ratio = 65 / 115 ≈ 0.565217... -> rounded to 2 decimal places = 0.57f
        val otherMetadata =
            FakeCameraMetadata.fromTemplate(
                template = HighEndDeviceTemplate,
                characteristicsOverrides =
                    baseCameraCharacteristics.toMutableMap().apply {
                        put(
                            CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS,
                            floatArrayOf(2.0f),
                        )
                        put(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE, SizeF(6.4f, 4.8f))
                    },
            )

        val fakeCameraDevices =
            FakeCameraDevices(
                defaultCameraBackendId = CameraBackendId(defaultCameraId),
                concurrentCameraBackendIds = emptySet(),
                cameraMetadataMap =
                    mapOf(
                        CameraBackendId(defaultCameraId) to listOf(defaultMetadata),
                        CameraBackendId(otherCameraId) to listOf(otherMetadata),
                    ),
            )
        val calculator = IntrinsicZoomCalculatorImpl(fakeCameraDevices)

        val ratio = calculator.calculateIntrinsicZoomRatio(otherMetadata)

        assertThat(ratio).isEqualTo(0.57f)
    }

    @Test
    fun calculateIntrinsicZoomRatio_returnsNullWhenMetadataIncomplete() {
        val incompleteMetadata =
            FakeCameraMetadata.fromTemplate(
                template = HighEndDeviceTemplate,
                characteristicsOverrides =
                    baseCameraCharacteristics.toMutableMap().apply {
                        put(CameraCharacteristics.LENS_FACING, null)
                    },
            )
        val fakeCameraDevices =
            FakeCameraDevices(
                defaultCameraBackendId = CameraBackendId(defaultCameraId),
                concurrentCameraBackendIds = emptySet(),
                cameraMetadataMap =
                    mapOf(CameraBackendId(defaultCameraId) to listOf(incompleteMetadata)),
            )
        val calculator = IntrinsicZoomCalculatorImpl(fakeCameraDevices)

        val ratio = calculator.calculateIntrinsicZoomRatio(incompleteMetadata)

        assertThat(ratio).isNull()
    }
}
