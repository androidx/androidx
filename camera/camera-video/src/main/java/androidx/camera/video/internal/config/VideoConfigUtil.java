/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.video.internal.config;

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
import static android.media.MediaCodecInfo.CodecProfileLevel.VP9Profile1;
import static android.media.MediaCodecInfo.CodecProfileLevel.VP9Profile2;
import static android.media.MediaCodecInfo.CodecProfileLevel.VP9Profile2HDR;
import static android.media.MediaCodecInfo.CodecProfileLevel.VP9Profile2HDR10Plus;
import static android.media.MediaCodecInfo.CodecProfileLevel.VP9Profile3;
import static android.media.MediaCodecInfo.CodecProfileLevel.VP9Profile3HDR;
import static android.media.MediaCodecInfo.CodecProfileLevel.VP9Profile3HDR10Plus;

import static androidx.camera.video.internal.encoder.VideoEncoderDataSpace.ENCODER_DATA_SPACE_BT2020_HLG;
import static androidx.camera.video.internal.encoder.VideoEncoderDataSpace.ENCODER_DATA_SPACE_BT2020_PQ;
import static androidx.camera.video.internal.encoder.VideoEncoderDataSpace.ENCODER_DATA_SPACE_BT709;
import static androidx.camera.video.internal.encoder.VideoEncoderDataSpace.ENCODER_DATA_SPACE_UNSPECIFIED;

