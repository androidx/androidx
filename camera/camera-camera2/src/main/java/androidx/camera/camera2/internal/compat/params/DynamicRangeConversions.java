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

package androidx.camera.camera2.internal.compat.params;

import static android.hardware.camera2.params.DynamicRangeProfiles.DOLBY_VISION_10B_HDR_OEM;
import static android.hardware.camera2.params.DynamicRangeProfiles.DOLBY_VISION_10B_HDR_OEM_PO;
import static android.hardware.camera2.params.DynamicRangeProfiles.DOLBY_VISION_10B_HDR_REF;
import static android.hardware.camera2.params.DynamicRangeProfiles.DOLBY_VISION_10B_HDR_REF_PO;
import static android.hardware.camera2.params.DynamicRangeProfiles.DOLBY_VISION_8B_HDR_OEM;
import static android.hardware.camera2.params.DynamicRangeProfiles.DOLBY_VISION_8B_HDR_OEM_PO;
import static android.hardware.camera2.params.DynamicRangeProfiles.DOLBY_VISION_8B_HDR_REF;
import static android.hardware.camera2.params.DynamicRangeProfiles.DOLBY_VISION_8B_HDR_REF_PO;
import static android.hardware.camera2.params.DynamicRangeProfiles.HDR10;
import static android.hardware.camera2.params.DynamicRangeProfiles.HDR10_PLUS;
import static android.hardware.camera2.params.DynamicRangeProfiles.HLG10;
import static android.hardware.camera2.params.DynamicRangeProfiles.STANDARD;

import android.hardware.camera2.params.DynamicRangeProfiles;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.DynamicRange;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utilities for converting between {@link DynamicRange} and profiles from
 * {@link DynamicRangeProfiles}.
 */
@RequiresApi(33)
public final class DynamicRangeConversions {
    private static final Map<Long, DynamicRange> PROFILE_TO_DR_MAP = new HashMap<>();
    private static final Map<DynamicRange, List<Long>> DR_TO_PROFILE_MAP = new HashMap<>();

    static {
        // SDR
        PROFILE_TO_DR_MAP.put(STANDARD, DynamicRange.SDR);
        DR_TO_PROFILE_MAP.put(DynamicRange.SDR, Collections.singletonList(STANDARD));

        // HLG
        PROFILE_TO_DR_MAP.put(HLG10,
                new DynamicRange(DynamicRange.FORMAT_HLG, DynamicRange.BIT_DEPTH_10_BIT));
        DR_TO_PROFILE_MAP.put(PROFILE_TO_DR_MAP.get(HLG10), Collections.singletonList(HLG10));

        // HDR10
        DynamicRange hdr10 = new DynamicRange(DynamicRange.FORMAT_HDR10,
                DynamicRange.BIT_DEPTH_10_BIT);
        PROFILE_TO_DR_MAP.put(HDR10, hdr10);
        DR_TO_PROFILE_MAP.put(hdr10, Collections.singletonList(HDR10));

        // HDR10+
        DynamicRange hdr10Plus = new DynamicRange(DynamicRange.FORMAT_HDR10_PLUS,
                DynamicRange.BIT_DEPTH_10_BIT);
        PROFILE_TO_DR_MAP.put(HDR10_PLUS, hdr10Plus);
        DR_TO_PROFILE_MAP.put(hdr10Plus, Collections.singletonList(HDR10_PLUS));

        // Dolby Vision 10-bit
        DynamicRange dolbyVision10Bit = new DynamicRange(DynamicRange.FORMAT_DOLBY_VISION,
                DynamicRange.BIT_DEPTH_10_BIT);
        // A list of the Camera2 10-bit dolby vision profiles ordered by priority. Any API that
        // takes a DynamicRange with dolby vision format will attempt to convert to these
        // profiles in order, using the first one that is supported. We will need to add a
        // mechanism for choosing between these
        List<Long> dolbyVision10BitProfilesOrdered = Arrays.asList(DOLBY_VISION_10B_HDR_OEM,
                DOLBY_VISION_10B_HDR_OEM_PO, DOLBY_VISION_10B_HDR_REF, DOLBY_VISION_10B_HDR_REF_PO);
        for (Long profile : dolbyVision10BitProfilesOrdered) {
            PROFILE_TO_DR_MAP.put(profile, dolbyVision10Bit);
        }
        DR_TO_PROFILE_MAP.put(dolbyVision10Bit, dolbyVision10BitProfilesOrdered);

        // Dolby vision 8-bit
        DynamicRange dolbyVision8Bit = new DynamicRange(DynamicRange.FORMAT_DOLBY_VISION,
                DynamicRange.BIT_DEPTH_8_BIT);
        List<Long> dolbyVision8BitProfilesOrdered = Arrays.asList(DOLBY_VISION_8B_HDR_OEM,
                DOLBY_VISION_8B_HDR_OEM_PO, DOLBY_VISION_8B_HDR_REF, DOLBY_VISION_8B_HDR_REF_PO);
        for (Long profile : dolbyVision8BitProfilesOrdered) {
            PROFILE_TO_DR_MAP.put(profile, dolbyVision8Bit);
        }
        DR_TO_PROFILE_MAP.put(dolbyVision8Bit, dolbyVision8BitProfilesOrdered);
    }

    /**
     * Converts Camera2 dynamic range profile constants to {@link DynamicRange}.
     */
    @DoNotInline
    @Nullable
    public static DynamicRange profileToDynamicRange(long profile) {
        return PROFILE_TO_DR_MAP.get(profile);
    }

    /**
     * Converts a {@link DynamicRange} to a Camera2 dynamic range profile.
     *
     * <p>For dynamic ranges which can resolve to multiple profiles, the first supported profile
     * from the passed {@link android.hardware.camera2.params.DynamicRangeProfiles} will be
     * returned. The order in which profiles are checked for support is internally defined.
     *
     * <p>This will only return profiles for fully defined dynamic ranges. For instance, if the
     * format returned by {@link DynamicRange#getFormat()} is
     * {@link DynamicRange#FORMAT_HDR_UNSPECIFIED}, this will return {@code null}.
     */
    @DoNotInline
    @Nullable
    public static Long dynamicRangeToFirstSupportedProfile(@NonNull DynamicRange dynamicRange,
            @NonNull DynamicRangeProfiles dynamicRangeProfiles) {
        List<Long> orderedProfiles = DR_TO_PROFILE_MAP.get(dynamicRange);
        if (orderedProfiles != null) {
            Set<Long> supportedList = dynamicRangeProfiles.getSupportedProfiles();
            for (Long profile : orderedProfiles) {
                if (supportedList.contains(profile)) {
                    return profile;
                }
            }
        }

        // No profile supported
        return null;
    }

    // Utility class should not be instantiated
    private DynamicRangeConversions() {}
}
