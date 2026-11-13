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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowBuild
import org.robolectric.util.ReflectionHelpers

@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.TARGET_SDK])
class CloseCameraDeviceOnCameraGraphCloseQuirkTest(
    private val brand: String,
    private val model: String,
    private val device: String,
    private val hardware: String,
    private val board: String,
    private val sdkInt: Int,
    private val expectedEnabled: Boolean,
    private val expectedCloseWithoutExtensions: Boolean,
    private val expectedCloseWithExtensions: Boolean,
) {
    private var originalSdkInt: Int = 0

    @Before
    fun setUp() {
        originalSdkInt = Build.VERSION.SDK_INT
    }

    @After
    fun tearDown() {
        ShadowBuild.reset()
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", originalSdkInt)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(
            name = "{0} {1} ({2}) API {5}: enabled={6}, close={7}, closeExt={8}"
        )
        fun data() =
            listOf(
                // Samsung Galaxy S9 (Snapdragon 845): unconditional.
                arrayOf<Any>("Samsung", "sm-g965u1", "star2qlteue", "qcom", "sdm845", 28, T, T, T),
                arrayOf<Any>("Samsung", "sm-g9650", "star2qltezh", "qcom", "sdm845", 29, T, T, T),
                arrayOf<Any>("Samsung", "sm-g960u", "starqlteue", "qcom", "sdm845", 29, T, T, T),
                // Samsung Galaxy S9 (Exynos 9810) before API 31: not enabled.
                arrayOf<Any>(
                    "Samsung",
                    "sm-g965f",
                    "star2ltexx",
                    "samsungexynos9810",
                    "universal9810",
                    29,
                    F,
                    F,
                    F,
                ),
                arrayOf<Any>(
                    "Samsung",
                    "sm-g960f",
                    "starltexx",
                    "samsungexynos9810",
                    "universal9810",
                    29,
                    F,
                    F,
                    F,
                ),
                // Samsung Exynos 7870 / 7570: unconditional.
                arrayOf<Any>("Samsung", "sm-j600f", "j6lte", "exynos7870", "7870", 28, T, T, T),
                arrayOf<Any>(
                    "Samsung",
                    "sm-j600g",
                    "j6lteub",
                    "samsungexynos7870",
                    "universal7870",
                    28,
                    T,
                    T,
                    T,
                ),
                arrayOf<Any>(
                    "Samsung",
                    "sm-j260f",
                    "j2corelte",
                    "samsungexynos7570",
                    "universal7570",
                    27,
                    T,
                    T,
                    T,
                ),
                // Samsung API 31+: Extensions-only (b/582678736 removed the API 34 ceiling).
                arrayOf<Any>("Samsung", "sm-s901b", "r0s", "s5e9925", "s5e9925", 31, T, F, T),
                arrayOf<Any>("Samsung", "sm-s916u", "dm2q", "qcom", "kalama", 34, T, F, T),
                arrayOf<Any>("Samsung", "sm-s916u", "dm2q", "qcom", "kalama", 35, T, F, T),
                arrayOf<Any>("Samsung", "sm-s938u1", "pa3q", "qcom", "sun", 35, T, F, T),
                arrayOf<Any>("Samsung", "sm-f966u", "q7q", "qcom", "sun", 36, T, F, T),
                arrayOf<Any>("Samsung", "sm-f968u1", "q7mq", "qcom", "sun", 36, T, F, T),
                arrayOf<Any>("Samsung", "sm-s942u1", "m1q", "qcom", "canoe", 36, T, F, T),
                arrayOf<Any>("Samsung", "sm-s908b", "b0s", "s5e9925", "s5e9925", 36, T, F, T),
                arrayOf<Any>("Samsung", "sm-s921b", "e1s", "s5e9945", "s5e9945", 36, T, F, T),
                arrayOf<Any>("Samsung", "sm-f971u1", "h8q", "qcom", "canoe", 37, T, F, T),
                // Samsung Galaxy S22-S24 Snapdragon on API 36+: unconditional (b/538140458).
                arrayOf<Any>("Samsung", "sm-s908u", "b0q", "qcom", "taro", 36, T, T, T),
                arrayOf<Any>("Samsung", "sm-s908u1", "b0q", "qcom", "taro", 36, T, T, T),
                arrayOf<Any>("Samsung", "sm-s918u1", "dm3q", "qcom", "kalama", 36, T, T, T),
                arrayOf<Any>("Samsung", "sm-s921u1", "e1q", "qcom", "pineapple", 36, T, T, T),
                arrayOf<Any>("Samsung", "sm-s926u", "e2q", "qcom", "pineapple", 36, T, T, T),
                arrayOf<Any>("Samsung", "sm-s928u1", "e3q", "qcom", "pineapple", 36, T, T, T),
                // Samsung before API 31 (not an unconditional model): not enabled.
                arrayOf<Any>("Samsung", "sm-s908u", "b0q", "qcom", "taro", 30, F, F, F),
                // Oppo-family API 30-33 vs API 34.
                arrayOf<Any>("OPPO", "CPH2451", "OP5913L1", "mt6983", "mt6983", 33, T, T, T),
                arrayOf<Any>("OPPO", "CPH2451", "OP5913L1", "mt6983", "mt6983", 34, F, F, F),
                // Vivo: unconditional.
                arrayOf<Any>("vivo", "V2250", "PD2241", "qcom", "kalama", 35, T, T, T),
                // Sony Xperia 1 V: unconditional.
                arrayOf<Any>("Sony", "XQ-DQ72", "XQ-DQ72", "qcom", "kalama", 34, T, T, T),
                // Xiaomi 14 Ultra / Xiaomi 14: Extensions-only.
                arrayOf<Any>("Xiaomi", "24031pn0dc", "aurora", "qcom", "pineapple", 34, T, F, T),
                arrayOf<Any>("Xiaomi", "23127pn0cc", "houji", "qcom", "pineapple", 35, T, F, T),
                // Non-quirk device.
                arrayOf<Any>("Google", "Pixel 8", "shiba", "zuma", "zuma", 35, F, F, F),
            )

        private const val T = true
        private const val F = false
    }

    @Test
    fun quirkMatchesExpectedBehavior() {
        ShadowBuild.setBrand(brand)
        ShadowBuild.setManufacturer(brand)
        ShadowBuild.setModel(model)
        ShadowBuild.setDevice(device)
        ShadowBuild.setHardware(hardware)
        ShadowBuild.setBoard(board)
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", sdkInt)

        assertThat(CloseCameraDeviceOnCameraGraphCloseQuirk.isEnabled()).isEqualTo(expectedEnabled)
        val quirk = DeviceQuirks[CloseCameraDeviceOnCameraGraphCloseQuirk::class.java]
        if (expectedEnabled) {
            assertThat(quirk).isNotNull()
        } else {
            assertThat(quirk).isNull()
        }
        if (quirk != null) {
            assertThat(quirk.shouldCloseCameraDevice(isExtensions = false))
                .isEqualTo(expectedCloseWithoutExtensions)
            assertThat(quirk.shouldCloseCameraDevice(isExtensions = true))
                .isEqualTo(expectedCloseWithExtensions)
        }
    }
}
