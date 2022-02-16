/*
 * Copyright 2021 The Android Open Source Project
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
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class ExtensionDisabledValidatorTest(private val config: TestConfig) {

    @Test
    fun shouldDisableExtensionMode() {
        // Set up device properties
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", config.brand)
        ReflectionHelpers.setStaticField(Build::class.java, "DEVICE", config.device)

        val validator = ExtensionDisabledValidator()
        assertThat(
            validator.shouldDisableExtension(
                config.cameraId,
                config.extensionMode,
                config.isAdvancedInterface
            )
        ).isEqualTo(config.shouldDisableExtension)
    }

    class TestConfig(
        val brand: String,
        val device: String,
        val cameraId: String,
        val extensionMode: Int,
        val isAdvancedInterface: Boolean,
        val shouldDisableExtension: Boolean
    )

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            return listOf(
                // Pixel 5 extension capability is disabled on basic extender
                TestConfig("Google", "Redfin", "0", ExtensionMode.BOKEH, false, true),
                TestConfig("Google", "Redfin", "0", ExtensionMode.HDR, false, true),
                TestConfig("Google", "Redfin", "0", ExtensionMode.NIGHT, false, true),
                TestConfig("Google", "Redfin", "0", ExtensionMode.FACE_RETOUCH, false, true),
                TestConfig("Google", "Redfin", "0", ExtensionMode.AUTO, false, true),
                TestConfig("Google", "Redfin", "1", ExtensionMode.BOKEH, false, true),
                TestConfig("Google", "Redfin", "1", ExtensionMode.HDR, false, true),
                TestConfig("Google", "Redfin", "1", ExtensionMode.NIGHT, false, true),
                TestConfig("Google", "Redfin", "1", ExtensionMode.FACE_RETOUCH, false, true),
                TestConfig("Google", "Redfin", "1", ExtensionMode.AUTO, false, true),

                // Pixel 5 extension capability is not disabled on advanced extender
                TestConfig("Google", "Redfin", "0", ExtensionMode.NIGHT, true, false),
                TestConfig("Google", "Redfin", "1", ExtensionMode.NIGHT, true, false),

                // Motorola Razr 5G bokeh mode is disabled. Other extension modes should still work.
                TestConfig("Motorola", "Smith", "0", ExtensionMode.BOKEH, false, true),
                TestConfig("Motorola", "Smith", "0", ExtensionMode.HDR, false, false),
                TestConfig("Motorola", "Smith", "1", ExtensionMode.BOKEH, false, true),
                TestConfig("Motorola", "Smith", "1", ExtensionMode.HDR, false, false),
                TestConfig("Motorola", "Smith", "2", ExtensionMode.BOKEH, false, false),
                TestConfig("Motorola", "Smith", "2", ExtensionMode.HDR, false, false),

                // Other cases should be kept normal.
                TestConfig("", "", "0", ExtensionMode.BOKEH, false, false),
                TestConfig("", "", "1", ExtensionMode.BOKEH, false, false)
            )
        }
    }
}
