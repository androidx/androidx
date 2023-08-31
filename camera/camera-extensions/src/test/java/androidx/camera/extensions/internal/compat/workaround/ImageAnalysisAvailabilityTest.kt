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
import androidx.camera.extensions.ExtensionMode.BOKEH
import androidx.camera.extensions.ExtensionMode.FACE_RETOUCH
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
class ImageAnalysisAvailabilityTest(private val config: TestConfig) {

    @Test
    fun checkImageAnalysisAvailability() {
        // Set up device properties
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", config.brand)
        ReflectionHelpers.setStaticField(Build::class.java, "DEVICE", config.device)
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", config.model)
        val imageAnalysisAvailability = ImageAnalysisAvailability()
        assertThat(imageAnalysisAvailability.isAvailable(
            config.cameraId,
            config.hardwareLevel,
            config.mode,
            config.hasPreviewProcessor,
            config.hasImageCaptureProcessor)).isEqualTo(
            config.isAvailable
        )
    }

    class TestConfig(
        val brand: String,
        val device: String,
        val model: String,
        val cameraId: String,
        val hardwareLevel: Int,
        val mode: Int,
        val hasPreviewProcessor: Boolean,
        val hasImageCaptureProcessor: Boolean,
        val isAvailable: Boolean
    )

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            val levelLimited = INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
            val levelFull = INFO_SUPPORTED_HARDWARE_LEVEL_FULL
            val level3 = INFO_SUPPORTED_HARDWARE_LEVEL_3
            return listOf(
                // Samsung Galaxy S23 Ultra 5G tests
                TestConfig("Samsung", "dm3q", "", "0", level3, BOKEH, true, true, true),
                TestConfig("Samsung", "dm3q", "", "1", levelFull, BOKEH, true, true, false),
                TestConfig("Samsung", "dm3q", "", "1", levelFull, FACE_RETOUCH, true, true, false),
                TestConfig("Samsung", "dm3q", "", "2", levelLimited, BOKEH, true, true, false),

                // Samsung Galaxy Z Fold3 5G
                TestConfig("Samsung", "q2q", "", "0", level3, BOKEH, true, true, false),
                TestConfig("Samsung", "q2q", "", "0", level3, FACE_RETOUCH, true, true, false),
                TestConfig("Samsung", "q2q", "", "1", levelLimited, BOKEH, true, true, false),

                // Samsung Galaxy A52s 5G
                TestConfig("Samsung", "a52sxq", "", "0", level3, BOKEH, true, true, false),
                TestConfig("Samsung", "a52sxq", "", "0", level3, BOKEH, true, true, false),
                TestConfig("Samsung", "a52sxq", "", "1", levelLimited, BOKEH, true, true, false),

                // Samsung Galaxy S22 Ultra tests
                TestConfig("Samsung", "b0q", "", "0", level3, BOKEH, true, true, true),
                TestConfig("Samsung", "b0q", "", "3", levelFull, BOKEH, true, true, false),
                TestConfig("Samsung", "b0q", "", "3", levelFull, FACE_RETOUCH, true, true, false),

                // Samsung Galaxy A51 (support extra full level surface combinations)
                TestConfig("Samsung", "", "SM-A515F", "1", levelLimited, BOKEH, true, true, true),

                // Other cases should be determined by hardware level and processors.
                TestConfig("", "", "", "0", level3, BOKEH, true, true, true),
                TestConfig("", "", "", "0", levelFull, BOKEH, true, true, true),
                TestConfig("", "", "", "0", levelFull, BOKEH, false, true, true),
                TestConfig("", "", "", "0", levelLimited, BOKEH, true, true, false),
                TestConfig("", "", "", "0", levelLimited, BOKEH, true, false, true),
                TestConfig("", "", "", "0", levelLimited, BOKEH, false, true, false),
            )
        }
    }
}
