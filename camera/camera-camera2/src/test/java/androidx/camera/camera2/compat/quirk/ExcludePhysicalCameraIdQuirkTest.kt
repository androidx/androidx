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

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowBuild

@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class ExcludePhysicalCameraIdQuirkTest(
    private val brand: String,
    private val manufacturer: String,
    private val model: String,
    private val expectedExcludedIds: Set<String>,
) {
    @Test
    fun canExcludePhysicalCameraIdsCorrectly() {
        ShadowBuild.setBrand(brand)
        ShadowBuild.setManufacturer(manufacturer)
        ShadowBuild.setModel(model)

        val isEnabled = ExcludePhysicalCameraIdQuirk.isEnabled()
        val quirk = DeviceQuirks[ExcludePhysicalCameraIdQuirk::class.java]

        if (expectedExcludedIds.isNotEmpty()) {
            assertThat(isEnabled).isTrue()
            assertThat(quirk).isNotNull()
            assertThat(quirk!!.excludedPhysicalCameraIds).isEqualTo(expectedExcludedIds)
            assertThat(quirk.excludedPhysicalCameraIds)
                .isSameInstanceAs(ExcludePhysicalCameraIdQuirk.loadExcludedPhysicalCameraIds())
        } else {
            assertThat(isEnabled).isFalse()
            assertThat(quirk).isNull()
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(
            name = "Brand: {0}, Manufacturer: {1}, Model: {2}"
        )
        fun data() =
            listOf(
                // Samsung Galaxy S25 / S25 Ultra
                arrayOf("Samsung", "Samsung", "SM-S938B", setOf("2", "3", "4")),
                arrayOf("samsung", "samsung", "SM-S931U", setOf("2", "3", "4")),
                // Samsung Galaxy Z Fold 6 / Z Flip 6
                arrayOf("Samsung", "Samsung", "SM-F968N", setOf("2", "3", "4")),
                arrayOf("Samsung", "Samsung", "SM-F966U", setOf("2", "5", "6")),
                arrayOf("Samsung", "Samsung", "SM-F956B", setOf("2", "5", "6")),
                arrayOf("Samsung", "Samsung", "SM-F766U", setOf("2", "3", "4")),
                arrayOf("Samsung", "Samsung", "SM-F741B", setOf("2", "5")),
                // Samsung Galaxy S24
                arrayOf("Samsung", "Samsung", "SM-S921U", setOf("2", "6")),
                // OPPO Find N2 Flip
                arrayOf("OPPO", "OPPO", "CPH2437", setOf("2")),
                // Xiaomi 15 Pro / 15 Ultra
                arrayOf("Xiaomi", "Xiaomi", "24129PN74C", setOf("2", "3", "4")),
                arrayOf("Xiaomi", "Xiaomi", "25010PN30G", setOf("2", "3", "4")),
                // Sony Xperia
                arrayOf("Sony", "Sony", "SO-41B", setOf("0", "2", "4")),
                arrayOf("Sony", "Sony", "SO-52A", setOf("2", "3", "4")),
                arrayOf("Sony", "Sony", "XQ-DQ72", setOf("2", "3", "4")),
                arrayOf("Sony", "Sony", "XQ-DC54", setOf("2", "3", "4")),
                // Non-problematic devices
                arrayOf("Google", "Google", "Pixel 8", emptySet<String>()),
                arrayOf("Google", "Google", "Pixel 9 Pro", emptySet<String>()),
                arrayOf("Samsung", "Samsung", "SM-S918U", emptySet<String>()), // S23 Ultra
                arrayOf("Xiaomi", "Xiaomi", "23127PN0CC", emptySet<String>()), // Xiaomi 14 Pro
                arrayOf("Motorola", "Motorola", "Moto G84", emptySet<String>()),
            )
    }
}
