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

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON
import android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY
import androidx.camera.camera2.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.HighEndDeviceTemplate
import androidx.camera.core.impl.Quirks
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowBuild
import org.robolectric.shadows.StreamConfigurationMapBuilder

@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class LowLightBoostStreamUseCaseQuirkTest(
    private val brand: String,
    private val lowLightBoostSupported: Boolean,
    private val enabled: Boolean,
) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(
            name = "Brand: {0}, lowLightBoostSupported: {1}, enabled: {2}"
        )
        fun data() =
            listOf(
                arrayOf<Any>("Samsung", true, true),
                arrayOf<Any>("samsung", true, true),
                arrayOf<Any>("Samsung", false, false),
                arrayOf<Any>("Google", true, false),
                arrayOf<Any>("Google", false, false),
            )
    }

    private fun getCameraQuirks(lowLightBoostSupported: Boolean): Quirks {
        val modes =
            if (lowLightBoostSupported) {
                intArrayOf(
                    CONTROL_AE_MODE_ON,
                    CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY,
                )
            } else {
                intArrayOf(CONTROL_AE_MODE_ON)
            }
        val cameraMetadata =
            FakeCameraMetadata.fromTemplate(
                template = HighEndDeviceTemplate,
                characteristicsOverrides =
                    mapOf(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES to modes),
            )
        val map = StreamConfigurationMapBuilder.newBuilder().build()
        return CameraQuirks(cameraMetadata, StreamConfigurationMapCompat(map, cameraMetadata))
            .quirks
    }

    @Test
    fun canEnableQuirkCorrectly() {
        ShadowBuild.setBrand(brand)
        ShadowBuild.setManufacturer(brand)
        val cameraQuirks = getCameraQuirks(lowLightBoostSupported)

        assertThat(cameraQuirks.contains(LowLightBoostStreamUseCaseQuirk::class.java))
            .isEqualTo(enabled)
    }
}
