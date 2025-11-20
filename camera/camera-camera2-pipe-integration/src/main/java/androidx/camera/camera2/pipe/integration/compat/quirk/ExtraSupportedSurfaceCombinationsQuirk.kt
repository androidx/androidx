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

import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import androidx.camera.camera2.pipe.integration.compat.quirk.Device.isGoogleDevice
import androidx.camera.camera2.pipe.integration.compat.quirk.Device.isSamsungDevice
import androidx.camera.core.impl.Quirk
import androidx.camera.core.impl.SurfaceCombination
import androidx.camera.core.impl.SurfaceConfig

/**
 * QuirkSummary Bug Id: b/194149215 Description: Quirk required to include extra supported surface
 * combinations which are additional to the guaranteed supported configurations. An example is the
 * Samsung S7's LIMITED-level camera device can support additional YUV/640x480 + PRIV/PREVIEW +
 * YUV/MAXIMUM combination. Device(s): Samsung S7 devices
 */
public class ExtraSupportedSurfaceCombinationsQuirk : Quirk {
    /** Returns the extra supported surface combinations for specific camera on the device. */
    public fun getExtraSupportedSurfaceCombinations(cameraId: String): List<SurfaceCombination> {
        if (isSamsungS7) {
            return getSamsungS7ExtraCombinations(cameraId)
        }
        return if (
            supportExtraLevel3ConfigurationsGoogleDevice() ||
                supportExtraLevel3ConfigurationsSamsungDevice()
        ) {
            listOf(LEVEL_3_LEVEL_PRIV_PRIV_YUV_SUBSET_CONFIGURATION)
        } else emptyList()
    }

    private fun getSamsungS7ExtraCombinations(cameraId: String): List<SurfaceCombination> {
        val extraCombinations: MutableList<SurfaceCombination> = ArrayList()
        if (cameraId == "1") {
            // (YUV, ANALYSIS) + (PRIV, PREVIEW) + (YUV, MAXIMUM)
            extraCombinations.add(FULL_LEVEL_YUV_PRIV_YUV_CONFIGURATION)
        }
        return extraCombinations
    }

    private fun getLimitedDeviceExtraSupportedFullConfigurations(
        hardwareLevel: Int
    ): List<SurfaceCombination> {
        val extraCombinations: MutableList<SurfaceCombination> = ArrayList()
        if (hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED) {
            // (YUV, ANALYSIS) + (PRIV, PREVIEW) + (YUV, MAXIMUM)
            extraCombinations.add(FULL_LEVEL_YUV_PRIV_YUV_CONFIGURATION)
            // (YUV, ANALYSIS) + (YUV, PREVIEW) + (YUV, MAXIMUM)
            extraCombinations.add(FULL_LEVEL_YUV_YUV_YUV_CONFIGURATION)
        }
        return extraCombinations
    }

