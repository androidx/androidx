/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.video

import android.util.Size
import androidx.camera.core.DynamicRange
import androidx.camera.core.impl.DynamicRanges
import androidx.camera.core.impl.EncoderProfilesProvider
import androidx.camera.core.impl.EncoderProfilesProxy
import androidx.camera.video.Quality.QUALITY_SOURCE_REGULAR
import androidx.camera.video.internal.DynamicRangeMatchedEncoderProfilesProvider
import androidx.camera.video.internal.VideoValidatedEncoderProfilesProxy

/**
 * A helper class that manages and resolves [EncoderProfilesProxy] from a [EncoderProfilesProvider].
 */
internal class EncoderProfilesResolver(
    private val hostProfilesProvider: EncoderProfilesProvider,
    @field:Quality.QualitySource private val qualitySource: Int,
    supportedDynamicRanges: Set<DynamicRange>,
) {
    companion object {
        @JvmField
        val EMPTY =
            EncoderProfilesResolver(
                EncoderProfilesProvider.EMPTY,
                QUALITY_SOURCE_REGULAR,
                emptySet(),
            )
    }

    // Mappings of DynamicRange to recording capability information. The mappings are divided
    // into two collections based on the key's (DynamicRange) category, one for specified
    // DynamicRange and one for others. Specified DynamicRange means that its bit depth and
    // format are specified values, not some wildcards, such as: ENCODING_UNSPECIFIED,
    // ENCODING_HDR_UNSPECIFIED or BIT_DEPTH_UNSPECIFIED.
    private val fullySpecifiedMap = mutableMapOf<DynamicRange, CapabilitiesByQuality>()
    private val nonFullySpecifiedMap = mutableMapOf<DynamicRange, CapabilitiesByQuality?>()

    init {
        for (dynamicRange in supportedDynamicRanges) {
            val constrainedProvider =
                DynamicRangeMatchedEncoderProfilesProvider(hostProfilesProvider, dynamicRange)
            val caps = CapabilitiesByQuality(constrainedProvider, qualitySource)
            if (caps.supportedQualities.isNotEmpty()) {
                fullySpecifiedMap[dynamicRange] = caps
            }
        }
    }

    val supportedDynamicRanges: Set<DynamicRange> = fullySpecifiedMap.keys

    /** Gets all supported qualities for the input dynamic range. */
    fun getSupportedQualities(dynamicRange: DynamicRange): List<Quality> =
        getCapabilities(dynamicRange)?.supportedQualities ?: emptyList()

    /** Checks if the quality is supported for the input dynamic range. */
    fun isQualitySupported(quality: Quality, dynamicRange: DynamicRange): Boolean =
        getCapabilities(dynamicRange)?.isQualitySupported(quality) ?: false

    /** Gets the resolution of the input quality and dynamic range. */
    fun getResolution(quality: Quality, dynamicRange: DynamicRange): Size? =
        getCapabilities(dynamicRange)?.getResolution(quality)

    /**
     * Gets the corresponding [VideoValidatedEncoderProfilesProxy] of the input quality and dynamic
     * range.
     */
    fun getProfiles(
        quality: Quality,
        dynamicRange: DynamicRange,
    ): VideoValidatedEncoderProfilesProxy? = getCapabilities(dynamicRange)?.getProfiles(quality)

    /** Finds the supported EncoderProfilesProxy with the resolution nearest to the given [Size]. */
    fun findNearestHigherSupportedEncoderProfilesFor(
        size: Size,
        dynamicRange: DynamicRange,
    ): VideoValidatedEncoderProfilesProxy? =
        getCapabilities(dynamicRange)?.findNearestHigherSupportedEncoderProfilesFor(size)

    /** Finds the nearest quality by number of pixels to the given [Size]. */
    fun findNearestHigherSupportedQualityFor(size: Size, dynamicRange: DynamicRange): Quality =
        getCapabilities(dynamicRange)?.findNearestHigherSupportedQualityFor(size) ?: Quality.NONE

    private fun getCapabilities(dynamicRange: DynamicRange): CapabilitiesByQuality? {
        if (dynamicRange.isFullySpecified) {
            return fullySpecifiedMap[dynamicRange]
        }
        return nonFullySpecifiedMap.getOrPut(dynamicRange) {
            if (DynamicRanges.canResolve(dynamicRange, fullySpecifiedMap.keys)) {
                val constrainedProvider =
                    DynamicRangeMatchedEncoderProfilesProvider(hostProfilesProvider, dynamicRange)
                CapabilitiesByQuality(constrainedProvider, qualitySource)
            } else {
                null
            }
        }
    }
}
