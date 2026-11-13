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

import androidx.camera.core.impl.QuirkSettings
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowBuild

@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.TARGET_SDK])
class ConcurrentCameraUnsupportedQuirkTest(
    private val brand: String,
    private val manufacturer: String,
    private val model: String,
    private val expectedEnabled: Boolean,
) {
    @Test
    fun canEnableConcurrentCameraUnsupportedQuirkCorrectly() {
        ShadowBuild.setBrand(brand)
        ShadowBuild.setManufacturer(manufacturer)
        ShadowBuild.setModel(model)

        assertThat(ConcurrentCameraUnsupportedQuirk.isEnabled()).isEqualTo(expectedEnabled)
        val quirk = DeviceQuirks[ConcurrentCameraUnsupportedQuirk::class.java]
        if (expectedEnabled) {
            assertThat(quirk).isNotNull()
        } else {
            assertThat(quirk).isNull()
        }

        val defaultQuirks = DeviceQuirksLoader.loadQuirks(QuirkSettings.withDefaultBehavior())
        assertThat(defaultQuirks.any { it is ConcurrentCameraUnsupportedQuirk })
            .isEqualTo(expectedEnabled)

        val disabledQuirks = DeviceQuirksLoader.loadQuirks(QuirkSettings.withAllQuirksDisabled())
        assertThat(disabledQuirks.any { it is ConcurrentCameraUnsupportedQuirk }).isFalse()

        val forcedQuirks =
            DeviceQuirksLoader.loadQuirks(
                QuirkSettings.Builder()
                    .forceEnableQuirks(setOf(ConcurrentCameraUnsupportedQuirk::class.java))
                    .build()
            )
        assertThat(forcedQuirks.any { it is ConcurrentCameraUnsupportedQuirk }).isTrue()
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(
            name = "Brand: {0}, Manufacturer: {1}, Model: {2}"
        )
        fun data(): List<Array<Any>> =
            listOf(
                arrayOf("OPPO", "OPPO", "PHT110", true),
                arrayOf("oppo", "oppo", "pht110", true),
                arrayOf("OPPO", "OPPO", "CPH2551", false),
                arrayOf("OPPO", "OPPO", "PHN110", false),
                arrayOf("OnePlus", "OnePlus", "PHT110", false),
                arrayOf("Google", "Google", "Pixel 9 Pro XL", false),
            )
    }
}
