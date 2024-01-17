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

import android.os.Build
import androidx.camera.extensions.ExtensionMode.BOKEH
import androidx.camera.extensions.ExtensionMode.FACE_RETOUCH
import androidx.camera.extensions.ExtensionMode.NIGHT
import com.google.common.truth.Truth
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
class ImageAnalysisAvailabilityTest(private val config: Config) {
    @Test
    fun checkImageAnalysisAvailability() {
        // Set up device properties
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", config.brand)
        ReflectionHelpers.setStaticField(Build::class.java, "DEVICE", config.device)
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", config.model)
        val imageAnalysisAvailability = ImageAnalysisAvailability()
        var isAvailable = imageAnalysisAvailability.isAvailable(config.cameraId, config.mode)
        Truth.assertThat(isAvailable).isEqualTo(config.isAvailable)
    }

    class Config(
        val brand: String,
        val device: String,
        val model: String,
        val cameraId: String,
        val mode: Int,
        val isAvailable: Boolean
    )

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun createTestSet(): List<Config> {
            return listOf(
                // Samsung Galaxy S23 Ultra 5G tests
                Config("Samsung", "dm3q", "", "0", BOKEH, false),
                Config("Samsung", "dm3q", "", "0", FACE_RETOUCH, false),
                Config("Samsung", "dm3q", "", "1", BOKEH, false),
                Config("Samsung", "dm3q", "", "1", FACE_RETOUCH, false),
                Config("Samsung", "dm3q", "", "3", BOKEH, false),
                Config("Samsung", "dm3q", "", "3", FACE_RETOUCH, false),
                Config("Samsung", "dm3q", "", "2", BOKEH, true),
                Config("Samsung", "dm3q", "", "0", NIGHT, true),

                // Samsung Galaxy Z Fold3 5G
                Config("Samsung", "q2q", "", "0", BOKEH, false),
                Config("Samsung", "q2q", "", "0", FACE_RETOUCH, false),
                Config("Samsung", "q2q", "", "1", BOKEH, true),
                Config("Samsung", "q2q", "", "0", NIGHT, true),

                // Samsung Galaxy A52s 5G
                Config("Samsung", "a52sxq", "", "0", BOKEH, false),
                Config("Samsung", "a52sxq", "", "0", FACE_RETOUCH, false),
                Config("Samsung", "a52sxq", "", "1", BOKEH, true),
                Config("Samsung", "a52sxq", "", "1", FACE_RETOUCH, true),

                // Samsung Galaxy S22 Ultra tests
                Config("Samsung", "b0q", "", "3", BOKEH, false),
                Config("Samsung", "b0q", "", "3", FACE_RETOUCH, false),
                Config("Samsung", "b0q", "", "0", BOKEH, true),
                Config("Samsung", "b0q", "", "0", FACE_RETOUCH, true),

                // Google Pixel doesn't support ImageAnalysis.
                Config("google", "", "redfin", "0", BOKEH, false),
                Config("google", "", "oriole", "1", NIGHT, false),
                Config("google", "", "pixel unknown", "0", BOKEH, false),

                // Xiaomi 13T Pro doesn't support ImageAnalysis.
                Config("xiaomi", "corot", "", "0", NIGHT, false),
                Config("xiaomi", "corot", "", "1", NIGHT, false),
            )
        }
    }
}
