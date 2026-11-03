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

package androidx.camera.camera2.compat.quirk

import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowBuild

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.TARGET_SDK])
class UnsupportedFormatsQuirkTest {

    @Test
    fun getUnsupportedFormats_onOppoA5BackCamera_returnsRawSensor() {
        ShadowBuild.setManufacturer("OPPO")
        ShadowBuild.setModel("CPH1931")

        assertThat(UnsupportedFormatsQuirk.isEnabled()).isTrue()
        val quirk = UnsupportedFormatsQuirk()
        val backMetadata =
            FakeCameraMetadata(
                characteristics =
                    mapOf(
                        CameraCharacteristics.LENS_FACING to CameraCharacteristics.LENS_FACING_BACK
                    )
            )
        val frontMetadata =
            FakeCameraMetadata(
                characteristics =
                    mapOf(
                        CameraCharacteristics.LENS_FACING to CameraCharacteristics.LENS_FACING_FRONT
                    )
            )

        assertThat(quirk.getUnsupportedFormats(backMetadata))
            .containsExactly(ImageFormat.RAW_SENSOR)
        assertThat(quirk.getUnsupportedFormats(null)).containsExactly(ImageFormat.RAW_SENSOR)
        assertThat(quirk.getUnsupportedFormatsByLensFacing(CameraCharacteristics.LENS_FACING_BACK))
            .containsExactly(ImageFormat.RAW_SENSOR)
        assertThat(quirk.getUnsupportedFormats(frontMetadata)).isEmpty()
        assertThat(quirk.getUnsupportedFormatsByLensFacing(CameraCharacteristics.LENS_FACING_FRONT))
            .isEmpty()
        assertThat(quirk.getUnsupportedFormatsByLensFacing(null)).isEmpty()
    }
}
