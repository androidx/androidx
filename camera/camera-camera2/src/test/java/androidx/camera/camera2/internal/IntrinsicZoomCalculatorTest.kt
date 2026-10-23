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

import android.hardware.camera2.CameraCharacteristics
import androidx.camera.camera2.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.CameraBackendId
import androidx.camera.camera2.pipe.testing.FakeCameraDevices
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.HighEndDeviceTemplate
import androidx.camera.common.CameraId
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

    @Test
    fun calculateIntrinsicZoomRatio_returnsOneForDefaultCamera() {
        val defaultMetadata = FakeCameraMetadata.fromTemplate(template = HighEndDeviceTemplate)
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
        // Default camera (HighEndDeviceTemplate): focal length 6.9mm, sensor 9.792x7.3728mm
        // (rotated 90 deg -> horizontal 7.3728mm): 2 * atan(7.3728 / (2 * 6.9)) ≈ 56.20° -> 56°
        val defaultMetadata = FakeCameraMetadata.fromTemplate(template = HighEndDeviceTemplate)

        // Ultra-wide physical camera "3": focal length 2.02mm, sensor 5.6448x4.2336mm
        // (rotated 90 deg -> horizontal 4.2336mm): 2 * atan(4.2336 / (2 * 2.02)) ≈ 92.68° -> 92°
        // Ratio = 56 / 92 ≈ 0.6086956... -> rounded to 2 decimal places = 0.61f
        val otherMetadata = defaultMetadata.awaitPhysicalMetadata(CameraId("3"))

        val fakeCameraDevices =
            FakeCameraDevices(
                defaultCameraBackendId = CameraBackendId(defaultCameraId),
                concurrentCameraBackendIds = emptySet(),
                cameraMetadataMap =
                    mapOf(CameraBackendId(defaultCameraId) to listOf(defaultMetadata)),
            )
        val calculator = IntrinsicZoomCalculatorImpl(fakeCameraDevices)

        val ratio = calculator.calculateIntrinsicZoomRatio(otherMetadata)

        assertThat(ratio).isEqualTo(0.61f)
    }

    @Test
    fun calculateIntrinsicZoomRatio_returnsNullWhenMetadataIncomplete() {
        val incompleteMetadata =
            FakeCameraMetadata.fromTemplate(
                template = HighEndDeviceTemplate,
                characteristicsOverrides = mapOf(CameraCharacteristics.LENS_FACING to null),
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
