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
import androidx.camera.extensions.ExtensionMode
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
        val imageAnalysisAvailability = ImageAnalysisAvailability()
        assertThat(imageAnalysisAvailability.isUnavailable(config.cameraId, config.mode)).isEqualTo(
            config.isUnavailable
        )
    }

    class TestConfig(
        val brand: String,
        val device: String,
        val cameraId: String,
        val mode: Int,
        val isUnavailable: Boolean
    )

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            return listOf(
                // Samsung Galaxy S23 Ultra 5G tests
                TestConfig("Samsung", "dm3q", "0", ExtensionMode.BOKEH, false),
                TestConfig("Samsung", "dm3q", "1", ExtensionMode.BOKEH, true),
                TestConfig("Samsung", "dm3q", "1", ExtensionMode.FACE_RETOUCH, true),
                TestConfig("Samsung", "dm3q", "1", ExtensionMode.HDR, false),

                // Samsung Galaxy Z Fold3 5G
                TestConfig("Samsung", "q2q", "0", ExtensionMode.BOKEH, true),
                TestConfig("Samsung", "q2q", "0", ExtensionMode.FACE_RETOUCH, true),
                TestConfig("Samsung", "q2q", "0", ExtensionMode.HDR, false),

                // Samsung Galaxy A52s 5G
                TestConfig("Samsung", "a52sxq", "0", ExtensionMode.BOKEH, true),
                TestConfig("Samsung", "a52sxq", "0", ExtensionMode.FACE_RETOUCH, true),
                TestConfig("Samsung", "a52sxq", "0", ExtensionMode.HDR, false),

                // Samsung Galaxy S22 Ultra
                TestConfig("Samsung", "b0q", "0", ExtensionMode.BOKEH, false),
                TestConfig("Samsung", "b0q", "3", ExtensionMode.BOKEH, true),
                TestConfig("Samsung", "b0q", "3", ExtensionMode.FACE_RETOUCH, true),
                TestConfig("Samsung", "b0q", "3", ExtensionMode.HDR, false),

                // Samsung Galaxy Tab S8 Ultra
                TestConfig("Samsung", "gts8uwifi", "0", ExtensionMode.BOKEH, false),
                TestConfig("Samsung", "gts8uwifi", "2", ExtensionMode.BOKEH, true),
                TestConfig("Samsung", "gts8uwifi", "2", ExtensionMode.FACE_RETOUCH, true),
                TestConfig("Samsung", "gts8uwifi", "2", ExtensionMode.HDR, false),

                // Other cases should be kept available.
                TestConfig("", "", "0", ExtensionMode.BOKEH, false),
                TestConfig("", "", "1", ExtensionMode.NONE, false),
            )
        }
    }
}
