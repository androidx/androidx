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
import static android.media.MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10;
import static android.media.MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10;
import static android.media.MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10Plus;
import static android.media.MediaCodecInfo.CodecProfileLevel.AV1ProfileMain8;
import static android.media.MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvavSe;
import static android.media.MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvheSt;
import static android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMain;
import static android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10;
import static android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10;
import static android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10Plus;
import static android.media.MediaCodecInfo.CodecProfileLevel.VP9Profile0;
import static android.media.MediaCodecInfo.CodecProfileLevel.VP9Profile2;
import static android.media.MediaCodecInfo.CodecProfileLevel.VP9Profile2HDR;
import static android.media.MediaCodecInfo.CodecProfileLevel.VP9Profile2HDR10Plus;

import static androidx.camera.core.DynamicRange.BIT_DEPTH_10_BIT;
import static androidx.camera.core.DynamicRange.BIT_DEPTH_8_BIT;
import static androidx.camera.core.DynamicRange.BIT_DEPTH_UNSPECIFIED;
import static androidx.camera.core.DynamicRange.DOLBY_VISION_10_BIT;
import static androidx.camera.core.DynamicRange.DOLBY_VISION_8_BIT;
import static androidx.camera.core.DynamicRange.ENCODING_DOLBY_VISION;
import static androidx.camera.core.DynamicRange.ENCODING_HDR10;
import static androidx.camera.core.DynamicRange.ENCODING_HDR10_PLUS;
import static androidx.camera.core.DynamicRange.ENCODING_HDR_UNSPECIFIED;
import static androidx.camera.core.DynamicRange.ENCODING_HLG;
import static androidx.camera.core.DynamicRange.ENCODING_SDR;
import static androidx.camera.core.DynamicRange.ENCODING_UNSPECIFIED;
import static androidx.camera.core.DynamicRange.HDR10_10_BIT;
import static androidx.camera.core.DynamicRange.HDR10_PLUS_10_BIT;
import static androidx.camera.core.DynamicRange.HLG_10_BIT;
import static androidx.camera.core.DynamicRange.SDR;
import static androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy.BIT_DEPTH_10;
import static androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy.BIT_DEPTH_8;
import static androidx.core.util.Preconditions.checkArgument;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

import android.media.MediaFormat;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.impl.EncoderProfilesProxy;

