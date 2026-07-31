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
import android.hardware.camera2.CameraExtensionCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.util.Range
import android.util.Size
import androidx.camera.common.CameraExtensionCharacteristicsWrapper
import androidx.camera.common.CameraId
import androidx.camera.common.Metadata
import androidx.camera.common.unwrapAs
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
public class FakeCameraExtensionCharacteristicsTest {

    private val cameraId = CameraId("0")
    private val testCustomKey = Metadata.Key<Int>("test.custom.key")

    @Test
    public fun fakeCameraExtensionCharacteristicsBehavior() {
        val testFormat = 35 // e.g., YUV_420_888
        val testSize = Size(640, 480)
        val testRange = Range(10L, 30L)

        val fake =
            FakeCameraExtensionCharacteristics(
                cameraId = cameraId,
                cameraExtension = CameraExtensionCharacteristics.EXTENSION_BOKEH,
                cameraCharacteristics =
                    mapOf(
                        CameraCharacteristics.LENS_FACING to CameraCharacteristics.LENS_FACING_FRONT
                    ),
                cameraMetadata = mapOf(testCustomKey to 42),
                captureRequestKeys = setOf(CaptureRequest.CONTROL_AE_MODE),
                captureResultKeys = setOf(CaptureResult.LENS_STATE),
                restrictedKeys = setOf(CameraCharacteristics.FLASH_INFO_AVAILABLE),
                dynamicKeys = setOf(CameraCharacteristics.LENS_FACING),
                isRestricted = true,
                isPostviewSupported = true,
                isCaptureProgressSupported = true,
                outputSizesFormat = mapOf(testFormat to setOf(testSize)),
                outputSizesClass = mapOf(String::class.java to setOf(testSize)),
                postviewSizes = mapOf((testSize to testFormat) to setOf(testSize)),
                latencies = mapOf((testSize to testFormat) to testRange),
            )

        assertThat(fake.cameraId).isEqualTo(cameraId)
        assertThat(fake.cameraExtension).isEqualTo(CameraExtensionCharacteristics.EXTENSION_BOKEH)
        assertThat(fake.isRestricted).isTrue()
        assertThat(fake.isPostviewSupported).isTrue()
        assertThat(fake.isCaptureProgressSupported).isTrue()

        // get characteristics
        assertThat(fake[CameraCharacteristics.LENS_FACING])
            .isEqualTo(CameraCharacteristics.LENS_FACING_FRONT)
        assertThat(fake.getOrDefault(CameraCharacteristics.SENSOR_ORIENTATION, 90)).isEqualTo(90)

        // get custom metadata (should always return null/default as it is not supported by
        // extensions, but fake allows it if mapped)
        // Wait, FakeCameraExtensionCharacteristics implements get(Metadata.Key) using
        // cameraMetadata
        assertThat(fake[testCustomKey]).isEqualTo(42)

        // keys
        assertThat(fake.keys).containsExactly(CameraCharacteristics.LENS_FACING)
        assertThat(fake.captureRequestKeys).containsExactly(CaptureRequest.CONTROL_AE_MODE)
        assertThat(fake.captureResultKeys).containsExactly(CaptureResult.LENS_STATE)
        assertThat(fake.restrictedKeys).containsExactly(CameraCharacteristics.FLASH_INFO_AVAILABLE)
        assertThat(fake.dynamicKeys).containsExactly(CameraCharacteristics.LENS_FACING)

        // sizes & latencies
        assertThat(fake.getOutputSizes(testFormat)).containsExactly(testSize)
        assertThat(fake.getOutputSizes(String::class.java)).containsExactly(testSize)
        assertThat(fake.getPostviewSizes(testSize, testFormat)).containsExactly(testSize)
        assertThat(fake.getEstimatedCaptureLatencyRangeMillis(testSize, testFormat))
            .isEqualTo(testRange)

        // unwrap
        assertThat(fake.unwrapAs<CameraExtensionCharacteristicsWrapper>()).isSameInstanceAs(fake)
    }
}
