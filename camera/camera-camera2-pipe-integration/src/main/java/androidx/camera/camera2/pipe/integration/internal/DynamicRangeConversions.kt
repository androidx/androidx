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

package androidx.camera.camera2.pipe.integration.internal

import android.hardware.camera2.params.DynamicRangeProfiles
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.camera.core.DynamicRange

/**
 * Utilities for converting between [DynamicRange] and profiles from
 * [DynamicRangeProfiles].
 */
@RequiresApi(33)
internal object DynamicRangeConversions {
    private val PROFILE_TO_DR_MAP: MutableMap<Long, DynamicRange> = mutableMapOf()
    private val DR_TO_PROFILE_MAP: MutableMap<DynamicRange?, List<Long>> = mutableMapOf()

    init {
        // SDR
        PROFILE_TO_DR_MAP[DynamicRangeProfiles.STANDARD] = DynamicRange.SDR
        DR_TO_PROFILE_MAP[DynamicRange.SDR] = listOf(DynamicRangeProfiles.STANDARD)

        // HLG
        PROFILE_TO_DR_MAP[DynamicRangeProfiles.HLG10] = DynamicRange.HLG_10_BIT
        DR_TO_PROFILE_MAP[PROFILE_TO_DR_MAP[DynamicRangeProfiles.HLG10]] =
            listOf(DynamicRangeProfiles.HLG10)

        // HDR10
        PROFILE_TO_DR_MAP[DynamicRangeProfiles.HDR10] = DynamicRange.HDR10_10_BIT
        DR_TO_PROFILE_MAP[DynamicRange.HDR10_10_BIT] = listOf(DynamicRangeProfiles.HDR10)

        // HDR10+
        PROFILE_TO_DR_MAP[DynamicRangeProfiles.HDR10_PLUS] = DynamicRange.HDR10_PLUS_10_BIT
        DR_TO_PROFILE_MAP[DynamicRange.HDR10_PLUS_10_BIT] = listOf(DynamicRangeProfiles.HDR10_PLUS)

        // Dolby Vision 10-bit
        // A list of the Camera2 10-bit dolby vision profiles ordered by priority. Any API that
        // takes a DynamicRange with dolby vision encoding will attempt to convert to these
        // profiles in order, using the first one that is supported. We will need to add a
        // mechanism for choosing between these
        val dolbyVision10BitProfilesOrdered = listOf(
            DynamicRangeProfiles.DOLBY_VISION_10B_HDR_OEM,
            DynamicRangeProfiles.DOLBY_VISION_10B_HDR_OEM_PO,
            DynamicRangeProfiles.DOLBY_VISION_10B_HDR_REF,
            DynamicRangeProfiles.DOLBY_VISION_10B_HDR_REF_PO
        )
        for (profile in dolbyVision10BitProfilesOrdered) {
            PROFILE_TO_DR_MAP[profile] = DynamicRange.DOLBY_VISION_10_BIT
        }
        DR_TO_PROFILE_MAP[DynamicRange.DOLBY_VISION_10_BIT] =
            dolbyVision10BitProfilesOrdered

        // Dolby vision 8-bit
        val dolbyVision8BitProfilesOrdered = listOf(
            DynamicRangeProfiles.DOLBY_VISION_8B_HDR_OEM,
            DynamicRangeProfiles.DOLBY_VISION_8B_HDR_OEM_PO,
            DynamicRangeProfiles.DOLBY_VISION_8B_HDR_REF,
            DynamicRangeProfiles.DOLBY_VISION_8B_HDR_REF_PO
        )
        for (profile in dolbyVision8BitProfilesOrdered) {
            PROFILE_TO_DR_MAP[profile] = DynamicRange.DOLBY_VISION_8_BIT
        }
        DR_TO_PROFILE_MAP[DynamicRange.DOLBY_VISION_8_BIT] =
            dolbyVision8BitProfilesOrdered
    }

    /**
     * Converts Camera2 dynamic range profile constants to [DynamicRange].
     */
    @DoNotInline
    fun profileToDynamicRange(profile: Long): DynamicRange? {
        return PROFILE_TO_DR_MAP[profile]
    }

    /**
     * Converts a [DynamicRange] to a Camera2 dynamic range profile.
     *
     *
     * For dynamic ranges which can resolve to multiple profiles, the first supported profile
     * from the passed [android.hardware.camera2.params.DynamicRangeProfiles] will be
     * returned. The order in which profiles are checked for support is internally defined.
     *
     *
     * This will only return profiles for fully defined dynamic ranges. For instance, if the
     * format returned by [DynamicRange.getEncoding] is
     * [DynamicRange.ENCODING_HDR_UNSPECIFIED], this will return `null`.
     */
    @DoNotInline
    fun dynamicRangeToFirstSupportedProfile(
        dynamicRange: DynamicRange,
        dynamicRangeProfiles: DynamicRangeProfiles
    ): Long? {
        val orderedProfiles = DR_TO_PROFILE_MAP[dynamicRange]
        if (orderedProfiles != null) {
            val supportedList = dynamicRangeProfiles.supportedProfiles
            for (profile in orderedProfiles) {
                if (supportedList.contains(profile)) {
                    return profile
                }
            }
        }

        // No profile supported
        return null
    }
}
