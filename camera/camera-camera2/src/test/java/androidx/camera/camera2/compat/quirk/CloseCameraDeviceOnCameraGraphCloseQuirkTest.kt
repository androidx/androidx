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

import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowBuild

@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class CloseCameraDeviceOnCameraGraphCloseQuirkTest(
    private val brand: String,
    private val model: String,
    private val device: String,
    private val hardware: String,
    private val board: String,
    private val expectedEnabled: Boolean,
    private val expectedShouldCloseWithoutExtensions: Boolean,
) {
    @After
    fun tearDown() {
        ShadowBuild.reset()
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(
            name =
                "Brand: {0}, Model: {1}, Device: {2}, Hardware: {3}, Board: {4}, " +
                    "Enabled: {5}, CloseWithoutExt: {6}"
        )
        fun data() =
            listOf(
                // Samsung Galaxy S9 / S9+ (Snapdragon 845)
                arrayOf<Any>("Samsung", "sm-g965u1", "star2qlteue", "qcom", "sdm845", true, true),
                arrayOf<Any>("Samsung", "sm-g9650", "star2qltezh", "qcom", "sdm845", true, true),
                arrayOf<Any>("Samsung", "sm-g960u", "starqlteue", "qcom", "sdm845", true, true),
                // Samsung Galaxy S9 / S9+ (Exynos 9810 - not affected, quirk not applied without
                // extensions)
                arrayOf<Any>(
                    "Samsung",
                    "sm-g965f",
                    "star2ltexx",
                    "samsungexynos9810",
                    "universal9810",
                    false,
                    false,
                ),
                arrayOf<Any>(
                    "Samsung",
                    "sm-g960f",
                    "starltexx",
                    "samsungexynos9810",
                    "universal9810",
                    false,
                    false,
                ),
                // Samsung Galaxy J6 (Exynos 7870)
                arrayOf<Any>(
                    "Samsung",
                    "sm-j600g",
                    "j6lteub",
                    "samsungexynos7870",
                    "universal7870",
                    true,
                    true,
                ),
                arrayOf<Any>("Samsung", "sm-j600f", "j6lte", "exynos7870", "7870", true, true),
                // Exynos 7570
                arrayOf<Any>(
                    "Samsung",
                    "sm-j260f",
                    "j2corelte",
                    "samsungexynos7570",
                    "universal7570",
                    true,
                    true,
                ),
                // Xiaomi (only enabled when extensions are active)
                arrayOf<Any>("Xiaomi", "23127pn0cc", "aurora", "qcom", "qcom", true, false),
                // Non-quirk device
                arrayOf<Any>("Google", "Pixel 8", "shiba", "zuma", "zuma", false, false),
            )
    }

    @Test
    fun testQuirk() {
        ShadowBuild.setBrand(brand)
        ShadowBuild.setManufacturer(brand)
        ShadowBuild.setModel(model)
        ShadowBuild.setDevice(device)
        ShadowBuild.setHardware(hardware)
        ShadowBuild.setBoard(board)

        val isSamsungProblematic =
            brand.equals("Samsung", ignoreCase = true) &&
                Build.VERSION.SDK_INT in Build.VERSION_CODES.S..Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        val isQuirkEnabled = expectedEnabled || isSamsungProblematic

        assertThat(CloseCameraDeviceOnCameraGraphCloseQuirk.isEnabled()).isEqualTo(isQuirkEnabled)

        if (isQuirkEnabled) {
            val quirk = CloseCameraDeviceOnCameraGraphCloseQuirk()
            assertThat(quirk.shouldCloseCameraDevice(false))
                .isEqualTo(expectedShouldCloseWithoutExtensions)
            if (!expectedShouldCloseWithoutExtensions) {
                assertThat(quirk.shouldCloseCameraDevice(true)).isTrue()
            }
        }
    }
}
