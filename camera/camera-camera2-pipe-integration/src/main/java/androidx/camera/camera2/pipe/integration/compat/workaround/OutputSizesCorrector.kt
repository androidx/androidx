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

import android.util.Size
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.integration.config.CameraScope
import javax.inject.Inject

/**
 * Helper class to provide the StreamConfigurationMap output sizes related correction functions.
 *
 * 1. ExtraSupportedOutputSizeQuirk
 * 2. ExcludedSupportedSizesContainer
 * 3. TargetAspectRatio
 */
@CameraScope
@RequiresApi(21)
class OutputSizesCorrector @Inject constructor(
    @Suppress("UNUSED_PARAMETER") private val cameraMetadata: CameraMetadata
) {
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
        @Suppress("UNUSED_PARAMETER") format: Int
    ): Array<Size>? {
        // TODO(b/271337551): ExtraSupportedOutputSizeQuirk
        return sizes
    }

    /**
     * Adds extra supported output sizes for the specified class by ExtraSupportedOutputSizeQuirk.
     */
    private fun <T> addExtraSupportedOutputSizesByClass(
        sizes: Array<Size>?,
        @Suppress("UNUSED_PARAMETER") klass: Class<T>
    ): Array<Size>? {
        // TODO(b/271337551): ExtraSupportedOutputSizeQuirk
        return sizes
    }

    /**
     * Excludes problematic output sizes for the specified format by
     * ExcludedSupportedSizesContainer.
     */
    private fun excludeProblematicOutputSizesByFormat(
        sizes: Array<Size>?,
        @Suppress("UNUSED_PARAMETER") format: Int
    ): Array<Size>? {
        // TODO(b/244477758): ExcludedSupportedSizeQuirk
        return sizes
    }

    /**
     * Excludes problematic output sizes for the specified class type by
     * ExcludedSupportedSizesContainer.
     */
    private fun <T> excludeProblematicOutputSizesByClass(
        sizes: Array<Size>?,
        @Suppress("UNUSED_PARAMETER") klass: Class<T>
    ): Array<Size>? {
        // TODO(b/244477758): ExcludedSupportedSizeQuirk
        return sizes
    }

    /**
     * Excludes output sizes by TargetAspectRatio.
     */
    private fun excludeOutputSizesByTargetAspectRatioWorkaround(sizes: Array<Size>?): Array<Size>? {
        // TODO(b/245622117): Nexus4AndroidLTargetAspectRatioQuirk and AspectRatioLegacyApi21Quirk
        return sizes
    }
}
