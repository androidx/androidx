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

package androidx.camera.camera2.pipe.integration.compat.quirk

import android.os.Build
import androidx.camera.core.impl.SurfaceCombination
import androidx.camera.core.impl.SurfaceConfig
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.util.ReflectionHelpers

@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class ExtraSupportedSurfaceCombinationsQuirkTest(private val config: Config) {

    @Test
    fun checkExtraSupportedSurfaceCombinations() {
        // Set up brand properties
        if (config.brand != null) {
            ReflectionHelpers.setStaticField(Build::class.java, "BRAND", config.brand)
        }

        // Set up device properties
        if (config.device != null) {
            ReflectionHelpers.setStaticField(Build::class.java, "DEVICE", config.device)
        }

        // Set up model properties
        if (config.model != null) {
            ReflectionHelpers.setStaticField(Build::class.java, "MODEL", config.model)
        }

        // Initializes ExtraSupportedSurfaceCombinationsContainer instance with camera id
        val quirk = ExtraSupportedSurfaceCombinationsQuirk()

        // Gets the extra supported surface combinations on the device
        val extraSurfaceCombinations: List<SurfaceCombination> =
            quirk.getExtraSupportedSurfaceCombinations(config.cameraId)
        for (expectedSupportedSurfaceCombination in config.expectedSupportedSurfaceCombinations) {
            var isSupported = false

            // Checks the combination is supported by the list retrieved from the
            // ExtraSupportedSurfaceCombinationsContainer.
            for (extraSurfaceCombination in extraSurfaceCombinations) {
                if (
                    extraSurfaceCombination.getOrderedSupportedSurfaceConfigList(
                        expectedSupportedSurfaceCombination.surfaceConfigList
                    ) != null
                ) {
                    isSupported = true
                    break
                }
            }
            Truth.assertThat(isSupported).isTrue()
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun data() =
            listOf(
                Config(
                    null,
                    "heroqltevzw",
                    null,
                    "0",
                ),
                Config(null, "heroqltevzw", null, "1", createFullLevelYPYSupportedCombinations()),
                // Tests for Samsung S7 case
                Config(
                    null,
                    "heroqltetmo",
                    null,
                    "0",
                ),
                Config(null, "heroqltetmo", null, "1", createFullLevelYPYSupportedCombinations()),
                // Tests for FULL Pixel devices
                Config(
                    "Google",
                    null,
                    "Pixel 6",
                    "0",
                    createLevel3PrivPrivYuvSubsetConfiguration()
                ),
                Config(
                    "Google",
                    null,
                    "Pixel 6",
                    "1",
                    createLevel3PrivPrivYuvSubsetConfiguration()
                ),
                Config(
                    "Google",
                    null,
                    "Pixel 6 Pro",
                    "0",
                    createLevel3PrivPrivYuvSubsetConfiguration()
                ),
                Config(
                    "Google",
                    null,
                    "Pixel 6 Pro",
                    "1",
                    createLevel3PrivPrivYuvSubsetConfiguration()
                ),
                Config(
                    "Google",
                    null,
                    "Pixel 7",
                    "0",
                    createLevel3PrivPrivYuvSubsetConfiguration()
                ),
                Config(
                    "Google",
                    null,
                    "Pixel 7",
                    "1",
                    createLevel3PrivPrivYuvSubsetConfiguration()
                ),
                Config(
                    "Google",
                    null,
                    "Pixel 7 Pro",
                    "0",
                    createLevel3PrivPrivYuvSubsetConfiguration()
                ),
                Config(
                    "Google",
                    null,
                    "Pixel 7 Pro",
                    "1",
                    createLevel3PrivPrivYuvSubsetConfiguration()
                ),
                Config(
                    "Google",
                    null,
                    "Pixel 8",
                    "0",
                    createLevel3PrivPrivYuvSubsetConfiguration()
                ),
                Config(
                    "Google",
                    null,
                    "Pixel 8",
                    "1",
                    createLevel3PrivPrivYuvSubsetConfiguration()
                ),
                Config(
                    "Google",
                    null,
                    "Pixel 8 Pro",
                    "0",
                    createLevel3PrivPrivYuvSubsetConfiguration()
                ),
                Config(
                    "Google",
                    null,
                    "Pixel 8 Pro",
                    "1",
                    createLevel3PrivPrivYuvSubsetConfiguration()
                ),
                // Tests for FULL Samsung devices
                Config("Samsung", null, "SCG25", "0", createLevel3PrivPrivYuvSubsetConfiguration()),
                Config(
                    "Samsung",
                    null,
                    "SM-S9210",
                    "1",
                    createLevel3PrivPrivYuvSubsetConfiguration()
                ),
                Config(
                    "Samsung",
                    null,
                    "SM-S926B",
                    "0",
                    createLevel3PrivPrivYuvSubsetConfiguration()
                ),
                Config(
                    "Samsung",
                    null,
                    "SM-S926U",
                    "1",
                    createLevel3PrivPrivYuvSubsetConfiguration()
                ),
                Config(
                    "Samsung",
                    null,
                    "SM-S928U1",
                    "0",
                    createLevel3PrivPrivYuvSubsetConfiguration()
                ),
                Config(
                    "Samsung",
                    null,
                    "SM-S928B",
                    "1",
                    createLevel3PrivPrivYuvSubsetConfiguration()
                ),
                // Other cases
                Config(null, null, null, "0")
            )

        private fun createFullLevelYPYSupportedCombinations(): Array<SurfaceCombination> {
            // (YUV, ANALYSIS) + (PRIV, PREVIEW) + (YUV, MAXIMUM)
            val surfaceCombination = SurfaceCombination()
            surfaceCombination.addSurfaceConfig(
                SurfaceConfig.create(SurfaceConfig.ConfigType.YUV, SurfaceConfig.ConfigSize.VGA)
            )
            surfaceCombination.addSurfaceConfig(
                SurfaceConfig.create(
                    SurfaceConfig.ConfigType.PRIV,
                    SurfaceConfig.ConfigSize.PREVIEW
                )
            )
            surfaceCombination.addSurfaceConfig(
                SurfaceConfig.create(SurfaceConfig.ConfigType.YUV, SurfaceConfig.ConfigSize.MAXIMUM)
            )
            return arrayOf(surfaceCombination)
        }

        private fun createFullLevelYPYAndYYYSupportedCombinations(): Array<SurfaceCombination> {
            // (YUV, ANALYSIS) + (PRIV, PREVIEW) + (YUV, MAXIMUM)
            val surfaceCombination1 = SurfaceCombination()
            surfaceCombination1.addSurfaceConfig(
                SurfaceConfig.create(SurfaceConfig.ConfigType.YUV, SurfaceConfig.ConfigSize.VGA)
            )
            surfaceCombination1.addSurfaceConfig(
                SurfaceConfig.create(
                    SurfaceConfig.ConfigType.PRIV,
                    SurfaceConfig.ConfigSize.PREVIEW
                )
            )
            surfaceCombination1.addSurfaceConfig(
                SurfaceConfig.create(SurfaceConfig.ConfigType.YUV, SurfaceConfig.ConfigSize.MAXIMUM)
            )

            // (YUV, ANALYSIS) + (YUV, PREVIEW) + (YUV, MAXIMUM)
            val surfaceCombination2 = SurfaceCombination()
            surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(SurfaceConfig.ConfigType.YUV, SurfaceConfig.ConfigSize.VGA)
            )
            surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(SurfaceConfig.ConfigType.YUV, SurfaceConfig.ConfigSize.PREVIEW)
            )
            surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(SurfaceConfig.ConfigType.YUV, SurfaceConfig.ConfigSize.MAXIMUM)
            )
            return arrayOf(surfaceCombination1, surfaceCombination2)
        }

        private fun createLevel3PrivPrivYuvSubsetConfiguration(): Array<SurfaceCombination> {
            // (PRIV, PREVIEW) + (PRIV, ANALYSIS) + (YUV, MAXIMUM)
            val surfaceCombination = SurfaceCombination()
            surfaceCombination.addSurfaceConfig(
                SurfaceConfig.create(
                    SurfaceConfig.ConfigType.PRIV,
                    SurfaceConfig.ConfigSize.PREVIEW
                )
            )
            surfaceCombination.addSurfaceConfig(
                SurfaceConfig.create(SurfaceConfig.ConfigType.PRIV, SurfaceConfig.ConfigSize.VGA)
            )
            surfaceCombination.addSurfaceConfig(
                SurfaceConfig.create(SurfaceConfig.ConfigType.YUV, SurfaceConfig.ConfigSize.MAXIMUM)
            )
            return arrayOf(surfaceCombination)
        }
    }

    class Config(
        val brand: String?,
        val device: String?,
        val model: String?,
        val cameraId: String,
        val expectedSupportedSurfaceCombinations: Array<SurfaceCombination> = arrayOf()
    )
}
