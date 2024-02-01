/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.extensions.internal.compat.workaround

import android.hardware.camera2.CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3
import android.hardware.camera2.CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
import android.hardware.camera2.CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.util.ReflectionHelpers

@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(
    minSdk = Build.VERSION_CODES.Q
)
class BasicExtenderSurfaceCombinationAvailabilityTest(private val config: Config) {

    @Test
    fun checkImageAnalysisAvailability() {
        // Set up device properties
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", config.brand)
        ReflectionHelpers.setStaticField(Build::class.java, "DEVICE", config.device)
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", config.model)
        val basicExtenderImageAnalysisAvailability =
                BasicExtenderSurfaceCombinationAvailability()
        val isAvailable = basicExtenderImageAnalysisAvailability.isImageAnalysisAvailable(
            config.hardwareLevel,
            config.hasPreviewProcessor,
            config.hasImageCaptureProcessor
        )
        assertThat(isAvailable).isEqualTo(config.isAvailable)
    }

    class Config(
        val brand: String,
        val device: String,
        val model: String,
        val hardwareLevel: Int,
        val hasPreviewProcessor: Boolean,
        val hasImageCaptureProcessor: Boolean,
        val isAvailable: Boolean
    )

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun createTestSet(): List<Config> {
            val levelLimited = INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
            val levelFull = INFO_SUPPORTED_HARDWARE_LEVEL_FULL
            val level3 = INFO_SUPPORTED_HARDWARE_LEVEL_3
            return listOf(
                // Samsung Galaxy A51 (support extra full level surface combinations)
                Config("Samsung", "", "SM-A515F", levelLimited, true, true, true),
                Config("Samsung", "", "SM-A515F", levelLimited, true, false, true),
                Config("Samsung", "", "SM-A515U", levelLimited, true, true, true),
                Config("Samsung", "", "SM-A515U", levelLimited, true, false, true),
                Config("Samsung", "", "SM-A515W", levelLimited, true, true, true),
                Config("Samsung", "", "SM-A516U1", levelLimited, true, true, true),
                Config("Samsung", "", "SM-A8050", levelLimited, true, true, true),
                Config("Samsung", "", "SM-F907B", levelLimited, true, true, true),

                // Other cases should be determined by hardware level and processors.
                Config("", "", "", level3, true, true, true),
                Config("", "", "", levelFull, true, true, true),
                Config("", "", "", levelFull, false, true, true),
                Config("", "", "", levelLimited, true, true, false),
                Config("", "", "", levelLimited, true, false, true),
                Config("", "", "", levelLimited, false, true, false),
            )
        }
    }
}
