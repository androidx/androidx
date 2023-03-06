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

package androidx.camera.video.internal.utils;

import static android.media.EncoderProfiles.VideoProfile.HDR_DOLBY_VISION;
import static android.media.EncoderProfiles.VideoProfile.HDR_HDR10;
import static android.media.EncoderProfiles.VideoProfile.HDR_HDR10PLUS;
import static android.media.EncoderProfiles.VideoProfile.HDR_HLG;
import static android.media.EncoderProfiles.VideoProfile.HDR_NONE;

import static androidx.camera.core.DynamicRange.BIT_DEPTH_10_BIT;
import static androidx.camera.core.DynamicRange.BIT_DEPTH_8_BIT;
import static androidx.camera.core.DynamicRange.BIT_DEPTH_UNSPECIFIED;
import static androidx.camera.core.DynamicRange.FORMAT_DOLBY_VISION;
import static androidx.camera.core.DynamicRange.FORMAT_HDR10;
import static androidx.camera.core.DynamicRange.FORMAT_HDR10_PLUS;
import static androidx.camera.core.DynamicRange.FORMAT_HDR_UNSPECIFIED;
import static androidx.camera.core.DynamicRange.FORMAT_HLG;
import static androidx.camera.core.DynamicRange.FORMAT_SDR;
import static androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy.BIT_DEPTH_10;
import static androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy.BIT_DEPTH_8;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import androidx.annotation.RequiresApi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for dynamic range related operations.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class DynamicRangeUtil {

    public static final Map<Integer, Set<Integer>> DR_TO_VP_BIT_DEPTH_MAP = new HashMap<>();
    public static final Map<Integer, Set<Integer>> DR_TO_VP_FORMAT_MAP = new HashMap<>();
    public static final Map<Integer, Integer> VP_TO_DR_FORMAT_MAP = new HashMap<>();

    private DynamicRangeUtil() {
    }

    static {
        // DynamicRange bit depth to VideoProfile bit depth.
        DR_TO_VP_BIT_DEPTH_MAP.put(BIT_DEPTH_8_BIT, new HashSet<>(singletonList(BIT_DEPTH_8)));
        DR_TO_VP_BIT_DEPTH_MAP.put(BIT_DEPTH_10_BIT, new HashSet<>(singletonList(BIT_DEPTH_10)));
        DR_TO_VP_BIT_DEPTH_MAP.put(BIT_DEPTH_UNSPECIFIED,
                new HashSet<>(asList(BIT_DEPTH_8, BIT_DEPTH_10)));

        // DynamicRange format to VideoProfile HDR format.
        DR_TO_VP_FORMAT_MAP.put(FORMAT_SDR, new HashSet<>(singletonList(HDR_NONE)));
        DR_TO_VP_FORMAT_MAP.put(FORMAT_HDR_UNSPECIFIED,
                new HashSet<>(asList(HDR_HLG, HDR_HDR10, HDR_HDR10PLUS, HDR_DOLBY_VISION)));
        DR_TO_VP_FORMAT_MAP.put(FORMAT_HLG, new HashSet<>(singletonList(HDR_HLG)));
        DR_TO_VP_FORMAT_MAP.put(FORMAT_HDR10, new HashSet<>(singletonList(HDR_HDR10)));
        DR_TO_VP_FORMAT_MAP.put(FORMAT_HDR10_PLUS, new HashSet<>(singletonList(HDR_HDR10PLUS)));
        DR_TO_VP_FORMAT_MAP.put(FORMAT_DOLBY_VISION,
                new HashSet<>(singletonList(HDR_DOLBY_VISION)));

        // VideoProfile HDR format to DynamicRange format.
        VP_TO_DR_FORMAT_MAP.put(HDR_NONE, FORMAT_SDR);
        VP_TO_DR_FORMAT_MAP.put(HDR_HLG, FORMAT_HLG);
        VP_TO_DR_FORMAT_MAP.put(HDR_HDR10, FORMAT_HDR10);
        VP_TO_DR_FORMAT_MAP.put(HDR_HDR10PLUS, FORMAT_HDR10_PLUS);
        VP_TO_DR_FORMAT_MAP.put(HDR_DOLBY_VISION, FORMAT_DOLBY_VISION);
    }
}
