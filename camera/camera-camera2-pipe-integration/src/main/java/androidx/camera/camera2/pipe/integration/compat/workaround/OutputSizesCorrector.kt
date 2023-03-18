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
    private val excludedSupportedSizesQuirk: ExcludedSupportedSizesQuirk? =
        DeviceQuirks[ExcludedSupportedSizesQuirk::class.java]
    private val extraSupportedOutputSizeQuirk: ExtraSupportedOutputSizeQuirk? =
        DeviceQuirks[ExtraSupportedOutputSizeQuirk::class.java]
    private val targetAspectRatio: TargetAspectRatio = TargetAspectRatio()

    /**
     * Applies the output sizes related quirks onto the input sizes array.
     */
    fun applyQuirks(sizes: Array<Size>?, format: Int): Array<Size>? {
        var result = addExtraSupportedOutputSizesByFormat(sizes, format)
        result = excludeProblematicOutputSizesByFormat(result, format)
        return excludeOutputSizesByTargetAspectRatioWorkaround(result)
    }

    /**
     * Applies the output sizes related quirks onto the input sizes array.
     */
    fun <T> applyQuirks(sizes: Array<Size>?, klass: Class<T>): Array<Size>? {
        var result = addExtraSupportedOutputSizesByClass(sizes, klass)
        result = excludeProblematicOutputSizesByClass(result, klass)
        return excludeOutputSizesByTargetAspectRatioWorkaround(result)
    }

    /**
     * Adds extra supported output sizes for the specified format by ExtraSupportedOutputSizeQuirk.
     */
    private fun addExtraSupportedOutputSizesByFormat(
        sizes: Array<Size>?,
        format: Int
    ): Array<Size>? {
        if (sizes == null || extraSupportedOutputSizeQuirk == null) {
            return sizes
        }
        val extraSizes: Array<Size> =
            extraSupportedOutputSizeQuirk.getExtraSupportedResolutions(format)
        return concatNullableSizeLists(sizes.toList(), extraSizes.toList()).toTypedArray()
    }

    /**
     * Adds extra supported output sizes for the specified class by ExtraSupportedOutputSizeQuirk.
     */
    private fun <T> addExtraSupportedOutputSizesByClass(
        sizes: Array<Size>?,
        klass: Class<T>
    ): Array<Size>? {
        if (sizes == null || extraSupportedOutputSizeQuirk == null) {
            return sizes
        }
        val extraSizes: Array<Size> =
            extraSupportedOutputSizeQuirk.getExtraSupportedResolutions(klass)
        return concatNullableSizeLists(sizes.toList(), extraSizes.toList()).toTypedArray()
    }

    /**
     * Excludes problematic output sizes for the specified format by
     * ExcludedSupportedSizesContainer.
     */
    private fun excludeProblematicOutputSizesByFormat(
        sizes: Array<Size>?,
        format: Int
    ): Array<Size>? {
        if (sizes == null || cameraMetadata == null || excludedSupportedSizesQuirk == null) {
            return sizes
        }
        val excludedSizes: List<Size> =
            excludedSupportedSizesQuirk.getExcludedSizes(cameraMetadata.camera.value, format)

        val resultList: MutableList<Size> = sizes.toMutableList()
        resultList.removeAll(excludedSizes)
        return resultList.toTypedArray()
    }

    /**
     * Excludes problematic output sizes for the specified class type by
     * ExcludedSupportedSizesContainer.
     */
    private fun <T> excludeProblematicOutputSizesByClass(
        sizes: Array<Size>?,
        klass: Class<T>
    ): Array<Size>? {
        if (sizes == null || cameraMetadata == null || excludedSupportedSizesQuirk == null) {
            return sizes
        }
        val excludedSizes: List<Size> =
            excludedSupportedSizesQuirk.getExcludedSizes(cameraMetadata.camera.value, klass)

        val resultList: MutableList<Size> = sizes.toMutableList()
        resultList.removeAll(excludedSizes)
        return resultList.toTypedArray()
    }

    /**
     * Excludes output sizes by TargetAspectRatio.
     */
    private fun excludeOutputSizesByTargetAspectRatioWorkaround(sizes: Array<Size>?): Array<Size>? {
        if (sizes == null || cameraMetadata == null) {
            return null
        }

        val targetAspectRatio: Int =
            targetAspectRatio[
                cameraMetadata,
                StreamConfigurationMapCompat(streamConfigurationMap, this)
            ]

        var ratio: Rational? = null

        when (targetAspectRatio) {
            TargetAspectRatio.RATIO_4_3 -> ratio =
                AspectRatioUtil.ASPECT_RATIO_4_3

            TargetAspectRatio.RATIO_16_9 -> ratio =
                AspectRatioUtil.ASPECT_RATIO_16_9

            TargetAspectRatio.RATIO_MAX_JPEG -> {
                val maxJpegSize = Collections.max(sizes.asList(), CompareSizesByArea())
                ratio = Rational(maxJpegSize.width, maxJpegSize.height)
            }

            TargetAspectRatio.RATIO_ORIGINAL -> ratio =
                null
        }

        if (ratio == null) {
            return sizes
        }

        val resultList: MutableList<Size> = java.util.ArrayList()

        for (size in sizes) {
            if (AspectRatioUtil.hasMatchingAspectRatio(size, ratio)) {
                resultList.add(size)
            }
        }

        return if (resultList.isEmpty()) {
            null
        } else {
            resultList.toTypedArray()
        }
    }

    private fun concatNullableSizeLists(
        sizeList1: List<Size>,
        sizeList2: List<Size>
    ): List<Size> {
        val resultList: MutableList<Size> = ArrayList(sizeList1)
        resultList.addAll(sizeList2)
        return resultList
    }
}
