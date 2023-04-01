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

package androidx.camera.camera2.pipe.integration.compat.workaround

import android.hardware.camera2.params.StreamConfigurationMap
import android.util.Rational
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.integration.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.pipe.integration.compat.quirk.DeviceQuirks
import androidx.camera.camera2.pipe.integration.compat.quirk.ExcludedSupportedSizesQuirk
import androidx.camera.camera2.pipe.integration.compat.quirk.ExtraSupportedOutputSizeQuirk
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.core.Logger
import androidx.camera.core.impl.utils.AspectRatioUtil
import androidx.camera.core.impl.utils.CompareSizesByArea
import java.util.Collections
import javax.inject.Inject

/**
 * Helper class to provide the StreamConfigurationMap output sizes related correction functions.
 *
 * 1. ExtraSupportedOutputSizeQuirk
 * 2. ExcludedSupportedSizesContainer
 * 3. Nexus4AndroidLTargetAspectRatioQuirk
 * 4. AspectRatioLegacyApi21Quirk
 */
@CameraScope
@RequiresApi(21)
class OutputSizesCorrector @Inject constructor(
    private val cameraMetadata: CameraMetadata?,
    private val streamConfigurationMap: StreamConfigurationMap?
) {
    private val tag = "OutputSizesCorrector"
    private val excludedSupportedSizesQuirk: ExcludedSupportedSizesQuirk? =
        DeviceQuirks[ExcludedSupportedSizesQuirk::class.java]
    private val extraSupportedOutputSizeQuirk: ExtraSupportedOutputSizeQuirk? =
        DeviceQuirks[ExtraSupportedOutputSizeQuirk::class.java]
    private val targetAspectRatio: TargetAspectRatio = TargetAspectRatio()
    private val streamConfigurationMapCompat =
        StreamConfigurationMapCompat(streamConfigurationMap, this)

    /**
     * Applies the output sizes related quirks onto the input sizes array.
     */
    fun applyQuirks(sizes: Array<Size>, format: Int): Array<Size> {
        val sizeList = sizes.toMutableList()
        addExtraSupportedOutputSizesByFormat(sizeList, format)
        excludeProblematicOutputSizesByFormat(sizeList, format)
        if (sizeList.isEmpty()) {
            Logger.w(tag, "Sizes array becomes empty after excluding problematic output sizes.")
        }
        val resultSizeArray = excludeOutputSizesByTargetAspectRatioWorkaround(sizeList)
        if (resultSizeArray.isEmpty()) {
            Logger.w(tag, "Sizes array becomes empty after excluding output sizes by target" +
                " aspect ratio workaround.")
        }
        return resultSizeArray
    }

    /**
     * Applies the output sizes related quirks onto the input sizes array.
     */
    fun <T> applyQuirks(sizes: Array<Size>, klass: Class<T>): Array<Size> {
        val sizeList = sizes.toMutableList()
        addExtraSupportedOutputSizesByClass(sizeList, klass)
        excludeProblematicOutputSizesByClass(sizeList, klass)
        if (sizeList.isEmpty()) {
            Logger.w(tag, "Sizes array becomes empty after excluding problematic output sizes.")
        }
        val resultSizeArray = excludeOutputSizesByTargetAspectRatioWorkaround(sizeList)
        if (resultSizeArray.isEmpty()) {
            Logger.w(tag, "Sizes array becomes empty after excluding output sizes by target" +
                " aspect ratio workaround.")
        }
        return resultSizeArray
    }

    /**
     * Adds extra supported output sizes for the specified format by ExtraSupportedOutputSizeQuirk.
     *
     * @param sizeList the original sizes list which must be a mutable list
     * @param format the image format to apply the workaround
     */
    private fun addExtraSupportedOutputSizesByFormat(
        sizeList: MutableList<Size>,
        format: Int
    ) {
        if (extraSupportedOutputSizeQuirk == null) {
            return
        }
        extraSupportedOutputSizeQuirk.getExtraSupportedResolutions(format).let {
            if (it.isNotEmpty()) {
                sizeList.addAll(it)
            }
        }
    }

    /**
     * Adds extra supported output sizes for the specified class by ExtraSupportedOutputSizeQuirk.
     *
     * @param sizeList the original sizes list which must be a mutable list
     * @param klass the class to apply the workaround
     */
    private fun <T> addExtraSupportedOutputSizesByClass(
        sizeList: MutableList<Size>,
        klass: Class<T>
    ) {
        if (extraSupportedOutputSizeQuirk == null) {
            return
        }
        extraSupportedOutputSizeQuirk.getExtraSupportedResolutions(klass).let {
            if (it.isNotEmpty()) {
                sizeList.addAll(it)
            }
        }
    }

    /**
     * Excludes problematic output sizes for the specified format by
     * ExcludedSupportedSizesContainer.
     *
     * @param sizeList the original sizes list which must be a mutable list
     * @param format the image format to apply the workaround
     */
    private fun excludeProblematicOutputSizesByFormat(
        sizeList: MutableList<Size>,
        format: Int
    ) {
        if (cameraMetadata == null || excludedSupportedSizesQuirk == null) {
            return
        }
        excludedSupportedSizesQuirk.getExcludedSizes(cameraMetadata.camera.value, format).let {
            if (it.isNotEmpty()) {
                sizeList.removeAll(it)
            }
        }
    }

    /**
     * Excludes problematic output sizes for the specified class type by
     * ExcludedSupportedSizesContainer.
     *
     * @param sizeList the original sizes list which must be a mutable list
     * @param klass the class to apply the workaround
     */
    private fun <T> excludeProblematicOutputSizesByClass(
        sizeList: MutableList<Size>,
        klass: Class<T>
    ) {
        if (cameraMetadata == null || excludedSupportedSizesQuirk == null) {
            return
        }
        excludedSupportedSizesQuirk.getExcludedSizes(cameraMetadata.camera.value, klass).let {
            if (it.isNotEmpty()) {
                sizeList.removeAll(it)
            }
        }
    }

    /**
     * Excludes output sizes by TargetAspectRatio.
     *
     * @param sizeList the original sizes list
     */
    private fun excludeOutputSizesByTargetAspectRatioWorkaround(
        sizeList: List<Size>
    ): Array<Size> {
        if (cameraMetadata == null) {
            return sizeList.toTypedArray()
        }

        val targetAspectRatio: Int =
            targetAspectRatio[
                cameraMetadata,
                streamConfigurationMapCompat
            ]

        var ratio: Rational? = null

        when (targetAspectRatio) {
            TargetAspectRatio.RATIO_4_3 -> ratio =
                AspectRatioUtil.ASPECT_RATIO_4_3

            TargetAspectRatio.RATIO_16_9 -> ratio =
                AspectRatioUtil.ASPECT_RATIO_16_9

            TargetAspectRatio.RATIO_MAX_JPEG -> {
                val maxJpegSize = Collections.max(sizeList, CompareSizesByArea())
                ratio = Rational(maxJpegSize.width, maxJpegSize.height)
            }

            TargetAspectRatio.RATIO_ORIGINAL -> ratio =
                null
        }

        if (ratio == null) {
            return sizeList.toTypedArray()
        }

        val resultList = mutableListOf<Size>()

        for (size in sizeList) {
            if (AspectRatioUtil.hasMatchingAspectRatio(size, ratio)) {
                resultList.add(size)
            }
        }

        return resultList.toTypedArray()
    }
}