import java.util.Collections;
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
    public static final Map<Integer, Integer> VP_TO_DR_BIT_DEPTH = new HashMap<>();
    public static final Map<Integer, Integer> VP_TO_DR_FORMAT_MAP = new HashMap<>();
    private static final Map<String, Map<DynamicRange, Integer>> MIME_TO_DEFAULT_PROFILE_LEVEL_MAP =
            new HashMap<>();

    private DynamicRangeUtil() {
    }

    static {
        // DynamicRange bit depth to VideoProfile bit depth.
        DR_TO_VP_BIT_DEPTH_MAP.put(BIT_DEPTH_8_BIT, new HashSet<>(singletonList(BIT_DEPTH_8)));
        DR_TO_VP_BIT_DEPTH_MAP.put(BIT_DEPTH_10_BIT, new HashSet<>(singletonList(BIT_DEPTH_10)));
        DR_TO_VP_BIT_DEPTH_MAP.put(BIT_DEPTH_UNSPECIFIED,
                new HashSet<>(asList(BIT_DEPTH_8, BIT_DEPTH_10)));

        // DynamicRange encoding to VideoProfile HDR format.
        DR_TO_VP_FORMAT_MAP.put(ENCODING_UNSPECIFIED, new HashSet<>(asList(HDR_NONE, HDR_HLG,
                HDR_HDR10, HDR_HDR10PLUS, HDR_DOLBY_VISION)));
        DR_TO_VP_FORMAT_MAP.put(ENCODING_SDR, new HashSet<>(singletonList(HDR_NONE)));
        DR_TO_VP_FORMAT_MAP.put(ENCODING_HDR_UNSPECIFIED,
                new HashSet<>(asList(HDR_HLG, HDR_HDR10, HDR_HDR10PLUS, HDR_DOLBY_VISION)));
        DR_TO_VP_FORMAT_MAP.put(ENCODING_HLG, new HashSet<>(singletonList(HDR_HLG)));
        DR_TO_VP_FORMAT_MAP.put(ENCODING_HDR10, new HashSet<>(singletonList(HDR_HDR10)));
        DR_TO_VP_FORMAT_MAP.put(ENCODING_HDR10_PLUS, new HashSet<>(singletonList(HDR_HDR10PLUS)));
        DR_TO_VP_FORMAT_MAP.put(ENCODING_DOLBY_VISION,
                new HashSet<>(singletonList(HDR_DOLBY_VISION)));

        // VideoProfile bit depth to DynamicRange bit depth.
        VP_TO_DR_BIT_DEPTH.put(BIT_DEPTH_8, BIT_DEPTH_8_BIT);
        VP_TO_DR_BIT_DEPTH.put(BIT_DEPTH_10, BIT_DEPTH_10_BIT);

        // VideoProfile HDR format to DynamicRange encoding.
        VP_TO_DR_FORMAT_MAP.put(HDR_NONE, ENCODING_SDR);
        VP_TO_DR_FORMAT_MAP.put(HDR_HLG, ENCODING_HLG);
        VP_TO_DR_FORMAT_MAP.put(HDR_HDR10, ENCODING_HDR10);
        VP_TO_DR_FORMAT_MAP.put(HDR_HDR10PLUS, ENCODING_HDR10_PLUS);
        VP_TO_DR_FORMAT_MAP.put(HDR_DOLBY_VISION, ENCODING_DOLBY_VISION);

        //--------------------------------------------------------------------------------------//
        // Default CodecProfileLevel mappings from                                              //
        // frameworks/av/media/codec2/sfplugin/utils/Codec2Mapper.cpp                           //
        //--------------------------------------------------------------------------------------//
        // DynamicRange encodings to HEVC profiles
        Map<DynamicRange, Integer> hevcMap = new HashMap<>();
        hevcMap.put(SDR, HEVCProfileMain);
        hevcMap.put(HLG_10_BIT, HEVCProfileMain10);
        hevcMap.put(HDR10_10_BIT, HEVCProfileMain10HDR10);
        hevcMap.put(HDR10_PLUS_10_BIT, HEVCProfileMain10HDR10Plus);

        // DynamicRange encodings to AV1 profiles for YUV 4:2:0 chroma subsampling
        Map<DynamicRange, Integer> av1420Map = new HashMap<>();
        av1420Map.put(SDR, AV1ProfileMain8);
        av1420Map.put(HLG_10_BIT, AV1ProfileMain10);
        av1420Map.put(HDR10_10_BIT, AV1ProfileMain10HDR10);
        av1420Map.put(HDR10_PLUS_10_BIT, AV1ProfileMain10HDR10Plus);

        // DynamicRange encodings to VP9 profile for YUV 4:2:0 chroma subsampling
        Map<DynamicRange, Integer> vp9420Map = new HashMap<>();
        vp9420Map.put(SDR, VP9Profile0);
        vp9420Map.put(HLG_10_BIT, VP9Profile2);
        vp9420Map.put(HDR10_10_BIT, VP9Profile2HDR);
        vp9420Map.put(HDR10_PLUS_10_BIT, VP9Profile2HDR10Plus);

        // Dolby vision encodings to dolby vision profiles
        Map<DynamicRange, Integer> dvMap = new HashMap<>();
        // Taken from the (now deprecated) Dolby Vision Profile Specification 1.3.3
        // DV Profile 8 (10-bit HEVC)
        dvMap.put(DOLBY_VISION_10_BIT, DolbyVisionProfileDvheSt);
        // DV Profile 9 (8-bit AVC)
        dvMap.put(DOLBY_VISION_8_BIT, DolbyVisionProfileDvavSe);

        // Combine all mime type maps
        MIME_TO_DEFAULT_PROFILE_LEVEL_MAP.put(MediaFormat.MIMETYPE_VIDEO_HEVC, hevcMap);
        MIME_TO_DEFAULT_PROFILE_LEVEL_MAP.put(MediaFormat.MIMETYPE_VIDEO_AV1, av1420Map);
        MIME_TO_DEFAULT_PROFILE_LEVEL_MAP.put(MediaFormat.MIMETYPE_VIDEO_VP9, vp9420Map);
        MIME_TO_DEFAULT_PROFILE_LEVEL_MAP.put(MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION, dvMap);
        //--------------------------------------------------------------------------------------//
    }

    /**
     * Returns a set of possible HDR formats for the given {@link DynamicRange}.
     *
     * <p>The returned HDR formats are those defined in
     * {@link android.media.EncoderProfiles.VideoProfile} prefixed with {@code HDR_}, such as
     * {@link android.media.EncoderProfiles.VideoProfile#HDR_HLG}.
     *
     * <p>Returns an empty set if no HDR formats are supported for the provided dynamic range.
     */
    @NonNull
    public static Set<Integer> dynamicRangeToVideoProfileHdrFormats(
            @NonNull DynamicRange dynamicRange) {
        Set<Integer> hdrFormats = DR_TO_VP_FORMAT_MAP.get(dynamicRange.getEncoding());
        if (hdrFormats == null) {
            hdrFormats = Collections.emptySet();
        }
        return hdrFormats;
    }

    /**
     * Returns a set of possible bit depths for the given {@link DynamicRange}.
     *
     * <p>The returned bit depths are the defined in
     * {@link androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy} prefixed with {@code
     * BIT_DEPTH_}, such as
     * {@link androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy#BIT_DEPTH_10}.
     *
     * <p>Returns an empty set if no bit depths are supported for the provided dynamic range.
     */
    @NonNull
    public static Set<Integer> dynamicRangeToVideoProfileBitDepth(
            @NonNull DynamicRange dynamicRange) {
        Set<Integer> bitDepths = DR_TO_VP_BIT_DEPTH_MAP.get(dynamicRange.getBitDepth());
        if (bitDepths == null) {
            bitDepths = Collections.emptySet();
        }
        return bitDepths;
    }

    /**
     * Returns a codec profile level for a given mime type and dynamic range.
     *
     * <p>If the mime type or dynamic range is not supported, returns
     * {@link EncoderProfilesProxy#CODEC_PROFILE_NONE}.
     *
     * <p>Only fully-specified dynamic ranges are supported. All other dynamic ranges will return
     * {@link EncoderProfilesProxy#CODEC_PROFILE_NONE}.
     */
    public static int dynamicRangeToCodecProfileLevelForMime(@NonNull String mimeType,
            @NonNull DynamicRange dynamicRange) {
        Map<DynamicRange, Integer> hdrToProfile = MIME_TO_DEFAULT_PROFILE_LEVEL_MAP.get(mimeType);
        if (hdrToProfile != null) {
            Integer profile = hdrToProfile.get(dynamicRange);
            if (profile != null) {
                return profile;
            }
        }

        return EncoderProfilesProxy.CODEC_PROFILE_NONE;
    }

    /**
     * Returns the encoding of {@link DynamicRange} for the given HDR format of
     * {@link androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy}.
     *
     * @throws IllegalArgumentException if the input HDR format is not defined in VideoProfileProxy.
     */
    public static int videoProfileHdrFormatsToDynamicRangeEncoding(int hdrFormat) {
        checkArgument(VP_TO_DR_FORMAT_MAP.containsKey(hdrFormat));
        return requireNonNull(VP_TO_DR_FORMAT_MAP.get(hdrFormat));
    }

    /**
     * Returns the bit depth of {@link DynamicRange} for the given bit depth of
     * {@link androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy}.
     *
     * @throws IllegalArgumentException if the input bit depth is not defined in VideoProfileProxy.
     */
    public static int videoProfileBitDepthToDynamicRangeBitDepth(int vpBitDepth) {
        checkArgument(VP_TO_DR_BIT_DEPTH.containsKey(vpBitDepth));
        return requireNonNull(VP_TO_DR_BIT_DEPTH.get(vpBitDepth));
    }

    /**
     * Checks if the HDR settings match between a {@link EncoderProfilesProxy.VideoProfileProxy}
     * and a {@link DynamicRange}.
     *
     * <p>HDR settings includes bit depth and encoding.
     */
    public static boolean isHdrSettingsMatched(
            @NonNull EncoderProfilesProxy.VideoProfileProxy videoProfile,
            @NonNull DynamicRange dynamicRange) {
        return isBitDepthMatched(videoProfile.getBitDepth(), dynamicRange)
                && isHdrEncodingMatched(videoProfile.getHdrFormat(), dynamicRange);
    }

    private static boolean isBitDepthMatched(int bitDepth, @NonNull DynamicRange dynamicRange) {
        Set<Integer> matchedBitDepths = DR_TO_VP_BIT_DEPTH_MAP.get(dynamicRange.getBitDepth());
        return matchedBitDepths != null && matchedBitDepths.contains(bitDepth);
    }

    private static boolean isHdrEncodingMatched(int hdrFormat, @NonNull DynamicRange dynamicRange) {
        Set<Integer> matchedHdrEncodings = DR_TO_VP_FORMAT_MAP.get(dynamicRange.getEncoding());
        return matchedHdrEncodings != null && matchedHdrEncodings.contains(hdrFormat);
    }
}