    public companion object {
        private const val TAG = "ExtraSupportedSurfaceCombinationsQuirk"
        private val FULL_LEVEL_YUV_PRIV_YUV_CONFIGURATION = createFullYuvPrivYuvConfiguration()
        private val FULL_LEVEL_YUV_YUV_YUV_CONFIGURATION = createFullYuvYuvYuvConfiguration()
        private val LEVEL_3_LEVEL_PRIV_PRIV_YUV_SUBSET_CONFIGURATION =
            createLevel3PrivPrivYuvSubsetConfiguration()
        private val SUPPORT_EXTRA_LEVEL_3_CONFIGURATIONS_GOOGLE_MODELS: Set<String> =
            setOf(
                "PIXEL 6",
                "PIXEL 6 PRO",
                "PIXEL 7",
                "PIXEL 7 PRO",
                "PIXEL 8",
                "PIXEL 8 PRO",
                "PIXEL 9",
                "PIXEL 9 PRO",
                "PIXEL 9 PRO XL",
                "PIXEL 9 PRO FOLD",
            )

        private val SUPPORT_EXTRA_LEVEL_3_CONFIGURATIONS_SAMSUNG_MODELS: Set<String> =
            setOf(
                "SM-S921", // Galaxy S24
                "SC-51E", // Galaxy S24
                "SCG25", // Galaxy S24
                "SM-S926", // Galaxy S24+
                "SM-S928", // Galaxy S24 Ultra
                "SC-52E", // Galaxy S24 Ultra
                "SCG26", // Galaxy S24 Ultra
                "SM-S931", // Galaxy S25
                "SM-S936", // Galaxy S25+
                "SM-S937", // Galaxy S25 Edge
                "SM-S938", // Galaxy S25 Ultra
                "SCG31", // Galaxy S25
                "SCG32", // Galaxy S25 Ultra
                "SC-51F", // Galaxy S25
                "SC-52F", // Galaxy S25 Ultra
            )

        public fun isEnabled(): Boolean {
            return (isSamsungS7 ||
                supportExtraLevel3ConfigurationsGoogleDevice() ||
                supportExtraLevel3ConfigurationsSamsungDevice())
        }

        internal val isSamsungS7: Boolean
            get() =
                "heroqltevzw".equals(Build.DEVICE, ignoreCase = true) ||
                    "heroqltetmo".equals(Build.DEVICE, ignoreCase = true)

        internal fun supportExtraLevel3ConfigurationsGoogleDevice(): Boolean {
            if (!isGoogleDevice()) {
                return false
            }
            val capitalModelName = Build.MODEL.uppercase()
            return SUPPORT_EXTRA_LEVEL_3_CONFIGURATIONS_GOOGLE_MODELS.contains(capitalModelName)
        }

        internal fun supportExtraLevel3ConfigurationsSamsungDevice(): Boolean {
            if (!isSamsungDevice()) {
                return false
            }

            val capitalModelName = Build.MODEL.uppercase()

            // Check if the device model starts with the one of the predefined models
            for (supportedModel in SUPPORT_EXTRA_LEVEL_3_CONFIGURATIONS_SAMSUNG_MODELS) {
                if (capitalModelName.startsWith(supportedModel)) {
                    return true
                }
            }
            return false
        }

        internal fun createFullYuvPrivYuvConfiguration(): SurfaceCombination {
            // (YUV, ANALYSIS) + (PRIV, PREVIEW) + (YUV, MAXIMUM)
            val surfaceCombination = SurfaceCombination()
            surfaceCombination.addSurfaceConfig(
                SurfaceConfig.create(SurfaceConfig.ConfigType.YUV, SurfaceConfig.ConfigSize.VGA)
            )
            surfaceCombination.addSurfaceConfig(
                SurfaceConfig.create(
                    SurfaceConfig.ConfigType.PRIV,
                    SurfaceConfig.ConfigSize.PREVIEW,
                )
            )
            surfaceCombination.addSurfaceConfig(
                SurfaceConfig.create(SurfaceConfig.ConfigType.YUV, SurfaceConfig.ConfigSize.MAXIMUM)
            )
            return surfaceCombination
        }

        internal fun createFullYuvYuvYuvConfiguration(): SurfaceCombination {
            // (YUV, ANALYSIS) + (YUV, PREVIEW) + (YUV, MAXIMUM)
            val surfaceCombination = SurfaceCombination()
            surfaceCombination.addSurfaceConfig(
                SurfaceConfig.create(SurfaceConfig.ConfigType.YUV, SurfaceConfig.ConfigSize.VGA)
            )
            surfaceCombination.addSurfaceConfig(
                SurfaceConfig.create(SurfaceConfig.ConfigType.YUV, SurfaceConfig.ConfigSize.PREVIEW)
            )
            surfaceCombination.addSurfaceConfig(
                SurfaceConfig.create(SurfaceConfig.ConfigType.YUV, SurfaceConfig.ConfigSize.MAXIMUM)
            )
            return surfaceCombination
        }

        /**
         * Creates (PRIV, PREVIEW) + (PRIV, ANALYSIS) + (YUV, MAXIMUM) surface combination.
         *
         * This is a subset of LEVEL_3 camera devices' (PRIV, PREVIEW) + (PRIV, ANALYSIS) + (YUV,
         * MAXIMUM) + (RAW, MAXIMUM) guaranteed supported configuration. This configuration has been
         * verified to make sure that the surface combination can work well on the target devices.
         */
        internal fun createLevel3PrivPrivYuvSubsetConfiguration(): SurfaceCombination {
            // (PRIV, PREVIEW) + (PRIV, ANALYSIS) + (YUV, MAXIMUM)
            val surfaceCombination = SurfaceCombination()
            surfaceCombination.addSurfaceConfig(
                SurfaceConfig.create(
                    SurfaceConfig.ConfigType.PRIV,
                    SurfaceConfig.ConfigSize.PREVIEW,
                )
            )
            surfaceCombination.addSurfaceConfig(
                SurfaceConfig.create(SurfaceConfig.ConfigType.PRIV, SurfaceConfig.ConfigSize.VGA)
            )
            surfaceCombination.addSurfaceConfig(
                SurfaceConfig.create(SurfaceConfig.ConfigType.YUV, SurfaceConfig.ConfigSize.MAXIMUM)
            )
            return surfaceCombination
        }
    }
}