import android.media.MediaFormat;
import android.util.Range;
import android.util.Rational;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy;
import androidx.camera.core.impl.Timebase;
import androidx.camera.video.MediaSpec;
import androidx.camera.video.VideoSpec;
import androidx.camera.video.internal.VideoValidatedEncoderProfilesProxy;
import androidx.camera.video.internal.encoder.VideoEncoderConfig;
import androidx.camera.video.internal.encoder.VideoEncoderDataSpace;
import androidx.camera.video.internal.utils.DynamicRangeUtil;
import androidx.core.util.Preconditions;
import androidx.core.util.Supplier;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A collection of utilities used for resolving and debugging video configurations.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class VideoConfigUtil {
    private static final String TAG = "VideoConfigUtil";

    private static final Map<String, Map<Integer, VideoEncoderDataSpace>> MIME_TO_DATA_SPACE_MAP =
            new HashMap<>();

    private static final Timebase DEFAULT_TIME_BASE = Timebase.UPTIME;

    // Should not be instantiated.
    private VideoConfigUtil() {
    }

    static {
        //--------------------------------------------------------------------------------------//
        // Mime and profile level to encoder data space map                                     //
        //--------------------------------------------------------------------------------------//
        Map<Integer, VideoEncoderDataSpace> profHevcMap = new HashMap<>();
        // We treat SDR (main profile) as unspecified. Allow the encoder to use default data space.
        profHevcMap.put(HEVCProfileMain, ENCODER_DATA_SPACE_UNSPECIFIED);
        profHevcMap.put(HEVCProfileMain10, ENCODER_DATA_SPACE_BT2020_HLG);
        profHevcMap.put(HEVCProfileMain10HDR10, ENCODER_DATA_SPACE_BT2020_PQ);
        profHevcMap.put(HEVCProfileMain10HDR10Plus, ENCODER_DATA_SPACE_BT2020_PQ);

        Map<Integer, VideoEncoderDataSpace> profAv1Map = new HashMap<>();
        // We treat SDR (main 8 profile) as unspecified. Allow the encoder to use default data
        // space.
        profAv1Map.put(AV1ProfileMain8, ENCODER_DATA_SPACE_UNSPECIFIED);
        profAv1Map.put(AV1ProfileMain10, ENCODER_DATA_SPACE_BT2020_HLG);
        profAv1Map.put(AV1ProfileMain10HDR10, ENCODER_DATA_SPACE_BT2020_PQ);
        profAv1Map.put(AV1ProfileMain10HDR10Plus, ENCODER_DATA_SPACE_BT2020_PQ);

        Map<Integer, VideoEncoderDataSpace> profVp9Map = new HashMap<>();
        // We treat SDR (profile 0) as unspecified. Allow the encoder to use default data space.
        profVp9Map.put(VP9Profile0, ENCODER_DATA_SPACE_UNSPECIFIED);
        profVp9Map.put(VP9Profile2, ENCODER_DATA_SPACE_BT2020_HLG);
        profVp9Map.put(VP9Profile2HDR, ENCODER_DATA_SPACE_BT2020_PQ);
        profVp9Map.put(VP9Profile2HDR10Plus, ENCODER_DATA_SPACE_BT2020_PQ);
        // Vp9 4:2:2 profiles
        profVp9Map.put(VP9Profile1, ENCODER_DATA_SPACE_UNSPECIFIED);
        profVp9Map.put(VP9Profile3, ENCODER_DATA_SPACE_BT2020_HLG);
        profVp9Map.put(VP9Profile3HDR, ENCODER_DATA_SPACE_BT2020_PQ);
        profVp9Map.put(VP9Profile3HDR10Plus, ENCODER_DATA_SPACE_BT2020_PQ);

        Map<Integer, VideoEncoderDataSpace> profDvMap = new HashMap<>();
        // For Dolby Vision profile 8, we only support 8.4 (10-bit HEVC HLG)
        profDvMap.put(DolbyVisionProfileDvheSt, ENCODER_DATA_SPACE_BT2020_HLG);
        // For Dolby Vision profile 9, we only support 9.2 (8-bit AVC SDR BT.709)
        profDvMap.put(DolbyVisionProfileDvavSe, ENCODER_DATA_SPACE_BT709);

        // Combine all mime type maps
        MIME_TO_DATA_SPACE_MAP.put(MediaFormat.MIMETYPE_VIDEO_HEVC, profHevcMap);
        MIME_TO_DATA_SPACE_MAP.put(MediaFormat.MIMETYPE_VIDEO_AV1, profAv1Map);
        MIME_TO_DATA_SPACE_MAP.put(MediaFormat.MIMETYPE_VIDEO_VP9, profVp9Map);
        MIME_TO_DATA_SPACE_MAP.put(MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION, profDvMap);
        //--------------------------------------------------------------------------------------//
    }

    /**
     * Resolves the video mime information into a {@link VideoMimeInfo}.
     *
     * @param mediaSpec        the media spec to resolve the mime info.
     * @param dynamicRange     a fully specified dynamic range.
     * @param encoderProfiles  the encoder profiles to resolve the mime info. It can be null if
     *                         there is no relevant encoder profiles.
     * @return the video MimeInfo.
     */
    @NonNull
    public static VideoMimeInfo resolveVideoMimeInfo(@NonNull MediaSpec mediaSpec,
            @NonNull DynamicRange dynamicRange,
            @Nullable VideoValidatedEncoderProfilesProxy encoderProfiles) {
        Preconditions.checkState(dynamicRange.isFullySpecified(), "Dynamic range must be a fully "
                + "specified dynamic range [provided dynamic range: " + dynamicRange + "]");
        String mediaSpecVideoMime = MediaSpec.outputFormatToVideoMime(mediaSpec.getOutputFormat());
        String resolvedVideoMime = mediaSpecVideoMime;
        VideoProfileProxy compatibleVideoProfile = null;
        if (encoderProfiles != null) {
            Set<Integer> encoderHdrFormats =
                    DynamicRangeUtil.dynamicRangeToVideoProfileHdrFormats(dynamicRange);
            Set<Integer> encoderBitDepths =
                    DynamicRangeUtil.dynamicRangeToVideoProfileBitDepth(dynamicRange);
            // Loop through EncoderProfile's VideoProfiles to search for one that supports the
            // provided dynamic range.
            for (VideoProfileProxy videoProfile : encoderProfiles.getVideoProfiles()) {
                // Skip if the dynamic range is not compatible
                if (!encoderHdrFormats.contains(videoProfile.getHdrFormat())
                        || !encoderBitDepths.contains(videoProfile.getBitDepth())) {
                    continue;
                }

                // Dynamic range is compatible. Use EncoderProfiles settings if the media spec's
                // output format is set to auto or happens to match the EncoderProfiles' output
                // format.
                String videoProfileMime = videoProfile.getMediaType();
                if (Objects.equals(mediaSpecVideoMime, videoProfileMime)) {
                    Logger.d(TAG, "MediaSpec video mime matches EncoderProfiles. Using "
                            + "EncoderProfiles to derive VIDEO settings [mime type: "
                            + resolvedVideoMime + "]");
                } else if (mediaSpec.getOutputFormat() == MediaSpec.OUTPUT_FORMAT_AUTO) {
                    Logger.d(TAG, "MediaSpec contains OUTPUT_FORMAT_AUTO. Using CamcorderProfile "
                            + "to derive VIDEO settings [mime type: " + resolvedVideoMime + ", "
                            + "dynamic range: " + dynamicRange + "]");
                } else {
                    continue;
                }

                compatibleVideoProfile = videoProfile;
                resolvedVideoMime = videoProfileMime;
                break;
            }
        }

        if (compatibleVideoProfile == null) {
            if (mediaSpec.getOutputFormat() == MediaSpec.OUTPUT_FORMAT_AUTO) {
                // If output format is AUTO, use the dynamic range to get the mime. Otherwise we
                // fall back to the default mime type from MediaSpec
                resolvedVideoMime = getDynamicRangeDefaultMime(dynamicRange);
            }

            if (encoderProfiles == null) {
                Logger.d(TAG, "No EncoderProfiles present. May rely on fallback defaults to derive "
                        + "VIDEO settings [chosen mime type: " + resolvedVideoMime + ", "
                        + "dynamic range: " + dynamicRange + "]");
            } else {
                Logger.d(TAG, "No video EncoderProfile is compatible with requested output format"
                        + " and dynamic range. May rely on fallback defaults to derive VIDEO "
                        + "settings [chosen mime type: " + resolvedVideoMime + ", "
                        + "dynamic range: " + dynamicRange + "]");
            }
        }

        VideoMimeInfo.Builder mimeInfoBuilder = VideoMimeInfo.builder(resolvedVideoMime);
        if (compatibleVideoProfile != null) {
            mimeInfoBuilder.setCompatibleVideoProfile(compatibleVideoProfile);
        }

        return mimeInfoBuilder.build();
    }

    /**
     * Returns a list of mimes required for the given dynamic range.
     *
     * <p>If the dynamic range is not supported, an {@link UnsupportedOperationException} will be
     * thrown.
     */
    @NonNull
    private static String getDynamicRangeDefaultMime(@NonNull DynamicRange dynamicRange) {
        switch (dynamicRange.getEncoding()) {
            case DynamicRange.ENCODING_DOLBY_VISION:
                // Dolby vision only supports dolby vision encoders
                return MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION;
            case DynamicRange.ENCODING_HLG:
            case DynamicRange.ENCODING_HDR10:
            case DynamicRange.ENCODING_HDR10_PLUS:
                // For now most hdr formats default to h265 (HEVC), though VP9 or AV1 may also be
                // supported.
                return MediaFormat.MIMETYPE_VIDEO_HEVC;
            case DynamicRange.ENCODING_SDR:
                // For SDR, default to h264 (AVC)
                return MediaFormat.MIMETYPE_VIDEO_AVC;
            default:
                throw new UnsupportedOperationException("Unsupported dynamic range: " + dynamicRange
                        + "\nNo supported default mime type available.");
        }
    }

    /**
     * Resolves video related information into a {@link VideoEncoderConfig}.
     *
     * @param videoMimeInfo          the video mime info.
     * @param videoSpec              the video spec.
     * @param inputTimebase          the timebase of the input frame.
     * @param surfaceSize            the surface size.
     * @param expectedFrameRateRange the expected frame rate range.
     * @return a VideoEncoderConfig.
     */
    @NonNull
    public static VideoEncoderConfig resolveVideoEncoderConfig(@NonNull VideoMimeInfo videoMimeInfo,
            @NonNull Timebase inputTimebase, @NonNull VideoSpec videoSpec,
            @NonNull Size surfaceSize, @NonNull DynamicRange dynamicRange,
            @NonNull Range<Integer> expectedFrameRateRange
    ) {
        Supplier<VideoEncoderConfig> configSupplier;
        VideoProfileProxy videoProfile = videoMimeInfo.getCompatibleVideoProfile();
        if (videoProfile != null) {
            configSupplier = new VideoEncoderConfigVideoProfileResolver(
                    videoMimeInfo.getMimeType(), inputTimebase, videoSpec, surfaceSize,
                    videoProfile, dynamicRange, expectedFrameRateRange);
        } else {
            configSupplier = new VideoEncoderConfigDefaultResolver(videoMimeInfo.getMimeType(),
                    inputTimebase, videoSpec, surfaceSize, dynamicRange, expectedFrameRateRange);
        }

        return configSupplier.get();
    }

    /**
     * Scales and clamps the bitrate based on the input conditions.
     *
     * @param baseBitrate     the bitrate to be scaled and clamped.
     * @param actualBitDepth  the actual bit depth.
     * @param baseBitDepth    the base bit depth.
     * @param actualFrameRate the actual frame rate.
     * @param baseFrameRate   the base frame rate.
     * @param actualWidth     the actual video width.
     * @param baseWidth       the base video width.
     * @param actualHeight    the actual video height.
     * @param baseHeight      the base video height.
     * @param clampedRange    the range to clamp. Set {@link VideoSpec#BITRATE_RANGE_AUTO} as no
     *                        clamp required.
     * @return the scaled and clamped bit rate.
     */
    public static int scaleAndClampBitrate(
            int baseBitrate,
            int actualBitDepth, int baseBitDepth,
            int actualFrameRate, int baseFrameRate,
            int actualWidth, int baseWidth,
            int actualHeight, int baseHeight,
            @NonNull Range<Integer> clampedRange) {
        //  Scale bit depth to match new bit depth
        Rational bitDepthRatio = new Rational(actualBitDepth, baseBitDepth);
        // Scale bitrate to match current frame rate
        Rational frameRateRatio = new Rational(actualFrameRate, baseFrameRate);
        // Scale bitrate depending on number of actual pixels relative to profile's
        // number of pixels.
        // TODO(b/191678894): This should come from the eventual crop rectangle rather
        //  than the full surface size.
        Rational widthRatio = new Rational(actualWidth, baseWidth);
        Rational heightRatio = new Rational(actualHeight, baseHeight);
        int resolvedBitrate =
                (int) (baseBitrate * bitDepthRatio.doubleValue() * frameRateRatio.doubleValue()
                        * widthRatio.doubleValue() * heightRatio.doubleValue());

        String debugString = "";
        if (Logger.isDebugEnabled(TAG)) {
            debugString = String.format("Base Bitrate(%dbps) * Bit Depth Ratio (%d / %d) * "
                            + "Frame Rate Ratio(%d / %d) * Width Ratio(%d / %d) * "
                            + "Height Ratio(%d / %d) = %d", baseBitrate,
                    actualBitDepth, baseBitDepth, actualFrameRate,
                    baseFrameRate, actualWidth, baseWidth, actualHeight, baseHeight,
                    resolvedBitrate);
        }

        if (!VideoSpec.BITRATE_RANGE_AUTO.equals(clampedRange)) {
            // Clamp the resolved bitrate
            resolvedBitrate = clampedRange.clamp(resolvedBitrate);
            if (Logger.isDebugEnabled(TAG)) {
                debugString += String.format("\nClamped to range %s -> %dbps", clampedRange,
                        resolvedBitrate);
            }
        }
        Logger.d(TAG, debugString);
        return resolvedBitrate;
    }

    /**
     * Returns the encoder data space for the given mime and profile.
     *
     * @return The data space for the given mime type and profile, or
     * {@link VideoEncoderDataSpace#ENCODER_DATA_SPACE_UNSPECIFIED} if the profile represents SDR or is unsupported.
     */
    @NonNull
    public static VideoEncoderDataSpace mimeAndProfileToEncoderDataSpace(@NonNull String mimeType,
            int codecProfileLevel) {
        Map<Integer, VideoEncoderDataSpace> profileToDataSpaceMap =
                MIME_TO_DATA_SPACE_MAP.get(mimeType);
        if (profileToDataSpaceMap != null) {
            VideoEncoderDataSpace dataSpace = profileToDataSpaceMap.get(codecProfileLevel);
            if (dataSpace != null) {
                return dataSpace;
            }
        }

        Logger.w(TAG, String.format("Unsupported mime type %s or profile level %d. Data space is "
                + "unspecified.", mimeType, codecProfileLevel));
        return ENCODER_DATA_SPACE_UNSPECIFIED;
    }

    /** Converts a {@link VideoProfileProxy} to a {@link VideoEncoderConfig}. */
    @NonNull
    public static VideoEncoderConfig toVideoEncoderConfig(@NonNull VideoProfileProxy videoProfile) {
        return VideoEncoderConfig.builder()
                .setMimeType(videoProfile.getMediaType())
                .setProfile(videoProfile.getProfile())
                .setResolution(new Size(videoProfile.getWidth(), videoProfile.getHeight()))
                .setFrameRate(videoProfile.getFrameRate())
                .setBitrate(videoProfile.getBitrate())
                .setInputTimebase(DEFAULT_TIME_BASE)
                .build();
    }
}
