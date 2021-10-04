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
    fun shouldUseDefaultVendorExtender() {
        // Set up device properties
        if (config.brand != null) {
            ReflectionHelpers.setStaticField(Build::class.java, "BRAND", config.brand)
            ReflectionHelpers.setStaticField(Build::class.java, "DEVICE", config.device)
        }

        val validator =
            ExtensionDisabledValidator()
        assertThat(validator.shouldDisableExtension(config.isAdvancedExtenderSupported))
            .isEqualTo(config.shouldDisableExtension)
    }

    class TestConfig(
        val brand: String?,
        val device: String?,
        val isAdvancedExtenderSupported: Boolean,
        val shouldDisableExtension: Boolean
    )

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            return listOf(
                TestConfig("Google", "Redfin", false, true),
                TestConfig("Google", "Redfin", true, false),
                TestConfig("", "", false, false),
                TestConfig("", "", true, false)
            )
        }
    }
}
