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
import androidx.camera.extensions.internal.ExtensionVersion
import androidx.camera.extensions.internal.util.ExtensionsTestUtil.resetSingleton
import androidx.camera.extensions.internal.util.ExtensionsTestUtil.setTestApiVersionAndAdvancedExtender
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.util.ReflectionHelpers

@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(
    minSdk = Build.VERSION_CODES.LOLLIPOP,
    instrumentedPackages = arrayOf("androidx.camera.extensions.internal")
)
class ExtensionDisabledValidatorTest(private val config: TestConfig) {

    @Before
    fun setUp() {
        setTestApiVersionAndAdvancedExtender(config.version, config.isAdvancedInterface)
    }

    @After
    fun tearDown() {
        resetSingleton(ExtensionVersion::class.java, "sExtensionVersion")
    }

    @Test
    fun shouldDisableExtensionMode() {
        // Set up device properties
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", config.brand)
        ReflectionHelpers.setStaticField(Build::class.java, "DEVICE", config.device)

        val validator = ExtensionDisabledValidator()
        assertThat(validator.shouldDisableExtension(config.cameraId))
            .isEqualTo(config.shouldDisableExtension)
    }

    class TestConfig(
        val brand: String,
        val device: String,
        val version: String,
        val isAdvancedInterface: Boolean,
        val cameraId: String,
        val shouldDisableExtension: Boolean
    )

    companion object {
        private const val DEFAULT_BACK_CAMERA_ID = "0"
        private const val DEFAULT_FRONT_CAMERA_ID = "1"

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            return listOf(
                // Pixel 5 extension capability is disabled on basic extender
                TestConfig("Google", "Redfin", "1.2.0", false, DEFAULT_BACK_CAMERA_ID, true),

                // Pixel 5 extension capability is enabled on advanced extender
                TestConfig("Google", "Redfin", "1.2.0", true, DEFAULT_BACK_CAMERA_ID, false),

                // All Motorola devices should be disabled for version 1.1.0 and older.
                TestConfig("Motorola", "Smith", "1.1.0", false, DEFAULT_BACK_CAMERA_ID, true),
                TestConfig("Motorola", "Hawaii P", "1.1.0", false, DEFAULT_BACK_CAMERA_ID, true),

                // Make sure Motorola device would still be enabled for newer versions
                // Motorola doesn't support this today but making sure there is a path to enable
                TestConfig("Motorola", "Hawaii P", "1.2.0", false, DEFAULT_BACK_CAMERA_ID, false),

                // Samsung A52s 5G devices should be disabled for the back camera.
                TestConfig("Samsung", "a52sxq", "1.2.0", true, DEFAULT_BACK_CAMERA_ID, true),
                TestConfig("Samsung", "a52sxq", "1.2.0", true, DEFAULT_FRONT_CAMERA_ID, false),

                // Other cases should be kept normal.
                TestConfig("", "", "1.2.0", false, DEFAULT_BACK_CAMERA_ID, false),

                // Advanced extender is enabled for all devices
                TestConfig("", "", "1.2.0", true, DEFAULT_BACK_CAMERA_ID, false),
            )
        }
    }
}
