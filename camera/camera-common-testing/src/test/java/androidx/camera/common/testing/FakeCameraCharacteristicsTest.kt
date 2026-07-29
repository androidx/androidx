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

package androidx.camera.common.testing

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import androidx.camera.common.CameraCharacteristicsWrapper
import androidx.camera.common.CameraCharacteristicsWrappers.streamConfigurationMap
import androidx.camera.common.CameraId
import androidx.camera.common.Metadata
import androidx.camera.common.unwrapAs
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
public class FakeCameraCharacteristicsTest {

    private val cameraId = CameraId("0")
    private val testCustomKey = Metadata.Key<Int>("test.custom.key")
    private val testCustomKeyAbsent = Metadata.Key<Int>("test.custom.key.absent")

    @Test
    public fun fakeCameraCharacteristicsBehavior() {
        val fake =
            FakeCameraCharacteristics(
                cameraId = cameraId,
                cameraCharacteristics =
                    mapOf(
                        CameraCharacteristics.LENS_FACING to CameraCharacteristics.LENS_FACING_FRONT
                    ),
                cameraMetadata = mapOf(testCustomKey to 42),
            )

        assertThat(fake.cameraId).isEqualTo(cameraId)

        assertThat(fake[CameraCharacteristics.LENS_FACING])
            .isEqualTo(CameraCharacteristics.LENS_FACING_FRONT)
        assertThat(fake[testCustomKey]).isEqualTo(42)
        assertThat(fake[testCustomKeyAbsent]).isNull()

        // getOrDefault
        assertThat(fake.getOrDefault(CameraCharacteristics.LENS_FACING, -1))
            .isEqualTo(CameraCharacteristics.LENS_FACING_FRONT)
        assertThat(fake.getOrDefault(CameraCharacteristics.SENSOR_ORIENTATION, 90)).isEqualTo(90)
        assertThat(fake.getOrDefault(testCustomKey, -1)).isEqualTo(42)
        assertThat(fake.getOrDefault(testCustomKeyAbsent, -1)).isEqualTo(-1)

        // keys and metadataKeys
        assertThat(fake.keys).containsExactly(CameraCharacteristics.LENS_FACING)
        assertThat(fake.metadataKeys).containsExactly(testCustomKey)

        // unwrapAs
        assertThat(fake.unwrapAs<CameraCharacteristicsWrapper>()).isSameInstanceAs(fake)
        assertThat(fake.unwrapAs<CameraCharacteristics>()).isNull()
    }

    @Test
    public fun fakeCameraCharacteristicsValidation_restrictedKeysMustBeInCharacteristics() {
        val restrictedKey = CameraCharacteristics.LENS_FACING
        assertThrows(IllegalArgumentException::class.java) {
            FakeCameraCharacteristics(
                cameraId = cameraId,
                cameraCharacteristics = emptyMap(),
                restrictedKeys = setOf(restrictedKey),
            )
        }
    }

    @Test
    public fun fakeCameraCharacteristicsValidation_sessionKeysMustBeInCharacteristics() {
        val sessionKey = CameraCharacteristics.LENS_FACING
        assertThrows(IllegalArgumentException::class.java) {
            FakeCameraCharacteristics(
                cameraId = cameraId,
                cameraCharacteristics = emptyMap(),
                sessionKeys = setOf(sessionKey),
            )
        }
    }

    @Test
    public fun fakeCameraCharacteristicsValidation_cameraIdMustNotBeInPhysicalCameraIds() {
        assertThrows(IllegalArgumentException::class.java) {
            FakeCameraCharacteristics(cameraId = cameraId, physicalCameraIds = setOf(cameraId))
        }
    }

    @Test
    public fun fakeCameraCharacteristicsProperties() {
        val captureRequestKey = CaptureRequest.CONTROL_AE_MODE
        val captureResultKey = CaptureResult.LENS_STATE
        val physicalCameraId = CameraId("physical-0")
        val sessionKey = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
        val restrictedKey = CameraCharacteristics.FLASH_INFO_AVAILABLE

        val fake =
            FakeCameraCharacteristics(
                cameraId = cameraId,
                cameraCharacteristics =
                    mapOf(
                        sessionKey to CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
                        restrictedKey to true,
                    ),
                captureRequestKeys = setOf(captureRequestKey),
                captureResultKeys = setOf(captureResultKey),
                physicalCaptureRequestKeys = setOf(captureRequestKey),
                sessionKeys = setOf(sessionKey),
                sessionCaptureRequestKeys = setOf(captureRequestKey),
                restrictedKeys = setOf(restrictedKey),
                physicalCameraIds = setOf(physicalCameraId),
            )

        assertThat(fake.captureRequestKeys).containsExactly(captureRequestKey)
        assertThat(fake.captureResultKeys).containsExactly(captureResultKey)
        assertThat(fake.physicalCaptureRequestKeys).containsExactly(captureRequestKey)
        assertThat(fake.sessionKeys).containsExactly(sessionKey)
        assertThat(fake.sessionCaptureRequestKeys).containsExactly(captureRequestKey)
        assertThat(fake.restrictedKeys).containsExactly(restrictedKey)
        assertThat(fake.physicalCameraIds).containsExactly(physicalCameraId)
    }

    @Test
    public fun canGetStreamConfigurationMap() {
        val fakeMap = FakeStreamConfigurationMap(linkedMapOf())
        val fake =
            FakeCameraCharacteristics(
                cameraId = cameraId,
                cameraMetadata =
                    mapOf(CameraCharacteristicsWrapper.Keys.STREAM_CONFIGURATION_MAP to fakeMap),
            )

        assertThat(fake.streamConfigurationMap).isSameInstanceAs(fakeMap)
        // Querying for regular Camera2 key returns null (since we didn't populate it)
        assertThat(fake[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]).isNull()
        // Querying for compatibility key directly returns fakeMap
        assertThat(fake[CameraCharacteristicsWrapper.Keys.STREAM_CONFIGURATION_MAP])
            .isSameInstanceAs(fakeMap)
    }
}
