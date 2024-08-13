/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.adapter

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.core.impl.SurfaceCombination
import androidx.camera.core.impl.SurfaceConfig
import androidx.camera.core.impl.SurfaceConfig.ConfigSize
import androidx.camera.core.impl.SurfaceConfig.ConfigType

public object GuaranteedConfigurationsUtil {
    @JvmStatic
    public fun getLegacySupportedCombinationList(): List<SurfaceCombination> {
        val combinationList: MutableList<SurfaceCombination> = ArrayList()

        // (PRIV, MAXIMUM)
        SurfaceCombination()
            .apply { addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.MAXIMUM)) }
            .also { combinationList.add(it) }
        // (JPEG, MAXIMUM)
        SurfaceCombination()
            .apply { addSurfaceConfig(SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM)) }
            .also { combinationList.add(it) }
        // (YUV, MAXIMUM)
        SurfaceCombination()
            .apply { addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM)) }
            .also { combinationList.add(it) }
        // Below two combinations are all supported in the combination
        // (PRIV, PREVIEW) + (JPEG, MAXIMUM)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM))
            }
            .also { combinationList.add(it) }
        // (YUV, PREVIEW) + (JPEG, MAXIMUM)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM))
            }
            .also { combinationList.add(it) }
        // (PRIV, PREVIEW) + (PRIV, PREVIEW)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
            }
            .also { combinationList.add(it) }
        // (PRIV, PREVIEW) + (YUV, PREVIEW)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW))
            }
            .also { combinationList.add(it) }
        // (PRIV, PREVIEW) + (PRIV, PREVIEW) + (JPEG, MAXIMUM)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW))

                addSurfaceConfig(SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM))
            }
            .also { combinationList.add(it) }
        return combinationList
    }

    @JvmStatic
    public fun getLimitedSupportedCombinationList(): List<SurfaceCombination> {
        val combinationList: MutableList<SurfaceCombination> = ArrayList()

        // (PRIV, PREVIEW) + (PRIV, RECORD)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.RECORD))
            }
            .also { combinationList.add(it) }
        // (PRIV, PREVIEW) + (YUV, RECORD)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.RECORD))
            }
            .also { combinationList.add(it) }
        // (YUV, PREVIEW) + (YUV, RECORD)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.RECORD))
            }
            .also { combinationList.add(it) }
        // (PRIV, PREVIEW) + (PRIV, RECORD) + (JPEG, RECORD)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.RECORD))

                addSurfaceConfig(SurfaceConfig.create(ConfigType.JPEG, ConfigSize.RECORD))
            }
            .also { combinationList.add(it) }
        // (PRIV, PREVIEW) + (YUV, RECORD) + (JPEG, RECORD)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.RECORD))

                addSurfaceConfig(SurfaceConfig.create(ConfigType.JPEG, ConfigSize.RECORD))
            }
            .also { combinationList.add(it) }
        // (YUV, PREVIEW) + (YUV, PREVIEW) + (JPEG, MAXIMUM)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW))

                addSurfaceConfig(SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM))
            }
            .also { combinationList.add(it) }
        return combinationList
    }

    @JvmStatic
    public fun getFullSupportedCombinationList(): List<SurfaceCombination> {
        val combinationList: MutableList<SurfaceCombination> = ArrayList()

        // (PRIV, PREVIEW) + (PRIV, MAXIMUM)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.MAXIMUM))
            }
            .also { combinationList.add(it) }
        // (PRIV, PREVIEW) + (YUV, MAXIMUM)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM))
            }
            .also { combinationList.add(it) }
        // (YUV, PREVIEW) + (YUV, MAXIMUM)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM))
            }
            .also { combinationList.add(it) }
        // (PRIV, PREVIEW) + (PRIV, PREVIEW) + (JPEG, MAXIMUM)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))

                addSurfaceConfig(SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM))
            }
            .also { combinationList.add(it) }
        // (YUV, VGA) + (PRIV, PREVIEW) + (YUV, MAXIMUM)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.VGA))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))

                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM))
            }
            .also { combinationList.add(it) }
        // (YUV, VGA) + (YUV, PREVIEW) + (YUV, MAXIMUM)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.VGA))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW))

                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM))
            }
            .also { combinationList.add(it) }
        return combinationList
    }

    @JvmStatic
    public fun getRAWSupportedCombinationList(): List<SurfaceCombination> {
        val combinationList: MutableList<SurfaceCombination> = ArrayList()

        // (RAW, MAXIMUM)
        SurfaceCombination()
            .apply { addSurfaceConfig(SurfaceConfig.create(ConfigType.RAW, ConfigSize.MAXIMUM)) }
            .also { combinationList.add(it) }
        // (PRIV, PREVIEW) + (RAW, MAXIMUM)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.RAW, ConfigSize.MAXIMUM))
            }
            .also { combinationList.add(it) }
        // (YUV, PREVIEW) + (RAW, MAXIMUM)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.RAW, ConfigSize.MAXIMUM))
            }
            .also { combinationList.add(it) }
        // (PRIV, PREVIEW) + (PRIV, PREVIEW) + (RAW, MAXIMUM)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.RAW, ConfigSize.MAXIMUM))
            }
            .also { combinationList.add(it) }
        // (PRIV, PREVIEW) + (YUV, PREVIEW) + (RAW, MAXIMUM)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.RAW, ConfigSize.MAXIMUM))
            }
            .also { combinationList.add(it) }
        // (YUV, PREVIEW) + (YUV, PREVIEW) + (RAW, MAXIMUM)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.RAW, ConfigSize.MAXIMUM))
            }
            .also { combinationList.add(it) }
        // (PRIV, PREVIEW) + (JPEG, MAXIMUM) + (RAW, MAXIMUM)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.RAW, ConfigSize.MAXIMUM))
            }
            .also { combinationList.add(it) }
        // (YUV, PREVIEW) + (JPEG, MAXIMUM) + (RAW, MAXIMUM)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.RAW, ConfigSize.MAXIMUM))
            }
            .also { combinationList.add(it) }
        return combinationList
    }

    @JvmStatic
    public fun getBurstSupportedCombinationList(): List<SurfaceCombination> {
        val combinationList: MutableList<SurfaceCombination> = ArrayList()
        // (PRIV, PREVIEW) + (PRIV, MAXIMUM)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.MAXIMUM))
            }
            .also { combinationList.add(it) }
        // (PRIV, PREVIEW) + (YUV, MAXIMUM)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM))
            }
            .also { combinationList.add(it) }
        // (YUV, PREVIEW) + (YUV, MAXIMUM)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM))
            }
            .also { combinationList.add(it) }
        return combinationList
    }

    @JvmStatic
    public fun getLevel3SupportedCombinationList(): List<SurfaceCombination> {
        val combinationList: MutableList<SurfaceCombination> = ArrayList()
        // (PRIV, PREVIEW) + (PRIV, VGA) + (YUV, MAXIMUM) + (RAW, MAXIMUM)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.VGA))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.RAW, ConfigSize.MAXIMUM))
            }
            .also { combinationList.add(it) }
        // (PRIV, PREVIEW) + (PRIV, VGA) + (JPEG, MAXIMUM) + (RAW, MAXIMUM)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.VGA))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.RAW, ConfigSize.MAXIMUM))
            }
            .also { combinationList.add(it) }
        return combinationList
    }

    @JvmStatic
    public fun getUltraHighResolutionSupportedCombinationList(): List<SurfaceCombination> {
        val combinationList: MutableList<SurfaceCombination> = ArrayList()

        // (YUV, ULTRA_MAXIMUM) + (PRIV, PREVIEW) + (PRIV, RECORD)
        // Covers (YUV, ULTRA_MAXIMUM) + (PRIV, PREVIEW) in the guaranteed table.
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.ULTRA_MAXIMUM))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.RECORD))
            }
            .also { combinationList.add(it) }

        // (JPEG, ULTRA_MAXIMUM) + (PRIV, PREVIEW) + (PRIV, RECORD)
        // Covers (JPEG, ULTRA_MAXIMUM) + (PRIV, PREVIEW) in the guaranteed table.
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.JPEG, ConfigSize.ULTRA_MAXIMUM))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.RECORD))
            }
            .also { combinationList.add(it) }

        // (RAW, ULTRA_MAXIMUM) + (PRIV, PREVIEW) + (PRIV, RECORD)
        // Covers (RAW, ULTRA_MAXIMUM) + (PRIV, PREVIEW) in the guaranteed table.
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.RAW, ConfigSize.ULTRA_MAXIMUM))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.RECORD))
            }
            .also { combinationList.add(it) }

        // (YUV, ULTRA_MAXIMUM) + (PRIV, PREVIEW) + (JPEG, MAXIMUM)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.ULTRA_MAXIMUM))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM))
            }
            .also { combinationList.add(it) }

        // (JPEG, ULTRA_MAXIMUM) + (PRIV, PREVIEW) + (JPEG, MAXIMUM)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.JPEG, ConfigSize.ULTRA_MAXIMUM))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM))
            }
            .also { combinationList.add(it) }

        // (RAW, ULTRA_MAXIMUM) + (PRIV, PREVIEW) + (JPEG, MAXIMUM)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.RAW, ConfigSize.ULTRA_MAXIMUM))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM))
            }
            .also { combinationList.add(it) }

        // (YUV, ULTRA_MAXIMUM) + (PRIV, PREVIEW) + (YUV, MAXIMUM)
        // Covers (YUV, ULTRA_MAXIMUM) + (PRIV, PREVIEW) + (YUV, RECORD) in the guaranteed table.
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.ULTRA_MAXIMUM))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM))
            }
            .also { combinationList.add(it) }

        // (JPEG, ULTRA_MAXIMUM) + (PRIV, PREVIEW) + (YUV, MAXIMUM)
        // Covers (JPEG, ULTRA_MAXIMUM) + (PRIV, PREVIEW) + (YUV, RECORD) in the guaranteed table.
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.JPEG, ConfigSize.ULTRA_MAXIMUM))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM))
            }
            .also { combinationList.add(it) }

        // (RAW, ULTRA_MAXIMUM) + (PRIV, PREVIEW) + (YUV, MAXIMUM)
        // Covers (RAW, ULTRA_MAXIMUM) + (PRIV, PREVIEW) + (YUV, RECORD) in the guaranteed table.
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.RAW, ConfigSize.ULTRA_MAXIMUM))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM))
            }
            .also { combinationList.add(it) }

        // (YUV, ULTRA_MAXIMUM) + (PRIV, PREVIEW) + (RAW, MAXIMUM)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.ULTRA_MAXIMUM))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.RAW, ConfigSize.MAXIMUM))
            }
            .also { combinationList.add(it) }

        // (JPEG, ULTRA_MAXIMUM) + (PRIV, PREVIEW) + (RAW, MAXIMUM)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.JPEG, ConfigSize.ULTRA_MAXIMUM))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.RAW, ConfigSize.MAXIMUM))
            }
            .also { combinationList.add(it) }

        // (RAW, ULTRA_MAXIMUM) + (PRIV, PREVIEW) + (RAW, MAXIMUM)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.RAW, ConfigSize.ULTRA_MAXIMUM))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.RAW, ConfigSize.MAXIMUM))
            }
            .also { combinationList.add(it) }

        return combinationList
    }

    /** Returns the minimally guaranteed stream combinations for Ultra HDR. */
    @JvmStatic
    public fun getUltraHdrSupportedCombinationList(): List<SurfaceCombination> {
        // Due to the unique characteristics of JPEG/R, some devices might configure an extra 8-bit
        // JPEG stream internally in addition to the 10-bit YUV stream. The 10-bit mandatory
        // stream combination table is actually not suitable for use. Adds only (PRIV, PREVIEW) +
        // (JPEG_R, MAXIMUM), which is guaranteed by CTS test, as the supported combination.

        val combinationList: MutableList<SurfaceCombination> = ArrayList()

        // (JPEG_R, MAXIMUM)
        SurfaceCombination()
            .apply { addSurfaceConfig(SurfaceConfig.create(ConfigType.JPEG_R, ConfigSize.MAXIMUM)) }
            .also { combinationList.add(it) }

        // (PRIV, PREVIEW) + (JPEG_R, MAXIMUM)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.JPEG_R, ConfigSize.MAXIMUM))
            }
            .also { combinationList.add(it) }

        return combinationList
    }

    @JvmStatic
    public fun getConcurrentSupportedCombinationList(): List<SurfaceCombination> {
        val combinationList: MutableList<SurfaceCombination> = ArrayList()
        // (YUV, s1440p)
        SurfaceCombination()
            .apply { addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.s1440p)) }
            .also { combinationList.add(it) }
        // (PRIV, s1440p)
        SurfaceCombination()
            .apply { addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.s1440p)) }
            .also { combinationList.add(it) }
        // (JPEG, s1440p)
        SurfaceCombination()
            .apply { addSurfaceConfig(SurfaceConfig.create(ConfigType.JPEG, ConfigSize.s1440p)) }
            .also { combinationList.add(it) }
        // (YUV, s720p) + (JPEG, s1440p)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.s720p))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.JPEG, ConfigSize.s1440p))
            }
            .also { combinationList.add(it) }
        // (PRIV, s720p) + (JPEG, s1440p)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.s720p))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.JPEG, ConfigSize.s1440p))
            }
            .also { combinationList.add(it) }
        // (YUV, s720p) + (YUV, s1440p)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.s720p))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.s1440p))
            }
            .also { combinationList.add(it) }
        // (YUV, s720p) + (PRIV, s1440p)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.s720p))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.s1440p))
            }
            .also { combinationList.add(it) }
        // (PRIV, s720p) + (YUV, s1440p)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.s720p))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.s1440p))
            }
            .also { combinationList.add(it) }
        // (PRIV, s720p) + (PRIV, s1440p)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.s720p))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.s1440p))
            }
            .also { combinationList.add(it) }
        return combinationList
    }

    @JvmStatic
    public fun generateSupportedCombinationList(
        hardwareLevel: Int,
        isRawSupported: Boolean,
        isBurstCaptureSupported: Boolean
    ): List<SurfaceCombination> {
        val surfaceCombinations: MutableList<SurfaceCombination> = arrayListOf()
        surfaceCombinations.addAll(getLegacySupportedCombinationList())
        if (
            hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED ||
                hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL ||
                hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3
        ) {
            surfaceCombinations.addAll(getLimitedSupportedCombinationList())
        }
        if (
            hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL ||
                hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3
        ) {
            surfaceCombinations.addAll(getFullSupportedCombinationList())
        }

        if (isRawSupported) {
            surfaceCombinations.addAll(getRAWSupportedCombinationList())
        }
        if (
            isBurstCaptureSupported &&
                hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        ) {
            surfaceCombinations.addAll(getBurstSupportedCombinationList())
        }
        if (hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3) {
            surfaceCombinations.addAll(getLevel3SupportedCombinationList())
        }
        return surfaceCombinations
    }

    /**
     * Returns the minimally guaranteed stream combinations when one or more streams are configured
     * as a 10-bit input.
     */
    @JvmStatic
    public fun get10BitSupportedCombinationList(): List<SurfaceCombination> {
        return listOf(
            // (PRIV, MAXIMUM)
            SurfaceCombination().apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.MAXIMUM))
            },
            // (YUV, MAXIMUM)
            SurfaceCombination().apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM))
            },
            // (PRIV, PREVIEW) + (JPEG, MAXIMUM)
            SurfaceCombination().apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM))
            },
            // (PRIV, PREVIEW) + (YUV, MAXIMUM)
            SurfaceCombination().apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM))
            },
            // (YUV, PREVIEW) + (YUV, MAXIMUM)
            SurfaceCombination().apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM))
            },
            // (PRIV, PREVIEW) + (PRIV, RECORD)
            SurfaceCombination().apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.RECORD))
            },
            // (PRIV, PREVIEW) + (PRIV, RECORD) + (YUV, RECORD)
            SurfaceCombination().apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.RECORD))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.RECORD))
            },
            // (PRIV, PREVIEW) + (PRIV, RECORD) + (JPEG, RECORD)
            SurfaceCombination().apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.RECORD))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.JPEG, ConfigSize.RECORD))
            },
        )
    }

    /**
     * Returns the entire supported stream combinations for devices with Stream Use Case capability
     */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public fun getStreamUseCaseSupportedCombinationList(): List<SurfaceCombination> {
        return listOf<SurfaceCombination>(
            // (PRIV, s1440p, PREVIEW_VIDEO_STILL)
            SurfaceCombination().apply {
                addSurfaceConfig(
                    SurfaceConfig.create(
                        ConfigType.PRIV,
                        ConfigSize.s1440p,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW_VIDEO_STILL
                            .toLong()
                    )
                )
            },
            // (YUV, s1440p, PREVIEW_VIDEO_STILL)
            SurfaceCombination().apply {
                addSurfaceConfig(
                    SurfaceConfig.create(
                        ConfigType.YUV,
                        ConfigSize.s1440p,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW_VIDEO_STILL
                            .toLong()
                    )
                )
            },
            // (PRIV, RECORD, VIDEO_RECORD)
            SurfaceCombination().apply {
                addSurfaceConfig(
                    SurfaceConfig.create(
                        ConfigType.PRIV,
                        ConfigSize.RECORD,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD.toLong()
                    )
                )
            },
            // (YUV, RECORD, VIDEO_RECORD)
            SurfaceCombination().apply {
                addSurfaceConfig(
                    SurfaceConfig.create(
                        ConfigType.YUV,
                        ConfigSize.RECORD,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD.toLong()
                    )
                )
            },
            // (JPEG, MAXIMUM, STILL_CAPTURE)
            SurfaceCombination().apply {
                addSurfaceConfig(
                    SurfaceConfig.create(
                        ConfigType.JPEG,
                        ConfigSize.MAXIMUM,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE.toLong()
                    )
                )
            },
            // (YUV, MAXIMUM, STILL_CAPTURE)
            SurfaceCombination().apply {
                addSurfaceConfig(
                    SurfaceConfig.create(
                        ConfigType.YUV,
                        ConfigSize.MAXIMUM,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE.toLong()
                    )
                )
            },
            // (PRIV, PREVIEW, PREVIEW) + (JPEG, MAXIMUM, STILL_CAPTURE)
            SurfaceCombination().apply {
                addSurfaceConfig(
                    SurfaceConfig.create(
                        ConfigType.PRIV,
                        ConfigSize.PREVIEW,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong()
                    )
                )
                addSurfaceConfig(
                    SurfaceConfig.create(
                        ConfigType.JPEG,
                        ConfigSize.MAXIMUM,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE.toLong()
                    )
                )
            },
            // (PRIV, PREVIEW, PREVIEW) + (YUV, MAXIMUM, STILL_CAPTURE)
            SurfaceCombination().apply {
                addSurfaceConfig(
                    SurfaceConfig.create(
                        ConfigType.PRIV,
                        ConfigSize.PREVIEW,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong()
                    )
                )
                addSurfaceConfig(
                    SurfaceConfig.create(
                        ConfigType.YUV,
                        ConfigSize.MAXIMUM,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE.toLong()
                    )
                )
            },
            // (PRIV, PREVIEW, PREVIEW) + (PRIV, RECORD, VIDEO_RECORD)
            SurfaceCombination().apply {
                addSurfaceConfig(
                    SurfaceConfig.create(
                        ConfigType.PRIV,
                        ConfigSize.PREVIEW,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong()
                    )
                )
                addSurfaceConfig(
                    SurfaceConfig.create(
                        ConfigType.PRIV,
                        ConfigSize.RECORD,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD.toLong()
                    )
                )
            },
            // (PRIV, PREVIEW, PREVIEW) + (YUV, RECORD, VIDEO_RECORD)
            SurfaceCombination().apply {
                addSurfaceConfig(
                    SurfaceConfig.create(
                        ConfigType.PRIV,
                        ConfigSize.PREVIEW,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong()
                    )
                )
                addSurfaceConfig(
                    SurfaceConfig.create(
                        ConfigType.YUV,
                        ConfigSize.RECORD,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD.toLong()
                    )
                )
            },
            // (PRIV, PREVIEW, PREVIEW) + (YUV, PREVIEW, PREVIEW)
            SurfaceCombination().apply {
                addSurfaceConfig(
                    SurfaceConfig.create(
                        ConfigType.PRIV,
                        ConfigSize.PREVIEW,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong()
                    )
                )
                addSurfaceConfig(
                    SurfaceConfig.create(
                        ConfigType.YUV,
                        ConfigSize.PREVIEW,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong()
                    )
                )
            },
            // (PRIV, PREVIEW, PREVIEW) + (PRIV, RECORD, VIDEO_RECORD) +
            // (JPEG, RECORD, STILL_CAPTURE)
            SurfaceCombination().apply {
                addSurfaceConfig(
                    SurfaceConfig.create(
                        ConfigType.PRIV,
                        ConfigSize.PREVIEW,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong()
                    )
                )
                addSurfaceConfig(
                    SurfaceConfig.create(
                        ConfigType.PRIV,
                        ConfigSize.RECORD,
                        CameraMetadata.CONTROL_CAPTURE_INTENT_VIDEO_RECORD.toLong()
                    )
                )
                addSurfaceConfig(
                    SurfaceConfig.create(
                        ConfigType.JPEG,
                        ConfigSize.RECORD,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE.toLong()
                    )
                )
            },
            // (PRIV, PREVIEW, PREVIEW) + (YUV, RECORD, VIDEO_RECORD) +
            // (JPEG, RECORD, STILL_CAPTURE)
            SurfaceCombination().apply {
                addSurfaceConfig(
                    SurfaceConfig.create(
                        ConfigType.PRIV,
                        ConfigSize.PREVIEW,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong()
                    )
                )
                addSurfaceConfig(
                    SurfaceConfig.create(
                        ConfigType.YUV,
                        ConfigSize.RECORD,
                        CameraMetadata.CONTROL_CAPTURE_INTENT_VIDEO_RECORD.toLong()
                    )
                )
                addSurfaceConfig(
                    SurfaceConfig.create(
                        ConfigType.JPEG,
                        ConfigSize.RECORD,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE.toLong()
                    )
                )
            },
            // (PRIV, PREVIEW, PREVIEW) + (YUV, PREVIEW, PREVIEW) + (JPEG, MAXIMUM, STILL_CAPTURE)
            SurfaceCombination().apply {
                addSurfaceConfig(
                    SurfaceConfig.create(
                        ConfigType.PRIV,
                        ConfigSize.PREVIEW,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong()
                    )
                )
                addSurfaceConfig(
                    SurfaceConfig.create(
                        ConfigType.YUV,
                        ConfigSize.PREVIEW,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong()
                    )
                )
                addSurfaceConfig(
                    SurfaceConfig.create(
                        ConfigType.JPEG,
                        ConfigSize.MAXIMUM,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE.toLong()
                    )
                )
            },
        )
    }

    @JvmStatic
    public fun getPreviewStabilizationSupportedCombinationList(): List<SurfaceCombination> {
        val combinationList: MutableList<SurfaceCombination> = ArrayList()
        // (PRIV, s1440p)
        SurfaceCombination()
            .apply { addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.s1440p)) }
            .also { combinationList.add(it) }
        // (YUV, s1440p)
        SurfaceCombination()
            .apply { addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.s1440p)) }
            .also { combinationList.add(it) }
        // (PRIV, s1440p) + (JPEG, MAXIMUM)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.s1440p))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM))
            }
            .also { combinationList.add(it) }
        // (YUV, s1440p) + (JPEG, MAXIMUM)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.s1440p))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM))
            }
            .also { combinationList.add(it) }
        // (PRIV, s1440p) + (YUV, MAXIMUM)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.s1440p))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM))
            }
            .also { combinationList.add(it) }
        // (YUV, s1440p) + (YUV, MAXIMUM)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.s1440p))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM))
            }
            .also { combinationList.add(it) }
        // (PRIV, PREVIEW) + (PRIV, s1440)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.s1440p))
            }
            .also { combinationList.add(it) }
        // (YUV, PREVIEW) + (PRIV, s1440)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.s1440p))
            }
            .also { combinationList.add(it) }
        // (PRIV, PREVIEW) + (YUV, s1440)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.s1440p))
            }
            .also { combinationList.add(it) }
        // (YUV, PREVIEW) + (YUV, s1440)
        SurfaceCombination()
            .apply {
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW))
                addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.s1440p))
            }
            .also { combinationList.add(it) }
        return combinationList
    }
}
