/*
 * Copyright 2025 The Android Open Source Project
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.util.ReflectionHelpers

@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.S)
class ExtensionCharacteristicsAccessGuardTest(private val config: TestConfig) {
    private lateinit var extensionCharacteristicsAccessGuard: ExtensionCharacteristicsAccessGuard

    @Before
    fun setUp() {
        // Set up device properties
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", config.brand)
        ReflectionHelpers.setStaticField(Build::class.java, "DEVICE", config.device)
        extensionCharacteristicsAccessGuard = ExtensionCharacteristicsAccessGuard()
    }

    @Test
    fun allowPostviewAvailabilityCheck() {
        assertThat(extensionCharacteristicsAccessGuard.allowPostviewAvailabilityCheck())
            .isEqualTo(config.allowPostviewAvailabilityCheck)
    }

    @Test
    fun allowCaptureProcessProgressAvailabilityCheck() {
        assertThat(
                extensionCharacteristicsAccessGuard.allowCaptureProcessProgressAvailabilityCheck()
            )
            .isEqualTo(config.allowCaptureProcessProgressAvailabilityCheck)
    }

    class TestConfig(
        val brand: String,
        val device: String,
        val allowPostviewAvailabilityCheck: Boolean,
        val allowCaptureProcessProgressAvailabilityCheck: Boolean,
    )

    companion object {

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> =
            listOf(TestConfig("Xiaomi", "dada", false, false), TestConfig("", "", true, true))
    }
}
