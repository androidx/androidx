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

package androidx.camera.camera2.adapter

import androidx.camera.camera2.internal.IntrinsicZoomCalculator
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.HighEndDeviceTemplate
import androidx.camera.camera2.testing.FakeCameraProperties
import androidx.camera.core.CameraInfo
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class PhysicalCameraInfoAdapterTest {

    @Test
    fun intrinsicZoomRatioIsEqualToCalculatorResult_whenCalculatorReturnsValidRatio() {
        // Arrange
        val expectedRatio = 2.5f
        val fakeCalculator =
            object : IntrinsicZoomCalculator {
                override fun calculateIntrinsicZoomRatio(cameraMetadata: CameraMetadata): Float? {
                    return expectedRatio
                }
            }
        val cameraProperties =
            FakeCameraProperties(FakeCameraMetadata.fromTemplate(HighEndDeviceTemplate))
        val adapter = PhysicalCameraInfoAdapter(cameraProperties, fakeCalculator)

        // Act
        val ratio = adapter.intrinsicZoomRatio

        // Assert
        assertThat(ratio).isEqualTo(expectedRatio)
    }

    @Test
    fun intrinsicZoomRatioIsUnknown_whenCalculatorReturnsNull() {
        // Arrange
        val fakeCalculator =
            object : IntrinsicZoomCalculator {
                override fun calculateIntrinsicZoomRatio(cameraMetadata: CameraMetadata): Float? {
                    return null
                }
            }
        val cameraProperties =
            FakeCameraProperties(FakeCameraMetadata.fromTemplate(HighEndDeviceTemplate))
        val adapter = PhysicalCameraInfoAdapter(cameraProperties, fakeCalculator)

        // Act
        val ratio = adapter.intrinsicZoomRatio

        // Assert
        assertThat(ratio).isEqualTo(CameraInfo.INTRINSIC_ZOOM_RATIO_UNKNOWN)
    }
}
